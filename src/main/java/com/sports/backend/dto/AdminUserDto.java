package com.sports.backend.dto;

import com.sports.backend.model.Role;
import com.sports.backend.model.User;

import java.time.OffsetDateTime;

public record AdminUserDto(
        Long id,
        String fullName,
        String username,
        String email,
        String phone,
        String idDocument,
        Role role,
        boolean active,
        OffsetDateTime createdAt
) {

    public static AdminUserDto from(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getIdDocument(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
