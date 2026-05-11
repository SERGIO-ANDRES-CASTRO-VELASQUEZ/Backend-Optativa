package com.sports.backend.security;

import com.sports.backend.exception.ApiException;
import com.sports.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Helper compartido para extraer el ID del usuario autenticado a partir del
 * UserDetails que inyecta Spring Security.
 *
 * El JwtAuthFilter carga el UserDetails usando el EMAIL como username,
 * por eso se busca en BD por email.
 *
 * Reemplaza el método privado resolveUserId() que estaba duplicado en los
 * controladores de Fase 2 (FavoriteController, ProductController).
 * En Fase 3 y 4, todos los controladores que necesitan el userId usan este bean.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Devuelve el ID del usuario autenticado.
     * Lanza 401 si userDetails es null.
     * Lanza 404 si el email del token no existe en BD (no debería ocurrir con JWT válido).
     */
    public Long requireUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw ApiException.unauthorized("No autenticado");
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .map(u -> u.getId())
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    /**
     * Igual que requireUserId pero devuelve null si no hay sesión activa.
     * Se usa en endpoints públicos que se comportan diferente según si hay token
     * (ej. GET /api/products/{id} → isFavorite depende del usuario).
     */
    public Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userRepository.findByEmail(userDetails.getUsername())
                .map(u -> u.getId())
                .orElse(null);
    }
}
