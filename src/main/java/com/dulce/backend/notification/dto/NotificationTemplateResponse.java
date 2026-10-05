package com.dulce.backend.notification.dto;

public record NotificationTemplateResponse(
        Long id, String code, String description, String subject, String content) {}
