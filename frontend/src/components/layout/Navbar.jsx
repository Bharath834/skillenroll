import { useEffect, useMemo, useRef, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import logo from '../../assets/logo.svg';
import { useAuth } from '../../context/AuthContext.jsx';
import { ROUTES } from '../../utils/constants.js';
import './Navbar.css';

/**
 * Top navigation bar with brand, auth-aware page links, user actions, and a
 * responsive mobile menu.
 *
 * Logged out: Home · Courses · Log in · Sign up
 * Logged in:  Home · Courses · My Enrollments · Progress · <avatar> Log out
 */
export default function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false);
  const navRef = useRef(null);

  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();

  const closeMenu = () => setMenuOpen(false);

  const navLinks = useMemo(
    () => [
      { to: ROUTES.home, label: 'Home' },
      { to: ROUTES.courses, label: 'Courses' },
      ...(isAuthenticated
        ? [
            { to: ROUTES.myEnrollments, label: 'My Enrollments' },
            { to: ROUTES.progress, label: 'Progress' },
          ]
        : []),
    ],
    [isAuthenticated]
  );

  const initials = useMemo(() => {
    const parts = [user?.firstName, user?.lastName].filter(Boolean);
    if (parts.length === 0) return '?';
    return parts
      .map((part) => part[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  }, [user]);

  // Close the mobile menu on Escape or when clicking outside it.
  useEffect(() => {
    if (!menuOpen) return undefined;

    const onKeyDown = (event) => {
      if (event.key === 'Escape') closeMenu();
    };
    const onOutsideClick = (event) => {
      if (navRef.current && !navRef.current.contains(event.target)) closeMenu();
    };

    document.addEventListener('keydown', onKeyDown);
    document.addEventListener('click', onOutsideClick);
    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.removeEventListener('click', onOutsideClick);
    };
  }, [menuOpen]);

  const handleLogout = async () => {
    closeMenu();
    await logout(); // best-effort server revoke; local state is always cleared
    navigate(ROUTES.home, { replace: true });
  };

  return (
    <header className="navbar">
      <div className="container navbar-inner">
        <Link to={ROUTES.home} className="navbar-brand" onClick={closeMenu}>
          <img src={logo} alt="" className="navbar-logo" width="34" height="34" />
          <span className="navbar-brand-text">
            Skill<span>Enroll</span>
          </span>
        </Link>

        <nav
          id="main-nav"
          ref={navRef}
          className={`navbar-nav ${menuOpen ? 'is-open' : ''}`}
          aria-label="Main navigation"
        >
          {navLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) =>
                `navbar-link${isActive ? ' is-active' : ''}`
              }
              onClick={closeMenu}
            >
              {link.label}
            </NavLink>
          ))}

          <div className="navbar-actions">
            {isAuthenticated ? (
              <>
                <span
                  className="navbar-user"
                  title={user?.email || 'Signed in'}
                  aria-label={`Signed in as ${user?.firstName || 'user'}`}
                >
                  {initials}
                </span>
                <button type="button" className="btn btn-outline btn-sm" onClick={handleLogout}>
                  Log out
                </button>
              </>
            ) : (
              <>
                <Link to={ROUTES.login} className="btn btn-ghost btn-sm" onClick={closeMenu}>
                  Log in
                </Link>
                <Link to={ROUTES.register} className="btn btn-primary btn-sm" onClick={closeMenu}>
                  Sign up
                </Link>
              </>
            )}
          </div>
        </nav>

        <button
          type="button"
          className="navbar-toggle"
          aria-expanded={menuOpen}
          aria-controls="main-nav"
          aria-label="Toggle navigation menu"
          onClick={() => setMenuOpen((open) => !open)}
        >
          <span className="navbar-toggle-bar" />
          <span className="navbar-toggle-bar" />
          <span className="navbar-toggle-bar" />
        </button>
      </div>
    </header>
  );
}
