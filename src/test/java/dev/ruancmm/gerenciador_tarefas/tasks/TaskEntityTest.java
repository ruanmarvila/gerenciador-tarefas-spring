package dev.ruancmm.gerenciador_tarefas.tasks;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import dev.ruancmm.gerenciador_tarefas.users.User;

@ExtendWith(MockitoExtension.class)
public class TaskEntityTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("Test", "test@test.com", "testtest");
        user.setId(1L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldUseDefaultTitleWhenTitleIsBlank(String title) {
        Task task = new Task(user, title, "description");

        assertEquals("title", task.getTitle());
    }

    @Test
    void shouldKeepProvidedTitle() {
        Task task = new Task(user, "Test", "description");

        assertEquals("Test", task.getTitle());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void shouldUseDefaultDescriptionWhenDescriptionIsBlank(String description) {
        Task task = new Task(user, "title", description);

        assertEquals("description", task.getDescription());
    }

    @Test
    void shouldKeepProvidedDescription() {
        Task task = new Task(user, "title", "Test");

        assertEquals("Test", task.getDescription());
    }
}
