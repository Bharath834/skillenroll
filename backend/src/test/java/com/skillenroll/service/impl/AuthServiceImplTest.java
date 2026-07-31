package com.skillenroll.service.impl;

import com.skillenroll.dto.request.LoginRequest;
import com.skillenroll.dto.request.RegisterRequest;
import com.skillenroll.dto.response.JwtResponse;
import com.skillenroll.entity.User;
import com.skillenroll.enums.Role;
import com.skillenroll.exception.DuplicateResourceException;
import com.skillenroll.repository.UserRepository;
import com.skillenroll.security.jwt.JwtService;
import com.skillenroll.security.service.CustomUserDetailsService;
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
import org.springframework.security.core.userdetails.UserDetails;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private UserDetails userDetails;

    @Mock
    private Authentication authentication;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder, authenticationManager,
                jwtService, customUserDetailsService);
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
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(TOKEN);
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        JwtResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(TOKEN, response.getToken());
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
    void login_withValidCredentials_shouldReturnJwtResponseWithToken() {
        LoginRequest request = LoginRequest.builder()
                .email(EMAIL)
                .password(RAW_PASSWORD)
                .build();

        User user = User.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Wonder")
                .email(EMAIL)
                .phoneNumber(PHONE)
                .password(ENCODED_PASSWORD)
                .role(Role.STUDENT)
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn(EMAIL);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(customUserDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(TOKEN);
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        JwtResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(TOKEN, response.getToken());
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
}
