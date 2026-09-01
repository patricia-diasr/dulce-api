package com.dulce.backend.customer.dto;

public record CustomerSummaryResponse(
        Long id, String name, String phone, String email, String notes) {}
