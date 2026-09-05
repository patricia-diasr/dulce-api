package com.dulce.backend.order.dto;

import com.dulce.backend.order.OrderCreationChannel;
import com.dulce.backend.order.OrderStatus;
import com.dulce.backend.payment.dto.InvoiceResponse;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        String customerName,
        OffsetDateTime createdAt,
        OffsetDateTime pickupAt,
        OffsetDateTime completedAt,
        OrderStatus status,
        OrderCreationChannel creationChannel,
        String notes,
        List<OrderItemResponse> items,
        InvoiceResponse invoice) {}
