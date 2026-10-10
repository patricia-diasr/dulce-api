package com.dulce.backend.report;

import com.dulce.backend.report.dto.TopFlavorsReportResponse;
import com.dulce.backend.report.dto.TopFlavorsReportResponse.Item;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TopFlavorsReportService {

    private static final String SQL =
            """
            SELECT f.id AS flavor_id, f.name AS name, f.active AS active,
                   COUNT(*) AS cakes_count,
                   COALESCE(SUM(s.weight_kg), 0) AS total_kg,
                   COALESCE(SUM(oi.unit_price), 0) AS gross_revenue
            FROM cake_order co
            JOIN order_item oi ON oi.cake_order_id = co.id
            JOIN flavor f ON f.id = oi.flavor_id
            JOIN size s ON s.id = oi.size_id
            WHERE co.status IN ('ACCEPTED', 'COMPLETED')
              AND co.pickup_at >= :start AND co.pickup_at < :endExclusive
            GROUP BY f.id, f.name, f.active
            ORDER BY %s DESC, name ASC
            LIMIT :limit
            """;

    private final JdbcClient jdbcClient;

    public TopFlavorsReportService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional(readOnly = true)
    public TopFlavorsReportResponse generate(
            LocalDate from, LocalDate to, TopFlavorsSortBy sortBy, int limit) {
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
                                                rs.getLong("flavor_id"),
                                                rs.getString("name"),
                                                rs.getBoolean("active"),
                                                rs.getLong("cakes_count"),
                                                ReportMath.money(rs.getBigDecimal("total_kg")),
                                                ReportMath.money(
                                                        rs.getBigDecimal("gross_revenue"))))
                        .list();

        return new TopFlavorsReportResponse(range.from(), range.to(), sortBy, items);
    }
}
