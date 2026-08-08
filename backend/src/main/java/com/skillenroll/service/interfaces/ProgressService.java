package com.skillenroll.service.interfaces;

import com.skillenroll.dto.ProgressRequest;
import com.skillenroll.dto.ProgressResponse;

import java.util.List;

/**
 * Business operations for {@link com.skillenroll.entity.Progress}.
 */
public interface ProgressService {

    ProgressResponse createProgress(ProgressRequest request);

    ProgressResponse getProgressById(Long id);

    ProgressResponse getProgressByUserAndCourse(Long userId, Long courseId);

    List<ProgressResponse> getProgressByUserId(Long userId);

    List<ProgressResponse> getProgressByCourseId(Long courseId);

    ProgressResponse updateProgress(Long id, ProgressRequest request);

    void deleteProgress(Long id);
}
