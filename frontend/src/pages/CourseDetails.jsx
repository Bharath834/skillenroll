import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import Badge from '../components/common/Badge.jsx';
import Button from '../components/common/Button.jsx';
import Alert from '../components/common/Alert.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { useAuth } from '../context/AuthContext.jsx';
import { courseApi } from '../services/courseService.js';
import { ROUTES } from '../utils/constants.js';
import { formatDuration, formatMinutes, formatPrice, pluralize } from '../utils/formatters.js';
import { getApiErrorMessage } from '../utils/errors.js';
import './CourseDetails.css';

const LOCK_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <rect x="5" y="11" width="14" height="9" rx="2" />
    <path d="M8 11V8a4 4 0 0 1 8 0v3" />
  </svg>
);

const SEARCH_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <circle cx="11" cy="11" r="7" />
    <path d="m21 21-4.35-4.35" />
  </svg>
);

const ERROR_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <circle cx="12" cy="12" r="9" />
    <path d="M12 8v5" />
    <path d="M12 16.5h.01" />
  </svg>
);

/**
 * Course details. Fetches the real course (GET /api/courses/{id}) and its
 * curriculum (GET /api/lessons/course/{courseId}) and lets an authenticated
 * learner enroll (POST /api/enrollments). All of these endpoints require a
 * valid JWT, so anonymous visitors see a sign-in prompt.
 */
export default function CourseDetails() {
  const { id } = useParams();
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  const [reloadKey, setReloadKey] = useState(0);

  const [course, setCourse] = useState(null);
  const [courseError, setCourseError] = useState(null);
  const [courseLoading, setCourseLoading] = useState(false);

  const [lessons, setLessons] = useState(null); // null until loaded
  const [lessonsLoading, setLessonsLoading] = useState(false);
  const [lessonsError, setLessonsError] = useState(null);

  const [enrolled, setEnrolled] = useState(false);
  const [enrollState, setEnrollState] = useState('idle'); // idle | submitting | success | error
  const [enrollMessage, setEnrollMessage] = useState('');

  useDocumentTitle(course ? course.title : courseError ? 'Course not available' : 'Course');

  // Load the course (and reset dependent state) whenever the id, the auth
  // state, or the reload key changes.
  useEffect(() => {
    if (!isAuthenticated) {
      setCourse(null);
      setCourseError(null);
      setCourseLoading(false);
      setLessons(null);
      setLessonsLoading(false);
      setLessonsError(null);
      setEnrolled(false);
      setEnrollState('idle');
      setEnrollMessage('');
      return undefined;
    }

    let cancelled = false;
    setCourseLoading(true);
    setCourseError(null);
    setCourse(null);
    setLessons(null);
    setLessonsError(null);
    setEnrolled(false);
    setEnrollState('idle');
    setEnrollMessage('');

    courseApi
      .getCourseById(id)
      .then((data) => {
        if (cancelled) return;
        setCourse(data);
      })
      .catch((err) => {
        if (cancelled) return;
        setCourseError(err);
      })
      .finally(() => {
        if (!cancelled) setCourseLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [id, isAuthenticated, reloadKey]);

  // Fetch the curriculum once the course is loaded.
  useEffect(() => {
    if (!course) return undefined;

    let cancelled = false;
    setLessonsLoading(true);
    setLessonsError(null);
    courseApi
      .getLessonsByCourse(course.id)
      .then((page) => {
        if (cancelled) return;
        setLessons(Array.isArray(page?.content) ? page.content : []);
      })
      .catch((err) => {
        if (cancelled) return;
        setLessonsError(err);
      })
      .finally(() => {
        if (!cancelled) setLessonsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [course]);

  // Pre-check whether the current user is already enrolled (non-blocking:
  // the Enroll action surfaces a 409 if this check is unavailable).
  useEffect(() => {
    if (!course || !user) return undefined;

    let cancelled = false;
    courseApi
      .getEnrollmentsForUserAndCourse(user.id, course.id)
      .then((page) => {
        if (cancelled) return;
        if (Array.isArray(page?.content) && page.content.length > 0) setEnrolled(true);
      })
      .catch(() => {
        // Ignored — the enroll action handles the already-enrolled case.
      });

    return () => {
      cancelled = true;
    };
  }, [course, user]);

  const handleEnroll = async () => {
    if (!user || !course || enrollState === 'submitting') return;

    setEnrollState('submitting');
    setEnrollMessage('');
    try {
      await courseApi.enroll({ userId: user.id, courseId: course.id });
      setEnrolled(true);
      setEnrollState('success');
      setEnrollMessage('You are enrolled. Start learning whenever you are ready.');
    } catch (err) {
      if (err.status === 409) {
        setEnrolled(true);
        setEnrollState('success');
        setEnrollMessage('You are already enrolled in this course.');
      } else {
        setEnrollState('error');
        setEnrollMessage(
          getApiErrorMessage(err, 'We could not complete the enrollment. Please try again.')
        );
      }
    }
  };

  if (!isAuthenticated) {
    return (
      <section className="section-sm">
        <div className="container">
          <div className="card">
            <EmptyState
              icon={LOCK_ICON}
              title="Sign in to view course details"
              description="The catalog requires a signed-in account. Log in to browse courses and enroll."
            >
              <Button to={ROUTES.login} state={{ from: location }}>
                Log in
              </Button>
              <Button to={ROUTES.register} variant="outline">
                Create account
              </Button>
            </EmptyState>
          </div>
        </div>
      </section>
    );
  }

  if (courseLoading) {
    return (
      <section className="section-sm">
        <div className="container">
          <div className="card">
            <div className="page-loading" role="status">
              <span className="spinner spinner-dark" aria-hidden="true" />
              Loading course…
            </div>
          </div>
        </div>
      </section>
    );
  }

  if (courseError) {
    const isNotFound = courseError.status === 404;
    return (
      <section className="section-sm">
        <div className="container">
          <div className="card">
            <EmptyState
              role="alert"
              icon={isNotFound ? SEARCH_ICON : ERROR_ICON}
              title={isNotFound ? 'Course not found' : "Couldn't load this course"}
              description={
                isNotFound
                  ? `We couldn't find a course with id “${id}”. It may have been removed.`
                  : getApiErrorMessage(courseError)
              }
            >
              <Button to={ROUTES.courses} variant="outline">
                Back to courses
              </Button>
              {!isNotFound ? (
                <Button onClick={() => setReloadKey((key) => key + 1)}>Try again</Button>
              ) : null}
            </EmptyState>
          </div>
        </div>
      </section>
    );
  }

  if (!course) return null;

  const lessonCount = Array.isArray(lessons) ? lessons.length : null;

  return (
    <>
      {/* Breadcrumb */}
      <nav className="container course-breadcrumb" aria-label="Breadcrumb">
        <Link to={ROUTES.home}>Home</Link>
        <span aria-hidden="true">/</span>
        <Link to={ROUTES.courses}>Courses</Link>
        <span aria-hidden="true">/</span>
        <span className="course-breadcrumb-current">{course.title}</span>
      </nav>

      <section className="container course-detail">
        {/* Main column */}
        <div className="course-detail-main">
          <div className="card course-detail-hero">
            <div className="course-detail-head">
              <div className="course-detail-badges">
                <Badge variant="primary">{course.category}</Badge>
              </div>
              <h1>{course.title}</h1>

              <div className="course-detail-meta">
                <span className="course-detail-meta-item">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 8a7 7 0 0 1 14 0" />
                  </svg>
                  {course.instructorName}
                </span>
                <span className="course-detail-meta-item">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <circle cx="12" cy="12" r="9" />
                    <path d="M12 7v5l3 2" />
                  </svg>
                  {formatDuration(course.duration)}
                </span>
                {lessonCount !== null ? (
                  <span className="course-detail-meta-item">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M4 5h16v14H4z" />
                      <path d="M8 9h8M8 13h5" />
                    </svg>
                    {pluralize(lessonCount, 'lesson')}
                  </span>
                ) : null}
              </div>
            </div>

            <div className="course-detail-body">
              <h2>About this course</h2>
              <p>
                {course.description ||
                  'No description has been provided for this course yet.'}
              </p>

              <h2>Course curriculum</h2>
              {lessonsLoading ? (
                <p className="course-detail-curriculum-note">Loading curriculum…</p>
              ) : lessonsError ? (
                <p className="course-detail-curriculum-note">
                  The curriculum could not be loaded right now.
                </p>
              ) : Array.isArray(lessons) && lessons.length > 0 ? (
                <ol className="course-curriculum">
                  {lessons.map((lesson) => (
                    <li key={lesson.id} className="course-lesson">
                      <div className="course-lesson-info">
                        <span className="course-lesson-index">
                          {String(lesson.lessonOrder ?? '—').padStart(2, '0')}
                        </span>
                        <div>
                          <h3>{lesson.title}</h3>
                          {lesson.description ? (
                            <p className="course-lesson-desc">{lesson.description}</p>
                          ) : null}
                        </div>
                      </div>
                      <span className="course-lesson-duration">
                        {formatMinutes(lesson.durationMinutes)}
                      </span>
                    </li>
                  ))}
                </ol>
              ) : (
                <p className="course-detail-curriculum-note">
                  No lessons have been added to this course yet.
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <aside className="course-detail-side">
          <div className="card course-detail-card">
            <div className="course-detail-price">
              <span className={course.price === 0 ? 'is-free' : ''}>
                {formatPrice(course.price)}
              </span>
            </div>

            <Button
              block
              size="lg"
              onClick={handleEnroll}
              disabled={enrolled || enrollState === 'submitting'}
            >
              {enrolled
                ? 'Enrolled'
                : enrollState === 'submitting'
                  ? 'Enrolling…'
                  : 'Enroll now'}
            </Button>
            <p className="course-detail-card-note">
              {enrolled
                ? 'You are enrolled in this course.'
                : 'Enroll in one click and start learning immediately.'}
            </p>

            {enrollState === 'success' ? (
              <Alert variant="success" className="course-detail-alert">
                {enrollMessage}
              </Alert>
            ) : null}
            {enrollState === 'error' ? (
              <Alert variant="danger" className="course-detail-alert">
                {enrollMessage}
              </Alert>
            ) : null}

            <ul className="course-detail-facts">
              <li>
                <span>Instructor</span>
                <strong>{course.instructorName}</strong>
              </li>
              <li>
                <span>Category</span>
                <strong>{course.category}</strong>
              </li>
              <li>
                <span>Duration</span>
                <strong>{formatDuration(course.duration)}</strong>
              </li>
              {lessonCount !== null ? (
                <li>
                  <span>Lessons</span>
                  <strong>{pluralize(lessonCount, 'lesson')}</strong>
                </li>
              ) : null}
            </ul>
          </div>
        </aside>
      </section>
    </>
  );
}
