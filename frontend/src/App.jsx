import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout.jsx';
import ProtectedRoute from './components/common/ProtectedRoute.jsx';
import { AuthProvider } from './context/AuthContext.jsx';
import Home from './pages/Home.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import Courses from './pages/Courses.jsx';
import CourseDetails from './pages/CourseDetails.jsx';
import MyEnrollments from './pages/MyEnrollments.jsx';
import Progress from './pages/Progress.jsx';
import NotFound from './pages/NotFound.jsx';
import { ROUTES } from './utils/constants.js';

/**
 * Application route tree.
 *
 * Public routes: Home, Courses, Course Details, Login, Register, 404.
 * Protected routes (require a valid session): My Enrollments, Progress.
 * All pages share the common <Layout /> shell (Navbar + Footer).
 */
export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route element={<Layout />}>
            <Route path={ROUTES.home} element={<Home />} />
            <Route path={ROUTES.courses} element={<Courses />} />
            <Route path={ROUTES.courseDetails} element={<CourseDetails />} />
            <Route path={ROUTES.login} element={<Login />} />
            <Route path={ROUTES.register} element={<Register />} />

            <Route
              path={ROUTES.myEnrollments}
              element={
                <ProtectedRoute>
                  <MyEnrollments />
                </ProtectedRoute>
              }
            />
            <Route
              path={ROUTES.progress}
              element={
                <ProtectedRoute>
                  <Progress />
                </ProtectedRoute>
              }
            />

            <Route path="*" element={<NotFound />} />
          </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}
