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

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    @Query(
        value = "SELECT * FROM tasks WHERE id = :id AND user_id = :userId AND deleted_at IS NOT NULL",
        nativeQuery = true
    )
    Optional<Task> findDeletedByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query(
    value = "SELECT * FROM tasks WHERE user_id = :userId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC, id DESC",
    countQuery = "SELECT COUNT(*) FROM tasks WHERE user_id = :userId AND deleted_at IS NOT NULL",
    nativeQuery = true
    )
    Page<Task> findAllDeletedByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        value = "DELETE FROM tasks WHERE user_id = :userId AND deleted_at IS NOT NULL",
        nativeQuery = true
    )
    void deleteAllDeletedByUserId(@Param("userId") Long userId);
}
