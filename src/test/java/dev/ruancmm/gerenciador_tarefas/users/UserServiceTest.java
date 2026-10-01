package dev.ruancmm.gerenciador_tarefas.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import dev.ruancmm.gerenciador_tarefas.core.exception.AuthenticationException;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserCreateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserUpdatePasswordRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserUpdateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.response.UserResponse;
import dev.ruancmm.gerenciador_tarefas.users.exception.EmailAlreadyExistsException;
import dev.ruancmm.gerenciador_tarefas.users.exception.PasswordReuseException;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldCreateUser() {
        UserCreateRequest request = new UserCreateRequest("Test", "test@test.com", "password");
        UserResponse response = new UserResponse(1l, "Test", "test@test.com");

        User user = new User(request.name(), request.email(), "hashedPassword");
        user.setId(1l);

        when(userRepository.existsByEmail(request.email()))
            .thenReturn(false);

        when(passwordEncoder.encode(request.password()))
            .thenReturn("hashedPassword");
        
        when(userRepository.save(any(User.class)))
            .thenReturn(user);
        
        UserResponse result = userService.create(request);

        assertEquals(response, result);
        
        verify(passwordEncoder).encode(request.password());

        verify(userRepository).save(argThat(savedUser ->
            savedUser.getName().equals(request.name()) &&
            savedUser.getEmail().equals(request.email()) &&
            savedUser.getPassword().equals("hashedPassword")
        ));
    }

    @Test
    void shouldRejectCreateWhenEmailAlreadyExists() {
        UserCreateRequest request = new UserCreateRequest("Test", "test@test.com", "password");

        when(userRepository.existsByEmail(request.email()))
            .thenReturn(true);

        assertThrows(
            EmailAlreadyExistsException.class,
            () -> userService.create(request)
        );
    }

    @Test
    void shouldUpdateUser() {
        UserUpdateRequest request = new UserUpdateRequest("newName", "new@gmail.com");
        User currentUser = new User("Test", "test@test.com", "password");
        currentUser.setId(1L);

        UserResponse response = new UserResponse(1L, "newName", "new@gmail.com");

        when(userRepository.existsByEmail(request.email()))
            .thenReturn(false);
        
        when(userRepository.save(any(User.class)))
            .thenReturn(currentUser);

        UserResponse result = userService.update(request, currentUser);

        assertEquals(response, result);

        verify(userRepository).save(argThat(savedUser -> 
            savedUser.getName().equals(request.name()) &&
            savedUser.getEmail().equals(request.email())
        ));
    }

    @Test 
    void shouldUpdateOnlyEmailWhenNameIsNull() {
        UserUpdateRequest request = new UserUpdateRequest(null, "new@gmail.com");
        User currentUser = new User("Test", "test@test.com", "password");
        currentUser.setId(1L);

        UserResponse response = new UserResponse(1L, "Test", "new@gmail.com");

        when(userRepository.existsByEmail(request.email()))
            .thenReturn(false);
        
        when(userRepository.save(any(User.class)))
            .thenReturn(currentUser);

        UserResponse result = userService.update(request, currentUser);

        assertEquals(response, result);

        verify(userRepository).save(argThat(savedUser -> 
            savedUser.getName().equals(response.name()) &&
            savedUser.getEmail().equals(request.email())
        ));
    }

    @Test
    void shouldUpdateOnlyNameWhenEmailIsNull() {
        UserUpdateRequest request = new UserUpdateRequest("newName", null);
        User currentUser = new User("Test", "test@test.com", "password");
        currentUser.setId(1L);

        UserResponse response = new UserResponse(1L, "newName", "test@test.com");
        
        when(userRepository.save(any(User.class)))
            .thenReturn(currentUser);

        UserResponse result = userService.update(request, currentUser);

        assertEquals(response, result);

        verify(userRepository).save(argThat(savedUser -> 
            savedUser.getName().equals(request.name()) &&
            savedUser.getEmail().equals(response.email())
        ));
    }

    @Test 
    void shouldUpdateOnlyNameWhenEmailAlreadyExists() {
        UserUpdateRequest request = new UserUpdateRequest("newName", "exists@email.com");
        User currentUser = new User("Test", "test@test.com", "password");
        currentUser.setId(1L);

        UserResponse response = new UserResponse(1L, "newName", "test@test.com");

        when(userRepository.existsByEmail(request.email()))
            .thenReturn(true);
        
        when(userRepository.save(any(User.class)))
            .thenReturn(currentUser);

        UserResponse result = userService.update(request, currentUser);

        assertEquals(response, result);

        verify(userRepository).save(argThat(savedUser -> 
            savedUser.getName().equals(request.name()) &&
            savedUser.getEmail().equals(response.email())
        ));
    }

    @Test
    void shouldUpdatePassword() {
        UserUpdatePasswordRequest request = new UserUpdatePasswordRequest("12345", "54321");
        User currentUser = new User("Test", "test@tess.com", "12345");

        when(passwordEncoder.matches(request.password(), currentUser.getPassword()))
            .thenReturn(true);
        
        when(passwordEncoder.encode(request.newPassword()))
            .thenReturn("hashed-54321");
        
        userService.updatePassword(request, currentUser);
        
        assertEquals("hashed-54321", currentUser.getPassword());
        verify(userRepository).save(currentUser);
    }

    @Test
    void shouldRejectWhenPasswordIsEqualsNewPassword() {
        UserUpdatePasswordRequest request = new UserUpdatePasswordRequest("12345", "12345");
        
        assertThrows(
            PasswordReuseException.class,
            () -> userService.updatePassword(request, new User())
        );
    }

    @Test
    void shouldRejectWhenPasswordNotIsCurrentPassword() {
        UserUpdatePasswordRequest request = new UserUpdatePasswordRequest("12345", "54321");
        User currentUser = new User("Test", "test@tess.com", "testtest");

        when(passwordEncoder.matches(request.password(), currentUser.getPassword()))
            .thenReturn(false);
        
        assertThrows(
            AuthenticationException.class,
            () -> userService.updatePassword(request, currentUser)
        );
    }

    @Test
    void shouldSoftDeleteUser() {
        User currentUser = new User("Test", "test@tess.com", "testtest");

        userService.delete(currentUser);

        verify(userRepository).delete(currentUser);
    }
}
