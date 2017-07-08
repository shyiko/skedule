package com.github.shyiko.skedule;

/**
 * Thrown in case of invalid schedule (during parsing, programmatic construction, etc).
 */
public class InvalidScheduleException extends RuntimeException {
    public InvalidScheduleException(String message) { super(message); }
}
