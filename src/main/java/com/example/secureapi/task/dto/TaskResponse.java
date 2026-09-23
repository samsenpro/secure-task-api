package com.example.secureapi.task.dto;

import com.example.secureapi.task.entity.Task;
import com.example.secureapi.task.entity.TaskPriority;
import com.example.secureapi.task.entity.TaskStatus;

import java.time.Instant;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        Long userId,
        Instant createdAt,
        Instant updatedAt
) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getUser().getId(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
