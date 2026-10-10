package com.dulce.backend.report;

import com.dulce.backend.payment.InvoiceStatus;
import com.dulce.backend.report.dto.FinancialReportResponse;
import com.dulce.backend.report.dto.FinancialReportResponse.PendingOrder;
import com.dulce.backend.report.dto.FinancialReportResponse.Refunds;
import com.dulce.backend.report.dto.FinancialReportResponse.StatusLine;
import com.dulce.backend.report.dto.FinancialReportResponse.Summary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialReportService {

    private static final int PENDING_LIST_LIMIT = 100;

    private static final String ORDERS_SQL =
            """
            SELECT co.id AS order_id, co.status AS order_status, co.customer_id AS customer_id,
                   c.name AS customer_name, c.phone AS customer_phone,
                   co.pickup_at AS pickup_at, co.completed_at AS completed_at,
                   (inv.gross_amount - inv.discount) AS due,
                   COALESCE((SELECT SUM(p.amount) FROM payment p WHERE p.invoice_id = inv.id), 0)
                       AS received
            FROM cake_order co
            JOIN invoice inv ON inv.cake_order_id = co.id
            JOIN customer c ON c.id = co.customer_id
            WHERE co.status IN ('ACCEPTED', 'COMPLETED')
              AND co.pickup_at >= :start AND co.pickup_at < :endExclusive
            ORDER BY co.pickup_at ASC, co.id ASC
            """;

    private static final String REFUNDS_SQL =
            """
            SELECT COUNT(DISTINCT co.id) AS orders_count,
                   COALESCE(SUM(p.amount), 0) AS total_amount
            FROM cake_order co
            JOIN invoice inv ON inv.cake_order_id = co.id
            JOIN payment p ON p.invoice_id = inv.id
            WHERE co.status IN ('CANCELED', 'REJECTED')
              AND co.pickup_at >= :start AND co.pickup_at < :endExclusive
            """;

    private record OrderFinance(
            long orderId,
            String orderStatus,
            long customerId,
            String customerName,
            String customerPhone,
            OffsetDateTime pickupAt,
            OffsetDateTime completedAt,
            BigDecimal due,
            BigDecimal received) {

        BigDecimal balance() {
            BigDecimal balance = due.subtract(received);
            return balance.signum() > 0 ? balance : ReportMath.ZERO;
        }

        InvoiceStatus invoiceStatus() {
            if (received.signum() <= 0) {
                return InvoiceStatus.PENDING;
            }
            return received.compareTo(due) >= 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL;
        }
    }

    private final JdbcClient jdbcClient;

    public FinancialReportService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Transactional(readOnly = true)
    public FinancialReportResponse generate(LocalDate from, LocalDate to) {
        ReportRange range = ReportRange.resolve(from, to, ReportRange.DEFAULT_MAX_DAYS);

        List<OrderFinance> orders =
                jdbcClient
                        .sql(ORDERS_SQL)
                        .param("start", range.start())
                        .param("endExclusive", range.endExclusive())
                        .query(
                                (rs, rowNum) ->
                                        new OrderFinance(
                                                rs.getLong("order_id"),
                                                rs.getString("order_status"),
                                                rs.getLong("customer_id"),
                                                rs.getString("customer_name"),
                                                rs.getString("customer_phone"),
                                                ReportRange.toBusinessZone(
                                                        rs.getObject(
                                                                "pickup_at", OffsetDateTime.class)),
                                                ReportRange.toBusinessZone(
                                                        rs.getObject(
                                                                "completed_at",
                                                                OffsetDateTime.class)),
                                                ReportMath.money(rs.getBigDecimal("due")),
                                                ReportMath.money(rs.getBigDecimal("received"))))
                        .list();

        BigDecimal totalDue = ReportMath.ZERO;
        BigDecimal totalReceived = ReportMath.ZERO;
        BigDecimal totalPending = ReportMath.ZERO;
        for (OrderFinance order : orders) {
            totalDue = totalDue.add(order.due());
            totalReceived = totalReceived.add(order.received());
            totalPending = totalPending.add(order.balance());
        }
        Summary summary = new Summary(orders.size(), totalDue, totalReceived, totalPending);

        List<StatusLine> byStatus = new ArrayList<>();
        for (InvoiceStatus status : InvoiceStatus.values()) {
            List<OrderFinance> group =
                    orders.stream().filter(o -> o.invoiceStatus() == status).toList();
            BigDecimal due =
                    group.stream().map(OrderFinance::due).reduce(ReportMath.ZERO, BigDecimal::add);
            BigDecimal received =
                    group.stream()
                            .map(OrderFinance::received)
                            .reduce(ReportMath.ZERO, BigDecimal::add);
            BigDecimal pending =
                    group.stream()
                            .map(OrderFinance::balance)
                            .reduce(ReportMath.ZERO, BigDecimal::add);
            byStatus.add(new StatusLine(status, group.size(), due, received, pending));
        }

        List<OrderFinance> pendingCompleted =
                orders.stream()
                        .filter(
                                o ->
                                        "COMPLETED".equals(o.orderStatus())
                                                && o.balance().signum() > 0)
                        .toList();
        List<PendingOrder> pendingList =
                pendingCompleted.stream()
                        .limit(PENDING_LIST_LIMIT)
                        .map(
                                o ->
                                        new PendingOrder(
                                                o.orderId(),
                                                o.customerId(),
                                                o.customerName(),
                                                o.customerPhone(),
                                                o.pickupAt(),
                                                o.completedAt(),
                                                o.due(),
                                                o.received(),
                                                o.balance()))
                        .toList();

        Refunds refunds =
                jdbcClient
                        .sql(REFUNDS_SQL)
                        .param("start", range.start())
                        .param("endExclusive", range.endExclusive())
                        .query(
                                (rs, rowNum) ->
                                        new Refunds(
                                                rs.getLong("orders_count"),
                                                ReportMath.money(rs.getBigDecimal("total_amount"))))
                        .single();

        return new FinancialReportResponse(
                range.from(),
                range.to(),
                summary,
                byStatus,
                pendingCompleted.size(),
                pendingList,
                refunds);
    }
}
