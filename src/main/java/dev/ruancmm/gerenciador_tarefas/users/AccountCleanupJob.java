package dev.ruancmm.gerenciador_tarefas.users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AccountCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AccountCleanupJob.class);
    private final UserService userService;

    public AccountCleanupJob(UserService userService) {
        this.userService = userService;
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "America/Sao_Paulo")
    public void purgeExpiredAccounts() {
        int removed = userService.purgeExpiredAccounts();
        log.info("Removed {} expired accounts", removed);
    }
}
