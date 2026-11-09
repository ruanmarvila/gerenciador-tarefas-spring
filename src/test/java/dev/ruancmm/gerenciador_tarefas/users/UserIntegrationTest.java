package dev.ruancmm.gerenciador_tarefas.users;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import dev.ruancmm.gerenciador_tarefas.config.PostgresContainerConfig;
import dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper;
import dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper.Session;
import static dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper.uniqueEmail;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
public class UserIntegrationTest {

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
    void shouldUserRestoreAccount() throws Exception {
        Session session = auth.registerAndLogin("User", uniqueEmail(), "12345");
        long taskId = auth.createTask(session.accessToken(), "User's Task");

        mockMvc.perform(delete("/users/me")
            .header("Authorization", "Bearer " + session.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message")
            .value("Account deletion started. You have 30 days to cancel the deletion"));

        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "%s", 
                    "password": "12345"
                }
                """.formatted(session.email())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Account disabled. Please reactivate your account"))
            .andExpect(jsonPath("$.timestamp").exists());
        
        MvcResult result = mockMvc.perform(post("/auth/restore")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "%s",
                    "password": "12345"
                }
            """.formatted(session.email())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn();

            String newAccessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asString();
            
            mockMvc.perform(get("/tasks/{id}", taskId)
                .header("Authorization", "Bearer " + newAccessToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("User's Task"))
            .andExpect(jsonPath("$.description").value("Created by integration test"))
            .andExpect(jsonPath("$.status").value("TODO"));
    }
}
