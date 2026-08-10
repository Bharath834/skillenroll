/**
 * End-to-end test of the FRONTEND auth service code against the live backend.
 * Loads the actual src/services/authService.js + apiClient.js through Vite's
 * SSR runner and performs register -> login -> logout -> 401-after-logout.
 * Run from the frontend directory:  node .auth-e2e.mjs   (backend must be on :8080)
 */
import { createServer } from 'vite';
import { dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = dirname(fileURLToPath(import.meta.url));

const server = await createServer({
  root,
  server: { middlewareMode: true },
  appType: 'custom',
  logLevel: 'error',
});

const { authApi } = await server.ssrLoadModule('/src/services/authService.js');
const { default: apiClient } = await server.ssrLoadModule('/src/services/apiClient.js');

const stamp = Date.now();
const email = `frontend.code.${stamp}@example.com`;
const payload = {
  firstName: 'Frontend',
  lastName: 'CodeTest',
  email,
  phoneNumber: `+1555${String(stamp).slice(-6)}`, // unique 10-digit number per run
  password: 'Phase2Pass123',
};

let ok = true;
const check = (name, cond, detail = '') => {
  if (!cond) ok = false;
  console.log(`${cond ? 'PASS' : 'FAIL'}  ${name}  ${detail}`);
};

try {
  // 1. Register through the exact frontend service (auto-login JwtResponse).
  const jwt = await authApi.register(payload);
  check(
    'authApi.register returns JwtResponse',
    Boolean(jwt?.token && jwt?.refreshToken && jwt?.user?.role === 'STUDENT'),
    `role=${jwt?.user?.role} token=${jwt?.token ? jwt.token.slice(0, 12) + '…' : 'none'}`
  );

  // 2. Login through the exact frontend service.
  const loginJwt = await authApi.login({ email, password: 'Phase2Pass123' });
  check('authApi.login returns JwtResponse', Boolean(loginJwt?.token), `user=${loginJwt?.user?.email}`);

  // 3. Logout through the exact frontend service (captured-token flow used by
  //    AuthContext after clearing local state).
  const logoutRes = await authApi.logout(loginJwt.refreshToken, loginJwt.token);
  check('authApi.logout -> 200', logoutRes?.status === 200, `status=${logoutRes?.status}`);

  // 4. The blacklisted access token must now be rejected with 401.
  let rejected = false;
  try {
    await apiClient.get('/users/me', {
      headers: { Authorization: `Bearer ${loginJwt.token}` },
    });
  } catch (error) {
    rejected = error.status === 401;
  }
  check('blacklisted token rejected with 401', rejected, '');

  // 5. Wrong credentials through the frontend service -> normalized 401 error.
  let badLogin = false;
  try {
    await authApi.login({ email, password: 'WrongPass123' });
  } catch (error) {
    badLogin = error.status === 401 && typeof error.message === 'string';
  }
  check('wrong password -> normalized 401 error', badLogin, '');
} catch (error) {
  ok = false;
  console.log(`ERROR: ${error.message}`);
}

await server.close();
console.log(ok ? '\nFRONTEND AUTH CODE E2E PASSED' : '\nFRONTEND AUTH CODE E2E FAILED');
process.exit(ok ? 0 : 1);
