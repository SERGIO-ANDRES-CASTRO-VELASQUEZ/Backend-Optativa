package com.sports.backend.service;

import com.sports.backend.dto.UpdateProfileRequest;
import com.sports.backend.dto.UserDto;
import com.sports.backend.exception.ApiException;
import com.sports.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // =========================================================================
    // GET /api/me  — ver perfil propio
    // =========================================================================

    @Transactional(readOnly = true)
    public UserDto getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(UserDto::from)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    // =========================================================================
    // PUT /api/me  — actualizar perfil propio
    // =========================================================================

    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest req) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));

        user.setFullName(req.fullName());
        user.setPhone(req.phone());
        user.setIdDocument(req.idDocument());

        userRepository.save(user);
        return UserDto.from(user);
    }

    // =========================================================================
    // Métodos reservados para Fase 4 (panel admin)
    // =========================================================================

}
