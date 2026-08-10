import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { AUTH_EVENTS, STORAGE_KEYS } from '../config.js';
import { authApi } from '../services/authService.js';

const AuthContext = createContext(null);

/**
 * Read persisted auth state (JWT + user) from localStorage.
 * Safe in non-browser environments (SSR smoke tests, prerendering).
 */
function readStoredAuth() {
  if (typeof window === 'undefined') return { token: null, user: null };
  try {
    const token = window.localStorage.getItem(STORAGE_KEYS.accessToken);
    const rawUser = window.localStorage.getItem(STORAGE_KEYS.user);
    return { token, user: rawUser ? JSON.parse(rawUser) : null };
  } catch {
    return { token: null, user: null };
  }
}

/** Persist (or clear) the auth triplet in localStorage. */
function persistAuth({ token, refreshToken, user }) {
  if (typeof window === 'undefined') return;
  try {
    if (token) window.localStorage.setItem(STORAGE_KEYS.accessToken, token);
    else window.localStorage.removeItem(STORAGE_KEYS.accessToken);

    if (refreshToken) window.localStorage.setItem(STORAGE_KEYS.refreshToken, refreshToken);
    else window.localStorage.removeItem(STORAGE_KEYS.refreshToken);

    if (user) window.localStorage.setItem(STORAGE_KEYS.user, JSON.stringify(user));
    else window.localStorage.removeItem(STORAGE_KEYS.user);
  } catch {
    // Storage may be unavailable (private mode) — the in-memory state still works.
  }
}

/**
 * Single source of truth for authentication state.
 *
 * Provides:
 * - current user + JWT token (restored from localStorage on first render)
 * - login(email, password)   → POST /api/auth/login
 * - register(payload)        → POST /api/auth/register (backend returns tokens,
 *                              so registration is an automatic login)
 * - logout()                 → best-effort POST /api/auth/logout, always clears
 *                              local state
 * - isAuthenticated
 *
 * The API client dispatches an `auth:unauthorized` window event when a request
 * with a token receives 401 (expired/revoked session); the provider listens and
 * clears the session so protected UI cannot remain visible.
 */
export function AuthProvider({ children }) {
  const [auth, setAuth] = useState(readStoredAuth);

  const applyAuth = useCallback((jwt) => {
    persistAuth({ token: jwt.token, refreshToken: jwt.refreshToken, user: jwt.user });
    setAuth({ token: jwt.token, user: jwt.user });
    return jwt.user;
  }, []);

  const clearAuth = useCallback(() => {
    persistAuth({ token: null, refreshToken: null, user: null });
    setAuth({ token: null, user: null });
  }, []);

  const login = useCallback(
    async (email, password) => {
      const jwt = await authApi.login({ email, password });
      return applyAuth(jwt);
    },
    [applyAuth]
  );

  const register = useCallback(
    async (payload) => {
      const jwt = await authApi.register(payload);
      return applyAuth(jwt);
    },
    [applyAuth]
  );

  const logout = useCallback(async () => {
    // Capture tokens first, then clear the local session IMMEDIATELY so
    // protected UI disappears right away (never blocked on the network).
    const refreshToken =
      typeof window !== 'undefined'
        ? window.localStorage.getItem(STORAGE_KEYS.refreshToken)
        : null;
    const accessToken =
      typeof window !== 'undefined'
        ? window.localStorage.getItem(STORAGE_KEYS.accessToken)
        : null;

    clearAuth();

    if (!refreshToken || typeof window === 'undefined') return;
    try {
      // Best-effort server-side revocation, using the captured access token
      // (storage is already cleared, so the interceptor cannot attach it).
      await authApi.logout(refreshToken, accessToken);
    } catch {
      // Ignore — the local session is already cleared.
    }
  }, [clearAuth]);

  // Session expired / token revoked — drop the local session immediately.
  useEffect(() => {
    const onUnauthorized = () => clearAuth();
    if (typeof window === 'undefined') return undefined;
    window.addEventListener(AUTH_EVENTS.unauthorized, onUnauthorized);
    return () => window.removeEventListener(AUTH_EVENTS.unauthorized, onUnauthorized);
  }, [clearAuth]);

  const value = useMemo(
    () => ({
      ...auth,
      isAuthenticated: Boolean(auth.token && auth.user),
      login,
      register,
      logout,
    }),
    [auth, login, register, logout]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/** Access the auth context; throws if used outside <AuthProvider>. */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export default AuthContext;
