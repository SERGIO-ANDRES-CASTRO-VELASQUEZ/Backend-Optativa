package com.sports.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RentalItemRequest(

        @NotNull(message = "El ID del producto es obligatorio")
        Long productId,

        @Min(value = 1, message = "La cantidad mínima es 1")
        int quantity
) {}
