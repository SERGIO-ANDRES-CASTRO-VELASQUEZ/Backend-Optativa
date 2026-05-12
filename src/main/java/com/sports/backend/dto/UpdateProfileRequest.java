package com.sports.backend.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(


        @Size(min = 2, max = 120, message = "El nombre debe tener entre 2 y 120 caracteres")
        String fullName,


        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String phone,


        @Size(max = 30, message = "El documento no puede superar 30 caracteres")
        String idDocument,


        String currentPassword,


        @Size(min = 6, max = 100, message = "La nueva contraseña debe tener entre 6 y 100 caracteres")
        String newPassword
) {}
