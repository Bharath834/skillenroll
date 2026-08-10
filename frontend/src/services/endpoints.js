/**
 * API endpoint paths, grouped by domain. These mirror the Spring Boot
 * controllers (backend/src/main/java/com/skillenroll/controller) and are the
 * single place where route strings live. Path params are functions so callers
 * can build concrete URLs: COURSE_ENDPOINTS.byId(3) -> "/courses/3".
 *
 * NOTE: No feature wiring yet — this file is part of the foundation layer and
 * will be consumed by service modules in upcoming phases.
 */

export const AUTH_ENDPOINTS = {
  register: '/auth/register',
  login: '/auth/login',
  refresh: '/auth/refresh',
  logout: '/auth/logout',
};

export const USER_ENDPOINTS = {
  me: '/users/me',
  byId: (id) => `/users/${id}`,
};

export const COURSE_ENDPOINTS = {
  list: '/courses',
  search: '/courses/search',
  byId: (id) => `/courses/${id}`,
};

export const ENROLLMENT_ENDPOINTS = {
  create: '/enrollments',
  list: '/enrollments',
  byId: (id) => `/enrollments/${id}`,
  byUser: (userId) => `/enrollments/user/${userId}`,
  byCourse: (courseId) => `/enrollments/course/${courseId}`,
};

export const LESSON_ENDPOINTS = {
  byId: (id) => `/lessons/${id}`,
  byCourse: (courseId) => `/lessons/course/${courseId}`,
};

export const PROGRESS_ENDPOINTS = {
  create: '/progress',
  byId: (id) => `/progress/${id}`,
  byUser: (userId) => `/progress/user/${userId}`,
  byCourse: (courseId) => `/progress/course/${courseId}`,
  byUserAndCourse: (userId, courseId) => `/progress/user/${userId}/course/${courseId}`,
};

export default {
  AUTH_ENDPOINTS,
  USER_ENDPOINTS,
  COURSE_ENDPOINTS,
  ENROLLMENT_ENDPOINTS,
  LESSON_ENDPOINTS,
  PROGRESS_ENDPOINTS,
};
