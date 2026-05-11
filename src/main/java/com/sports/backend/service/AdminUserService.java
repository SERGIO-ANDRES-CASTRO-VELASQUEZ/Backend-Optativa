package com.sports.backend.service;

import com.sports.backend.dto.AdminUserDto;
import com.sports.backend.dto.CreateUserRequest;
import com.sports.backend.dto.UpdateUserRequest;
import com.sports.backend.exception.ApiException;
import com.sports.backend.model.Role;
import com.sports.backend.model.User;
import com.sports.backend.repository.RentalRepository;
import com.sports.backend.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de administración de usuarios.
 *
 * <p>Expone las operaciones del panel admin sobre usuarios:
 * listado paginado con búsqueda, detalle, creación, edición,
 * activar/desactivar y borrado (hard delete con guardas de negocio).
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository   userRepository;
    private final RentalRepository rentalRepository;
    private final PasswordEncoder  passwordEncoder;

    // =========================================================================
    // GET /api/admin/users — listado paginado con búsqueda
    // =========================================================================

    /**
     * Lista todos los usuarios con paginación y búsqueda opcional.
     *
     * @param q        texto libre que se busca en fullName, email y username
     * @param pageable paginación y ordenación
     * @return página de {@link AdminUserDto}
     */
    @Transactional(readOnly = true)
    public Page<AdminUserDto> listAll(String q, Pageable pageable) {
        Specification<User> spec = buildSpec(q);
        return userRepository.findAll(spec, pageable).map(AdminUserDto::from);
    }

    // =========================================================================
    // GET /api/admin/users/{id} — detalle
    // =========================================================================

    /**
     * Devuelve el detalle de un usuario por id.
     *
     * @throws ApiException 404 si no existe
     */
    @Transactional(readOnly = true)
    public AdminUserDto getById(Long id) {
        return AdminUserDto.from(findOrThrow(id));
    }

    // =========================================================================
    // POST /api/admin/users — crear usuario
    // =========================================================================

    /**
     * Crea un usuario desde el panel admin.
     *
     * <p>El admin puede asignar cualquier rol. Si no indica rol, se usa CLIENT.
     * Si no indica active, el usuario se crea activo.
     *
     * @throws ApiException 409 si el email o username ya existen
     */
    @Transactional
    public AdminUserDto create(CreateUserRequest req) {

        if (userRepository.existsByEmail(req.email().toLowerCase())) {
            throw ApiException.conflict("Ya existe un usuario con ese email");
        }
        if (userRepository.existsByUsername(req.username())) {
            throw ApiException.conflict("Ya existe un usuario con ese username");
        }

        User user = User.builder()
                .fullName(req.fullName())
                .username(req.username())
                .email(req.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(req.password()))
                .phone(req.phone())
                .idDocument(req.idDocument())
                .role(req.role() != null ? req.role() : Role.CLIENT)
                .active(req.active() == null || req.active())
                .build();

        return AdminUserDto.from(userRepository.save(user));
    }

    // =========================================================================
    // PUT /api/admin/users/{id} — editar (patch semántico)
    // =========================================================================

    /**
     * Edita un usuario; solo actualiza campos no-null del request.
     *
     * @throws ApiException 404 si no existe, 409 si email/username duplicados
     */
    @Transactional
    public AdminUserDto update(Long id, UpdateUserRequest req) {

        User user = findOrThrow(id);

        if (req.fullName() != null) {
            user.setFullName(req.fullName());
        }

        if (req.username() != null && !req.username().equals(user.getUsername())) {
            if (userRepository.existsByUsername(req.username())) {
                throw ApiException.conflict("Ya existe un usuario con ese username");
            }
            user.setUsername(req.username());
        }

        if (req.email() != null && !req.email().equalsIgnoreCase(user.getEmail())) {
            String newEmail = req.email().toLowerCase();
            if (userRepository.existsByEmail(newEmail)) {
                throw ApiException.conflict("Ya existe un usuario con ese email");
            }
            user.setEmail(newEmail);
        }

        if (req.newPassword() != null) {
            user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        }

        if (req.phone() != null) {
            user.setPhone(req.phone());
        }

        if (req.idDocument() != null) {
            user.setIdDocument(req.idDocument());
        }

        return AdminUserDto.from(userRepository.save(user));
    }

    // =========================================================================
    // PUT /api/admin/users/{id}/active — activar / desactivar
    // =========================================================================

    /**
     * Cambia el estado activo de un usuario sin tocar el resto de sus datos.
     *
     * @throws ApiException 404 si no existe
     */
    @Transactional
    public AdminUserDto toggleActive(Long id, boolean active) {
        User user = findOrThrow(id);
        user.setActive(active);
        return AdminUserDto.from(userRepository.save(user));
    }

    // =========================================================================
    // DELETE /api/admin/users/{id} — hard delete
    // =========================================================================

    /**
     * Elimina definitivamente un usuario de la BD.
     *
     * <p>Si el usuario tiene alquileres registrados, lanza 400 y sugiere
     * usar {@link #toggleActive(Long, boolean)} con {@code false} en su lugar.
     *
     * @throws ApiException 404 si no existe
     * @throws ApiException 400 si el usuario tiene alquileres
     */
    @Transactional
    public void delete(Long id) {
        findOrThrow(id); // valida que existe

        if (rentalRepository.existsByUserId(id)) {
            throw ApiException.badRequest(
                    "No se puede eliminar un usuario con alquileres registrados. " +
                    "Usa 'desactivar' (PUT /active con { \"active\": false }) en su lugar.");
        }

        userRepository.deleteById(id);
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Usuario no encontrado: id=" + id));
    }

    /**
     * Construye la Specification de búsqueda.
     * Si {@code q} es null o blank, devuelve todos los usuarios.
     */
    private Specification<User> buildSpec(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();

            String pattern = "%" + q.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(cb.lower(root.get("fullName")), pattern));
            predicates.add(cb.like(cb.lower(root.get("email")), pattern));
            predicates.add(cb.like(cb.lower(root.get("username")), pattern));
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
