package com.example.secureapi.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {

    public static AuthResponse bearer(String accessToken, long expiresInSeconds) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds);
    }

    @Override
    public String toString() {
        return "AuthResponse{tokenType=" + tokenType + ", expiresIn=" + expiresIn + "}";
    }
}
