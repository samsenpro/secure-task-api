package com.example.secureapi.task.dto;

import com.example.secureapi.task.entity.TaskPriority;
import com.example.secureapi.task.entity.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Reemplazo completo de la tarea (semántica PUT).
 */
public record UpdateTaskRequest(
        @Schema(example = "Preparar la demo")
        @NotBlank @Size(max = 150)
        String title,

        @Schema(example = "Revisar los endpoints antes de la reunión")
        @Size(max = 2000)
        String description,

        @Schema(example = "IN_PROGRESS")
        @NotNull
        TaskStatus status,

        @Schema(example = "HIGH")
        @NotNull
        TaskPriority priority
) {
}
