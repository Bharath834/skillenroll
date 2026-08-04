package com.skillenroll.dto.response;

import com.skillenroll.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Successful authentication payload: JWT access token, opaque refresh token,
 * token type, lifetime in seconds and the authenticated user's profile
 * (incl. role). The password is never part of any response DTO.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {

    private String token;

    private String refreshToken;

    private String tokenType;

    /** Token lifetime in seconds. */
    private long expiresIn;

    private UserResponse user;
}
