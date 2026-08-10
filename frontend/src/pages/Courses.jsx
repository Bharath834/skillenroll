import { useMemo, useState } from 'react';
import PageHeader from '../components/common/PageHeader.jsx';
import CourseCard from '../components/common/CourseCard.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import Button from '../components/common/Button.jsx';
import Badge from '../components/common/Badge.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { SKILL_LEVELS, SKILL_LEVEL_LABELS } from '../utils/constants.js';
import { pluralize } from '../utils/formatters.js';
import { SAMPLE_COURSES } from '../utils/sampleData.js';
import './Courses.css';

/**
 * Course catalog placeholder.
 * Renders sample data with local search + skill-level filtering so the UI
 * patterns (toolbar, empty state, count) are real. Live catalog integration
 * (GET /api/courses) replaces this in the next phase.
 */
export default function Courses() {
  useDocumentTitle('Courses');

  const [search, setSearch] = useState('');
  const [level, setLevel] = useState('ALL');

  const filtered = useMemo(() => {
    const query = search.trim().toLowerCase();
    return SAMPLE_COURSES.filter((course) => {
      const matchesLevel = level === 'ALL' || course.skillLevel === level;
      const matchesSearch =
        query.length === 0 ||
        course.title.toLowerCase().includes(query) ||
        course.description.toLowerCase().includes(query) ||
        course.category.toLowerCase().includes(query) ||
        course.instructorName.toLowerCase().includes(query);
      return matchesLevel && matchesSearch;
    });
  }, [search, level]);

  const hasFilters = search.trim() !== '' || level !== 'ALL';

  const clearFilters = () => {
    setSearch('');
    setLevel('ALL');
  };

  return (
    <>
      <PageHeader
        title="Course Catalog"
        subtitle="Browse skill programs across web development, data science, cloud, design, and more."
      />

      <section className="section-sm courses-section">
        <div className="container">
          {/* Toolbar */}
          <div className="courses-toolbar card">
            <div className="courses-search">
              <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <circle cx="11" cy="11" r="7" />
                <path d="m21 21-4.35-4.35" />
              </svg>
              <input
                type="search"
                className="courses-search-input"
                placeholder="Search courses, categories, instructors…"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                aria-label="Search courses"
              />
            </div>

            <div className="courses-filters">
              <label className="sr-only" htmlFor="level-filter">
                Filter by skill level
              </label>
              <select
                id="level-filter"
                className="form-control courses-level-select"
                value={level}
                onChange={(event) => setLevel(event.target.value)}
              >
                <option value="ALL">All levels</option>
                {SKILL_LEVELS.map((option) => (
                  <option key={option} value={option}>
                    {SKILL_LEVEL_LABELS[option]}
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
              {filtered.length > 0
                ? `Showing ${pluralize(filtered.length, 'course')}`
                : 'No courses found'}
            </span>
            <Badge variant="outline">Sample data — API integration coming soon</Badge>
          </div>

          {/* Grid / empty state */}
          {filtered.length > 0 ? (
            <div className="grid grid-3 courses-grid">
              {filtered.map((course) => (
                <CourseCard key={course.id} course={course} />
              ))}
            </div>
          ) : (
            <div className="card courses-empty">
              <EmptyState
                icon={
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <circle cx="11" cy="11" r="7" />
                    <path d="m21 21-4.35-4.35" />
                    <path d="M8 11h6" />
                  </svg>
                }
                title={search ? `No courses match “${search}”` : 'No courses found'}
                description="Try a different search term or reset the filters to see the full catalog."
              >
                <Button variant="outline" onClick={clearFilters}>
                  Clear filters
                </Button>
              </EmptyState>
            </div>
          )}
        </div>
      </section>
    </>
  );
}
