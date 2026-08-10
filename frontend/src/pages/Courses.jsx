import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import PageHeader from '../components/common/PageHeader.jsx';
import CourseCard from '../components/common/CourseCard.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import Button from '../components/common/Button.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { useAuth } from '../context/AuthContext.jsx';
import { courseApi } from '../services/courseService.js';
import { ROUTES } from '../utils/constants.js';
import { pluralize } from '../utils/formatters.js';
import { getApiErrorMessage } from '../utils/errors.js';
import './Courses.css';

const SEARCH_DEBOUNCE_MS = 350;

const SEARCH_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <circle cx="11" cy="11" r="7" />
    <path d="m21 21-4.35-4.35" />
  </svg>
);

const LOCK_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <rect x="5" y="11" width="14" height="9" rx="2" />
    <path d="M8 11V8a4 4 0 0 1 8 0v3" />
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
 * Course catalog. Fetches live courses from GET /api/courses, uses the
 * backend's server-side title search (GET /api/courses/search?title=) with a
 * debounce, and applies an optional client-side category filter (categories are
 * derived from the fetched data — never hard-coded).
 *
 * The backend requires a valid JWT for all /api/courses endpoints, so anonymous
 * visitors see a sign-in prompt instead of an empty catalog.
 */
export default function Courses() {
  useDocumentTitle('Courses');
  const { isAuthenticated } = useAuth();

  const [searchInput, setSearchInput] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [category, setCategory] = useState('ALL');

  const [catalog, setCatalog] = useState([]); // full list — source of categories
  const [courses, setCourses] = useState([]); // server list (all or search results)
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const requestIdRef = useRef(0);

  // Debounce typing so we only call the search API once the user pauses.
  useEffect(() => {
    const timer = setTimeout(() => setDebouncedQuery(searchInput.trim()), SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [searchInput]);

  const loadAll = useCallback(async () => {
    const requestId = ++requestIdRef.current;
    setLoading(true);
    setError(null);
    try {
      const data = await courseApi.getCourses();
      if (requestId !== requestIdRef.current) return; // stale response
      const list = Array.isArray(data) ? data : [];
      setCatalog(list);
      setCourses(list);
    } catch (err) {
      if (requestId !== requestIdRef.current) return;
      setError(err);
    } finally {
      if (requestId === requestIdRef.current) setLoading(false);
    }
  }, []);

  const search = useCallback(async (query) => {
    const requestId = ++requestIdRef.current;
    setLoading(true);
    setError(null);
    try {
      const data = await courseApi.searchCourses(query);
      if (requestId !== requestIdRef.current) return;
      setCourses(Array.isArray(data) ? data : []);
    } catch (err) {
      if (requestId !== requestIdRef.current) return;
      setError(err);
    } finally {
      if (requestId === requestIdRef.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isAuthenticated) {
      requestIdRef.current += 1; // cancel any in-flight requests
      setCatalog([]);
      setCourses([]);
      setError(null);
      setLoading(false);
      return;
    }
    if (debouncedQuery) search(debouncedQuery);
    else loadAll();
  }, [isAuthenticated, debouncedQuery, loadAll, search]);

  const categories = useMemo(
    () =>
      [...new Set(catalog.map((course) => course.category).filter(Boolean))].sort((a, b) =>
        a.localeCompare(b)
      ),
    [catalog]
  );

  const visibleCourses = useMemo(() => {
    if (category === 'ALL') return courses;
    return courses.filter((course) => course.category === category);
  }, [courses, category]);

  const hasFilters = searchInput.trim() !== '' || category !== 'ALL';

  const clearFilters = () => {
    setSearchInput('');
    setCategory('ALL');
  };

  return (
    <>
      <PageHeader
        title="Course Catalog"
        subtitle="Browse live skill programs across every category."
      />

      <section className="section-sm courses-section">
        <div className="container">
          {!isAuthenticated ? (
            <div className="card">
              <EmptyState
                icon={LOCK_ICON}
                title="Sign in to browse the catalog"
                description="Course browsing requires a signed-in account. Log in or create one to explore available programs."
              >
                <Button to={ROUTES.login} state={{ from: { pathname: ROUTES.courses } }}>
                  Log in
                </Button>
                <Button to={ROUTES.register} variant="outline">
                  Create account
                </Button>
              </EmptyState>
            </div>
          ) : (
            <>
              {/* Toolbar */}
              <div className="courses-toolbar card">
                <div className="courses-search">
                  {SEARCH_ICON}
                  <input
                    type="search"
                    className="courses-search-input"
                    placeholder="Search by course title…"
                    value={searchInput}
                    onChange={(event) => setSearchInput(event.target.value)}
                    aria-label="Search courses by title"
                  />
                </div>

                <div className="courses-filters">
                  <label className="sr-only" htmlFor="category-filter">
                    Filter by category
                  </label>
                  <select
                    id="category-filter"
                    className="form-control courses-category-select"
                    value={category}
                    onChange={(event) => setCategory(event.target.value)}
                  >
                    <option value="ALL">All categories</option>
                    {categories.map((option) => (
                      <option key={option} value={option}>
                        {option}
                      </option>
                    ))}
                  </select>

                  {hasFilters ? (
                    <button type="button" className="courses-clear" onClick={clearFilters}>
                      Clear filters
                    </button>
                  ) : null}
                </div>
              </div>

              <div className="courses-meta">
                <span>
                  {!loading && !error
                    ? `Showing ${pluralize(visibleCourses.length, 'course')}`
                    : ' '}
                </span>
              </div>

              {/* Grid / loading / error / empty */}
              {loading ? (
                <div className="card">
                  <div className="page-loading" role="status">
                    <span className="spinner spinner-dark" aria-hidden="true" />
                    Loading courses…
                  </div>
                </div>
              ) : error ? (
                <div className="card">
                  <EmptyState
                    role="alert"
                    icon={ERROR_ICON}
                    title="Couldn't load the catalog"
                    description={getApiErrorMessage(error)}
                  >
                    <Button variant="outline" onClick={loadAll}>
                      Try again
                    </Button>
                  </EmptyState>
                </div>
              ) : visibleCourses.length > 0 ? (
                <div className="grid grid-3 courses-grid">
                  {visibleCourses.map((course) => (
                    <CourseCard key={course.id} course={course} />
                  ))}
                </div>
              ) : (
                <div className="card courses-empty">
                  <EmptyState
                    icon={SEARCH_ICON}
                    title={
                      debouncedQuery
                        ? `No courses match “${debouncedQuery}”`
                        : category !== 'ALL'
                          ? 'No courses in this category yet'
                          : 'No courses yet'
                    }
                    description={
                      hasFilters
                        ? 'Try a different search term or clear the filters to see the full catalog.'
                        : 'New programs will appear here as they are published.'
                    }
                  >
                    {hasFilters ? (
                      <Button variant="outline" onClick={clearFilters}>
                        Clear filters
                      </Button>
                    ) : null}
                  </EmptyState>
                </div>
              )}
            </>
          )}
        </div>
      </section>
    </>
  );
}
