package com.example.secureapi.security;

import com.example.secureapi.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends AbstractIntegrationTest {

    // --- 401: autenticación ---

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/api/v1/tasks"));
    }

    @Test
    void requestWithInvalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer("invalid.token.value")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithTamperedTokenIsUnauthorized() throws Exception {
        String token = newUser().token();
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonBearerAuthorizationHeaderIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    // --- 403: autorización vertical (roles) ---

    @Test
    void userCannotAccessAdminEndpoints() throws Exception {
        String token = newUser().token();

        mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/admin/tasks").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/admin/users/1").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdministrativeResources() throws Exception {
        String adminToken = adminToken();
        TestUser user = newUser();
        long taskId = createTask(user.token(), "Visible to admin");

        mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].password").doesNotExist());

        mockMvc.perform(get("/api/v1/admin/users/{id}", user.id()).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.email()));

        mockMvc.perform(get("/api/v1/admin/tasks").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + taskId + ")]").exists());
    }

    // --- Autorización horizontal (propiedad del recurso) ---

    @Test
    void userCanAccessOwnTask() throws Exception {
        TestUser owner = newUser();
        long taskId = createTask(owner.token(), "Mine");

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId).header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotReadUpdateOrDeleteAnotherUsersTask() throws Exception {
        TestUser owner = newUser();
        TestUser attacker = newUser();
        long taskId = createTask(owner.token(), "Private");

        mockMvc.perform(get("/api/v1/tasks/{id}", taskId).header(HttpHeaders.AUTHORIZATION, bearer(attacker.token())))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/tasks/{id}", taskId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(attacker.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("title", "Hacked", "status", "COMPLETED", "priority", "LOW"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/tasks/{id}", taskId).header(HttpHeaders.AUTHORIZATION, bearer(attacker.token())))
                .andExpect(status().isNotFound());

        // La tarea sigue intacta para su propietario
        mockMvc.perform(get("/api/v1/tasks/{id}", taskId).header(HttpHeaders.AUTHORIZATION, bearer(owner.token())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Private"));
    }

    @Test
    void ownerIdInRequestBodyIsIgnored() throws Exception {
        TestUser victim = newUser();
        TestUser attacker = newUser();

        mockMvc.perform(post("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(attacker.token()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("title", "Injected", "userId", victim.id()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(attacker.id()));
    }

    // --- Bloqueo y eliminación de cuentas ---

    @Test
    void disabledUserLosesAccessImmediatelyAndCannotLogIn() throws Exception {
        String adminToken = adminToken();
        TestUser user = newUser();

        mockMvc.perform(patch("/api/v1/admin/users/{id}/disable", user.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", user.email(), "password", DEFAULT_PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        mockMvc.perform(patch("/api/v1/admin/users/{id}/enable", user.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        login(user.email(), DEFAULT_PASSWORD);
    }

    @Test
    void deletedUserTokenStopsWorkingAndTasksAreRemoved() throws Exception {
        String adminToken = adminToken();
        TestUser user = newUser();
        long taskId = createTask(user.token(), "Will be deleted");

        mockMvc.perform(delete("/api/v1/admin/users/{id}", user.id())
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(user.token())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/users/{id}", user.id()).header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/admin/tasks").param("size", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(jsonPath("$.content[?(@.id == " + taskId + ")]").doesNotExist());
    }

    @Test
    void adminCannotDisableThemselves() throws Exception {
        String adminToken = adminToken();
        long adminId = objectMapper.readTree(mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/api/v1/admin/users/{id}/disable", adminId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(adminToken)))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- Endpoints públicos ---

    @Test
    void swaggerDocumentationIsPublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }
}
