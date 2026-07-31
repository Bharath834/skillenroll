# 03_ARCHITECTURE.md — SkillEnroll

> **Document Version:** 1.0  
> **Author:** Senior Software Architect  
> **Framework:** TrainingMug AI Development Framework  
> **Status:** ✅ Approved  
> **Related Docs:** [01_PROJECT_CONTEXT.md](./01_PROJECT_CONTEXT.md) · [02_REQUIREMENTS.md](./02_REQUIREMENTS.md)

---

## Table of Contents

1. [High-Level Architecture](#1-high-level-architecture)
2. [Component Diagram](#2-component-diagram)
3. [Layered Architecture](#3-layered-architecture)
4. [Authentication Flow](#4-authentication-flow)
5. [Deployment Diagram](#5-deployment-diagram)
6. [Module Deep-Dive](#6-module-deep-dive)
7. [Key Design Decisions](#7-key-design-decisions)

---

## 1. High-Level Architecture

### 1.1 Architecture Style

SkillEnroll follows a **Modular Monolith** architecture — a single deployable unit (Spring Boot 3 application) with **strictly separated domain modules** (bounded contexts). This provides the development simplicity of a monolith with the organizational clarity of microservices.

### 1.2 Why Modular Monolith?

| Factor | Decision | Rationale |
|--------|----------|-----------|
| Team size | 4–5 engineers | A monolith avoids the operational overhead of microservices (service discovery, distributed tracing, inter-service communication) |
| Domain complexity | Moderate | The domain is CRUD-heavy with clear boundaries; no need for independent scaling of individual modules |
| Deployment simplicity | Single JAR | One CI/CD pipeline, one artifact, one deploy target — faster shipping |
| Future-proofing | Module extraction path | If a module (e.g., Reporting) needs independent scaling later, its bounded context design allows extraction into a separate service with minimal refactoring |

### 1.3 System Context Diagram (C4 Level 1)

The system context diagram shows SkillEnroll as a black box and its interactions with external actors and systems.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│   ┌──────────────────────────────────────────────────────────────────┐      │
│   │                     SKILLENROLL SYSTEM                           │      │
│   │                                                                  │      │
│   │   ┌──────────────┐    ┌──────────────┐    ┌──────────────┐      │      │
│   │   │  React SPA   │◄──►│ Spring Boot  │◄──►│   MySQL 8    │      │      │
│   │   │  (Browser)   │    │  Backend API │    │   Database   │      │      │
│   │   └──────┬───────┘    └──────┬───────┘    └──────────────┘      │      │
│   │          │                   │                                   │      │
│   │          │            ┌──────┴───────┐                           │      │
│   │          │            │  File Store  │                           │      │
│   │          │            │ (Azure Blob) │                           │      │
│   │          │            └──────────────┘                           │      │
│   └──────────────────────────────────────────────────────────────────┘      │
│         ▲                      ▲                      ▲                     │
│         │                      │                      │                     │
│   ┌─────┴──────┐        ┌──────┴──────┐        ┌──────┴──────┐             │
│   │  Learner   │        │  Instructor │        │    Admin    │             │
│   │ (Browser)  │        │  (Browser)  │        │  (Browser)  │             │
│   └────────────┘        └─────────────┘        └─────────────┘             │
│                                                                             │
│   ┌─────────────────────────────────────────────┐                          │
│   │             EXTERNAL SYSTEMS                 │                          │
│   │  ┌──────────┐  ┌──────────┐  ┌───────────┐  │                          │
│   │  │ Email    │  │  Video   │  │   Social  │  │                          │
│   │  │ Service  │  │ Hosting  │  │   Login   │  │                          │
│   │  │ (SMTP)   │  │(YouTube) │  │ (Google)  │  │                          │
│   │  └──────────┘  └──────────┘  └───────────┘  │                          │
│   └─────────────────────────────────────────────┘                          │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.4 Architectural Principles

| Principle | Description |
|-----------|-------------|
| **Separation of Concerns** | Each domain module (Catalog, Enrollment, Learning, etc.) owns its data, logic, and API surface. Modules communicate through well-defined service interfaces, never through shared databases. |
| **API-First** | RESTful APIs are designed and documented (OpenAPI) before UI implementation. Frontend and backend can be developed in parallel against the contract. |
| **Stateless Backend** | The backend does not store session state. All authentication state is carried in JWT tokens, enabling horizontal scaling without sticky sessions. |
| **Defensive Design** | Input validation happens at the controller boundary (Jakarta Bean Validation). Domain logic is kept pure and testable, free from framework concerns. |
| **Security by Design** | Role-based access control is enforced at the Spring Security method-security layer, not in the UI. This prevents privilege escalation even if the client is tampered with. |
| **Persistence Ignorance** | Domain entities are plain Java objects (POJOs) with no JPA annotations. JPA mapping is handled separately in the infrastructure layer (Hexagonal Architecture pattern). |

---

## 2. Component Diagram

### 2.1 Container Diagram (C4 Level 2)

The following diagram shows the major runtime containers within SkillEnroll and how they communicate.

```
┌────────────────────────────────────────────────────────────────────────────────────┐
│                          SKILLENROLL — CONTAINER DIAGRAM                           │
│                                                                                    │
│  ┌──────────────────────────────────────────────────────────────────────────┐      │
│  │  SINGLE PAGE APPLICATION (React 18 + TypeScript + Vite)                   │      │
│  │                                                                          │      │
│  │  ┌────────────────────────────────────────────────────────────────────┐ │      │
│  │  │                          REACT APP                                 │ │      │
│  │  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐  │ │      │
│  │  │  │ Auth     │ │ Catalog │ │Learning  │ │Instructor│ │ Admin  │  │ │      │
│  │  │  │ Feature  │ │ Feature │ │ Feature  │ │ Feature  │ │ Feature│  │ │      │
│  │  │  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────┘  │ │      │
│  │  │                                                                   │ │      │
│  │  │  ┌────────────────────────────────────────────────────────────┐  │ │      │
│  │  │  │  Shared Layer: Components · Hooks · API Client · Store     │  │ │      │
│  │  │  │  Axios (HTTP) · React Router · Context/Zustand             │  │ │      │
│  │  │  └────────────────────────────────────────────────────────────┘  │ │      │
│  │  └──────────────────────────────────────────────────────────────────┘ │      │
│  │  [HTTPS]                                                            │      │
│  │  Deployment: Vercel (Global CDN)                                    │      │
│  └──────────────────────────┬───────────────────────────────────────────┘      │
│                             │ HTTPS / REST / JSON                               │
│                             ▼                                                   │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │  WEB APPLICATION (Spring Boot 3 + Java 17) — Azure App Service           │  │
│  │                                                                          │  │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │  │
│  │  │  API Layer: REST Controllers                                        │  │  │
│  │  │  ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐     │  │  │
│  │  │  │Auth   │ │Catalog│ │Enroll │ │Learn  │ │Assess │ │Report │     │  │  │
│  │  │  │Ctrl   │ │Ctrl   │ │Ctrl   │ │Ctrl   │ │Ctrl   │ │Ctrl   │     │  │  │
│  │  │  └───┬───┘ └───┬───┘ └───┬───┘ └───┬───┘ └───┬───┘ └───┬───┘     │  │  │
│  │  │      │         │         │         │         │         │         │  │  │
│  │  │  ┌───▼─────────▼─────────▼─────────▼─────────▼─────────▼───────┐ │  │  │
│  │  │  │  Service Layer: @Service / @Transactional                    │ │  │  │
│  │  │  │  ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐        │ │  │  │
│  │  │  │  │Auth   │ │Catalog│ │Enroll │ │Learn  │ │Assess │        │ │  │  │
│  │  │  │  │Svc    │ │Svc    │ │Svc    │ │Svc    │ │Svc    │        │ │  │  │
│  │  │  │  └───────┘ └───────┘ └───────┘ └───────┘ └───────┘        │ │  │  │
│  │  │  └───────────────────────────────────────────────────────────┘ │  │  │
│  │  │                                                                  │  │  │
│  │  │  ┌────────────────────────────────────────────────────────────┐ │  │  │
│  │  │  │  Repository Layer: Spring Data JPA                          │ │  │  │
│  │  │  │  JpaRepository<Entity, Long> · Custom Queries ·            │ │  │  │
│  │  │  │  Specifications · Pageable                                  │ │  │  │
│  │  │  └────────────────────────────────────────────────────────────┘ │  │  │
│  │  └─────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                          │  │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │  │
│  │  │  Cross-Cutting: Spring Security · JWT · Validation · Caching       │  │  │
│  │  │  Exception Handler · Logging (SLF4J+Logback) · OpenAPI (Swagger)   │  │  │
│  │  └────────────────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
│                             │                                                  │
│                             ▼                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │  DATABASE (MySQL 8) — Azure Database for MySQL                          │  │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │  │
│  │  │  Tables: users · programs · modules · lessons · enrollments ·      │  │  │
│  │  │  assessments · quiz_questions · submissions · grades ·             │  │  │
│  │  │  certificates · notifications · audit_logs                        │  │  │
│  │  │  Migrations: Flyway (V1__init, V2__seed, ...)                     │  │  │
│  │  └────────────────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
│                             │                                                  │
│                             ▼                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐  │
│  │  FILE STORAGE (Azure Blob Storage)                                      │  │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │  │
│  │  │  Containers: program-thumbnails · lesson-materials ·               │  │  │
│  │  │  assignment-submissions · certificate-templates                    │  │  │
│  │  └────────────────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Inter-Component Communication

| From | To | Protocol | Format | Notes |
|------|----|----------|--------|-------|
| Browser (React SPA) | Spring Boot API | HTTPS | JSON (REST) | All data operations; JWT in `Authorization: Bearer` header |
| Spring Boot API | MySQL 8 | MySQL Wire Protocol | SQL | HikariCP connection pool; managed by Spring Data JPA |
| Spring Boot API | Azure Blob Storage | HTTPS | Binary | File upload/download via Azure Storage SDK |
| Spring Boot API | SMTP Email Service | SMTP | MIME | Transactional emails via Spring Mail |
| Spring Boot API | Application Insights | HTTPS | JSON | Telemetry and APM data |

### 2.3 Module Dependency Rules

To preserve the modular monolith integrity, the following dependency rules are enforced. Arrows (▼) point in the **direction of dependency** — the module at the arrow tail depends on the module at the arrow head.

```
               ┌──────────────┐
               │    Auth      │  (No module-level dependencies — foundational)
               └──────┬───────┘
                      │
         ┌────────────┼────────────┐
         ▼            ▼            ▼
   ┌──────────┐ ┌──────────┐ ┌──────────┐
   │ User     │ │ Catalog  │ │Program   │
   │ Module   │ │ Module   │ │Mgmt Mod  │
   └────┬─────┘ └────┬─────┘ └────┬─────┘
        │            │            │
        └──────────┬─┼────────────┘
                   ▼ ▼
            ┌──────────────┐
            │  Enrollment  │
            │   Module     │
            └──────┬───────┘
                   │
         ┌─────────┼─────────┐
         ▼         ▼         ▼
   ┌──────────┐ ┌────────┐ ┌──────────┐
   │ Learning │ │Assess  │ │ Reporting│
   │ Module   │ │Module  │ │ Module   │
   └──────────┘ └────────┘ └──────────┘
         │         │
         └─────────┼─────────┐
                   ▼         ▼
            ┌──────────┐ ┌──────────┐
            │ Notifica- │ │  Admin   │
            │ tion Mod  │ │  Module  │
            └──────────┘ └──────────┘
```

**Rules:**
- Lower modules **never** depend on higher modules (e.g., Catalog never imports Enrollment)
- Communication between sibling modules happens only through **service interfaces**, never by sharing database tables
- The `common` package provides shared DTOs, utilities, and exception types that all modules may use

---

## 3. Layered Architecture

### 3.1 Backend Layered Structure (Spring Boot 3)

Each domain module follows a strict **four-layer** internal architecture, based on the Hexagonal Architecture (Ports & Adapters) pattern:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        MODULE BOUNDARY                                      │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  LAYER 1: API (Inbound Adapter) — REST Controllers                   │   │
│  │                                                                       │   │
│  │  Role: Handle HTTP requests/responses                                 │   │
│  │  Contains: @RestController classes, DTOs (Request/Response)          │   │
│  │  Responsibilities:                                                    │   │
│  │  - Parse and validate incoming HTTP requests (Jakarta Validation)     │   │
│  │  - Map DTOs to domain objects (via MapStruct or manual mapper)        │   │
│  │  - Delegate to the Application layer                                  │   │
│  │  - Return HTTP responses with proper status codes                     │   │
│  │  - Handle Swagger/OpenAPI annotations                                 │   │
│  │                                                                       │   │
│  │  Rules:                                                               │   │
│  │  - Controllers contain NO business logic                              │   │
│  │  - Controllers NEVER access repositories directly                     │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  LAYER 2: Application (Service Layer) — Use Cases                     │   │
│  │                                                                       │   │
│  │  Role: Orchestrate business operations                                │   │
│  │  Contains: @Service classes, @Transactional methods                   │   │
│  │  Responsibilities:                                                    │   │
│  │  - Implement use cases / business workflows                           │   │
│  │  - Coordinate multiple aggregate operations                           │   │
│  │  - Transaction management (begin/commit/rollback)                     │   │
│  │  - Authorization checks (Spring Security @PreAuthorize)               │   │
│  │  - Event publishing (e.g., "EnrollmentCreatedEvent")                  │   │
│  │                                                                       │   │
│  │  Rules:                                                               │   │
│  │  - Services depend on domain interfaces (NOT on infrastructure)       │   │
│  │  - Services are unit-testable with mocked repositories                │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  LAYER 3: Domain (Business Logic) — Entities & Value Objects          │   │
│  │                                                                       │   │
│  │  Role: Encapsulate core business rules                                │   │
│  │  Contains: Entities, Value Objects, Domain Services, Enums            │   │
│  │  Responsibilities:                                                    │   │
│  │  - Enforce business invariants (e.g., "enrollment requires active     │   │
│  │    program and verified learner")                                     │   │
│  │  - Pure Java — NO framework annotations (@Entity, @Column, etc.)      │   │
│  │  - Domain events for cross-module communication                       │   │
│  │                                                                       │   │
│  │  Rules:                                                               │   │
│  │  - Domain layer has ZERO external dependencies                        │   │
│  │  - Domain objects are POJOs with business methods                     │   │
│  │  - No getters/setters for the sake of ORM — design for behavior       │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  LAYER 4: Infrastructure (Outbound Adapter) — Persistence & I/O       │   │
│  │                                                                       │   │
│  │  Role: Implement interfaces defined by the domain/application layers   │   │
│  │  Contains: JPA Repositories, Entity Mappings, File Storage Adapters   │   │
│  │  Responsibilities:                                                    │   │
│  │  - Database access via Spring Data JPA                                │   │
│  │  - ORM mapping (JPA annotations on separate entity classes)           │   │
│  │  - File upload/download to Azure Blob Storage                         │   │
│  │  - Email sending via Spring Mail                                      │   │
│  │                                                                       │   │
│  │  Rules:                                                               │   │
│  │  - Infrastructure implements domain interfaces (Dependency Inversion) │   │
│  │  - Swapping databases/storage requires changing ONLY this layer       │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 Frontend Layered Structure (React + TypeScript)

The frontend follows a **feature-based** layered architecture:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        REACT APPLICATION                                   │
│                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  LAYER 1: Presentation (Pages & Components)                          │   │
│  │                                                                       │   │
│  │  - Page-level components (e.g., CatalogPage, DashboardPage)          │   │
│  │  - Reusable UI components (common/, shared/)                          │   │
│  │  - Layout components (Header, Sidebar, Footer)                       │   │
│  │  - Feature-specific components (co-located within feature folders)    │   │
│  │                                                                       │   │
│  │  Rules:                                                               │   │
│  │  - Components NEVER call API directly — use hooks                     │   │
│  │  - Components receive data via props or hooks                         │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  LAYER 2: State Management (Store & Context)                          │   │
│  │                                                                       │   │
│  │  - React Context for global auth/user state                           │   │
│  │  - Zustand (or useReducer) for complex feature state                  │   │
│  │  - React Query (TanStack Query) for server state caching              │   │
│  │                                                                       │   │
│  │  Responsibilities:                                                    │   │
│  │  - Manage UI state (loading, error, empty, data)                      │   │
│  │  - Cache API responses                                                │   │
│  │  - Optimistic updates for fast UX                                     │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                    │                                        │
│                                    ▼                                        │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  LAYER 3: API Client (Axios-based Service Layer)                      │   │
│  │                                                                       │   │
│  │  - Axios instance with base URL, interceptors                         │   │
│  │  - Request interceptor: attach JWT token                              │   │
│  │  - Response interceptor: handle 401 → refresh token / redirect login  │   │
│  │  - Per-feature API modules (auth.api.ts, catalog.api.ts, etc.)        │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Authentication Flow

### 4.1 Authentication Architecture

SkillEnroll uses **JWT (JSON Web Token)** for stateless authentication. The flow is managed by **Spring Security** with a custom filter chain.

**Key Components:**

| Component | File (Conceptual) | Role |
|-----------|-------------------|------|
| `SecurityConfig` | `config/SecurityConfig.java` | Configures the Spring Security filter chain; defines public vs authenticated endpoints; sets CORS, CSRF, session management |
| `JwtTokenProvider` | `security/JwtTokenProvider.java` | Generates and validates JWT tokens (access + refresh); extracts user details from token |
| `JwtAuthenticationFilter` | `security/JwtAuthenticationFilter.java` | Once-per-request filter: extracts JWT from `Authorization` header, validates, sets `SecurityContext` |
| `UserDetailsServiceImpl` | `security/UserDetailsServiceImpl.java` | Loads user details from the database for authentication |
| `SecurityUtils` | `security/SecurityUtils.java` | Utility for getting the currently authenticated user's ID and roles |

### 4.2 Login Flow

```
  ┌─────────┐          ┌──────────────────┐          ┌──────────────┐          ┌────────┐
  │ Browser │          │ React SPA        │          │ Spring Boot  │          │ MySQL  │
  │         │          │                  │          │ Backend      │          │        │
  └────┬────┘          └────────┬─────────┘          └──────┬───────┘          └───┬────┘
       │                       │                           │                      │
       │  1. User enters       │                           │                      │
       │     email & password  │                           │                      │
       │──────────────────────►│                           │                      │
       │                       │  2. POST /api/auth/login  │                      │
       │                       │     { email, password }   │                      │
       │                       │──────────────────────────►│                      │
       │                       │                           │  3. Load user by      │
       │                       │                           │     email             │
       │                       │                           │─────────────────────►│
       │                       │                           │◄─────────────────────┤
       │                       │                           │                      │
       │                       │                           │  4. Verify password   │
       │                       │                           │     (BCrypt)         │
       │                       │                           │                      │
       │                       │                           │  5. Generate tokens   │
       │                       │                           │     Access (15 min)  │
       │                       │                           │     Refresh (7 days) │
       │                       │                           │                      │
       │                       │  6. 200 OK                │                      │
       │                       │  { accessToken,           │                      │
       │                       │    refreshToken,           │                      │
       │                       │    user { id, name,       │                      │
       │                       │    email, role } }        │                      │
       │                       │◄──────────────────────────┤                      │
       │                       │                           │                      │
       │  7. Store tokens:     │                           │                      │
       │     Access → memory   │                           │                      │
       │     Refresh → httpOnly│                           │                      │
       │     cookie (optional) │                           │                      │
       │◄──────────────────────┤                           │                      │
       │                       │                           │                      │
       │  8. Redirect to       │                           │                      │
       │     dashboard         │                           │                      │
```

### 4.3 Authenticated Request Flow

```
  ┌─────────┐          ┌──────────────────┐          ┌──────────────────────┐          ┌────────┐
  │ Browser │          │ React SPA        │          │ Spring Boot          │          │ MySQL  │
  │         │          │                  │          │ (JwtAuthFilter)      │          │        │
  └────┬────┘          └────────┬─────────┘          └──────────┬───────────┘          └───┬────┘
       │                       │                              │                          │
       │                       │  1. API call with            │                          │
       │                       │     Authorization:           │                          │
       │                       │     Bearer <accessToken>     │                          │
       │                       │─────────────────────────────►│                          │
       │                       │                              │                          │
       │                       │                              │  2. Extract token from    │
       │                       │                              │     Authorization header  │
       │                       │                              │                          │
       │                       │                              │  3. Validate:             │
       │                       │                              │     - Signature          │
       │                       │                              │     - Expiry             │
       │                       │                              │     - Issuer             │
       │                       │                              │                          │
       │                       │                              │  4. Set SecurityContext  │
       │                       │                              │     (Authentication obj) │
       │                       │                              │                          │
       │                       │                              │  5. Controller checks    │
       │                       │                              │     @PreAuthorize roles  │
       │                       │                              │                          │
       │                       │                              │  6. Business logic       │
       │                       │                              │─────────────────────────►│
       │                       │                              │◄─────────────────────────┤
       │                       │                              │                          │
       │                       │  7. 200 OK (response data)   │                          │
       │                       │◄─────────────────────────────┤                          │
       │  8. Render data       │                              │                          │
       │◄──────────────────────┤                              │                          │
```

### 4.4 Token Refresh Flow

```
  ┌──────────┐          ┌──────────────────┐          ┌────────────────────┐
  │  Browser │          │ React SPA        │          │ Spring Boot        │
  │          │          │ (Axios Intercept)│          │ Backend            │
  └────┬─────┘          └────────┬─────────┘          └────────┬───────────┘
       │                        │                             │
       │  1. API call with      │                             │
       │     expired token      │                             │
       │────────────────────────►                             │
       │                        │  2. POST /api/auth/...      │
       │                        │────────────────────────────►│
       │                        │                             │
       │                        │  3. 401 Unauthorized        │
       │                        │◄────────────────────────────┤
       │                        │                             │
       │                        │  4. Axios interceptor       │
       │                        │     catches 401             │
       │                        │     (queue failed request)  │
       │                        │                             │
       │                        │  5. POST /api/auth/refresh  │
       │                        │     { refreshToken }        │
       │                        │────────────────────────────►│
       │                        │                             │  6. Validate refresh
       │                        │                             │     token (check
       │                        │                             │     DB/rotation)
       │                        │                             │
       │                        │  7. 200 OK                  │
       │                        │  { newAccessToken,          │
       │                        │    newRefreshToken }        │
       │                        │◄────────────────────────────┤
       │                        │                             │
       │                        │  8. Retry original request  │
       │                        │     with new access token   │
       │                        │────────────────────────────►│
       │                        │                             │
       │                        │  9. 200 OK                  │
       │                        │◄────────────────────────────┤
       │ 10. Render data        │                             │
       │◄───────────────────────┤                             │
```

### 4.5 Token Structure

**Access Token (JWT):**
- **Header:** Algorithm (HS512) and token type (JWT)
- **Subject:** User UUID (unique identifier for the user)
- **Email:** User's email address
- **Roles:** Granted authorities (e.g., `ROLE_LEARNER`)
- **Issued At (iat):** Unix epoch timestamp (seconds) when the token was issued
- **Expiry (exp):** Unix epoch timestamp (seconds) — 15 minutes after `iat`
- **Issuer (iss):** `skillenroll-api`
- **Signature:** HMAC-SHA512 of the base64-encoded header and payload, signed with the server's secret key

**Refresh Token:**
- Opaque string (UUID v4) stored as hashed value in the database
- 7-day expiry
- Rotation enabled: each refresh invalidates the previous token (prevents token reuse on theft)

### 4.6 Security Configuration Summary

| Endpoint Pattern | Access | Notes |
|-----------------|--------|-------|
| `POST /api/auth/register` | **Anonymous** | Rate-limited (5/min/IP) |
| `POST /api/auth/login` | **Anonymous** | Rate-limited (5/min/IP) |
| `POST /api/auth/refresh` | **Anonymous** (with valid refresh token) | — |
| `POST /api/auth/password-reset` | **Anonymous** | Rate-limited |
| `GET /api/catalog/**` | **Anonymous** (public) | Browse, search, view details |
| `POST /api/enrollments/**` | **ROLE_LEARNER** | Authenticated learners only |
| `POST /api/programs/**` | **ROLE_INSTRUCTOR** | Program creation/editing |
| `GET /api/admin/**` | **ROLE_ADMIN** | Admin-only endpoints |
| `GET /api/swagger-ui/**` | **ROLE_ADMIN** (optionally) | API docs in production |

---

## 5. Deployment Diagram

### 5.1 Environment Overview

```
┌────────────────────────────────────────────────────────────────────────────────────────────┐
│                              SKILLENROLL — DEPLOYMENT DIAGRAM                              │
│                                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────────────────────┐  │
│  │                            GITHUB.COM                                                  │  │
│  │                                                                                       │  │
│  │  ┌─────────────────────────────────────────────────────────────────────────────────┐  │  │
│  │  │  Repository: skillenroll/skillenroll                                            │  │  │
│  │  │  ┌────────────────────────────────────────┐  ┌───────────────────────────────┐  │  │  │
│  │  │  │  frontend/                             │  │  backend/                      │  │  │  │
│  │  │  │  (React + TypeScript + Vite)           │  │  (Spring Boot 3 + Maven)       │  │  │  │
│  │  │  └────────────────────────────────────────┘  └───────────────────────────────┘  │  │  │
│  │  └─────────────────────────────────────────────────────────────────────────────────┘  │  │
│  │                                                                                       │  │
│  │  ┌─────────────────────────────────────────────────────────────────────────────────┐  │  │
│  │  │  GitHub Actions Workflows                                                        │  │  │
│  │  │  ┌──────────────────────────────────────┐  ┌──────────────────────────────────┐  │  │  │
│  │  │  │  CI Workflow:                        │  │  CD Workflow:                    │  │  │  │
│  │  │  │  • Push / PR → main                 │  │  • Push to main → deploy         │  │  │  │
│  │  │  │  • Lint (ESLint + Checkstyle)       │  │  • Build JAR + Docker image      │  │  │  │
│  │  │  │  • Test (JUnit + Vitest)            │  │  • Deploy FE to Vercel           │  │  │  │
│  │  │  │  • Build (Maven + Vite)             │  │  • Deploy BE to Azure App Srv    │  │  │  │
│  │  │  └──────────────────────────────────────┘  │  • Run Flyway migrations         │  │  │  │
│  │  │                                            └──────────────────────────────────┘  │  │  │
│  │  └─────────────────────────────────────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────────┐               │
│  │  DEVELOPMENT (Local Machine)                                            │               │
│  │                                                                         │               │
│  │  ┌──────────────────┐     ┌──────────────────┐     ┌────────────────┐   │               │
│  │  │  Vite Dev Server │────▶│ Spring Boot 3     │────▶│ MySQL 8 (Docker│   │               │
│  │  │  localhost:5173  │     │ localhost:8080    │     │ or local)      │   │               │
│  │  └──────────────────┘     └──────────────────┘     │ port: 3306     │   │               │
│  │                                                    └────────────────┘   │               │
│  └──────────────────────────────────────────────────────────────────────────┘               │
│                                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────────────────────┐  │
│  │  STAGING (Azure + Vercel)                                                              │  │
│  │                                                                                       │  │
│  │  ┌─────────────────────────────────────┐    ┌─────────────────────────────────────┐   │  │
│  │  │  Vercel (Preview Deployment)        │    │  Azure App Service (B1: 1 vCPU,      │   │  │
│  │  │  URL: preview-xxx.skillenroll.app   │    │  2 GB RAM)                          │   │  │
│  │  │  • Auto-deployed per PR             │    │  URL: staging-api.skillenroll.app   │   │  │
│  │  │  • Environment variables from Vercel│    │  • Java 17 SE Runtime               │   │  │
│  │  │  • SSL auto-enabled                 │    │  • JAR deployment                   │   │  │
│  │  └─────────────────────────────────────┘    │  • App Settings (env vars)          │   │  │
│  │                                              │  • Always On: true                 │   │  │
│  │                                              └──────────────┬──────────────────────┘   │  │
│  │                                                             │                          │  │
│  │                                                             ▼                          │  │
│  │                                              ┌─────────────────────────────────────┐   │  │
│  │                                              │  Azure Database for MySQL           │   │  │
│  │                                              │  (Burstable B2s: 2 vCPU, 4 GB)     │   │  │
│  │                                              │  • Auto-backups: daily              │   │  │
│  │                                              │  • SSL enforced                    │   │  │
│  │                                              │  • Same VNet as App Service        │   │  │
│  │                                              └─────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                             │
│  ┌──────────────────────────────────────────────────────────────────────────────────────┐  │
│  │  PRODUCTION (Azure + Vercel)                                                           │  │
│  │                                                                                       │  │
│  │  ┌─────────────────────────────────────┐    ┌─────────────────────────────────────┐   │  │
│  │  │  Vercel (Production)                │    │  Azure App Service (S2: 2 vCPU,      │   │  │
│  │  │  URL: app.skillenroll.app           │    │  4 GB RAM)                          │   │  │
│  │  │  • Auto-scaled globally (CDN)       │    │  URL: api.skillenroll.app           │   │  │
│  │  │  • Custom domain + SSL             │    │  • Auto-scale: 2–10 instances        │   │  │
│  │  │  • DDoS protection                 │    │  • Health check endpoint             │   │  │
│  │  │  • Environment variables:          │    │  • Staging slots for blue-green      │   │  │
│  │  │    VITE_API_URL → api.skillenroll  │    │  • Application Insights enabled      │   │  │
│  │  └─────────────────────────────────────┘    └──────────────┬──────────────────────┘   │  │
│  │                                                             │                          │  │
│  │                                                             ▼                          │  │
│  │                                              ┌─────────────────────────────────────┐   │  │
│  │                                              │  Azure Database for MySQL           │   │  │
│  │                                              │  (General Purpose D2ds v4:          │   │  │
│  │                                              │   2 vCPU, 8 GB, 100 GB storage)    │   │  │
│  │                                              │  • Geo-redundant backups: daily     │   │  │
│  │                                              │  • Read replica for reports (future)│   │  │
│  │                                              │  • Private endpoint (VNet)          │   │  │
│  │                                              │  • SSL/TLS enforced                │   │  │
│  │                                              └─────────────────────────────────────┘   │  │
│  │                                                                                       │  │
│  │  ┌──────────────────────────────────────────────────────────────────────────────┐   │  │
│  │  │  Azure Blob Storage                                                          │   │  │
│  │  │  • program-thumbnails: Public read, private write                             │   │  │
│  │  │  • lesson-materials: Private read/write (accessed via SAS tokens)             │   │  │
│  │  │  • assignment-submissions: Private read/write (per-learner folders)           │   │  │
│  │  │  • certificate-templates: Private read (template storage)                    │   │  │
│  │  └──────────────────────────────────────────────────────────────────────────────┘   │  │
│  │                                                                                       │  │
│  │  ┌──────────────────────────────────────────────────────────────────────────────┐   │  │
│  │  │  Azure Key Vault                                                              │   │  │
│  │  │  • JWT signing secret                                                         │   │  │
│  │  │  • Database connection string                                                 │   │  │
│  │  │  • SMTP credentials (email service)                                           │   │  │
│  │  │  • Azure Storage connection string                                            │   │  │
│  │  └──────────────────────────────────────────────────────────────────────────────┘   │  │
│  └──────────────────────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 CI/CD Pipeline Steps

| Step | Action | Tools |
|------|--------|-------|
| **1. Code Push** | Developer pushes code to a feature branch | Git |
| **2. CI Trigger** | GitHub Actions runs on push/PR to main | GitHub Actions |
| **3. Lint** | ESLint (frontend) + Checkstyle (backend) | ESLint, Checkstyle |
| **4. Test** | Vitest (FE unit tests) + JUnit 5 + Mockito (BE) | Vitest, JUnit, Mockito |
| **5. Build** | `npm run build` (FE) + `mvn clean package` (BE) | Vite, Maven |
| **6. Security Scan** | Dependency vulnerability scan (Dependabot / Snyk) | Dependabot |
| **7. Deploy (Staging)** | Auto-deploy main to staging environment | Vercel CLI + Azure WebApp Deploy |
| **8. E2E Tests** | Run end-to-end tests against staging (if configured) | Playwright / Cypress |
| **9. Deploy (Production)** | Manual trigger or tag-based release | Vercel CLI + Azure WebApp Deploy |
| **10. Migration** | Flyway runs automatically on Spring Boot startup | Flyway |

### 5.3 Network Architecture

```
Internet
    │
    ├──► Vercel CDN (Global Edge Network)
    │       │
    │       ├──► app.skillenroll.app (Production)
    │       └──► preview-*.skillenroll.app (PR previews)
    │
    └──► Azure Front Door (Optional, future)
            │
            └──► Azure App Service
                    │
                    ├──► Azure Database for MySQL (Private Endpoint)
                    └──► Azure Blob Storage (Private Endpoint)
```

---

## 6. Module Deep-Dive

This section explains each domain module in detail — its responsibilities, key entities, and interactions with other modules.

### 6.1 Authentication & Authorization Module (AUTH)

**Responsibility:** Manage user identity, authentication, and authorization across the platform.

**Key Entities (Conceptual):**
- `User` — Core identity record (UUID, email, hashed password, display name, role, status, created date)
- `Role` — Enum: `ROLE_GUEST`, `ROLE_LEARNER`, `ROLE_INSTRUCTOR`, `ROLE_ADMIN`
- `RefreshToken` — Opaque token for refreshing sessions (token hash, user ID, expiry, revoked flag)
- `PasswordResetToken` — Time-limited token for password reset flows

**Module Interactions:**
- **→ User Module:** Auth creates the initial user record during registration
- **→ All Modules:** Auth validates JWT tokens and provides `SecurityContext` for authorization decisions
- **→ Notification Module:** Auth triggers welcome emails, password reset emails

**Key Flows:**
- **Registration:** Guest submits form → Auth creates unverified User → sends OTP → on OTP verification, marks user as verified
- **Login:** User submits credentials → Auth verifies BCrypt hash → generates access + refresh tokens → returns to client
- **Authorization:** Every request passes through `JwtAuthenticationFilter` → Spring Security `@PreAuthorize` checks the required role

---

### 6.2 User Module (USER)

**Responsibility:** Manage user profiles, preferences, and account lifecycle.

**Key Entities (Conceptual):**
- `UserProfile` — Extended profile data (avatar URL, bio, contact number, timezone)
- `UserPreference` — Notification preferences (email opt-in/out per event type)
- `UserSession` — Active session tracking (device info, last activity, IP address)

**Module Interactions:**
- **← Auth Module:** Receives user identity after authentication
- **→ Enrollment Module:** Provides learner details for enrollment rosters
- **→ Reporting Module:** Contributes user data for reports (active users, registrations over time)
- **→ Admin Module:** Admin CRUD operations on users

**Key Flows:**
- **Profile Update:** User edits name/avatar → User module validates and persists changes
- **Session Management:** User views active sessions → can revoke a session (invalidates refresh token)
- **Account Deactivation:** User requests deactivation → User module soft-deletes the account; Admin can reactivate

---

### 6.3 Catalog Module (CAT)

**Responsibility:** Provide a browsable, searchable, filterable listing of all published programs.

**Key Entities (Conceptual):**
- `Program` — Core program record (title, description, skill level, duration, thumbnail URL, status)
- `Category` — Taxonomy for program classification (e.g., "Web Development", "Data Science")
- `ProgramReview` — Learner ratings and reviews for published programs
- `ProgramPreview` — Preview content (sample video, sample lesson) visible to guests

**Module Interactions:**
- **→ Program Management Module:** Reads program data for display (only published programs)
- **← Enrollment Module:** Updates enrolled count on program cards
- **← Reporting Module:** Feeds popular/trending program data

**Key Flows:**
- **Browse:** Guest/User requests catalog page → Catalog module paginates and returns programs with basic info
- **Search:** User enters keyword → Catalog module performs full-text search on title, description, instructor name → ranks by relevance
- **Filter:** User selects filters → Catalog module dynamically builds query with category, level, duration, price constraints
- **Detail View:** User clicks a program → Catalog module returns full program details including syllabus outline from the Program Management module

---

### 6.4 Program Management Module (PM)

**Responsibility:** Enable instructors to create, structure, publish, and maintain skill programs.

**Key Entities (Conceptual):**
- `Program` — Same core entity as Catalog, but with extended fields (status, version, approval status)
- `Module` — A top-level section within a program (title, description, order index)
- `Lesson` — A individual content unit within a module (title, content type, content body/file URL, order index, estimated duration)
- `ProgramVersion` — Version tracking for published program updates
- `ProgramDraft` — Draft changes pending publication

**Module Interactions:**
- **← Catalog Module:** Shares published program data for public listing
- **→ Learning Module:** Provides structured curriculum for enrolled learners
- **→ Assessment Module:** Provides lesson context for assessments (quizzes attached to lessons)
- **← Admin Module:** Receives approval/rejection decisions

**Key Flows:**
- **Program Creation:** Instructor fills program metadata → creates modules → creates lessons within modules → uploads materials → saves draft
- **Curriculum Building:** Instructor adds/removes/reorders modules and lessons via drag-and-drop; structure is persisted atomically
- **Publishing:** Instructor clicks "Publish" → PM validates that program has ≥ 1 module with ≥ 1 lesson → changes status to "Published" (or "Pending Approval" if moderation is enabled)
- **Draft-only Concurrent Editing:** An instructor can have at most one active draft per program at any time. If an instructor edits a published program, the changes are saved as a new draft version. The previous published version remains live for enrolled learners until the draft is published. There is no branching or multi-draft support in MVP.
- **Version Snapshots:** Each publish creates a version snapshot. Enrolled learners are pinned to the version that was live when they enrolled. This ensures their learning experience does not change mid-program.

---

### 6.5 Enrollment Module (ENR)

**Responsibility:** Manage the lifecycle of learner enrollment in programs.

**Key Entities (Conceptual):**
- `Enrollment` — Core enrollment record (learner ID, program ID, enrolled date, status [active, withdrawn, completed])
- `WaitlistEntry` — Waitlist position for full programs (learner ID, program ID, joined date, notified flag)

**Module Interactions:**
- **← Catalog Module:** Reads program availability and capacity
- **→ Learning Module:** Creates the initial learning record (unlocked modules) upon enrollment
- **→ Notification Module:** Triggers enrollment confirmation notifications
- **→ Reporting Module:** Provides enrollment data for analytics

**Key Flows:**
- **Enrollment:** Learner clicks "Enroll Now" → checks program capacity → creates Enrollment record with status "active" → initializes learning progress (all modules locked except first) → sends confirmation
- **Unenrollment:** Learner clicks "Unenroll" → checks if within cancellation window → marks Enrollment as "withdrawn" → retains progress data for 30 days
- **Waitlist:** If program is full → creates WaitlistEntry → when slot opens, notifies next learner → 48-hour window to enroll

---

### 6.6 Learning Module (LRN)

**Responsibility:** Deliver program content to enrolled learners and track their progress.

**Key Entities (Conceptual):**
- `LearningProgress` — Per-learner, per-program progress record (enrollment ID, current module, current lesson, completion percentage)
- `ModuleProgress` — Per-learner, per-module status (not started, in progress, completed)
- `LessonProgress` — Per-learner, per-lesson completion tracking (completed date, time spent, notes)
- `Bookmark` — Learner's last-viewed position per program
- `LearnerNote` — Personal notes attached to a specific lesson

**Module Interactions:**
- **← Enrollment Module:** Receives enrollment events to initialize progress tracking
- **← Program Management Module:** Reads curriculum structure for rendering
- **→ Assessment Module:** Directs learners to assessments within the learning flow

**Key Flows:**
- **Learning View:** Learner opens a program → LRN loads curriculum tree with progress indicators → shows current/next lesson
- **Lesson Completion:** Learner views lesson content → LRN marks lesson as "completed" → updates ModuleProgress → if all lessons in module complete, marks module "completed" → unlocks next module
- **Resume:** Learner re-enters program → LRN loads the last bookmarked lesson
- **Notes:** Learner adds a note to a lesson → LRN persists note privately

---

### 6.7 Assessment Module (ASM)

**Responsibility:** Enable creation, delivery, and grading of quizzes and assignments.

**Key Entities (Conceptual):**
- `Quiz` — Assessment configuration (title, time limit, passing score, max attempts, shuffle questions flag)
- `Question` — Individual question within a quiz (type, prompt, options, correct answer, point value, explanation)
- `QuizAttempt` — A learner's attempt at a quiz (learner ID, quiz ID, started at, submitted at, score, passed flag)
- `Answer` — Individual answer within an attempt (question ID, selected/submitted answer, score earned, graded flag)
- `Assignment` — A task requiring submission (lesson ID, instructions, due date, max file size, allowed file types)
- `AssignmentSubmission` — Learner's submitted work (assignment ID, learner ID, file URL, text response, submitted at, grade, feedback)

**Module Interactions:**
- **← Learning Module:** Assessments are associated with lessons; ASM provides assessment status to the learning view
- **→ Notification Module:** Triggers grade-posted notifications
- **→ Reporting Module:** Feeds grade data for reports

**Key Flows:**
- **Quiz Creation:** Instructor creates quiz → adds questions (multiple choice, true/false, short answer) → configures time limit, passing score, attempts → associates quiz with a lesson
- **Quiz Taking:** Learner navigates to quiz → timer starts → learner answers questions → submits (or auto-submit on timer expiry) → auto-graded questions scored → short answers flagged for manual grading → score displayed
- **Grading:** Instructor reviews flagged questions → enters scores → provides feedback → ASM marks submission as graded → sends notification to learner
- **Grade Book:** Learner views all assessment scores per program; Instructor views aggregate scores

---

### 6.8 Progress & Reporting Module (RPT)

**Responsibility:** Aggregate learning data and present actionable dashboards and reports.

**Key Entities (Conceptual):**
- `DashboardSummary` — Derived view: total enrolled, completed, in-progress counts per learner
- `ProgramAnalytics` — Derived view: enrollment count, avg progress %, avg score, completion rate per program
- `SystemReport` — System-wide metrics snapshot (generated on demand or scheduled)
- `Certificate` — Generated certificate record (certificate ID, learner ID, program ID, completion date, verification hash, revoked flag)

**Module Interactions:**
- **← Enrollment Module:** Reads enrollment data for dashboard counts
- **← Learning Module:** Reads progress data for completion percentages
- **← Assessment Module:** Reads grade data for score analytics
- **→ Certificate Generation:** Creates certificates when progress and assessments are fully completed

**Key Flows:**
- **Learner Dashboard:** Learner navigates to dashboard → RPT queries enrollment, progress, and certificate data → constructs summary view
- **Instructor Reports:** Instructor selects program → RPT queries all enrollments with progress and grades → calculates aggregates and at-risk list
- **Admin Reports:** Admin views system-wide metrics → RPT runs aggregated queries across all programs and users
- **Certificate Generation:** On 100 % completion and passing all required assessments → RPT generates PDF with certificate data → stores certificate record → provides verification endpoint
- **Report Export:** User requests CSV/PDF export → RPT runs the same query as the report view → formats as CSV or generates PDF document

---

### 6.9 Notification Module (NTF)

**Responsibility:** Deliver timely notifications to users via in-app and email channels.

**Key Entities (Conceptual):**
- `Notification` — A notification record (user ID, type [enrollment, grade, etc.], title, message, link URL, read flag, created date)
- `EmailTemplate` — Reusable email template (template name, subject, HTML body with placeholders)
- `NotificationPreference` — Per-user notification opt-in/out settings

**Module Interactions:**
- **← Enrollment Module:** Listens for enrollment events → creates enrollment confirmation notification
- **← Assessment Module:** Listens for grade-posted events → creates grade notification
- **← Admin Module:** Listens for user management events → creates account notifications
- **→ External Email Service:** Sends transactional emails via SMTP

**Key Flows:**
- **In-App Notification:** Event occurs (e.g., grade posted) → NTF creates Notification record with user ID and event details → user sees badge count on bell icon → user reads notification → marked as read
- **Email Notification:** Event occurs + user has email enabled → NTF renders email template with event data → queues email → sends via Spring Mail (SMTP) → logs send status
- **Preferences:** User updates notification preferences → NTF stores preference per event type → future notifications respect the preference
- **Digest:** (Future enhancement) NTF could batch non-urgent notifications into a daily/weekly digest

---

### 6.10 Admin Module (ADM)

**Responsibility:** Provide system administration capabilities for platform management.

**Key Entities (Conceptual):**
- `SystemConfig` — Key-value configuration store (enrollment policies, platform name, branding settings)
- `AuditLog` — Immutable log of admin actions (actor ID, action type, target entity, details, timestamp, IP address)
- `BulkOperation` — Batch job tracking (operation type, CSV file reference, status, results summary)

**Module Interactions:**
- **← User Module:** Admin CRUD operations on users
- **← Program Management Module:** Program moderation (approve/reject)
- **← All Modules:** Audit events sent from various modules for logging
- **→ Notification Module:** Sends notifications for admin actions (e.g., "Your program has been approved")

**Key Flows:**
- **User Management:** Admin views user list → searches/filters → clicks "Edit" → changes role → ADM updates user → writes audit log entry
- **Program Moderation:** Admin views pending programs → reviews details → approves or rejects (with reason) → PM updates program status → NTF sends notification to instructor
- **System Configuration:** Admin updates platform branding → ADM persists config → all modules read config on next load
- **Audit Trail:** Every admin action writes to AuditLog → logs are immutable (append-only) → admins can search and view audit history

---

## 7. Key Design Decisions

| Decision | Option Chosen | Rationale |
|----------|--------------|-----------|
| **Architecture Style** | Modular Monolith | Team size (4–5) and domain complexity do not justify microservices. Modular design allows future extraction. |
| **Communication Pattern** | In-process method calls (via Spring Services) | Simpler than message queues for MVP. Cross-module events use Spring `ApplicationEventPublisher` for loose coupling. |
| **Persistence Pattern** | Database-per-module (schema-level isolation) | Each module owns its tables. Cross-module joins are not allowed — data is joined in application code. This enforces bounded contexts. |
| **Authentication** | JWT (stateless) | Enables horizontal scaling without sticky sessions. Refresh token rotation mitigates token theft. |
| **API Style** | REST (not GraphQL) | CRUD-heavy domain is well-served by REST. OpenAPI provides clear documentation. GraphQL adds complexity without proportional benefit for this use case. |
| **Caching Strategy** | Spring Cache (in-memory) for MVP; Redis for future | In-memory caching is sufficient for the expected scale (500 concurrent users). Redis adds operational complexity that is unnecessary in year 1. |
| **File Storage** | Azure Blob Storage (not local filesystem) | Decouples storage from compute. Enables horizontal scaling (any instance can serve any file). SAS tokens provide secure, time-limited access. |
| **Error Handling** | Global `@ControllerAdvice` exception handler | Consistent error response format across all APIs. Domain exceptions (e.g., `EnrollmentFullException`) map to appropriate HTTP status codes. |
| **Module Communication** | Spring events (sync) for MVP; message queue (RabbitMQ) for future | Synchronous events are simpler to debug and transactionally safe. Asynchronous messaging can be introduced when modules are extracted to separate services. |

---

> **Next Document:** `04_API_SPECIFICATION.md` — Full REST API endpoint definitions, request/response schemas, and integration patterns.
