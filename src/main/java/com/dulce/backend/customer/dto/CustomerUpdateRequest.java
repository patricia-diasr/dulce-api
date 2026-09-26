package com.dulce.backend.customer.dto;

import com.dulce.backend.common.validation.Phone;
import com.dulce.backend.common.validation.PhoneNormalizer;
import com.dulce.backend.common.validation.StrictEmail;
import jakarta.validation.constraints.Size;

public record CustomerUpdateRequest(
        @Size(max = 150) String name,
        @StrictEmail @Size(max = 255) String email,
        @Size(max = 30) @Phone String phone,
        @Size(max = 500) String notes) {

    public CustomerUpdateRequest {
        name = name == null ? null : name.trim();
        email = (email == null || email.isBlank()) ? null : email.trim().toLowerCase();
        phone = phone == null ? null : PhoneNormalizer.normalize(phone);
    }
}
