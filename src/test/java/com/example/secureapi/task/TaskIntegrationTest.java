package com.example.secureapi.task;

import com.example.secureapi.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createTaskAppliesDefaultsAndReturnsLocation() throws Exception {
        TestUser user = newUser();

        mockMvc.perform(post("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("title", "Write report"))))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, matchesPattern(".*/api/v1/tasks/\\d+$")))
                .andExpect(jsonPath("$.title").value("Write report"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.priority").value("MEDIUM"))
                .andExpect(jsonPath("$.userId").value(user.id()));
    }

    @Test
    void listReturnsOnlyTheUsersOwnTasks() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        createTask(alice.token(), "Alice 1");
        createTask(alice.token(), "Alice 2");
        createTask(bob.token(), "Bob 1");

        mockMvc.perform(get("/api/v1/tasks").header(HttpHeaders.AUTHORIZATION, bearer(alice.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Alice 2"))
                .andExpect(jsonPath("$.content[1].title").value("Alice 1"));
    }

    @Test
    void listIsPaginated() throws Exception {
        TestUser user = newUser();
        for (int i = 0; i < 3; i++) {
            createTask(user.token(), "Task " + i);
        }

        mockMvc.perform(get("/api/v1/tasks").param("page", "1").param("size", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void invalidPageSizeIsRejected() throws Exception {
        TestUser user = newUser();

        mockMvc.perform(get("/api/v1/tasks").param("size", "1000")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void getUpdateAndDeleteOwnTask() throws Exception {
        TestUser user = newUser();
        long taskId = createTask(user.token(), "Initial");

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId).header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Initial"))
                .andExpect(jsonPath("$.priority").value("HIGH"));

        mockMvc.perform(put("/api/v1/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "title", "Updated",
                                "description", "Done",
                                "status", "COMPLETED",
                                "priority", "LOW"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.priority").value("LOW"));

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId).header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId).header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Task not found"))
                .andExpect(jsonPath("$.path").value("/api/v1/tasks/" + taskId))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void updateWithInvalidEnumValueIsRejected() throws Exception {
        TestUser user = newUser();
        long taskId = createTask(user.token(), "Task");

        mockMvc.perform(put("/api/v1/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("title", "T", "status", "UNKNOWN", "priority", "LOW"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWithMissingFieldsReturnsValidationErrors() throws Exception {
        TestUser user = newUser();
        long taskId = createTask(user.token(), "Task");

        mockMvc.perform(put("/api/v1/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("title", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    void userCanReadAndUpdateTheirProfile() throws Exception {
        TestUser user = newUser();

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.email()))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "name", "Renamed",
                                "currentPassword", DEFAULT_PASSWORD,
                                "newPassword", "NewPassword456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));

        login(user.email(), "NewPassword456");
    }

    @Test
    void wrongCurrentPasswordIsUnprocessable() throws Exception {
        TestUser user = newUser();

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(user.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of(
                                "name", "Renamed",
                                "currentPassword", "WrongPassword1",
                                "newPassword", "NewPassword456"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("UNPROCESSABLE_ENTITY"));
    }
}
