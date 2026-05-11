package com.sports.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body de {@code POST /api/admin/products} — crear un producto nuevo.
 *
 * <p>Las imágenes se pueden adjuntar después via multipart con
 * {@code POST /api/admin/products/{id}/images}, pero también se aceptan
 * URLs externas en este request para facilitar el seeder y tests.
 *
 * <p>Las specs son pares clave/valor (ej. "Peso" → "13 kg").
 * Si se omiten, el producto queda sin specs y pueden añadirse después.
 *
 * <p>Todos los mensajes de validación están en español.
 */
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

        /** Si es {@code null} o {@code true}, el producto se crea activo (visible en catálogo). */
        Boolean active,

        /**
         * URLs externas de imágenes (opcional).
         * Para subir archivos usar {@code POST /api/admin/products/{id}/images}.
         * El primer elemento de la lista tendrá {@code orderIndex = 0}.
         */
        List<String> imageUrls,

        /** Especificaciones técnicas opcionales. */
        @Valid
        List<SpecRequest> specs
) {

    /**
     * Sub-record para un par clave/valor de especificación técnica.
     * Ejemplo: {@code new SpecRequest("Peso", "13.5 kg")}.
     */
    public record SpecRequest(
            @NotBlank(message = "La clave de la especificación es obligatoria")
            @Size(max = 60, message = "La clave no puede superar 60 caracteres")
            String key,

            @NotBlank(message = "El valor de la especificación es obligatorio")
            @Size(max = 150, message = "El valor no puede superar 150 caracteres")
            String value
    ) {}
}
