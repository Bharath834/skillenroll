package com.skillenroll.exception;

/**
 * Thrown when a refresh token has passed its expiry. Mapped to HTTP 401 by
 * {@link GlobalExceptionHandler}.
 */
public class RefreshTokenExpiredException extends RuntimeException {

    public RefreshTokenExpiredException(String message) {
        super(message);
    }
}
