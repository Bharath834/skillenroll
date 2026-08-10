import { Link } from 'react-router-dom';
import Badge from './Badge.jsx';
import { coursePath } from '../../utils/constants.js';
import { formatDuration, formatPrice } from '../../utils/formatters.js';
import './CourseCard.css';

/**
 * Card summarizing a course. Used in the Home featured section and the
 * Courses catalog grid. Renders only fields present in the backend
 * CourseResponse DTO (id, title, description, category, price, duration,
 * instructorName) — no invented fields.
 */
export default function CourseCard({ course }) {
  return (
    <article className="course-card card card-hover">
      <div className="course-card-top">
        <Badge variant="primary">{course.category}</Badge>
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
