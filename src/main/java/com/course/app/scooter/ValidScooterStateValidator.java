package com.course.app.scooter;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidScooterStateValidator implements ConstraintValidator<ValidScooterState, ScooterDto> {

    @Override
    public boolean isValid(ScooterDto scooterDto, ConstraintValidatorContext context) {
        if (scooterDto == null || scooterDto.status() != ScooterStatus.AVAILABLE) {
            return true;
        }
        return scooterDto.batteryLevel() != null && scooterDto.batteryLevel() >= 20;
    }
}
