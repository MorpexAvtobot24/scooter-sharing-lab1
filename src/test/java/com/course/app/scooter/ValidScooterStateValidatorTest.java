package com.course.app.scooter;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidScooterStateValidatorTest {

    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void validate_availableScooterWithBatteryAtLeastTwenty_isValid() {
        ScooterDto dto = dto(ScooterStatus.AVAILABLE, 20);

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void validate_availableScooterWithLowBattery_hasDescriptiveViolation() {
        ScooterDto dto = dto(ScooterStatus.AVAILABLE, 19);

        var violations = validator.validate(dto);

        assertEquals(1, violations.size());
        assertEquals(
                "Available scooters must have a battery level of at least 20",
                violations.iterator().next().getMessage());
    }

    @Test
    void validate_inUseScooterWithLowBattery_isValidForStateConstraint() {
        assertTrue(validator.validate(dto(ScooterStatus.IN_USE, 5)).isEmpty());
    }

    private static ScooterDto dto(ScooterStatus status, Integer battery) {
        return new ScooterDto(null, "SC-1", "Model X", battery, new BigDecimal("0.25"), status);
    }
}
