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
