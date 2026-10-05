package dev.ruancmm.gerenciador_tarefas.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.ruancmm.gerenciador_tarefas.auth.dto.request.RefreshRequest;
import dev.ruancmm.gerenciador_tarefas.auth.dto.response.AccessTokenResponse;
import dev.ruancmm.gerenciador_tarefas.auth.dto.response.TokenResponse;
import dev.ruancmm.gerenciador_tarefas.auth.exception.AccountAlreadyActivateException;
import dev.ruancmm.gerenciador_tarefas.auth.exception.AccountDisabledException;
import dev.ruancmm.gerenciador_tarefas.core.exception.AuthenticationException;
import dev.ruancmm.gerenciador_tarefas.core.security.SecurityConfig;
import dev.ruancmm.gerenciador_tarefas.users.UserRepository;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserCreateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.response.UserResponse;

@Import(SecurityConfig.class)
@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean 
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void shouldRegisterUserAndReturn201() throws Exception {
        UserResponse response = new UserResponse(1L, "Test", "test@test.com");

        when(authService.register(any(UserCreateRequest.class)))
            .thenReturn(response);

        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "Test",
                    "email": "test@test.com",
                    "password": "testtest"
                }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Test"))
            .andExpect(jsonPath("$.email").value("test@test.com"));
        
        verify(authService).register(any(UserCreateRequest.class));
    }

    @Test
    void shouldReturn422WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "Test",
                    "email": "invalid-email",
                    "password": "testtest"
                }    
                """))
            .andExpect(status().isUnprocessableContent());
    }

    @Test
    void shouldLoginAndReturnTokens() throws Exception {
        TokenResponse response = new TokenResponse("access-token", "refresh-token");

        when(authService.login("test@test.com", "testtest"))
            .thenReturn(response);

        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "test@test.com",
                    "password": "testtest"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
        
        verify(authService).login("test@test.com", "testtest");
    }

    @Test
    void shouldReturn401WhenInvalidCredentials() throws Exception {
        when(authService.login("test@test.com", "invalid"))
            .thenThrow(new AuthenticationException());

        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "test@test.com",
                    "password": "invalid"
                }
                """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenAccountPendingRestore() throws Exception {
        when(authService.login("test2@test.com", "12345678"))
            .thenThrow(new AccountDisabledException());
        
        mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "test2@test.com",
                    "password": "12345678"
                }    
                """))
            .andExpect(status().isForbidden());
    }

    @Test
    void shouldRefreshAndReturnAccessToken() throws Exception {
        AccessTokenResponse response = new AccessTokenResponse("access-token");

        when(authService.refresh(any(RefreshRequest.class)))
            .thenReturn(response);
        
        mockMvc.perform(post("/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "refreshToken": "refresh-token"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"));
        
        verify(authService).refresh(any(RefreshRequest.class));
    }

    @Test
    void shouldRestoreAccountAndReturnTokens() throws Exception {
        TokenResponse response = new TokenResponse("access-token", "refresh-token");

        when(authService.restoreAndLogin("test@test.com", "12345"))
            .thenReturn(response);
        
        mockMvc.perform(post("/auth/restore")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "test@test.com",
                    "password": "12345"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token"))
            .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
        
        verify(authService).restoreAndLogin("test@test.com", "12345");
    }

    @Test
    void shouldReturn401WhenRestoreAuthenticationFails() throws Exception {
        when(authService.restoreAndLogin("test@test.com", "wrong"))
            .thenThrow(new AuthenticationException());

        mockMvc.perform(post("/auth/restore")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "test@test.com",
                    "password": "wrong"
                }
                """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnConflictWhenAccountIsAlreadyActive() throws Exception {
        when(authService.restoreAndLogin("test@test.com", "testtest"))
            .thenThrow(new AccountAlreadyActivateException());

        mockMvc.perform(post("/auth/restore")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "email": "test@test.com",
                    "password": "testtest"
                }
                """))
            .andExpect(status().isConflict());
    }

}
