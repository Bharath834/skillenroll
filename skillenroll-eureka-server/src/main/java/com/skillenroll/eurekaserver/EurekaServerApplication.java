package com.skillenroll.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * SkillEnroll Eureka service discovery server.
 *
 * <p>Runs standalone on port 8761 and exposes the Eureka dashboard at
 * {@code http://localhost:8761}. Microservices (e.g. the SkillEnroll backend)
 * register with this server so they can be discovered by name.
 *
 * @author SkillEnroll
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
