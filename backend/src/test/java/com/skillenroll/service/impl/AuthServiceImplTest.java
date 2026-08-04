package com.skillenroll.service.impl;

import com.skillenroll.dto.request.LoginRequest;
import com.skillenroll.dto.request.LogoutRequest;
import com.skillenroll.dto.request.RefreshTokenRequest;
import com.skillenroll.dto.request.RegisterRequest;
import com.skillenroll.dto.response.JwtResponse;
import com.skillenroll.entity.RefreshToken;
import com.skillenroll.entity.User;
import com.skillenroll.enums.Role;
import com.skillenroll.exception.DuplicateResourceException;
import com.skillenroll.exception.InvalidRefreshTokenException;
import com.skillenroll.repository.UserRepository;
import com.skillenroll.security.jwt.JwtService;
import com.skillenroll.security.service.CustomUserDetails;
import com.skillenroll.service.interfaces.BlacklistedTokenService;
import com.skillenroll.service.interfaces.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthServiceImpl} using Mockito. All collaborators are
 * mocked; no database, Spring context or real JWT keys are involved.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String EMAIL = "alice@example.com";
    private static final String PHONE = "9876543210";
    private static final String RAW_PASSWORD = "Passw0rd!";
    private static final String ENCODED_PASSWORD = "$2a$10$mocked.bcrypt.hash.abcdefghijklmnopqrstuvwxyz";
    private static final String TOKEN = "mock.jwt.token";
    private static final String REFRESH_TOKEN = "mock-refresh-token";
    private static final String ROTATED_REFRESH_TOKEN = "mock-rotated-refresh-token";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private BlacklistedTokenService blacklistedTokenService;

    @Mock
    private Authentication authentication;

    private AuthServiceImpl authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Wonder")
                .email(EMAIL)
                .phoneNumber(PHONE)
                .password(ENCODED_PASSWORD)
                .role(Role.STUDENT)
                .build();
        authService = new AuthServiceImpl(userRepository, passwordEncoder, authenticationManager,
                jwtService, refreshTokenService, blacklistedTokenService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ------------------------------------------------------------------
    // register
    // ------------------------------------------------------------------

    @Test
    void register_shouldEncodePasswordSaveUserAndReturnJwtResponse() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Alice")
                .lastName("Wonder")
                .email(EMAIL)
                .phoneNumber(PHONE)
                .password(RAW_PASSWORD)
                .build();

        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);
        when(refreshTokenService.issueRefreshToken(any(User.class))).thenReturn(REFRESH_TOKEN);
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        JwtResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(TOKEN, response.getToken());
        assertEquals(REFRESH_TOKEN, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400L, response.getExpiresIn());
        assertNotNull(response.getUser());
        assertEquals(EMAIL, response.getUser().getEmail());
        assertEquals(Role.STUDENT, response.getUser().getRole());

        // The plain-text password is never stored or returned.
        verify(passwordEncoder).encode(RAW_PASSWORD);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(ENCODED_PASSWORD, savedUser.getPassword());
        assertNotEquals(RAW_PASSWORD, savedUser.getPassword());
        assertEquals(Role.STUDENT, savedUser.getRole());
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void register_whenEmailAlreadyExists_shouldThrowDuplicateResourceException() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Alice")
                .lastName("Wonder")
                .email(EMAIL)
                .phoneNumber(PHONE)
                .password(RAW_PASSWORD)
                .build();

        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void register_whenPhoneNumberAlreadyExists_shouldThrowDuplicateResourceException() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Alice")
                .lastName("Wonder")
                .email(EMAIL)
                .phoneNumber(PHONE)
                .password(RAW_PASSWORD)
                .build();

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.existsByPhoneNumber(PHONE)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> authService.register(request));

        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    // ------------------------------------------------------------------
    // login
    // ------------------------------------------------------------------

    @Test
    void login_withValidCredentials_shouldReturnJwtResponseWithTokenAndRefreshToken() {
        LoginRequest request = LoginRequest.builder()
                .email(EMAIL)
                .password(RAW_PASSWORD)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);
        when(refreshTokenService.issueRefreshToken(any(User.class))).thenReturn(REFRESH_TOKEN);
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        JwtResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(TOKEN, response.getToken());
        assertEquals(REFRESH_TOKEN, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(86400L, response.getExpiresIn());
        assertEquals(EMAIL, response.getUser().getEmail());
        assertEquals(Role.STUDENT, response.getUser().getRole());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_withInvalidCredentials_shouldThrowBadCredentialsException() {
        LoginRequest request = LoginRequest.builder()
                .email(EMAIL)
                .password("wrong-password")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        verify(userRepository, never()).findByEmail(any());
        verify(jwtService, never()).generateToken(any());
        verify(refreshTokenService, never()).issueRefreshToken(any());
    }

    @Test
    void login_whenUserMissingAfterAuthentication_shouldThrowUsernameNotFoundException() {
        LoginRequest request = LoginRequest.builder()
                .email(EMAIL)
                .password(RAW_PASSWORD)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(request));

        verify(jwtService, never()).generateToken(any());
    }

    // ------------------------------------------------------------------
    // refresh
    // ------------------------------------------------------------------

    @Test
    void refresh_withValidToken_shouldRotateAndReturnNewTokenPair() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken(REFRESH_TOKEN)
                .build();
        RefreshToken oldToken = RefreshToken.builder()
                .id(10L)
                .token(REFRESH_TOKEN)
                .user(user)
                .build();

        when(refreshTokenService.validateForRotation(REFRESH_TOKEN)).thenReturn(oldToken);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any(CustomUserDetails.class))).thenReturn(TOKEN);
        when(refreshTokenService.issueRefreshToken(user)).thenReturn(ROTATED_REFRESH_TOKEN);
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        JwtResponse response = authService.refresh(request);

        assertNotNull(response);
        assertEquals(TOKEN, response.getToken());
        assertEquals(ROTATED_REFRESH_TOKEN, response.getRefreshToken());
        assertEquals(EMAIL, response.getUser().getEmail());
        verify(refreshTokenService).revoke(oldToken);
    }

    @Test
    void refresh_withInvalidToken_shouldPropagate() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("unknown-token")
                .build();
        when(refreshTokenService.validateForRotation("unknown-token"))
                .thenThrow(new InvalidRefreshTokenException("Invalid refresh token"));

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refresh(request));

        verify(refreshTokenService, never()).revoke(any());
        verify(jwtService, never()).generateToken(any());
    }

    // ------------------------------------------------------------------
    // logout
    // ------------------------------------------------------------------

    @Test
    void logout_withTokenBelongingToAuthenticatedUser_shouldRevokeAndBlacklistAccessToken() {
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken(REFRESH_TOKEN)
                .build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(10L)
                .token(REFRESH_TOKEN)
                .user(user)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CustomUserDetails.from(user), TOKEN));

        when(refreshTokenService.findByToken(REFRESH_TOKEN)).thenReturn(Optional.of(refreshToken));

        authService.logout(request);

        verify(refreshTokenService).revoke(refreshToken);
        verify(blacklistedTokenService).blacklist(TOKEN);
    }

    @Test
    void logout_withTokenOfAnotherUser_shouldThrowIllegalArgument() {
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken(REFRESH_TOKEN)
                .build();
        User otherUser = User.builder().id(2L).email("other@example.com").build();
        RefreshToken refreshToken = RefreshToken.builder()
                .id(10L)
                .token(REFRESH_TOKEN)
                .user(otherUser)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CustomUserDetails.from(user), null));

        when(refreshTokenService.findByToken(REFRESH_TOKEN)).thenReturn(Optional.of(refreshToken));

        assertThrows(IllegalArgumentException.class, () -> authService.logout(request));

        verify(refreshTokenService, never()).revoke(any());
    }

    @Test
    void logout_withUnknownToken_shouldThrowInvalidRefreshToken() {
        LogoutRequest request = LogoutRequest.builder()
                .refreshToken("unknown-token")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(CustomUserDetails.from(user), null));

        when(refreshTokenService.findByToken("unknown-token")).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> authService.logout(request));
    }
}
