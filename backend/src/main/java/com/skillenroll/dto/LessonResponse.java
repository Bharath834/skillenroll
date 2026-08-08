package com.skillenroll.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response payload for a {@link com.skillenroll.entity.Lesson}.
 * Includes a flattened course summary (id + title) for client convenience;
 * the entity's {@code course} object itself is never exposed.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Lesson details returned by the API")
public class LessonResponse {

    @Schema(description = "Unique lesson id", example = "1")
    private Long id;

    @Schema(description = "ID of the course the lesson belongs to", example = "4")
    private Long courseId;

    @Schema(description = "Title of the course the lesson belongs to", example = "Python Full Stack")
    private String courseTitle;

    @Schema(description = "Lesson title", example = "Introduction to Python")
    private String title;

    @Schema(description = "Lesson description", example = "Python fundamentals")
    private String description;

    @Schema(description = "Order of the lesson within its course, starting at 1", example = "1")
    private Integer lessonOrder;

    @Schema(description = "Lesson duration in minutes", example = "45")
    private Integer durationMinutes;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
