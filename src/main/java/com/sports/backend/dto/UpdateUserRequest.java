package com.sports.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

        @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
        String fullName,

        @Size(min = 3, max = 60, message = "El username debe tener entre 3 y 60 caracteres")
        String username,

        @Email(message = "El email no tiene un formato válido")
        String email,

        @Size(min = 6, max = 100, message = "La contraseña debe tener entre 6 y 100 caracteres")
        String newPassword,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String phone,

        @Size(max = 30, message = "La cédula no puede superar 30 caracteres")
        String idDocument
) {}
