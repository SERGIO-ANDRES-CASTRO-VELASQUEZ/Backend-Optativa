package com.sports.backend.service;

import com.sports.backend.dto.AuthResponse;
import com.sports.backend.dto.ForgotPasswordRequest;
import com.sports.backend.dto.ForgotPasswordResponse;
import com.sports.backend.dto.LoginRequest;
import com.sports.backend.dto.RegisterRequest;
import com.sports.backend.dto.UserDto;
import com.sports.backend.exception.ApiException;
import com.sports.backend.model.Role;
import com.sports.backend.model.User;
import com.sports.backend.repository.UserRepository;
import com.sports.backend.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email    = req.email().trim().toLowerCase();
        String username = req.username().trim();

        if (userRepository.existsByEmail(email)) {
            throw ApiException.conflict("Ya existe una cuenta con ese email");
        }
        if (userRepository.existsByUsername(username)) {
            throw ApiException.conflict("Ese nombre de usuario ya esta en uso");
        }

        User user = User.builder()
                .fullName(req.fullName().trim())
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(Role.CLIENT)
                .active(true)
                .build();

        userRepository.save(user);
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, jwtService.getExpirationMs(), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = req.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Credenciales invalidas"));

        if (!user.isActive()) {
            throw ApiException.forbidden("La cuenta esta desactivada");
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales invalidas");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, jwtService.getExpirationMs(), UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest req) {
        String email  = req.email().trim().toLowerCase();
        boolean exists = userRepository.existsByEmail(email);
        if (exists) {
            return new ForgotPasswordResponse(true,
                    "Cuenta encontrada. Contacta al administrador para restablecer tu contrasena.");
        }
        return new ForgotPasswordResponse(false,
                "No encontramos ninguna cuenta asociada a ese email.");
    }
}
