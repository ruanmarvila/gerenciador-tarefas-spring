package dev.ruancmm.gerenciador_tarefas.auth;

import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import dev.ruancmm.gerenciador_tarefas.auth.exception.InvalidCredentialsException;
import dev.ruancmm.gerenciador_tarefas.users.User;
import dev.ruancmm.gerenciador_tarefas.users.UserService;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserCreateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.response.UserResponse;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserService userService;
  private final JwtUtil jwtUtil;
	
  public AuthService(AuthenticationManager authenticationManager, UserService userService, JwtUtil jwtUtil) {
      this.authenticationManager = authenticationManager;
      this.userService = userService;
      this.jwtUtil = jwtUtil;
	}

  public UserResponse register(UserCreateRequest request) {
    return userService.create(request);
  }

  public Map<String, String> login(String email, String password) {
    try {
      Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(email, password)
      );
  
      User user = (User) auth.getPrincipal();
  
      return Map.of(
        "accessToken", jwtUtil.generateAccessToken(user.getId()),
        "refreshToken", jwtUtil.generateRefreshToken(user.getId())
      );
    } catch (BadCredentialsException | UsernameNotFoundException e) {
      throw new InvalidCredentialsException();
    }
  }

  public String refresh(String refreshToken) {
    Long userId = jwtUtil.extractUserId(refreshToken, "refresh");

    if (userId == null) {
      throw new InvalidCredentialsException();
    }

    return jwtUtil.generateRefreshToken(userId);
  }
  
}
