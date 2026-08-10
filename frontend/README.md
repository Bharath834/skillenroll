# SkillEnroll Frontend

React (Vite) single-page application for the SkillEnroll learning platform.

## Prerequisites

- Node.js 18+ (built and tested with Node 24 / npm 12)
- The Spring Boot backend running on `http://localhost:8080` (see repo `backend/`)

## Getting started

```bash
npm install
npm run dev        # http://localhost:5173
```

## Scripts

| Command           | Description                          |
| ----------------- | ------------------------------------ |
| `npm run dev`     | Start the Vite dev server (port 5173) |
| `npm run build`   | Production build to `dist/`           |
| `npm run preview` | Preview the production build locally  |

## Configuration

Copy `.env.example` to `.env` and set `VITE_API_BASE_URL` to point at the
backend (default `http://localhost:8080/api`). All API configuration is
centralized in `src/config.js` and consumed through `src/services/apiClient.js`
— components never hardcode backend URLs.

## Project structure

```
src/
├── components/
│   ├── common/        # Button, Badge, PageHeader, EmptyState, Alert
│   └── layout/        # Layout, Navbar, Footer
├── pages/             # Route-level pages (Home, Login, Register, Courses, ...)
├── services/          # Centralized API client + endpoint paths
├── context/           # React context providers (populated with auth, Day 8+)
├── hooks/             # Custom hooks (useDocumentTitle, ...)
├── utils/             # Constants, formatters, sample data
├── assets/            # Static assets (logo.svg)
├── App.jsx            # Route definitions
└── main.jsx           # Entry point
```

> Status: authentication (login / register / logout via the existing JWT API) is
> implemented. Catalog, enrollment, and progress pages still render UI shells
> with sample data — live API integration lands in later phases.
