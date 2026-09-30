package dev.ruancmm.gerenciador_tarefas.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.ruancmm.gerenciador_tarefas.auth.dto.request.RefreshRequest;
import dev.ruancmm.gerenciador_tarefas.auth.dto.response.AccessTokenResponse;
import dev.ruancmm.gerenciador_tarefas.auth.dto.response.TokenResponse;
import dev.ruancmm.gerenciador_tarefas.auth.exception.AccountAlreadyActivateException;
import dev.ruancmm.gerenciador_tarefas.auth.exception.AccountDisabledException;
import dev.ruancmm.gerenciador_tarefas.auth.exception.InvalidCredentialsException;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserCreateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.response.UserResponse;
import dev.ruancmm.gerenciador_tarefas.users.User;
import dev.ruancmm.gerenciador_tarefas.users.UserRepository;
import dev.ruancmm.gerenciador_tarefas.users.UserService;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @Mock 
    private UserRepository userRepository;

    @Mock 
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldDelegateToUserService() {
        UserCreateRequest request = new UserCreateRequest("Bob", "bob@gmail.com", "44444");
        UserResponse response = new UserResponse(1L, "Bob", "bob@gmail.com");

        when(userService.create(request))
            .thenReturn(response);

        UserResponse result = authService.register(request);

        assertEquals(response, result);
        verify(userService).create(request);
    }

    @Test
    void shouldLoginWithValidCredentials() {
        User user = new User("Ana", "ana@gmail.com", "12345");
        user.setId(1L);

        when(authenticationManager.authenticate(any()))
            .thenReturn(authentication);
        
        when(authentication.getPrincipal())
            .thenReturn(user);

        when(jwtUtil.generateAccessToken(user.getId()))
            .thenReturn("access");

        when(jwtUtil.generateRefreshToken(user.getId()))
            .thenReturn("refresh");

        TokenResponse result = authService.login("ana@gmail.com", "12345");

        verify(authenticationManager).authenticate(any());
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());

        verify(jwtUtil).generateAccessToken(user.getId());
        verify(jwtUtil).generateRefreshToken(user.getId());
    }

    @Test
    void shouldRejectInvalidCredentials() {
        String email = "test@test.com";
        String password = "teste";

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad Credentials"));
        
        assertThrows(
            InvalidCredentialsException.class,
            () -> authService.login(email, password)
        );
    }

    @Test
    void shouldRejectWhenUserNotFound() {
        String email = "test@test.com";
        String password = "teste";

        when(authenticationManager.authenticate(any()))
            .thenThrow(new UsernameNotFoundException("user not found"));
        
        assertThrows(
            InvalidCredentialsException.class,
            () -> authService.login(email, password)
        );
    }

    @Test
    void shouldRejectWhenAccountIsPendingRestore() {
        String email = "julia@gmail.com";
        String password = "99999";
        User user = new User("Julia", email, password);

        when(authenticationManager.authenticate(any()))
            .thenThrow(new UsernameNotFoundException("user not found"));
        
        when(userRepository.findByEmailIncludingDeleted(email))
            .thenReturn(Optional.of(user));
        
        when(passwordEncoder.matches(password, user.getPassword()))
            .thenReturn(true);

        user.setDeletedAt(LocalDateTime.now().minusDays(10));

        assertThrows(
            AccountDisabledException.class,
            () -> authService.login(email, password)
        );
    }

    @Test
    void shouldRejectWhenAccountRestoreWindowHasExpired() {
        String email = "joao@gmail.com";
        String password = "99999";
        User user = new User("João", email, password);

        when(authenticationManager.authenticate(any()))
            .thenThrow(new UsernameNotFoundException("user not found"));
        
        when(userRepository.findByEmailIncludingDeleted(email))
            .thenReturn(Optional.of(user));
        
        when(passwordEncoder.matches(password, user.getPassword()))
            .thenReturn(true);

        user.setDeletedAt(LocalDateTime.now().minusDays(31));

        assertThrows(
            InvalidCredentialsException.class,
            () -> authService.login(email, password)
        );
    }

    @Test
    void shouldReturnAccessToken() {
        Long userId = 1L;
        RefreshRequest request = new RefreshRequest("refreshToken");

        when(jwtUtil.extractUserId(request.refreshToken(), "refresh"))
            .thenReturn(userId);

        when(jwtUtil.generateAccessToken(userId))
            .thenReturn("access");

        AccessTokenResponse result = authService.refresh(request);

        assertEquals("access", result.accessToken());
    }

    @Test
    void shouldRestoreAccountAndLogin() {
        String email = "helena@hotmail.com";
        String password = "12345678";
        User user = new User("Helena", email, password);

        when(userRepository.findByEmailIncludingDeleted(email))
            .thenReturn(Optional.of(user));
        
        when(passwordEncoder.matches(password, user.getPassword()))
            .thenReturn(true);
        
        user.setDeletedAt(LocalDateTime.now().minusDays(21));
        user.setId(1L);

        when(jwtUtil.generateAccessToken(user.getId()))
            .thenReturn("access");

        when(jwtUtil.generateRefreshToken(user.getId()))
            .thenReturn("refresh");
        
        TokenResponse result = authService.restoreAndLogin(email, password);

        assertNull(user.getDeletedAt());
        assertEquals("access", result.accessToken());
        assertEquals("refresh", result.refreshToken());

        verify(userRepository).save(user);
    }

    @Test
    void shouldRejectRestoreWhenUserNotFound() {
        String email = "test@test.com";
        String password = "testtest";

        when(userRepository.findByEmailIncludingDeleted(email))
            .thenReturn(Optional.empty());
        
        assertThrows(
            InvalidCredentialsException.class,
            () -> authService.restoreAndLogin(email, password)
        );
    }

    @Test
    void shouldRejectRestoreWhenPasswordIsInvalid() {
        String email = "test@test.com";
        String password = "testtest";
        User user = new User("Test", email, "password");

        when(userRepository.findByEmailIncludingDeleted(email))
            .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(password, user.getPassword()))
            .thenReturn(false);
        
        assertThrows(
            InvalidCredentialsException.class,
            () -> authService.restoreAndLogin(email, password)
        );
    }

    @Test
    void shouldRejectRestoreWhenAccountAlreadyActivated() {
        String email = "test@test.com";
        String password = "testtest";
        User user = new User("Test", email, password);

        when(userRepository.findByEmailIncludingDeleted(email))
            .thenReturn(Optional.of(user));
        
        when(passwordEncoder.matches(password, user.getPassword()))
            .thenReturn(true);
        
        assertThrows(
            AccountAlreadyActivateException.class,
            () -> authService.restoreAndLogin(email, password)
        );
    }

    @Test
    void shouldRejectRestoreWhenWindowRestoreHasExpired() {
        String email = "test@test.com";
        String password = "testtest";
        User user = new User("Test", email, password);

        when(userRepository.findByEmailIncludingDeleted(email))
            .thenReturn(Optional.of(user));
        
        when(passwordEncoder.matches(password, user.getPassword()))
            .thenReturn(true);
        
        user.setDeletedAt(LocalDateTime.now().minusDays(31));
        user.setId(1L);

        assertThrows(
            InvalidCredentialsException.class,
            () -> authService.restoreAndLogin(email, password)
        );
    }
}
