package dev.ruancmm.gerenciador_tarefas.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);
    private final RefreshTokenService refreshTokenService;

    public RefreshTokenCleanupJob(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "America/Sao_Paulo")
    public void purgeExpiredTokens() {
        int removed = refreshTokenService.purgeExpired();
        log.info("Removed {} expired refresh tokens", removed);
    }
}
