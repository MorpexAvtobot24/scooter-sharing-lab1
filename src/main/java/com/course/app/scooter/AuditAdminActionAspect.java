package com.course.app.scooter;

import com.course.app.security.CurrentUser;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAdminActionAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAdminActionAspect.class);

    @AfterReturning("@annotation(com.course.app.scooter.AuditAdminAction)")
    public void auditAdminAction(JoinPoint joinPoint) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        String actor = principal instanceof CurrentUser currentUser
                ? currentUser.username()
                : authentication == null ? "anonymous" : authentication.getName();
        log.info("Admin action {} by {} completed successfully",
                joinPoint.getSignature().toShortString(), actor);
    }
}
