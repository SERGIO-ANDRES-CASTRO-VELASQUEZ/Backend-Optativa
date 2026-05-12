package com.sports.backend.security;

import com.sports.backend.exception.ApiException;
import com.sports.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public Long requireUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw ApiException.unauthorized("No autenticado");
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .map(u -> u.getId())
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    public Long resolveUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        return userRepository.findByEmail(userDetails.getUsername())
                .map(u -> u.getId())
                .orElse(null);
    }
}
