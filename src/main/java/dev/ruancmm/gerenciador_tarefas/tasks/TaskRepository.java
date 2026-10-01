package dev.ruancmm.gerenciador_tarefas.tasks;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    @Query(
        value = "SELECT * FROM tasks WHERE id = :id AND deleted_at IS NOT NULL",
        nativeQuery = true
    )
    Optional<Task> findDeletedById(@Param("id") Long id);

    @Modifying
    @Query(
        value = "DELETE FROM tasks WHERE user_id = :userId AND deleted_at IS NOT NULL",
        nativeQuery = true
    )
    void deleteAllDeletedByUserId(@Param("userId") Long userId);
}
