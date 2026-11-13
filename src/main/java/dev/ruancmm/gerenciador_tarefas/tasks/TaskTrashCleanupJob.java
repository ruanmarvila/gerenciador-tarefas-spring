package dev.ruancmm.gerenciador_tarefas.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TaskTrashCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(TaskTrashCleanupJob.class);
    private final TaskService taskService;

    public TaskTrashCleanupJob(TaskService taskService) {
        this.taskService = taskService;
    }

    @Scheduled(cron = "0 0 3 30 * *", zone = "America/Sao_Paulo")
    public void purgeDeletedTasks() {
        int removed = taskService.purgeExpiredTrash();
        log.info("Removed {} expired tasks from trash", removed);
    }
}
