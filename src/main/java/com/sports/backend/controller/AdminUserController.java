package com.sports.backend.controller;

import com.sports.backend.dto.AdminUserDto;
import com.sports.backend.dto.CreateUserRequest;
import com.sports.backend.dto.ToggleActiveRequest;
import com.sports.backend.dto.UpdateUserRequest;
import com.sports.backend.service.AdminUserService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador del panel de administración para la gestión de usuarios.
 *
 * <p>Todos los endpoints requieren rol {@code ADMIN}.
 *
 * <pre>
 *   GET    /api/admin/users              → listAll       (?q= búsqueda, paginado)
 *   GET    /api/admin/users/{id}         → getById
 *   POST   /api/admin/users              → create
 *   PUT    /api/admin/users/{id}         → update        (patch semántico)
 *   PUT    /api/admin/users/{id}/active  → toggleActive  (body: ToggleActiveRequest)
 *   DELETE /api/admin/users/{id}         → delete        (hard delete con guardia)
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin – Usuarios", description = "CRUD de usuarios para el panel de administración")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    // =========================================================================
    // GET /api/admin/users  — listado paginado
    // =========================================================================

    @GetMapping
    @Operation(
            summary = "Listar usuarios (admin)",
            description = "Lista todos los usuarios. Búsqueda opcional (?q=) en nombre, email y username."
    )
    public ResponseEntity<Page<AdminUserDto>> listAll(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        return ResponseEntity.ok(adminUserService.listAll(q, pageable));
    }

    // =========================================================================
    // GET /api/admin/users/{id}  — detalle
    // =========================================================================

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un usuario (admin)")
    public ResponseEntity<AdminUserDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getById(id));
    }

    // =========================================================================
    // POST /api/admin/users  — crear usuario
    // =========================================================================

    @PostMapping
    @Operation(
            summary = "Crear usuario (admin)",
            description = "Crea un usuario con rol, teléfono y cédula. " +
                          "Si no se indica rol, se asigna CLIENT. Si no se indica active, se crea activo."
    )
    public ResponseEntity<AdminUserDto> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(201).body(adminUserService.create(request));
    }

    // =========================================================================
    // PUT /api/admin/users/{id}  — editar (patch)
    // =========================================================================

    @PutMapping("/{id}")
    @Operation(
            summary = "Editar usuario (admin)",
            description = "Actualiza solo los campos no-null del body (patch semántico). " +
                          "Para activar/desactivar usar PUT /{id}/active."
    )
    public ResponseEntity<AdminUserDto> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(adminUserService.update(id, request));
    }

    // =========================================================================
    // PUT /api/admin/users/{id}/active  — activar / desactivar
    // =========================================================================

    @PutMapping("/{id}/active")
    @Operation(
            summary = "Activar o desactivar usuario",
            description = "Cambia el estado activo del usuario sin modificar otros datos. " +
                          "Un usuario inactivo no puede hacer login."
    )
    public ResponseEntity<AdminUserDto> toggleActive(
            @PathVariable Long id,
            @Valid @RequestBody ToggleActiveRequest request
    ) {
        return ResponseEntity.ok(adminUserService.toggleActive(id, request.active()));
    }

    // =========================================================================
    // DELETE /api/admin/users/{id}  — hard delete
    // =========================================================================

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar usuario (admin)",
            description = "Elimina definitivamente al usuario. " +
                          "Falla con 400 si tiene alquileres; en ese caso usa PUT /{id}/active con false."
    )
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
