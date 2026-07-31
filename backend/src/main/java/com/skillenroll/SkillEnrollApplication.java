package com.skillenroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SkillEnroll - Spring Boot entry point.
 *
 * <p>Day 2 scope: backend foundation (entities, repositories, services,
 * REST controllers, exception handling). Authentication, JWT, OAuth2 and
 * Spring Security arrive on Day 3.
 */
@SpringBootApplication
public class SkillEnrollApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillEnrollApplication.class, args);
    }
}
