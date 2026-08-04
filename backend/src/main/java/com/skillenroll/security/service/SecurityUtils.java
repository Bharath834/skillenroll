package com.skillenroll.security.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared accessors for the current security context, so the "resolve the
 * authenticated user from the {@code SecurityContext}" logic lives in exactly
 * one place.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Resolves the authenticated {@link CustomUserDetails} principal.
     *
     * @return the principal installed by {@code JwtAuthenticationFilter}
     * @throws IllegalStateException if no (or an unexpected) principal is present
     */
    public static CustomUserDetails currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
            // Defensive: every authenticated request installs a CustomUserDetails
            // principal via JwtAuthenticationFilter, so this path is unreachable
            // in normal operation.
            throw new IllegalStateException("No authenticated user found in the security context");
        }
        return customUserDetails;
    }

    /**
     * Resolves the raw JWT access token that authenticated this request
     * (installed as the {@code Authentication} credentials by the JWT filter).
     *
     * @return the raw access token
     * @throws IllegalStateException if no (or an unexpected) token is present
     */
    public static String currentToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getCredentials() instanceof String token)) {
            throw new IllegalStateException("No access token found in the security context");
        }
        return token;
    }
}
