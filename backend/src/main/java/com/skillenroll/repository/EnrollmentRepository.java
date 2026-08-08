package com.skillenroll.repository;

import com.skillenroll.entity.Enrollment;
import com.skillenroll.enums.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Data access for {@link Enrollment}. Database access only - no business logic.
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findByUserId(Long userId);

    List<Enrollment> findByCourseId(Long courseId);

    Page<Enrollment> findByUserId(Long userId, Pageable pageable);

    Page<Enrollment> findByCourseId(Long courseId, Pageable pageable);

    /**
     * Paginated enrollment list with optional filters. Every filter is
     * optional: a {@code null} parameter disables that predicate, so any
     * combination of status / userId / courseId can be supplied and an
     * unfiltered call behaves exactly like a plain {@code findAll}.
     */
    @Query("""
            SELECT e FROM Enrollment e
            WHERE (:status IS NULL OR e.status = :status)
              AND (:userId IS NULL OR e.user.id = :userId)
              AND (:courseId IS NULL OR e.course.id = :courseId)
            """)
    Page<Enrollment> filter(@Param("status") EnrollmentStatus status,
                            @Param("userId") Long userId,
                            @Param("courseId") Long courseId,
                            Pageable pageable);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
}
