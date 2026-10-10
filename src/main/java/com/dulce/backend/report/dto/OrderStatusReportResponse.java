package com.dulce.backend.report.dto;

import com.dulce.backend.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record OrderStatusReportResponse(
        LocalDate from, LocalDate to, Total total, List<StatusLine> byStatus) {

    public record Total(long ordersCount, BigDecimal netAmount) {}

    public record StatusLine(OrderStatus status, long ordersCount, BigDecimal netAmount) {}
}
