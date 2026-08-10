import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import Badge from '../components/common/Badge.jsx';
import Button from '../components/common/Button.jsx';
import Alert from '../components/common/Alert.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { ROUTES, SKILL_LEVEL_LABELS } from '../utils/constants.js';
import { formatDuration, formatMinutes, formatPrice, pluralize } from '../utils/formatters.js';
import { getSampleCourse } from '../utils/sampleData.js';
import './CourseDetails.css';

const LEVEL_VARIANT = {
  BEGINNER: 'success',
  INTERMEDIATE: 'warning',
  ADVANCED: 'danger',
};

/**
 * Course detail placeholder.
 * Renders a single sample course by :id with curriculum and an enrollment
 * CTA. Live catalog + enrollment integration replaces this in later phases.
 */
export default function CourseDetails() {
  const { id } = useParams();
  const course = getSampleCourse(id);
  const [enrollNotice, setEnrollNotice] = useState(false);

  useDocumentTitle(course ? course.title : 'Course not found');

  if (!course) {
    return (
      <section className="section-sm">
        <div className="container">
          <div className="card">
            <EmptyState
              title="Course not found"
              description={`We couldn't find a course with id “${id}”. It may have been removed, or the catalog API isn't connected yet.`}
            >
              <Button to={ROUTES.courses} variant="outline">
                Back to courses
              </Button>
            </EmptyState>
          </div>
        </div>
      </section>
    );
  }

  const levelLabel = SKILL_LEVEL_LABELS[course.skillLevel] || course.skillLevel;
  const totalLessons = course.syllabus.reduce((sum, module) => sum + module.lessons, 0);

  const handleEnroll = () => setEnrollNotice(true);

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
                <Badge variant={LEVEL_VARIANT[course.skillLevel] || 'neutral'}>
                  {levelLabel}
                </Badge>
              </div>
              <h1>{course.title}</h1>
              <p className="course-detail-desc">{course.description}</p>

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
                <span className="course-detail-meta-item">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M4 5h16v14H4z" />
                    <path d="M8 9h8M8 13h5" />
                  </svg>
                  {pluralize(totalLessons, 'lesson')}
                </span>
                <span className="course-detail-meta-item">
                  <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="M12 3l1.9 3.9 4.3.6-3.1 3 0.7 4.3L12 13l-3.8 2 .7-4.3-3.1-3 4.3-.6z" />
                  </svg>
                  {course.rating} rating
                </span>
              </div>
            </div>

            <div className="course-detail-body">
              <h2>About this course</h2>
              <p>
                {course.description} You&apos;ll learn through structured modules,
                hands-on exercises, and practical projects that build real,
                portfolio-ready skills.
              </p>

              <h2>Course curriculum</h2>
              <ol className="course-curriculum">
                {course.syllabus.map((module, index) => (
                  <li key={module.title} className="course-module">
                    <div className="course-module-info">
                      <span className="course-module-index">
                        {String(index + 1).padStart(2, '0')}
                      </span>
                      <div>
                        <h3>{module.title}</h3>
                        <span className="course-module-lessons">
                          {pluralize(module.lessons, 'lesson')}
                        </span>
                      </div>
                    </div>
                    <span className="course-module-duration">
                      {formatMinutes(module.lessons * 45)}
                    </span>
                  </li>
                ))}
              </ol>
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

            <Button block size="lg" onClick={handleEnroll}>
              Enroll now
            </Button>
            <p className="course-detail-card-note">
              No payment required to browse. Enroll in one click and start learning immediately.
            </p>

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
                <span>Skill level</span>
                <strong>{levelLabel}</strong>
              </li>
              <li>
                <span>Duration</span>
                <strong>{formatDuration(course.duration)}</strong>
              </li>
              <li>
                <span>Lessons</span>
                <strong>{pluralize(totalLessons, 'lesson')}</strong>
              </li>
            </ul>

            {enrollNotice ? (
              <Alert variant="info" className="course-detail-alert">
                Enrollment is not connected yet — the enroll flow (POST{' '}
                <code>/api/enrollments</code>) arrives in a later phase.
              </Alert>
            ) : null}

            <Badge variant="outline" className="course-detail-sample-badge">
              Sample course — API integration coming soon
            </Badge>
          </div>
        </aside>
      </section>
    </>
  );
}
