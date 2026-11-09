package dev.ruancmm.gerenciador_tarefas.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

public class AuthTestHelper {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public AuthTestHelper(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    public Session registerAndLogin(String name, String email, String password) throws Exception {
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "name": "%s", 
                        "email": "%s", 
                        "password": "%s"
                    }
                    """.formatted(name, email, password)))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "email": "%s", 
                        "password": "%s"
                    }
                    """.formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return new Session(
            email, password, body.get("accessToken").asString(), body.get("refreshToken").asString()
        );
    }

    public ResultActions refresh(String refreshToken) throws Exception {
    return mockMvc.perform(post("/auth/refresh")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"refreshToken": "%s"}
            """.formatted(refreshToken)));
    }

    public String rotate(String refreshToken) throws Exception {
        MvcResult result = refresh(refreshToken).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("refreshToken").asString();
    }

    public ResultActions logout(String refreshToken) throws Exception {
        return mockMvc.perform(post("/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"refreshToken": "%s"}
                """.formatted(refreshToken)));
    }

    public long createTask(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/tasks")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "%s", 
                    "description": "Created by integration test"
                }
                """.formatted(title)))
            .andExpect(status().isCreated())
            .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("id").asLong();
    }

    public record Session(String email, String password, String accessToken, String refreshToken) {}
}
