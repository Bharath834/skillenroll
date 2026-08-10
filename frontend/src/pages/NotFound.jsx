import Button from '../components/common/Button.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { ROUTES } from '../utils/constants.js';
import './NotFound.css';

export default function NotFound() {
  useDocumentTitle('Page not found');

  return (
    <section className="not-found">
      <div className="container not-found-inner">
        <p className="not-found-code" aria-hidden="true">
          404
        </p>
        <h1>Page not found</h1>
        <p className="not-found-text">
          The page you&apos;re looking for doesn&apos;t exist or may have moved.
          Let&apos;s get you back on track.
        </p>
        <div className="not-found-actions">
          <Button to={ROUTES.home}>Back to home</Button>
          <Button to={ROUTES.courses} variant="outline">
            Browse courses
          </Button>
        </div>
      </div>
    </section>
  );
}
