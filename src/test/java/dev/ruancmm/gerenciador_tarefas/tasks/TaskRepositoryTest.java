package dev.ruancmm.gerenciador_tarefas.tasks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskFilterRequest;
import dev.ruancmm.gerenciador_tarefas.users.User;
import dev.ruancmm.gerenciador_tarefas.users.UserRepository;

@DataJpaTest
public class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private User anotherUser;

    @BeforeEach
    void setUp() {
        user = userRepository.save(
            new User("Test", "test@test.com", "testtest")
        );

        anotherUser = userRepository.save(
            new User("Another", "another@gmail", "anotherPassword")
        );
    }

    @Test
    void shouldFindDeletedTaskById() {
        Task task = new Task(user, null, null);
        task.setDeletedAt(LocalDateTime.now());

        taskRepository.save(task);

        Optional<Task> result = taskRepository.findDeletedById(task.getId());

        assertTrue(result.isPresent());
        assertEquals(task.getTitle(), result.get().getTitle());
    }

    @Test
    void shouldReturnEmptyWhenTaskDoesNotExist() {
        Optional<Task> result = taskRepository.findDeletedById(123L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDeleteAllDeletedTasksByUserId() {
        Task task = new Task(user, null, null);
        task.setDeletedAt(LocalDateTime.now());

        taskRepository.save(task);

        taskRepository.deleteAllDeletedByUserId(user.getId());

        Optional<Task> result = taskRepository.findDeletedById(task.getId());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotDeleteDeletedTasksFromAnotherUser() {
        Task task = new Task(anotherUser, null, null);
        task.setDeletedAt(LocalDateTime.now());

        taskRepository.save(task);

        taskRepository.deleteAllDeletedByUserId(user.getId());

        Optional<Task> result = taskRepository.findDeletedById(task.getId());

        assertTrue(result.isPresent());
        assertEquals(anotherUser.getId(), result.get().getUser().getId());
    }

    @Test
    void shouldNotDeleteActiveTasks() {
        Task task = new Task(user, null, null);

        taskRepository.save(task);

        taskRepository.deleteAllDeletedByUserId(user.getId());

        Optional<Task> result = taskRepository.findById(task.getId());

        assertTrue(result.isPresent());
        assertEquals(user.getId(), result.get().getUser().getId());
    }

    @Test
    void shouldFindOnlyCurrentUserTasksWhenFilterIsEmpty() {
        Task userTask = new Task(user, "User task", null);
        Task anotherUserTask = new Task(anotherUser, "Another task", null);

        taskRepository.saveAll(List.of(userTask, anotherUserTask));

        TaskFilterRequest filter = new TaskFilterRequest(null, null, null);

        List<Task> result = taskRepository.findAll(TaskSpecification.withFilter(filter, user.getId()));

        assertEquals(1, result.size());
        assertEquals(user.getId(), result.get(0).getUser().getId());
    }

    @Test
    void shouldFilterTasksByTitle() {
        Task task1 = new Task(user, "Study Java", null);
        Task task2 = new Task(user, "Watch Anime", null);

        taskRepository.saveAll(List.of(task1, task2));

        TaskFilterRequest filter = new TaskFilterRequest("Jav", null, null);

        List<Task> reusult = taskRepository.findAll(TaskSpecification.withFilter(filter, user.getId()));

        assertEquals(1, reusult.size());
        assertEquals(task1.getTitle(), reusult.get(0).getTitle());
    }

    @Test
    void shouldFilterTasksByDescription() {
        Task task1 = new Task(user, null, "watch a video about the Korean war");
        Task task2 = new Task(user, null, "to go vote in the election");

        taskRepository.saveAll(List.of(task1, task2));

        TaskFilterRequest filter = new TaskFilterRequest(null, "Korea", null);

        List<Task> reusult = taskRepository.findAll(TaskSpecification.withFilter(filter, user.getId()));

        assertEquals(1, reusult.size());
        assertEquals(task1.getDescription(), reusult.get(0).getDescription());
    }

    @Test
    void shouldFilterTasksByStatus() {
        Task task1 = new Task(user, null, null);
        Task task2 = new Task(user, null, null);

        task2.setStatus(TaskStatus.DONE);

        taskRepository.saveAll(List.of(task1, task2));

        TaskFilterRequest filter = new TaskFilterRequest(null, null, TaskStatus.DONE);

        List<Task> reusult = taskRepository.findAll(TaskSpecification.withFilter(filter, user.getId()));

        assertEquals(1, reusult.size());
        assertEquals(task2.getStatus(), reusult.get(0).getStatus());
    }

    @Test 
    void shouldReturnOnlyDeletedTasks() {
        Task task1 = new Task(user, "Domestic task", "clean the house");
        Task task2 = new Task(user, "Book", "read Romeo and Juliet");

        task2.setDeletedAt(LocalDateTime.now());

        taskRepository.saveAll(List.of(task1, task2));

        Pageable pageable = PageRequest.of(0, 10);

        Page<Task> result = taskRepository.findAllDeletedByUserId(user.getId(), pageable);

        assertEquals(1, result.getNumberOfElements());

        Task found = result.getContent().get(0);

        assertEquals(task2.getId(), found.getId());
        assertEquals(task2.getDeletedAt(), found.getDeletedAt());
    }
}
