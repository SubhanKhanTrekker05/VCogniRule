package com.ooad.mhs.service;

/**
 * Thrown by a service when input breaks a business rule that needs the database to check,
 * e.g. a rule ID that already exists or a field that does not exist.
 *
 * Extends RuntimeException (an unchecked exception), so it does not have to be declared on
 * every method. The controller catches it and shows its message to the admin.
 */
public class InvalidInputException extends RuntimeException {

    /** @param message the explanation shown to the user */
    public InvalidInputException(String message) {
        super(message);
    }
}
