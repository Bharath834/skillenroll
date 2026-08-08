package com.skillenroll.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the OpenAPI document still generates (springdoc) and documents the
 * paginated enrollment endpoints with their operation metadata.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiSpec_shouldGenerateAndDocumentEnrollmentEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // Paginated list endpoint with page/size/sort and the optional filters documented.
                .andExpect(jsonPath("$.paths['/api/enrollments'].get.summary").value(
                        "List enrollments (paginated, sortable, filterable)"))
                .andExpect(jsonPath("$.paths['/api/enrollments'].get.parameters[*].name",
                        org.hamcrest.Matchers.hasItems("page", "size", "sort", "status", "userId", "courseId")))
                .andExpect(jsonPath("$.paths['/api/enrollments/user/{userId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/enrollments/course/{courseId}'].get").exists())
                // The bearer security scheme must remain wired up.
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists());
    }
}
