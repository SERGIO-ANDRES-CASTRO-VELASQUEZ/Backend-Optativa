package com.sports.backend.controller;

import com.sports.backend.dto.AdminChangeStatusRequest;
import com.sports.backend.dto.AdminRentalSummaryDto;
import com.sports.backend.dto.CreateRentalRequest;
import com.sports.backend.dto.RentalDto;
import com.sports.backend.model.RentalStatus;
import com.sports.backend.security.SecurityUtils;
import com.sports.backend.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador del panel admin para gestión de alquileres.
 *
 * <p>Todos los endpoints requieren rol {@code ADMIN}.
 *
 * <pre>
 *   GET  /api/admin/rentals             → findAllAdmin  (?status=, ?q=, paginado)
 *   GET  /api/admin/rentals/{id}        → detalle completo sin verificar propiedad
 *   POST /api/admin/rentals             → createForClient  (crear desde mostrador)
 *   PUT  /api/admin/rentals/{id}/status → forceChangeStatus
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/rentals")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin – Alquileres", description = "Gestión global de alquileres para el panel admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminRentalController {

    private final RentalService  rentalService;
    private final SecurityUtils  securityUtils;

    // =========================================================================
    // GET /api/admin/rentals  — listado global
    // =========================================================================

    @GetMapping
    @Operation(
            summary = "Listar todos los alquileres (admin)",
            description = "Filtros opcionales: status (enum RentalStatus) y q (búsqueda en código, email y nombre del cliente)."
    )
    public ResponseEntity<Page<AdminRentalSummaryDto>> listAll(
            @RequestParam(required = false) RentalStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(rentalService.findAllAdmin(status, q, pageable));
    }

    // =========================================================================
    // GET /api/admin/rentals/{id}  — detalle
    // =========================================================================

    @GetMapping("/{id}")
    @Operation(
            summary = "Detalle de un alquiler (admin)",
            description = "Devuelve el alquiler completo con todos sus ítems. No verifica propiedad."
    )
    public ResponseEntity<RentalDto> detail(@PathVariable Long id) {
        return ResponseEntity.ok(rentalService.findByIdAdmin(id));
    }

    // =========================================================================
    // POST /api/admin/rentals  — crear desde mostrador
    // =========================================================================

    @PostMapping
    @Operation(
            summary = "Crear alquiler desde mostrador (admin)",
            description = "El admin crea el alquiler en nombre de un cliente. " +
                          "Requiere 'clientId' en el body. El alquiler queda registrado con createdBy = admin."
    )
    public ResponseEntity<RentalDto> createForClient(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long clientId,
            @Valid @RequestBody CreateRentalRequest request
    ) {
        Long adminId = securityUtils.requireUserId(userDetails);
        return ResponseEntity.status(201).body(
                rentalService.createForClient(adminId, clientId, request));
    }

    // =========================================================================
    // PUT /api/admin/rentals/{id}/status  — cambio forzado de estado
    // =========================================================================

    @PutMapping("/{id}/status")
    @Operation(
            summary = "Cambiar estado de alquiler (admin)",
            description = "Permite al admin cambiar el estado a cualquier valor de RentalStatus " +
                          "sin restricciones de flujo (ej. marcar ACTIVO o FINALIZADO manualmente)."
    )
    public ResponseEntity<RentalDto> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminChangeStatusRequest request
    ) {
        return ResponseEntity.ok(rentalService.forceChangeStatus(id, request.status()));
    }
}
