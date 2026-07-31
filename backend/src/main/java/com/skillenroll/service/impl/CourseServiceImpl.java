package com.skillenroll.service.impl;

import com.skillenroll.dto.CourseRequest;
import com.skillenroll.dto.CourseResponse;
import com.skillenroll.entity.Course;
import com.skillenroll.exception.DuplicateResourceException;
import com.skillenroll.exception.ResourceNotFoundException;
import com.skillenroll.mapper.CourseMapper;
import com.skillenroll.repository.CourseRepository;
import com.skillenroll.service.interfaces.CourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link CourseService}. All business logic lives here;
 * the controller layer stays thin.
 */
@Service
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;

    public CourseServiceImpl(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        if (courseRepository.existsByTitle(request.getTitle())) {
            throw new DuplicateResourceException("Course with title '" + request.getTitle() + "' already exists");
        }
        Course course = CourseMapper.toEntity(request);
        return CourseMapper.toResponse(courseRepository.save(course));
    }

    @Override
    public CourseResponse getCourseById(Long id) {
        return CourseMapper.toResponse(findCourseOrThrow(id));
    }

    @Override
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(CourseMapper::toResponse)
                .toList();
    }

    @Override
    public List<CourseResponse> searchCoursesByTitle(String title) {
        return courseRepository.findByTitleContainingIgnoreCase(title).stream()
                .map(CourseMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = findCourseOrThrow(id);
        if (courseRepository.existsByTitleAndIdNot(request.getTitle(), id)) {
            throw new DuplicateResourceException("Course with title '" + request.getTitle() + "' already exists");
        }
        CourseMapper.updateEntity(course, request);
        courseRepository.flush(); // trigger @PreUpdate so updatedAt is fresh
        return CourseMapper.toResponse(course);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.delete(findCourseOrThrow(id));
    }

    private Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }
}
