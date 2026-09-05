package com.dulce.backend.order.dto;

import com.dulce.backend.catalog.CakeFinish;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderItemRequest(
        @NotNull Long flavorId,
        @NotNull Long sizeId,
        @NotNull CakeFinish cakeBase,
        @NotNull CakeFinish topping,
        @Size(max = 30) String message,
        @Size(max = 300) String notes) {}
