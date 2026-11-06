package dev.ruancmm.gerenciador_tarefas.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.ruancmm.gerenciador_tarefas.auth.RotationResult.Rejected.Reason;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    RefreshTokenRepository repository;

    Clock clock = Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);
    RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(repository, clock, Duration.ofDays(7));
    }

    @Test
    void shouldReturnIssueForNewSession() {
        String result = service.issueForNewSession(1L);

        assertFalse(result.isBlank());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertEquals(TokenHasher.sha256(result), saved.getTokenHash());
        assertEquals(1L, saved.getUserId());
        assertEquals(LocalDateTime.now(clock).plusDays(7L), saved.getExpiresAt());

    }

    @Test
    void shouldReturnNewRaw() {
        UUID family = UUID.randomUUID();
        RefreshToken refresh = new RefreshToken(
            1L, 
            TokenHasher.sha256("abc"), 
            family,
            LocalDateTime.now(clock).plusSeconds(60)
        );

        when(repository.findByTokenHash(TokenHasher.sha256("abc")))
            .thenReturn(Optional.of(refresh));

        when(repository.revokeIfActive(eq(refresh.getId()), any(LocalDateTime.class)))
            .thenReturn(1);

        RotationResult result = service.rotate("abc");

        RotationResult.Rotated rotated = 
            assertInstanceOf(RotationResult.Rotated.class, result);
        assertEquals(1L, rotated.userId());
        assertFalse(rotated.newRawToken().isBlank());
        assertNotEquals("abc", rotated.newRawToken());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertEquals(TokenHasher.sha256(rotated.newRawToken()), saved.getTokenHash());
        assertEquals(family, saved.getFamilyId());
        assertEquals(1L, saved.getUserId());
        assertEquals(LocalDateTime.now(clock).plusDays(7L), saved.getExpiresAt());
    }

    @Test
    void shouldRejectWhenTokenNotFound() {
        when(repository.findByTokenHash(TokenHasher.sha256("abc")))
            .thenReturn(Optional.empty());
        
        RotationResult result = service.rotate("abc");

        assertEquals(result, new RotationResult.Rejected(Reason.UNKNOWN));
        verify(repository, never()).save(any());
    }

    @Test
    void shouldRejectWhenTokenWasConsumedByAnotherRequest() {
        UUID family = UUID.randomUUID();
        RefreshToken consumed = new RefreshToken(
            1L, 
            TokenHasher.sha256("abc"), 
            family,
            LocalDateTime.now(clock).plusSeconds(60)
        );

        when(repository.findByTokenHash(TokenHasher.sha256("abc")))
            .thenReturn(Optional.of(consumed));

        when(repository.revokeIfActive(eq(consumed.getId()), any(LocalDateTime.class)))
            .thenReturn(0);

        RotationResult result = service.rotate("abc");

        assertEquals(result, new RotationResult.Rejected(Reason.REUSE_DETECTED));
        verify(repository).revokeFamily(family, LocalDateTime.now(clock));
    }

    @Test
    void shouldRevokeFamilyWhenRevokedTokenIsReused() {
        UUID family = UUID.randomUUID();
        RefreshToken revoked = new RefreshToken(
            1L, 
            TokenHasher.sha256("abc"), 
            family,
            LocalDateTime.now(clock).plusSeconds(60)
        );
        revoked.revoke(LocalDateTime.now(clock).minusSeconds(10));
        
        when(repository.findByTokenHash(TokenHasher.sha256("abc")))
            .thenReturn(Optional.of(revoked));

        RotationResult result = service.rotate("abc");

        assertEquals(result, new RotationResult.Rejected(Reason.REUSE_DETECTED));
        verify(repository).revokeFamily(family, LocalDateTime.now(clock));
    }

    @Test
    void shouldRejectWhenTokenIsExpired() {
        UUID family = UUID.randomUUID();
        RefreshToken expired = new RefreshToken(
            1L, 
            TokenHasher.sha256("abc"), 
            family,
            LocalDateTime.now(clock).plusSeconds(60)
        );
        expired.setExpiresAt(LocalDateTime.now(clock).minusSeconds(10));

        when(repository.findByTokenHash(TokenHasher.sha256("abc")))
            .thenReturn(Optional.of(expired));
        
        RotationResult result = service.rotate("abc");

        assertEquals(result, new RotationResult.Rejected(Reason.EXPIRED));
        verify(repository, never()).revokeIfActive(any(), any());
    }

    @Test
    void shouldRevokeToken() {
        UUID family = UUID.randomUUID();
        RefreshToken token = new RefreshToken(
            1L, 
            TokenHasher.sha256("abc"), 
            family,
            LocalDateTime.now(clock).plusSeconds(60)
        );

        when(repository.findByTokenHash(eq(token.getTokenHash())))
            .thenReturn(Optional.of(token));

        service.revoke("abc");

        assertNotNull(token.getRevokedAt());
        verify(repository).findByTokenHash(eq(token.getTokenHash()));
    }

    @Test
    void shouldDelegateRevokeAllForUserToRepository() {
        service.revokeAllForUser(1L);

        verify(repository).revokeAllByUserId(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void shouldPurgeExpired() {

        when(repository.deleteExpiredBefore(any()))
            .thenReturn(1);

        int result = service.purgeExpired();

        assertEquals(1, result);
        verify(repository).deleteExpiredBefore(any());
    }
}
