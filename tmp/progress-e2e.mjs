/**
 * End-to-end test of the FRONTEND progress service code against the live
 * backend. Loads the actual src/services/progressService.js, authService.js
 * and apiClient.js through Vite's SSR runner (with a localStorage shim so the
 * real Bearer-token interceptor runs, exactly as in the browser).
 *
 * Covers: anonymous 401 -> register -> empty progress -> create progress
 * (45.5%) -> getByUser -> update to 100% (completed) -> getByUser reflects it.
 * Run from the frontend directory:  node .progress-e2e.mjs  (backend :8080)
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

const { STORAGE_KEYS } = await server.ssrLoadModule('/src/config.js');
const store = {};
const storage = {
  getItem: (key) => store[key] ?? null,
  setItem: (key, value) => {
    store[key] = String(value);
  },
  removeItem: (key) => {
    delete store[key];
  },
};
globalThis.window = { localStorage: storage, dispatchEvent: () => true };
const setToken = (token) => storage.setItem(STORAGE_KEYS.accessToken, token);

const { authApi } = await server.ssrLoadModule('/src/services/authService.js');
const { progressApi } = await server.ssrLoadModule('/src/services/progressService.js');
const { default: apiClient } = await server.ssrLoadModule('/src/services/apiClient.js');

const stamp = Date.now();
const user = {
  firstName: 'Progress',
  lastName: 'E2E',
  email: `progress.e2e.${stamp}@example.com`,
  phoneNumber: `+1559${String(stamp).slice(-6)}`,
  password: 'Phase4Pass123',
};

let ok = true;
const check = (name, cond, detail = '') => {
  if (!cond) ok = false;
  console.log(`${cond ? 'PASS' : 'FAIL'}  ${name}  ${detail}`);
};

try {
  // 1. Anonymous access -> normalized 401.
  let anonRejected = false;
  try {
    await progressApi.getByUser(999);
  } catch (error) {
    anonRejected = error.status === 401;
  }
  check('anonymous GET /progress/user/{id} -> normalized 401', anonRejected, '');

  // 2. Register + create course + enroll (prerequisite for progress records).
  const jwt = await authApi.register(user);
  setToken(jwt.token);
  const userId = jwt.user.id;
  const course = await apiClient
    .post('/courses', {
      title: `Progress E2E ${stamp}`,
      description: 'Created by the progress E2E test.',
      category: 'E2E Testing',
      price: 19.99,
      duration: 8,
      instructorName: 'Progress E2E',
    })
    .then((r) => r.data?.data ?? r.data);
  await apiClient
    .post('/enrollments', { userId, courseId: course.id })
    .then((r) => r.data?.data ?? r.data);
  check('prerequisites (register/course/enroll) ready', Boolean(course.id), `courseId=${course.id}`);

  // 3. Fresh user has no progress records.
  const before = await progressApi.getByUser(userId);
  check('progressApi.getByUser -> empty list', Array.isArray(before) && before.length === 0, '');

  // 4. Create progress at 45.5%.
  const created = await apiClient
    .post('/progress', { userId, courseId: course.id, progressPercentage: 45.5 })
    .then((r) => r.data?.data ?? r.data);
  check(
    'POST /progress 45.5 -> not completed',
    Number(created.progressPercentage) === 45.5 && created.completed === false,
    `pct=${created.progressPercentage} completed=${created.completed}`
  );

  // 5. The real frontend service returns the record with UI fields.
  const list = await progressApi.getByUser(userId);
  const first = list[0];
  check(
    'progressApi.getByUser -> 1 record with UI fields',
    list.length === 1 &&
      first.courseId === course.id &&
      first.courseTitle === course.title &&
      Boolean(first.startedAt),
    `title=${first?.courseTitle} started=${Boolean(first?.startedAt)}`
  );

  // 6. Update to 100% -> completed, completedAt populated.
  const updated = await apiClient
    .put(`/progress/${created.id}`, { userId, courseId: course.id, progressPercentage: 100 })
    .then((r) => r.data?.data ?? r.data);
  check(
    'PUT /progress 100 -> completed + completedAt',
    updated.completed === true && Boolean(updated.completedAt),
    `completed=${updated.completed} completedAt=${updated.completedAt}`
  );

  // 7. The dashboard list now reflects completion.
  const after = await progressApi.getByUser(userId);
  check(
    'getByUser reflects completed status',
    after.length === 1 && after[0].completed === true && Number(after[0].progressPercentage) === 100,
    `pct=${after[0]?.progressPercentage}`
  );
} catch (error) {
  ok = false;
  console.log(`ERROR: ${error.message}`);
}

await server.close();
console.log(ok ? '\nPROGRESS E2E PASSED' : '\nPROGRESS E2E FAILED');
process.exit(ok ? 0 : 1);
