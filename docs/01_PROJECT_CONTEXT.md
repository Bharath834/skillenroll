# 01_PROJECT_CONTEXT.md — SkillEnroll

> **Document Version:** 1.0  
> **Author:** Senior Software Architect  
> **Framework:** TrainingMug AI Development Framework  
> **Status:** ✅ Approved  

---

## 1. Problem Statement

Organizations and individuals struggle to manage skill development in a structured, trackable way. Existing solutions are either:
- Too rigid — enterprise LMS platforms that are over-engineered for small-to-medium use cases.
- Too shallow — simple todo-like trackers that lack curriculum design, enrollment workflows, and progress analytics.
- Too fragmented — spreadsheets, ad‑hoc tools, and manual processes that make it impossible to get a unified view of learner progress.

SkillEnroll solves this by providing a dedicated platform where **skill programs are designed, published, enrolled into, and tracked** — all in one place.

---

## 2. Business Objectives

| # | Objective | Success Metric |
|---|-----------|----------------|
| 1 | Provide a centralized catalog of skill programs/courses | ≥ 50 programs published within 6 months of launch |
| 2 | Streamline learner enrollment with an intuitive workflow | ≤ 3 clicks from discovery to enrollment |
| 3 | Deliver real‑time progress tracking for learners and admins | < 500 ms dashboard load time |
| 4 | Support role‑based access (Admin, Instructor, Learner) | Zero unauthorized-access incidents |
| 5 | Enable program creators to define curricula with modules, lessons, and assessments | Instructor NPS ≥ 40 after first quarter |
| 6 | Generate actionable completion & performance reports | Admin adoption rate ≥ 80 % within 3 months |

---

## 3. Target Users

| Persona | Description | Key Needs |
|---------|-------------|-----------|
| **Learner** | Individual enrolling in skill programs | Browse catalog, enroll, complete modules, view progress, receive certificates |
| **Instructor / Content Creator** | Subject-matter expert who designs and delivers programs | Create curricula, upload materials, set assessments, review learner submissions |
| **Admin / Manager** | Organization administrator overseeing skill development | Manage users & roles, approve programs, generate reports, configure system |
| **Guest (unauthenticated)** | Prospective learner exploring the platform | Browse public catalog, view program details, sign up / log in |

---

## 4. Technology Stack

### 4.1 Frontend

| Layer | Technology | Justification |
|-------|-----------|---------------|
| Language | **TypeScript** | Type safety, better DX, catches errors at compile time |
| UI Library | **React 18+** | Component-based, huge ecosystem, excellent for dynamic UIs |
| Styling | **Tailwind CSS** (or Material UI — TBD at sprint zero) | Utility-first for rapid prototyping / MUI for enterprise-grade component consistency |
| Build / Bundler | **Vite** | Fast HMR, quick builds, native ESM |
| State Management | **React Context + useReducer** (or Zustand if complexity grows) | Lightweight; avoid premature Redux |
| HTTP Client | **Axios** | Interceptors, request cancellation, better error handling |
| Deployment | **Vercel** | Optimized for React/Next.js, CI/CD with GitHub, global CDN |

### 4.2 Backend

| Layer | Technology | Justification |
|-------|-----------|---------------|
| Language | **Java 17+** | LTS, modern language features (records, sealed classes, pattern matching) |
| Framework | **Spring Boot 3** | Production-grade, autoconfiguration, mature ecosystem |
| Build Tool | **Maven** | Declarative builds, mature plugin ecosystem, widely adopted in Java ecosystem |
| ORM | **Spring Data JPA (Hibernate)** | Eliminates boilerplate data access code, rich query capabilities |
| Auth / Security | **Spring Security + JWT** | Battle-tested security framework; JWT for stateless authentication |
| API Documentation | **Swagger (OpenAPI 3)** | Auto-generated interactive API docs via springdoc-openapi |
| Validation | **Jakarta Bean Validation** | Declarative input validation with annotations |
| Testing | **JUnit 5 + Mockito** | Industry standard for unit & integration testing in Java |
| Deployment | **Azure App Service** | Managed Java runtime, auto-scaling, easy CI/CD integration |

### 4.3 Database

| Component | Technology | Justification |
|-----------|-----------|---------------|
| RDBMS | **MySQL 8** | Reliable, widely supported, great for structured educational data |
| Migration | **Flyway** (or Liquibase) | Version-controlled, repeatable schema migrations |
| Connection Pool | **HikariCP** | Default in Spring Boot 3, fastest pool for Java |

### 4.4 Infrastructure & DevOps

| Area | Tool | Details |
|------|------|---------|
| Version Control | **Git + GitHub** | Source code hosting, PR workflows, GitHub Issues |
| CI/CD | **GitHub Actions** | Lint, test, build, and deploy on push / PR merge |
| Containerization | **Docker** (optional) | Consistent dev & prod environments |
| Secrets Management | **GitHub Secrets / Azure Key Vault** | Protect API keys, DB credentials, JWT secrets |
| Monitoring | **Azure Monitor / Application Insights** | APM, error tracking, performance metrics |

---

## 5. Architecture Style

**Hybrid: Modular Monolith (with a clear path to microservices)**

### Rationale
- Team size and domain complexity do not warrant full microservices from day one.
- A modular monolith with strict bounded contexts keeps the codebase organized while allowing future extraction of services (e.g., a separate Notification Service or Reporting Service) if needed.
- Spring Boot 3's module structure maps naturally to Java packages / Maven modules.

### High-Level Architecture Diagram (Textual)

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                                 │
│  ┌───────────────┐  ┌───────────────┐  ┌───────────────────────┐   │
│  │  React SPA    │  │  Swagger UI   │  │  External Clients     │   │
│  │  (Vercel)     │  │  (docs)       │  │  (mobile, 3rd-party)  │   │
│  └──────┬────────┘  └──────┬────────┘  └──────────┬────────────┘   │
└─────────┼──────────────────┼───────────────────────┼───────────────┘
          │                  │                       │
          ▼                  ▼                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       API GATEWAY (optional)                        │
│              Spring Cloud Gateway / Nginx (future)                  │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER (Spring Boot 3)                │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  REST Controllers  ──►  Service Layer  ──►  Repository      │   │
│  │                          ▲    ▲    ▲                         │   │
│  │                          │    │    │                         │   │
│  │  ┌───────────────────────┴────┴────┴───────────────────┐    │   │
│  │  │  Domain Modules (Bounded Contexts)                  │    │   │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────┐  │    │   │
│  │  │  │ Catalog  │ │ Enrollment│ │ Learning │ │Auth  │  │    │   │
│  │  │  │ Module   │ │ Module   │ │ Module   │ │Module│  │    │   │
│  │  │  └──────────┘ └──────────┘ └──────────┘ └──────┘  │    │   │
│  │  │  ┌──────────┐ ┌──────────┐                         │    │   │
│  │  │  │Reporting │ │ Notification                      │    │   │
│  │  │  │ Module   │ │ Module   │                         │    │   │
│  │  │  └──────────┘ └──────────┘                         │    │   │
│  │  └────────────────────────────────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  Cross-Cutting Concerns                                     │   │
│  │  Security (Spring Security) │ Logging (SLF4J + Logback)     │   │
│  │  Exception Handling │ Validation │ Caching (Spring Cache)   │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────┬───────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       DATA LAYER                                    │
│                                                                     │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  MySQL 8                                                      │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌──────────┐ │   │
│  │  │ Users      │ │ Programs   │ │ Enrollments│ │ Progress │ │   │
│  │  └────────────┘ └────────────┘ └────────────┘ └──────────┘ │   │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────┐              │   │
│  │  │ Assessments│ │ Grades     │ │ Certificates│              │   │
│  │  └────────────┘ └────────────┘ └────────────┘              │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### Architectural Principles
- **Separation of Concerns** — Each domain module owns its data, logic, and API surface.
- **API-First** — RESTful APIs designed before UI implementation; documentation generated via OpenAPI.
- **Stateless Backend** — JWT-based auth enables horizontal scaling; session state is not stored on the server.
- **Defensive Design** — Validate at the boundary (controller), keep domain logic pure and testable.
- **Security by Design** — Role-based access control enforced at Spring Security layer, not in UI.

---

## 6. Folder Structure

```
skillenroll/
│
├── docs/                           # Project documentation
│   └── 01_PROJECT_CONTEXT.md       # This file
│
├── frontend/                       # React + TypeScript SPA
│   ├── public/                     # Static assets
│   ├── src/
│   │   ├── api/                    # API client configuration & endpoint wrappers
│   │   ├── assets/                 # Images, icons, fonts
│   │   ├── components/             # Reusable UI components
│   │   │   ├── common/             # Button, Input, Modal, etc.
│   │   │   ├── layout/             # Header, Footer, Sidebar, AppShell
│   │   │   └── shared/            # Shared domain-specific components
│   │   ├── features/               # Feature-based modules (co-located pages + logic)
│   │   │   ├── auth/               # Login, Register, ForgotPassword
│   │   │   ├── catalog/            # Browse & search programs
│   │   │   ├── enrollment/         # Enroll in programs
│   │   │   ├── learning/           # View modules, lessons, take assessments
│   │   │   ├── progress/           # Dashboard, progress tracking
│   │   │   ├── admin/              # Admin panel (users, programs, reports)
│   │   │   └── instructor/         # Instructor workspace (create programs, grade)
│   │   ├── hooks/                  # Custom React hooks
│   │   ├── lib/                    # Utility functions, constants, helpers
│   │   ├── routes/                 # Route definitions
│   │   ├── store/                  # State management (Context/Zustand)
│   │   ├── styles/                 # Global styles, Tailwind config
│   │   ├── types/                  # TypeScript type definitions & interfaces
│   │   ├── App.tsx                 # Root component
│   │   └── main.tsx                # Entry point
│   ├── .env.example                # Environment variable template
│   ├── index.html                  # SPA entry HTML
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── tailwind.config.js (if using Tailwind)
│
├── backend/                        # Spring Boot 3 + Maven
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/skillenroll/
│   │   │   │   ├── SkillEnrollApplication.java   # Main entry point
│   │   │   │   │
│   │   │   │   ├── config/                       # Application configuration
│   │   │   │   │   ├── SecurityConfig.java       # Spring Security + JWT config
│   │   │   │   │   ├── CorsConfig.java           # CORS configuration
│   │   │   │   │   ├── OpenApiConfig.java        # Swagger/OpenAPI config
│   │   │   │   │   └── AppConfig.java            # General bean definitions
│   │   │   │   │
│   │   │   │   ├── security/                     # Auth & authorization
│   │   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   │   ├── UserDetailsServiceImpl.java
│   │   │   │   │   └── SecurityUtils.java
│   │   │   │   │
│   │   │   │   ├── common/                       # Cross-cutting utilities
│   │   │   │   │   ├── exception/
│   │   │   │   │   ├── dto/                      # Generic DTOs (PageResponse, ApiResponse)
│   │   │   │   │   ├── validation/
│   │   │   │   │   └── annotation/
│   │   │   │   │
│   │   │   │   ├── user/                         # User management module
│   │   │   │   │   ├── api/                      # REST controllers
│   │   │   │   │   ├── application/              # Service layer
│   │   │   │   │   ├── domain/                   # Entity, VO, domain services
│   │   │   │   │   ├── infrastructure/           # Repository, JPA mapping
│   │   │   │   │   └── dto/                      # Request/Response DTOs
│   │   │   │   │
│   │   │   │   ├── catalog/                      # Program/course catalog module
│   │   │   │   │   ├── api/
│   │   │   │   │   ├── application/
│   │   │   │   │   ├── domain/
│   │   │   │   │   ├── infrastructure/
│   │   │   │   │   └── dto/
│   │   │   │   │
│   │   │   │   ├── enrollment/                   # Enrollment workflow module
│   │   │   │   │   ├── api/
│   │   │   │   │   ├── application/
│   │   │   │   │   ├── domain/
│   │   │   │   │   ├── infrastructure/
│   │   │   │   │   └── dto/
│   │   │   │   │
│   │   │   │   ├── learning/                     # Learning & progress module
│   │   │   │   │   ├── api/
│   │   │   │   │   ├── application/
│   │   │   │   │   ├── domain/
│   │   │   │   │   ├── infrastructure/
│   │   │   │   │   └── dto/
│   │   │   │   │
│   │   │   │   ├── assessment/                   # Quizzes, assignments, grading
│   │   │   │   │   ├── api/
│   │   │   │   │   ├── application/
│   │   │   │   │   ├── domain/
│   │   │   │   │   ├── infrastructure/
│   │   │   │   │   └── dto/
│   │   │   │   │
│   │   │   │   ├── reporting/                    # Reports & analytics module
│   │   │   │   │   ├── api/
│   │   │   │   │   ├── application/
│   │   │   │   │   ├── domain/
│   │   │   │   │   ├── infrastructure/
│   │   │   │   │   └── dto/
│   │   │   │   │
│   │   │   │   └── notification/                 # Notifications module (email, in-app)
│   │   │   │       ├── api/
│   │   │   │       ├── application/
│   │   │   │       ├── domain/
│   │   │   │       ├── infrastructure/
│   │   │   │       └── dto/
│   │   │   │
│   │   │   └── resources/
│   │   │       ├── application.yml               # Main config
│   │   │       ├── application-dev.yml           # Dev profile
│   │   │       ├── application-prod.yml          # Prod profile
│   │   │       └── db/migration/                 # Flyway migrations
│   │   │           ├── V1__init_schema.sql
│   │   │           └── V2__seed_data.sql
│   │   │
│   │   └── test/
│   │       └── java/com/skillenroll/
│   │           ├── user/
│   │           ├── catalog/
│   │           ├── enrollment/
│   │           ├── learning/
│   │           ├── assessment/
│   │           ├── reporting/
│   │           └── notification/
│   │
│   ├── pom.xml                      # Maven build descriptor
│   ├── Dockerfile                   # (Optional) Container image
│   └── .dockerignore
│
├── .gitignore
├── .github/                         # GitHub configuration
│   ├── workflows/
│   │   ├── ci.yml                   # CI pipeline (lint, test, build)
│   │   └── cd.yml                   # CD pipeline (deploy)
│   └── PULL_REQUEST_TEMPLATE.md
│
├── README.md
└── .env.example                     # Root-level env vars (if any)
```

---

## 7. Branch Strategy

The project follows **GitHub Flow** — a lightweight, branch-based workflow:

```
main (production)                           # Production-ready; protected — no direct pushes
  │
  ├── feature/SS-123-login                  # New features — branched from main
  ├── feature/SS-456-catalog                 # Short-lived (< 3 days)
  ├── bugfix/SS-789-fix-enroll              # Bug fixes — branched from main
  │
  └── hotfix/SS-999-critical                # Urgent production fix — branched from main
```

> **Note for complex releases:** If concurrent features need coordinated release, teams may optionally create a `release/v*` branch from `main` and merge features into it. This is **not the default workflow** — use only when multiple features must ship together.

### Naming Convention
```
<type>/<issue-id>-<short-description>
```

| Type | Description | Branch From | Merge Into |
|------|-------------|-------------|------------|
| `feature/` | New functionality | `main` | `main` via PR |
| `bugfix/` | Non-critical bug fix | `main` | `main` via PR |
| `hotfix/` | Critical production bug | `main` | `main` via PR |
| `docs/` | Documentation-only changes | `main` | `main` via PR |
| `chore/` | Build/config/tooling | `main` | `main` via PR |

### Workflow Rules
1. **Never push directly to `main`** — all changes require a pull request.
2. **PRs must be reviewed** by at least one other team member before merging.
3. **CI must pass** (lint → test → build) before merge.
4. **Feature branches should be short-lived** (< 3 days) to avoid merge conflicts.
5. **Squash-merge** into `main` to keep history clean.
6. **Delete the branch** after merging.

---

## 8. Deployment Strategy

### 8.1 Environments

| Environment | URL | Purpose | Deployed From |
|-------------|-----|---------|---------------|
| **Local** | `localhost:5173` (FE), `localhost:8080` (BE) | Development & debugging | Local machine |
| **Staging** | `staging.skillenroll.app` | QA, integration testing, UAT | `main` branch (auto-deploy) |
| **Production** | `app.skillenroll.app` | Live end-user access | Git tag / release |

### 8.2 CI/CD Pipeline (GitHub Actions)

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  Push /  │───▶│   Lint   │───▶│   Test   │───▶│  Build   │───▶│  Deploy  │
│  PR      │    │          │    │          │    │          │    │          │
└──────────┘    └──────────┘    └──────────┘    └──────────┘    └──────────┘
                                                                    │
                                          ┌─────────────────────────┼──────────┐
                                          │                         │          │
                                          ▼                         ▼          ▼
                                   ┌──────────────┐        ┌──────────────┐
                                   │  Vercel      │        │ Azure App    │
                                   │  (Frontend)  │        │ Service (BE) │
                                   └──────────────┘        └──────────────┘
```

### 8.3 Frontend Deployment — Vercel

| Aspect | Detail |
|--------|--------|
| Provider | **Vercel** (optimized for React SPAs) |
| Trigger | Push to `main` or PR preview |
| Build Command | `npm run build` |
| Output Dir | `dist/` |
| Environment Variables | Configured via Vercel Dashboard (API URL, Auth keys) |
| Preview Deployments | Auto-generated for every PR (ephemeral URL) |
| Domain | Custom domain with automatic SSL via Vercel |

### 8.4 Backend Deployment — Azure App Service

| Aspect | Detail |
|--------|--------|
| Provider | **Azure App Service** (Java SE runtime / Linux) |
| Trigger | Push to `main` → GitHub Actions builds JAR → deploys via Azure WebApp deploy action |
| Artifact | `backend/target/skillenroll-backend-*.jar` (fat JAR) |
| Startup Command | `java -jar skillenroll-backend-*.jar` |
| Configuration | Application settings via Azure App Service → Configuration (env vars) |
| Auto-scaling | Scale out based on CPU / memory thresholds |
| Database | Azure Database for MySQL (or MySQL 8 on Azure VM) |

### 8.5 Database Strategy

| Stage | DB Instance |
|-------|-------------|
| Local | MySQL 8 via Docker (`docker-compose up -d db`) or local install |
| Staging | Azure Database for MySQL — Single Server (Burstable, B2s) |
| Production | Azure Database for MySQL — Flexible Server (General Purpose, D2ds v4) |
| Migrations | Flyway — run as part of Spring Boot startup (`spring.flyway.enabled=true`) |
| Backups | Automated daily backups with 7-day retention (configurable) |

### 8.6 Security & Secrets Management

- **JWT Secret**: Generated per environment; stored in Azure Key Vault / GitHub Secrets.
- **Database Credentials**: Stored as Azure App Service application settings (injected as env vars).
- **API Keys**: Third-party service keys stored in GitHub Secrets for CI/CD, Azure Key Vault for runtime.
- **HTTPS**: Enforced at Vercel (FE) and Azure App Service (BE) — TLS 1.2+ only.
- **CORS**: Restricted to known origins (staging & production domains only).

---

## 9. Risks, Assumptions & Constraints

### 9.1 Key Assumptions

| # | Assumption | Impact if False |
|---|------------|----------------|
| A1 | MySQL 8 meets reporting query performance needs without a read replica in year 1 | May need to add read replicas or introduce a caching layer (Redis) |
| A2 | The team is familiar with Spring Boot 3, React 18, and TypeScript | Onboarding delay and potential quality issues if ramp-up is needed |
| A3 | Learners access the platform primarily via modern web browsers (Chrome, Firefox, Safari, Edge) | Mobile app or polyfill effort may be required for older browsers |
| A4 | Course content is primarily text, video (embedded), and PDF documents | Special handling needed for interactive content (labs, code sandboxes) |
| A5 | Average concurrent active users in first 6 months ≤ 500 | Architecture may need re-evaluation for scale beyond this threshold |

### 9.2 Key Risks

| # | Risk | Likelihood | Impact | Mitigation |
|---|------|:----------:|:------:|------------|
| R1 | Scope creep — feature requests during development | Medium | High | Strict MVP scope definition; formal change request process after sprint 1 |
| R2 | Third-party service downtime (video hosting, email delivery) | Low | Medium | Graceful degradation; cache critical data; queue email delivery |
| R3 | Data migration complexity from legacy systems | Medium | High | Early data audit; phased migration with mock runs |
| R4 | JWT token security vulnerability | Low | Critical | Short token expiry; refresh token rotation; HTTPS enforcement; security audit |
| R5 | Single point of failure (monolith) as user base grows | Low (year 1) | High | Modular design enables extraction to microservices; monitor performance metrics |

### 9.3 Constraints

| # | Constraint | Description |
|---|------------|-------------|
| C1 | **Budget** | Cloud infrastructure costs must stay within defined monthly budget (to be set at sprint zero) |
| C2 | **Timeline** | MVP must ship within 3 sprints (6 weeks) from development start |
| C3 | **Team size** | Core dev team of 4–5 engineers — limits parallel workstreams |
| C4 | **Regulatory** | Must comply with applicable data protection regulations (GDPR if EU users, or regional equivalent) |
| C5 | **Accessibility** | Frontend must meet WCAG 2.1 AA standards for inclusive access |

---

## 10. Appendix — Key Design Decisions

| Decision | Option Chosen | Rationale |
|----------|--------------|-----------|
| Monolith vs Microservices | Modular Monolith | Team size, domain cohesion, and operational simplicity |
| REST vs GraphQL | REST (OpenAPI) | Simpler tooling, broad client support, sufficient for CRUD-heavy domain |
| JWT vs Session | JWT | Stateless backend enables horizontal scaling; mobile-friendly |
| MySQL vs PostgreSQL | MySQL 8 | Team familiarity, Azure MySQL managed service availability |
| Maven vs Gradle | Maven | Standard in enterprise Java; stable, well-documented |
| Vite vs CRA | Vite | Faster dev server, smaller bundle, modern default |
| Tailwind vs MUI | TBD at Sprint Zero | Both viable; decision based on designer availability & UI complexity |

---

> **Next Document:** `02_REQUIREMENTS.md` — Functional and non-functional requirements, user stories, and acceptance criteria.
