package com.moneytransfer.audit;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {
    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @AfterReturning("@annotation(auditable)")
    public void logAudit(JoinPoint joinPoint, Auditable auditable) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            var request = attributes.getRequest();
            var auth = (org.springframework.security.core.Authentication) request.getUserPrincipal();
            if (auth == null) return;
            var details = auth.getDetails();
            if (details instanceof io.jsonwebtoken.Claims claims) {
                Long userId = ((Integer) claims.get("userId")).longValue();
                String ip = request.getRemoteAddr();
                String detail = String.format("Executed %s with args: %s",
                        joinPoint.getSignature().getName(), java.util.Arrays.toString(joinPoint.getArgs()));
                auditLogRepository.save(new AuditLog(userId, auditable.action(), detail, ip));
            }
        } catch (Exception ignored) {}
    }
}
