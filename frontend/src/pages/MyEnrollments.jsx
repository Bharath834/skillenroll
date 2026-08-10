import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../components/common/PageHeader.jsx';
import Badge from '../components/common/Badge.jsx';
import Button from '../components/common/Button.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { useAuth } from '../context/AuthContext.jsx';
import { courseApi } from '../services/courseService.js';
import { ROUTES, coursePath, ENROLLMENT_STATUS_LABELS } from '../utils/constants.js';
import { formatDate, pluralize } from '../utils/formatters.js';
import { getApiErrorMessage } from '../utils/errors.js';
import './MyEnrollments.css';

/** Badge color per backend EnrollmentStatus. */
const STATUS_VARIANT = {
  PENDING: 'warning',
  ACTIVE: 'success',
  COMPLETED: 'success',
  CANCELLED: 'neutral',
};

const BOOKMARK_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M6 3h12v18l-6-4-6 4z" />
    <path d="M9 8h6" />
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
 * "My Enrollments" — the signed-in learner's enrollments, loaded from
 * GET /api/enrollments/user/{userId}. Each row links back to its course.
 * The route is guarded by ProtectedRoute; without a user the page shows the
 * empty state.
 */
export default function MyEnrollments() {
  useDocumentTitle('My Enrollments');
  const { user } = useAuth();

  const [enrollments, setEnrollments] = useState(null); // null until loaded
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!user?.id) {
      setLoading(false);
      setEnrollments([]);
      setError(null);
      return undefined;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);
    setEnrollments(null);
    courseApi
      .getEnrollmentsByUser(user.id)
      .then((page) => {
        if (cancelled) return;
        const content = Array.isArray(page?.content) ? page.content : [];
        setEnrollments(content);
        setTotalElements(page?.totalElements ?? content.length);
      })
      .catch((err) => {
        if (cancelled) return;
        setError(err);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [user?.id, reloadKey]);

  const emptyStateCard = (
    <div className="card">
      <EmptyState
        icon={BOOKMARK_ICON}
        title="No enrollments yet"
        description="Browse the catalog and enroll in your first program — it takes one click and you can start right away."
      >
        <Button to={ROUTES.courses}>Browse courses</Button>
      </EmptyState>
    </div>
  );

  return (
    <>
      <PageHeader
        title="My Enrollments"
        subtitle="Every program you've enrolled in, with status and quick access to continue learning."
      />

      <section className="section-sm">
        <div className="container">
          {/* Anonymous / no user (normally blocked by ProtectedRoute; also the
              SSR render path) — show the empty state. */}
          {!user?.id ? (
            emptyStateCard
          ) : loading ? (
            <div className="card">
              <div className="page-loading" role="status">
                <span className="spinner spinner-dark" aria-hidden="true" />
                Loading your enrollments…
              </div>
            </div>
          ) : error ? (
            <div className="card">
              <EmptyState
                role="alert"
                icon={ERROR_ICON}
                title="Couldn't load your enrollments"
                description={getApiErrorMessage(error)}
              >
                <Button variant="outline" onClick={() => setReloadKey((key) => key + 1)}>
                  Try again
                </Button>
              </EmptyState>
            </div>
          ) : enrollments && enrollments.length === 0 ? (
            emptyStateCard
          ) : enrollments ? (
            <>
              <div className="enrollments-meta">
                <span>{pluralize(totalElements, 'enrollment')}</span>
                <Link to={ROUTES.courses} className="enrollments-browse-link">
                  Browse more courses
                </Link>
              </div>

              <ul className="enrollments-list">
                {enrollments.map((enrollment) => (
                  <li key={enrollment.id} className="enrollment-item card">
                    <div className="enrollment-item-main">
                      <h3>
                        <Link to={coursePath(enrollment.courseId)}>
                          {enrollment.courseTitle}
                        </Link>
                      </h3>
                      <div className="enrollment-item-meta">
                        <span title="Enrollment date">
                          Enrolled {formatDate(enrollment.enrollmentDate)}
                        </span>
                      </div>
                    </div>
                    <div className="enrollment-item-actions">
                      <Badge variant={STATUS_VARIANT[enrollment.status] || 'neutral'}>
                        {ENROLLMENT_STATUS_LABELS[enrollment.status] || enrollment.status}
                      </Badge>
                      <Button to={coursePath(enrollment.courseId)} variant="outline" size="sm">
                        View course
                      </Button>
                    </div>
                  </li>
                ))}
              </ul>
            </>
          ) : null}
        </div>
      </section>
    </>
  );
}
