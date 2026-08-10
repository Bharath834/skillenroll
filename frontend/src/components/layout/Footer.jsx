import { Link } from 'react-router-dom';
import logo from '../../assets/logo.svg';
import { ROUTES } from '../../utils/constants.js';
import './Footer.css';

const footerColumns = [
  {
    title: 'Platform',
    links: [
      { label: 'Home', to: ROUTES.home },
      { label: 'Browse Courses', to: ROUTES.courses },
      { label: 'My Enrollments', to: ROUTES.myEnrollments },
      { label: 'My Progress', to: ROUTES.progress },
    ],
  },
  {
    title: 'Account',
    links: [
      { label: 'Log in', to: ROUTES.login },
      { label: 'Create account', to: ROUTES.register },
    ],
  },
];

export default function Footer() {
  const year = new Date().getFullYear();

  return (
    <footer className="footer">
      <div className="container footer-grid">
        <div className="footer-brand">
          <div className="footer-brand-row">
            <img src={logo} alt="" className="footer-logo" width="34" height="34" />
            <span className="footer-brand-name">SkillEnroll</span>
          </div>
          <p className="footer-tagline">
            Discover skill programs, enroll with one click, and track your
            learning progress — all in one place.
          </p>
        </div>

        {footerColumns.map((column) => (
          <div key={column.title} className="footer-column">
            <h3 className="footer-heading">{column.title}</h3>
            <ul className="footer-links">
              {column.links.map((link) => (
                <li key={link.to}>
                  <Link to={link.to}>{link.label}</Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      <div className="container footer-bottom">
        <span>© {year} SkillEnroll. All rights reserved.</span>
        <span className="footer-made">Built with React · Spring Boot · MySQL</span>
      </div>
    </footer>
  );
}
