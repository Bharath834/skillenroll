package com.skillenroll.service.impl;

import com.skillenroll.dto.EnrollmentRequest;
import com.skillenroll.dto.EnrollmentResponse;
import com.skillenroll.entity.Course;
import com.skillenroll.entity.Enrollment;
import com.skillenroll.entity.User;
import com.skillenroll.exception.DuplicateResourceException;
import com.skillenroll.exception.ResourceNotFoundException;
import com.skillenroll.mapper.EnrollmentMapper;
import com.skillenroll.repository.CourseRepository;
import com.skillenroll.repository.EnrollmentRepository;
import com.skillenroll.repository.UserRepository;
import com.skillenroll.service.interfaces.EnrollmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link EnrollmentService}. All business logic lives here;
 * the controller layer stays thin.
 */
@Service
@Transactional(readOnly = true)
public class EnrollmentServiceImpl implements EnrollmentService {

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
            throw new DuplicateResourceException("User is already enrolled in this course");
        }

        // enrollmentDate defaults to now via @PrePersist on Enrollment.
        Enrollment enrollment = EnrollmentMapper.toEntity(request, user, course);
        return EnrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
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
    public List<EnrollmentResponse> getEnrollmentsByUserId(Long userId) {
        return enrollmentRepository.findByUserId(userId).stream()
                .map(EnrollmentMapper::toResponse)
                .toList();
    }

    @Override
    public List<EnrollmentResponse> getEnrollmentsByCourseId(Long courseId) {
        return enrollmentRepository.findByCourseId(courseId).stream()
                .map(EnrollmentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public EnrollmentResponse updateEnrollmentStatus(Long id, EnrollmentRequest request) {
        Enrollment enrollment = findEnrollmentOrThrow(id);
        EnrollmentMapper.updateStatus(enrollment, request);
        enrollmentRepository.flush(); // trigger @PreUpdate so updatedAt is fresh
        return EnrollmentMapper.toResponse(enrollment);
    }

    @Override
    @Transactional
    public void deleteEnrollment(Long id) {
        enrollmentRepository.delete(findEnrollmentOrThrow(id));
    }

    private Enrollment findEnrollmentOrThrow(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + id));
    }
}
