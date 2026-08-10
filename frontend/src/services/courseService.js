import apiClient from './apiClient.js';
import { COURSE_ENDPOINTS, ENROLLMENT_ENDPOINTS, LESSON_ENDPOINTS } from './endpoints.js';

/**
 * Course-domain service: catalog, details, lessons, and enrollment.
 *
 * Reuses the centralized apiClient (base URL, Bearer-token injection, error
 * normalization) and the endpoint constants — no duplicate client or
 * hardcoded URLs. The backend wraps every response in
 * { success, message, data, timestamp }, so these helpers unwrap to `data`.
 *
 * NOTE: per the backend SecurityConfig, every /api/courses, /api/lessons and
 * /api/enrollments call requires a valid JWT — callers must be authenticated.
 */
const unwrap = (response) => response.data?.data ?? response.data;

export const courseApi = {
  /** GET /api/courses -> CourseResponse[] (full catalog, no server pagination). */
  getCourses: () => apiClient.get(COURSE_ENDPOINTS.list).then(unwrap),

  /** GET /api/courses/search?title=... -> CourseResponse[] (title contains, case-insensitive). */
  searchCourses: (title) =>
    apiClient.get(COURSE_ENDPOINTS.search, { params: { title } }).then(unwrap),

  /** GET /api/courses/{id} -> CourseResponse. */
  getCourseById: (id) => apiClient.get(COURSE_ENDPOINTS.byId(id)).then(unwrap),

  /**
   * GET /api/lessons/course/{courseId}?size=100 -> PageResponse<LessonResponse>.
   * Requests the backend's page-size cap (100) so the full curriculum is
   * returned in one call, preserving the backend's default lessonOrder,asc sort.
   */
  getLessonsByCourse: (courseId) =>
    apiClient
      .get(LESSON_ENDPOINTS.byCourse(courseId), { params: { size: 100 } })
      .then(unwrap),

  /**
   * GET /api/enrollments?userId=&courseId=&size=1 -> PageResponse<EnrollmentResponse>.
   * Used to pre-check whether the current user is already enrolled in a course.
   */
  getEnrollmentsForUserAndCourse: (userId, courseId) =>
    apiClient
      .get(ENROLLMENT_ENDPOINTS.list, { params: { userId, courseId, size: 1 } })
      .then(unwrap),

  /**
   * GET /api/enrollments/user/{userId}?size=100 -> PageResponse<EnrollmentResponse>.
   * All enrollments for the signed-in learner (default sort createdAt,desc;
   * page-size cap requested so the full list returns in one call).
   */
  getEnrollmentsByUser: (userId) =>
    apiClient
      .get(ENROLLMENT_ENDPOINTS.byUser(userId), { params: { size: 100 } })
      .then(unwrap),

  /**
   * POST /api/enrollments { userId, courseId } -> EnrollmentResponse.
   * Status is optional on create and defaults to PENDING. 409 = already
   * enrolled (surfaced by the apiClient's normalized error handling).
   */
  enroll: (payload) => apiClient.post(ENROLLMENT_ENDPOINTS.create, payload).then(unwrap),
};

export default courseApi;
