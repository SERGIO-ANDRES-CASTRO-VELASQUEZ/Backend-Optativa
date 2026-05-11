package com.sports.backend.dto;

public record ForgotPasswordResponse(
        boolean exists,
        String message
) {}
