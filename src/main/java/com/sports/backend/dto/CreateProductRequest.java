package com.sports.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateProductRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,

        @NotBlank(message = "La descripción es obligatoria")
        String description,

        @NotNull(message = "La categoría es obligatoria")
        Long categoryId,

        @NotNull(message = "El precio por día es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0")
        BigDecimal pricePerDay,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        Boolean active,


        List<String> imageUrls,


        @Valid
        List<SpecRequest> specs
) {


    public record SpecRequest(
            @NotBlank(message = "La clave de la especificación es obligatoria")
            @Size(max = 60, message = "La clave no puede superar 60 caracteres")
            String key,

            @NotBlank(message = "El valor de la especificación es obligatorio")
            @Size(max = 150, message = "El valor no puede superar 150 caracteres")
            String value
    ) {}
}
