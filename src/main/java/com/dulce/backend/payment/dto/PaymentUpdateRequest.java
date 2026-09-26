package com.dulce.backend.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PaymentUpdateRequest(
        @DecimalMin(value = "0.01", message = "Valor do pagamento deve ser maior que zero.")
                BigDecimal amount,
        @Size(max = 50) String paymentMethod) {}
