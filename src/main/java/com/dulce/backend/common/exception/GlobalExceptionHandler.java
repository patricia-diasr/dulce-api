package com.dulce.backend.common.exception;

import com.dulce.backend.auth.InvalidCredentialsException;
import com.dulce.backend.customer.DuplicateEmailException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(FieldError::getDefaultMessage)
                        .orElse("Dados inválidos.");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        new ApiError(
                                Instant.now(),
                                400,
                                "Bad Request",
                                message,
                                request.getRequestURI()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        new ApiError(
                                Instant.now(),
                                401,
                                "Unauthorized",
                                ex.getMessage(),
                                request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ApiError> handleDuplicateEmail(
            DuplicateEmailException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        new ApiError(
                                Instant.now(),
                                409,
                                "Conflict",
                                ex.getMessage(),
                                request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        new ApiError(
                                Instant.now(),
                                500,
                                "Internal Server Error",
                                "Erro inesperado.",
                                request.getRequestURI()));
    }
}
