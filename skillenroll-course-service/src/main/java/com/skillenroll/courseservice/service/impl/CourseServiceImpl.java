package com.skillenroll.courseservice.service.impl;

import com.skillenroll.courseservice.dto.CourseRequest;
import com.skillenroll.courseservice.dto.CourseResponse;
import com.skillenroll.courseservice.entity.Course;
import com.skillenroll.courseservice.exception.DuplicateResourceException;
import com.skillenroll.courseservice.exception.ResourceNotFoundException;
import com.skillenroll.courseservice.mapper.CourseMapper;
import com.skillenroll.courseservice.repository.CourseRepository;
import com.skillenroll.courseservice.service.CourseService;
import com.skillenroll.courseservice.util.PageResponse;
import com.skillenroll.courseservice.util.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link CourseService}. All business logic lives here;
 * the controller layer stays thin.
 */
@Service
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseServiceImpl.class);

    /** Sort properties exposed to clients (entity property names). */
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "title", "description", "category", "price",
            "duration", "instructorName", "createdAt", "updatedAt");

    private final CourseRepository courseRepository;

    public CourseServiceImpl(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        if (courseRepository.existsByTitle(request.getTitle())) {
            log.warn("Course creation rejected - title already exists: '{}'", request.getTitle());
            throw new DuplicateResourceException("Course with title '" + request.getTitle() + "' already exists");
        }
        Course course = CourseMapper.toEntity(request);
        Course saved = courseRepository.save(course);
        log.info("Course created with id {}", saved.getId());
        return CourseMapper.toResponse(saved);
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
    public PageResponse<CourseResponse> searchCourses(
            String title,
            String category,
            String instructor,
            Pageable pageable) {

        Pageable safePageable = PaginationUtils.normalize(
                pageable,
                ALLOWED_SORT_PROPERTIES,
                Map.of());

        System.out.println("Title      = " + normalize(title));
        System.out.println("Category   = " + normalize(category));
        System.out.println("Instructor = " + normalize(instructor));

        return PageResponse.from(
                courseRepository.search(
                        normalize(title),
                        normalize(category),
                        normalize(instructor),
                        safePageable),
                CourseMapper::toResponse);
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = findCourseOrThrow(id);
        if (courseRepository.existsByTitleAndIdNot(request.getTitle(), id)) {
            log.warn("Course update rejected - title already exists: '{}'", request.getTitle());
            throw new DuplicateResourceException("Course with title '" + request.getTitle() + "' already exists");
        }
        CourseMapper.updateEntity(course, request);
        courseRepository.flush(); // trigger @PreUpdate so updatedAt is fresh
        log.info("Course updated with id {}", id);
        return CourseMapper.toResponse(course);
    }

    @Override
    @Transactional
    public void deleteCourse(Long id) {
        courseRepository.delete(findCourseOrThrow(id));
        log.info("Course deleted with id {}", id);
    }

    private Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }

    /**
     * Normalizes a nullable filter to an empty string (disables the
     * predicate) and escapes LIKE wildcards so user input matches literally.
     */
    private String normalize(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
