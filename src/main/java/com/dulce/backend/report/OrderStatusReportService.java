package com.dulce.backend.report;

import com.dulce.backend.order.OrderStatus;
import com.dulce.backend.report.dto.OrderStatusReportResponse;
import com.dulce.backend.report.dto.OrderStatusReportResponse.StatusLine;
import com.dulce.backend.report.dto.OrderStatusReportResponse.Total;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderStatusReportService {

    private static final String SQL =
            """
            SELECT co.status AS status,
                   COUNT(*) AS orders_count,
                   COALESCE(SUM(inv.gross_amount - inv.discount), 0) AS net_amount
            FROM cake_order co
            LEFT JOIN invoice inv ON inv.cake_order_id = co.id
            WHERE co.pickup_at >= :start AND co.pickup_at < :endExclusive
            GROUP BY co.status
            """;

    private record StatusRow(long ordersCount, BigDecimal netAmount) {}

    private final JdbcClient jdbcClient;

    public OrderStatusReportService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional(readOnly = true)
    public OrderStatusReportResponse generate(LocalDate from, LocalDate to) {
        ReportRange range = ReportRange.resolve(from, to, ReportRange.DEFAULT_MAX_DAYS);

        Map<OrderStatus, StatusRow> rows =
                jdbcClient
                        .sql(SQL)
                        .param("start", range.start())
                        .param("endExclusive", range.endExclusive())
                        .query(
                                (rs, rowNum) ->
                                        Map.entry(
                                                OrderStatus.valueOf(rs.getString("status")),
                                                new StatusRow(
                                                        rs.getLong("orders_count"),
                                                        rs.getBigDecimal("net_amount"))))
                        .list()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<StatusLine> lines = new ArrayList<>();
        long totalOrders = 0;
        BigDecimal totalNet = ReportMath.ZERO;

        for (OrderStatus status : OrderStatus.values()) {
            StatusRow row = rows.get(status);
            long count = row == null ? 0 : row.ordersCount();
            BigDecimal net = row == null ? ReportMath.ZERO : ReportMath.money(row.netAmount());
            lines.add(new StatusLine(status, count, net));
            totalOrders += count;
            totalNet = totalNet.add(net);
        }

        return new OrderStatusReportResponse(
                range.from(), range.to(), new Total(totalOrders, totalNet), lines);
    }
}
