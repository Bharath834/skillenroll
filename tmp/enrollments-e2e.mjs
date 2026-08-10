/**
 * End-to-end test of the FRONTEND "My Enrollments" service code against the
 * live backend. Loads the actual src/services/courseService.js, authService.js
 * and apiClient.js through Vite's SSR runner (with a localStorage shim so the
 * real Bearer-token interceptor runs, exactly as in the browser).
 *
 * Covers: register -> empty enrollments -> enroll in two courses ->
 * getEnrollmentsByUser (count, fields, newest-first order) -> empty for a
 * fresh user.
 * Run from the frontend directory:  node .enrollments-e2e.mjs  (backend :8080)
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
const { courseApi } = await server.ssrLoadModule('/src/services/courseService.js');
const { default: apiClient } = await server.ssrLoadModule('/src/services/apiClient.js');

const stamp = Date.now();
const user = {
  firstName: 'Enroll',
  lastName: 'E2E',
  email: `enroll.e2e.${stamp}@example.com`,
  phoneNumber: `+1557${String(stamp).slice(-6)}`,
  password: 'Phase3Pass123',
};

let ok = true;
const check = (name, cond, detail = '') => {
  if (!cond) ok = false;
  console.log(`${cond ? 'PASS' : 'FAIL'}  ${name}  ${detail}`);
};

try {
  // 1. Register a fresh user.
  const jwt = await authApi.register(user);
  setToken(jwt.token);
  const userId = jwt.user.id;
  check('authApi.register -> user.id', Boolean(userId), `userId=${userId}`);

  // 2. Fresh user has no enrollments.
  const empty = await courseApi.getEnrollmentsByUser(userId);
  check(
    'courseApi.getEnrollmentsByUser -> empty page',
    (empty?.content ?? []).length === 0 && empty?.totalElements === 0,
    `totalElements=${empty?.totalElements}`
  );

  // 3. Create two courses and enroll in both.
  const createdCourses = [];
  for (const name of ['Alpha', 'Beta']) {
    const course = await apiClient
      .post('/courses', {
        title: `Enroll E2E ${name} ${stamp}`,
        description: `Created by enrollments E2E (${name}).`,
        category: 'E2E Testing',
        price: 9.99,
        duration: 4,
        instructorName: 'Enroll E2E',
      })
      .then((r) => r.data?.data ?? r.data);
    createdCourses.push(course);
  }
  const first = await courseApi.enroll({ userId, courseId: createdCourses[0].id });
  const second = await courseApi.enroll({ userId, courseId: createdCourses[1].id });
  check('enroll x2 -> 201 PENDING', first.status === 'PENDING' && second.status === 'PENDING', '');

  // 4. The learner's list now has both, with the newest (Beta) first and
  //    full EnrollmentResponse fields for the UI (title, date, status).
  const list = await courseApi.getEnrollmentsByUser(userId);
  const titles = list?.content?.map((e) => e.courseTitle) ?? [];
  check(
    'getEnrollmentsByUser -> 2 enrollments, newest first',
    list?.totalElements === 2 &&
      titles[0] === createdCourses[1].title &&
      titles[1] === createdCourses[0].title,
    `order=[${titles.join(' | ')}]`
  );
  const firstEnrollment = list.content[0];
  check(
    'EnrollmentResponse has UI fields',
    Boolean(firstEnrollment.enrollmentDate) &&
      firstEnrollment.userId === userId &&
      firstEnrollment.courseId === createdCourses[1].id,
    `date=${firstEnrollment.enrollmentDate} status=${firstEnrollment.status}`
  );

  // 5. Another fresh user sees an empty list (per-user isolation).
  const other = await authApi.register({
    ...user,
    email: `enroll.e2e.other.${stamp}@example.com`,
    phoneNumber: `+1558${String(stamp).slice(-6)}`,
  });
  const otherList = await courseApi.getEnrollmentsByUser(other.user.id);
  check(
    'enrollments are per-user (fresh user -> empty)',
    (otherList?.content ?? []).length === 0,
    ''
  );
} catch (error) {
  ok = false;
  console.log(`ERROR: ${error.message}`);
}

await server.close();
console.log(ok ? '\nENROLLMENTS E2E PASSED' : '\nENROLLMENTS E2E FAILED');
process.exit(ok ? 0 : 1);
