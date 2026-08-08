package com.skillenroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request payload for creating or updating a
 * {@link com.skillenroll.entity.Progress} record.
 *
 * <p>On update, the record is pinned to its user and course, so only
 * {@code progressPercentage} is applied (userId/courseId are validated but
 * ignored), mirroring the enrollment update behavior.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for creating or updating course progress. "
        + "progressPercentage must be between 0 and 100.")
public class ProgressRequest {

    @NotNull(message = "User ID is required")
    @Positive(message = "User ID must be a positive number")
    @Schema(description = "ID of the user whose progress is tracked", example = "1")
    private Long userId;

    @NotNull(message = "Course ID is required")
    @Positive(message = "Course ID must be a positive number")
    @Schema(description = "ID of the course", example = "2")
    private Long courseId;

    @NotNull(message = "Progress percentage is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Progress percentage must be at least 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "Progress percentage must not exceed 100")
    @Schema(description = "Progress percentage, between 0 and 100. Setting it to 100 marks the "
            + "course completed.", example = "45.50")
    private BigDecimal progressPercentage;
}
