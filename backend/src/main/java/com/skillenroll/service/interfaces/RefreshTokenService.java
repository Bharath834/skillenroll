package com.skillenroll.service.interfaces;

import com.skillenroll.entity.RefreshToken;
import com.skillenroll.entity.User;

import java.util.Optional;

/**
 * Lifecycle of opaque, database-backed refresh tokens: issuing, rotation
 * (with reuse detection) and revocation.
 */
public interface RefreshTokenService {

    /**
     * Issues and persists a new refresh token for the given user.
     *
     * @param user the token owner
     * @return the raw (UUID) token value
     */
    String issueRefreshToken(User user);

    /**
     * Validates a refresh token before rotation.
     *
     * @param tokenValue the raw token value
     * @return the valid, non-revoked, non-expired token entity
     * @throws com.skillenroll.exception.InvalidRefreshTokenException  if unknown
     * @throws com.skillenroll.exception.RefreshTokenExpiredException  if expired
     * @throws com.skillenroll.exception.RefreshTokenReuseException    if already rotated
     */
    RefreshToken validateForRotation(String tokenValue);

    /**
     * Revokes a token (idempotent - already-revoked tokens stay revoked).
     *
     * @param token the token to revoke
     */
    void revoke(RefreshToken token);

    /**
     * Looks a token up by its raw value (used by logout for ownership checks).
     *
     * @param tokenValue the raw token value
     * @return the token if present
     */
    Optional<RefreshToken> findByToken(String tokenValue);
}
