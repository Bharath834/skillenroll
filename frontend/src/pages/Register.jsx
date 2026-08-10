import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import Button from '../components/common/Button.jsx';
import Alert from '../components/common/Alert.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { useAuth } from '../context/AuthContext.jsx';
import { ROUTES } from '../utils/constants.js';
import { getAuthErrorMessage } from '../utils/errors.js';
import './auth.css';

const initialForm = {
  firstName: '',
  lastName: '',
  email: '',
  phoneNumber: '',
  password: '',
  confirmPassword: '',
};

const PHONE_PATTERN = /^\+?[0-9]{10,15}$/;
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Registration page — submits to the existing backend endpoint
 * POST /api/auth/register (RegisterRequest: firstName, lastName, email,
 * phoneNumber, password). Public registration always creates a STUDENT
 * account. The backend returns a JwtResponse, so a successful registration
 * automatically logs the user in and redirects Home.
 */
export default function Register() {
  useDocumentTitle('Create account');

  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [loading, setLoading] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();

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
    if (!form.firstName.trim()) {
      next.firstName = 'First name is required.';
    } else if (form.firstName.trim().length > 50) {
      next.firstName = 'First name must not exceed 50 characters.';
    }
    if (!form.lastName.trim()) {
      next.lastName = 'Last name is required.';
    } else if (form.lastName.trim().length > 50) {
      next.lastName = 'Last name must not exceed 50 characters.';
    }
    if (!form.email.trim()) {
      next.email = 'Email is required.';
    } else if (!EMAIL_PATTERN.test(form.email)) {
      next.email = 'Please enter a valid email address.';
    }
    if (!form.phoneNumber.trim()) {
      next.phoneNumber = 'Phone number is required.';
    } else if (!PHONE_PATTERN.test(form.phoneNumber.trim())) {
      next.phoneNumber = 'Enter 10–15 digits; an optional leading + is allowed.';
    }
    if (form.password.length < 8) {
      next.password = 'Password must be at least 8 characters.';
    }
    if (form.confirmPassword !== form.password) {
      next.confirmPassword = 'Passwords do not match.';
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
      // Exact RegisterRequest payload the backend expects.
      await register({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        phoneNumber: form.phoneNumber.trim(),
        password: form.password,
      });
      navigate(ROUTES.home, { replace: true });
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
          <h1 className="auth-title">Create your account</h1>
          <p className="auth-sub">
            Join SkillEnroll as a Student and start building your skills today.
          </p>
        </div>

        {submitError ? <Alert variant="danger">{submitError}</Alert> : null}

        <form onSubmit={handleSubmit} noValidate>
          <div className="form-group">
            <label className="form-label" htmlFor="reg-first-name">
              First name
            </label>
            <input
              id="reg-first-name"
              name="firstName"
              type="text"
              className="form-control"
              placeholder="Jane"
              autoComplete="given-name"
              value={form.firstName}
              onChange={handleChange}
              disabled={loading}
            />
            {errors.firstName ? <p className="form-error">{errors.firstName}</p> : null}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reg-last-name">
              Last name
            </label>
            <input
              id="reg-last-name"
              name="lastName"
              type="text"
              className="form-control"
              placeholder="Doe"
              autoComplete="family-name"
              value={form.lastName}
              onChange={handleChange}
              disabled={loading}
            />
            {errors.lastName ? <p className="form-error">{errors.lastName}</p> : null}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reg-email">
              Email
            </label>
            <input
              id="reg-email"
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
            <label className="form-label" htmlFor="reg-phone">
              Phone number
            </label>
            <input
              id="reg-phone"
              name="phoneNumber"
              type="tel"
              className="form-control"
              placeholder="+1234567890"
              autoComplete="tel"
              value={form.phoneNumber}
              onChange={handleChange}
              disabled={loading}
            />
            <p className="form-hint">10–15 digits; an optional leading + is allowed.</p>
            {errors.phoneNumber ? <p className="form-error">{errors.phoneNumber}</p> : null}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reg-password">
              Password
            </label>
            <input
              id="reg-password"
              name="password"
              type="password"
              className="form-control"
              placeholder="At least 8 characters"
              autoComplete="new-password"
              value={form.password}
              onChange={handleChange}
              disabled={loading}
            />
            <p className="form-hint">Use at least 8 characters (max 100).</p>
            {errors.password ? <p className="form-error">{errors.password}</p> : null}
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="reg-confirm">
              Confirm password
            </label>
            <input
              id="reg-confirm"
              name="confirmPassword"
              type="password"
              className="form-control"
              placeholder="Repeat your password"
              autoComplete="new-password"
              value={form.confirmPassword}
              onChange={handleChange}
              disabled={loading}
            />
            {errors.confirmPassword ? (
              <p className="form-error">{errors.confirmPassword}</p>
            ) : null}
          </div>

          <Button type="submit" block size="lg" disabled={loading}>
            {loading ? 'Creating account…' : 'Create account'}
          </Button>
        </form>

        <p className="auth-switch">
          Already have an account? <Link to={ROUTES.login}>Log in</Link>
        </p>
      </div>
    </div>
  );
}
