package com.dulce.backend.report.dto;

import com.dulce.backend.report.TopFlavorsSortBy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TopFlavorsReportResponse(
        LocalDate from, LocalDate to, TopFlavorsSortBy sortBy, List<Item> items) {

    public record Item(
            Long flavorId,
            String name,
            boolean active,
            long cakesCount,
            BigDecimal totalKg,
            BigDecimal grossRevenue) {}
}
