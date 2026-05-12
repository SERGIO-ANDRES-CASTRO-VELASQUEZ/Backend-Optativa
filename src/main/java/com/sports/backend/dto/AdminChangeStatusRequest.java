package com.sports.backend.dto;

import com.sports.backend.model.RentalStatus;
import jakarta.validation.constraints.NotNull;

public record AdminChangeStatusRequest(

        @NotNull(message = "El campo 'status' es obligatorio")
        RentalStatus status
) {}
