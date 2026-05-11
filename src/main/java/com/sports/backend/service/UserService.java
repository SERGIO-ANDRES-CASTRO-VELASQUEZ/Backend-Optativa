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

    /**
     * Devuelve el perfil del usuario autenticado.
     *
     * <p>En Fase 4 se puede ampliar el DTO para incluir también
     * {@code phone} e {@code idDocument}. Por ahora se devuelve {@link UserDto}
     * que incluye los campos más relevantes (id, fullName, username, email, role).
     *
     * @param userId ID del usuario autenticado.
     * @return UserDto con los datos del perfil.
     */
    @Transactional(readOnly = true)
    public UserDto getProfile(Long userId) {
        return userRepository.findById(userId)
                .map(UserDto::from)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    // =========================================================================
    // PUT /api/me  — actualizar perfil propio
    // =========================================================================

    /**
     * Actualiza los campos editables del perfil del usuario autenticado.
     *
     * <p>Campos actualizables en Fase 3: {@code fullName}, {@code phone},
     * {@code idDocument}. Email y username son inmutables desde el perfil
     * del cliente (el admin podrá modificarlos en Fase 4).
     *
     * <p>El método guarda los cambios y devuelve el perfil actualizado.
     * La respuesta usa {@link UserDto}; si en fases posteriores se necesita
     * devolver también {@code phone} e {@code idDocument}, crear
     * {@code UserProfileDto} y reemplazar la firma de retorno.
     *
     * @param userId ID del usuario autenticado.
     * @param req    Campos a actualizar.
     * @return UserDto actualizado.
     */
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
    //
    //   Page<UserDto> listAll(String q, Pageable pageable)
    //     → listado de usuarios para el panel admin
    //
    //   UserDto getById(Long id)
    //     → detalle de cualquier usuario para el admin
    //
    //   UserDto changeRole(Long id, Role newRole)
    //     → cambiar rol de un usuario
    //
    //   void deactivate(Long id) / void activate(Long id)
    //     → habilitar / deshabilitar cuenta
}
