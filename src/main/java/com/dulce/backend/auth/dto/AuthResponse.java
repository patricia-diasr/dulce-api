package com.dulce.backend.auth.dto;

import java.time.OffsetDateTime;

public record AuthResponse(
        String token, Long user, String role, String name, OffsetDateTime expiresAt) {}
