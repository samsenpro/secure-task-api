package com.example.secureapi.auth.service;

import com.example.secureapi.auth.dto.AuthResponse;
import com.example.secureapi.auth.dto.LoginRequest;
import com.example.secureapi.auth.dto.RegisterRequest;
import com.example.secureapi.exception.ConflictException;
import com.example.secureapi.security.JwtService;
import com.example.secureapi.security.UserPrincipal;
import com.example.secureapi.user.dto.UserResponse;
import com.example.secureapi.user.entity.Role;
import com.example.secureapi.user.entity.User;
import com.example.secureapi.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.example.secureapi.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerNormalizesEmailHashesPasswordAndAssignsUserRole() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        UserResponse response = authService.register(
                new RegisterRequest("  Jane  ", "  Jane@Example.COM ", "Password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getName()).isEqualTo("Jane");
        assertThat(saved.getPassword()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(response.email()).isEqualTo("jane@example.com");
        assertThat(response.role()).isEqualTo(Role.USER);
    }

    @Test
    void registerRejectsDuplicatedEmail() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jane", "jane@example.com", "Password123")))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerTranslatesConcurrentDuplicateIntoConflict() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jane", "jane@example.com", "Password123")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void loginReturnsBearerToken() {
        UserPrincipal principal = new UserPrincipal(1L, "jane@example.com", "hashed", Role.USER, true);
        Authentication authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest("Jane@Example.com", "Password123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        ArgumentCaptor<Authentication> captor = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("jane@example.com");
    }

    @Test
    void loginFailureAlwaysProducesTheSameGenericError() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("User is disabled"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("jane@example.com", "Password123")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
        verify(jwtService, never()).generateToken(any());
    }

    private static User withId(User user) {
        User copy = user(10L, user.getEmail(), user.getRole());
        copy.rename(user.getName());
        copy.changePassword(user.getPassword());
        return copy;
    }
}
