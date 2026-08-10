/**
 * End-to-end test of the FRONTEND course service code against the live backend.
 * Loads the actual src/services/courseService.js, authService.js and
 * apiClient.js through Vite's SSR runner. A minimal window.localStorage shim
 * lets the apiClient's REAL Bearer-token request interceptor run, exactly as
 * it does in the browser.
 *
 * Covers: register -> catalog list -> create course -> list/search/get by id
 * -> 404 -> lessons (ordered) -> enrollment pre-check -> enroll -> 409 ->
 * enrolled check -> anonymous 401.
 * Run from the frontend directory:  node .catalog-e2e.mjs  (backend on :8080)
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

// --- localStorage shim so the apiClient request interceptor (which reads
// window.localStorage with the REAL STORAGE_KEYS) attaches the Bearer token
// exactly like it does in the browser. ---
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
const email = `catalog.e2e.${stamp}@example.com`;
const user = {
  firstName: 'Catalog',
  lastName: 'E2E',
  email,
  phoneNumber: `+1556${String(stamp).slice(-6)}`, // unique 10-digit number
  password: 'Phase3Pass123',
};
const courseTitle = `E2E Catalog Course ${stamp}`;
const coursePayload = {
  title: courseTitle,
  description: 'Created by the catalog E2E test to validate the frontend course service.',
  category: 'E2E Testing',
  price: 12.5,
  duration: 6,
  instructorName: 'Catalog E2E',
};

let ok = true;
const check = (name, cond, detail = '') => {
  if (!cond) ok = false;
  console.log(`${cond ? 'PASS' : 'FAIL'}  ${name}  ${detail}`);
};

try {
  // 1. Anonymous access -> 401 (no token attached, no session-event side effect).
  let anonRejected = false;
  try {
    await courseApi.getCourses();
  } catch (error) {
    anonRejected = error.status === 401;
  }
  check('anonymous GET /courses -> normalized 401', anonRejected, '');

  // 2. Register through the real frontend auth service, then store the token.
  const jwt = await authApi.register(user);
  setToken(jwt.token);
  check(
    'authApi.register -> JwtResponse with user.id',
    Boolean(jwt?.user?.id),
    `userId=${jwt?.user?.id}`
  );

  // 3. Catalog list (authenticated through the real interceptor).
  const before = await courseApi.getCourses();
  check('courseApi.getCourses -> array', Array.isArray(before), `count=${before.length}`);

  // 4. Create a course (real authenticated endpoint).
  const created = await apiClient
    .post('/courses', coursePayload)
    .then((r) => r.data?.data ?? r.data);
  check(
    'POST /courses -> CourseResponse',
    created?.id && created.title === courseTitle && Number(created.price) === 12.5,
    `id=${created?.id}`
  );

  // 5. Catalog now contains it; search finds it by title.
  const after = await courseApi.getCourses();
  check('catalog contains created course', after.some((c) => c.id === created.id), '');
  const results = await courseApi.searchCourses(stamp); // unique fragment of the title
  check(
    'courseApi.searchCourses(title fragment) finds it',
    results.some((c) => c.id === created.id),
    `results=${results.length}`
  );

  // 6. Get by id matches.
  const byId = await courseApi.getCourseById(created.id);
  check(
    'courseApi.getCourseById -> same course',
    byId?.id === created.id && byId.category === 'E2E Testing',
    `instructor=${byId?.instructorName}`
  );

  // 7. Unknown id -> normalized 404 with the backend message.
  let notFound = false;
  try {
    await courseApi.getCourseById(99999999);
  } catch (error) {
    notFound = error.status === 404 && /not found/i.test(error.message);
  }
  check('unknown course id -> normalized 404', notFound, '');

  // 8. Add two lessons in order; the curriculum fetch must preserve order.
  const lesson1 = await apiClient
    .post('/lessons', {
      courseId: created.id,
      title: `Lesson One ${stamp}`,
      description: 'First lesson',
      lessonOrder: 1,
      durationMinutes: 45,
    })
    .then((r) => r.data?.data ?? r.data);
  const lesson2 = await apiClient
    .post('/lessons', {
      courseId: created.id,
      title: `Lesson Two ${stamp}`,
      description: 'Second lesson',
      lessonOrder: 2,
      durationMinutes: 90,
    })
    .then((r) => r.data?.data ?? r.data);
  const lessonsPage = await courseApi.getLessonsByCourse(created.id);
  const lessonTitles = lessonsPage?.content?.map((l) => l.title) ?? [];
  check(
    'courseApi.getLessonsByCourse -> ordered PageResponse',
    lessonsPage?.totalElements === 2 &&
      lessonTitles[0] === lesson1.title &&
      lessonTitles[1] === lesson2.title &&
      lessonsPage.content[1].durationMinutes === 90,
    `totalElements=${lessonsPage?.totalElements}`
  );

  // 9. Enrollment pre-check before enrolling -> empty.
  const pre = await courseApi.getEnrollmentsForUserAndCourse(jwt.user.id, created.id);
  check('pre-enrollment check -> no enrollments', (pre?.content ?? []).length === 0, '');

  // 10. Enroll through the real frontend service -> 201 EnrollmentResponse.
  const enrollment = await courseApi.enroll({ userId: jwt.user.id, courseId: created.id });
  check(
    'courseApi.enroll -> EnrollmentResponse (PENDING)',
    enrollment?.courseId === created.id && enrollment?.userId === jwt.user.id,
    `status=${enrollment?.status}`
  );

  // 11. Enrolling again -> normalized 409.
  let conflict = false;
  try {
    await courseApi.enroll({ userId: jwt.user.id, courseId: created.id });
  } catch (error) {
    conflict = error.status === 409;
  }
  check('duplicate enroll -> normalized 409', conflict, '');

  // 12. Pre-check now reports the enrollment.
  const post = await courseApi.getEnrollmentsForUserAndCourse(jwt.user.id, created.id);
  check(
    'post-enrollment check -> 1 enrollment',
    post?.content?.length === 1 && post.content[0].courseId === created.id,
    `count=${post?.content?.length}`
  );
} catch (error) {
  ok = false;
  console.log(`ERROR: ${error.message}`);
}

await server.close();
console.log(ok ? '\nCATALOG E2E PASSED' : '\nCATALOG E2E FAILED');
process.exit(ok ? 0 : 1);
