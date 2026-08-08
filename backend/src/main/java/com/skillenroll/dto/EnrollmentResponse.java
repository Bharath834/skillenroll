package com.skillenroll.dto;

import com.skillenroll.enums.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Enrollment details returned by the API")
public class EnrollmentResponse {

    @Schema(description = "Unique enrollment id", example = "5")
    private Long id;

    @Schema(description = "ID of the enrolled user", example = "1")
    private Long userId;

    @Schema(description = "Full name of the enrolled user", example = "Bharath Kumar")
    private String userName;

    @Schema(description = "ID of the course", example = "2")
    private Long courseId;

    @Schema(description = "Title of the course", example = "Spring Boot Masterclass")
    private String courseTitle;

    @Schema(description = "Enrollment status", example = "PENDING")
    private EnrollmentStatus status;

    @Schema(description = "Date the learner enrolled")
    private LocalDateTime enrollmentDate;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
