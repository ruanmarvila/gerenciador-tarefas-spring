package dev.ruancmm.gerenciador_tarefas.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import dev.ruancmm.gerenciador_tarefas.config.PostgresContainerConfig;
import dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper;
import dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper.Session;
import static dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper.uniqueEmail;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
public class RefreshTokenIntegrationTest {

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
    void shouldRotateRefreshTokenAndIssueWorkingAccessToken() throws Exception {
        Session session = auth.registerAndLogin("Test", uniqueEmail(), "testtest");

        MvcResult result = auth.refresh(session.refreshToken())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertNotEquals(session.refreshToken(), body.get("refreshToken").asString());

        mockMvc.perform(get("/users/me")
            .header("Authorization", "Bearer " + session.accessToken()))
        .andExpect(status().isOk());
    }

    @Test
    void shouldRejectOldRefreshTokenAfterRotation() throws Exception {
        Session session = auth.registerAndLogin("Test", uniqueEmail(), "testtest");
        auth.rotate(session.refreshToken());

        auth.refresh(session.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRevokeFamilyWhenOldTokenIsReused() throws Exception {
        Session session = auth.registerAndLogin("Test", uniqueEmail(), "testtest");
        String second = auth.rotate(session.refreshToken());

        auth.refresh(session.refreshToken()).andExpect(status().isUnauthorized());
        auth.refresh(second).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRefreshTokenAfterLogout() throws Exception {
        Session session = auth.registerAndLogin("Test", uniqueEmail(), "testtest");

        auth.logout(session.refreshToken()).andExpect(status().isNoContent());
        auth.refresh(session.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutShouldBePublicAndIdempotent() throws Exception {
        auth.logout("obwoebvbu").andExpect(status().isNoContent());
    }

    @Test
    void shouldRevokeRefreshTokenWhenPasswordChanges() throws Exception {
        Session session = auth.registerAndLogin("Test", uniqueEmail(), "oldPassword");

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

        auth.refresh(session.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRevokeRefreshTokenWhenAccountIsDeleted() throws Exception {
        Session session = auth.registerAndLogin("Test", uniqueEmail(), "testtest");

        mockMvc.perform(delete("/users/me")
            .header("Authorization", "Bearer " + session.accessToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message")
            .value("Account deletion started. You have 30 days to cancel the deletion"));

        auth.refresh(session.refreshToken()).andExpect(status().isUnauthorized());
    }
}
