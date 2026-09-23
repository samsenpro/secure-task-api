package com.example.secureapi.task.dto;

import com.example.secureapi.task.entity.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @Schema(example = "Preparar la demo")
        @NotBlank @Size(max = 150)
        String title,

        @Schema(example = "Revisar los endpoints antes de la reunión")
        @Size(max = 2000)
        String description,

        @Schema(description = "Por defecto MEDIUM", example = "HIGH")
        TaskPriority priority
) {
}
