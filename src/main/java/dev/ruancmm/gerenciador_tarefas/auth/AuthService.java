package dev.ruancmm.gerenciador_tarefas.auth;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.ruancmm.gerenciador_tarefas.users.User;
import dev.ruancmm.gerenciador_tarefas.users.UserRepository;
import dev.ruancmm.gerenciador_tarefas.users.UserService;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserCreateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.response.UserResponse;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
	
  public AuthService(UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
      this.userRepository = userRepository;
      this.userService = userService;
      this.passwordEncoder = passwordEncoder;
      this.jwtUtil = jwtUtil;
	}

  public UserResponse register(UserCreateRequest request) {
    return userService.create(request);
  }

  public Map<String, String> login(String email, String password) {
      User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new RuntimeException("Invalid credentials"));

      if (!passwordEncoder.matches(password, user.getPassword())) {
        throw new RuntimeException("Invalid credentials");
      }

      return Map.of(
        "accessToken", jwtUtil.generateAccessToken(user.getId()),
        "refreshToken", jwtUtil.generateRefreshToken(user.getId())
      );
  }

  public String refresh(String refreshToken) {
    Long userId = jwtUtil.extractUserId(refreshToken, "refresh");

    if (userId == null) {
      throw new RuntimeException("Invalid credentials");
    }

    return jwtUtil.generateRefreshToken(userId);
  }
  
}
