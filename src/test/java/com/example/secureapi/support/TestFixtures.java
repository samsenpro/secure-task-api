package com.example.secureapi.support;

import com.example.secureapi.task.entity.Task;
import com.example.secureapi.task.entity.TaskPriority;
import com.example.secureapi.task.entity.TaskStatus;
import com.example.secureapi.user.entity.Role;
import com.example.secureapi.user.entity.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

/**
 * Crea entidades con id y timestamps, que en ejecución real asigna JPA.
 */
public final class TestFixtures {

    private TestFixtures() {
    }

    public static User user(Long id, String email, Role role) {
        User user = new User("Test User", email, "$2a$10$encoded", role);
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(user, "updatedAt", Instant.parse("2026-01-01T00:00:00Z"));
        return user;
    }

    public static Task task(Long id, User owner) {
        Task task = new Task("Title", "Description", TaskStatus.PENDING, TaskPriority.MEDIUM, owner);
        ReflectionTestUtils.setField(task, "id", id);
        ReflectionTestUtils.setField(task, "createdAt", Instant.parse("2026-01-01T00:00:00Z"));
        ReflectionTestUtils.setField(task, "updatedAt", Instant.parse("2026-01-01T00:00:00Z"));
        return task;
    }
}
