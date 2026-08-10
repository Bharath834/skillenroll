import { Link } from 'react-router-dom';
import Button from '../components/common/Button.jsx';
import CourseCard from '../components/common/CourseCard.jsx';
import useDocumentTitle from '../hooks/useDocumentTitle.js';
import { ROUTES } from '../utils/constants.js';
import { SAMPLE_COURSES } from '../utils/sampleData.js';
import './Home.css';

const STATS = [
  { value: '120+', label: 'Skill programs' },
  { value: '5,000+', label: 'Active learners' },
  { value: '40+', label: 'Expert instructors' },
];

const FEATURES = [
  {
    title: 'Discover your next skill',
    description:
      'Browse a curated catalog of programs across web development, data science, cloud, design, and more.',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="11" cy="11" r="7" />
        <path d="m21 21-4.35-4.35" />
        <path d="M8 11h6M11 8v6" />
      </svg>
    ),
  },
  {
    title: 'Enroll in one click',
    description:
      'No lengthy checkout flows. Pick a program that fits your goals and start learning immediately.',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M20 6 9 17l-5-5" />
        <path d="M12 3l7 2v6.5c0 4.5-3 7.5-7 9-4-1.5-7-4.5-7-9V5z" />
      </svg>
    ),
  },
  {
    title: 'Track your progress',
    description:
      'See completion percentages across all your programs and stay motivated with a clear path forward.',
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M3 20h18" />
        <path d="M6 20v-6M12 20V8M18 20V4" />
      </svg>
    ),
  },
];

const STEPS = [
  { number: '01', title: 'Create your account', text: 'Sign up as a student or instructor in under a minute.' },
  { number: '02', title: 'Pick a program', text: 'Filter by category and skill level to find the right fit.' },
  { number: '03', title: 'Learn & track progress', text: 'Follow lessons and monitor your completion in real time.' },
];

export default function Home() {
  useDocumentTitle('Learn. Enroll. Grow.');

  return (
    <>
      {/* Hero */}
      <section className="home-hero">
        <div className="container home-hero-inner">
          <span className="home-hero-eyebrow">SkillEnroll · Skill development platform</span>
          <h1 className="home-hero-title">
            Learn new skills.
            <br />
            Track your <span className="home-hero-accent">growth</span>.
          </h1>
          <p className="home-hero-sub">
            Discover structured skill programs, enroll with a single click, and follow
            your progress from day one to completion — all in one place.
          </p>
          <div className="home-hero-actions">
            <Button to={ROUTES.courses} size="lg">
              Browse courses
            </Button>
            <Button to={ROUTES.register} variant="outline" size="lg" className="home-hero-btn-outline">
              Create free account
            </Button>
          </div>
          <dl className="home-hero-stats">
            {STATS.map((stat) => (
              <div key={stat.label} className="home-hero-stat">
                <dt>{stat.value}</dt>
                <dd>{stat.label}</dd>
              </div>
            ))}
          </dl>
        </div>
      </section>

      {/* Features */}
      <section className="section home-features">
        <div className="container">
          <div className="home-section-head">
            <span className="home-kicker">Why SkillEnroll</span>
            <h2>Everything you need to level up</h2>
          </div>
          <div className="grid grid-3">
            {FEATURES.map((feature) => (
              <div key={feature.title} className="card home-feature">
                <div className="home-feature-icon">{feature.icon}</div>
                <h3>{feature.title}</h3>
                <p>{feature.description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Featured courses (sample data until catalog integration) */}
      <section className="section home-featured">
        <div className="container">
          <div className="home-section-head">
            <span className="home-kicker">Featured programs</span>
            <h2>Popular right now</h2>
            <p className="home-section-note">
              Sample catalog shown until the course API is integrated.
            </p>
          </div>
          <div className="grid grid-3">
            {SAMPLE_COURSES.slice(0, 3).map((course) => (
              <CourseCard key={course.id} course={course} />
            ))}
          </div>
          <div className="home-see-all">
            <Button to={ROUTES.courses} variant="outline">
              View all courses
            </Button>
          </div>
        </div>
      </section>

      {/* How it works */}
      <section className="section home-steps">
        <div className="container">
          <div className="home-section-head">
            <span className="home-kicker">How it works</span>
            <h2>Start learning in three steps</h2>
          </div>
          <div className="grid grid-3">
            {STEPS.map((step) => (
              <div key={step.number} className="home-step">
                <span className="home-step-number">{step.number}</span>
                <h3>{step.title}</h3>
                <p>{step.text}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA band */}
      <section className="home-cta">
        <div className="container home-cta-inner">
          <h2>Ready to take the next step?</h2>
          <p>
            Join thousands of learners building in-demand skills with SkillEnroll.
          </p>
          <div className="home-cta-actions">
            <Button to={ROUTES.register} size="lg">
              Get started free
            </Button>
            <Link to={ROUTES.courses} className="home-cta-link">
              Explore the catalog
            </Link>
          </div>
        </div>
      </section>
    </>
  );
}
