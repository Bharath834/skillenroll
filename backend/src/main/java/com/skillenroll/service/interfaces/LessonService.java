package com.skillenroll.service.interfaces;

import com.skillenroll.dto.LessonRequest;
import com.skillenroll.dto.LessonResponse;
import com.skillenroll.util.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Business operations for {@link com.skillenroll.entity.Lesson}.
 */
public interface LessonService {

    LessonResponse createLesson(LessonRequest request);

    LessonResponse getLessonById(Long id);

    List<LessonResponse> getAllLessons();

    /**
     * Paginated lessons for a course with client-controlled sorting.
     * Sort properties are whitelisted (id, title, lessonOrder,
     * durationMinutes, createdAt, updatedAt).
     */
    PageResponse<LessonResponse> getLessonsByCourseId(Long courseId, Pageable pageable);

    LessonResponse updateLesson(Long id, LessonRequest request);

    void deleteLesson(Long id);
}
