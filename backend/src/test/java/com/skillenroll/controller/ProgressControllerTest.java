package com.skillenroll.controller;

import com.jayway.jsonpath.JsonPath;
import com.skillenroll.entity.Course;
import com.skillenroll.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end MockMvc tests for the course progress module against the real
 * security chain with an in-memory H2 database: creation rules (enrollment
 * requirement, one record per user/course), the 0-100 percentage validation,
 * the completed/completedAt behavior at 100%, the query endpoints and delete.
 * Every test rolls back its own data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProgressControllerTest {

    private static final String STUDENT_EMAIL = "progress.student@test.com";
    private static final String STUDENT_PHONE = "9666666666";
    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    /** JWT for the registered student. */
    private String token;

    /** Id of the registered student (from the register response). */
    private Long userId;

    @BeforeEach
    void setUp() throws Exception {
        token = registerStudent();
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @Test
    void createProgress_forEnrolledUser_shouldReturnCreated() throws Exception {
        long courseId = createEnrolledCourse();

        mockMvc.perform(post("/api/progress")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(courseId, "45.50")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.progressPercentage").value(45.50))
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.completedAt").doesNotExist())
                .andExpect(jsonPath("$.data.startedAt").exists())
                .andExpect(jsonPath("$.data.userName").value("Bharath Kumar"));
    }

    @Test
    void createProgress_withDuplicatePair_shouldReturnConflict() throws Exception {
        long courseId = createEnrolledCourse();
        String body = progressBody(courseId, "45.50");

        mockMvc.perform(post("/api/progress")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/progress")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Progress already exists for this user and course"));
    }

    @Test
    void createProgress_forNonEnrolledUser_shouldReturnNotFound() throws Exception {
        Course course = saveCourse("Spring Boot Masterclass", "49.99");

        mockMvc.perform(post("/api/progress")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(course.getId(), "10.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User is not enrolled in this course"));
    }

    @Test
    void createProgress_withUnknownUser_shouldReturnNotFound() throws Exception {
        Course course = saveCourse("Spring Boot Masterclass", "49.99");

        mockMvc.perform(post("/api/progress")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":999999,\"courseId\":" + course.getId()
                                + ",\"progressPercentage\":10.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 999999"));
    }

    @Test
    void createProgress_withUnknownCourse_shouldReturnNotFound() throws Exception {
        mockMvc.perform(post("/api/progress")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId
                                + ",\"courseId\":999999,\"progressPercentage\":10.00}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found with id: 999999"));
    }

    @Test
    void createProgress_withPercentageAbove100_shouldReturnBadRequest() throws Exception {
        long courseId = createEnrolledCourse();

        mockMvc.perform(post("/api/progress")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(courseId, "100.01")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void createProgress_withNegativePercentage_shouldReturnBadRequest() throws Exception {
        long courseId = createEnrolledCourse();

        mockMvc.perform(post("/api/progress")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(courseId, "-1.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createProgress_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"courseId\":1,\"progressPercentage\":10.00}"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Update / completion rules
    // ------------------------------------------------------------------

    @Test
    void updateProgress_fromZeroToFifty_shouldStayIncomplete() throws Exception {
        long progressId = createEnrolledProgress("0.00");

        mockMvc.perform(put("/api/progress/{id}", progressId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(1L, "50.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progressPercentage").value(50.00))
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.completedAt").doesNotExist());
    }

    @Test
    void updateProgress_toHundred_shouldCompleteWithCompletedAt() throws Exception {
        long progressId = createEnrolledProgress("50.00");

        mockMvc.perform(put("/api/progress/{id}", progressId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(1L, "100.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progressPercentage").value(100.00))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.completedAt").value(notNullValue()));
    }

    @Test
    void updateProgress_backBelowHundred_shouldResetCompletion() throws Exception {
        long progressId = createEnrolledProgress("100.00");

        mockMvc.perform(put("/api/progress/{id}", progressId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(1L, "50.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.progressPercentage").value(50.00))
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.completedAt").doesNotExist());
    }

    @Test
    void updateProgress_withPercentageAbove100_shouldReturnBadRequest() throws Exception {
        long progressId = createEnrolledProgress("50.00");

        mockMvc.perform(put("/api/progress/{id}", progressId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(1L, "150.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void updateProgress_withNegativePercentage_shouldReturnBadRequest() throws Exception {
        long progressId = createEnrolledProgress("50.00");

        mockMvc.perform(put("/api/progress/{id}", progressId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(1L, "-5.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateProgress_withUnknownId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(put("/api/progress/{id}", 999999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(1L, "50.00")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Progress not found with id: 999999"));
    }

    // ------------------------------------------------------------------
    // Read endpoints
    // ------------------------------------------------------------------

    @Test
    void getProgressById_shouldReturnProgress() throws Exception {
        long progressId = createEnrolledProgress("45.50");

        mockMvc.perform(get("/api/progress/{id}", progressId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(progressId))
                .andExpect(jsonPath("$.data.courseTitle").value("Spring Boot Masterclass"))
                .andExpect(jsonPath("$.data.progressPercentage").value(45.50));
    }

    @Test
    void getProgressByUserAndCourse_shouldReturnProgress() throws Exception {
        long courseId = createEnrolledCourse();
        long progressId = createProgress(courseId, "45.50");

        mockMvc.perform(get("/api/progress/user/{userId}/course/{courseId}", userId, courseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(progressId))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.courseId").value(courseId));
    }

    @Test
    void getProgressByUserAndCourse_withNoRecord_shouldReturnNotFound() throws Exception {
        long courseId = createEnrolledCourse();

        mockMvc.perform(get("/api/progress/user/{userId}/course/{courseId}", userId, courseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(
                        "Progress not found for user " + userId + " and course " + courseId));
    }

    @Test
    void getProgressByUserId_shouldReturnAllRecordsForUser() throws Exception {
        long first = createEnrolledProgress("10.00");
        Course second = saveCourse("Advanced Kubernetes", "79.99");
        enroll(second.getId());
        long secondProgress = createProgress(second.getId(), "90.00");

        mockMvc.perform(get("/api/progress/user/{userId}", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.id == " + first + ")]").exists())
                .andExpect(jsonPath("$.data[?(@.id == " + secondProgress + ")]").exists());
    }

    @Test
    void getProgressByCourseId_shouldReturnAllRecordsForCourse() throws Exception {
        long courseId = createEnrolledCourse();
        createProgress(courseId, "30.00");

        mockMvc.perform(get("/api/progress/course/{courseId}", courseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].courseId").value(courseId));
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------

    @Test
    void deleteProgress_shouldReturnOk() throws Exception {
        long progressId = createEnrolledProgress("50.00");

        mockMvc.perform(delete("/api/progress/{id}", progressId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/progress/{id}", progressId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProgress_withUnknownId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/api/progress/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // Swagger documentation
    // ------------------------------------------------------------------

    @Test
    void openApiSpec_shouldDocumentAllProgressEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/progress'].post").exists())
                .andExpect(jsonPath("$.paths['/api/progress/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/progress/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/progress/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/progress/user/{userId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/progress/course/{courseId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/progress/user/{userId}/course/{courseId}'].get").exists())
                .andExpect(jsonPath("$.components.schemas.ProgressRequest").exists())
                .andExpect(jsonPath("$.components.schemas.ProgressResponse").exists());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String registerStudent() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Bharath","lastName":"Kumar","email":"%s","phoneNumber":"%s","password":"%s"}
                                """.formatted(STUDENT_EMAIL, STUDENT_PHONE, PASSWORD).strip()))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        userId = JsonPath.parse(body).read("$.data.user.id", Long.class);
        return JsonPath.parse(body).read("$.data.token");
    }

    private Course saveCourse(String title, String price) {
        return courseRepository.save(Course.builder()
                .title(title)
                .category("Programming")
                .price(new BigDecimal(price))
                .duration(20)
                .instructorName("Jane Smith")
                .build());
    }

    private void enroll(long courseId) throws Exception {
        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"courseId\":" + courseId + "}"))
                .andExpect(status().isCreated());
    }

    private String progressBody(long courseId, String percentage) {
        return "{\"userId\":" + userId + ",\"courseId\":" + courseId
                + ",\"progressPercentage\":" + percentage + "}";
    }

    private long createEnrolledCourse() throws Exception {
        Course course = saveCourse("Spring Boot Masterclass", "49.99");
        enroll(course.getId());
        return course.getId();
    }

    private long createProgress(long courseId, String percentage) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/progress")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(progressBody(courseId, percentage)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return JsonPath.parse(body).read("$.data.id", Long.class);
    }

    private long createEnrolledProgress(String percentage) throws Exception {
        long courseId = createEnrolledCourse();
        return createProgress(courseId, percentage);
    }
}
