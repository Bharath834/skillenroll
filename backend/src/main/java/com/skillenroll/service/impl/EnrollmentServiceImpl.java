package com.skillenroll.service.impl;

import com.skillenroll.dto.EnrollmentRequest;
import com.skillenroll.dto.EnrollmentResponse;
import com.skillenroll.entity.Course;
import com.skillenroll.entity.Enrollment;
import com.skillenroll.entity.User;
import com.skillenroll.enums.EnrollmentStatus;
import com.skillenroll.exception.DuplicateResourceException;
import com.skillenroll.exception.ResourceNotFoundException;
import com.skillenroll.mapper.EnrollmentMapper;
import com.skillenroll.repository.CourseRepository;
import com.skillenroll.repository.EnrollmentRepository;
import com.skillenroll.repository.UserRepository;
import com.skillenroll.service.interfaces.EnrollmentService;
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
 * Implementation of {@link EnrollmentService}. All business logic lives here;
 * the controller layer stays thin.
 */
@Service
@Transactional(readOnly = true)
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentServiceImpl.class);

    /** Sort properties exposed to clients, mapped onto the entity property paths. */
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "status", "enrollmentDate", "createdAt", "updatedAt",
            "user.id", "course.id");

    /** Friendly aliases so clients can sort by the flattened response fields. */
    private static final Map<String, String> SORT_ALIASES = Map.of(
            "userId", "user.id",
            "courseId", "course.id");

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    public EnrollmentServiceImpl(EnrollmentRepository enrollmentRepository,
                                 UserRepository userRepository,
                                 CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    @Transactional
    public EnrollmentResponse createEnrollment(EnrollmentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + request.getCourseId()));

        if (enrollmentRepository.existsByUserIdAndCourseId(request.getUserId(), request.getCourseId())) {
            log.warn("Enrollment rejected - user {} already enrolled in course {}",
                    request.getUserId(), request.getCourseId());
            throw new DuplicateResourceException("User is already enrolled in this course");
        }

        // enrollmentDate defaults to now via @PrePersist on Enrollment.
        Enrollment enrollment = EnrollmentMapper.toEntity(request, user, course);
        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("Enrollment created with id {} (user {}, course {})", saved.getId(), saved.getUser().getId(),
                saved.getCourse().getId());
        return EnrollmentMapper.toResponse(saved);
    }

    @Override
    public EnrollmentResponse getEnrollmentById(Long id) {
        return EnrollmentMapper.toResponse(findEnrollmentOrThrow(id));
    }

    @Override
    public List<EnrollmentResponse> getAllEnrollments() {
        return enrollmentRepository.findAll().stream()
                .map(EnrollmentMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<EnrollmentResponse> getAllEnrollments(Pageable pageable) {
        // Single query path: the unfiltered list is just the filter query with no filters.
        return getAllEnrollments(null, null, null, pageable);
    }

    @Override
    public PageResponse<EnrollmentResponse> getAllEnrollments(EnrollmentStatus status, Long userId, Long courseId,
                                                              Pageable pageable) {
        Pageable safePageable = PaginationUtils.normalize(pageable, ALLOWED_SORT_PROPERTIES, SORT_ALIASES);
        Page<Enrollment> page = enrollmentRepository.filter(status, userId, courseId, safePageable);
        log.info("Enrollments listed (status={}, userId={}, courseId={}) -> {} results on page {} of {}",
                status, userId, courseId, page.getNumberOfElements(), page.getTotalPages());
        return PageResponse.from(page, EnrollmentMapper::toResponse);
    }

    @Override
    public List<EnrollmentResponse> getEnrollmentsByUserId(Long userId) {
        return enrollmentRepository.findByUserId(userId).stream()
                .map(EnrollmentMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<EnrollmentResponse> getEnrollmentsByUserId(Long userId, Pageable pageable) {
        Pageable safePageable = PaginationUtils.normalize(pageable, ALLOWED_SORT_PROPERTIES, SORT_ALIASES);
        return PageResponse.from(enrollmentRepository.findByUserId(userId, safePageable), EnrollmentMapper::toResponse);
    }

    @Override
    public List<EnrollmentResponse> getEnrollmentsByCourseId(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(EnrollmentMapper::toResponse)
                .toList();
    }

    @Override
    public PageResponse<EnrollmentResponse> getEnrollmentsByCourseId(Long courseId, Pageable pageable) {
        Pageable safePageable = PaginationUtils.normalize(pageable, ALLOWED_SORT_PROPERTIES, SORT_ALIASES);
        return PageResponse.from(enrollmentRepository.findByCourseId(courseId, safePageable), EnrollmentMapper::toResponse);
    }

    @Override
    @Transactional
    public EnrollmentResponse updateEnrollmentStatus(Long id, EnrollmentRequest request) {
        Enrollment enrollment = findEnrollmentOrThrow(id);
        EnrollmentMapper.updateStatus(enrollment, request);
        enrollmentRepository.flush(); // trigger @PreUpdate so updatedAt is fresh
        log.info("Enrollment status updated for id {} to {}", id, enrollment.getStatus());
        return EnrollmentMapper.toResponse(enrollment);
    }

    @Override
    @Transactional
    public void deleteEnrollment(Long id) {
        enrollmentRepository.delete(findEnrollmentOrThrow(id));
        log.info("Enrollment deleted with id {}", id);
    }

    private Enrollment findEnrollmentOrThrow(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
    }
}
