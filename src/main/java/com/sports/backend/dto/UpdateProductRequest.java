package com.sports.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record UpdateProductRequest(

        @Size(min = 1, max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,

        String description,

        Long categoryId,

        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0")
        BigDecimal pricePerDay,

        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,


        Boolean active,


        @Valid
        List<CreateProductRequest.SpecRequest> specs
) {}
