package com.dulce.backend.customer.dto;

import com.dulce.backend.common.validation.Phone;
import com.dulce.backend.common.validation.PhoneNormalizer;
import com.dulce.backend.common.validation.StrictEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegistrationRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @StrictEmail @Size(max = 255) String email,
        @NotBlank @Size(max = 30) @Phone String phone) {

    public CustomerRegistrationRequest {
        name = name == null ? null : name.trim();
        email = email == null ? null : email.trim().toLowerCase();
        phone = phone == null ? null : PhoneNormalizer.normalize(phone);
    }
}
