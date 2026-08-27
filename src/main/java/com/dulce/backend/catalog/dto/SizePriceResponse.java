package com.dulce.backend.catalog.dto;

import java.math.BigDecimal;

public record SizePriceResponse(
        Long sizeId, String sizeName, BigDecimal costPrice, BigDecimal salePrice) {}
