package com.skillenroll.courseservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Request payload for creating or updating a {@link com.skillenroll.courseservice.entity.Course}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for creating or updating a course. All fields except description are required.")
public class CourseRequest {

    @NotBlank(message = "Course title is required")
    @Size(max = 150, message = "Course title must not exceed 150 characters")
    @Schema(description = "Course title", example = "Spring Boot Masterclass", maxLength = 150)
    private String title;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Course description (optional)", example = "Build production-ready REST APIs with Spring Boot", maxLength = 2000)
    private String description;

    @NotBlank(message = "Course category is required")
    @Size(max = 100, message = "Course category must not exceed 100 characters")
    @Schema(description = "Course category", example = "Programming", maxLength = 100)
    private String category;

    @NotNull(message = "Course price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Course price must be greater than zero")
    @Schema(description = "Course price (must be greater than zero)", example = "49.99")
    private BigDecimal price;

    @NotNull(message = "Course duration is required")
    @Min(value = 1, message = "Course duration must be at least 1 hour")
    @Schema(description = "Course duration in hours (minimum 1)", example = "40")
    private Integer duration;

    @NotBlank(message = "Instructor name is required")
    @Size(max = 100, message = "Instructor name must not exceed 100 characters")
    @Schema(description = "Name of the course instructor", example = "Jane Smith", maxLength = 100)
    private String instructorName;
}
