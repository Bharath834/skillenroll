package com.skillenroll.courseservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload for a {@link com.skillenroll.courseservice.entity.Course}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Course details returned by the API")
public class CourseResponse {

    @Schema(description = "Unique course id", example = "1")
    private Long id;

    @Schema(description = "Course title", example = "Spring Boot Masterclass")
    private String title;

    @Schema(description = "Course description", example = "Build production-ready REST APIs with Spring Boot")
    private String description;

    @Schema(description = "Course category", example = "Programming")
    private String category;

    @Schema(description = "Course price", example = "49.99")
    private BigDecimal price;

    @Schema(description = "Course duration in hours", example = "40")
    private Integer duration;

    @Schema(description = "Name of the course instructor", example = "Jane Smith")
    private String instructorName;

    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "Last update timestamp")
    private LocalDateTime updatedAt;
}
