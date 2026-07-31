package com.skillenroll.service.interfaces;

import com.skillenroll.dto.EnrollmentRequest;
import com.skillenroll.dto.EnrollmentResponse;

import java.util.List;

/**
 * Business operations for {@link com.skillenroll.entity.Enrollment}.
 */
public interface EnrollmentService {

    EnrollmentResponse createEnrollment(EnrollmentRequest request);

    EnrollmentResponse getEnrollmentById(Long id);

    List<EnrollmentResponse> getAllEnrollments();

    List<EnrollmentResponse> getEnrollmentsByUserId(Long userId);

    List<EnrollmentResponse> getEnrollmentsByCourseId(Long courseId);

    EnrollmentResponse updateEnrollmentStatus(Long id, EnrollmentRequest request);

    void deleteEnrollment(Long id);
}
