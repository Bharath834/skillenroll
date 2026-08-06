package com.skillenroll.courseservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi configuration. Exposes the API contract at
 * {@code /v3/api-docs} and the interactive UI at {@code /swagger-ui/index.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI courseServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkillEnroll Course Service API")
                        .description("Course/catalog management for SkillEnroll: create, read, "
                                + "search, update and delete courses.")
                        .version("1.0.0")
                        .contact(new Contact().name("SkillEnroll Team")));
    }
}
