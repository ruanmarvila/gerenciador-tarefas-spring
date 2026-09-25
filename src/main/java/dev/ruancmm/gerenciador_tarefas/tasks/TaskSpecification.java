package dev.ruancmm.gerenciador_tarefas.tasks;

import org.springframework.data.jpa.domain.Specification;

import dev.ruancmm.gerenciador_tarefas.tasks.dto.request.TaskFilterRequest;

public class TaskSpecification {

    public static Specification<Task> withFilter(TaskFilterRequest filter, Long id) {
        return Specification
            .where(hasUserId(id))
            .and(titleContains(filter.title()))
            .and(descriptionContains(filter.description()))
            .and(hasStatus(filter.status()));
    }

    private static Specification<Task> hasUserId(Long userId) {
        return (root, query, cb) -> {
            return cb.equal(root.get("user").get("id"), userId);
        };
    }

    private static Specification<Task> titleContains(String title) {
        return (root, query, cb) -> {
            if (title == null || title.isBlank()) {
                return null;
            }

            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
        };
    }

    private static Specification<Task> descriptionContains(String description) {
        return (root, query, cb) -> {
            if (description == null || description.isBlank()) {
                return null;
            }

            return cb.like(cb.lower(root.get("description")), "%" + description.toLowerCase() + "%");
        };
    }

    private static Specification<Task> hasStatus(TaskStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }

            return cb.equal(root.get("status"), status);
        };
    }
}
