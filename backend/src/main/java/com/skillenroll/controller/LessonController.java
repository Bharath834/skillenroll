package com.skillenroll.controller;

import com.skillenroll.dto.LessonRequest;
import com.skillenroll.dto.LessonResponse;
import com.skillenroll.service.interfaces.LessonService;
import com.skillenroll.util.ApiResponse;
import com.skillenroll.util.PageResponse;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for {@code /api/lessons}. Thin controller - delegates all
 * business logic to {@link LessonService}.
 */
@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @Operation(
            summary = "Create a lesson for a course",
            description = "Creates a lesson linked to an existing course. The lesson order must be unique "
                    + "within that course (409 otherwise). Requires authentication.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Lesson details. courseId, title, lessonOrder and durationMinutes are required.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "CreateLessonRequest",
                                    summary = "Example request",
                                    value = """
                                            {"courseId":4,"title":"Introduction to Python","description":"Python fundamentals","lessonOrder":1,"durationMinutes":45}
                                            """))))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Lesson created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "CreateLessonResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Lesson created successfully","data":{"id":1,"courseId":4,"courseTitle":"Python Full Stack","title":"Introduction to Python","description":"Python fundamentals","lessonOrder":1,"durationMinutes":45,"createdAt":"2026-08-08T10:00:00","updatedAt":"2026-08-08T10:00:00"},"timestamp":"2026-08-08T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed - missing or invalid fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Course not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Lesson order already exists for this course")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<LessonResponse>> createLesson(@Valid @RequestBody LessonRequest request) {
        LessonResponse created = lessonService.createLesson(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Lesson created successfully", created));
    }

    @Operation(
            summary = "Get a lesson by id",
            description = "Returns the full lesson details for the given id, or 404 when it does not exist. "
                    + "Requires authentication.",
            parameters = {@Parameter(name = "id", description = "Lesson id", example = "1")})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lesson found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "GetLessonResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Lesson retrieved successfully","data":{"id":1,"courseId":4,"courseTitle":"Python Full Stack","title":"Introduction to Python","description":"Python fundamentals","lessonOrder":1,"durationMinutes":45,"createdAt":"2026-08-08T10:00:00","updatedAt":"2026-08-08T10:00:00"},"timestamp":"2026-08-08T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> getLessonById(@PathVariable Long id) {
        LessonResponse lesson = lessonService.getLessonById(id);
        return ResponseEntity.ok(ApiResponse.success("Lesson retrieved successfully", lesson));
    }

    @Operation(
            summary = "List lessons for a course (paginated, sortable)",
            description = "Returns a page of lessons belonging to the given course id. Control paging with "
                    + "page/size and order results with sort, e.g. sort=lessonOrder,asc or sort=lessonOrder,desc. "
                    + "Allowed sort fields: id, title, lessonOrder, durationMinutes, createdAt, updatedAt. Page "
                    + "size is capped at 100. Requires authentication.",
            parameters = {
                    @Parameter(name = "courseId", description = "Course id", example = "4")
            })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of lessons for the course",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "ListCourseLessonsResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Lessons retrieved successfully","data":{"content":[{"id":1,"courseId":4,"courseTitle":"Python Full Stack","title":"Introduction to Python","description":"Python fundamentals","lessonOrder":1,"durationMinutes":45,"createdAt":"2026-08-08T10:00:00","updatedAt":"2026-08-08T10:00:00"}],"page":0,"size":10,"totalElements":1,"totalPages":1,"first":true,"last":true,"hasNext":false,"hasPrevious":false},"timestamp":"2026-08-08T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters (page, size or sort)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<PageResponse<LessonResponse>>> getLessonsByCourseId(
            @PathVariable Long courseId,
            @ParameterObject
            @PageableDefault(size = 10, sort = "lessonOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        PageResponse<LessonResponse> lessons = lessonService.getLessonsByCourseId(courseId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lessons retrieved successfully", lessons));
    }

    @Operation(
            summary = "Update a lesson",
            description = "Updates the lesson identified by id. courseId may be changed to move the lesson to "
                    + "another course (404 if that course does not exist). The lesson order must stay unique "
                    + "within its course; keeping the current order is always allowed. Requires authentication.",
            parameters = {@Parameter(name = "id", description = "Lesson id", example = "1")},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Lesson details. courseId, title, lessonOrder and durationMinutes are required.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "UpdateLessonRequest",
                                    summary = "Example request",
                                    value = """
                                            {"courseId":4,"title":"Python Basics","description":"Updated description","lessonOrder":1,"durationMinutes":50}
                                            """))))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lesson updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "UpdateLessonResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Lesson updated successfully","data":{"id":1,"courseId":4,"courseTitle":"Python Full Stack","title":"Python Basics","description":"Updated description","lessonOrder":1,"durationMinutes":50,"createdAt":"2026-08-08T10:00:00","updatedAt":"2026-08-08T10:30:00"},"timestamp":"2026-08-08T10:30:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed - missing or invalid fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Lesson or course not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Lesson order already exists for this course")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LessonResponse>> updateLesson(@PathVariable Long id,
                                                                    @Valid @RequestBody LessonRequest request) {
        LessonResponse updated = lessonService.updateLesson(id, request);
        return ResponseEntity.ok(ApiResponse.success("Lesson updated successfully", updated));
    }

    @Operation(
            summary = "Delete a lesson",
            description = "Permanently deletes the lesson with the given id, or returns 404 when it does not "
                    + "exist. The associated course and its other data are untouched. Requires authentication.",
            parameters = {@Parameter(name = "id", description = "Lesson id", example = "1")})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lesson deleted successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "DeleteLessonResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Lesson deleted successfully","timestamp":"2026-08-08T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Lesson not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLesson(@PathVariable Long id) {
        lessonService.deleteLesson(id);
        return ResponseEntity.ok(ApiResponse.success("Lesson deleted successfully"));
    }
}
