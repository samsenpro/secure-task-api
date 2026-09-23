package com.example.secureapi.auth.service;

import com.example.secureapi.auth.dto.AuthResponse;
import com.example.secureapi.auth.dto.LoginRequest;
import com.example.secureapi.auth.dto.RegisterRequest;
import com.example.secureapi.common.Emails;
import com.example.secureapi.exception.ConflictException;
import com.example.secureapi.security.JwtService;
import com.example.secureapi.security.UserPrincipal;
import com.example.secureapi.user.dto.UserResponse;
import com.example.secureapi.user.entity.Role;
import com.example.secureapi.user.entity.User;
import com.example.secureapi.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = Emails.normalize(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        User user = new User(request.name().trim(), email, passwordEncoder.encode(request.password()), Role.USER);
        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Dos registros simultáneos con el mismo email: la restricción única de la BD decide.
            throw new ConflictException("Email is already registered");
        }
        log.info("User registered with id={}", saved.getId());
        return UserResponse.from(saved);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            Emails.normalize(request.email()), request.password()));
        } catch (AuthenticationException ex) {
            // Email inexistente, contraseña incorrecta o cuenta bloqueada producen la misma
            // respuesta para no permitir la enumeración de usuarios.
            log.info("Failed login attempt ({})", ex.getClass().getSimpleName());
            throw new BadCredentialsException("Invalid email or password");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        log.info("User id={} logged in", principal.id());
        return AuthResponse.bearer(jwtService.generateToken(principal), jwtService.getExpirationSeconds());
    }
}
