package com.skillenroll.dto;

import com.skillenroll.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response payload for an {@link com.skillenroll.entity.Enrollment}.
 * Includes flattened user and course summaries for client convenience.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long courseId;
    private String courseTitle;
    private EnrollmentStatus status;
    private LocalDateTime enrollmentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
