package com.skillenroll.dto;

import com.skillenroll.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating or updating an
 * {@link com.skillenroll.entity.Enrollment}.
 *
 * <p>On create, {@code status} is optional and defaults to PENDING.
 * On update, {@code status} is the field that changes.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnrollmentRequest {

    @NotNull(message = "User id is required")
    @Positive(message = "User id must be positive")
    private Long userId;

    @NotNull(message = "Course id is required")
    @Positive(message = "Course id must be positive")
    private Long courseId;

    private EnrollmentStatus status;
}
