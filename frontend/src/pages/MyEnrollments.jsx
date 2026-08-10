import PageHeader from '../components/common/PageHeader.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import Button from '../components/common/Button.jsx';
import Alert from '../components/common/Alert.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { ROUTES } from '../utils/constants.js';
import './MyEnrollments.css';

/**
 * "My Enrollments" placeholder.
 * Will list the learner's enrollments (GET /api/enrollments) with status,
 * progress, and continue-learning actions once the enrollment API is wired.
 */
export default function MyEnrollments() {
  useDocumentTitle('My Enrollments');

  return (
    <>
      <PageHeader
        title="My Enrollments"
        subtitle="Every program you've enrolled in, with status and quick access to continue learning."
      />

      <section className="section-sm">
        <div className="container">
          <div className="card">
            <EmptyState
              icon={
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M6 3h12v18l-6-4-6 4z" />
                  <path d="M9 8h6" />
                </svg>
              }
              title="No enrollments yet"
              description="Browse the catalog and enroll in your first program — it takes one click and you can start right away."
            >
              <Button to={ROUTES.courses}>Browse courses</Button>
            </EmptyState>
          </div>

          <div className="enrollments-note">
            <Alert variant="info">
              The enrollments list will be loaded from the backend (GET{' '}
              <code>/api/enrollments</code>) in the enrollment integration phase.
            </Alert>
          </div>
        </div>
      </section>
    </>
  );
}
