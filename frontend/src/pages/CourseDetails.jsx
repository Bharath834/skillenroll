import { useEffect, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import Badge from '../components/common/Badge.jsx';
import Button from '../components/common/Button.jsx';
import Alert from '../components/common/Alert.jsx';
import EmptyState from '../components/common/EmptyState.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { useAuth } from '../context/AuthContext.jsx';
import { courseApi } from '../services/courseService.js';
import { paymentApi, loadRazorpayCheckout } from '../services/paymentService.js';
import { ROUTES } from '../utils/constants.js';
import { formatDuration, formatMinutes, formatPrice, pluralize } from '../utils/formatters.js';
import { getApiErrorMessage } from '../utils/errors.js';
import './CourseDetails.css';

const LOCK_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <rect x="5" y="11" width="14" height="9" rx="2" />
    <path d="M8 11V8a4 4 0 0 1 8 0v3" />
  </svg>
);

const SEARCH_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <circle cx="11" cy="11" r="7" />
    <path d="m21 21-4.35-4.35" />
  </svg>
);

const ERROR_ICON = (
  <svg viewBox="0 0 24 24" aria-hidden="true">
    <circle cx="12" cy="12" r="9" />
    <path d="M12 8v5" />
    <path d="M12 16.5h.01" />
  </svg>
);

// sessionStorage key (per course) for a captured-but-unconfirmed payment.
// Survives reloads so the learner can confirm the SAME payment (no double
// charge) even after navigating away or refreshing the page.
const PENDING_PAYMENT_STORAGE_KEY = (courseId) => `skillenroll:pending-payment:${courseId}`;

/**
 * Course details. Fetches the real course (GET /api/courses/{id}) and its
 * curriculum (GET /api/lessons/course/{courseId}) and lets an authenticated
 * learner enroll (POST /api/enrollments). All of these endpoints require a
 * valid JWT, so anonymous visitors see a sign-in prompt.
 */
export default function CourseDetails() {
  const { id } = useParams();
  const { isAuthenticated, user } = useAuth();
  const location = useLocation();

  const [reloadKey, setReloadKey] = useState(0);

  const [course, setCourse] = useState(null);
  const [courseError, setCourseError] = useState(null);
  const [courseLoading, setCourseLoading] = useState(false);

  const [lessons, setLessons] = useState(null); // null until loaded
  const [lessonsLoading, setLessonsLoading] = useState(false);
  const [lessonsError, setLessonsError] = useState(null);

  const [enrolled, setEnrolled] = useState(false);
  // Enrollment lifecycle status when a record exists: 'ACTIVE' | 'PENDING' | null.
  // Paid courses enroll as PENDING and are activated after payment verification.
  const [enrollmentStatus, setEnrollmentStatus] = useState(null);
  const [enrollState, setEnrollState] = useState('idle'); // idle | submitting | checkout | verifying | success | error
  const [enrollMessage, setEnrollMessage] = useState('');
  // Set when a payment may have been captured but verification failed — the
  // learner can then re-confirm without creating a new order (no double charge).
  const [pendingOrderId, setPendingOrderId] = useState(null);
  // Razorpay payment id + signature captured by the checkout handler, kept so
  // the SAME /verify payload can be re-sent after a reload instead of paying
  // again. Null when only the order id is known (server-side re-verify).
  const [pendingSignature, setPendingSignature] = useState(null);

  useDocumentTitle(course ? course.title : courseError ? 'Course not available' : 'Course');

  // Load the course (and reset dependent state) whenever the id, the auth
  // state, or the reload key changes.
  useEffect(() => {
    if (!isAuthenticated) {
      setCourse(null);
      setCourseError(null);
      setCourseLoading(false);
      setLessons(null);
      setLessonsLoading(false);
      setLessonsError(null);
      setEnrolled(false);
      setEnrollmentStatus(null);
      setEnrollState('idle');
      setEnrollMessage('');
      setPendingOrderId(null);
      setPendingSignature(null);
      return undefined;
    }

    let cancelled = false;
    setCourseLoading(true);
    setCourseError(null);
    setCourse(null);
    setLessons(null);
    setLessonsError(null);
    setEnrolled(false);
    setEnrollmentStatus(null);
    setEnrollState('idle');
    setEnrollMessage('');
    setPendingOrderId(null);
    setPendingSignature(null);

    courseApi
      .getCourseById(id)
      .then((data) => {
        if (cancelled) return;
        setCourse(data);
      })
      .catch((err) => {
        if (cancelled) return;
        setCourseError(err);
      })
      .finally(() => {
        if (!cancelled) setCourseLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [id, isAuthenticated, reloadKey]);

  // Fetch the curriculum once the course is loaded.
  useEffect(() => {
    if (!course) return undefined;

    let cancelled = false;
    setLessonsLoading(true);
    setLessonsError(null);
    courseApi
      .getLessonsByCourse(course.id)
      .then((page) => {
        if (cancelled) return;
        setLessons(Array.isArray(page?.content) ? page.content : []);
      })
      .catch((err) => {
        if (cancelled) return;
        setLessonsError(err);
      })
      .finally(() => {
        if (!cancelled) setLessonsLoading(false);
      });

    return () => {
      cancelled = true;
    };
  }, [course]);

  // Pre-check whether the current user is already enrolled (non-blocking:
  // the Enroll action surfaces a 409 if this check is unavailable).
  useEffect(() => {
    if (!course || !user) return undefined;

    let cancelled = false;
    courseApi
      .getEnrollmentsForUserAndCourse(user.id, course.id)
      .then((page) => {
        if (cancelled) return;
        const enrollment = Array.isArray(page?.content) && page.content.length > 0 ? page.content[0] : null;
        if (enrollment) {
          setEnrolled(true);
          setEnrollmentStatus(enrollment.status ?? null);
        }
      })
      .catch(() => {
        // Ignored — the enroll action handles the already-enrolled case.
      });

    return () => {
      cancelled = true;
    };
  }, [course, user]);

  // Restore a captured-but-unconfirmed payment for this course across reloads
  // and navigation, so the learner can confirm it WITHOUT paying again.
  useEffect(() => {
    if (!course || !isAuthenticated) return undefined;

    let raw = null;
    try {
      raw = sessionStorage.getItem(PENDING_PAYMENT_STORAGE_KEY(course.id));
    } catch {
      return undefined;
    }
    if (!raw) return undefined;
    try {
      const saved = JSON.parse(raw);
      if (saved?.orderId) {
        setPendingOrderId(saved.orderId);
        setPendingSignature(
          saved.paymentId && saved.signature
            ? { paymentId: saved.paymentId, signature: saved.signature }
            : null
        );
        setEnrollState('error');
        setEnrollMessage(
          'Your payment may have been captured — confirm it to finish enrolling without paying again.'
        );
      }
    } catch {
      // Malformed storage — ignore.
    }
    return undefined;
  }, [course, isAuthenticated]);

  // A paid course needs a completed Razorpay payment before access is active.
  const isPaidCourse = Number(course?.price) > 0;
  // Free courses treat any enrollment as active; paid ones only when ACTIVE.
  const enrolledActive = enrolled && (enrollmentStatus === 'ACTIVE' || !isPaidCourse);
  // Paid course, enrolled but not yet paid → offer to (re)open the checkout.
  const needsPayment = enrolled && enrollmentStatus === 'PENDING' && isPaidCourse;
  const enrollBusy =
    enrollState === 'submitting' || enrollState === 'checkout' || enrollState === 'verifying';

  /** Persists a captured-but-unconfirmed payment so it survives reloads. */
  const savePendingPayment = (orderId, paymentId, signature) => {
    try {
      sessionStorage.setItem(
        PENDING_PAYMENT_STORAGE_KEY(course.id),
        JSON.stringify({ orderId, paymentId, signature })
      );
    } catch {
      // Storage unavailable — the in-memory state still covers this session.
    }
  };

  /** Clears the in-memory + persisted pending payment reference. */
  const clearPendingPayment = () => {
    setPendingOrderId(null);
    setPendingSignature(null);
    try {
      sessionStorage.removeItem(PENDING_PAYMENT_STORAGE_KEY(course.id));
    } catch {
      // Ignore.
    }
  };

  const handleEnroll = async () => {
    if (!user || !course || enrollBusy) return;

    setEnrollState('submitting');
    setEnrollMessage('');
    try {
      // 1. Make sure a PENDING enrollment exists (409 = already enrolled is fine).
      if (!enrolled) {
        try {
          await courseApi.enroll({ userId: user.id, courseId: course.id });
        } catch (err) {
          if (err.status !== 409) throw err;
        }
        setEnrolled(true);
        setEnrollmentStatus((status) => status || 'PENDING');
      }

      // 2. Free course → the enrollment is all that is needed.
      if (!isPaidCourse) {
        setEnrollState('success');
        setEnrollMessage('You are enrolled. Start learning whenever you are ready.');
        return;
      }

      // 3. Paid course → Razorpay checkout, then server-side verification.
      await startCheckout();
    } catch (err) {
      setEnrollState('error');
      setEnrollMessage(
        getApiErrorMessage(err, 'We could not complete the enrollment. Please try again.')
      );
    }
  };

  /**
   * Opens the Razorpay checkout for the current course:
   * 1. POST /api/payment/create-order → order (amount derived server-side).
   * 2. Load Checkout.js (cached after the first load).
   * 3. Open the modal with order_id + key; on success POST /api/payment/verify
   *    with the Razorpay ids + signature to activate the enrollment.
   */
  const startCheckout = async () => {
    // 1. Create the Razorpay order (amount is never taken from the client).
    const order = await paymentApi.createOrder(course.id);

    // 2. Load the checkout script (no-op after the first successful load).
    const Razorpay = await loadRazorpayCheckout();

    // 3. Open the payment modal.
    const options = {
      key: order.keyId,
      amount: order.amount, // paise
      currency: order.currency || 'INR',
      name: 'SkillEnroll',
      description: course.title,
      order_id: order.orderId,
      prefill: {
        name: [user.firstName, user.lastName].filter(Boolean).join(' ') || 'SkillEnroll learner',
        email: user.email || '',
        contact: user.phoneNumber || '',
      },
      theme: { color: '#4f46e5' },
      modal: {
        ondismiss: () =>
          setEnrollState((state) => (state === 'checkout' ? 'idle' : state)),
      },
      handler: async (response) => {
        setEnrollState('verifying');
        try {
          const verification = await paymentApi.verifyPayment({
            orderId: response.razorpay_order_id,
            paymentId: response.razorpay_payment_id,
            signature: response.razorpay_signature,
            courseId: course.id,
          });
          // Trust the backend: it only reports ACTIVE when the enrollment was
          // actually activated (PENDING -> ACTIVE).
          const activated = verification.enrollmentStatus === 'ACTIVE';
          setEnrollmentStatus(verification.enrollmentStatus ?? null);
          if (activated) clearPendingPayment();
          setEnrollState(activated ? 'success' : 'error');
          setEnrollMessage(
            activated
              ? 'Payment successful — you are enrolled. Start learning whenever you are ready.'
              : 'Payment successful, but your enrollment could not be activated yet. Please try again shortly.'
          );
        } catch (verifyError) {
          // The payment may have been captured even if confirmation failed
          // (e.g. a transient error). Keep the order id AND the Razorpay
          // signature so the learner can re-confirm the SAME payment — never
          // auto-create a new order.
          const unconfirmedOrderId = response.razorpay_order_id || order.orderId;
          setPendingOrderId(unconfirmedOrderId);
          setPendingSignature({
            paymentId: response.razorpay_payment_id,
            signature: response.razorpay_signature,
          });
          savePendingPayment(
            unconfirmedOrderId,
            response.razorpay_payment_id,
            response.razorpay_signature
          );
          setEnrollState('error');
          setEnrollMessage(
            getApiErrorMessage(
              verifyError,
              'Payment received, but we could not confirm it yet. Please try again shortly.'
            )
          );
        }
      },
    };

    const razorpay = new Razorpay(options);
    razorpay.on('payment.failed', () => {
      // The payment definitively failed — no confirmation retry is offered.
      clearPendingPayment();
      setEnrollState('error');
      setEnrollMessage('Payment failed. You can try again whenever you are ready.');
    });
    razorpay.open();
    setEnrollState('checkout');
  };

  /**
   * Re-confirms a previously captured payment. When the Razorpay signature was
   * captured, the SAME /verify payload is re-sent (idempotent, no new order);
   * otherwise the server re-verifies from its ledger via /re-verify. A 4xx
   * means no verified payment is on record — fall back to a fresh checkout.
   */
  const handleConfirmPayment = async () => {
    if (!user || !course || enrollBusy || !pendingOrderId) return;

    setEnrollState('verifying');
    setEnrollMessage('');
    try {
      const verification = pendingSignature
        ? await paymentApi.verifyPayment({
            orderId: pendingOrderId,
            paymentId: pendingSignature.paymentId,
            signature: pendingSignature.signature,
            courseId: course.id,
          })
        : await paymentApi.reVerifyPayment({
            orderId: pendingOrderId,
            courseId: course.id,
          });
      const activated = verification.enrollmentStatus === 'ACTIVE';
      setEnrollmentStatus(verification.enrollmentStatus ?? null);
      if (activated) clearPendingPayment();
      setEnrollState(activated ? 'success' : 'error');
      setEnrollMessage(
        activated
          ? 'Payment confirmed — you are enrolled. Start learning whenever you are ready.'
          : 'Payment confirmed, but your enrollment could not be activated yet. Please try again shortly.'
      );
    } catch (confirmError) {
      const status = confirmError.status;
      if (status && status >= 400 && status < 500) {
        // Deterministic failure (e.g. 409 no verified payment, 400 order not
        // found or signature invalid): there is nothing more to confirm — let
        // the learner pay fresh.
        clearPendingPayment();
        setEnrollState('error');
        setEnrollMessage(
          confirmError.message || 'No verified payment was found. Please complete a new payment.'
        );
      } else {
        // Transient (5xx / network): keep the order id so they can retry the
        // confirmation without paying again.
        setEnrollState('error');
        setEnrollMessage(
          getApiErrorMessage(confirmError, 'We could not confirm the payment. Please try again shortly.')
        );
      }
    }
  };

  if (!isAuthenticated) {
    return (
      <section className="section-sm">
        <div className="container">
          <div className="card">
            <EmptyState
              icon={LOCK_ICON}
              title="Sign in to view course details"
              description="The catalog requires a signed-in account. Log in to browse courses and enroll."
            >
              <Button to={ROUTES.login} state={{ from: location }}>
                Log in
              </Button>
              <Button to={ROUTES.register} variant="outline">
                Create account
              </Button>
            </EmptyState>
          </div>
        </div>
      </section>
    );
  }

  if (courseLoading) {
    return (
      <section className="section-sm">
        <div className="container">
          <div className="card">
            <div className="page-loading" role="status">
              <span className="spinner spinner-dark" aria-hidden="true" />
              Loading course…
            </div>
          </div>
        </div>
      </section>
    );
  }

  if (courseError) {
    const isNotFound = courseError.status === 404;
    return (
      <section className="section-sm">
        <div className="container">
          <div className="card">
            <EmptyState
              role="alert"
              icon={isNotFound ? SEARCH_ICON : ERROR_ICON}
              title={isNotFound ? 'Course not found' : "Couldn't load this course"}
              description={
                isNotFound
                  ? `We couldn't find a course with id “${id}”. It may have been removed.`
                  : getApiErrorMessage(courseError)
              }
            >
              <Button to={ROUTES.courses} variant="outline">
                Back to courses
              </Button>
              {!isNotFound ? (
                <Button onClick={() => setReloadKey((key) => key + 1)}>Try again</Button>
              ) : null}
            </EmptyState>
          </div>
        </div>
      </section>
    );
  }

  if (!course) return null;

  const lessonCount = Array.isArray(lessons) ? lessons.length : null;

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
              </div>
              <h1>{course.title}</h1>

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
                {lessonCount !== null ? (
                  <span className="course-detail-meta-item">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M4 5h16v14H4z" />
                      <path d="M8 9h8M8 13h5" />
                    </svg>
                    {pluralize(lessonCount, 'lesson')}
                  </span>
                ) : null}
              </div>
            </div>

            <div className="course-detail-body">
              <h2>About this course</h2>
              <p>
                {course.description ||
                  'No description has been provided for this course yet.'}
              </p>

              <h2>Course curriculum</h2>
              {lessonsLoading ? (
                <p className="course-detail-curriculum-note">Loading curriculum…</p>
              ) : lessonsError ? (
                <p className="course-detail-curriculum-note">
                  The curriculum could not be loaded right now.
                </p>
              ) : Array.isArray(lessons) && lessons.length > 0 ? (
                <ol className="course-curriculum">
                  {lessons.map((lesson) => (
                    <li key={lesson.id} className="course-lesson">
                      <div className="course-lesson-info">
                        <span className="course-lesson-index">
                          {String(lesson.lessonOrder ?? '—').padStart(2, '0')}
                        </span>
                        <div>
                          <h3>{lesson.title}</h3>
                          {lesson.description ? (
                            <p className="course-lesson-desc">{lesson.description}</p>
                          ) : null}
                        </div>
                      </div>
                      <span className="course-lesson-duration">
                        {formatMinutes(lesson.durationMinutes)}
                      </span>
                    </li>
                  ))}
                </ol>
              ) : (
                <p className="course-detail-curriculum-note">
                  No lessons have been added to this course yet.
                </p>
              )}
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

            <Button
              block
              size="lg"
              onClick={pendingOrderId ? handleConfirmPayment : handleEnroll}
              disabled={enrolledActive || enrollBusy}
            >
              {enrolledActive
                ? 'Enrolled'
                : enrollState === 'submitting'
                  ? 'Enrolling…'
                  : enrollState === 'checkout'
                    ? 'Payment…'
                    : enrollState === 'verifying'
                      ? 'Confirming…'
                      : pendingOrderId
                        ? 'Confirm payment'
                        : needsPayment
                          ? 'Complete payment'
                          : 'Enroll now'}
            </Button>
            <p className="course-detail-card-note">
              {enrolledActive
                ? 'You are enrolled in this course.'
                : pendingOrderId
                  ? 'Your payment may have been captured — confirm it to finish enrolling without paying again.'
                  : needsPayment
                    ? 'Complete your payment to activate access.'
                    : 'Enroll in one click and start learning immediately.'}
            </p>

            {enrollState === 'success' ? (
              <Alert variant="success" className="course-detail-alert">
                {enrollMessage}
              </Alert>
            ) : null}
            {enrollState === 'error' ? (
              <Alert variant="danger" className="course-detail-alert">
                {enrollMessage}
              </Alert>
            ) : null}

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
                <span>Duration</span>
                <strong>{formatDuration(course.duration)}</strong>
              </li>
              {lessonCount !== null ? (
                <li>
                  <span>Lessons</span>
                  <strong>{pluralize(lessonCount, 'lesson')}</strong>
                </li>
              ) : null}
            </ul>
          </div>
        </aside>
      </section>
    </>
  );
}
