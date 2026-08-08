package com.skillenroll.mapper;

import com.skillenroll.dto.LessonRequest;
import com.skillenroll.dto.LessonResponse;
import com.skillenroll.entity.Course;
import com.skillenroll.entity.Lesson;

/**
 * Manual mapping between {@link Lesson}, {@link LessonRequest} and
 * {@link LessonResponse}.
 */
public final class LessonMapper {

    private LessonMapper() {
    }

    public static Lesson toEntity(LessonRequest request, Course course) {
        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setTitle(request.getTitle());
        lesson.setDescription(request.getDescription());
        lesson.setLessonOrder(request.getLessonOrder());
        lesson.setDurationMinutes(request.getDurationMinutes());
        return lesson;
    }

    public static LessonResponse toResponse(Lesson lesson) {
        Course course = lesson.getCourse();
        return LessonResponse.builder()
                .id(lesson.getId())
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .title(lesson.getTitle())
                .description(lesson.getDescription())
                .lessonOrder(lesson.getLessonOrder())
                .durationMinutes(lesson.getDurationMinutes())
                .createdAt(lesson.getCreatedAt())
                .updatedAt(lesson.getUpdatedAt())
                .build();
    }

    public static void updateEntity(Lesson lesson, LessonRequest request) {
        lesson.setTitle(request.getTitle());
        lesson.setDescription(request.getDescription());
        lesson.setLessonOrder(request.getLessonOrder());
        lesson.setDurationMinutes(request.getDurationMinutes());
    }
}
