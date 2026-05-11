package com.sports.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ExtendRentalRequest(

        @NotNull(message = "La nueva fecha de fin es obligatoria")
        LocalDate newEndDate
) {}
