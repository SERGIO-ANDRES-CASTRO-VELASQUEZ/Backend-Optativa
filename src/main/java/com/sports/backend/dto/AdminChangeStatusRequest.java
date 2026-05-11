package com.sports.backend.dto;

import com.sports.backend.model.RentalStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Body de {@code PUT /api/admin/rentals/{id}/status}.
 *
 * <p>Permite al admin cambiar el estado de un alquiler a cualquier
 * valor del enum {@link RentalStatus} sin restricciones de flujo.
 *
 * <p>Ejemplo:
 * <pre>
 *   PUT /api/admin/rentals/12/status
 *   { "status": "ACTIVO" }
 * </pre>
 */
public record AdminChangeStatusRequest(

        @NotNull(message = "El campo 'status' es obligatorio")
        RentalStatus status
) {}
