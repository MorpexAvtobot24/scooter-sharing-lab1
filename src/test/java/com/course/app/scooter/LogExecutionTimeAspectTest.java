package com.course.app.scooter;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogExecutionTimeAspectTest {

    private final LogExecutionTimeAspect aspect = new LogExecutionTimeAspect();

    @Test
    void measureExecutionTime_proceedsAndReturnsOriginalResult() throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        Object result = new Object();
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("ScooterService.findAll()");
        when(joinPoint.proceed()).thenReturn(result);

        Object actual = aspect.measureExecutionTime(joinPoint);

        assertSame(result, actual);
        verify(joinPoint).proceed();
    }
}
