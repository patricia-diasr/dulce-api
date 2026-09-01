package com.dulce.backend.customer.dto;

import com.dulce.backend.order.dto.OrderSummaryResponse;
import java.util.List;

public record CustomerDetailResponse(
        Long id,
        String name,
        String email,
        String phone,
        String notes,
        List<OrderSummaryResponse> orders) {}
