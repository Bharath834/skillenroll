package com.skillenroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response payload for a {@link com.skillenroll.entity.Course}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {

    private Long id;
    private String title;
    private String description;
    private String category;
    private BigDecimal price;
    private Integer duration;
    private String instructorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
