package com.dulce.backend.report.dto;

import com.dulce.backend.payment.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record FinancialReportResponse(
        LocalDate from,
        LocalDate to,
        Summary summary,
        List<StatusLine> byInvoiceStatus,
        long pendingCompletedOrdersCount,
        List<PendingOrder> pendingCompletedOrders,
        Refunds refunds) {

    public record Summary(
            long ordersCount,
            BigDecimal totalDue,
            BigDecimal totalReceived,
            BigDecimal totalPending) {}

    public record StatusLine(
            InvoiceStatus status,
            long ordersCount,
            BigDecimal due,
            BigDecimal received,
            BigDecimal pending) {}

    public record PendingOrder(
            Long orderId,
            Long customerId,
            String customerName,
            String customerPhone,
            OffsetDateTime pickupAt,
            OffsetDateTime completedAt,
            BigDecimal netAmount,
            BigDecimal amountPaid,
            BigDecimal balance) {}

    public record Refunds(long ordersCount, BigDecimal totalAmount) {}
}
