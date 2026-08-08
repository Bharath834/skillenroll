package com.skillenroll.mapper;

import com.skillenroll.dto.ProgressRequest;
import com.skillenroll.dto.ProgressResponse;
import com.skillenroll.entity.Course;
import com.skillenroll.entity.Progress;
import com.skillenroll.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Manual mapping between {@link Progress}, {@link ProgressRequest}
 * and {@link ProgressResponse}.
 */
public final class ProgressMapper {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private ProgressMapper() {
    }

    public static Progress toEntity(ProgressRequest request, User user, Course course) {
        Progress progress = new Progress();
        progress.setUser(user);
        progress.setCourse(course);
        progress.setCompleted(false);
        progress.setCompletedAt(null);
        // startedAt defaults to now via @PrePersist on Progress.
        applyPercentage(progress, request.getProgressPercentage());
        return progress;
    }

    public static ProgressResponse toResponse(Progress progress) {
        User user = progress.getUser();
        Course course = progress.getCourse();
        return ProgressResponse.builder()
                .id(progress.getId())
                .userId(user.getId())
                .userName(user.getFirstName() + " " + user.getLastName())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .progressPercentage(progress.getProgressPercentage())
                .completed(progress.isCompleted())
                .startedAt(progress.getStartedAt())
                .completedAt(progress.getCompletedAt())
                .createdAt(progress.getCreatedAt())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }

    /**
     * Applies an update. The record is pinned to its user and course, so only
     * {@code progressPercentage} is applied; the completion state is derived
     * from it (100% -> completed with a completedAt timestamp).
     */
    public static void updateEntity(Progress progress, ProgressRequest request) {
        applyPercentage(progress, request.getProgressPercentage());
    }

    /**
     * Sets the percentage and keeps {@code completed}/{@code completedAt} in
     * sync: exactly 100% marks the course completed with a fresh timestamp,
     * anything below keeps (or resets) it to not-completed with no timestamp.
     */
    private static void applyPercentage(Progress progress, BigDecimal percentage) {
        progress.setProgressPercentage(percentage);
        if (percentage != null && percentage.compareTo(HUNDRED) >= 0) {
            progress.setCompleted(true);
            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(LocalDateTime.now());
            }
        } else {
            progress.setCompleted(false);
            progress.setCompletedAt(null);
        }
    }
}
