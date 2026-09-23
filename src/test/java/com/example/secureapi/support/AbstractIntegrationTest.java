package com.example.secureapi.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base de los tests de integración: una única instancia de PostgreSQL (Testcontainers)
 * compartida por todas las clases, y un único contexto de Spring cacheado.
 * Cada test usa emails aleatorios, así que no hace falta limpiar la BD entre tests.
 */
@SpringBootTest(properties = {
        "app.jwt.secret=integration-test-secret-key-with-32-bytes-min",
        "app.jwt.expiration=3600",
        "app.admin.email=" + AbstractIntegrationTest.ADMIN_EMAIL,
        "app.admin.password=" + AbstractIntegrationTest.ADMIN_PASSWORD
})
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@test.local";
    protected static final String ADMIN_PASSWORD = "AdminPassword1";
    protected static final String DEFAULT_PASSWORD = "Password123";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static String randomEmail() {
        return "user-" + UUID.randomUUID() + "@test.local";
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }

    protected String toJson(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    protected JsonNode readJson(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected long register(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("name", "Test User", "email", email, "password", password))))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }

    protected String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return readJson(result).get("accessToken").asText();
    }

    /** Registra un usuario nuevo y devuelve su token. */
    protected TestUser newUser() throws Exception {
        String email = randomEmail();
        long id = register(email, DEFAULT_PASSWORD);
        return new TestUser(id, email, login(email, DEFAULT_PASSWORD));
    }

    protected String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected long createTask(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tasks")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(Map.of("title", title, "description", "desc", "priority", "HIGH"))))
                .andExpect(status().isCreated())
                .andReturn();
        return readJson(result).get("id").asLong();
    }

    protected record TestUser(long id, String email, String token) {
    }
}
