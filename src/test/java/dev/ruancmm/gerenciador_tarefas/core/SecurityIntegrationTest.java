package dev.ruancmm.gerenciador_tarefas.core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired 
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    // Token tests
    @Test
    void shouldPermitsAccessToProtegedEndpoint() throws Exception {
        String accessToken = registerAndLogin("Test", "test@test.com", "testtest");

        mockMvc.perform(get("/users/me")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk());
    }

    @Test
    void shouldReturn401WithoutJwt() throws Exception {
        mockMvc.perform(get("/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WithInvalidJwt() throws Exception {
        mockMvc.perform(get("/users/me")
            .header("Authorization", "Bearer invalidAccessToken"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenAuthorizationHeaderIsNotBearer() throws Exception {
        mockMvc.perform(get("/users/me")
            .header("Authorization", "Basic abc123"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenTokenExpired() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(jwtSecret)
        );

        String expiredToken = Jwts.builder()
            .subject("1")
            .claim("type", "access")
            .expiration(new Date(System.currentTimeMillis() - 60_000))
            .signWith(key)
            .compact();
        
        mockMvc.perform(get("/users/me")
            .header("Authorization", "Bearer " + expiredToken))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenRefreshTokenIsUsedAsAccessToken() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(jwtSecret)
        );

        String refreshToken = Jwts.builder()
            .subject("1")
            .claim("type", "refresh")
            .expiration(new Date(System.currentTimeMillis() + 900_000))
            .signWith(key)
            .compact();
        
        mockMvc.perform(get("/users/me")
            .header("Authorization", "Bearer " + refreshToken))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectTokenWithoutUserId() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(jwtSecret)
        );

        String accessToken = Jwts.builder()
            .claim("type", "access")
            .expiration(new Date(System.currentTimeMillis() + 900_000))
            .signWith(key)
            .compact();
        
        mockMvc.perform(get("/users/me")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectTokenWhitInvalidUserId() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(jwtSecret)
        );

        String accessToken = Jwts.builder()
            .subject("abc")
            .claim("type", "access")
            .expiration(new Date(System.currentTimeMillis() + 900_000))
            .signWith(key)
            .compact();
        
        mockMvc.perform(get("/users/me")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WhenUserDoesNotExist() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(
            Decoders.BASE64.decode(jwtSecret)
        );

        String accessToken = Jwts.builder()
            .subject("1")
            .claim("type", "access")
            .expiration(new Date(System.currentTimeMillis() + 900_000))
            .signWith(key)
            .compact();
        
        mockMvc.perform(get("/users/me")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isUnauthorized());
    }

    // Task tests
    @Test
    void shouldAllowOwnerToDeleteOwnTask() throws Exception {
        String ownerToken = registerAndLogin("Owner", uniqueEmail(), "ownerPassword");
        long taskId = createTask(ownerToken, "Owner's task");

        mockMvc.perform(delete("/tasks/{id}", taskId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isNoContent());
    }
    
    @Test
    void shouldReturn404WhenUserTriesToDeleteAnotherUsersTask() throws Exception {
        String ownerToken = registerAndLogin("Owner", uniqueEmail(), "ownerPassword");
        String intruderToken = registerAndLogin("Intruder", uniqueEmail(), "intruderPassword");
        long taskId = createTask(ownerToken, "Owner's task");

        mockMvc.perform(delete("/tasks/{id}", taskId)
                .header("Authorization", "Bearer " + intruderToken))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Task not found"))
            .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(get("/tasks/{id}", taskId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk());
    }

    @Test
    void shouldAllowOwnerToRestoreOwnTask() throws Exception {
        String ownerToken = registerAndLogin("Owner", uniqueEmail(), "ownerPassword");
        long taskId = createTask(ownerToken, "Owner's Task");

        mockMvc.perform(delete("/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isNoContent());

        mockMvc.perform(post("/tasks/{id}/restore", taskId)
            .header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isOk());

        mockMvc.perform(get("/tasks/{id}", taskId)
            .header("Authorization", "Bearer " + ownerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Owner's Task"))
        .andExpect(jsonPath("$.description").value("Created by integration test"))
        .andExpect(jsonPath("$.status").value("TODO"));
    }

    // User tests
    @Test
    void shouldUserRestoreAccount() throws Exception {
        String userEmail = uniqueEmail();
        String userToken = registerAndLogin("User", userEmail, "12345");

        mockMvc.perform(delete("/users/me")
            .header("Authorization", "Bearer " + userToken))
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
                """.formatted(userEmail)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("Account disabled. Please reactivate your account"))
            .andExpect(jsonPath("$.timestamp").exists());
        
        mockMvc.perform(post("/auth/restore")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "%s",
                    "password": "12345"
                }
            """.formatted(userEmail)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void shouldExposeHealthEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private String registerAndLogin(String name, String email, String password) throws Exception {
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

        return objectMapper.readTree(result.getResponse().getContentAsString())
            .get("accessToken").asString();
    }

    private long createTask(String token, String title) throws Exception {
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
}
