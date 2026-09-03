package com.dulce.backend.customer.dto;

import java.util.List;

public record CustomerPageResponse(
        List<CustomerSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {}
