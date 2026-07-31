package com.skillenroll.repository;

import com.skillenroll.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data access for {@link Course}. Database access only - no business logic.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTitleContainingIgnoreCase(String title);

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);
}
