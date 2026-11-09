package dev.ruancmm.gerenciador_tarefas.core;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import dev.ruancmm.gerenciador_tarefas.config.PostgresContainerConfig;
import dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper;
import dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper.Session;
import static dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper.uniqueEmail;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired 
    private ObjectMapper objectMapper;

    @Value("${jwt.secret}")
    private String jwtSecret;

    private AuthTestHelper auth;

    @BeforeEach
    void setUp() {
        auth = new AuthTestHelper(mockMvc, objectMapper);
    }

    @Test
    void shouldPermitsAccessToProtegedEndpoint() throws Exception {
        Session session = auth.registerAndLogin("Test", uniqueEmail(), "testtest");

        mockMvc.perform(get("/users/me")
            .header("Authorization", "Bearer " + session.accessToken()))
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
            .subject("999999")
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
        Session session = auth.registerAndLogin("Test", uniqueEmail(), "testtest");
        
        mockMvc.perform(get("/users/me")
            .header("Authorization", "Bearer " + session.refreshToken()))
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
            .subject("555555")
            .claim("type", "access")
            .expiration(new Date(System.currentTimeMillis() + 900_000))
            .signWith(key)
            .compact();
        
        mockMvc.perform(get("/users/me")
            .header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldExposeHealthEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }
}
