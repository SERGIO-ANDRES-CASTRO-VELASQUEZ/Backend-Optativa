package com.sports.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Body de {@code PUT /api/admin/products/{id}} — editar un producto existente.
 *
 * <p>Todos los campos son opcionales: {@code null} significa "no cambiar".
 * Solo se actualiza lo que explícitamente se envía con valor no-null.
 *
 * <p>Para gestión de imágenes usar los endpoints específicos:
 * <ul>
 *   <li>{@code POST   /api/admin/products/{id}/images} — añadir imagen multipart</li>
 *   <li>{@code DELETE /api/admin/products/{id}/images/{imageId}} — eliminar imagen</li>
 * </ul>
 *
 * <p>Para gestionar specs, este request acepta la lista completa de specs.
 * Si se envía la lista (aunque sea vacía), reemplaza todas las specs existentes.
 * Si se envía {@code null}, las specs no se tocan.
 */
public record UpdateProductRequest(

        @Size(min = 1, max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,

        String description,

        Long categoryId,

        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que 0")
        BigDecimal pricePerDay,

        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        /** {@code true} = visible en catálogo; {@code false} = oculto (soft delete). */
        Boolean active,

        /**
         * Lista completa de specs. Si se envía (incluso vacía), reemplaza todas.
         * Si se envía {@code null}, no se modifican las specs actuales.
         */
        @Valid
        List<CreateProductRequest.SpecRequest> specs
) {}
