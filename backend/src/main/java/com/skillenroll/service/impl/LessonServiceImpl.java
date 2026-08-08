package com.skillenroll.service.impl;

import com.skillenroll.dto.LessonRequest;
import com.skillenroll.dto.LessonResponse;
import com.skillenroll.entity.Course;
import com.skillenroll.entity.Lesson;
import com.skillenroll.exception.DuplicateResourceException;
import com.skillenroll.exception.ResourceNotFoundException;
import com.skillenroll.mapper.LessonMapper;
import com.skillenroll.repository.CourseRepository;
import com.skillenroll.repository.LessonRepository;
import com.skillenroll.service.interfaces.LessonService;
import com.skillenroll.util.PageResponse;
import com.skillenroll.util.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link LessonService}. All business logic lives here;
 * the controller layer stays thin.
 */
@Service
@Transactional(readOnly = true)
public class LessonServiceImpl implements LessonService {

    private static final Logger log = LoggerFactory.getLogger(LessonServiceImpl.class);

    /** Sort properties exposed to clients. */
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "title", "lessonOrder", "durationMinutes", "createdAt", "updatedAt");

    private final LessonRepository lessonRepository;
    private final CourseRepository courseRepository;

    public LessonServiceImpl(LessonRepository lessonRepository, CourseRepository courseRepository) {
        this.lessonRepository = lessonRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public LessonResponse createLesson(LessonRequest request) {
        Course course = findCourseOrThrow(request.getCourseId());

        if (lessonRepository.existsByCourseIdAndLessonOrder(request.getCourseId(), request.getLessonOrder())) {
            log.warn("Lesson rejected - lesson order {} already exists for course {}",
                    request.getLessonOrder(), request.getCourseId());
            throw new DuplicateResourceException("Lesson order already exists for this course");
        }

        Lesson lesson = LessonMapper.toEntity(request, course);
        Lesson saved = lessonRepository.save(lesson);
        log.info("Lesson created with id {} (course {}, order {})", saved.getId(),
                saved.getCourse().getId(), saved.getLessonOrder());
        return LessonMapper.toResponse(saved);
    }

    @Override
    public LessonResponse getLessonById(Long id) {
        return LessonMapper.toResponse(findLessonOrThrow(id));
    }

    @Override
    public List<LessonResponse> getAllLessons() {
        return lessonRepository.findAll().stream()
                .map(LessonMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<LessonResponse> getLessonsByCourseId(Long courseId, Pageable pageable) {
        Pageable safePageable = PaginationUtils.normalize(pageable, ALLOWED_SORT_PROPERTIES, Map.of());
        Page<Lesson> page = lessonRepository.findByCourseId(courseId, safePageable);
        log.info("Lessons listed for course {} -> {} results on page {} of {}",
                courseId, page.getNumberOfElements(), page.getNumber(), page.getTotalPages());
        return PageResponse.from(page, LessonMapper::toResponse);
    }

    @Override
    @Transactional
    public LessonResponse updateLesson(Long id, LessonRequest request) {
        Lesson lesson = findLessonOrThrow(id);

        // If the lesson is moving to a different course, make sure that course
        // exists first (no entity mutation yet - just verification).
        if (!lesson.getCourse().getId().equals(request.getCourseId())) {
            findCourseOrThrow(request.getCourseId());
        }

        // Uniqueness is checked inside the (possibly new) course, excluding the
        // lesson being updated so keeping the same order is always allowed. The
        // check must run before mutating the entity: otherwise the auto-flush
        // triggered by the check's query would already violate the
        // (course_id, lesson_order) unique constraint when moving a lesson into
        // an order that is taken.
        if (lessonRepository.existsByCourseIdAndLessonOrderAndIdNot(
                request.getCourseId(), request.getLessonOrder(), id)) {
            log.warn("Lesson update rejected - lesson order {} already exists for course {}",
                    request.getLessonOrder(), request.getCourseId());
            throw new DuplicateResourceException("Lesson order already exists for this course");
        }

        // Only now mutate the entity: apply the (verified) course change, then
        // the remaining fields.
        if (!lesson.getCourse().getId().equals(request.getCourseId())) {
            lesson.setCourse(findCourseOrThrow(request.getCourseId()));
        }
        LessonMapper.updateEntity(lesson, request);
        lessonRepository.flush(); // trigger @PreUpdate so updatedAt is fresh
        log.info("Lesson updated with id {} (course {}, order {})", id,
                lesson.getCourse().getId(), lesson.getLessonOrder());
        return LessonMapper.toResponse(lesson);
    }

    @Override
    @Transactional
    public void deleteLesson(Long id) {
        lessonRepository.delete(findLessonOrThrow(id));
        log.info("Lesson deleted with id {}", id);
    }

    private Lesson findLessonOrThrow(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + id));
    }

    private Course findCourseOrThrow(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
    }
}
