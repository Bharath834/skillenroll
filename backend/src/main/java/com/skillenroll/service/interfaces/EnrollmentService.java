package com.skillenroll.service.interfaces;

import com.skillenroll.dto.EnrollmentRequest;
import com.skillenroll.dto.EnrollmentResponse;
import com.skillenroll.enums.EnrollmentStatus;
import com.skillenroll.util.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Business operations for {@link com.skillenroll.entity.Enrollment}.
 */
public interface EnrollmentService {

    EnrollmentResponse createEnrollment(EnrollmentRequest request);

    EnrollmentResponse getEnrollmentById(Long id);

    /**
     * Legacy list of every enrollment. Preserved for service-layer backward
     * compatibility; the controller uses the paginated
     * {@link #getAllEnrollments(Pageable)} instead.
     */
    List<EnrollmentResponse> getAllEnrollments();

    /**
     * Paginated list of all enrollments with client-controlled sorting.
     * Equivalent to {@link #getAllEnrollments(EnrollmentStatus, Long, Long, Pageable)}
     * with every filter disabled.
     */
    PageResponse<EnrollmentResponse> getAllEnrollments(Pageable pageable);

    /**
     * Paginated enrollment list with optional filters. Every filter is
     * optional - a {@code null} value disables that predicate, so an
     * unfiltered call behaves exactly like {@link #getAllEnrollments(Pageable)}.
     *
     * @param status   exact status filter, or {@code null} for any status
     * @param userId   exact user filter, or {@code null} for any user
     * @param courseId exact course filter, or {@code null} for any course
     * @param pageable paging and (whitelisted) sorting controls
     */
    PageResponse<EnrollmentResponse> getAllEnrollments(EnrollmentStatus status, Long userId, Long courseId,
                                                       Pageable pageable);

    /**
     * Legacy unfiltered list. Preserved for service-layer backward
     * compatibility; the controller uses the paginated
     * {@link #getEnrollmentsByUserId(Long, Pageable)} instead.
     */
    List<EnrollmentResponse> getEnrollmentsByUserId(Long userId);

    /**
     * Paginated enrollments for a user with client-controlled sorting.
     */
    PageResponse<EnrollmentResponse> getEnrollmentsByUserId(Long userId, Pageable pageable);

    /**
     * Legacy unfiltered list. Preserved for service-layer backward
     * compatibility; the controller uses the paginated
     * {@link #getEnrollmentsByCourseId(Long, Pageable)} instead.
     */
    List<EnrollmentResponse> getEnrollmentsByCourseId(Long courseId);

    /**
     * Paginated enrollments for a course with client-controlled sorting.
     */
    PageResponse<EnrollmentResponse> getEnrollmentsByCourseId(Long courseId, Pageable pageable);

    EnrollmentResponse updateEnrollmentStatus(Long id, EnrollmentRequest request);

    void deleteEnrollment(Long id);
}
