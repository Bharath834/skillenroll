import PageHeader from '../components/common/PageHeader.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import Button from '../components/common/Button.jsx';
import Alert from '../components/common/Alert.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { ROUTES } from '../utils/constants.js';
import './Progress.css';

/**
 * "My Progress" placeholder.
 * Will show per-course completion percentages, timelines, and an overview
 * dashboard (GET /api/progress) once the progress API is wired.
 */
export default function Progress() {
  useDocumentTitle('My Progress');

  return (
    <>
      <PageHeader
        title="My Progress"
        subtitle="Track completion percentages across all your programs and see how far you've come."
      />

      <section className="section-sm">
        <div className="container">
          <div className="card">
            <EmptyState
              icon={
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M3 20h18" />
                  <path d="M6 20v-6M12 20V8M18 20V4" />
                </svg>
              }
              title="Your progress dashboard is on its way"
              description="Once you're enrolled in courses, this page will show your completion percentage for each one, powered by the progress API."
            >
              <Button to={ROUTES.courses}>Browse courses</Button>
            </EmptyState>
          </div>

          <div className="progress-note">
            <Alert variant="info">
              Progress data will be loaded from the backend (GET{' '}
              <code>/api/progress</code>) in the progress integration phase.
            </Alert>
          </div>
        </div>
      </section>
    </>
  );
}
