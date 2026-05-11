package com.sports.backend.controller;

import com.sports.backend.dto.CreateRentalRequest;
import com.sports.backend.dto.ExtendRentalRequest;
import com.sports.backend.dto.RentalDto;
import com.sports.backend.dto.RentalSummaryDto;
import com.sports.backend.security.SecurityUtils;
import com.sports.backend.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador de alquileres del cliente.
 *
 * <p>Todos los endpoints requieren autenticación JWT. La verificación de
 * propiedad (que el alquiler pertenece al usuario que lo solicita) se realiza
 * en {@link RentalService}, no aquí.
 *
 * <p>Los endpoints de administración (listado global, cambio forzado de estado,
 * creación en mostrador) se añadirán en Fase 4 bajo {@code /api/admin/rentals}.
 *
 * <pre>
 *   POST   /api/rentals              → crear alquiler          (cliente)
 *   GET    /api/rentals/mine         → mis alquileres          (cliente)
 *   GET    /api/rentals/{id}         → detalle de un alquiler  (cliente)
 *   POST   /api/rentals/{id}/extend  → extender devolución     (cliente)
 *   POST   /api/rentals/{id}/cancel  → cancelar (solo PENDIENTE)(cliente)
 * </pre>
 */
@RestController
@RequestMapping("/api/rentals")
@RequiredArgsConstructor
@Tag(name = "Alquileres", description = "Gestión de alquileres del cliente")
@SecurityRequirement(name = "bearerAuth")
public class RentalController {

    private final RentalService  rentalService;
    private final SecurityUtils  securityUtils;

    // -------------------------------------------------------------------------
    // POST /api/rentals  — crear alquiler
    // -------------------------------------------------------------------------

    /**
     * Crea un nuevo alquiler para el usuario autenticado.
     *
     * <p>El sistema valida disponibilidad de stock para el rango de fechas
     * antes de persistir. El pago es simulado (solo se guarda el método).
     *
     * @return 201 Created con el {@link RentalDto} completo (sirve como ticket).
     */
    @PostMapping
    @Operation(
            summary = "Crear alquiler",
            description = "Crea un alquiler para el usuario autenticado. " +
                          "Valida stock en el rango de fechas y genera el código SR-YYYY-NNNNN."
    )
    public ResponseEntity<RentalDto> create(
            @Valid @RequestBody CreateRentalRequest req,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = securityUtils.requireUserId(userDetails);
        RentalDto created = rentalService.create(userId, req);
        return ResponseEntity.status(201).body(created);
    }

    // -------------------------------------------------------------------------
    // GET /api/rentals/mine  — mis alquileres
    // -------------------------------------------------------------------------

    /**
     * Lista todos los alquileres del usuario autenticado, del más reciente
     * al más antiguo.
     *
     * <p>Devuelve {@link RentalSummaryDto} (sin lista de ítems detallada)
     * para no sobrecargar el listado. Para el detalle completo usar
     * {@code GET /api/rentals/{id}}.
     *
     * @return 200 OK con la lista de resúmenes (puede ser vacía).
     */
    @GetMapping("/mine")
    @Operation(
            summary = "Mis alquileres",
            description = "Lista todos los alquileres del usuario autenticado (más reciente primero). " +
                          "Devuelve RentalSummaryDto — para el detalle completo usar GET /api/rentals/{id}."
    )
    public ResponseEntity<List<RentalSummaryDto>> mine(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = securityUtils.requireUserId(userDetails);
        return ResponseEntity.ok(rentalService.findMine(userId));
    }

    // -------------------------------------------------------------------------
    // GET /api/rentals/{id}  — detalle de un alquiler
    // -------------------------------------------------------------------------

    /**
     * Devuelve el detalle completo de un alquiler, incluyendo todos sus ítems.
     *
     * <p>Si el alquiler no existe o no pertenece al usuario autenticado,
     * se devuelve 404 (no 403) para no revelar la existencia del recurso.
     *
     * @param id ID del alquiler.
     * @return 200 OK con el {@link RentalDto} completo.
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Detalle de un alquiler",
            description = "Devuelve el alquiler con todos sus ítems. 404 si no existe o no pertenece al usuario."
    )
    public ResponseEntity<RentalDto> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = securityUtils.requireUserId(userDetails);
        return ResponseEntity.ok(rentalService.findById(userId, id));
    }

    // -------------------------------------------------------------------------
    // POST /api/rentals/{id}/extend  — extender devolución
    // -------------------------------------------------------------------------

    /**
     * Extiende la fecha de devolución de un alquiler PENDIENTE o ACTIVO.
     *
     * <p>Valida disponibilidad de stock en el período de extensión
     * (desde el día siguiente al fin actual hasta la nueva fecha).
     * Recalcula automáticamente los totales.
     *
     * @param id  ID del alquiler.
     * @param req Nueva fecha de fin.
     * @return 200 OK con el {@link RentalDto} actualizado.
     */
    @PostMapping("/{id}/extend")
    @Operation(
            summary = "Extender fecha de devolución",
            description = "Extiende un alquiler PENDIENTE o ACTIVO. " +
                          "Valida stock para el período extra y recalcula los totales."
    )
    public ResponseEntity<RentalDto> extend(
            @PathVariable Long id,
            @Valid @RequestBody ExtendRentalRequest req,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = securityUtils.requireUserId(userDetails);
        return ResponseEntity.ok(rentalService.extend(userId, id, req));
    }

    // -------------------------------------------------------------------------
    // POST /api/rentals/{id}/cancel  — cancelar alquiler
    // -------------------------------------------------------------------------

    /**
     * Cancela un alquiler en estado PENDIENTE.
     *
     * <p>No se puede cancelar un alquiler en cualquier otro estado.
     * Para cancelar un alquiler ACTIVO, el cliente debe contactar
     * directamente con la tienda (flujo de Fase 4 — admin).
     *
     * @param id ID del alquiler.
     * @return 204 No Content si se canceló correctamente.
     */
    @PostMapping("/{id}/cancel")
    @Operation(
            summary = "Cancelar alquiler",
            description = "Cancela un alquiler PENDIENTE. No se puede cancelar si ya está ACTIVO, " +
                          "FINALIZADO, CANCELADO o VENCIDO."
    )
    public ResponseEntity<Void> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = securityUtils.requireUserId(userDetails);
        rentalService.cancel(userId, id);
        return ResponseEntity.noContent().build();
    }
}
