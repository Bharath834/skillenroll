package com.skillenroll.dto;

import com.skillenroll.enums.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Payload for creating or updating an enrollment. "
        + "On create, status is optional and defaults to PENDING; on update, only status is applied.")
public class EnrollmentRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be a positive number")
    @Schema(description = "ID of the enrolling user", example = "1")
    private Long userId;

    @NotNull(message = "Course ID is required")
    @Positive(message = "Course ID must be a positive number")
    @Schema(description = "ID of the course being enrolled in", example = "2")
    private Long courseId;

    @Schema(description = "Enrollment status (optional on create, defaults to PENDING). "
            + "Allowed values: PENDING, ACTIVE, COMPLETED, CANCELLED.",
            example = "PENDING")
    private EnrollmentStatus status;
}
