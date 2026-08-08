package com.skillenroll.courseservice.controller;

import com.skillenroll.courseservice.dto.CourseRequest;
import com.skillenroll.courseservice.dto.CourseResponse;
import com.skillenroll.courseservice.service.CourseService;
import com.skillenroll.courseservice.util.ApiResponse;
import com.skillenroll.courseservice.util.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for {@code /api/courses}. Thin controller - delegates all
 * business logic to {@link CourseService}.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(
            summary = "Create a new course",
            description = "Creates a course with the given details and returns it with its generated id. "
                    + "Fails with 409 Conflict when a course with the same title already exists.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Course details to create. All fields except description are required.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "CreateCourseRequest",
                                    summary = "Example request",
                                    value = """
                                            {"title":"Spring Boot Masterclass","description":"Build production-ready REST APIs with Spring Boot","category":"Programming","price":49.99,"duration":40,"instructorName":"Jane Smith"}
                                            """))))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Course created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "CreateCourseResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Course created successfully","data":{"id":1,"title":"Spring Boot Masterclass","description":"Build production-ready REST APIs with Spring Boot","category":"Programming","price":49.99,"duration":40,"instructorName":"Jane Smith","createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed - missing or invalid fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "A course with this title already exists")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(@Valid @RequestBody CourseRequest request) {
        CourseResponse created = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Course created successfully", created));
    }

    @Operation(
            summary = "List courses (paginated, sortable, searchable)",
            description = "Returns a page of courses. All query parameters are optional: filter by title, category "
                    + "or instructor (case-insensitive substring match), control paging with page/size and order "
                    + "results with sort, e.g. sort=createdAt,desc. Allowed sort fields: id, title, description, "
                    + "category, price, duration, instructorName, createdAt, updatedAt. Page size is capped at 100.",
            parameters = {
                    @Parameter(name = "title", description = "Case-insensitive substring match on course title",
                            example = "spring"),
                    @Parameter(name = "category", description = "Case-insensitive substring match on category",
                            example = "programming"),
                    @Parameter(name = "instructor",
                            description = "Case-insensitive substring match on instructor name", example = "jane")
            })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of courses",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "ListCoursesResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Courses retrieved successfully","data":{"content":[{"id":1,"title":"Spring Boot Masterclass","description":"Build production-ready REST APIs","category":"Programming","price":49.99,"duration":40,"instructorName":"Jane Smith","createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"}],"page":0,"size":10,"totalElements":1,"totalPages":1,"first":true,"last":true,"hasNext":false,"hasPrevious":false},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters (page, size, sort or filters)")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> getAllCourses(
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "instructor", required = false) String instructor) {
        PageResponse<CourseResponse> courses = courseService.searchCourses(title, category, instructor, pageable);
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", courses));
    }

    @Operation(
            summary = "Search courses (paginated, sortable)",
            description = "Search endpoint kept for backward compatibility: behaves exactly like GET /api/courses "
                    + "but was originally title-only. Filters by title, category and instructor are optional "
                    + "(case-insensitive substring match); paging and sorting work the same way.",
            parameters = {
                    @Parameter(name = "title", description = "Case-insensitive substring match on course title",
                            example = "spring"),
                    @Parameter(name = "category", description = "Case-insensitive substring match on category",
                            example = "programming"),
                    @Parameter(name = "instructor",
                            description = "Case-insensitive substring match on instructor name", example = "jane")
            })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated search results",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "SearchCoursesResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Courses retrieved successfully","data":{"content":[],"page":0,"size":10,"totalElements":0,"totalPages":0,"first":true,"last":true,"hasNext":false,"hasPrevious":false},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters (page, size, sort or filters)")
    })
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<CourseResponse>>> searchCourses(
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(value = "title", required = false, defaultValue = "") String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "instructor", required = false) String instructor) {
        PageResponse<CourseResponse> courses = courseService.searchCourses(title, category, instructor, pageable);
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", courses));
    }

    @Operation(
            summary = "Get a course by id",
            description = "Returns the full course details for the given id, or 404 when it does not exist.",
            parameters = {@Parameter(name = "id", description = "Course id", example = "1")})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Course found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "GetCourseResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Course retrieved successfully","data":{"id":1,"title":"Spring Boot Masterclass","description":"Build production-ready REST APIs","category":"Programming","price":49.99,"duration":40,"instructorName":"Jane Smith","createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(@PathVariable Long id) {
        CourseResponse course = courseService.getCourseById(id);
        return ResponseEntity.ok(ApiResponse.success("Course retrieved successfully", course));
    }

    @Operation(
            summary = "Update an existing course",
            description = "Replaces the course fields for the given id. Fails with 409 Conflict when another "
                    + "course already uses the new title.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated course details. All fields except description are required.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "UpdateCourseRequest",
                                    summary = "Example request",
                                    value = """
                                            {"title":"Spring Boot Masterclass - Advanced","description":"Now covers testing and deployment","category":"Programming","price":59.99,"duration":45,"instructorName":"Jane Smith"}
                                            """))))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Course updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "UpdateCourseResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Course updated successfully","data":{"id":1,"title":"Spring Boot Masterclass - Advanced","description":"Now covers testing and deployment","category":"Programming","price":59.99,"duration":45,"instructorName":"Jane Smith","createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed - missing or invalid fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Another course already uses this title")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(@PathVariable Long id,
                                                                    @Valid @RequestBody CourseRequest request) {
        CourseResponse updated = courseService.updateCourse(id, request);
        return ResponseEntity.ok(ApiResponse.success("Course updated successfully", updated));
    }

    @Operation(
            summary = "Delete a course",
            description = "Permanently deletes the course with the given id, or returns 404 when it does not exist.",
            parameters = {@Parameter(name = "id", description = "Course id", example = "1")})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Course deleted successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "DeleteCourseResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Course deleted successfully","timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success("Course deleted successfully"));
    }
}
