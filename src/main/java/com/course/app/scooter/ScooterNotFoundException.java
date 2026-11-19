package com.course.app.scooter;

public class ScooterNotFoundException extends RuntimeException {

    public ScooterNotFoundException(Long id) {
        super("Scooter with id " + id + " was not found");
    }
}
