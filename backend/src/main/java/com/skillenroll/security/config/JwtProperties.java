package com.skillenroll.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code app.jwt.*} properties from {@code application.yml}.
 *
 * <p>{@code secret} must be a Base64-encoded key of at least 256 bits
 * (the value in {@code application.yml} is a 384-bit key). {@code expirationMs}
 * controls the access-token lifetime.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String secret;

    private long expirationMs;
}
