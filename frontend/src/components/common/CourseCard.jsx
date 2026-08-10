import { Link } from 'react-router-dom';
import Badge from './Badge.jsx';
import { coursePath, SKILL_LEVEL_LABELS } from '../../utils/constants.js';
import { formatDuration, formatPrice, pluralize } from '../../utils/formatters.js';
import './CourseCard.css';

const LEVEL_VARIANT = {
  BEGINNER: 'success',
  INTERMEDIATE: 'warning',
  ADVANCED: 'danger',
};

/**
 * Card summarizing a course. Used in the Home featured section and the
 * Courses catalog grid. Currently renders sample data; the shape mirrors the
 * backend CourseResponse DTO so it can be swapped to live API data later.
 */
export default function CourseCard({ course }) {
  const levelLabel = SKILL_LEVEL_LABELS[course.skillLevel] || course.skillLevel;

  return (
    <article className="course-card card card-hover">
      <div className="course-card-top">
        <Badge variant="primary">{course.category}</Badge>
        <Badge variant={LEVEL_VARIANT[course.skillLevel] || 'neutral'}>{levelLabel}</Badge>
      </div>

      <h3 className="course-card-title">
        <Link to={coursePath(course.id)}>{course.title}</Link>
      </h3>

      <p className="course-card-desc">{course.description}</p>

      <div className="course-card-meta">
        <span className="course-card-meta-item" title="Instructor">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8Zm-7 8a7 7 0 0 1 14 0" />
          </svg>
          {course.instructorName}
        </span>
        <span className="course-card-meta-item" title="Duration">
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 7v5l3 2" />
          </svg>
          {formatDuration(course.duration)}
        </span>
        {course.lessonsCount ? (
          <span className="course-card-meta-item" title="Lessons">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M4 5h16v14H4z" />
              <path d="M8 9h8M8 13h5" />
            </svg>
            {pluralize(course.lessonsCount, 'lesson')}
          </span>
        ) : null}
      </div>

      <div className="course-card-footer">
        <span className={`course-card-price${course.price === 0 ? ' is-free' : ''}`}>
          {formatPrice(course.price)}
        </span>
        <Link to={coursePath(course.id)} className="course-card-link">
          View course
          <svg viewBox="0 0 24 24" aria-hidden="true">
            <path d="M5 12h14m-6-6 6 6-6 6" />
          </svg>
        </Link>
      </div>
    </article>
  );
}
