package com.sports.backend.controller;

import com.sports.backend.dto.ProductSummaryDto;
import com.sports.backend.exception.ApiException;
import com.sports.backend.repository.UserRepository;
import com.sports.backend.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Favoritos", description = "Marcar/desmarcar productos favoritos del cliente")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserRepository userRepository;

    public FavoriteController(FavoriteService favoriteService, UserRepository userRepository) {
        this.favoriteService = favoriteService;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------------------
    // POST /api/products/{id}/favorite  — marcar
    // -------------------------------------------------------------------------

    @PostMapping("/api/products/{id}/favorite")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Marcar producto como favorito", description = "Solo CLIENT. Si ya está marcado devuelve 409.")
    public ResponseEntity<Void> add(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        favoriteService.addFavorite(resolveUserId(userDetails), id);
        return ResponseEntity.status(201).build();
    }

    // -------------------------------------------------------------------------
    // DELETE /api/products/{id}/favorite  — desmarcar
    // -------------------------------------------------------------------------

    @DeleteMapping("/api/products/{id}/favorite")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Desmarcar producto favorito", description = "Solo CLIENT. Si no estaba marcado devuelve 404.")
    public ResponseEntity<Void> remove(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        favoriteService.removeFavorite(resolveUserId(userDetails), id);
        return ResponseEntity.noContent().build();
    }

    // -------------------------------------------------------------------------
    // GET /api/me/favorites  — listar mis favoritos
    // -------------------------------------------------------------------------

    @GetMapping("/api/me/favorites")
    @Operation(summary = "Listar mis productos favoritos", description = "Cualquier usuario autenticado.")
    public ResponseEntity<List<ProductSummaryDto>> myFavorites(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(favoriteService.getMyFavorites(resolveUserId(userDetails)));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private Long resolveUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(u -> u.getId())
                .orElseThrow(() -> ApiException.unauthorized("Usuario no encontrado"));
    }
}
