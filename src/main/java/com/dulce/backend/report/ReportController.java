package com.dulce.backend.report;

import com.dulce.backend.report.dto.FinancialReportResponse;
import com.dulce.backend.report.dto.OrderStatusReportResponse;
import com.dulce.backend.report.dto.ProductionReportResponse;
import com.dulce.backend.report.dto.SalesReportResponse;
import com.dulce.backend.report.dto.TopCustomersReportResponse;
import com.dulce.backend.report.dto.TopFlavorsReportResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final SalesReportService salesReportService;
    private final ProductionReportService productionReportService;
    private final OrderStatusReportService orderStatusReportService;
    private final TopFlavorsReportService topFlavorsReportService;
    private final FinancialReportService financialReportService;
    private final TopCustomersReportService topCustomersReportService;

    public ReportController(
            SalesReportService salesReportService,
            ProductionReportService productionReportService,
            OrderStatusReportService orderStatusReportService,
            TopFlavorsReportService topFlavorsReportService,
            FinancialReportService financialReportService,
            TopCustomersReportService topCustomersReportService) {
        this.salesReportService = salesReportService;
        this.productionReportService = productionReportService;
        this.orderStatusReportService = orderStatusReportService;
        this.topFlavorsReportService = topFlavorsReportService;
        this.financialReportService = financialReportService;
        this.topCustomersReportService = topCustomersReportService;
    }

    @GetMapping("/sales")
    public SalesReportResponse sales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(defaultValue = "day") ReportGroupBy groupBy) {
        return salesReportService.generate(from, to, groupBy);
    }

    @GetMapping("/production")
    public ProductionReportResponse production(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {
        return productionReportService.generate(date, from, to);
    }

    @GetMapping("/orders-by-status")
    public OrderStatusReportResponse ordersByStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {
        return orderStatusReportService.generate(from, to);
    }

    @GetMapping("/top-flavors")
    public TopFlavorsReportResponse topFlavors(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(defaultValue = "revenue") TopFlavorsSortBy sortBy,
            @RequestParam(defaultValue = "10")
                    @Min(value = 1, message = "Parâmetro inválido.")
                    @Max(value = 50, message = "Parâmetro inválido.")
                    int limit) {
        return topFlavorsReportService.generate(from, to, sortBy, limit);
    }

    @GetMapping("/financial")
    public FinancialReportResponse financial(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to) {
        return financialReportService.generate(from, to);
    }

    @GetMapping("/top-customers")
    public TopCustomersReportResponse topCustomers(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(defaultValue = "revenue") TopCustomersSortBy sortBy,
            @RequestParam(defaultValue = "10")
                    @Min(value = 1, message = "Parâmetro inválido.")
                    @Max(value = 50, message = "Parâmetro inválido.")
                    int limit) {
        return topCustomersReportService.generate(from, to, sortBy, limit);
    }
}
