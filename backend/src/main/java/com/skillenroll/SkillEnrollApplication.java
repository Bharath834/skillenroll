package com.skillenroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SkillEnroll - Spring Boot entry point.
 *
 * <p>Day 3 scope: stateless JWT authentication &amp; authorization via
 * Spring Security (public {@code /api/auth/**}, protected everywhere else).
 * OAuth2 remains a future enhancement.
 */
@SpringBootApplication
public class SkillEnrollApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillEnrollApplication.class, args);
    }
}
