package com.example.secureapi.security;

import com.example.secureapi.user.entity.Role;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-bytes!!";
    private static final Instant NOW = Instant.parse("2026-09-22T20:00:00Z");
    private static final UserPrincipal PRINCIPAL =
            new UserPrincipal(1L, "user@example.com", "hash", Role.USER, true);

    private final JwtProperties properties = new JwtProperties(SECRET, 3600, "secure-task-api");
    private final JwtService jwtService = new JwtService(properties, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void generatedTokenIsValidAndCarriesTheEmailAsSubject() {
        String token = jwtService.generateToken(PRINCIPAL);

        assertThat(jwtService.extractValidSubject(token)).contains("user@example.com");
    }

    @Test
    void exposesConfiguredExpiration() {
        assertThat(jwtService.getExpirationSeconds()).isEqualTo(3600);
    }

    @Test
    void rejectsExpiredToken() {
        String token = jwtService.generateToken(PRINCIPAL);
        Clock later = Clock.fixed(NOW.plus(Duration.ofSeconds(3601)), ZoneOffset.UTC);
        JwtService serviceInTheFuture = new JwtService(properties, later);

        assertThat(serviceInTheFuture.extractValidSubject(token)).isEmpty();
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateToken(PRINCIPAL);
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + "x." + parts[2];

        assertThat(jwtService.extractValidSubject(tampered)).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtService otherService = new JwtService(
                new JwtProperties("another-secret-key-with-at-least-32-bytes", 3600, "secure-task-api"),
                Clock.fixed(NOW, ZoneOffset.UTC));
        String foreignToken = otherService.generateToken(PRINCIPAL);

        assertThat(jwtService.extractValidSubject(foreignToken)).isEmpty();
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        JwtService otherIssuer = new JwtService(
                new JwtProperties(SECRET, 3600, "another-issuer"), Clock.fixed(NOW, ZoneOffset.UTC));
        String token = otherIssuer.generateToken(PRINCIPAL);

        assertThat(jwtService.extractValidSubject(token)).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(jwtService.extractValidSubject("not-a-jwt")).isEmpty();
        assertThat(jwtService.extractValidSubject("")).isEmpty();
    }

    @Test
    void refusesToStartWithAShortSecret() {
        JwtProperties weak = new JwtProperties("too-short", 3600, "secure-task-api");

        assertThatThrownBy(() -> new JwtService(weak, Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }
}
