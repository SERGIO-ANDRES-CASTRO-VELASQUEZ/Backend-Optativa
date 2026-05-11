package com.sports.backend.dto;

import com.sports.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code POST /api/admin/users} — crear un usuario desde el panel admin.
 *
 * <p>A diferencia del registro público ({@link RegisterRequest}), el admin puede:
 * <ul>
 *   <li>Especificar el rol ({@code ADMIN} o {@code CLIENT}).</li>
 *   <li>Añadir teléfono y cédula directamente.</li>
 *   <li>Activar o desactivar al usuario desde el inicio.</li>
 * </ul>
 */
public record CreateUserRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
        String fullName,

        @NotBlank(message = "El username es obligatorio")
        @Size(min = 3, max = 60, message = "El username debe tener entre 3 y 60 caracteres")
        String username,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
        String password,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String phone,

        @Size(max = 30, message = "La cédula no puede superar 30 caracteres")
        String idDocument,

        /** Si es {@code null}, se asigna {@code CLIENT} por defecto. */
        Role role,

        /** Si es {@code null}, el usuario se crea activo ({@code true}) por defecto. */
        Boolean active
) {}
