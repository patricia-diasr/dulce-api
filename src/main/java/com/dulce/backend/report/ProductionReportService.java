package com.dulce.backend.report;

import com.dulce.backend.common.exception.BadRequestException;
import com.dulce.backend.report.dto.ProductionReportResponse;
import com.dulce.backend.report.dto.ProductionReportResponse.Block;
import com.dulce.backend.report.dto.ProductionReportResponse.Combination;
import com.dulce.backend.report.dto.ProductionReportResponse.Day;
import com.dulce.backend.report.dto.ProductionReportResponse.GroupLine;
import com.dulce.backend.report.dto.ProductionReportResponse.SizeLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Necessidade de produção (RF33): pedidos ACCEPTED, em kg de bolo, com a divisão por tamanho. */
@Service
public class ProductionReportService {

    private static final long MAX_DAYS = 31;

    private static final String CAKES_SQL =
            """
            SELECT CAST(co.pickup_at AT TIME ZONE 'America/Sao_Paulo' AS date) AS pickup_date,
                   f.id AS flavor_id, f.name AS flavor_name,
                   s.id AS size_id, s.name AS size_name, s.weight_kg AS weight_kg,
                   oi.cake_base AS cake_base, oi.topping AS topping,
                   COUNT(*) AS quantity
            FROM cake_order co
            JOIN order_item oi ON oi.cake_order_id = co.id
            JOIN flavor f ON f.id = oi.flavor_id
            JOIN size s ON s.id = oi.size_id
            WHERE co.status = 'ACCEPTED'
              AND co.pickup_at >= :start AND co.pickup_at < :endExclusive
            GROUP BY pickup_date, f.id, f.name, s.id, s.name, s.weight_kg, oi.cake_base, oi.topping
            """;

    private static final String ORDERS_SQL =
            """
            SELECT CAST(co.pickup_at AT TIME ZONE 'America/Sao_Paulo' AS date) AS pickup_date,
                   COUNT(*) AS orders_count
            FROM cake_order co
            WHERE co.status = 'ACCEPTED'
              AND co.pickup_at >= :start AND co.pickup_at < :endExclusive
            GROUP BY pickup_date
            """;

    private record CakeRow(
            LocalDate date,
            long flavorId,
            String flavorName,
            long sizeId,
            String sizeName,
            BigDecimal weightKg,
            String cakeBase,
            String topping,
            long quantity) {}

    private record GroupKey(Long flavorId, String finish, String label) {}

    private record ComboKey(long flavorId, long sizeId, String cakeBase, String topping) {}

    private final JdbcClient jdbcClient;

    public ProductionReportService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional(readOnly = true)
    public ProductionReportResponse generate(LocalDate date, LocalDate from, LocalDate to) {
        ReportRange range = resolveRange(date, from, to);

        List<CakeRow> rows =
                jdbcClient
                        .sql(CAKES_SQL)
                        .param("start", range.start())
                        .param("endExclusive", range.endExclusive())
                        .query(
                                (rs, rowNum) ->
                                        new CakeRow(
                                                rs.getObject("pickup_date", LocalDate.class),
                                                rs.getLong("flavor_id"),
                                                rs.getString("flavor_name"),
                                                rs.getLong("size_id"),
                                                rs.getString("size_name"),
                                                rs.getBigDecimal("weight_kg"),
                                                rs.getString("cake_base"),
                                                rs.getString("topping"),
                                                rs.getLong("quantity")))
                        .list();

        Map<LocalDate, Long> ordersPerDay =
                jdbcClient
                        .sql(ORDERS_SQL)
                        .param("start", range.start())
                        .param("endExclusive", range.endExclusive())
                        .query(
                                (rs, rowNum) ->
                                        Map.entry(
                                                rs.getObject("pickup_date", LocalDate.class),
                                                rs.getLong("orders_count")))
                        .list()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        long totalOrders = ordersPerDay.values().stream().mapToLong(Long::longValue).sum();
        Block totals = buildBlock(totalOrders, rows);

        Map<LocalDate, List<CakeRow>> rowsByDate =
                rows.stream()
                        .collect(
                                Collectors.groupingBy(
                                        CakeRow::date, TreeMap::new, Collectors.toList()));

        List<Day> days =
                rowsByDate.entrySet().stream()
                        .map(
                                entry -> {
                                    Block block =
                                            buildBlock(
                                                    ordersPerDay.getOrDefault(entry.getKey(), 0L),
                                                    entry.getValue());
                                    return new Day(
                                            entry.getKey(),
                                            block.ordersCount(),
                                            block.cakesCount(),
                                            block.totalKg(),
                                            block.byFlavor(),
                                            block.byCakeBase(),
                                            block.byTopping(),
                                            block.combinations());
                                })
                        .toList();

        return new ProductionReportResponse(range.from(), range.to(), totals, days);
    }

    private ReportRange resolveRange(LocalDate date, LocalDate from, LocalDate to) {
        if (date != null && (from != null || to != null)) {
            throw new BadRequestException("Informe apenas 'date' ou o par 'from' e 'to'.");
        }

        if (date != null) {
            return new ReportRange(date, date);
        }

        LocalDate today = LocalDate.now(ReportRange.BUSINESS_ZONE);
        return ReportRange.resolve(from, to, MAX_DAYS, new ReportRange(today, today));
    }

    private Block buildBlock(long ordersCount, List<CakeRow> rows) {
        long cakes = rows.stream().mapToLong(CakeRow::quantity).sum();
        return new Block(
                ordersCount,
                cakes,
                ReportMath.money(totalKg(rows)),
                groupLines(rows, r -> new GroupKey(r.flavorId(), null, r.flavorName())),
                groupLines(rows, r -> new GroupKey(null, r.cakeBase(), finishLabel(r.cakeBase()))),
                groupLines(rows, r -> new GroupKey(null, r.topping(), finishLabel(r.topping()))),
                combinations(rows));
    }

    private List<GroupLine> groupLines(List<CakeRow> rows, Function<CakeRow, GroupKey> keyFn) {
        Map<GroupKey, List<CakeRow>> grouped =
                rows.stream()
                        .collect(
                                Collectors.groupingBy(
                                        keyFn, LinkedHashMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .map(entry -> toGroupLine(entry.getKey(), entry.getValue()))
                .sorted(
                        Comparator.comparing(GroupLine::totalKg)
                                .reversed()
                                .thenComparing(GroupLine::label))
                .toList();
    }

    private GroupLine toGroupLine(GroupKey key, List<CakeRow> rows) {
        Map<Long, List<CakeRow>> bySizeId =
                rows.stream().collect(Collectors.groupingBy(CakeRow::sizeId));

        List<SizeLine> sizeLines =
                bySizeId.values().stream()
                        .map(
                                sizeRows -> {
                                    CakeRow first = sizeRows.get(0);
                                    long quantity =
                                            sizeRows.stream().mapToLong(CakeRow::quantity).sum();
                                    return new SizeLine(
                                            first.sizeId(),
                                            first.sizeName(),
                                            ReportMath.money(first.weightKg()),
                                            quantity,
                                            ReportMath.money(
                                                    first.weightKg()
                                                            .multiply(
                                                                    BigDecimal.valueOf(quantity))));
                                })
                        .sorted(Comparator.comparing(SizeLine::weightKg).reversed())
                        .toList();

        long cakes = rows.stream().mapToLong(CakeRow::quantity).sum();
        return new GroupLine(
                key.flavorId(),
                key.finish(),
                key.label(),
                cakes,
                ReportMath.money(totalKg(rows)),
                sizeLines);
    }

    private List<Combination> combinations(List<CakeRow> rows) {
        Map<ComboKey, List<CakeRow>> grouped =
                rows.stream()
                        .collect(
                                Collectors.groupingBy(
                                        r ->
                                                new ComboKey(
                                                        r.flavorId(),
                                                        r.sizeId(),
                                                        r.cakeBase(),
                                                        r.topping()),
                                        LinkedHashMap::new,
                                        Collectors.toList()));

        return grouped.values().stream()
                .sorted(
                        Comparator.comparing((List<CakeRow> g) -> g.get(0).flavorName())
                                .thenComparing(g -> g.get(0).weightKg(), Comparator.reverseOrder())
                                .thenComparing(g -> g.get(0).cakeBase())
                                .thenComparing(g -> g.get(0).topping()))
                .map(
                        group -> {
                            CakeRow first = group.get(0);
                            long quantity = group.stream().mapToLong(CakeRow::quantity).sum();
                            return new Combination(
                                    first.flavorName(),
                                    first.sizeName(),
                                    first.cakeBase(),
                                    first.topping(),
                                    quantity,
                                    ReportMath.money(
                                            first.weightKg()
                                                    .multiply(BigDecimal.valueOf(quantity))));
                        })
                .toList();
    }

    private BigDecimal totalKg(List<CakeRow> rows) {
        return rows.stream()
                .map(r -> r.weightKg().multiply(BigDecimal.valueOf(r.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String finishLabel(String finish) {
        return "white".equals(finish) ? "Branca" : "Preta";
    }
}
