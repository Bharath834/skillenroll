package com.skillenroll.security.filter;

import com.skillenroll.entity.User;
import com.skillenroll.enums.Role;
import com.skillenroll.security.jwt.JwtService;
import com.skillenroll.security.service.CustomUserDetails;
import com.skillenroll.security.service.CustomUserDetailsService;
import com.skillenroll.service.interfaces.BlacklistedTokenService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtAuthenticationFilter} using Mockito.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "valid.jwt.token";
    private static final String EMAIL = "alice@example.com";

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private BlacklistedTokenService blacklistedTokenService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private CustomUserDetails userDetails;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        userDetails = CustomUserDetails.from(User.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Wonder")
                .email(EMAIL)
                .phoneNumber("9876543210")
                .password("$2a$10$enc0d3d.passw0rd.hash.abcdefghijklmnopqrstuvwxyz0123456789")
                .role(Role.STUDENT)
                .build());
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, blacklistedTokenService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void withoutAuthorizationHeader_shouldPassThroughWithoutAuthentication() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void withValidNonBlacklistedToken_shouldAuthenticateAndCarryTokenAsCredentials() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
        when(blacklistedTokenService.isBlacklisted(TOKEN)).thenReturn(false);
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtService.isTokenValid(TOKEN, userDetails)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(userDetails, authentication.getPrincipal());
        assertEquals(TOKEN, authentication.getCredentials());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void withBlacklistedToken_shouldRecordRevokedErrorAndSkipAuthentication() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenReturn(EMAIL);
        when(blacklistedTokenService.isBlacklisted(TOKEN)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(request).setAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE, "JWT token has been revoked");
        verify(userDetailsService, never()).loadUserByUsername(EMAIL);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void withExpiredToken_shouldRecordExpiredError() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + TOKEN);
        when(jwtService.extractUsername(TOKEN)).thenThrow(new ExpiredJwtException(null, null, "expired"));

        filter.doFilter(request, response, filterChain);

        verify(request).setAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE, "JWT token has expired");
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
