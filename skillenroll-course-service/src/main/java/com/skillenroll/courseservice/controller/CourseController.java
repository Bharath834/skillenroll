package com.skillenroll.courseservice.controller;

import com.skillenroll.courseservice.dto.CourseRequest;
import com.skillenroll.courseservice.dto.CourseResponse;
import com.skillenroll.courseservice.service.CourseService;
import com.skillenroll.courseservice.util.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    public ResponseEntity<ApiResponse<CourseResponse>> createCourse(@Valid @RequestBody CourseRequest request) {
        CourseResponse created = courseService.createCourse(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Course created successfully", created));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CourseResponse>>> getAllCourses() {
        List<CourseResponse> courses = courseService.getAllCourses();
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", courses));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<CourseResponse>>> searchCourses(
            @RequestParam(value = "title", required = false, defaultValue = "") String title) {
        List<CourseResponse> courses = courseService.searchCoursesByTitle(title);
        return ResponseEntity.ok(ApiResponse.success("Courses retrieved successfully", courses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> getCourseById(@PathVariable Long id) {
        CourseResponse course = courseService.getCourseById(id);
        return ResponseEntity.ok(ApiResponse.success("Course retrieved successfully", course));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CourseResponse>> updateCourse(@PathVariable Long id,
                                                                    @Valid @RequestBody CourseRequest request) {
        CourseResponse updated = courseService.updateCourse(id, request);
        return ResponseEntity.ok(ApiResponse.success("Course updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success("Course deleted successfully"));
    }
}
