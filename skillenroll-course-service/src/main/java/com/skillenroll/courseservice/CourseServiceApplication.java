package com.skillenroll.courseservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * SkillEnroll Course Service.
 *
 * <p>Microservice responsible for the course/program catalog domain. It
 * registers with the Eureka discovery server ({@code skillenroll-eureka-server})
 * so it can be located by name (e.g. by the API gateway via
 * {@code lb://skillenroll-course-service}).
 *
 * <p>Exposes SpringDoc OpenAPI (Swagger UI) and Actuator endpoints; see
 * {@code application.yml} for configuration.
 *
 * @author SkillEnroll
 */
@SpringBootApplication
public class CourseServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CourseServiceApplication.class, args);
    }
}
