package dev.ruancmm.gerenciador_tarefas.tasks;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query(
    value = "SELECT * FROM tasks WHERE user_id = :userId AND deleted_at IS NOT NULL",
    countQuery = "SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND deleted_at IS NOT NULL",
    nativeQuery = true
    )
    Page<Task> findAllDeletedByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query(
        value = "DELETE FROM tasks WHERE user_id = :userId AND deleted_at IS NOT NULL",
        nativeQuery = true
    )
    void deleteAllDeletedByUserId(@Param("userId") Long userId);
}
