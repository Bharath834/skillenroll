package com.skillenroll.courseservice.controller;

import com.skillenroll.courseservice.entity.Course;
import com.skillenroll.courseservice.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end MockMvc tests for course pagination, search, sorting and error
 * handling against an in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseControllerPaginationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @BeforeEach
    void seedCourses() {
        courseRepository.save(course("Spring Boot Basics", "Programming", "49.99", "Jane Smith"));
        courseRepository.save(course("Advanced Kubernetes", "DevOps", "79.99", "John Doe"));
        courseRepository.save(course("Data Science with Python", "Data", "59.99", "Jane Smith"));
    }

    // ------------------------------------------------------------------
    // Pagination
    // ------------------------------------------------------------------

    @Test
    void getAllCourses_withPagination_shouldReturnPageMetadata() throws Exception {
        mockMvc.perform(get("/api/courses")
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
    void getAllCourses_secondPage_shouldReturnRemainingCourse() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.first").value(false))
                .andExpect(jsonPath("$.data.hasNext").value(false));
    }

    // ------------------------------------------------------------------
    // Sorting
    // ------------------------------------------------------------------

    @Test
    void getAllCourses_sortByPriceDesc_shouldReturnMostExpensiveFirst() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("sort", "price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Advanced Kubernetes"))
                .andExpect(jsonPath("$.data.content[0].price").value(79.99));
    }

    @Test
    void getAllCourses_sortByTitleAsc_shouldReturnAlphabetical() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Advanced Kubernetes"))
                .andExpect(jsonPath("$.data.content[2].title").value("Spring Boot Basics"));
    }

    @Test
    void getAllCourses_withUnknownSortField_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("sort", "unknownField,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid sort field")));
    }

    @Test
    void getAllCourses_withPageSizeAboveCap_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getAllCourses_withWildcardInFilter_shouldMatchLiterally() throws Exception {
        // "%" must not act as a LIKE wildcard: without escaping it would match
        // every seeded course, with escaping it matches nothing.
        mockMvc.perform(get("/api/courses")
                        .param("title", "100%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ------------------------------------------------------------------
    // Search
    // ------------------------------------------------------------------

    @Test
    void getAllCourses_filterByTitle_shouldReturnOnlyMatches() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("title", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Spring Boot Basics"));
    }

    @Test
    void getAllCourses_filterByCategory_shouldReturnOnlyMatches() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("category", "devops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Advanced Kubernetes"));
    }

    @Test
    void getAllCourses_filterByInstructor_shouldReturnOnlyMatches() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("instructor", "jane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @Test
    void getAllCourses_withCombinedFilters_shouldApplyAllPredicates() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .param("title", "python")
                        .param("instructor", "jane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Data Science with Python"));
    }

    @Test
    void searchEndpoint_withTitle_shouldRemainBackwardCompatible() throws Exception {
        mockMvc.perform(get("/api/courses/search")
                        .param("title", "kubernetes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Advanced Kubernetes"));
    }

    // ------------------------------------------------------------------
    // CRUD regression coverage
    // ------------------------------------------------------------------

    @Test
    void createCourse_shouldReturnCreated() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"REST APIs with Spring","category":"Programming","price":39.99,"duration":25,"instructorName":"Jane Smith"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    void createCourse_withDuplicateTitle_shouldReturnConflict() throws Exception {
        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Spring Boot Basics","category":"Programming","price":39.99,"duration":25,"instructorName":"Jane Smith"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getCourseById_shouldReturnCourse() throws Exception {
        Course course = courseRepository.findAll().iterator().next();
        mockMvc.perform(get("/api/courses/{id}", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(course.getId()));
    }

    @Test
    void getCourseById_withUnknownId_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/courses/{id}", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void updateCourse_shouldReturnUpdatedCourse() throws Exception {
        Course course = courseRepository.findAll().iterator().next();
        mockMvc.perform(put("/api/courses/{id}", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Spring Boot Basics - Updated","category":"Programming","price":55.00,"duration":30,"instructorName":"Jane Smith"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Spring Boot Basics - Updated"));
    }

    @Test
    void deleteCourse_shouldReturnOk() throws Exception {
        Course course = courseRepository.findAll().iterator().next();
        mockMvc.perform(delete("/api/courses/{id}", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Course course(String title, String category, String price, String instructor) {
        return Course.builder()
                .title(title)
                .category(category)
                .price(new BigDecimal(price))
                .duration(20)
                .instructorName(instructor)
                .build();
    }
}
