package com.skillenroll.service.impl;

import com.skillenroll.entity.RefreshToken;
import com.skillenroll.entity.User;
import com.skillenroll.enums.Role;
import com.skillenroll.exception.InvalidRefreshTokenException;
import com.skillenroll.exception.RefreshTokenExpiredException;
import com.skillenroll.exception.RefreshTokenReuseException;
import com.skillenroll.repository.RefreshTokenRepository;
import com.skillenroll.security.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RefreshTokenServiceImpl} using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    private static final long REFRESH_EXPIRATION_MS = 604800000L; // 7 days

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenServiceImpl refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setRefreshExpirationMs(REFRESH_EXPIRATION_MS);
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, properties);

        user = User.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Wonder")
                .email("alice@example.com")
                .phoneNumber("9876543210")
                .password("$2a$10$enc0d3d.passw0rd.hash.abcdefghijklmnopqrstuvwxyz0123456789")
                .role(Role.STUDENT)
                .build();
    }

    @Test
    void issueRefreshToken_shouldPersistTokenWithExpiryAndReturnUuid() {
        when(refreshTokenRepository.save(org.mockito.ArgumentMatchers.any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String tokenValue = refreshTokenService.issueRefreshToken(user);

        assertNotNull(tokenValue);
        assertDoesNotThrow(() -> UUID.fromString(tokenValue));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertEquals(user, saved.getUser());
        assertFalse(saved.isRevoked());
        assertNotNull(saved.getExpiresAt());
        LocalDateTime expected = LocalDateTime.now().plus(Duration.ofMillis(REFRESH_EXPIRATION_MS));
        assertTrue(saved.getExpiresAt().isAfter(expected.minusSeconds(5)));
    }

    @Test
    void validateForRotation_withValidToken_shouldReturnToken() {
        RefreshToken token = RefreshToken.builder()
                .id(10L)
                .token("valid-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));

        assertSame(token, refreshTokenService.validateForRotation("valid-token"));
    }

    @Test
    void validateForRotation_withUnknownToken_shouldThrowInvalidRefreshToken() {
        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> refreshTokenService.validateForRotation("unknown"));
    }

    @Test
    void validateForRotation_withExpiredToken_shouldThrowExpired() {
        RefreshToken token = RefreshToken.builder()
                .id(10L)
                .token("expired-token")
                .user(user)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(RefreshTokenExpiredException.class, () -> refreshTokenService.validateForRotation("expired-token"));
        verify(refreshTokenRepository, never()).revokeAllActiveForUser(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void validateForRotation_withRevokedToken_shouldRevokeAllAndThrowReuse() {
        RefreshToken token = RefreshToken.builder()
                .id(10L)
                .token("revoked-token")
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(1))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.revokeAllActiveForUser(1L)).thenReturn(2);

        RefreshTokenReuseException ex = assertThrows(RefreshTokenReuseException.class,
                () -> refreshTokenService.validateForRotation("revoked-token"));

        assertTrue(ex.getMessage().contains("reuse detected"));
        verify(refreshTokenRepository).revokeAllActiveForUser(1L);
    }

    @Test
    void revoke_shouldMarkTokenRevoked() {
        RefreshToken token = RefreshToken.builder()
                .id(10L)
                .token("active-token")
                .user(user)
                .revoked(false)
                .build();
        when(refreshTokenRepository.save(token)).thenReturn(token);

        refreshTokenService.revoke(token);

        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void revoke_onAlreadyRevokedToken_shouldBeIdempotent() {
        RefreshToken token = RefreshToken.builder()
                .id(10L)
                .token("already-revoked")
                .user(user)
                .revoked(true)
                .build();

        refreshTokenService.revoke(token);

        verify(refreshTokenRepository, never()).save(token);
    }

    @Test
    void findByToken_shouldDelegateToRepository() {
        RefreshToken token = RefreshToken.builder().id(10L).build();
        when(refreshTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        assertEquals(Optional.of(token), refreshTokenService.findByToken("abc"));
    }
}
