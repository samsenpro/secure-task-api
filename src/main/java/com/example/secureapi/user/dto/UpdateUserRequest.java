package com.example.secureapi.user.dto;

import com.example.secureapi.common.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Actualización del perfil propio. El email es la identidad del token y no se modifica aquí.
 * Para cambiar la contraseña hay que enviar {@code currentPassword} y {@code newPassword}.
 */
public record UpdateUserRequest(
        @Schema(example = "Jane Smith")
        @NotBlank @Size(max = 100)
        String name,

        @Schema(description = "Obligatorio solo si se envía newPassword")
        @Size(max = 128)
        String currentPassword,

        @Schema(example = "NewPassword123!")
        @Size(max = PasswordPolicy.MAX_LENGTH)
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
        String newPassword
) {

    public boolean wantsPasswordChange() {
        return newPassword != null;
    }

    @Override
    public String toString() {
        return "UpdateUserRequest{name=" + name + ", passwordChange=" + wantsPasswordChange() + "}";
    }
}
