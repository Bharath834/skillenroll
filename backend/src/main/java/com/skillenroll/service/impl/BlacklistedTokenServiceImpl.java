package com.skillenroll.service.impl;

import com.skillenroll.entity.BlacklistedToken;
import com.skillenroll.repository.BlacklistedTokenRepository;
import com.skillenroll.security.jwt.JwtService;
import com.skillenroll.service.interfaces.BlacklistedTokenService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HexFormat;

/**
 * Stores SHA-256 hashes of logged-out JWTs. The hash (not the raw token) is
 * persisted so profile data embedded in the token never lands in the database;
 * the blacklist entry is only useful until the token's natural expiry.
 */
@Service
@Transactional(readOnly = true)
public class BlacklistedTokenServiceImpl implements BlacklistedTokenService {

    private static final String SHA_256 = "SHA-256";
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(BlacklistedTokenServiceImpl.class);

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final JwtService jwtService;

    public BlacklistedTokenServiceImpl(BlacklistedTokenRepository blacklistedTokenRepository, JwtService jwtService) {
        this.blacklistedTokenRepository = blacklistedTokenRepository;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public void blacklist(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String tokenHash = hash(token);
        if (blacklistedTokenRepository.existsByTokenHash(tokenHash)) {
            return; // idempotent
        }
        Date expiration = jwtService.extractExpiration(token);
        BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault()))
                .build();
        try {
            blacklistedTokenRepository.save(blacklistedToken);
        } catch (DataIntegrityViolationException ex) {
            // Lost the race with a concurrent logout of the same token - the
            // token is blacklisted either way.
            log.debug("Token already blacklisted by a concurrent request");
        }
    }

    @Override
    public boolean isBlacklisted(String token) {
        return token != null && !token.isBlank() && blacklistedTokenRepository.existsByTokenHash(hash(token));
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", ex);
        }
    }
}
