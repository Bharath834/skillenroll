package com.skillenroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload for a {@link com.skillenroll.entity.Progress} record.
 * Includes flattened user and course summaries for client convenience.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Course progress details returned by the API")
public class ProgressResponse {

    @Schema(description = "Unique progress id", example = "1")
    private Long id;

    @Schema(description = "ID of the learner", example = "1")
    private Long userId;

    @Schema(description = "Full name of the learner", example = "Bharath Kumar")
    private String userName;

    @Schema(description = "ID of the course", example = "2")
    private Long courseId;

    @Schema(description = "Title of the course", example = "Spring Boot Masterclass")
    private String courseTitle;

    @Schema(description = "Progress percentage (0-100)", example = "45.50")
    private BigDecimal progressPercentage;

    @Schema(description = "Whether the course is fully completed (true only at 100%)", example = "false")
    private boolean completed;

    @Schema(description = "When the learner started the course")
    private LocalDateTime startedAt;

    @Schema(description = "When the course was completed (null until progress reaches 100)")
    private LocalDateTime completedAt;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
