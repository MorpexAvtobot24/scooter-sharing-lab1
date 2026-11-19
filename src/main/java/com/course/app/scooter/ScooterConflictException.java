package com.course.app.scooter;

public class ScooterConflictException extends RuntimeException {

    public ScooterConflictException(String serialNumber) {
        super("A scooter with serial number " + serialNumber + " already exists");
    }
}
