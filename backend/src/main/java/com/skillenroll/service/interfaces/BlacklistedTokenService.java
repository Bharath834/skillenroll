package com.skillenroll.service.interfaces;

/**
 * Lifecycle of blacklisted JWT access tokens: adding a token on logout and
 * checking whether a presented token has been blacklisted.
 */
public interface BlacklistedTokenService {

    /**
     * Blacklists a JWT access token so it can no longer be used. Stored as a
     * SHA-256 hash with the token's natural expiry. Idempotent.
     *
     * @param token the raw JWT
     */
    void blacklist(String token);

    /**
     * Returns whether the given JWT has been blacklisted (e.g. via logout).
     *
     * @param token the raw JWT
     * @return {@code true} if the token's hash is present in the blacklist
     */
    boolean isBlacklisted(String token);
}
