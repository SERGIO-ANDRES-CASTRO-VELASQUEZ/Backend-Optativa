package com.sports.backend.dto;

public record AuthResponse(
        String token,
        long expiresInMs,
        UserDto user
) {}
