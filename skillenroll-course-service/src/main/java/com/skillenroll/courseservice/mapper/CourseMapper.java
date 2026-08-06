package com.skillenroll.courseservice.mapper;

import com.skillenroll.courseservice.dto.CourseRequest;
import com.skillenroll.courseservice.dto.CourseResponse;
import com.skillenroll.courseservice.entity.Course;

/**
 * Manual mapping between {@link Course}, {@link CourseRequest} and {@link CourseResponse}.
 */
public final class CourseMapper {

    private CourseMapper() {
    }

    public static Course toEntity(CourseRequest request) {
        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCategory(request.getCategory());
        course.setPrice(request.getPrice());
        course.setDuration(request.getDuration());
        course.setInstructorName(request.getInstructorName());
        return course;
    }

    public static CourseResponse toResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .category(course.getCategory())
                .price(course.getPrice())
                .duration(course.getDuration())
                .instructorName(course.getInstructorName())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    public static void updateEntity(Course course, CourseRequest request) {
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setCategory(request.getCategory());
        course.setPrice(request.getPrice());
        course.setDuration(request.getDuration());
        course.setInstructorName(request.getInstructorName());
    }
}
