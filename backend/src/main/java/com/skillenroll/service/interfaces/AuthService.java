package com.skillenroll.service.interfaces;

import com.skillenroll.dto.request.LoginRequest;
import com.skillenroll.dto.request.LogoutRequest;
import com.skillenroll.dto.request.RefreshTokenRequest;
import com.skillenroll.dto.request.RegisterRequest;
import com.skillenroll.dto.response.JwtResponse;

/**
 * Authentication use cases: registration, login, token refresh and logout.
 */
public interface AuthService {

    JwtResponse register(RegisterRequest request);

    JwtResponse login(LoginRequest request);

    /**
     * Rotates the presented refresh token: revokes it, then issues a fresh
     * access token and refresh token pair for the same user.
     */
    JwtResponse refresh(RefreshTokenRequest request);

    /**
     * Revokes the presented refresh token, provided it belongs to the
     * authenticated user.
     */
    void logout(LogoutRequest request);
}
