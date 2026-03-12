package openclaw.server.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Audit Aspect - High Priority Improvement
 *
 * <p>Records all sensitive operations for security auditing.</p>
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuditAspect.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");

    private final CopyOnWriteArrayList<Consumer<AuditEvent>> auditListeners = new CopyOnWriteArrayList<>();
    private final Map<String, AuditEntry> pendingAudits = new ConcurrentHashMap<>();

    /**
     * Pointcut for all controller methods
     */
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerMethods() {}

    /**
     * Pointcut for sensitive operations
     */
    @Pointcut("@annotation(Audited)")
    public void auditedMethods() {}

    /**
     * Pointcut for agent operations
     */
    @Pointcut("execution(* openclaw.server.controller.AgentController.*(..))")
    public void agentOperations() {}

    /**
     * Pointcut for gateway operations
     */
    @Pointcut("execution(* openclaw.server.controller.GatewayController.*(..))")
    public void gatewayOperations() {}

    /**
     * Audit around advice
     */
    @Around("controllerMethods() || auditedMethods() || agentOperations() || gatewayOperations()")
    public Object auditAround(ProceedingJoinPoint pjp) throws Throwable {
        String auditId = UUID.randomUUID().toString();
        Instant startTime = Instant.now();

        // Get request info
        HttpServletRequest request = getCurrentRequest();
        String user = getCurrentUser();
        String ip = getClientIp(request);
        String method = pjp.getSignature().getName();
        String target = pjp.getTarget().getClass().getSimpleName();

        // Create audit entry
        AuditEntry entry = new AuditEntry(
                auditId,
                user,
                ip,
                target + "." + method,
                maskSensitiveData(Arrays.toString(pjp.getArgs())),
                startTime
        );
        pendingAudits.put(auditId, entry);

        // Log start
        auditLogger.info("[START] {} - User: {}, IP: {}, Method: {}, Args: {}",
                auditId, user, ip, method, entry.args());

        try {
            // Execute method
            Object result = pjp.proceed();

            // Success
            Instant endTime = Instant.now();
            long duration = endTime.toEpochMilli() - startTime.toEpochMilli();

            AuditEvent event = new AuditEvent(
                    auditId,
                    user,
                    ip,
                    target + "." + method,
                    entry.args(),
                    "SUCCESS",
                    null,
                    startTime,
                    endTime,
                    duration
            );

            logAudit(event);
            notifyListeners(event);

            return result;

        } catch (Exception e) {
            // Failure
            Instant endTime = Instant.now();
            long duration = endTime.toEpochMilli() - startTime.toEpochMilli();

            AuditEvent event = new AuditEvent(
                    auditId,
                    user,
                    ip,
                    target + "." + method,
                    entry.args(),
                    "FAILURE",
                    e.getMessage(),
                    startTime,
                    endTime,
                    duration
            );

            logAudit(event);
            notifyListeners(event);

            throw e;
        } finally {
            pendingAudits.remove(auditId);
        }
    }

    /**
     * Audit after throwing
     */
    @AfterThrowing(pointcut = "controllerMethods() || auditedMethods()", throwing = "ex")
    public void auditAfterThrowing(JoinPoint jp, Exception ex) {
        // Already handled in around advice
    }

    /**
     * Add audit listener
     */
    public void addAuditListener(Consumer<AuditEvent> listener) {
        auditListeners.add(listener);
    }

    /**
     * Remove audit listener
     */
    public void removeAuditListener(Consumer<AuditEvent> listener) {
        auditListeners.remove(listener);
    }

    private void logAudit(AuditEvent event) {
        if ("SUCCESS".equals(event.status())) {
            auditLogger.info("[{}] {} - User: {}, IP: {}, Method: {}, Duration: {}ms",
                    event.status(), event.auditId(), event.user(), event.ip(),
                    event.method(), event.durationMs());
        } else {
            auditLogger.warn("[{}] {} - User: {}, IP: {}, Method: {}, Error: {}, Duration: {}ms",
                    event.status(), event.auditId(), event.user(), event.ip(),
                    event.method(), event.error(), event.durationMs());
        }
    }

    private void notifyListeners(AuditEvent event) {
        for (Consumer<AuditEvent> listener : auditListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.error("Audit listener error: {}", e.getMessage());
            }
        }
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }

    private String maskSensitiveData(String data) {
        if (data == null) {
            return "null";
        }

        // Mask API keys
        String masked = data.replaceAll("(api[_-]?key[=:]\\s*['\"]?)[^'\"&\\s]+", "$1***MASKED***");

        // Mask passwords
        masked = masked.replaceAll("(password[=:]\\s*['\"]?)[^'\"&\\s]+", "$1***MASKED***");

        // Mask tokens
        masked = masked.replaceAll("(token[=:]\\s*['\"]?)[^'\"&\\s]+", "$1***MASKED***");

        // Limit length
        if (masked.length() > 500) {
            masked = masked.substring(0, 500) + "... [truncated]";
        }

        return masked;
    }

    /**
     * Get pending audit count
     */
    public int getPendingAuditCount() {
        return pendingAudits.size();
    }

    /**
     * Get audit statistics
     */
    public AuditStats getStats() {
        return new AuditStats(
                auditListeners.size(),
                pendingAudits.size()
        );
    }

    // Records
    private record AuditEntry(
            String auditId,
            String user,
            String ip,
            String method,
            String args,
            Instant startTime
    ) {}

    public record AuditEvent(
            String auditId,
            String user,
            String ip,
            String method,
            String args,
            String status,
            String error,
            Instant startTime,
            Instant endTime,
            long durationMs
    ) {}

    public record AuditStats(
            int listenerCount,
            int pendingCount
    ) {}

    /**
     * Audited annotation
     */
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
    public @interface Audited {
        String value() default "";
    }
}
