package com.skillenroll.service.interfaces;

import com.skillenroll.dto.CourseRequest;
import com.skillenroll.dto.CourseResponse;

import java.util.List;

/**
 * Business operations for {@link com.skillenroll.entity.Course}.
 */
public interface CourseService {

    CourseResponse createCourse(CourseRequest request);

    CourseResponse getCourseById(Long id);

    List<CourseResponse> getAllCourses();

    List<CourseResponse> searchCoursesByTitle(String title);

    CourseResponse updateCourse(Long id, CourseRequest request);

    void deleteCourse(Long id);
}
