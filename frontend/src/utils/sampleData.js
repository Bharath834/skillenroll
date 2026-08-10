/**
 * SAMPLE DATA — placeholders only.
 *
 * Used by the Courses and Course Details pages to render realistic UI before
 * the catalog API integration lands (next phase). Fields roughly follow the
 * backend CourseResponse DTO; extra display fields (skillLevel, lessonsCount,
 * rating, syllabus) are UI conveniences.
 *
 * Once the catalog is wired to the real API, delete this file and its imports.
 */
export const SAMPLE_COURSES = [
  {
    id: 1,
    title: 'Introduction to Web Development',
    description:
      'Learn the fundamentals of the web: HTML, CSS, and JavaScript. Build and style your first responsive pages from scratch with hands-on exercises and real-world projects.',
    category: 'Web Development',
    price: 0,
    duration: 24,
    instructorName: 'Jane Smith',
    skillLevel: 'BEGINNER',
    lessonsCount: 28,
    rating: 4.8,
    syllabus: [
      { title: 'Getting Started', lessons: 5 },
      { title: 'HTML Fundamentals', lessons: 6 },
      { title: 'CSS Styling & Layout', lessons: 8 },
      { title: 'JavaScript Essentials', lessons: 7 },
      { title: 'Capstone Project', lessons: 2 },
    ],
  },
  {
    id: 2,
    title: 'Spring Boot Masterclass',
    description:
      'Build production-ready REST APIs with Spring Boot 3, Spring Security, JPA, and JWT authentication. Covers testing, deployment, and best practices.',
    category: 'Backend Development',
    price: 89,
    duration: 32,
    instructorName: 'Bharath Kumar',
    skillLevel: 'INTERMEDIATE',
    lessonsCount: 42,
    rating: 4.9,
    syllabus: [
      { title: 'Spring Boot Fundamentals', lessons: 8 },
      { title: 'REST API Design', lessons: 10 },
      { title: 'Spring Security & JWT', lessons: 9 },
      { title: 'Persistence with JPA', lessons: 9 },
      { title: 'Testing & Deployment', lessons: 6 },
    ],
  },
  {
    id: 3,
    title: 'Data Science with Python',
    description:
      'Master data analysis and visualization with Python: NumPy, Pandas, Matplotlib, and an introduction to machine learning with scikit-learn.',
    category: 'Data Science',
    price: 59,
    duration: 28,
    instructorName: 'Alice Johnson',
    skillLevel: 'INTERMEDIATE',
    lessonsCount: 36,
    rating: 4.7,
    syllabus: [
      { title: 'Python for Data', lessons: 7 },
      { title: 'NumPy & Pandas', lessons: 10 },
      { title: 'Data Visualization', lessons: 8 },
      { title: 'Intro to Machine Learning', lessons: 8 },
      { title: 'Final Project', lessons: 3 },
    ],
  },
  {
    id: 4,
    title: 'UI/UX Design Essentials',
    description:
      'Learn the principles of great user experience: research, wireframing, prototyping, and visual design using modern design tools and methodologies.',
    category: 'Design',
    price: 45,
    duration: 18,
    instructorName: 'Carlos Mendez',
    skillLevel: 'BEGINNER',
    lessonsCount: 22,
    rating: 4.6,
    syllabus: [
      { title: 'Design Thinking', lessons: 5 },
      { title: 'User Research', lessons: 4 },
      { title: 'Wireframing & Prototyping', lessons: 7 },
      { title: 'Visual Design', lessons: 6 },
    ],
  },
  {
    id: 5,
    title: 'Cloud Engineering with AWS',
    description:
      'Deploy and scale applications on AWS: EC2, S3, RDS, Lambda, and infrastructure as code. Includes a hands-on capstone deployment project.',
    category: 'Cloud Computing',
    price: 99,
    duration: 35,
    instructorName: 'Priya Sharma',
    skillLevel: 'ADVANCED',
    lessonsCount: 40,
    rating: 4.8,
    syllabus: [
      { title: 'AWS Foundations', lessons: 8 },
      { title: 'Compute & Storage', lessons: 10 },
      { title: 'Databases & Networking', lessons: 9 },
      { title: 'Serverless & IaC', lessons: 8 },
      { title: 'Capstone Deployment', lessons: 5 },
    ],
  },
  {
    id: 6,
    title: 'DevOps & CI/CD Pipelines',
    description:
      'Automate the software delivery lifecycle with Git, Docker, Kubernetes, and GitHub Actions. Learn monitoring, logging, and release strategies.',
    category: 'DevOps',
    price: 79,
    duration: 30,
    instructorName: 'Tom Becker',
    skillLevel: 'ADVANCED',
    lessonsCount: 34,
    rating: 4.5,
    syllabus: [
      { title: 'Containers & Docker', lessons: 8 },
      { title: 'CI/CD with GitHub Actions', lessons: 9 },
      { title: 'Kubernetes Basics', lessons: 9 },
      { title: 'Monitoring & Logging', lessons: 5 },
      { title: 'Release Strategies', lessons: 3 },
    ],
  },
];

/** Find a sample course by numeric id (used by the Course Details placeholder). */
export function getSampleCourse(id) {
  const numericId = Number(id);
  return SAMPLE_COURSES.find((course) => course.id === numericId) || null;
}

export default SAMPLE_COURSES;
