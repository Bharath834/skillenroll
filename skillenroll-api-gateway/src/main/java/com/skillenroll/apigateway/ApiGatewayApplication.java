package com.skillenroll.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SkillEnroll API Gateway.
 *
 * <p>Edge entry point for the SkillEnroll microservices, built with Spring
 * Cloud Gateway (reactive / WebFlux on Netty). The gateway registers itself
 * with the Eureka discovery server so it appears on the dashboard at
 * {@code http://localhost:8761}. It forwards {@code /api/auth/**} and
 * {@code /api/users/**} to the {@code skillenroll-backend} service,
 * resolved at runtime via Eureka service discovery (see application.yml).
 *
 * @author SkillEnroll
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
