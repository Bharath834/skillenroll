package com.skillenroll.service.impl;

import com.skillenroll.entity.BlacklistedToken;
import com.skillenroll.repository.BlacklistedTokenRepository;
import com.skillenroll.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BlacklistedTokenServiceImpl} using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class BlacklistedTokenServiceImplTest {

    private static final String TOKEN = "header.payload.signature";
    private static final Instant EXPIRY = Instant.parse("2026-08-11T10:00:00Z");

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Mock
    private JwtService jwtService;

    private BlacklistedTokenServiceImpl blacklistedTokenService;

    @BeforeEach
    void setUp() {
        blacklistedTokenService = new BlacklistedTokenServiceImpl(blacklistedTokenRepository, jwtService);
    }

    @Test
    void blacklist_shouldPersistHashedTokenWithNaturalExpiry() {
        when(jwtService.extractExpiration(TOKEN)).thenReturn(Date.from(EXPIRY));
        when(blacklistedTokenRepository.existsByTokenHash(anyString())).thenReturn(false);
        when(blacklistedTokenRepository.save(any(BlacklistedToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        blacklistedTokenService.blacklist(TOKEN);

        ArgumentCaptor<BlacklistedToken> captor = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(blacklistedTokenRepository).save(captor.capture());
        BlacklistedToken saved = captor.getValue();
        assertEquals(sha256(TOKEN), saved.getTokenHash());
        assertEquals(LocalDateTime.ofInstant(EXPIRY, ZoneId.systemDefault()), saved.getExpiresAt());
    }

    @Test
    void blacklist_shouldBeIdempotentWhenHashAlreadyPresent() {
        when(blacklistedTokenRepository.existsByTokenHash(sha256(TOKEN))).thenReturn(true);

        blacklistedTokenService.blacklist(TOKEN);

        verify(blacklistedTokenRepository, never()).save(any(BlacklistedToken.class));
        verify(jwtService, never()).extractExpiration(anyString());
    }

    @Test
    void blacklist_withBlankToken_shouldDoNothing() {
        blacklistedTokenService.blacklist("   ");

        verify(blacklistedTokenRepository, never()).save(any(BlacklistedToken.class));
        verify(jwtService, never()).extractExpiration(anyString());
    }

    @Test
    void isBlacklisted_shouldReturnTrueForKnownHash() {
        when(blacklistedTokenRepository.existsByTokenHash(sha256(TOKEN))).thenReturn(true);

        assertTrue(blacklistedTokenService.isBlacklisted(TOKEN));
    }

    @Test
    void isBlacklisted_shouldReturnFalseForUnknownHash() {
        when(blacklistedTokenRepository.existsByTokenHash(sha256(TOKEN))).thenReturn(false);

        assertFalse(blacklistedTokenService.isBlacklisted(TOKEN));
    }

    @Test
    void isBlacklisted_withBlankToken_shouldReturnFalse() {
        assertFalse(blacklistedTokenService.isBlacklisted(""));
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
