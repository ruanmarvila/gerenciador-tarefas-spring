package dev.ruancmm.gerenciador_tarefas.users;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import dev.ruancmm.gerenciador_tarefas.auth.JwtUtil;
import dev.ruancmm.gerenciador_tarefas.core.security.SecurityConfig;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserUpdatePasswordRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserUpdateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.response.UserResponse;
import dev.ruancmm.gerenciador_tarefas.users.exception.PasswordReuseException;

@Import(SecurityConfig.class)
@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired 
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean 
    private JwtUtil jwtUtil;

    @MockitoBean
    private UserRepository userRepository;

    private final User mockUser = new User("Test", "test@test.com", "testtest");

    @Test
    void shouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/users/me")
            .with(user(mockUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    void shouldReturn401WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/users/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldUpdateUser() throws Exception {
        UserResponse response = new UserResponse(1L, "Ana", "ana@gmail.com");

        when(userService.update(any(UserUpdateRequest.class), eq(mockUser)))
            .thenReturn(response);

        mockMvc.perform(patch("/users/me")
            .with(user(mockUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "name": "Ana",
                    "email": "ana@gmail.com"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Ana"))
            .andExpect(jsonPath("$.email").value("ana@gmail.com"));
        
        verify(userService).update(any(UserUpdateRequest.class), eq(mockUser));
    }

    @Test
    void shouldReturn422WhenUpdateRequestIsNull() throws Exception {
        mockMvc.perform(patch("/users/me")
        .with(user(mockUser))
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
                "name": null,
                "email": null
            }
            """))
        .andExpect(status().isUnprocessableContent());
    }

    @Test
    void shouldUpdatePassword() throws Exception {
        mockMvc.perform(patch("/users/me/password")
            .with(user(mockUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "password": "testtest",
                    "newPassword": "12345678"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("message").value("password successfully updated"));
        
        verify(userService).updatePassword(any(UserUpdatePasswordRequest.class), eq(mockUser));
    }

    @Test
    void shouldReturn400WhenNewPasswordEqualsCurrent() throws Exception {
        doThrow(new PasswordReuseException())
            .when(userService).updatePassword(any(UserUpdatePasswordRequest.class), eq(mockUser));

        mockMvc.perform(patch("/users/me/password")
            .with(user(mockUser))
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "password": "12345678",
                    "newPassword": "12345678"
                }
                """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteUser() throws Exception {
        mockMvc.perform(delete("/users/me")
            .with(user(mockUser)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("message")
            .value("Account deletion started. You have 30 days to cancel the deletion"));
        
        verify(userService).delete(mockUser);
    }
}
