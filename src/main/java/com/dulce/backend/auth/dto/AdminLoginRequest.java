package com.dulce.backend.auth.dto;

import com.dulce.backend.common.validation.StrictEmail;
import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(@NotBlank @StrictEmail String email, @NotBlank String password) {

    public AdminLoginRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
