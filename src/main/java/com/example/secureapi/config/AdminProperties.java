package com.example.secureapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Credenciales opcionales del administrador inicial (ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_NAME).
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password, String name) {

    public boolean isConfigured() {
        return email != null && !email.isBlank() && password != null && !password.isBlank();
    }

    @Override
    public String toString() {
        return "AdminProperties{configured=" + isConfigured() + "}";
    }
}
