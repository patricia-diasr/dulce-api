package com.dulce.backend.auth.dto;

import com.dulce.backend.common.validation.StrictEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CustomerLoginRequest(
        @NotBlank @StrictEmail String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Código deve conter 6 dígitos numéricos.")
                String code) {

    public CustomerLoginRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
