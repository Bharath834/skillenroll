package com.skillenroll.repository;

import com.skillenroll.entity.Progress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Data access for {@link Progress}. Database access only - no business logic.
 */
public interface ProgressRepository extends JpaRepository<Progress, Long> {

    Optional<Progress> findByUserIdAndCourseId(Long userId, Long courseId);

    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    List<Progress> findByUserId(Long userId);

    List<Progress> findByCourseId(Long courseId);
}
