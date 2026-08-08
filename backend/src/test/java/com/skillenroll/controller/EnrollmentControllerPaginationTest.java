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
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end MockMvc tests for enrollment pagination, sorting and error
 * handling against the real security chain with an in-memory H2 database.
 * Every test rolls back its own data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EnrollmentControllerPaginationTest {

    private static final String STUDENT_EMAIL = "enroll.student@test.com";
    private static final String STUDENT_PHONE = "9555555555";
    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    /** JWT for the enrolled student. */
    private String token;

    /** Id of the registered student (from the register response). */
    private Long userId;

    @BeforeEach
    void setUp() throws Exception {
        // Registers the student; also populates userId from the register response.
        token = registerStudent();
    }

    // ------------------------------------------------------------------
    // Pagination
    // ------------------------------------------------------------------

    @Test
    void getAllEnrollments_withPagination_shouldReturnPageMetadata() throws Exception {
        createThreeEnrollments();

        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.hasPrevious").value(false));
    }

    @Test
    void getAllEnrollments_sortByIdDesc_shouldReturnNewestFirst() throws Exception {
        List<Long> ids = createThreeEnrollments();
        long newest = ids.get(2);
        long oldest = ids.get(0);

        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(newest))
                .andExpect(jsonPath("$.data.content[2].id").value(oldest));
    }

    @Test
    void getAllEnrollments_sortByUserIdAlias_shouldBeAccepted() throws Exception {
        createThreeEnrollments();

        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("sort", "userId,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));
    }

    @Test
    void getEnrollmentsByUserId_shouldReturnPaginatedFilteredResults() throws Exception {
        createThreeEnrollments();

        mockMvc.perform(get("/api/enrollments/user/{userId}", userId)
                        .header("Authorization", "Bearer " + token)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content[0].userId").value(userId));
    }

    @Test
    void getEnrollmentsByCourseId_shouldReturnPaginatedFilteredResults() throws Exception {
        createThreeEnrollments();
        long courseId = courseRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/enrollments/course/{courseId}", courseId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].courseId").value(courseId));
    }

    // ------------------------------------------------------------------
    // Parameter validation
    // ------------------------------------------------------------------

    @Test
    void getAllEnrollments_withUnknownSortField_shouldReturnBadRequest() throws Exception {
        createThreeEnrollments();

        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("sort", "unknownField,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort field")));
    }

    @Test
    void getAllEnrollments_withPageSizeAboveCap_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ------------------------------------------------------------------
    // Optional filters
    // ------------------------------------------------------------------

    @Test
    void getAllEnrollments_withStatusFilter_shouldReturnOnlyMatching() throws Exception {
        List<Long> ids = createThreeEnrollments(); // all PENDING by default
        updateStatus(ids.get(0), "ACTIVE");

        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(ids.get(0)));

        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void getAllEnrollments_withUserIdFilter_shouldReturnOnlyMatching() throws Exception {
        createThreeEnrollments();

        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("userId", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3));

        // An id with no enrollments must yield an empty page, proving the filter applies.
        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("userId", "999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    void getAllEnrollments_withCourseIdFilter_shouldReturnOnlyMatching() throws Exception {
        createThreeEnrollments();
        long firstCourseId = courseRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("courseId", String.valueOf(firstCourseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void getAllEnrollments_withCombinedFilters_shouldReturnOnlyMatching() throws Exception {
        List<Long> ids = createThreeEnrollments();
        long firstCourseId = courseRepository.findAll().get(0).getId();
        updateStatus(ids.get(0), "ACTIVE");

        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "ACTIVE")
                        .param("userId", String.valueOf(userId))
                        .param("courseId", String.valueOf(firstCourseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].id").value(ids.get(0)));

        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PENDING")
                        .param("courseId", String.valueOf(firstCourseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void getAllEnrollments_withFiltersAndPagination_shouldWorkTogether() throws Exception {
        createThreeEnrollments();

        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(3));
    }

    @Test
    void getAllEnrollments_withInvalidStatus_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ------------------------------------------------------------------
    // CRUD regression coverage
    // ------------------------------------------------------------------

    @Test
    void createEnrollment_shouldReturnCreated() throws Exception {
        Course course = saveCourse("Spring Boot Masterclass", "49.99");

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"courseId\":" + course.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void createEnrollment_withDuplicatePair_shouldReturnConflict() throws Exception {
        Course course = saveCourse("Spring Boot Masterclass", "49.99");
        String body = "{\"userId\":" + userId + ",\"courseId\":" + course.getId() + "}";

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getEnrollmentById_shouldReturnEnrollment() throws Exception {
        long enrollmentId = createOneEnrollment();

        mockMvc.perform(get("/api/enrollments/{id}", enrollmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(enrollmentId))
                .andExpect(jsonPath("$.data.courseTitle").value("Spring Boot Masterclass"));
    }

    @Test
    void getEnrollmentById_withUnknownId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/enrollments/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateEnrollmentStatus_shouldReturnUpdatedEnrollment() throws Exception {
        long enrollmentId = createOneEnrollment();

        mockMvc.perform(put("/api/enrollments/{id}", enrollmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"courseId\":1,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void deleteEnrollment_shouldReturnOk() throws Exception {
        long enrollmentId = createOneEnrollment();

        mockMvc.perform(delete("/api/enrollments/{id}", enrollmentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
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

    private List<Long> createThreeEnrollments() throws Exception {
        List<Long> ids = new ArrayList<>();
        ids.add(createOneEnrollment()); // course 1
        Course second = saveCourse("Advanced Kubernetes", "79.99");
        ids.add(enroll(second.getId()));
        Course third = saveCourse("Data Science", "59.99");
        ids.add(enroll(third.getId()));
        return ids;
    }

    private long createOneEnrollment() throws Exception {
        Course course = saveCourse("Spring Boot Masterclass", "49.99");
        return enroll(course.getId());
    }

    private long enroll(long courseId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/enrollments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"courseId\":" + courseId + "}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return JsonPath.parse(body).read("$.data.id", Long.class);
    }

    private void updateStatus(long enrollmentId, String status) throws Exception {
        mockMvc.perform(put("/api/enrollments/{id}", enrollmentId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + userId + ",\"courseId\":1,\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }
}
