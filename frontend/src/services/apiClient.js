import axios from 'axios';
import { API_BASE_URL, AUTH_EVENTS, STORAGE_KEYS } from '../config.js';

/**
 * Centralized HTTP client for the SkillEnroll backend / API gateway.
 *
 * Every API call in the app must go through this instance — components and
 * services never construct raw axios calls with hardcoded URLs.
 *
 * Behavior:
 * - baseURL + timeout configured once from src/config.js.
 * - A request interceptor attaches `Authorization: Bearer <accessToken>`
 *   automatically when a token is present in localStorage (no-op for public
 *   endpoints, which simply ignore the header).
 * - A response interceptor normalizes errors so callers always receive a
 *   friendly `error.message` plus optional `error.details` (validation map).
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Public auth endpoints use their own credentials, so we never attach a
// possibly-stale stored token to them (a stale token could otherwise cause
// spurious 401s on login/register), and a 401 there must not trigger the
// session-expiry handling. Logout is NOT in this list: it needs the access
// token to blacklist it (passed explicitly by authService after clearing).
const AUTH_ENDPOINT_FRAGMENTS = ['/auth/login', '/auth/register', '/auth/refresh'];

// Attach the JWT access token to every request when available.
apiClient.interceptors.request.use(
  (config) => {
    if (typeof window === 'undefined') return config;
    const url = config.url || '';
    if (AUTH_ENDPOINT_FRAGMENTS.some((fragment) => url.includes(fragment))) {
      return config;
    }
    const token = window.localStorage.getItem(STORAGE_KEYS.accessToken);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Normalize errors: extract the backend message + validation details.
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    const body = error.response?.data;

    const message =
      body?.message ||
      error.message ||
      (status ? `Request failed with status ${status}` : 'Network error — is the backend running?');

    const normalized = new Error(message);
    normalized.status = status;
    // Field-level validation info: the backend reports it as `data` for 400s
    // ({ field: message } map); the documented `details` shape is supported too.
    normalized.details =
      body?.details || (typeof body?.data === 'object' && body.data !== null ? body.data : null);

    // Session expired / token revoked: if an authenticated request gets a 401,
    // clear persisted tokens and notify the app so protected UI disappears
    // immediately (AuthContext listens for 'auth:unauthorized').
    const requestedUrl = error.config?.url || '';
    const sentToken = Boolean(error.config?.headers?.Authorization);
    const isAuthEndpoint = AUTH_ENDPOINT_FRAGMENTS.some((fragment) =>
      requestedUrl.includes(fragment)
    );

    if (status === 401 && sentToken && !isAuthEndpoint && typeof window !== 'undefined') {
      try {
        window.localStorage.removeItem(STORAGE_KEYS.accessToken);
        window.localStorage.removeItem(STORAGE_KEYS.refreshToken);
        window.localStorage.removeItem(STORAGE_KEYS.user);
        window.dispatchEvent(new Event(AUTH_EVENTS.unauthorized));
      } catch {
        // Storage unavailable — ignore; the request error still propagates.
      }
    }

    return Promise.reject(normalized);
  }
);

export default apiClient;
