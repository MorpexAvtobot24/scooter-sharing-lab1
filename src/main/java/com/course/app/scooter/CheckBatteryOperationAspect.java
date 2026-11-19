package com.course.app.scooter;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CheckBatteryOperationAspect {

    @Before("@annotation(com.course.app.scooter.CheckBatteryOperation)")
    public void checkBatteryBeforeOperation(JoinPoint joinPoint) {
        for (Object argument : joinPoint.getArgs()) {
            if (argument instanceof ScooterDto scooterDto
                    && scooterDto.status() == ScooterStatus.AVAILABLE
                    && (scooterDto.batteryLevel() == null || scooterDto.batteryLevel() < 20)) {
                throw new IllegalArgumentException(
                        "Available scooters must have a battery level of at least 20");
            }
        }
    }
}
