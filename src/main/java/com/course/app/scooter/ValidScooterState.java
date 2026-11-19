package com.course.app.scooter;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Constraint(validatedBy = ValidScooterStateValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidScooterState {

    String message() default "Available scooters must have a battery level of at least 20";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
