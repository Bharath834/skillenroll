package com.skillenroll.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Razorpay configuration (Day 8 / T07 — setup only).
 *
 * <p>Exposes the {@link RazorpayClient} bean built from the configured TEST
 * credentials for the upcoming payment-order service. Constructing the client
 * performs no network calls; the application fails fast at startup with a
 * clear message when either credential is missing, instead of silently using
 * fake credentials. No Razorpay order API is called from this class.
 */
@Configuration
@EnableConfigurationProperties(RazorpayProperties.class)
public class RazorpayConfig {

    @Bean
    public RazorpayClient razorpayClient(RazorpayProperties properties) throws RazorpayException {
        String keyId = properties.getKeyId();
        String keySecret = properties.getKeySecret();
        if (!StringUtils.hasText(keyId) || !StringUtils.hasText(keySecret)) {
            throw new IllegalStateException(
                    "Razorpay TEST credentials are not configured. "
                            + "Set the RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET "
                            + "environment variables before starting the application.");
        }
        return new RazorpayClient(keyId, keySecret);
    }
}
