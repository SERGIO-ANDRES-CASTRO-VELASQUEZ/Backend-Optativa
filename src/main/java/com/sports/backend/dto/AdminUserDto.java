package com.sports.backend.dto;

import com.sports.backend.model.Role;
import com.sports.backend.model.User;

import java.time.OffsetDateTime;

/**
 * DTO de respuesta del panel de administración para un usuario.
 *
 * <p>Diferencias con {@link UserDto} (vista pública/cliente):
 * <ul>
 *   <li>Incluye {@code phone} e {@code idDocument} (datos sensibles solo para admin).</li>
 *   <li>Incluye {@code active} para ver estado de la cuenta.</li>
 *   <li>Incluye {@code createdAt} para auditoría.</li>
 * </ul>
 *
 * <p>Usado como respuesta de:
 * <ul>
 *   <li>{@code GET  /api/admin/users}        (lista)</li>
 *   <li>{@code GET  /api/admin/users/{id}}   (detalle)</li>
 *   <li>{@code POST /api/admin/users}        (crear)</li>
 *   <li>{@code PUT  /api/admin/users/{id}}   (editar)</li>
 *   <li>{@code PUT  /api/admin/users/{id}/active} (toggle)</li>
 * </ul>
 */
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
