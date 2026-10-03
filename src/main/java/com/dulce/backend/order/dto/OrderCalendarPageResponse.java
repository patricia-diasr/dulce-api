package com.dulce.backend.order.dto;

import java.util.List;

public record OrderCalendarPageResponse(
        List<OrderCalendarSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {}
