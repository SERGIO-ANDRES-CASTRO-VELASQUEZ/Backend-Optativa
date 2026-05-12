package com.sports.backend.controller;

import com.sports.backend.dto.DashboardDto;
import com.sports.backend.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin – Dashboard", description = "KPIs del panel de administración")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @Operation(
            summary = "KPIs del dashboard (admin)",
            description = "Devuelve: alquileres activos hoy, vencidos, finalizados este mes, " +
                          "ingresos del mes, top productos alquilados y favoritos, " +
                          "nuevos usuarios del mes y ocupación por categoría."
    )
    public ResponseEntity<DashboardDto> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
