import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import Button from '../components/common/Button.jsx';
import Alert from '../components/common/Alert.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { useAuth } from '../context/AuthContext.jsx';
import { ROUTES } from '../utils/constants.js';
import { getAuthErrorMessage } from '../utils/errors.js';
import './auth.css';

/**
 * Login page — authenticates against the existing backend endpoint
 * POST /api/auth/login (LoginRequest: email + password). On success the JWT
 * is persisted, auth state updates, and the user is redirected (to the page
 * they originally requested, or Home).
 */
export default function Login() {
  useDocumentTitle('Log in');

  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [loading, setLoading] = useState(false);

  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Redirect target: the protected page the user tried to open, else Home.
  // Preserves any query string that was part of the originally-requested URL.
  const from = location.state?.from;
  const redirectTo = from ? from.pathname + (from.search || '') : ROUTES.home;

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[name];
        return next;
      });
    }
  };

  const validate = () => {
    const next = {};
    if (!form.email.trim()) {
      next.email = 'Email is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      next.email = 'Please enter a valid email address.';
    }
    if (!form.password) {
      next.password = 'Password is required.';
    }
    return next;
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = validate();
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setLoading(true);
    setSubmitError(null);
    try {
      await login(form.email, form.password);
      navigate(redirectTo, { replace: true });
    } catch (error) {
      setSubmitError(getAuthErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card card">
        <div className="auth-heading">
          <h1 className="auth-title">Welcome back</h1>
          <p className="auth-sub">Log in to continue your learning journey.</p>
        </div>

        {submitError ? <Alert variant="danger">{submitError}</Alert> : null}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="login-email">
              Email
            </label>
            <input
              id="login-email"
              name="email"
              type="email"
              className="form-control"
              placeholder="you@example.com"
              autoComplete="email"
              value={form.email}
              onChange={handleChange}
              disabled={loading}
            />
            {errors.email ? <p className="form-error">{errors.email}</p> : null}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="login-password">
              Password
            </label>
            <input
              id="login-password"
              name="password"
              type="password"
              className="form-control"
              placeholder="Your password"
              autoComplete="current-password"
              value={form.password}
              onChange={handleChange}
              disabled={loading}
            />
            {errors.password ? <p className="form-error">{errors.password}</p> : null}
          </div>

          <Button type="submit" block size="lg" disabled={loading}>
            {loading ? 'Logging in…' : 'Log in'}
          </Button>
        </form>

        <p className="auth-switch">
          Don&apos;t have an account? <Link to={ROUTES.register}>Sign up free</Link>
        </p>
      </div>
    </div>
  );
}
