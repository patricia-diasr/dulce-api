package com.dulce.backend.auth.dto;

import java.time.OffsetDateTime;

public record AuthResponse(String token, String role, String name, OffsetDateTime expiresAt) {}
