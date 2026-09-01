package com.dulce.backend.payment.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(
        Long id, BigDecimal amount, OffsetDateTime paidAt, String paymentMethod) {}
