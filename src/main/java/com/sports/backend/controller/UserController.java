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
