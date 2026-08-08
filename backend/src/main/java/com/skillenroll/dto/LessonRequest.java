package com.skillenroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating or updating a {@link com.skillenroll.entity.Lesson}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for creating or updating a lesson. "
        + "On update, courseId may be changed; the lesson moves to the new course.")
public class LessonRequest {

    @NotNull(message = "Course ID is required")
    @Positive(message = "Course ID must be a positive number")
    @Schema(description = "ID of the course the lesson belongs to", example = "4")
    private Long courseId;

    @NotBlank(message = "Lesson title is required")
    @Size(max = 200, message = "Lesson title must not exceed 200 characters")
    @Schema(description = "Lesson title", example = "Introduction to Python")
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Lesson description (optional)", example = "Python fundamentals")
    private String description;

    @NotNull(message = "Lesson order is required")
    @Min(value = 1, message = "Lesson order must be at least 1")
    @Schema(description = "Order of the lesson within its course, starting at 1. "
            + "Must be unique within the course.", example = "1")
    private Integer lessonOrder;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be greater than zero")
    @Schema(description = "Lesson duration in minutes", example = "45")
    private Integer durationMinutes;
}
