package dev.ruancmm.gerenciador_tarefas.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import dev.ruancmm.gerenciador_tarefas.config.PostgresContainerConfig;
import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresContainerConfig.class)
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager em;

    @Test
    void shouldFindUserIncludingDeleted() {
        User user = new User("Test", "test@test.com", "testtest");
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        Optional<User> result = userRepository.findByEmailIncludingDeleted("test@test.com");

        assertTrue(result.isPresent());
        assertEquals(user.getEmail(), result.get().getEmail());
        assertNotNull(result.get().getDeletedAt());
    }

    @Test
    void shouldReturnEmptyWhenEmailDoesNotExist() {
        Optional<User> result = userRepository.findByEmailIncludingDeleted("not@exists.com");

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDeleteOnlyAccountsPastTheRecoveryWindow() {
        saveUser("expired@test.com", LocalDateTime.now().minusDays(31));
        saveUser("recent@test.com", LocalDateTime.now().minusDays(5));
        saveUser("active@test.com", null);
        em.flush();
        em.clear();

        int result = userRepository.deleteExpired(LocalDateTime.now().minusDays(30));

        assertEquals(1, result);
        assertEquals(0, countByEmail("expired@test.com"));
        assertEquals(1, countByEmail("recent@test.com"));
        assertEquals(1, countByEmail("active@test.com"));
    }

    private void saveUser(String email, LocalDateTime deletedAt) {
        User user = new User("Test", email, "testtest");
        user.setDeletedAt(deletedAt);
        userRepository.save(user);
    }

    private long countByEmail(String email) {
        return ((Number) em.createNativeQuery("SELECT COUNT(*) FROM users WHERE email = :email")
                .setParameter("email", email)
                .getSingleResult()).longValue();
    }
}
