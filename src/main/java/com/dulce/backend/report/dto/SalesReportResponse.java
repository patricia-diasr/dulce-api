package com.dulce.backend.report.dto;

import com.dulce.backend.report.ReportGroupBy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SalesReportResponse(
        LocalDate from,
        LocalDate to,
        ReportGroupBy groupBy,
        Summary summary,
        List<SeriesItem> series) {

    public record Summary(
            long ordersCount,
            long itemsCount,
            BigDecimal totalKg,
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal netAmount,
            BigDecimal averageTicket) {}

    public record SeriesItem(
            LocalDate periodStart,
            long ordersCount,
            long itemsCount,
            BigDecimal totalKg,
            BigDecimal grossAmount,
            BigDecimal discountAmount,
            BigDecimal netAmount,
            BigDecimal averageTicket) {}
}
