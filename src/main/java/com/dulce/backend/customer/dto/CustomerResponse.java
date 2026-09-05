package com.dulce.backend.customer.dto;

public record CustomerResponse(Long id, String name, String email, String phone, String notes) {}
