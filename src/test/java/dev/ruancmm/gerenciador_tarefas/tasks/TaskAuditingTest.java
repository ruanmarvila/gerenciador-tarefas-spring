package dev.ruancmm.gerenciador_tarefas.tasks;

import static dev.ruancmm.gerenciador_tarefas.support.AuthTestHelper.uniqueEmail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import dev.ruancmm.gerenciador_tarefas.config.FixedTimeConfig;
import dev.ruancmm.gerenciador_tarefas.config.PostgresContainerConfig;
import dev.ruancmm.gerenciador_tarefas.core.config.JpaAuditingConfig;
import dev.ruancmm.gerenciador_tarefas.support.MutableClock;
import dev.ruancmm.gerenciador_tarefas.users.User;
import dev.ruancmm.gerenciador_tarefas.users.UserRepository;
import jakarta.persistence.EntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresContainerConfig.class, JpaAuditingConfig.class, FixedTimeConfig.class})
public class TaskAuditingTest {

    @Autowired 
    TaskRepository repository;

    @Autowired
    UserRepository userRepository;

    @Autowired 
    MutableClock clock;

    @Autowired
    EntityManager em;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("Test", uniqueEmail(), "testtest"));
    }

    @Test
    void shouldFillBothDatesOnCreate() {
        Task task = repository.saveAndFlush(new Task(user, null, null));

        LocalDateTime expected = LocalDateTime.now(clock);
        assertEquals(expected, task.getCreatedAt());
        assertEquals(expected, task.getUpdatedAt());
    }

    @Test 
    void shouldOnlyUpdatedAtOnUpdate() {
        Task task = repository.saveAndFlush(new Task(user, null, null));
        LocalDateTime created = task.getCreatedAt();

        clock.advance(Duration.ofMinutes(10));
        task.setTitle("New Title");
        repository.saveAndFlush(task);
        em.clear();

        Task reloaded = repository.findById(task.getId()).orElseThrow();
        assertEquals(created, reloaded.getCreatedAt());
        assertEquals(created.plusMinutes(10), reloaded.getUpdatedAt());
    }

    @Test
    void shouldNotTouchUpdatedAtWhenNothingChanged() {
        Task task = repository.saveAndFlush(new Task(user, null, null));
        LocalDateTime  updated = task.getUpdatedAt();

        clock.advance(Duration.ofMinutes(10));
        repository.saveAndFlush(task);

        assertEquals(updated, task.getUpdatedAt());
    }

    @Test
    void softDeleteShouldAlsoUpdateUpdatedAt() {
        Task task = repository.saveAndFlush(new Task(user, null, null));
        LocalDateTime created = task.getCreatedAt();

        repository.delete(task);
        em.flush();
        em.clear();

        LocalDateTime updated = (LocalDateTime) em
            .createNativeQuery("SELECT updated_at FROM tasks WHERE id = ?1")
            .setParameter(1, task.getId())
            .getSingleResult();

        assertTrue(updated.isAfter(created));
    }
}
