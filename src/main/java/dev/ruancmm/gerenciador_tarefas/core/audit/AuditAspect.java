package dev.ruancmm.gerenciador_tarefas.core.audit;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import dev.ruancmm.gerenciador_tarefas.users.User;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    @Around("@annotation(dev.ruancmm.gerenciador_tarefas.core.audit.Audited)")
    public Object audit(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        Audited audited = AnnotatedElementUtils.findMergedAnnotation(method, Audited.class);
        String action = audited != null ? audited.action() : method.getName();
        String actor = resolveActor();
        try {
            Object result = pjp.proceed();
            log.info("action={} actor={} outcome=SUCESS", action, actor);
            return result;
        } catch (Throwable ex) {
            log.warn("action={} actor={} outcome=FAILED reason={}", 
                action, actor, ex.getClass().getSimpleName());
            throw ex;
        }
    }

    private String resolveActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return String.valueOf(user.getId());
        }
        return "anonymous";
    }
}
