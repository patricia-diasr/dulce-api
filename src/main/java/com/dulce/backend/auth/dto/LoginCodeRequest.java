package com.dulce.backend.auth.dto;

import com.dulce.backend.common.validation.StrictEmail;
import jakarta.validation.constraints.NotBlank;

public record LoginCodeRequest(@NotBlank @StrictEmail String email) {

    public LoginCodeRequest {
        email = email == null ? null : email.trim().toLowerCase();
    }
}
