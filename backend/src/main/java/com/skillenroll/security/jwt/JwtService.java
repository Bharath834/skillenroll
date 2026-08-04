package com.skillenroll.security.jwt;

import com.skillenroll.entity.User;
import com.skillenroll.security.config.JwtProperties;
import com.skillenroll.security.service.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

/**
 * Generates, parses and validates JWT access tokens (HS384).
 *
 * <p>Stateless by design: no server-side session is involved. The token
 * carries the subject (email) plus the user's {@code userId}, {@code firstName},
 * {@code lastName} and {@code role} claims, and is signed with a shared
 * HMAC-SHA secret.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(JwtProperties jwtProperties) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
        this.expirationMs = jwtProperties.getExpirationMs();
    }

    /**
     * Builds a signed JWT for the given authenticated user.
     *
     * <p>The subject stays the user's email (login identifier).
     * {@code userId}, {@code firstName}, {@code lastName} and {@code role}
     * are embedded as additional claims for stateless profile access.
     *
     * @param userDetails the authenticated user
     * @return a compact JWT string
     */
    public String generateToken(CustomUserDetails userDetails) {
        User user = userDetails.getUser();
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Extracts the username (email) from a valid JWT.
     *
     * @param token the compact JWT
     * @return the subject claim
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Verifies signature, expiry and that the token belongs to the given user.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Extracts the token's expiry ({@code exp}) claim. Used when blacklisting
     * a token so the blacklist entry carries the same lifetime as the JWT.
     *
     * @param token the compact JWT
     * @return the expiration date
     */
    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
