package com.skillenroll.repository;

import com.skillenroll.entity.Lesson;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for {@link Lesson}. Database access only - no business logic.
 */
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByCourseId(Long courseId);

    Page<Lesson> findByCourseId(Long courseId, Pageable pageable);

    boolean existsByCourseIdAndLessonOrder(Long courseId, Integer lessonOrder);

    boolean existsByCourseIdAndLessonOrderAndIdNot(Long courseId, Integer lessonOrder, Long id);
}
