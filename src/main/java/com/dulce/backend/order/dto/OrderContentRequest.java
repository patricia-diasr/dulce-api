package com.dulce.backend.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderContentRequest(
        @NotNull OffsetDateTime pickupAt,
        @Size(max = 500) String notes,
        BigDecimal discount,
        @NotEmpty @Valid List<OrderItemRequest> items) {}
