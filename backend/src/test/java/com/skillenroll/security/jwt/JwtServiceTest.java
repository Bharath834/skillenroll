package com.skillenroll.security.jwt;

import com.skillenroll.entity.User;
import com.skillenroll.enums.Role;
import com.skillenroll.security.config.JwtProperties;
import com.skillenroll.security.service.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link JwtService}. Uses the real signing key (same Base64
 * value as {@code application.yml}) so generated tokens can be parsed back
 * and their claims asserted.
 */
class JwtServiceTest {

    private static final String SECRET = "Naxi60zfyJogjfLK/iRaMX0qPP0qhOUuU3xHnUpjoqGeVxPsAsWadrefaVEFYfuC";
    private static final long EXPIRATION_MS = 86400000L;

    private JwtService jwtService;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setExpirationMs(EXPIRATION_MS);
        jwtService = new JwtService(properties);

        userDetails = CustomUserDetails.from(User.builder()
                .id(42L)
                .firstName("Bharath")
                .lastName("Kumar")
                .email("bharath@gmail.com")
                .phoneNumber("9876543210")
                .password("$2a$10$enc0d3d.passw0rd.hash.abcdefghijklmnopqrstuvwxyz0123456789")
                .role(Role.STUDENT)
                .build());
    }

    @Test
    void generateToken_shouldEmbedProfileClaimsAndKeepEmailAsSubject() {
        String token = jwtService.generateToken(userDetails);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("bharath@gmail.com", claims.getSubject());
        assertEquals(42L, claims.get("userId", Number.class).longValue());
        assertEquals("Bharath", claims.get("firstName", String.class));
        assertEquals("Kumar", claims.get("lastName", String.class));
        assertEquals("STUDENT", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void extractUsernameAndValidation_shouldStillWorkForLoginFlow() {
        String token = jwtService.generateToken(userDetails);

        assertEquals("bharath@gmail.com", jwtService.extractUsername(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }
}
