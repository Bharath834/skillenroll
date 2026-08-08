package com.skillenroll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end MockMvc tests for the lesson module against the real security
 * chain with an in-memory H2 database: creation rules (course requirement,
 * per-course lesson order uniqueness), field validation, the paginated and
 * sortable course listing, update/delete and the Swagger documentation.
 * Every test rolls back its own data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class LessonControllerTest {

    private static final String STUDENT_EMAIL = "lesson.student@test.com";
    private static final String STUDENT_PHONE = "9777777777";
    private static final String PASSWORD = "Passw0rd!";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    /** JWT for the registered student. */
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        token = registerStudent();
    }

    // ------------------------------------------------------------------
    // Create
    // ------------------------------------------------------------------

    @Test
    void createLesson_shouldReturnCreated() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(courseId, "Introduction to Python", "Python fundamentals", 1, 45)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.courseTitle").value("Python Full Stack"))
                .andExpect(jsonPath("$.data.title").value("Introduction to Python"))
                .andExpect(jsonPath("$.data.description").value("Python fundamentals"))
                .andExpect(jsonPath("$.data.lessonOrder").value(1))
                .andExpect(jsonPath("$.data.durationMinutes").value(45))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
    }

    @Test
    void createLesson_withNonExistingCourse_shouldReturnNotFound() throws Exception {
        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(999999L, "Intro", null, 1, 45)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Course not found with id: 999999"));
    }

    @Test
    void createLesson_withDuplicateLessonOrderInSameCourse_shouldReturnConflict() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();
        createLesson(courseId, "Introduction to Python", 1);

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(courseId, "Second lesson", null, 1, 30)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Lesson order already exists for this course"));
    }

    @Test
    void createLesson_sameOrderInDifferentCourses_shouldBeAllowed() throws Exception {
        long first = saveCourse("Python Full Stack", "49.99").getId();
        long second = saveCourse("Advanced Kubernetes", "79.99").getId();

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(first, "Intro to Python", null, 1, 45)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(second, "Intro to Kubernetes", null, 1, 40)))
                .andExpect(status().isCreated());
    }

    @Test
    void createLesson_withLessonOrderBelowOne_shouldReturnBadRequest() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(courseId, "Intro", null, 0, 45)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void createLesson_withNonPositiveDuration_shouldReturnBadRequest() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();

        mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(courseId, "Intro", null, 1, 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void createLesson_withoutToken_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(1L, "Intro", null, 1, 45)))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Read
    // ------------------------------------------------------------------

    @Test
    void getLessonById_shouldReturnLesson() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();
        long lessonId = createLesson(courseId, "Introduction to Python", 1);

        mockMvc.perform(get("/api/lessons/{id}", lessonId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(lessonId))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.courseTitle").value("Python Full Stack"))
                .andExpect(jsonPath("$.data.title").value("Introduction to Python"))
                .andExpect(jsonPath("$.data.lessonOrder").value(1));
    }

    @Test
    void getLessonById_withUnknownId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/lessons/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lesson not found with id: 999999"));
    }

    // ------------------------------------------------------------------
    // Course listing: pagination and sorting
    // ------------------------------------------------------------------

    @Test
    void getLessonsByCourseId_shouldReturnPaginatedResults() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();
        createLesson(courseId, "Lesson A", 1);
        createLesson(courseId, "Lesson B", 2);
        createLesson(courseId, "Lesson C", 3);

        mockMvc.perform(get("/api/lessons/course/{courseId}", courseId)
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
    void getLessonsByCourseId_sortByLessonOrderDesc_shouldReturnNewestFirst() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();
        createLesson(courseId, "Lesson A", 3);
        createLesson(courseId, "Lesson B", 1);
        createLesson(courseId, "Lesson C", 2);

        mockMvc.perform(get("/api/lessons/course/{courseId}", courseId)
                        .header("Authorization", "Bearer " + token)
                        .param("sort", "lessonOrder,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].lessonOrder").value(3))
                .andExpect(jsonPath("$.data.content[1].lessonOrder").value(2))
                .andExpect(jsonPath("$.data.content[2].lessonOrder").value(1));
    }

    @Test
    void getLessonsByCourseId_sortByLessonOrderAsc_shouldReturnOldestFirst() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();
        createLesson(courseId, "Lesson A", 3);
        createLesson(courseId, "Lesson B", 1);
        createLesson(courseId, "Lesson C", 2);

        mockMvc.perform(get("/api/lessons/course/{courseId}", courseId)
                        .header("Authorization", "Bearer " + token)
                        .param("sort", "lessonOrder,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].lessonOrder").value(1))
                .andExpect(jsonPath("$.data.content[1].lessonOrder").value(2))
                .andExpect(jsonPath("$.data.content[2].lessonOrder").value(3));
    }

    @Test
    void getLessonsByCourseId_onlyReturnsLessonsOfThatCourse() throws Exception {
        long first = saveCourse("Python Full Stack", "49.99").getId();
        long second = saveCourse("Advanced Kubernetes", "79.99").getId();
        createLesson(first, "Python A", 1);
        createLesson(first, "Python B", 2);
        createLesson(second, "K8s A", 1);

        mockMvc.perform(get("/api/lessons/course/{courseId}", first)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));

        mockMvc.perform(get("/api/lessons/course/{courseId}", second)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("K8s A"));
    }

    @Test
    void getLessonsByCourseId_withUnknownSortField_shouldReturnBadRequest() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();

        mockMvc.perform(get("/api/lessons/course/{courseId}", courseId)
                        .header("Authorization", "Bearer " + token)
                        .param("sort", "unknownField,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("Invalid sort field")));
    }

    @Test
    void getLessonsByCourseId_withPageSizeAboveCap_shouldReturnBadRequest() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();

        mockMvc.perform(get("/api/lessons/course/{courseId}", courseId)
                        .header("Authorization", "Bearer " + token)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ------------------------------------------------------------------
    // Update
    // ------------------------------------------------------------------

    @Test
    void updateLesson_shouldReturnUpdatedLesson() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();
        long lessonId = createLesson(courseId, "Introduction to Python", 1);

        mockMvc.perform(put("/api/lessons/{id}", lessonId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(courseId, "Python Basics", "Updated description", 1, 50)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(lessonId))
                .andExpect(jsonPath("$.data.title").value("Python Basics"))
                .andExpect(jsonPath("$.data.description").value("Updated description"))
                .andExpect(jsonPath("$.data.durationMinutes").value(50))
                .andExpect(jsonPath("$.data.lessonOrder").value(1));
    }

    @Test
    void updateLesson_keepingSameOrder_shouldBeAllowed() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();
        long first = createLesson(courseId, "Lesson A", 1);
        createLesson(courseId, "Lesson B", 2);

        mockMvc.perform(put("/api/lessons/{id}", first)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(courseId, "Lesson A renamed", null, 1, 30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Lesson A renamed"));
    }

    @Test
    void updateLesson_withOrderUsedByAnotherLessonInSameCourse_shouldReturnConflict() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();
        long first = createLesson(courseId, "Lesson A", 1);
        createLesson(courseId, "Lesson B", 2);

        mockMvc.perform(put("/api/lessons/{id}", first)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(courseId, "Lesson A", null, 2, 30)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Lesson order already exists for this course"));
    }

    @Test
    void updateLesson_movingToAnotherCourse_shouldReturnUpdatedLesson() throws Exception {
        long first = saveCourse("Python Full Stack", "49.99").getId();
        long second = saveCourse("Advanced Kubernetes", "79.99").getId();
        long lessonId = createLesson(first, "Lesson A", 1);

        mockMvc.perform(put("/api/lessons/{id}", lessonId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(second, "Lesson A", null, 2, 30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(second))
                .andExpect(jsonPath("$.data.courseTitle").value("Advanced Kubernetes"))
                .andExpect(jsonPath("$.data.lessonOrder").value(2));
    }

    @Test
    void updateLesson_movingToCourseWithTakenOrder_shouldReturnConflict() throws Exception {
        long first = saveCourse("Python Full Stack", "49.99").getId();
        long second = saveCourse("Advanced Kubernetes", "79.99").getId();
        long lessonId = createLesson(first, "Lesson A", 1);
        createLesson(second, "K8s Lesson", 1);

        mockMvc.perform(put("/api/lessons/{id}", lessonId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(second, "Lesson A", null, 1, 30)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Lesson order already exists for this course"));
    }

    @Test
    void updateLesson_withNonExistingCourse_shouldReturnNotFound() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();
        long lessonId = createLesson(courseId, "Lesson A", 1);

        mockMvc.perform(put("/api/lessons/{id}", lessonId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(999999L, "Lesson A", null, 1, 30)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found with id: 999999"));
    }

    @Test
    void updateLesson_withUnknownId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(put("/api/lessons/{id}", 999999L)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(1L, "Lesson A", null, 1, 30)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lesson not found with id: 999999"));
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------

    @Test
    void deleteLesson_shouldReturnOk() throws Exception {
        long courseId = saveCourse("Python Full Stack", "49.99").getId();
        long lessonId = createLesson(courseId, "Lesson A", 1);

        mockMvc.perform(delete("/api/lessons/{id}", lessonId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/lessons/{id}", lessonId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLesson_withUnknownId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(delete("/api/lessons/{id}", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLesson_shouldNotDeleteTheCourse() throws Exception {
        Course course = saveCourse("Python Full Stack", "49.99");
        long lessonId = createLesson(course.getId(), "Lesson A", 1);

        mockMvc.perform(delete("/api/lessons/{id}", lessonId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/courses/{id}", course.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(course.getId()));
    }

    // ------------------------------------------------------------------
    // Swagger documentation
    // ------------------------------------------------------------------

    @Test
    void openApiSpec_shouldDocumentAllLessonEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/lessons'].post").exists())
                .andExpect(jsonPath("$.paths['/api/lessons/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/lessons/{id}'].put").exists())
                .andExpect(jsonPath("$.paths['/api/lessons/{id}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/lessons/course/{courseId}'].get").exists())
                .andExpect(jsonPath("$.components.schemas.LessonRequest").exists())
                .andExpect(jsonPath("$.components.schemas.LessonResponse").exists());
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

    private String lessonBody(long courseId, String title, String description, int order, int duration)
            throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("courseId", courseId);
        body.put("title", title);
        if (description != null) {
            body.put("description", description);
        }
        body.put("lessonOrder", order);
        body.put("durationMinutes", duration);
        return objectMapper.writeValueAsString(body);
    }

    private long createLesson(long courseId, String title, int order) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/lessons")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(lessonBody(courseId, title, null, order, 45)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return JsonPath.parse(body).read("$.data.id", Long.class);
    }
}
