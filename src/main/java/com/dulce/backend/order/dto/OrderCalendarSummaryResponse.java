package com.dulce.backend.order.dto;

import com.dulce.backend.order.OrderStatus;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderCalendarSummaryResponse(
        Long id,
        Long customerId,
        String customerName,
        OffsetDateTime pickupAt,
        OrderStatus status,
        String notes,
        List<OrderCalendarItemResponse> items) {}
