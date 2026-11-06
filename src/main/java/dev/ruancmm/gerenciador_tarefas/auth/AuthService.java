package dev.ruancmm.gerenciador_tarefas.auth;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.ruancmm.gerenciador_tarefas.auth.dto.request.RefreshTokenRequest;
import dev.ruancmm.gerenciador_tarefas.auth.dto.response.TokenResponse;
import dev.ruancmm.gerenciador_tarefas.auth.exception.AccountAlreadyActivateException;
import dev.ruancmm.gerenciador_tarefas.auth.exception.AccountDisabledException;
import dev.ruancmm.gerenciador_tarefas.auth.exception.InvalidTokenException;
import dev.ruancmm.gerenciador_tarefas.core.exception.AuthenticationException;
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
  private final RefreshTokenService refreshTokenService;
	
  public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
      this.authenticationManager = authenticationManager;
      this.userRepository = userRepository;
      this.userService = userService;
      this.passwordEncoder = passwordEncoder;
      this.jwtUtil = jwtUtil;
      this.refreshTokenService = refreshTokenService;
	}

  public UserResponse register(UserCreateRequest request) {
    return userService.create(request);
  }

  public TokenResponse login(String email, String password) {
    try {
      Authentication auth = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(email, password)
      );
  
      User user = (User) auth.getPrincipal();
  
      return new TokenResponse(
        jwtUtil.generateAccessToken(user.getId()),
        refreshTokenService.issueForNewSession(user.getId())
      );
    } catch (BadCredentialsException | UsernameNotFoundException e) {
      verifyPendingRestore(email, password);
      throw new AuthenticationException();
    }
  }

  public TokenResponse refresh(RefreshTokenRequest request) {
    return switch (refreshTokenService.rotate(request.refreshToken())) {
      case RotationResult.Rotated r -> new TokenResponse(
        jwtUtil.generateAccessToken(r.userId()), r.newRawToken()
      );
      case RotationResult.Rejected r -> throw new InvalidTokenException("Invalid token");
    };
  }

  @Transactional
  public TokenResponse restoreAndLogin(String email, String password) {
    User user = userRepository.findByEmailIncludingDeleted(email).
      orElseThrow(AuthenticationException::new);

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new AuthenticationException();
    }

    if (user.getDeletedAt() == null) {
      throw new AccountAlreadyActivateException();
    }

    if (Duration.between(user.getDeletedAt(), LocalDateTime.now()).toDays() > 30) {
      throw new AuthenticationException();
    }

    user.setDeletedAt(null);

    return new TokenResponse(
      jwtUtil.generateAccessToken(user.getId()),
      refreshTokenService.issueForNewSession(user.getId())
    );
  }

  public void logout(RefreshTokenRequest request) {
    refreshTokenService.revoke(request.refreshToken());
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
