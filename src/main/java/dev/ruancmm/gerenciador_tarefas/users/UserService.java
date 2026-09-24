package dev.ruancmm.gerenciador_tarefas.users;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserCreateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserUpdateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserUpdatePasswordRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.response.UserResponse;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(request.name(), request.email(), hashedPassword);
        return UserResponse.fromEntity(userRepository.save(user));
    }

    public UserResponse update(UserUpdateRequest request, User currentUser) {
        if (request.name() != null) {
            currentUser.setName(request.name());
        }

        if (request.email() != null && !userRepository.existsByEmail(request.email())) {
            currentUser.setEmail(request.email());
        }

        return UserResponse.fromEntity(userRepository.save(currentUser));
    }

    public void updatePassword(UserUpdatePasswordRequest request, User currentUser) {
        if (request.password().equals(request.newPassword())) {
            throw new RuntimeException("New password cannot be the same as the current password");
        }

        if (!passwordEncoder.matches(request.password(), currentUser.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        String newHashedPassword = passwordEncoder.encode(request.newPassword());
        currentUser.setPassword(newHashedPassword);
        userRepository.save(currentUser);
    }

    public void delete(User currentUser) {
        userRepository.delete(currentUser);
    }
}
