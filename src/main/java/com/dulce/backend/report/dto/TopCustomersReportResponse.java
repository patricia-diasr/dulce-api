package com.dulce.backend.report.dto;

import com.dulce.backend.report.TopCustomersSortBy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TopCustomersReportResponse(
        LocalDate from, LocalDate to, TopCustomersSortBy sortBy, List<Item> items) {

    public record Item(
            Long customerId,
            String name,
            String phone,
            String email,
            long ordersCount,
            BigDecimal totalKg,
            BigDecimal netAmount) {}
}
