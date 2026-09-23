package com.example.secureapi.security;

import com.example.secureapi.user.entity.Role;
import com.example.secureapi.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Usuario autenticado. Inmutable y desacoplado de la entidad JPA para no
 * arrastrar el contexto de persistencia al {@code SecurityContext}.
 */
public record UserPrincipal(
        Long id,
        String email,
        String passwordHash,
        Role role,
        boolean enabled
) implements UserDetails {

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPassword(), user.getRole(), user.isEnabled());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return "UserPrincipal{id=" + id + ", role=" + role + ", enabled=" + enabled + "}";
    }
}
