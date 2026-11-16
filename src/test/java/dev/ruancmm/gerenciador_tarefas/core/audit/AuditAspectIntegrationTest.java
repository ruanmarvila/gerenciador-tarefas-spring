package dev.ruancmm.gerenciador_tarefas.core.audit;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import dev.ruancmm.gerenciador_tarefas.config.PostgresContainerConfig;
import dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper;
import dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper.Session;
import tools.jackson.databind.ObjectMapper;

import static dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper.uniqueEmail;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
public class AuditAspectIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired 
    private ObjectMapper objectMapper;

    private AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestHelper(mockMvc, objectMapper);
    }

    @Test
    void loginShouldBeAudited() throws Exception {
        try (var logs = new AuditLogCapture()) {
            Session session = auth.registerAndLogin("Test", uniqueEmail(), "testtest");

            assertTrue(logs.messages().contains("action=LOGIN actor=anonymous outcome=SUCESS"));
        }
    }

    @Test
    void passwordChangeShouldBeAuditedWithTheUserId() throws Exception {
        Session session = auth.registerAndLogin("Test", uniqueEmail(), "oldPassword");

        try (var logs = new AuditLogCapture()) {
            mockMvc.perform(patch("/users/me/password")
                .header("Authorization", "Bearer " + session.accessToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "password": "oldPassword",
                        "newPassword": "newPassword"
                    }
                    """))
                .andExpect(status().isOk());

            assertTrue(logs.messages().stream()
                .anyMatch(m -> m.matches("action=PASSWORD_CHANGE actor=\\d+ outcome=SUCESS")));
        }
    }

    @Test
    void failedLoginShouldBeAudited() throws Exception {
        try(var logs = new AuditLogCapture()) {
            mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "nobody@test.com",
                        "password": "password"
                    }
                    """))
                .andExpect(status().isUnauthorized());

            assertTrue(logs.messages().stream()
                .anyMatch(m -> m.startsWith("action=LOGIN") && m.contains("outcome=FAILED")));
        }
    }
}
