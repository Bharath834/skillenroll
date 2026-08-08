package com.skillenroll.service.impl;

import com.skillenroll.dto.ProgressRequest;
import com.skillenroll.dto.ProgressResponse;
import com.skillenroll.entity.Course;
import com.skillenroll.entity.Progress;
import com.skillenroll.entity.User;
import com.skillenroll.exception.DuplicateResourceException;
import com.skillenroll.exception.ResourceNotFoundException;
import com.skillenroll.mapper.ProgressMapper;
import com.skillenroll.repository.CourseRepository;
import com.skillenroll.repository.EnrollmentRepository;
import com.skillenroll.repository.ProgressRepository;
import com.skillenroll.repository.UserRepository;
import com.skillenroll.service.interfaces.ProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link ProgressService}. All business logic lives here;
 * the controller layer stays thin.
 */
@Service
@Transactional(readOnly = true)
public class ProgressServiceImpl implements ProgressService {

    private static final Logger log = LoggerFactory.getLogger(ProgressServiceImpl.class);

    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public ProgressServiceImpl(ProgressRepository progressRepository,
                               UserRepository userRepository,
                               CourseRepository courseRepository,
                               EnrollmentRepository enrollmentRepository) {
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    @Transactional
    public ProgressResponse createProgress(ProgressRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));

        if (!enrollmentRepository.existsByUserIdAndCourseId(request.getUserId(), request.getCourseId())) {
            log.warn("Progress rejected - user {} is not enrolled in course {}",
                    request.getUserId(), request.getCourseId());
            throw new ResourceNotFoundException("User is not enrolled in this course");
        }

        if (progressRepository.existsByUserIdAndCourseId(request.getUserId(), request.getCourseId())) {
            log.warn("Progress rejected - user {} already has progress for course {}",
                    request.getUserId(), request.getCourseId());
            throw new DuplicateResourceException("Progress already exists for this user and course");
        }

        // startedAt defaults to now via @PrePersist on Progress.
        Progress progress = ProgressMapper.toEntity(request, user, course);
        Progress saved = progressRepository.save(progress);
        log.info("Progress created with id {} (user {}, course {}, {}%)", saved.getId(), saved.getUser().getId(),
                saved.getCourse().getId(), saved.getProgressPercentage());
        return ProgressMapper.toResponse(saved);
    }

    @Override
    public ProgressResponse getProgressById(Long id) {
        return ProgressMapper.toResponse(findProgressOrThrow(id));
    }

    @Override
    public ProgressResponse getProgressByUserAndCourse(Long userId, Long courseId) {
        return ProgressMapper.toResponse(progressRepository.findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Progress not found for user " + userId + " and course " + courseId)));
    }

    @Override
    public List<ProgressResponse> getProgressByUserId(Long userId) {
        return progressRepository.findByUserId(userId).stream()
                .map(ProgressMapper::toResponse)
                .toList();
    }

    @Override
    public List<ProgressResponse> getProgressByCourseId(Long courseId) {
        return progressRepository.findByCourseId(courseId).stream()
                .map(ProgressMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProgressResponse updateProgress(Long id, ProgressRequest request) {
        Progress progress = findProgressOrThrow(id);
        // Only progressPercentage is applied; the record stays pinned to its user and course.
        ProgressMapper.updateEntity(progress, request);
        progressRepository.flush(); // trigger @PreUpdate so updatedAt is fresh
        log.info("Progress updated for id {} to {}% (completed={})",
                id, progress.getProgressPercentage(), progress.isCompleted());
        return ProgressMapper.toResponse(progress);
    }

    @Override
    @Transactional
    public void deleteProgress(Long id) {
        progressRepository.delete(findProgressOrThrow(id));
        log.info("Progress deleted with id {}", id);
    }

    private Progress findProgressOrThrow(Long id) {
        return progressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Progress not found with id: " + id));
    }
}
