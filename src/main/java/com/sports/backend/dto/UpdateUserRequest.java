package com.sports.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code PUT /api/admin/users/{id}} — editar un usuario desde el panel admin.
 *
 * <p>Todos los campos son opcionales: {@code null} = no cambiar.
 *
 * <p>Para activar/desactivar un usuario usar el endpoint dedicado:
 * {@code PUT /api/admin/users/{id}/active} con {@link ToggleActiveRequest}.
 *
 * <p>El email se normaliza a minúsculas antes de guardar.
 */
public record UpdateUserRequest(

        @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
        String fullName,

        @Size(min = 3, max = 60, message = "El username debe tener entre 3 y 60 caracteres")
        String username,

        @Email(message = "El email no tiene un formato válido")
        String email,

        /**
         * Nueva contraseña. Si se envía, debe tener entre 6 y 100 caracteres.
         * Si es {@code null}, no se modifica la contraseña actual.
         */
        @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
        String newPassword,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String phone,

        @Size(max = 30, message = "La cédula no puede superar 30 caracteres")
        String idDocument
) {}
