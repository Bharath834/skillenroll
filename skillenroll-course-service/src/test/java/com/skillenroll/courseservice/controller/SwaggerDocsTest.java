package com.skillenroll.courseservice.controller;

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
 * paginated, searchable course endpoints with their operation metadata.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SwaggerDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiSpec_shouldGenerateAndDocumentCourseEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                // Paginated, searchable list endpoint with page/size/sort and filter params.
                .andExpect(jsonPath("$.paths['/api/courses'].get.summary").value(
                        "List courses (paginated, sortable, searchable)"))
                .andExpect(jsonPath("$.paths['/api/courses'].get.parameters[*].name",
                        org.hamcrest.Matchers.hasItems("page", "size", "sort", "title", "category", "instructor")))
                // Backward-compatible search endpoint.
                .andExpect(jsonPath("$.paths['/api/courses/search'].get").exists())
                // Create endpoint carries an example request body.
                .andExpect(jsonPath("$.paths['/api/courses'].post.requestBody").exists());
    }
}
