package com.dulce.backend.payment.dto;

import com.dulce.backend.payment.InvoiceStatus;
import java.math.BigDecimal;
import java.util.List;

public record InvoiceResponse(
        Long id,
        BigDecimal grossAmount,
        BigDecimal discount,
        InvoiceStatus status,
        BigDecimal refundDue,
        List<PaymentResponse> payments) {}
