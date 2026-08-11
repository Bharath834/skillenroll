package com.skillenroll.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code razorpay.*} properties from {@code application.yml}.
 *
 * <p>{@code keyId} and {@code keySecret} are the Razorpay TEST-mode
 * credentials, injected via the {@code RAZORPAY_KEY_ID} and
 * {@code RAZORPAY_KEY_SECRET} environment variables (see
 * {@code application.yml}). Real secrets must never be committed or logged.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayProperties {

    /** Razorpay API key id (TEST mode), e.g. {@code rzp_test_...}. */
    private String keyId;

    /** Razorpay API key secret (TEST mode). */
    private String keySecret;
}
