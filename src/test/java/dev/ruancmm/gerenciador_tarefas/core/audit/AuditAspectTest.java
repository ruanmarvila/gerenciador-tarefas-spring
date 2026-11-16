package dev.ruancmm.gerenciador_tarefas.core.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;

class AuditAspectTest {

    static class SampleService {
        @Audited(action = "OK") 
        public void works() {}

        @Audited(action = "BOOM") 
        public void fails() { throw new IllegalStateException("secret detail"); }

        @Audited(action = "INNER")
        public void inner() {}

        public void outer() { this.inner(); }
    }

    private SampleService proxy() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new SampleService());
        factory.addAspect(new AuditAspect());
        return factory.getProxy();
    }

    @Test
    void shouldLogSucess() {
        try (var logs = new AuditLogCapture()) {
            proxy().works();
            assertEquals(List.of("action=OK actor=anonymous outcome=SUCESS"), logs.messages());
        }
    }

    @Test
    void shouldLogFailureWithoutMessageAndRethrow() {
        try(var logs = new AuditLogCapture()) {
            assertThrows(IllegalStateException.class, () -> proxy().fails());

            String line = logs.messages().get(0);
            assertTrue(line.contains("outcome=FAILED reason=IllegalStateException"));
            assertFalse(line.contains("secret detail"));
        }
    }

    @Test
    void shouldNotAuditSelfInvocation() {
        try (var logs = new AuditLogCapture()) {
            proxy().outer();
            assertTrue(logs.messages().isEmpty());
        }
    }
}