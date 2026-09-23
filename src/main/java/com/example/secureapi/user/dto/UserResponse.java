package com.example.secureapi.user.dto;

import com.example.secureapi.user.entity.Role;
import com.example.secureapi.user.entity.User;

import java.time.Instant;

/**
 * Representación pública del usuario. No incluye el password bajo ningún concepto.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
