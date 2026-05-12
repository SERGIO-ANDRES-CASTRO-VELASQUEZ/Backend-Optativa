package com.sports.backend.dto;

import com.sports.backend.model.Role;
import com.sports.backend.model.User;

public record UserDto(
        Long id,
        String fullName,
        String username,
        String email,
        Role role,
        String phone,
        String idDocument
) {
    public static UserDto from(User u) {
        return new UserDto(
                u.getId(),
                u.getFullName(),
                u.getUsername(),
                u.getEmail(),
                u.getRole(),
                u.getPhone(),
                u.getIdDocument()
        );
    }
}
