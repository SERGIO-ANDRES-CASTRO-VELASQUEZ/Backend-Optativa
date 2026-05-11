package com.sports.backend.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Body de {@code PUT /api/admin/users/{id}/active}.
 *
 * <p>Permite activar o desactivar un usuario sin tocar el resto de sus datos.
 * Un usuario desactivado no puede hacer login ({@code AuthService} lo verifica).
 *
 * <p>Ejemplo de uso:
 * <pre>
 *   PUT /api/admin/users/5/active
 *   { "active": false }
 * </pre>
 */
public record ToggleActiveRequest(

        @NotNull(message = "El campo 'active' es obligatorio")
        Boolean active
) {}
