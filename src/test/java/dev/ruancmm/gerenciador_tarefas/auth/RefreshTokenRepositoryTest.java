package dev.ruancmm.gerenciador_tarefas.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import dev.ruancmm.gerenciador_tarefas.config.PostgresContainerConfig;
import dev.ruancmm.gerenciador_tarefas.users.User;
import dev.ruancmm.gerenciador_tarefas.users.UserRepository;
import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresContainerConfig.class)
class RefreshTokenRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 5, 12, 0);

    @Autowired private RefreshTokenRepository repository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("Test", "test@test.com", "testtest"));
        otherUser = userRepository.save(new User("Other", "other@test.com", "testtest"));
    }

    @Test
    void shouldConsumeTokenOnlyOnce() {
        RefreshToken saved = token(user, "h1", UUID.randomUUID(), NOW.plusDays(1));

        assertEquals(1, repository.revokeIfActive(saved.getId(), NOW));
        assertEquals(0, repository.revokeIfActive(saved.getId(), NOW));
    }

    @Test
    void shouldRevokeOnlyTheGivenFamily() {
        UUID familyA = UUID.randomUUID();
        UUID familyB = UUID.randomUUID();
        token(user, "a1", familyA, NOW.plusDays(1));
        token(user, "a2", familyA, NOW.plusDays(1));
        token(user, "b1", familyB, NOW.plusDays(1));

        int result = repository.revokeFamily(familyA, NOW);

        assertEquals(2, result);
        assertNotNull(repository.findByTokenHash("a1").orElseThrow().getRevokedAt());
        assertNotNull(repository.findByTokenHash("a2").orElseThrow().getRevokedAt());
        assertNull(repository.findByTokenHash("b1").orElseThrow().getRevokedAt());
    }

    @Test
    void shouldNotCountTokensThatAreAlreadyRevoked() {
        UUID family = UUID.randomUUID();
        token(user, "c1", family, NOW.plusDays(1));

        assertEquals(1, repository.revokeFamily(family, NOW));
        assertEquals(0, repository.revokeFamily(family, NOW));
    }

    @Test
    void shouldRevokeAllTokensOfTheUserOnly() {
        token(user, "u1", UUID.randomUUID(), NOW.plusDays(1));
        token(user, "u2", UUID.randomUUID(), NOW.plusDays(1));
        token(otherUser, "o1", UUID.randomUUID(), NOW.plusDays(1));

        int result = repository.revokeAllByUserId(user.getId(), NOW);

        assertEquals(2, result);
        assertNull(repository.findByTokenHash("o1").orElseThrow().getRevokedAt());
    }

    @Test
    void shouldDeleteOnlyTokensExpiredBeforeTheLimit() {
        UUID family = UUID.randomUUID();
        token(user, "old", family, NOW.minusDays(10));
        token(user, "valid", family, NOW.plusDays(1));

        int result = repository.deleteExpiredBefore(NOW);
        em.clear();

        assertEquals(1, result);
        assertTrue(repository.findByTokenHash("old").isEmpty());
        assertTrue(repository.findByTokenHash("valid").isPresent());
    }

    private RefreshToken token(User owner, String hash, UUID family, LocalDateTime expiresAt) {
        return repository.save(new RefreshToken(owner.getId(), hash, family, expiresAt));
    }
}
