package com.skillenroll.service.interfaces;

import com.skillenroll.dto.request.LoginRequest;
import com.skillenroll.dto.request.RegisterRequest;
import com.skillenroll.dto.response.JwtResponse;

/**
 * Authentication use cases: registration and login.
 */
public interface AuthService {

    JwtResponse register(RegisterRequest request);

    JwtResponse login(LoginRequest request);
}
