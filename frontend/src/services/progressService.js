import apiClient from './apiClient.js';
import { PROGRESS_ENDPOINTS } from './endpoints.js';

/**
 * Progress-domain service: course progress records for the signed-in learner.
 *
 * Reuses the centralized apiClient (base URL, Bearer-token injection, error
 * normalization) and the endpoint constants — no duplicate client or
 * hardcoded URLs. The backend wraps every response in
 * { success, message, data, timestamp }, so these helpers unwrap to `data`.
 *
 * NOTE: per the backend SecurityConfig, every /api/progress call requires a
 * valid JWT — callers must be authenticated.
 */
const unwrap = (response) => response.data?.data ?? response.data;

export const progressApi = {
  /**
   * GET /api/progress/user/{userId} -> ProgressResponse[].
   * Every progress record for the learner (ProgressResponse: id, userId,
   * userName, courseId, courseTitle, progressPercentage, completed,
   * startedAt, completedAt, createdAt, updatedAt).
   */
  getByUser: (userId) => apiClient.get(PROGRESS_ENDPOINTS.byUser(userId)).then(unwrap),
};

export default progressApi;
