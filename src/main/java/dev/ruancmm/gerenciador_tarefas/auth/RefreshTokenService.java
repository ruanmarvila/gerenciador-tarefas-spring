package dev.ruancmm.gerenciador_tarefas.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.ruancmm.gerenciador_tarefas.auth.RotationResult.Rejected.Reason;

@Service
public class RefreshTokenService {

    private static final Duration PURGE_RETENTION = Duration.ofDays(7);
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final Clock clock;
    private final Duration refreshExpiration;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
        RefreshTokenRepository repository, 
        Clock clock, 
        @Value("${jwt.refresh-expiration}") Duration refreshExpiration
    ) {
        this.repository = repository;
        this.clock = clock;
        this.refreshExpiration = refreshExpiration;
    }

    @Transactional
    public String issueForNewSession(Long userId) {
        return issue(userId, UUID.randomUUID());
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        LocalDateTime now = LocalDateTime.now(clock);

        Optional<RefreshToken> found = repository.findByTokenHash(TokenHasher.sha256(rawToken));

        if (found.isEmpty()) {
            return new RotationResult.Rejected(Reason.UNKNOWN);
        }

        RefreshToken current = found.get();

        if (current.isExpired(now)) {
            return new RotationResult.Rejected(Reason.EXPIRED);
        }

        if (repository.revokeIfActive(current.getId(), now) == 0) {
            repository.revokeFamily(current.getFamilyId(), now);
            log.warn("Refresh token reuse detected for user {}", current.getUserId());
            return new RotationResult.Rejected(Reason.REUSE_DETECTED);
        }

        String newRaw = issue(current.getUserId(), current.getFamilyId());
        return new RotationResult.Rotated(current.getUserId(), newRaw);
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(TokenHasher.sha256(rawToken))
            .ifPresent(token -> token.revoke(LocalDateTime.now(clock)));
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        repository.revokeAllByUserId(userId, LocalDateTime.now(clock));
    }

    @Transactional
    int purgeExpired() {
        return repository.deleteExpiredBefore(LocalDateTime.now(clock).minus(PURGE_RETENTION));
    }

    private String issue(Long userId, UUID familyId) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        repository.save(new RefreshToken(
            userId,
            TokenHasher.sha256(raw),
            familyId,
            LocalDateTime.now(clock).plus(refreshExpiration)
        ));

        return raw;
    }
}
