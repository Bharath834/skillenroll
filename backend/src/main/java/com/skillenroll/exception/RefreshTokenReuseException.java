package com.skillenroll.exception;

/**
 * Thrown when an already-rotated (revoked) refresh token is presented again.
 * Signals possible token theft; all of the user's active tokens are revoked.
 * Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class RefreshTokenReuseException extends RuntimeException {

    public RefreshTokenReuseException(String message) {
        super(message);
    }
}
