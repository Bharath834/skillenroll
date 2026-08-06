package com.skillenroll.courseservice.exception;

/**
 * Thrown when a unique constraint would be violated by creating a
 * duplicate resource (e.g. duplicate course title). Maps to HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
