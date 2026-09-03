package com.dulce.backend.order.dto;

import com.dulce.backend.catalog.CakeFinish;
import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long flavorId,
        String flavorName,
        Long sizeId,
        String sizeName,
        CakeFinish topping,
        CakeFinish cakeBase,
        BigDecimal unitPrice,
        String message,
        String notes) {}
