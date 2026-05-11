package com.sports.backend.controller;

import com.sports.backend.dto.UpdateProfileRequest;
import com.sports.backend.dto.UserDto;
import com.sports.backend.security.SecurityUtils;
import com.sports.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador del perfil del usuario autenticado.
 *
 * <p>Todos los endpoints requieren autenticación JWT. Cualquier rol
 * (CLIENT, ADMIN) puede acceder a su propio perfil.
 *
 * <pre>
 *   GET  /api/me  → ver perfil propio
 *   PUT  /api/me  → actualizar fullName, phone, idDocument
 * </pre>
 *
 * <p>El endpoint {@code GET /api/me/favorites} (Fase 2) está en
 * {@link FavoriteController} para mantener la separación de responsabilidades.
 *
 * <p>En Fase 4 se añadirán endpoints de administración de usuarios bajo
 * {@code /api/admin/users}.
 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "Perfil", description = "Ver y editar el perfil del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService   userService;
    private final SecurityUtils securityUtils;

    // -------------------------------------------------------------------------
    // GET /api/me  — ver perfil propio
    // -------------------------------------------------------------------------

    /**
     * Devuelve los datos del perfil del usuario autenticado.
     *
     * <p>Respuesta: {@link UserDto} (id, fullName, username, email, role).
     * En Fase 4 se puede ampliar para incluir phone e idDocument
     * creando un {@code UserProfileDto} dedicado.
     *
     * @return 200 OK con el UserDto del usuario autenticado.
     */
    @GetMapping
    @Operation(
            summary = "Ver mi perfil",
            description = "Devuelve id, fullName, username, email y role del usuario autenticado."
    )
    public ResponseEntity<UserDto> me(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = securityUtils.requireUserId(userDetails);
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    // -------------------------------------------------------------------------
    // PUT /api/me  — actualizar perfil propio
    // -------------------------------------------------------------------------

    /**
     * Actualiza los campos editables del perfil del usuario autenticado.
     *
     * <p>Campos actualizables: {@code fullName}, {@code phone}, {@code idDocument}.
     * Email y username son inmutables desde el perfil del cliente.
     *
     * @param req Campos a actualizar (fullName obligatorio; phone e idDocument opcionales).
     * @return 200 OK con el {@link UserDto} actualizado.
     */
    @PutMapping
    @Operation(
            summary = "Actualizar mi perfil",
            description = "Actualiza fullName, phone e idDocument. " +
                          "Email y username son inmutables desde el perfil del cliente."
    )
    public ResponseEntity<UserDto> update(
            @Valid @RequestBody UpdateProfileRequest req,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long userId = securityUtils.requireUserId(userDetails);
        return ResponseEntity.ok(userService.updateProfile(userId, req));
    }
}
