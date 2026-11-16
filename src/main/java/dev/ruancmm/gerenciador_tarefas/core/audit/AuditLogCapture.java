package dev.ruancmm.gerenciador_tarefas.core.audit;

import java.util.List;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

final class AuditLogCapture implements AutoCloseable {

    private final Logger logger = (Logger) LoggerFactory.getLogger("AUDIT");
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    AuditLogCapture() {
        appender.start();
        logger.addAppender(appender);
    }

    List<String> messages() {
        return appender.list.stream()
            .map(event -> event.getFormattedMessage())
            .toList();
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
    }

}
