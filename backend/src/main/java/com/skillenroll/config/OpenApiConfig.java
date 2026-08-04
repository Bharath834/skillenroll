package com.skillenroll.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi configuration. Exposes the API contract at
 * {@code /v3/api-docs} and the interactive UI at {@code /swagger-ui/index.html},
 * with a global {@code Bearer} security scheme so authenticated endpoints can
 * be exercised from Swagger UI directly.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI skillEnrollOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkillEnroll API")
                        .description("SkillEnroll backend REST API: authentication (register, login, "
                                + "refresh, logout), user profiles and course/enrollment management.")
                        .version("1.0.0")
                        .contact(new Contact().name("SkillEnroll Team")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the access token returned by /api/auth/login "
                                        + "or /api/auth/register.")));
    }
}
