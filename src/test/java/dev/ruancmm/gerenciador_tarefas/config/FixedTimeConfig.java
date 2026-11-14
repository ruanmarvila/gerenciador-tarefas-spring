package dev.ruancmm.gerenciador_tarefas.config;

import java.time.Instant;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import dev.ruancmm.gerenciador_tarefas.support.MutableClock;

@TestConfiguration
public class FixedTimeConfig {

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC);
    }
}
