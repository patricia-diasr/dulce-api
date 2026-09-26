package com.dulce.backend.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull
                @DecimalMin(value = "0.01", message = "Valor do pagamento deve ser maior que zero.")
                BigDecimal amount,
        @NotBlank(message = "Método de pagamento é obrigatório.") @Size(max = 50)
                String paymentMethod) {}
