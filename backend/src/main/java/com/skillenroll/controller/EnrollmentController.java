package com.skillenroll.controller;

import com.skillenroll.dto.EnrollmentRequest;
import com.skillenroll.dto.EnrollmentResponse;
import com.skillenroll.enums.EnrollmentStatus;
import com.skillenroll.service.interfaces.EnrollmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for {@code /api/enrollments}. Thin controller - delegates all
 * business logic to {@link EnrollmentService}.
 */
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @Operation(
            summary = "Enroll a user in a course",
            description = "Creates an enrollment linking an existing user to an existing course. Fails with 409 "
                    + "Conflict when the user is already enrolled in that course. Requires authentication.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Enrollment details. On create, status is optional and defaults to PENDING.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "CreateEnrollmentRequest",
                                    summary = "Example request",
                                    value = """
                                            {"userId":1,"courseId":2,"status":"PENDING"}
                                            """))))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Enrollment created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "CreateEnrollmentResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Enrollment created successfully","data":{"id":5,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","status":"PENDING","enrollmentDate":"2026-08-07T10:00:00","createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed - missing or invalid fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User or course not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "User is already enrolled in this course")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<EnrollmentResponse>> createEnrollment(
            @Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse created = enrollmentService.createEnrollment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Enrollment created successfully", created));
    }

    @Operation(
            summary = "List enrollments (paginated, sortable, filterable)",
            description = "Returns a page of all enrollments, optionally narrowed with the status, userId and "
                    + "courseId filters (all optional - omit any of them to disable that filter). Control paging "
                    + "with page/size and order results with sort, e.g. sort=enrollmentDate,desc. Allowed sort "
                    + "fields: id, status, enrollmentDate, createdAt, updatedAt, userId, courseId. Page size is "
                    + "capped at 100. Requires authentication.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of enrollments",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "ListEnrollmentsResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Enrollments retrieved successfully","data":{"content":[{"id":5,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","status":"PENDING","enrollmentDate":"2026-08-07T10:00:00","createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"}],"page":0,"size":10,"totalElements":1,"totalPages":1,"first":true,"last":true,"hasNext":false,"hasPrevious":false},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters (page, size, sort, status, userId or courseId)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EnrollmentResponse>>> getAllEnrollments(
            @RequestParam(required = false)
            @Parameter(name = "status", description = "Filter by exact enrollment status. One of: PENDING, ACTIVE, "
                    + "COMPLETED, CANCELLED.", example = "ACTIVE")
            EnrollmentStatus status,
            @RequestParam(required = false)
            @Parameter(name = "userId", description = "Filter by the enrolling user's id.", example = "1")
            Long userId,
            @RequestParam(required = false)
            @Parameter(name = "courseId", description = "Filter by the course's id.", example = "2")
            Long courseId,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<EnrollmentResponse> enrollments =
                enrollmentService.getAllEnrollments(status, userId, courseId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Enrollments retrieved successfully", enrollments));
    }

    @Operation(
            summary = "List enrollments for a user (paginated, sortable)",
            description = "Returns a page of enrollments for the given user id. Sorting and paging work the same "
                    + "way as GET /api/enrollments. Requires authentication.",
            parameters = {
                    @Parameter(name = "userId", description = "User id", example = "1")
            })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of enrollments for the user",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "ListUserEnrollmentsResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Enrollments retrieved successfully","data":{"content":[{"id":5,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","status":"PENDING","enrollmentDate":"2026-08-07T10:00:00","createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"}],"page":0,"size":10,"totalElements":1,"totalPages":1,"first":true,"last":true,"hasNext":false,"hasPrevious":false},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters (page, size or sort)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<EnrollmentResponse>>> getEnrollmentsByUserId(
            @PathVariable Long userId,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<EnrollmentResponse> enrollments = enrollmentService.getEnrollmentsByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Enrollments retrieved successfully", enrollments));
    }

    @Operation(
            summary = "List enrollments for a course (paginated, sortable)",
            description = "Returns a page of enrollments for the given course id. Sorting and paging work the same "
                    + "way as GET /api/enrollments. Requires authentication.",
            parameters = {
                    @Parameter(name = "courseId", description = "Course id", example = "2")
            })
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Paginated list of enrollments for the course",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "ListCourseEnrollmentsResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Enrollments retrieved successfully","data":{"content":[{"id":5,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","status":"PENDING","enrollmentDate":"2026-08-07T10:00:00","createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"}],"page":0,"size":10,"totalElements":1,"totalPages":1,"first":true,"last":true,"hasNext":false,"hasPrevious":false},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid query parameters (page, size or sort)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/course/{courseId}")
    public ResponseEntity<ApiResponse<PageResponse<EnrollmentResponse>>> getEnrollmentsByCourseId(
            @PathVariable Long courseId,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<EnrollmentResponse> enrollments = enrollmentService.getEnrollmentsByCourseId(courseId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Enrollments retrieved successfully", enrollments));
    }

    @Operation(
            summary = "Get an enrollment by id",
            description = "Returns the full enrollment details for the given id, or 404 when it does not exist. "
                    + "Requires authentication.",
            parameters = {@Parameter(name = "id", description = "Enrollment id", example = "5")})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollment found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "GetEnrollmentResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Enrollment retrieved successfully","data":{"id":5,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","status":"PENDING","enrollmentDate":"2026-08-07T10:00:00","createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> getEnrollmentById(@PathVariable Long id) {
        EnrollmentResponse enrollment = enrollmentService.getEnrollmentById(id);
        return ResponseEntity.ok(ApiResponse.success("Enrollment retrieved successfully", enrollment));
    }

    /**
     * Updates the enrollment identified by {@code id}. Only {@code status}
     * is applied; {@code userId}/{@code courseId} are validated but ignored
     * (an enrollment is pinned to its user and course).
     */
    @Operation(
            summary = "Update an enrollment status",
            description = "Updates the status of the enrollment identified by id. Only status is applied; "
                    + "userId and courseId are validated but ignored because an enrollment is pinned to its user "
                    + "and course. Requires authentication.",
            parameters = {@Parameter(name = "id", description = "Enrollment id", example = "5")},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Enrollment details; only status is applied. Allowed statuses: PENDING, ACTIVE, "
                            + "COMPLETED, CANCELLED.",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "UpdateEnrollmentRequest",
                                    summary = "Example request",
                                    value = """
                                            {"userId":1,"courseId":2,"status":"ACTIVE"}
                                            """))))
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollment updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "UpdateEnrollmentResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Enrollment updated successfully","data":{"id":5,"userId":1,"userName":"Bharath Kumar","courseId":2,"courseTitle":"Spring Boot Masterclass","status":"ACTIVE","enrollmentDate":"2026-08-07T10:00:00","createdAt":"2026-08-07T10:00:00","updatedAt":"2026-08-07T10:00:00"},"timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed - missing or invalid fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> updateEnrollmentStatus(
            @PathVariable Long id, @Valid @RequestBody EnrollmentRequest request) {
        EnrollmentResponse updated = enrollmentService.updateEnrollmentStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Enrollment updated successfully", updated));
    }

    @Operation(
            summary = "Delete an enrollment",
            description = "Permanently deletes the enrollment with the given id, or returns 404 when it does not "
                    + "exist. Requires authentication.",
            parameters = {@Parameter(name = "id", description = "Enrollment id", example = "5")})
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enrollment deleted successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(name = "DeleteEnrollmentResponse",
                                    summary = "Example response",
                                    value = """
                                            {"success":true,"message":"Enrollment deleted successfully","timestamp":"2026-08-07T10:00:00"}
                                            """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Enrollment not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEnrollment(@PathVariable Long id) {
        enrollmentService.deleteEnrollment(id);
        return ResponseEntity.ok(ApiResponse.success("Enrollment deleted successfully"));
    }
}
