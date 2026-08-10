/**
 * Centralized application configuration.
 *
 * This is the single source of truth for environment-dependent values
 * (backend API base URL, etc.). Components and services MUST import from
 * here instead of hardcoding URLs.
 */

const env = import.meta.env || {};

/**
 * Base URL of the SkillEnroll API.
 * - Defaults to the local Spring Boot backend (which serves every /api/* endpoint).
 * - Override via VITE_API_BASE_URL in a frontend/.env file (see .env.example).
 * - The API gateway (http://localhost:8081) is an alternative for the routes it
 *   currently proxies (auth, users, courses).
 */
export const API_BASE_URL = env.VITE_API_BASE_URL || 'http://localhost:8080/api';

/** Storage keys used across the app (auth tokens, etc.). */
export const STORAGE_KEYS = {
  accessToken: 'skillenroll.accessToken',
  refreshToken: 'skillenroll.refreshToken',
  user: 'skillenroll.user',
};

/** Window events the app listens for (dispatched by the API client). */
export const AUTH_EVENTS = {
  unauthorized: 'auth:unauthorized',
};

/** Default pagination used by list views. */
export const DEFAULT_PAGE_SIZE = 20;

export default { API_BASE_URL, STORAGE_KEYS, AUTH_EVENTS, DEFAULT_PAGE_SIZE };
