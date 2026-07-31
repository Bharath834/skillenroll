package com.skillenroll.mapper;

import com.skillenroll.dto.EnrollmentRequest;
import com.skillenroll.dto.EnrollmentResponse;
import com.skillenroll.entity.Course;
import com.skillenroll.entity.Enrollment;
import com.skillenroll.entity.User;
import com.skillenroll.enums.EnrollmentStatus;

/**
 * Manual mapping between {@link Enrollment}, {@link EnrollmentRequest}
 * and {@link EnrollmentResponse}.
 */
public final class EnrollmentMapper {

    private EnrollmentMapper() {
    }

    public static Enrollment toEntity(EnrollmentRequest request, User user, Course course) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStatus(request.getStatus() != null
                ? request.getStatus()
                : EnrollmentStatus.PENDING);
        enrollment.setUser(user);
        enrollment.setCourse(course);
        return enrollment;
    }

    public static EnrollmentResponse toResponse(Enrollment enrollment) {
        User user = enrollment.getUser();
        Course course = enrollment.getCourse();
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .userId(user.getId())
                .userName(user.getFirstName() + " " + user.getLastName())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .status(enrollment.getStatus())
                .enrollmentDate(enrollment.getEnrollmentDate())
                .createdAt(enrollment.getCreatedAt())
                .updatedAt(enrollment.getUpdatedAt())
                .build();
    }

    public static void updateStatus(Enrollment enrollment, EnrollmentRequest request) {
        if (request.getStatus() != null) {
            enrollment.setStatus(request.getStatus());
        }
    }
}
