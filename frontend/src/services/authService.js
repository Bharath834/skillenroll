import apiClient from './apiClient.js';
import { AUTH_ENDPOINTS } from './endpoints.js';

/**
 * Thin auth-domain service. Reuses the centralized apiClient (base URL,
 * Bearer-token injection, error normalization) and the endpoint constants —
 * no duplicate client or hardcoded URLs.
 *
 * The backend wraps every response in { success, message, data, timestamp },
 * so these helpers unwrap to the payload (JwtResponse / UserResponse).
 */
const unwrap = (response) => response.data?.data ?? response.data;

export const authApi = {
  /** POST /api/auth/login -> JwtResponse */
  login: (credentials) => apiClient.post(AUTH_ENDPOINTS.login, credentials).then(unwrap),

  /** POST /api/auth/register -> JwtResponse (registration auto-authenticates) */
  register: (payload) => apiClient.post(AUTH_ENDPOINTS.register, payload).then(unwrap),

  /**
   * POST /api/auth/logout — revokes the refresh token (requires a valid access
   * token). `accessToken` is passed explicitly so logout still works after the
   * local session has been cleared (the request interceptor reads storage, which
   * by then is empty).
   */
  logout: (refreshToken, accessToken) =>
    apiClient.post(
      AUTH_ENDPOINTS.logout,
      { refreshToken },
      accessToken ? { headers: { Authorization: `Bearer ${accessToken}` } } : undefined
    ),
};

export default authApi;
