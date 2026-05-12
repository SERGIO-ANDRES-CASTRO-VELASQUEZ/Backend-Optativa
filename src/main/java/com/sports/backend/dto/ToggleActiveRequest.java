package com.sports.backend.dto;

import jakarta.validation.constraints.NotNull;

public record ToggleActiveRequest(

        @NotNull(message = "El campo 'active' es obligatorio")
        Boolean active
) {}
