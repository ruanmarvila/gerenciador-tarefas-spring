package dev.ruancmm.gerenciador_tarefas.auth;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import dev.ruancmm.gerenciador_tarefas.auth.exception.AccountAlreadyActivateException;
import dev.ruancmm.gerenciador_tarefas.auth.exception.AccountDisabledException;
import dev.ruancmm.gerenciador_tarefas.auth.exception.InvalidCredentialsException;
import dev.ruancmm.gerenciador_tarefas.users.User;
import dev.ruancmm.gerenciador_tarefas.users.UserRepository;
import dev.ruancmm.gerenciador_tarefas.users.UserService;
import dev.ruancmm.gerenciador_tarefas.users.dto.request.UserCreateRequest;
import dev.ruancmm.gerenciador_tarefas.users.dto.response.UserResponse;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final UserService userService;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
	
  public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
      this.authenticationManager = authenticationManager;
      this.userRepository = userRepository;
      this.userService = userService;
      this.passwordEncoder = passwordEncoder;
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
      verifyPendingRestore(email, password);
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

  public Map<String, String> restoreAndLogin(String email, String password) {
    User user = userRepository.findByEmailIncludingDeleted(email).
      orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new InvalidCredentialsException();
    }

    if (user.getDeletedAt() == null) {
      throw new AccountAlreadyActivateException();
    }

    if (Duration.between(user.getDeletedAt(), LocalDateTime.now()).toDays() > 30) {
      throw new InvalidCredentialsException();
    }

    user.setDeletedAt(null);
    userRepository.save(user);

    return Map.of(
        "accessToken", jwtUtil.generateAccessToken(user.getId()),
        "refreshToken", jwtUtil.generateRefreshToken(user.getId())
      );
  }
  
  private void verifyPendingRestore(String email, String password) {
    userRepository.findByEmailIncludingDeleted(email).ifPresent(user -> {
      boolean rightPassword = passwordEncoder.matches(password, user.getPassword());
      boolean isDeleted = user.getDeletedAt() != null;

      if (rightPassword && isDeleted) {
        if (Duration.between(user.getDeletedAt(), LocalDateTime.now()).toDays() <= 30) {
          throw new AccountDisabledException();
        }
      }
    });
  }
}
