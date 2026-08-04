package com.skillenroll.service.impl;

import com.skillenroll.entity.RefreshToken;
import com.skillenroll.entity.User;
import com.skillenroll.exception.InvalidRefreshTokenException;
import com.skillenroll.exception.RefreshTokenExpiredException;
import com.skillenroll.exception.RefreshTokenReuseException;
import com.skillenroll.repository.RefreshTokenRepository;
import com.skillenroll.security.config.JwtProperties;
import com.skillenroll.service.interfaces.RefreshTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implements the refresh-token lifecycle. Tokens are opaque UUIDs persisted
 * in the database so they can be individually revoked (logout) and rotated
 * (every refresh invalidates the previous token).
 */
@Service
@Transactional(readOnly = true)
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Override
    @Transactional
    public String issueRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(jwtProperties.getRefreshExpirationMs())))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken).getToken();
    }

    @Override
    @Transactional
    public RefreshToken validateForRotation(String tokenValue) {
        RefreshToken token = findByToken(tokenValue)
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
        if (token.isRevoked()) {
            // A rotated token being re-used signals possible theft.
            refreshTokenRepository.revokeAllActiveForUser(token.getUser().getId());
            throw new RefreshTokenReuseException(
                    "Refresh token reuse detected. All active sessions for this user have been revoked.");
        }
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RefreshTokenExpiredException("Refresh token has expired");
        }
        return token;
    }

    @Override
    @Transactional
    public void revoke(RefreshToken token) {
        if (!token.isRevoked()) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        }
    }

    @Override
    public Optional<RefreshToken> findByToken(String tokenValue) {
        return refreshTokenRepository.findByToken(tokenValue);
    }
}
