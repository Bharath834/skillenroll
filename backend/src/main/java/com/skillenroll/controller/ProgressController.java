package com.skillenroll.controller;

import com.skillenroll.dto.ProgressRequest;
import com.skillenroll.dto.ProgressResponse;
import com.skillenroll.service.interfaces.ProgressService;
import com.skillenroll.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
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

import java.util.List;

/**
 * REST endpoints for {@code /api/progress}. Thin controller - delegates all
 * business logic to {@link ProgressService}.
 */
@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    @Operation(
            summary = "Create course progress for an enrolled user",
            description = "Creates a progress record linking an existing user to an existing course. The user must "
                    + "be enrolled in the course (404 otherwise) and must not already have a progress record for it "
                    + "(409 otherwise). A percentage of 100 immediately marks the course completed. Requires "
                    + "authentication.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Progress details. progressPercentage must be between 0 and 100.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "CreateProgressRequest",
                                    summary = "Example request",
                                    value = """
                                            {"userId":1,"courseId":2,"progressPercentage":45.50}
                                            """))))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Progress created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "CreateProgressResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Progress created successfully","data":{"id":1,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","progressPercentage":45.50,"completed":false,"startedAt":"2026-08-07T10:00:00","completedAt":null,"createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed - missing or invalid fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User, course or enrollment not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Progress already exists for this user and course")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ProgressResponse>> createProgress(
            @Valid @RequestBody ProgressRequest request) {
        ProgressResponse created = progressService.createProgress(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Progress created successfully", created));
    }

    @Operation(
            summary = "Get progress by id",
            description = "Returns the full progress record for the given id, or 404 when it does not exist. "
                    + "Requires authentication.",
            parameters = {@Parameter(name = "id", description = "Progress id", example = "1")})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Progress found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "GetProgressResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Progress retrieved successfully","data":{"id":1,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","progressPercentage":45.50,"completed":false,"startedAt":"2026-08-07T10:00:00","completedAt":null,"createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Progress not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProgressResponse>> getProgressById(@PathVariable Long id) {
        ProgressResponse progress = progressService.getProgressById(id);
        return ResponseEntity.ok(ApiResponse.success("Progress retrieved successfully", progress));
    }

    @Operation(
            summary = "List all progress records for a user",
            description = "Returns every progress record belonging to the given user id. Requires authentication.",
            parameters = {@Parameter(name = "userId", description = "User id", example = "1")})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of progress records for the user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "ListUserProgressResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Progress retrieved successfully","data":[{"id":1,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","progressPercentage":45.50,"completed":false,"startedAt":"2026-08-07T10:00:00","completedAt":null,"createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"}],"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ProgressResponse>>> getProgressByUserId(@PathVariable Long userId) {
        List<ProgressResponse> progress = progressService.getProgressByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Progress retrieved successfully", progress));
    }

    @Operation(
            summary = "List all progress records for a course",
            description = "Returns every progress record for the given course id. Requires authentication.",
            parameters = {@Parameter(name = "courseId", description = "Course id", example = "2")})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "List of progress records for the course",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "ListCourseProgressResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Progress retrieved successfully","data":[{"id":1,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","progressPercentage":45.50,"completed":false,"startedAt":"2026-08-07T10:00:00","completedAt":null,"createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"}],"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<List<ProgressResponse>>> getProgressByCourseId(@PathVariable Long courseId) {
        List<ProgressResponse> progress = progressService.getProgressByCourseId(courseId);
        return ResponseEntity.ok(ApiResponse.success("Progress retrieved successfully", progress));
    }

    @Operation(
            summary = "Get progress for one user in one course",
            description = "Returns the single progress record for the given user/course pair, or 404 when none "
                    + "exists. Requires authentication.",
            parameters = {
                    @Parameter(name = "userId", description = "User id", example = "1"),
                    @Parameter(name = "courseId", description = "Course id", example = "2")
            })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Progress found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "GetUserCourseProgressResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Progress retrieved successfully","data":{"id":1,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","progressPercentage":45.50,"completed":false,"startedAt":"2026-08-07T10:00:00","completedAt":null,"createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Progress not found for this user and course")
    })
    @GetMapping("/user/{userId}/course/{courseId}")
    public ResponseEntity<ApiResponse<ProgressResponse>> getProgressByUserAndCourse(
            @PathVariable Long userId, @PathVariable Long courseId) {
        ProgressResponse progress = progressService.getProgressByUserAndCourse(userId, courseId);
        return ResponseEntity.ok(ApiResponse.success("Progress retrieved successfully", progress));
    }

    /**
     * Updates the progress record identified by {@code id}. Only
     * {@code progressPercentage} is applied; {@code userId}/{@code courseId}
     * are validated but ignored (a progress record is pinned to its user and
     * course). Setting the percentage to 100 marks the course completed and
     * populates {@code completedAt}.
     */
    @Operation(
            summary = "Update course progress",
            description = "Updates the progress percentage of the record identified by id. Only progressPercentage "
                    + "is applied; userId and courseId are validated but ignored because a progress record is pinned "
                    + "to its user and course. Values below 0 or above 100 are rejected (400). Setting 100 marks the "
                    + "course completed (completed=true, completedAt populated); anything below keeps it "
                    + "not-completed. Requires authentication.",
            parameters = {@Parameter(name = "id", description = "Progress id", example = "1")},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Progress details; only progressPercentage is applied. Must be between 0 and 100.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "UpdateProgressRequest",
                                    summary = "Example request",
                                    value = """
                                            {"userId":1,"courseId":2,"progressPercentage":100.00}
                                            """))))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Progress updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "UpdateProgressResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Progress updated successfully","data":{"id":1,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","progressPercentage":100.00,"completed":true,"startedAt":"2026-08-07T10:00:00","completedAt":"2026-08-07T10:30:00","createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:30:00"},"timestamp":"2026-08-07T10:30:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed - missing, invalid or out-of-range fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Progress not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProgressResponse>> updateProgress(
            @PathVariable Long id, @Valid @RequestBody ProgressRequest request) {
        ProgressResponse updated = progressService.updateProgress(id, request);
        return ResponseEntity.ok(ApiResponse.success("Progress updated successfully", updated));
    }

    @Operation(
            summary = "Delete course progress",
            description = "Permanently deletes the progress record with the given id, or returns 404 when it does "
                    + "not exist. Requires authentication.",
            parameters = {@Parameter(name = "id", description = "Progress id", example = "1")})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Progress deleted successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "DeleteProgressResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Progress deleted successfully","timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Progress not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProgress(@PathVariable Long id) {
        progressService.deleteProgress(id);
        return ResponseEntity.ok(ApiResponse.success("Progress deleted successfully"));
    }
}
