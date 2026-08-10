import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import PageHeader from '../components/common/PageHeader.jsx';
import Badge from '../components/common/Badge.jsx';
import Button from '../components/common/Button.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { useAuth } from '../context/AuthContext.jsx';
import { progressApi } from '../services/progressService.js';
import { ROUTES, coursePath } from '../utils/constants.js';
import { formatDate, formatPercent, pluralize } from '../utils/formatters.js';
import { getApiErrorMessage } from '../utils/errors.js';
import './Progress.css';

const CHART_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <path d="M3 20h18" />
    <path d="M6 20v-6M12 20V8M18 20V4" />
  </svg>
);

const ERROR_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <circle cx="12" cy="12" r="9" />
    <path d="M12 8v5" />
    <path d="M12 16.5h.01" />
  </svg>
);

/** Clamp + sanitize the backend percentage (0-100) for safe rendering. */
function toPercent(value) {
  const n = Number(value);
  if (Number.isNaN(n)) return 0;
  return Math.min(100, Math.max(0, n));
}

/**
 * "My Progress" dashboard. Loads the learner's progress records from
 * GET /api/progress/user/{userId} and shows per-course completion with
 * progress bars, status badges, and dates. The route is guarded by
 * ProtectedRoute; without a user the page shows the empty state.
 */
export default function Progress() {
  useDocumentTitle('My Progress');
  const { user } = useAuth();

  const [records, setRecords] = useState(null); // null until loaded
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!user?.id) {
      setLoading(false);
      setRecords([]);
      setError(null);
      return undefined;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);
    setRecords(null);
    progressApi
      .getByUser(user.id)
      .then((data) => {
        if (cancelled) return;
        setRecords(Array.isArray(data) ? data : []);
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

  const stats = useMemo(() => {
    const list = Array.isArray(records) ? records : [];
    const completed = list.filter(
      (record) => record.completed || toPercent(record.progressPercentage) >= 100
    ).length;
    return { total: list.length, inProgress: list.length - completed, completed };
  }, [records]);

  const emptyStateCard = (
    <div className="card">
      <EmptyState
        icon={CHART_ICON}
        title="No progress yet"
        description="Enroll in a course and start learning — your completion percentage for each program will show up here."
      >
        <Button to={ROUTES.courses}>Browse courses</Button>
      </EmptyState>
    </div>
  );

  return (
    <>
      <PageHeader
        title="My Progress"
        subtitle="Track completion percentages across all your programs and see how far you've come."
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
                Loading your progress…
              </div>
            </div>
          ) : error ? (
            <div className="card">
              <EmptyState
                role="alert"
                icon={ERROR_ICON}
                title="Couldn't load your progress"
                description={getApiErrorMessage(error)}
              >
                <Button variant="outline" onClick={() => setReloadKey((key) => key + 1)}>
                  Try again
                </Button>
              </EmptyState>
            </div>
          ) : records && records.length === 0 ? (
            emptyStateCard
          ) : records ? (
            <>
              {/* Summary stats */}
              <div className="progress-stats">
                <div className="progress-stat card">
                  <span className="progress-stat-value">{stats.total}</span>
                  <span className="progress-stat-label">Courses started</span>
                </div>
                <div className="progress-stat card">
                  <span className="progress-stat-value">{stats.inProgress}</span>
                  <span className="progress-stat-label">In progress</span>
                </div>
                <div className="progress-stat card">
                  <span className="progress-stat-value">{stats.completed}</span>
                  <span className="progress-stat-label">Completed</span>
                </div>
              </div>

              {/* Per-course progress */}
              <div className="progress-meta">
                <span>{pluralize(stats.total, 'course')}</span>
                <Link to={ROUTES.courses} className="progress-browse-link">
                  Browse more courses
                </Link>
              </div>

              <ul className="progress-list">
                {records.map((record) => {
                  const pct = toPercent(record.progressPercentage);
                  const isCompleted = record.completed || pct >= 100;
                  return (
                    <li key={record.id} className="progress-item card">
                      <div className="progress-item-head">
                        <div className="progress-item-main">
                          <h3>
                            <Link to={coursePath(record.courseId)}>{record.courseTitle}</Link>
                          </h3>
                          <div className="progress-item-meta">
                            <span>Started {formatDate(record.startedAt)}</span>
                            {isCompleted && record.completedAt ? (
                              <span>Completed {formatDate(record.completedAt)}</span>
                            ) : null}
                          </div>
                        </div>
                        <div className="progress-item-actions">
                          <Badge variant={isCompleted ? 'success' : 'neutral'}>
                            {isCompleted ? 'Completed' : 'In progress'}
                          </Badge>
                          <Button to={coursePath(record.courseId)} variant="outline" size="sm">
                            View course
                          </Button>
                        </div>
                      </div>

                      <div className="progress-bar-row">
                        <div
                          className="progress-bar"
                          role="progressbar"
                          aria-valuemin="0"
                          aria-valuemax="100"
                          aria-valuenow={pct}
                          aria-label={`${record.courseTitle} progress`}
                        >
                          <div
                            className={`progress-bar-fill${isCompleted ? ' is-completed' : ''}`}
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                        <span className="progress-percent">
                          {formatPercent(pct, pct % 1 === 0 ? 0 : 1)}
                        </span>
                      </div>
                    </li>
                  );
                })}
              </ul>
            </>
          ) : null}
        </div>
      </section>
    </>
  );
}
