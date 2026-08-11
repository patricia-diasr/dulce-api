package com.dulce.backend.common.exception;

import java.time.Instant;

/** Formato padrão de erro retornado pela API. */
public record ApiError(Instant timestamp, int status, String error, String message, String path) {}
