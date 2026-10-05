package com.dulce.backend.notification.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record NotificationTemplateUpdateRequest(
        @Size(max = 200)
                @Pattern(regexp = "[^\r\n]*", message = "Assunto não pode conter quebra de linha.")
                String subject,
        @Size(max = 5000) String content) {}
