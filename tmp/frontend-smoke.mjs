/**
 * Temporary runtime smoke test for the SkillEnroll frontend.
 * SSR-renders every page component (via Vite's ssrLoadModule) inside an
 * AuthProvider + MemoryRouter to prove each route renders without throwing.
 * Also verifies ProtectedRoute redirects anonymous visitors.
 * Run from the frontend directory:  node .smoke-test.mjs
 */
import { createServer } from 'vite';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import React from 'react';
import { renderToString } from 'react-dom/server';

const root = dirname(fileURLToPath(import.meta.url));

const server = await createServer({
  root,
  server: { middlewareMode: true },
  appType: 'custom',
  logLevel: 'error',
  // react-router v7 ships dual CJS/ESM builds; point SSR at the ESM dist
  // builds (development/ folder) and inline them so named imports
  // (Link, useParams, ...) resolve and share one module instance.
  resolve: {
    // Anchored regexes so 'react-router' never shadows the 'react-router/dom'
    // subpath import emitted by react-router-dom's index.mjs.
    alias: [
      { find: /^react-router-dom$/, replacement: join(root, 'node_modules/react-router-dom/dist/index.mjs') },
      { find: /^react-router\/dom$/, replacement: join(root, 'node_modules/react-router/dist/development/dom-export.mjs') },
      { find: /^react-router$/, replacement: join(root, 'node_modules/react-router/dist/development/index.mjs') },
    ],
  },
  ssr: { noExternal: ['react-router-dom', 'react-router'] },
});

// Load react-router and the auth provider through Vite's SSR runner so the
// harness and the app modules share the same module instances (and context).
const { MemoryRouter, Routes, Route } = await server.ssrLoadModule('react-router-dom');
const { AuthProvider } = await server.ssrLoadModule('/src/context/AuthContext.jsx');

const cases = [
  { entry: '/', pattern: '/', file: '/src/pages/Home.jsx', expect: 'Learn new skills' },
  { entry: '/courses', pattern: '/courses', file: '/src/pages/Courses.jsx', expect: 'Course Catalog' },
  // Course pages are gated behind the backend's auth requirement; the
  // anonymous SSR render shows the sign-in prompt (fetching is skipped).
  { entry: '/courses/1', pattern: '/courses/:id', file: '/src/pages/CourseDetails.jsx', expect: 'Sign in to view course details' },
  { entry: '/courses/999', pattern: '/courses/:id', file: '/src/pages/CourseDetails.jsx', expect: 'Sign in to view course details' },
  { entry: '/login', pattern: '/login', file: '/src/pages/Login.jsx', expect: 'Welcome back' },
  { entry: '/register', pattern: '/register', file: '/src/pages/Register.jsx', expect: 'Create your account' },
  { entry: '/my-enrollments', pattern: '/my-enrollments', file: '/src/pages/MyEnrollments.jsx', expect: 'No enrollments yet' },
  { entry: '/progress', pattern: '/progress', file: '/src/pages/Progress.jsx', expect: 'No progress yet' },
  { entry: '/nope', pattern: '*', file: '/src/pages/NotFound.jsx', expect: 'Page not found' },
];

const renderWithProviders = (Component, entry, pattern) =>
  renderToString(
    React.createElement(
      AuthProvider,
      null,
      React.createElement(
        MemoryRouter,
        { initialEntries: [entry] },
        React.createElement(
          Routes,
          null,
          React.createElement(Route, {
            path: pattern,
            element: React.createElement(Component),
          })
        )
      )
    )
  );

let failures = 0;

for (const c of cases) {
  try {
    const mod = await server.ssrLoadModule(c.file);
    const html = renderWithProviders(mod.default, c.entry, c.pattern);
    const ok = html.includes(c.expect);
    console.log(`${ok ? 'PASS' : 'FAIL'}  ${c.entry}  (expects "${c.expect}")`);
    if (!ok) failures += 1;
  } catch (err) {
    failures += 1;
    console.log(`ERROR ${c.entry}: ${err.message}`);
  }
}

// Whole-shell check: Layout (Navbar + Outlet + Footer) with a nested route,
// mirroring the real App.jsx tree.
const Layout = (await server.ssrLoadModule('/src/components/layout/Layout.jsx')).default;
const Home = (await server.ssrLoadModule('/src/pages/Home.jsx')).default;
try {
  const shellHtmlFull = renderToString(
    React.createElement(
      AuthProvider,
      null,
      React.createElement(
        MemoryRouter,
        { initialEntries: ['/'] },
        React.createElement(
          Routes,
          null,
          React.createElement(
            Route,
            { element: React.createElement(Layout) },
            React.createElement(Route, { path: '/', element: React.createElement(Home) })
          )
        )
      )
    )
  );
  const shellOk =
    shellHtmlFull.includes('SkillEnroll') &&
    shellHtmlFull.includes('Learn new skills') &&
    shellHtmlFull.includes('My Enrollments');
  console.log(`${shellOk ? 'PASS' : 'FAIL'}  <Layout shell with nested route>`);
  if (!shellOk) failures += 1;
} catch (err) {
  failures += 1;
  console.log(`ERROR <Layout shell>: ${err.message}`);
}

// Protected route: an anonymous visitor must NOT see protected content
// (ProtectedRoute renders a <Navigate> to /login instead).
const ProtectedRoute = (await server.ssrLoadModule('/src/components/common/ProtectedRoute.jsx')).default;
const guardedHtml = renderToString(
  React.createElement(
    AuthProvider,
    null,
    React.createElement(
      MemoryRouter,
      { initialEntries: ['/my-enrollments'] },
      React.createElement(
        Routes,
        null,
        React.createElement(Route, {
          path: '/my-enrollments',
          element: React.createElement(ProtectedRoute, null, React.createElement('div', null, 'TOP-SECRET')),
        })
      )
    )
  )
);
const guardOk = !guardedHtml.includes('TOP-SECRET');
console.log(`${guardOk ? 'PASS' : 'FAIL'}  <ProtectedRoute hides content from anonymous visitors>`);
if (!guardOk) failures += 1;

await server.close();

console.log(failures === 0 ? '\nAll checks passed.' : `\n${failures} check(s) failed.`);
process.exit(failures === 0 ? 0 : 1);
