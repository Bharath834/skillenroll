package com.skillenroll.exception;

/**
 * Thrown when a refresh token does not exist in the database. Mapped to
 * HTTP 401 by {@link GlobalExceptionHandler}.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
