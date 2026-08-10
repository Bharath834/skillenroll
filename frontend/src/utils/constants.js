/**
 * Shared application constants. Mirrors backend enums where applicable
 * (backend/src/main/java/com/skillenroll/enums).
 */

/** Client-side route paths. Centralized so links never drift apart. */
export const ROUTES = {
  home: '/',
  courses: '/courses',
  courseDetails: '/courses/:id',
  login: '/login',
  register: '/register',
  myEnrollments: '/my-enrollments',
  progress: '/progress',
};

/** Helper to build a concrete course detail path: coursePath(3) -> "/courses/3" */
export const coursePath = (id) => `/courses/${id}`;

/** User roles (mirrors backend Role enum). */
export const ROLES = {
  ADMIN: 'ADMIN',
  INSTRUCTOR: 'INSTRUCTOR',
  STUDENT: 'STUDENT',
};

/** Course difficulty levels. */
export const SKILL_LEVELS = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

export const SKILL_LEVEL_LABELS = {
  BEGINNER: 'Beginner',
  INTERMEDIATE: 'Intermediate',
  ADVANCED: 'Advanced',
};

/** Enrollment lifecycle statuses (mirrors backend EnrollmentStatus enum). */
export const ENROLLMENT_STATUSES = ['PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED'];

export const ENROLLMENT_STATUS_LABELS = {
  PENDING: 'Pending',
  ACTIVE: 'Active',
  COMPLETED: 'Completed',
  CANCELLED: 'Cancelled',
};

export default {
  ROUTES,
  coursePath,
  ROLES,
  SKILL_LEVELS,
  SKILL_LEVEL_LABELS,
  ENROLLMENT_STATUSES,
  ENROLLMENT_STATUS_LABELS,
};
