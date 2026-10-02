package dev.ruancmm.gerenciador_tarefas.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindUserIncludingDeleted() {
        User user = new User("Test", "test@test.com", "testtest");
        user.setDeletedAt(LocalDateTime.now());

        userRepository.save(user);

        Optional<User> result = userRepository.findByEmailIncludingDeleted("test@test.com");

        assertTrue(result.isPresent());
        assertEquals(user.getEmail(), result.get().getEmail());
    }

    @Test
    void shouldReturnEmptyWhenEmailDoesNotExist() {
        Optional<User> result = userRepository.findByEmailIncludingDeleted("not@exists.com");

        assertTrue(result.isEmpty());
    }
}
