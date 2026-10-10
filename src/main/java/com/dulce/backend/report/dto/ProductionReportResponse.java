package com.dulce.backend.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductionReportResponse(LocalDate from, LocalDate to, Block totals, List<Day> days) {

    public record Block(
            long ordersCount,
            long cakesCount,
            BigDecimal totalKg,
            List<GroupLine> byFlavor,
            List<GroupLine> byCakeBase,
            List<GroupLine> byTopping,
            List<Combination> combinations) {}

    public record Day(
            LocalDate date,
            long ordersCount,
            long cakesCount,
            BigDecimal totalKg,
            List<GroupLine> byFlavor,
            List<GroupLine> byCakeBase,
            List<GroupLine> byTopping,
            List<Combination> combinations) {}

    public record GroupLine(
            Long flavorId,
            String finish,
            String label,
            long cakesCount,
            BigDecimal totalKg,
            List<SizeLine> bySize) {}

    public record SizeLine(
            Long sizeId, String sizeName, BigDecimal weightKg, long quantity, BigDecimal totalKg) {}

    public record Combination(
            String flavorName,
            String sizeName,
            String cakeBase,
            String topping,
            long quantity,
            BigDecimal totalKg) {}
}
