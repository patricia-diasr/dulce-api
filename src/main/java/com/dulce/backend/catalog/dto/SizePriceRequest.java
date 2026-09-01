package com.dulce.backend.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SizePriceRequest(
        @NotNull Long sizeId,
        @NotNull @DecimalMin(value = "0.01", message = "Preço de custo deve ser maior que zero.")
                BigDecimal costPrice,
        @NotNull @DecimalMin(value = "0.01", message = "Preço de venda deve ser maior que zero.")
                BigDecimal salePrice) {}
