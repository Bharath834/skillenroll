package com.skillenroll.courseservice.repository;

import com.skillenroll.courseservice.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Data access for {@link Course}. Database access only - no business logic.
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByTitleContainingIgnoreCase(String title);

    /**
     * Paginated search across title, category and instructor name. Every
     * filter is optional: an empty string disables that predicate, so any
     * combination of filters can be supplied. Matching is case-insensitive
     * and substring-based (MySQL {@code LIKE %...%}); {@code \} is the escape
     * character so {@code %}/{\@code _} in the input match literally.
     */
    @Query("""
            SELECT c FROM Course c
            WHERE (:title = '' OR LOWER(c.title) LIKE LOWER(CONCAT('%', :title, '%')) ESCAPE '\\')
              AND (:category = '' OR LOWER(c.category) LIKE LOWER(CONCAT('%', :category, '%')) ESCAPE '\\')
              AND (:instructor = '' OR LOWER(c.instructorName) LIKE LOWER(CONCAT('%', :instructor, '%')) ESCAPE '\\')
            """)
    Page<Course> search(@Param("title") String title,
                        @Param("category") String category,
                        @Param("instructor") String instructor,
                        Pageable pageable);

    boolean existsByTitle(String title);

    boolean existsByTitleAndIdNot(String title, Long id);
}
