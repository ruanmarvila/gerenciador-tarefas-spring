package dev.ruancmm.gerenciador_tarefas.core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import tools.jackson.databind.JsonNode;
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

    @Test
    void shouldPermitsAccessToProtegedEndpoint() throws Exception {
        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "Test",
                    "email": "test@test.com",
                    "password": "testtest"
                }
                """))
            .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "test@test.com",
                    "password": "testtest"
                }
                """))
            .andExpect(status().isOk())
            .andReturn();

        String response = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        String accessToken = json.get("accessToken").asString();

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
    
    @Test
    void shouldReturn403WhenUserTriesToUpdateAnotherUsersTask() throws Exception {
        String otherUserEmail = uniqueEmail();
        String userEmail = uniqueEmail();
        
        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "Other User",
                    "email": "%s",
                    "password": "otherPassword"
                }
                """.formatted(otherUserEmail)))
            .andExpect(status().isCreated());
        
        MvcResult result = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "%s",
                    "password": "otherPassword"
                }
                """.formatted(otherUserEmail)))
            .andExpect(status().isOk())
            .andReturn();
        
        String response = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        String accessToken = json.get("accessToken").asString();

        MvcResult taskResult =mockMvc.perform(post("/tasks/create")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": null,
                    "description": null
                }
                """))
            .andExpect(status().isCreated())
            .andReturn();
        
        JsonNode taskJson = objectMapper.readTree(
            taskResult.getResponse().getContentAsString()
        );
        long taskId = taskJson.get("id").asLong();
        
        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "Current User",
                    "email": "%s",
                    "password": "password"
                }
                """.formatted(userEmail)))
            .andExpect(status().isCreated());
        
        MvcResult result2 = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "%s",
                    "password": "password"
                }
                """.formatted(userEmail)))
            .andExpect(status().isOk())
            .andReturn();
        
        String response2 = result2.getResponse().getContentAsString();
        JsonNode json2 = objectMapper.readTree(response2);
        String accessToken2 = json2.get("accessToken").asString();

        mockMvc.perform(patch("/tasks/update/{id}", taskId)
            .header("Authorization", "Bearer " + accessToken2)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": "New Title",
                    "description": null,
                    "status": "DONE"
                }
                """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.message").value("You don't have authorization for this operation"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn403WhenUserTriesToDeleteAnotherUsersTask() throws Exception {
        String otherUserEmail = uniqueEmail();
        String userEmail = uniqueEmail();
        
        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "Other User",
                    "email": "%s",
                    "password": "otherPassword"
                }
                """.formatted(otherUserEmail)))
            .andExpect(status().isCreated());
        
        MvcResult result = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "%s",
                    "password": "otherPassword"
                }
                """.formatted(otherUserEmail)))
            .andExpect(status().isOk())
            .andReturn();
        
        String response = result.getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        String accessToken = json.get("accessToken").asString();

        MvcResult taskResult = mockMvc.perform(post("/tasks/create")
            .header("Authorization", "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "title": null,
                    "description": null
                }
                """))
            .andExpect(status().isCreated())
            .andReturn();

        JsonNode taskJson = objectMapper.readTree(
            taskResult.getResponse().getContentAsString()
        );
        long taskId = taskJson.get("id").asLong();
        
        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "Current User",
                    "email": "%s",
                    "password": "password"
                }
                """.formatted(userEmail)))
            .andExpect(status().isCreated());
        
        MvcResult result2 = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "%s",
                    "password": "password"
                }
                """.formatted(userEmail)))
            .andExpect(status().isOk())
            .andReturn();
        
        String response2 = result2.getResponse().getContentAsString();
        JsonNode json2 = objectMapper.readTree(response2);
        String accessToken2 = json2.get("accessToken").asString();

        mockMvc.perform(delete("/tasks/delete/{id}", taskId)
            .header("Authorization", "Bearer " + accessToken2))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.message").value("You don't have authorization for this operation"))
        .andExpect(jsonPath("$.timestamp").exists());
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }
}
