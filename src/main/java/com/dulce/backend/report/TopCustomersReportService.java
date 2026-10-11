package com.dulce.backend.report;

import com.dulce.backend.report.dto.TopCustomersReportResponse;
import com.dulce.backend.report.dto.TopCustomersReportResponse.Item;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopCustomersReportService {

    private static final String SQL =
            """
            WITH valid_orders AS (
              SELECT co.id AS order_id, co.customer_id AS customer_id,
                     inv.gross_amount - inv.discount AS net_amount
              FROM cake_order co
              JOIN invoice inv ON inv.cake_order_id = co.id
              WHERE co.status IN ('ACCEPTED', 'COMPLETED')
                AND co.pickup_at >= :start AND co.pickup_at < :endExclusive
            ),
            order_kg AS (
              SELECT oi.cake_order_id AS order_id, SUM(s.weight_kg) AS kg
              FROM order_item oi
              JOIN size s ON s.id = oi.size_id
              WHERE oi.cake_order_id IN (SELECT order_id FROM valid_orders)
              GROUP BY oi.cake_order_id
            )
            SELECT c.id AS customer_id, c.name AS name, c.phone AS phone, c.email AS email,
                   COUNT(*) AS orders_count,
                   COALESCE(SUM(ok.kg), 0) AS total_kg,
                   COALESCE(SUM(vo.net_amount), 0) AS net_amount
            FROM valid_orders vo
            JOIN customer c ON c.id = vo.customer_id
            LEFT JOIN order_kg ok ON ok.order_id = vo.order_id
            GROUP BY c.id, c.name, c.phone, c.email
            ORDER BY %s DESC, name ASC
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public TopCustomersReportService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional(readOnly = true)
    public TopCustomersReportResponse generate(
            LocalDate from, LocalDate to, TopCustomersSortBy sortBy, int limit) {
        ReportRange range = ReportRange.resolve(from, to, ReportRange.DEFAULT_MAX_DAYS);

        List<Item> items =
                jdbcClient
                        .sql(SQL.formatted(sortBy.column()))
                        .param("start", range.start())
                        .param("endExclusive", range.endExclusive())
                        .param("limit", limit)
                        .query(
                                (rs, rowNum) ->
                                        new Item(
                                                rs.getLong("customer_id"),
                                                rs.getString("name"),
                                                rs.getString("phone"),
                                                rs.getString("email"),
                                                rs.getLong("orders_count"),
                                                ReportMath.money(rs.getBigDecimal("total_kg")),
                                                ReportMath.money(rs.getBigDecimal("net_amount"))))
                        .list();

        return new TopCustomersReportResponse(range.from(), range.to(), sortBy, items);
    }
}
