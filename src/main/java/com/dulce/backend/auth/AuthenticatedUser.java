package com.dulce.backend.auth;

public record AuthenticatedUser(Long id, String email, Role role) {}
