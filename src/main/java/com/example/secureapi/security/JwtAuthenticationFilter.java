package com.example.secureapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Autentica la petición a partir del header {@code Authorization: Bearer <token>}.
 * <p>
 * Si el token falta o no es válido, la petición continúa sin autenticación y es
 * Spring Security quien responde 401 en los endpoints protegidos.
 * <p>
 * No se registra como {@code @Component} para que no se añada también a la
 * cadena de filtros del servlet; se instancia en {@link SecurityConfig}.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. Obtener el token del header Authorization
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();

        // 2 y 3. Validar el token y obtener el email
        jwtService.extractValidSubject(token).ifPresent(email -> authenticate(email, request));

        chain.doFilter(request, response);
    }

    private void authenticate(String email, HttpServletRequest request) {
        try {
            // 4. Cargar el usuario (se consulta la BD para reflejar bloqueos al instante)
            UserDetails user = userDetailsService.loadUserByUsername(email);
            if (!user.isEnabled()) {
                log.debug("Rejected JWT for disabled account");
                return;
            }

            // 5. Establecer el SecurityContext
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (UsernameNotFoundException ex) {
            log.debug("Rejected JWT for a user that no longer exists");
        }
    }
}
