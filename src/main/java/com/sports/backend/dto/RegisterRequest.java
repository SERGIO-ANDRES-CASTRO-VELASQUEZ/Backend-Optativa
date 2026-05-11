package com.sports.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 120)
        String fullName,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "Email invalido")
        @Size(max = 120)
        String email,

        @NotBlank(message = "El usuario es obligatorio")
        @Size(min = 3, max = 60)
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 6, max = 100, message = "La contrasena debe tener entre 6 y 100 caracteres")
        String password
) {}
