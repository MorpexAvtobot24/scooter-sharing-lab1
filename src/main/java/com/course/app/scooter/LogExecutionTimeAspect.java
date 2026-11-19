package com.course.app.scooter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogExecutionTimeAspect {

    private static final Logger log = LoggerFactory.getLogger(LogExecutionTimeAspect.class);

    @Around("@annotation(com.course.app.scooter.LogExecutionTime)")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedNanos = System.nanoTime() - start;
            log.info("{} completed in {} ns", joinPoint.getSignature().toShortString(), elapsedNanos);
        }
    }
}
