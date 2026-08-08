package com.skillenroll.courseservice.service;

import com.skillenroll.courseservice.dto.CourseRequest;
import com.skillenroll.courseservice.dto.CourseResponse;
import com.skillenroll.courseservice.util.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Business operations for {@link com.skillenroll.courseservice.entity.Course}.
 */
public interface CourseService {

    CourseResponse createCourse(CourseRequest request);

    CourseResponse getCourseById(Long id);

    /**
     * Legacy list of every course. Preserved for service-layer backward
     * compatibility; the controller uses the paginated
     * {@link #searchCourses(String, String, String, Pageable)} instead.
     */
    List<CourseResponse> getAllCourses();

    /**
     * Legacy title-only search. Preserved for service-layer backward
     * compatibility; the controller uses the paginated
     * {@link #searchCourses(String, String, String, Pageable)} instead.
     */
    List<CourseResponse> searchCoursesByTitle(String title);

    /**
     * Paginated, case-insensitive search across course title, category and
     * instructor name. Any of the filters may be {@code null} to skip it, so
     * this method also serves the plain list-all case.
     */
    PageResponse<CourseResponse> searchCourses(String title, String category, String instructor, Pageable pageable);

    CourseResponse updateCourse(Long id, CourseRequest request);

    void deleteCourse(Long id);
}
