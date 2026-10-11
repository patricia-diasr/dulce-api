package com.dulce.backend.report;

import com.dulce.backend.report.dto.SalesReportResponse;
import com.dulce.backend.report.dto.SalesReportResponse.SeriesItem;
import com.dulce.backend.report.dto.SalesReportResponse.Summary;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalesReportService {

    private static final String SQL =
            """
            WITH order_totals AS (
              SELECT co.id AS order_id,
                     CAST(date_trunc('%s', co.pickup_at AT TIME ZONE 'America/Sao_Paulo') AS date)
                         AS period_start,
                     inv.gross_amount AS gross_amount,
                     inv.discount AS discount
              FROM cake_order co
              JOIN invoice inv ON inv.cake_order_id = co.id
              WHERE co.status IN ('ACCEPTED', 'COMPLETED')
                AND co.pickup_at >= :start AND co.pickup_at < :endExclusive
            ),
            item_totals AS (
              SELECT oi.cake_order_id AS order_id,
                     COUNT(*) AS items_count,
                     SUM(s.weight_kg) AS total_kg
              FROM order_item oi
              JOIN size s ON s.id = oi.size_id
              WHERE oi.cake_order_id IN (SELECT order_id FROM order_totals)
              GROUP BY oi.cake_order_id
            )
            SELECT ot.period_start AS period_start,
                   COUNT(*) AS orders_count,
                   COALESCE(SUM(it.items_count), 0) AS items_count,
                   COALESCE(SUM(it.total_kg), 0) AS total_kg,
                   COALESCE(SUM(ot.gross_amount), 0) AS gross_amount,
                   COALESCE(SUM(ot.discount), 0) AS discount_amount
            FROM order_totals ot
            LEFT JOIN item_totals it ON it.order_id = ot.order_id
            GROUP BY ot.period_start
            """;

    private record PeriodRow(
            LocalDate periodStart,
            long ordersCount,
            long itemsCount,
            BigDecimal totalKg,
            BigDecimal grossAmount,
            BigDecimal discountAmount) {}

    private final JdbcClient jdbcClient;

    public SalesReportService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional(readOnly = true)
    public SalesReportResponse generate(LocalDate from, LocalDate to, ReportGroupBy groupBy) {
        ReportRange range = ReportRange.resolve(from, to, ReportRange.DEFAULT_MAX_DAYS);

        Map<LocalDate, PeriodRow> rowsByPeriod =
                jdbcClient
                        .sql(SQL.formatted(groupBy.name()))
                        .param("start", range.start())
                        .param("endExclusive", range.endExclusive())
                        .query(
                                (rs, rowNum) ->
                                        new PeriodRow(
                                                rs.getObject("period_start", LocalDate.class),
                                                rs.getLong("orders_count"),
                                                rs.getLong("items_count"),
                                                rs.getBigDecimal("total_kg"),
                                                rs.getBigDecimal("gross_amount"),
                                                rs.getBigDecimal("discount_amount")))
                        .list()
                        .stream()
                        .collect(Collectors.toMap(PeriodRow::periodStart, Function.identity()));

        List<SeriesItem> series = new ArrayList<>();
        long totalOrders = 0;
        long totalItems = 0;
        BigDecimal totalKg = ReportMath.ZERO;
        BigDecimal totalGross = ReportMath.ZERO;
        BigDecimal totalDiscount = ReportMath.ZERO;

        for (LocalDate period = firstPeriod(range.from(), groupBy);
                !period.isAfter(range.to());
                period = nextPeriod(period, groupBy)) {
            PeriodRow row = rowsByPeriod.get(period);

            long orders = row == null ? 0 : row.ordersCount();
            long items = row == null ? 0 : row.itemsCount();
            BigDecimal kg = row == null ? ReportMath.ZERO : ReportMath.money(row.totalKg());
            BigDecimal gross = row == null ? ReportMath.ZERO : ReportMath.money(row.grossAmount());
            BigDecimal discount =
                    row == null ? ReportMath.ZERO : ReportMath.money(row.discountAmount());
            BigDecimal net = gross.subtract(discount);

            series.add(
                    new SeriesItem(
                            period,
                            orders,
                            items,
                            kg,
                            gross,
                            discount,
                            net,
                            ReportMath.average(net, orders)));

            totalOrders += orders;
            totalItems += items;
            totalKg = totalKg.add(kg);
            totalGross = totalGross.add(gross);
            totalDiscount = totalDiscount.add(discount);
        }

        BigDecimal totalNet = totalGross.subtract(totalDiscount);
        Summary summary =
                new Summary(
                        totalOrders,
                        totalItems,
                        totalKg,
                        totalGross,
                        totalDiscount,
                        totalNet,
                        ReportMath.average(totalNet, totalOrders));

        return new SalesReportResponse(range.from(), range.to(), groupBy, summary, series);
    }

    private LocalDate firstPeriod(LocalDate from, ReportGroupBy groupBy) {
        return switch (groupBy) {
            case day -> from;
            case week -> from.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case month -> from.withDayOfMonth(1);
        };
    }

    private LocalDate nextPeriod(LocalDate period, ReportGroupBy groupBy) {
        return switch (groupBy) {
            case day -> period.plusDays(1);
            case week -> period.plusWeeks(1);
            case month -> period.plusMonths(1);
        };
    }
}
