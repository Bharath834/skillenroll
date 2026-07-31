# 04_DATABASE.md — SkillEnroll

> **Document Version:** 1.0  
> **Author:** Senior Software Architect  
> **Framework:** TrainingMug AI Development Framework  
> **Status:** ✅ Approved  
> **Related Docs:** [01_PROJECT_CONTEXT.md](./01_PROJECT_CONTEXT.md) · [02_REQUIREMENTS.md](./02_REQUIREMENTS.md) · [03_ARCHITECTURE.md](./03_ARCHITECTURE.md)

---

## Table of Contents

1. [Database Overview](#1-database-overview)
2. [Entity-Relationship Diagram](#2-entity-relationship-diagram)
3. [Table Definitions](#3-table-definitions)
4. [Primary Keys](#4-primary-keys)
5. [Foreign Keys](#5-foreign-keys)
6. [Indexes](#6-indexes)
7. [Constraints](#7-constraints)
8. [Normalization](#8-normalization)
9. [MySQL Engine & Charset](#9-mysql-engine--charset)

---

## 1. Database Overview

### 1.1 Technology

| Property | Value |
|----------|-------|
| **RDBMS** | MySQL 8.0+ |
| **Storage Engine** | InnoDB (default for all tables) |
| **Default Charset** | `utf8mb4` — full Unicode support including emoji in lesson content and user bios |
| **Default Collation** | `utf8mb4_unicode_ci` — case-insensitive, language-neutral sorting |
| **Connection Pool** | HikariCP (managed by Spring Boot) |
| **Migration Tool** | Flyway (version-controlled, repeatable schema migrations) |

### 1.2 Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| **Table names** | `snake_case`, plural nouns | `users`, `programs`, `enrollments` |
| **Column names** | `snake_case`, descriptive | `created_at`, `is_published`, `full_name` |
| **Primary keys** | `id` (auto-increment BIGINT) for all tables | `id` |
| **Foreign keys** | `<referenced_table_singular>_id` | `user_id`, `program_id` |
| **Indexes** | `idx_<table>_<column>` | `idx_users_email` |
| **Unique constraints** | `uq_<table>_<column>` | `uq_users_email` |
| **Timestamps** | `created_at`, `updated_at` on every table | `created_at DATETIME(3)` |

### 1.3 Table Inventory

The SkillEnroll database consists of **25 tables** organized across 10 domain modules:

| # | Table Name | Module | Purpose |
|---|------------|--------|---------|
| 1 | `users` | AUTH | Core user identity and authentication |
| 2 | `refresh_tokens` | AUTH | JWT refresh token storage |
| 3 | `password_reset_tokens` | AUTH | Time-limited password reset links |
| 4 | `email_verifications` | AUTH | Email OTP verification records |
| 5 | `user_profiles` | USER | Extended user profile data |
| 6 | `user_preferences` | USER | User notification and UI preferences |
| 7 | `user_sessions` | USER | Active session tracking |
| 8 | `categories` | CAT | Program classification taxonomy |
| 9 | `programs` | CAT / PM | Core program/course records |
| 10 | `program_versions` | PM | Version snapshots for published programs |
| 11 | `program_drafts` | PM | Draft changes pending publication |
| 12 | `program_reviews` | CAT | Learner ratings and reviews |
| 13 | `modules` | PM | Top-level curriculum sections within a program |
| 14 | `lessons` | PM | Individual content units within a module |
| 15 | `lesson_materials` | PM | Attached files/resources for a lesson |
| 16 | `enrollments` | ENR | Learner enrollment records |
| 17 | `waitlist_entries` | ENR | Waitlist positions for full programs |
| 18 | `learning_progress` | LRN | Per-learner, per-program progress tracking |
| 19 | `learner_notes` | LRN | Personal notes attached to lessons |
| 20 | `quizzes` | ASM | Quiz/assessment configurations |
| 21 | `quiz_questions` | ASM | Individual questions within a quiz |
| 22 | `quiz_attempts` | ASM | Learner quiz attempt records |
| 23 | `assignment_submissions` | ASM | Learner assignment submissions |
| 24 | `certificates` | RPT | Generated completion certificates |
| 25 | `notifications` | NTF | In-app notification records |
| 26 | `audit_logs` | ADM | Immutable admin action audit trail |
| 27 | `system_config` | ADM | Key-value platform configuration store |

---

## 2. Entity-Relationship Diagram

### 2.1 Overall ER Diagram

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                          SKILLENROLL — ENTITY-RELATIONSHIP DIAGRAM                    │
│                                                                                       │
│  ┌───────────┐     ┌──────────────────┐     ┌──────────────────┐     ┌────────────┐  │
│  │  users    │1──N│ refresh_tokens   │     │ categories       │     │ programs   │  │
│  │           │     │                  │     │                  │     │            │  │
│  │ PK: id    │     │ PK: id           │     │ PK: id           │     │ PK: id     │  │
│  │ email (UQ)│     │ FK: user_id      │     │ name (UQ)        │1──N │ FK: cat_id  │  │
│  │ role (ENUM)│    └──────────────────┘     └──────────────────┘     │ FK: instr_id│  │
│  └─────┬─────┘                                                      └──────┬──────┘  │
│        │                                                                  │          │
│        │1─────────────────────────────────────────────────────────────────┤          │
│        │                          (instructor_id)                                     │
│        │                                                                             │
│        │1─────────────────────────────────────────────────────────────┐             │
│        │  (user_id)                                                  │             │
│        ▼                                                             ▼             │
│  ┌───────────┐     ┌──────────────────┐     ┌──────────────────┐     ┌────────────┐  │
│  │ user_     │     │ user_            │     │ user_            │     │ program_   │  │
│  │ profiles  │     │ preferences      │     │ sessions         │     │ versions   │  │
│  │           │     │                  │     │                  │     │            │  │
│  │ PK: id    │     │ PK: id           │     │ PK: id           │     │ PK: id     │  │
│  │ FK: user_id│    │ FK: user_id      │     │ FK: user_id      │     │ FK: prog_id │  │
│  └───────────┘     └──────────────────┘     └──────────────────┘     └────────────┘  │
│                                                                                       │
│        ┌─────────────────────────────────────────────────────────────────────┐        │
│        │                    PROGRAM CURRICULUM                               │        │
│        │                                                                    │        │
│        │  ┌──────────┐     ┌──────────┐     ┌───────────┐                  │        │
│        │  │ programs │1──N│ modules  │1──N│ lessons   │                   │        │
│        │  │          │     │          │     │           │                   │        │
│        │  │ PK: id   │     │ PK: id   │     │ PK: id    │                   │        │
│        │  └──────────┘     │ FK: prog_│     │ FK: mod_id│                   │        │
│        │                   └──────────┘     └─────┬─────┘                   │        │
│        │                                          │1                        │        │
│        │                                          │                         │        │
│        │                                          ▼                         │        │
│        │                                   ┌──────────────┐                 │        │
│        │                                   │ lesson_      │                 │        │
│        │                                   │ materials    │                 │        │
│        │                                   │              │                 │        │
│        │                                   │ PK: id       │                 │        │
│        │                                   │ FK: lesson_id│                 │        │
│        │                                   └──────────────┘                 │        │
│        └─────────────────────────────────────────────────────────────────────┘        │
│                                                                                       │
│        ┌─────────────────────────────────────────────────────────────────────┐        │
│        │                    ENROLLMENT & LEARNING                            │        │
│        │                                                                    │        │
│        │  ┌────────────┐     ┌──────────────────┐     ┌──────────────────┐  │        │
│        │  │ enrollments│1──N│ learning_progress │     │ learner_notes    │  │        │
│        │  │            │     │                  │     │                  │  │        │
│        │  │ PK: id     │     │ PK: id           │     │ PK: id           │  │        │
│        │  │ FK: user_id│     │ FK: enrollment_id│     │ FK: user_id      │  │        │
│        │  │ FK: prog_id│     │ FK: module_id    │     │ FK: lesson_id    │  │        │
│        │  └────────────┘     └──────────────────┘     └──────────────────┘  │        │
│        │                                                                    │        │
│        │  ┌──────────────────┐                                               │        │
│        │  │ waitlist_entries │                                               │        │
│        │  │                  │                                               │        │
│        │  │ PK: id           │                                               │        │
│        │  │ FK: user_id      │                                               │        │
│        │  │ FK: program_id   │                                               │        │
│        │  └──────────────────┘                                               │        │
│        └─────────────────────────────────────────────────────────────────────┘        │
│                                                                                       │
│        ┌─────────────────────────────────────────────────────────────────────┐        │
│        │                    ASSESSMENTS                                      │        │
│        │                                                                    │        │
│        │  ┌──────────┐     ┌──────────────────┐     ┌──────────────────┐    │        │
│        │  │ quizzes  │1──N│ quiz_questions    │     │ quiz_attempts    │    │        │
│        │  │          │     │                  │     │                  │    │        │
│        │  │ PK: id   │     │ PK: id           │     │ PK: id           │    │        │
│        │  │ FK: les_ │     │ FK: quiz_id      │     │ FK: quiz_id      │    │        │
│        │  └──────────┘     └──────────────────┘     │ FK: user_id      │    │        │
│        │                                             └────────┬─────────┘    │        │
│        │                                                      │1             │        │
│        │                                                      │              │        │
│        │                                             ┌────────▼─────────┐    │        │
│        │                                             │ answers          │    │        │
│        │                                             │                  │    │        │
│        │                                             │ PK: id           │    │        │
│        │                                             │ FK: attempt_id   │    │        │
│        │                                             │ FK: question_id  │    │        │
│        │                                             └──────────────────┘    │        │
│        │                                                                    │        │
│        │  ┌─────────────────────────────────────────────────────────────┐   │        │
│        │  │ assignment_submissions                                       │   │        │
│        │  │                                                             │   │        │
│        │  │ PK: id | FK: lesson_id | FK: user_id | FK: grader_id       │   │        │
│        │  └─────────────────────────────────────────────────────────────┘   │        │
│        └─────────────────────────────────────────────────────────────────────┘        │
│                                                                                       │
│        ┌─────────────────────────────────────────────────────────────────────┐        │
│        │                    REPORTING & NOTIFICATIONS                        │        │
│        │                                                                    │        │
│        │  ┌──────────────┐     ┌──────────────────┐     ┌──────────────┐    │        │
│        │  │ certificates │     │ notifications    │     │ audit_logs   │    │        │
│        │  │              │     │                  │     │              │    │        │
│        │  │ PK: id       │     │ PK: id           │     │ PK: id       │    │        │
│        │  │ FK: user_id  │     │ FK: user_id      │     │ FK: actor_id │    │        │
│        │  │ FK: prog_id  │     └──────────────────┘     └──────────────┘    │        │
│        │  └──────────────┘                                                    │        │
│        │                                                                    │        │
│        │  ┌──────────────────┐     ┌──────────────────┐                    │        │
│        │  │ program_reviews  │     │ system_config   │                    │        │
│        │  │                  │     │                  │                    │        │
│        │  │ PK: id           │     │ PK: id           │                    │        │
│        │  │ FK: user_id      │     │ key (UQ)         │                    │        │
│        │  │ FK: program_id   │     └──────────────────┘                    │        │
│        │  └──────────────────┘                                              │        │
│        └─────────────────────────────────────────────────────────────────────┘        │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Legend

| Symbol | Meaning |
|--------|---------|
| `PK` | Primary Key |
| `FK` | Foreign Key |
| `UQ` | Unique Constraint |
| `1──N` | One-to-Many relationship |
| `1──1` | One-to-One relationship |
| `N──M` | Many-to-Many (resolved with junction table) |

---

## 3. Table Definitions

### 3.1 `users` — Core User Identity (AUTH Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `uuid` | `CHAR(36)` | NOT NULL | UUID() | Public-facing unique identifier (never expose internal `id`) |
| `email` | `VARCHAR(255)` | NOT NULL | — | Login email |
| `password_hash` | `VARCHAR(255)` | NOT NULL | — | BCrypt hashed password (cost factor ≥ 12) |
| `full_name` | `VARCHAR(150)` | NOT NULL | — | Display name |
| `role` | `ENUM('LEARNER','INSTRUCTOR','ADMIN')` | NOT NULL | 'LEARNER' | Authorization role |
| `status` | `ENUM('ACTIVE','SUSPENDED','DEACTIVATED')` | NOT NULL | 'ACTIVE' | Account status |
| `is_email_verified` | `TINYINT(1)` | NOT NULL | 0 | Whether email has been verified via OTP |
| `last_login_at` | `DATETIME(3)` | YES | NULL | Last successful login timestamp |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** AUTH  
**Key Queries:** Login (by email), User management list (by role/status), Duplicate check (by email)

---

### 3.2 `refresh_tokens` — JWT Refresh Token Store (AUTH Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id |
| `token_hash` | `VARCHAR(255)` | NOT NULL | — | SHA-256 hash of the opaque refresh token |
| `expires_at` | `DATETIME(3)` | NOT NULL | — | 7 days from issue |
| `is_revoked` | `TINYINT(1)` | NOT NULL | 0 | Whether the token was explicitly revoked |
| `replaced_by_token_hash` | `VARCHAR(255)` | YES | NULL | Token rotation chain (points to new token) |
| `device_info` | `VARCHAR(500)` | YES | NULL | User-agent string for display in session management |
| `ip_address` | `VARCHAR(45)` | YES | NULL | IPv4 or IPv6 address |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |

**Module:** AUTH  
**Key Queries:** Find valid token (by hash), Revoke all tokens for user, Cleanup expired tokens

---

### 3.3 `password_reset_tokens` — Password Reset (AUTH Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id |
| `token_hash` | `VARCHAR(255)` | NOT NULL | — | SHA-256 hash of reset token |
| `expires_at` | `DATETIME(3)` | NOT NULL | — | 1 hour from issue |
| `is_used` | `TINYINT(1)` | NOT NULL | 0 | Whether the token was already consumed |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |

**Module:** AUTH  
**Cleanup:** Delete rows where `expires_at < NOW()` (handled by scheduled task)

---

### 3.4 `email_verifications` — Email OTP Verification (AUTH Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id |
| `otp` | `CHAR(6)` | NOT NULL | — | 6-digit numeric OTP |
| `expires_at` | `DATETIME(3)` | NOT NULL | — | 15 minutes from issue |
| `attempts` | `TINYINT UNSIGNED` | NOT NULL | 0 | Number of failed verification attempts |
| `is_verified` | `TINYINT(1)` | NOT NULL | 0 | Whether OTP was successfully verified |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |

**Module:** AUTH  
**Rate Limit:** Max 5 failed attempts before invalidating the OTP

---

### 3.5 `user_profiles` — Extended Profile (USER Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id (1:1) |
| `bio` | `TEXT` | YES | NULL | Short biography |
| `avatar_url` | `VARCHAR(500)` | YES | NULL | URL to profile image (stored in Azure Blob) |
| `contact_number` | `VARCHAR(20)` | YES | NULL | Phone number |
| `timezone` | `VARCHAR(50)` | NOT NULL | 'UTC' | User's timezone (for scheduling) |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** USER  
**Relationship:** One-to-One with `users` (each user has exactly one profile)

---

### 3.6 `user_preferences` — User Preferences (USER Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id (1:1) |
| `email_notifications` | `JSON` | NOT NULL | — | JSON: `{"enrollment": true, "grade": true, "new_content": false, "certificate": true}` |
| `ui_preferences` | `JSON` | YES | NULL | JSON: UI preferences (theme, sidebar state) |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** USER  
**Relationship:** One-to-One with `users`  
**Storage:** JSON column for flexible preference structure (no schema changes needed for new notification types)

---

### 3.7 `user_sessions` — Active Session Tracking (USER Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id |
| `refresh_token_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to refresh_tokens.id |
| `device_name` | `VARCHAR(200)` | YES | NULL | Parsed device name from user-agent |
| `ip_address` | `VARCHAR(45)` | YES | NULL | Last known IP address |
| `last_activity_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Last request timestamp |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Session started timestamp |

**Module:** USER  
**Key Query:** List active sessions for user (joined with refresh_tokens for "revoke" action)

---

### 3.8 `categories` — Program Taxonomy (CAT Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `name` | `VARCHAR(100)` | NOT NULL | — | Category name (e.g., "Web Development") |
| `slug` | `VARCHAR(120)` | NOT NULL | — | URL-friendly identifier (e.g., "web-development") |
| `description` | `VARCHAR(500)` | YES | NULL | Brief category description |
| `icon_url` | `VARCHAR(500)` | YES | NULL | Category icon/image |
| `display_order` | `INT UNSIGNED` | NOT NULL | 0 | Order in catalog filter UI |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** CAT  
**Unique:** `name` and `slug` each have unique constraints

---

### 3.9 `programs` — Core Program/Course Record (CAT / PM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `instructor_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id (the instructor who created it) |
| `category_id` | `BIGINT UNSIGNED` | YES | NULL | FK to categories.id |
| `title` | `VARCHAR(255)` | NOT NULL | — | Program title |
| `slug` | `VARCHAR(280)` | NOT NULL | — | URL-friendly identifier |
| `summary` | `VARCHAR(300)` | NOT NULL | — | Short description (card view, max 300 chars) |
| `description` | `TEXT` | YES | NULL | Full program description (supports HTML/markdown) |
| `skill_level` | `ENUM('BEGINNER','INTERMEDIATE','ADVANCED')` | NOT NULL | 'BEGINNER' | Target skill level |
| `duration_hours` | `DECIMAL(6,1)` | YES | NULL | Estimated total effort in hours |
| `prerequisites` | `TEXT` | YES | NULL | Free-text prerequisites description |
| `thumbnail_url` | `VARCHAR(500)` | YES | NULL | Program thumbnail image URL |
| `preview_video_url` | `VARCHAR(500)` | YES | NULL | Optional preview video URL |
| `is_published` | `TINYINT(1)` | NOT NULL | 0 | Whether the program is visible to learners |
| `is_archived` | `TINYINT(1)` | NOT NULL | 0 | Whether the program is archived |
| `approval_status` | `ENUM('PENDING','APPROVED','REJECTED')` | NOT NULL | 'APPROVED' | Moderation status |
| `max_enrollments` | `INT UNSIGNED` | YES | NULL | Max capacity (NULL = unlimited) |
| `current_enrollment_count` | `INT UNSIGNED` | NOT NULL | 0 | Denormalized count for catalog display |
| `average_rating` | `DECIMAL(2,1)` | NOT NULL | 0.0 | Denormalized average rating (0.0–5.0) |
| `review_count` | `INT UNSIGNED` | NOT NULL | 0 | Denormalized count of reviews |
| `published_version` | `INT UNSIGNED` | NOT NULL | 1 | Current published version number |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** CAT / PM  
**Denormalized Columns:** `current_enrollment_count`, `average_rating`, `review_count` — updated via triggers or scheduled jobs to avoid expensive aggregate queries on every catalog page load

---

### 3.10 `program_versions` — Version Snapshots (PM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `program_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to programs.id |
| `version_number` | `INT UNSIGNED` | NOT NULL | — | Monotonically increasing version (1, 2, 3...) |
| `published_at` | `DATETIME(3)` | NOT NULL | — | When this version was published |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |

**Module:** PM  
**Unique:** `(program_id, version_number)`  
**Purpose:** Enrolled learners are pinned to the version that was live when they enrolled. This table records each publish event so progress can be tracked against a frozen curriculum snapshot.

---

### 3.11 `program_drafts` — Pending Draft Changes (PM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `program_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to programs.id |
| `draft_data` | `JSON` | NOT NULL | — | Full JSON snapshot of the draft curriculum (modules, lessons, materials) |
| `is_published` | `TINYINT(1)` | NOT NULL | 0 | Whether this draft was published |
| `published_version_id` | `BIGINT UNSIGNED` | YES | NULL | FK to program_versions.id (once published) |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Draft created timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modified timestamp |

**Module:** PM  
**Unique:** One active draft per program (enforced at application layer)

---

### 3.12 `program_reviews` — Learner Ratings & Reviews (CAT Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `program_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to programs.id |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id (the learner who reviewed) |
| `rating` | `TINYINT UNSIGNED` | NOT NULL | — | Rating 1–5 |
| `review_text` | `TEXT` | YES | NULL | Optional written review |
| `is_approved` | `TINYINT(1)` | NOT NULL | 1 | Whether the review is publicly visible |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modified timestamp |

**Module:** CAT  
**Unique:** `(program_id, user_id)` — one review per learner per program  
**Trigger:** On insert/update, recalculate `programs.average_rating` and `programs.review_count`

---

### 3.13 `modules` — Curriculum Sections (PM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `program_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to programs.id |
| `title` | `VARCHAR(255)` | NOT NULL | — | Module title |
| `description` | `TEXT` | YES | NULL | Optional module description |
| `sort_order` | `INT UNSIGNED` | NOT NULL | 0 | Display order within the program |
| `is_optional` | `TINYINT(1)` | NOT NULL | 0 | Whether module is optional (not required for completion) |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** PM  
**Unique:** `(program_id, sort_order)` — no two modules can have the same order within a program

---

### 3.14 `lessons` — Content Units (PM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `module_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to modules.id |
| `title` | `VARCHAR(255)` | NOT NULL | — | Lesson title |
| `content_type` | `ENUM('TEXT','VIDEO','DOCUMENT','LINK','QUIZ','ASSIGNMENT')` | NOT NULL | 'TEXT' | Type of lesson content |
| `content_body` | `LONGTEXT` | YES | NULL | Rich text content (HTML/markdown) — for TEXT type |
| `content_url` | `VARCHAR(500)` | YES | NULL | URL to external content (video, document, link) |
| `duration_minutes` | `INT UNSIGNED` | YES | NULL | Estimated time to complete this lesson |
| `sort_order` | `INT UNSIGNED` | NOT NULL | 0 | Display order within the module |
| `is_free_preview` | `TINYINT(1)` | NOT NULL | 0 | Whether this lesson is available to guests as preview |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** PM  
**Unique:** `(module_id, sort_order)` — no two lessons can have the same order within a module

---

### 3.15 `lesson_materials` — Attached Resources (PM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `lesson_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to lessons.id |
| `material_type` | `ENUM('PDF','IMAGE','VIDEO','AUDIO','ARCHIVE','OTHER')` | NOT NULL | — | File type category |
| `file_name` | `VARCHAR(255)` | NOT NULL | — | Original file name |
| `file_url` | `VARCHAR(500)` | NOT NULL | — | Azure Blob Storage URL (SAS token appended at access time) |
| `file_size_bytes` | `BIGINT UNSIGNED` | NOT NULL | 0 | File size in bytes |
| `display_name` | `VARCHAR(255)` | YES | NULL | User-friendly display name |
| `sort_order` | `INT UNSIGNED` | NOT NULL | 0 | Display order within the lesson |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |

**Module:** PM  
**Max File Size:** 50 MB (enforced at application layer)

---

### 3.16 `enrollments` — Learner Enrollment (ENR Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id (the learner) |
| `program_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to programs.id |
| `pinned_version_id` | `BIGINT UNSIGNED` | YES | NULL | FK to program_versions.id (curriculum snapshot at enrollment time) |
| `status` | `ENUM('ACTIVE','WITHDRAWN','COMPLETED')` | NOT NULL | 'ACTIVE' | Current enrollment status |
| `progress_percentage` | `DECIMAL(5,2)` | NOT NULL | 0.00 | Denormalized: overall progress 0.00–100.00 |
| `withdrawn_at` | `DATETIME(3)` | YES | NULL | When the learner withdrew |
| `completed_at` | `DATETIME(3)` | YES | NULL | When the learner completed the program |
| `enrolled_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Enrollment timestamp |

**Module:** ENR  
**Unique:** `(user_id, program_id)` — a learner can only enroll once per program

---

### 3.17 `waitlist_entries` — Enrollment Waitlist (ENR Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id |
| `program_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to programs.id |
| `position` | `INT UNSIGNED` | NOT NULL | — | Queue position |
| `notified_at` | `DATETIME(3)` | YES | NULL | When the user was notified of an available slot |
| `expires_at` | `DATETIME(3)` | YES | NULL | 48 hours after notification |
| `is_fulfilled` | `TINYINT(1)` | NOT NULL | 0 | Whether the user enrolled from the waitlist |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |

**Module:** ENR  
**Unique:** `(user_id, program_id)` — one waitlist entry per learner per program

---

### 3.18 `learning_progress` — Per-Learner Progress Tracking (LRN Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `enrollment_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to enrollments.id |
| `module_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to modules.id |
| `status` | `ENUM('NOT_STARTED','IN_PROGRESS','COMPLETED')` | NOT NULL | 'NOT_STARTED' | Module completion status |
| `started_at` | `DATETIME(3)` | YES | NULL | When the learner first accessed this module |
| `completed_at` | `DATETIME(3)` | YES | NULL | When all lessons in this module were completed |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** LRN  
**Unique:** `(enrollment_id, module_id)` — one progress record per module per enrollment

---

### 3.19 `learner_notes` — Personal Lesson Notes (LRN Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id |
| `lesson_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to lessons.id |
| `note_text` | `TEXT` | NOT NULL | — | The learner's personal note |
| `highlighted_text` | `TEXT` | YES | NULL | The original text that was highlighted (if applicable) |
| `color` | `VARCHAR(7)` | YES | '#FFFF00' | Highlight color (hex) |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** LRN  
**Privacy:** Notes are private — each learner can only see their own notes

---

### 3.20 `quizzes` — Assessment Configuration (ASM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `lesson_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to lessons.id |
| `title` | `VARCHAR(255)` | NOT NULL | — | Quiz title |
| `instructions` | `TEXT` | YES | NULL | Instructions shown to learner before starting |
| `time_limit_minutes` | `INT UNSIGNED` | YES | NULL | NULL = no time limit |
| `passing_score_percent` | `DECIMAL(5,2)` | NOT NULL | 60.00 | Score threshold to pass (0.00–100.00) |
| `max_attempts` | `INT UNSIGNED` | NOT NULL | 1 | Number of allowed attempts (NULL = unlimited) |
| `shuffle_questions` | `TINYINT(1)` | NOT NULL | 0 | Whether to randomize question order |
| `show_results_immediately` | `TINYINT(1)` | NOT NULL | 1 | Whether to show score/correct answers after submission |
| `total_points` | `INT UNSIGNED` | NOT NULL | 0 | Sum of all question point values (computed) |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** ASM  
**Relationship:** One quiz is associated with one lesson (via `lesson_id`)

---

### 3.21 `quiz_questions` — Quiz Questions (ASM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `quiz_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to quizzes.id |
| `question_type` | `ENUM('MULTIPLE_CHOICE_SINGLE','MULTIPLE_CHOICE_MULTI','TRUE_FALSE','SHORT_ANSWER','FILL_BLANK')` | NOT NULL | — | Question type |
| `question_text` | `TEXT` | NOT NULL | — | The question prompt (supports HTML for formatting) |
| `options` | `JSON` | YES | NULL | JSON array of options: `[{"key": "A", "text": "..."}, ...]` |
| `correct_answer` | `JSON` | NOT NULL | — | JSON: for single choice: `"A"`, for multi: `["A","C"]`, for short answer: `"expected text"` |
| `points` | `INT UNSIGNED` | NOT NULL | 1 | Point value for this question |
| `explanation` | `TEXT` | YES | NULL | Explanation shown after answering (if enabled) |
| `sort_order` | `INT UNSIGNED` | NOT NULL | 0 | Display order within the quiz |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** ASM  
**Unique:** `(quiz_id, sort_order)` — no two questions can have the same order within a quiz

---

### 3.22 `quiz_attempts` — Learner Quiz Attempts (ASM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `quiz_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to quizzes.id |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id |
| `attempt_number` | `INT UNSIGNED` | NOT NULL | — | Which attempt (1, 2, 3...) |
| `score` | `DECIMAL(5,2)` | YES | NULL | Total points earned (NULL if not yet graded/auto-graded) |
| `score_percentage` | `DECIMAL(5,2)` | YES | NULL | Score / total_points × 100 |
| `is_passed` | `TINYINT(1)` | YES | NULL | Whether score >= passing_score_percent |
| `is_graded` | `TINYINT(1)` | NOT NULL | 0 | Whether all questions have been graded (auto or manual) |
| `started_at` | `DATETIME(3)` | NOT NULL | — | When the learner started |
| `submitted_at` | `DATETIME(3)` | YES | NULL | When the learner submitted (NULL = in progress) |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |

**Module:** ASM  
**Unique:** `(quiz_id, user_id, attempt_number)` — no duplicate attempt numbers

---

### 3.23 `answers` — Individual Answers Within Attempts (ASM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `quiz_attempt_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to quiz_attempts.id |
| `question_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to quiz_questions.id |
| `submitted_answer` | `JSON` | YES | NULL | The learner's answer (format matches `correct_answer` structure) |
| `is_correct` | `TINYINT(1)` | YES | NULL | Whether the answer matches the correct answer (NULL for ungraded short answer) |
| `points_earned` | `DECIMAL(5,2)` | YES | NULL | Points awarded (NULL if not yet graded) |
| `instructor_feedback` | `TEXT` | YES | NULL | Feedback for manually graded questions |
| `graded_at` | `DATETIME(3)` | YES | NULL | When the instructor graded this answer |
| `graded_by` | `BIGINT UNSIGNED` | YES | NULL | FK to users.id (the instructor who graded) |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |

**Module:** ASM  
**Unique:** `(quiz_attempt_id, question_id)` — one answer per question per attempt

---

### 3.24 `assignment_submissions` — Learner Assignment Submissions (ASM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `lesson_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to lessons.id |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id (the learner) |
| `title` | `VARCHAR(255)` | NOT NULL | — | Assignment title from instructor |
| `instructions` | `TEXT` | YES | NULL | Assignment instructions |
| `submission_text` | `TEXT` | YES | NULL | Text/rich-text response from learner |
| `file_url` | `VARCHAR(500)` | YES | NULL | URL to uploaded assignment file |
| `file_name` | `VARCHAR(255)` | YES | NULL | Original file name |
| `file_size_bytes` | `BIGINT UNSIGNED` | YES | NULL | File size in bytes |
| `grade` | `DECIMAL(5,2)` | YES | NULL | Score awarded (NULL = not yet graded) |
| `max_grade` | `DECIMAL(5,2)` | NOT NULL | 100.00 | Maximum possible score |
| `feedback` | `TEXT` | YES | NULL | Instructor's written feedback |
| `graded_by` | `BIGINT UNSIGNED` | YES | NULL | FK to users.id (the instructor who graded) |
| `graded_at` | `DATETIME(3)` | YES | NULL | When the instructor graded this submission |
| `submitted_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Submission timestamp |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** ASM  
**Unique:** `(lesson_id, user_id)` — one submission per assignment per learner

---

### 3.25 `certificates` — Completion Certificates (RPT Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `certificate_uuid` | `CHAR(36)` | NOT NULL | UUID() | Public-facing certificate ID (used in verification URL) |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id |
| `program_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to programs.id |
| `enrollment_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to enrollments.id |
| `verification_hash` | `CHAR(64)` | NOT NULL | — | SHA-256(user_id + program_id + completed_at) |
| `is_revoked` | `TINYINT(1)` | NOT NULL | 0 | Whether the certificate has been revoked |
| `revoked_at` | `DATETIME(3)` | YES | NULL | When the certificate was revoked |
| `revoked_reason` | `VARCHAR(500)` | YES | NULL | Reason for revocation |
| `issued_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | When the certificate was issued |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |

**Module:** RPT  
**Unique:** `certificate_uuid`, `(user_id, program_id)` — one certificate per learner per completed program

---

### 3.26 `notifications` — In-App Notifications (NTF Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `user_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id (recipient) |
| `type` | `ENUM('ENROLLMENT','GRADE','CERTIFICATE','NEW_CONTENT','PROGRAM_APPROVED','PROGRAM_REJECTED','WAITLIST_AVAILABLE')` | NOT NULL | — | Notification event type |
| `title` | `VARCHAR(255)` | NOT NULL | — | Notification title |
| `message` | `TEXT` | NOT NULL | — | Notification body text |
| `link_url` | `VARCHAR(500)` | YES | NULL | Deep link to the relevant page |
| `is_read` | `TINYINT(1)` | NOT NULL | 0 | Whether the notification has been read |
| `read_at` | `DATETIME(3)` | YES | NULL | When the notification was read |
| `email_sent` | `TINYINT(1)` | NOT NULL | 0 | Whether an email was sent for this notification |
| `email_sent_at` | `DATETIME(3)` | YES | NULL | When the email was dispatched |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |

**Module:** NTF  
**Index:** `(user_id, is_read, created_at)` — for efficient "fetch unread notifications" queries

---

### 3.27 `audit_logs` — Admin Audit Trail (ADM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `actor_id` | `BIGINT UNSIGNED` | NOT NULL | — | FK to users.id (the admin who performed the action) |
| `action` | `VARCHAR(100)` | NOT NULL | — | Action name (e.g., 'USER_ROLE_CHANGED', 'PROGRAM_DELETED') |
| `entity_type` | `VARCHAR(50)` | NOT NULL | — | Entity affected (e.g., 'USER', 'PROGRAM', 'SYSTEM_CONFIG') |
| `entity_id` | `BIGINT UNSIGNED` | YES | NULL | ID of the affected entity |
| `details` | `JSON` | YES | NULL | Action-specific data (old/new values, context) |
| `ip_address` | `VARCHAR(45)` | NOT NULL | — | Actor's IP address |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Event timestamp |

**Module:** ADM  
**Immutability:** This table is append-only. No UPDATE or DELETE operations are permitted.  
**Retention:** Records are retained for 90 days, then purged by scheduled task.

---

### 3.28 `system_config` — Platform Configuration (ADM Module)

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| `id` | `BIGINT UNSIGNED` | NOT NULL | AUTO_INCREMENT | Primary key |
| `config_key` | `VARCHAR(100)` | NOT NULL | — | Configuration key (e.g., 'platform.name', 'certificate.template', 'enrollment.cancellation_window_days') |
| `config_value` | `JSON` | NOT NULL | — | Configuration value (JSON for flexibility — can store strings, numbers, arrays, objects) |
| `description` | `VARCHAR(500)` | YES | NULL | Human-readable description of this config key |
| `updated_by` | `BIGINT UNSIGNED` | YES | NULL | FK to users.id (last admin who modified this) |
| `created_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) | Record creation timestamp |
| `updated_at` | `DATETIME(3)` | NOT NULL | CURRENT_TIMESTAMP(3) ON UPDATE | Last modification timestamp |

**Module:** ADM  
**Unique:** `config_key`

---

## 4. Primary Keys

All tables in SkillEnroll follow a consistent primary key strategy:

| Rule | Detail |
|------|--------|
| **Type** | `BIGINT UNSIGNED` — 64-bit unsigned integer (supports up to ~18.4 × 10¹⁸ rows) |
| **Name** | Always `id` |
| **Generation** | `AUTO_INCREMENT` (native MySQL auto-increment) |
| **Rationale** | Surrogate keys are simpler than natural keys for relational integrity, allow changing business keys without cascade updates, and are more efficient for InnoDB clustered index storage |

**Why not UUID as primary key?**
- UUIDs (CHAR(36)) are 4× larger than BIGINT
- Random UUIDs cause index fragmentation in InnoDB's B+Tree clustered index
- Instead, UUIDs are stored in separate columns (e.g., `users.uuid`, `certificates.certificate_uuid`) for public-facing identifiers where security-through-obscurity is desired

---

## 5. Foreign Keys

### 5.1 Foreign Key Summary

| FK Name | Source Table | Column | Referenced Table | Column | On Delete |
|---------|-------------|--------|-----------------|--------|-----------|
| `fk_refresh_tokens_user` | `refresh_tokens` | `user_id` | `users` | `id` | CASCADE |
| `fk_password_reset_tokens_user` | `password_reset_tokens` | `user_id` | `users` | `id` | CASCADE |
| `fk_email_verifications_user` | `email_verifications` | `user_id` | `users` | `id` | CASCADE |
| `fk_user_profiles_user` | `user_profiles` | `user_id` | `users` | `id` | CASCADE |
| `fk_user_preferences_user` | `user_preferences` | `user_id` | `users` | `id` | CASCADE |
| `fk_user_sessions_user` | `user_sessions` | `user_id` | `users` | `id` | CASCADE |
| `fk_user_sessions_refresh_token` | `user_sessions` | `refresh_token_id` | `refresh_tokens` | `id` | CASCADE |
| `fk_programs_instructor` | `programs` | `instructor_id` | `users` | `id` | RESTRICT |
| `fk_programs_category` | `programs` | `category_id` | `categories` | `id` | SET NULL |
| `fk_program_versions_program` | `program_versions` | `program_id` | `programs` | `id` | CASCADE |
| `fk_program_drafts_program` | `program_drafts` | `program_id` | `programs` | `id` | CASCADE |
| `fk_program_reviews_program` | `program_reviews` | `program_id` | `programs` | `id` | CASCADE |
| `fk_program_reviews_user` | `program_reviews` | `user_id` | `users` | `id` | CASCADE |
| `fk_modules_program` | `modules` | `program_id` | `programs` | `id` | CASCADE |
| `fk_lessons_module` | `lessons` | `module_id` | `modules` | `id` | CASCADE |
| `fk_lesson_materials_lesson` | `lesson_materials` | `lesson_id` | `lessons` | `id` | CASCADE |
| `fk_enrollments_user` | `enrollments` | `user_id` | `users` | `id` | RESTRICT |
| `fk_enrollments_program` | `enrollments` | `program_id` | `programs` | `id` | RESTRICT |
| `fk_enrollments_version` | `enrollments` | `pinned_version_id` | `program_versions` | `id` | SET NULL |
| `fk_waitlist_user` | `waitlist_entries` | `user_id` | `users` | `id` | CASCADE |
| `fk_waitlist_program` | `waitlist_entries` | `program_id` | `programs` | `id` | CASCADE |
| `fk_learning_progress_enrollment` | `learning_progress` | `enrollment_id` | `enrollments` | `id` | CASCADE |
| `fk_learning_progress_module` | `learning_progress` | `module_id` | `modules` | `id` | RESTRICT |
| `fk_learner_notes_user` | `learner_notes` | `user_id` | `users` | `id` | CASCADE |
| `fk_learner_notes_lesson` | `learner_notes` | `lesson_id` | `lessons` | `id` | CASCADE |
| `fk_quizzes_lesson` | `quizzes` | `lesson_id` | `lessons` | `id` | CASCADE |
| `fk_quiz_questions_quiz` | `quiz_questions` | `quiz_id` | `quizzes` | `id` | CASCADE |
| `fk_quiz_attempts_quiz` | `quiz_attempts` | `quiz_id` | `quizzes` | `id` | CASCADE |
| `fk_quiz_attempts_user` | `quiz_attempts` | `user_id` | `users` | `id` | CASCADE |
| `fk_answers_attempt` | `answers` | `quiz_attempt_id` | `quiz_attempts` | `id` | CASCADE |
| `fk_answers_question` | `answers` | `question_id` | `quiz_questions` | `id` | RESTRICT |
| `fk_assignment_submissions_lesson` | `assignment_submissions` | `lesson_id` | `lessons` | `id` | CASCADE |
| `fk_assignment_submissions_user` | `assignment_submissions` | `user_id` | `users` | `id` | CASCADE |
| `fk_assignment_submissions_grader` | `assignment_submissions` | `graded_by` | `users` | `id` | SET NULL |
| `fk_certificates_user` | `certificates` | `user_id` | `users` | `id` | CASCADE |
| `fk_certificates_program` | `certificates` | `program_id` | `programs` | `id` | RESTRICT |
| `fk_certificates_enrollment` | `certificates` | `enrollment_id` | `enrollments` | `id` | RESTRICT |
| `fk_notifications_user` | `notifications` | `user_id` | `users` | `id` | CASCADE |
| `fk_audit_logs_actor` | `audit_logs` | `actor_id` | `users` | `id` | RESTRICT |
| `fk_system_config_updater` | `system_config` | `updated_by` | `users` | `id` | SET NULL |

### 5.2 Deletion Behavior Rules

| Rule | Usage | Rationale |
|------|-------|-----------|
| **CASCADE** | Owned records (user data, program curriculum, enrollment progress) | When a parent is deleted, all dependent records are automatically cleaned up |
| **RESTRICT** | Critical business data (programs with enrollments, quiz questions with attempts) | Prevents accidental deletion of data that would orphan important business records |
| **SET NULL** | Optional references (category on program, grader on submission) | Preserves the parent record when the referenced entity is deleted |

---

## 6. Indexes

### 6.1 Performance Indexes

These indexes are designed to support the most frequent and performance-critical queries identified in the requirements.

| Table | Index Name | Columns | Type | Purpose |
|-------|-----------|---------|------|---------|
| `users` | `idx_users_email` | `email` | UNIQUE | Fast login by email |
| `users` | `idx_users_role_status` | `role, status` | INDEX | Admin user management list filtering |
| `users` | `idx_users_uuid` | `uuid` | UNIQUE | Public-facing user lookup |
| `refresh_tokens` | `idx_refresh_tokens_hash` | `token_hash` | UNIQUE | Fast refresh token lookup |
| `refresh_tokens` | `idx_refresh_tokens_user` | `user_id, is_revoked` | INDEX | "Revoke all tokens for user" operation |
| `refresh_tokens` | `idx_refresh_tokens_expires` | `expires_at` | INDEX | Expired token cleanup |
| `password_reset_tokens` | `idx_password_reset_hash` | `token_hash` | UNIQUE | Fast reset token lookup |
| `email_verifications` | `idx_email_verifications_user` | `user_id` | INDEX | Get latest OTP for user |
| `categories` | `idx_categories_slug` | `slug` | UNIQUE | URL-based lookup |
| `programs` | `idx_programs_instructor` | `instructor_id` | INDEX | "My programs" for instructors |
| `programs` | `idx_programs_category` | `category_id` | INDEX | Catalog filter by category |
| `programs` | `idx_programs_published` | `is_published, is_archived` | INDEX | Catalog: show only published & not archived |
| `programs` | `idx_programs_search` | `title` | FULLTEXT | Full-text search on program title |
| `programs` | `idx_programs_description` | `description` | FULLTEXT | Full-text search on program description |
| `programs` | `idx_programs_slug` | `slug` | UNIQUE | URL-based program lookup |
| `program_versions` | `idx_program_versions_program` | `program_id, version_number` | UNIQUE | Version history lookup |
| `modules` | `idx_modules_program_order` | `program_id, sort_order` | UNIQUE | Curriculum ordering |
| `lessons` | `idx_lessons_module_order` | `module_id, sort_order` | UNIQUE | Lesson ordering within module |
| `enrollments` | `idx_enrollments_user` | `user_id` | INDEX | Learner dashboard: "my enrollments" |
| `enrollments` | `idx_enrollments_program` | `program_id, status` | INDEX | Instructor enrollment roster |
| `enrollments` | `idx_enrollments_user_program` | `user_id, program_id` | UNIQUE | Prevent duplicate enrollment |
| `learning_progress` | `idx_learning_progress_enrollment` | `enrollment_id` | INDEX | Load all module progress for an enrollment |
| `quiz_attempts` | `idx_quiz_attempts_user_quiz` | `user_id, quiz_id` | INDEX | "My attempts" for a specific quiz |
| `quiz_attempts` | `idx_quiz_attempts_quiz` | `quiz_id` | INDEX | Instructor: view all attempts for a quiz |
| `notifications` | `idx_notifications_user_unread` | `user_id, is_read, created_at` | INDEX | Fetch unread notifications (sorted by newest) |
| `audit_logs` | `idx_audit_logs_actor` | `actor_id, created_at` | INDEX | Audit trail: actions by specific admin |
| `audit_logs` | `idx_audit_logs_entity` | `entity_type, entity_id` | INDEX | Audit trail: changes to specific entity |
| `audit_logs` | `idx_audit_logs_created` | `created_at` | INDEX | Time-based audit log queries + cleanup |
| `certificates` | `idx_certificates_uuid` | `certificate_uuid` | UNIQUE | Fast certificate verification |
| `certificates` | `idx_certificates_user` | `user_id` | INDEX | Learner dashboard: "my certificates" |

### 6.2 Full-Text Search Configuration

A **MySQL FULLTEXT index** is applied to the `programs` table on the combination of `title` and `description` columns. This enables the catalog search feature to perform natural-language text searches with built-in relevance ranking.

**Indexed columns:** `programs.title` + `programs.description` (composite FULLTEXT)

**Query behavior:** When a user types a search term (e.g., "web development"), the system executes a natural language mode search that:
- Matches against both title and description simultaneously
- Ranks results by relevance (title matches weighted higher)
- Automatically filters for `is_published = 1` and `is_archived = 0`
- Returns results sorted by relevance score descending
- Handles stop words and stemming automatically (MySQL InnoDB FULLTEXT built-in)

### 6.3 Index Strategy Rationale

| Strategy | Explanation |
|----------|-------------|
| **Unique indexes for lookup columns** | `email`, `slug`, `uuid`, `certificate_uuid`, `token_hash` — these columns are queried with exact-match lookups and must be unique |
| **Composite indexes for filtered queries** | `(user_id, is_read, created_at)` on notifications — covers WHERE, ORDER BY, and LIMIT in a single index |
| **Full-text indexes for search** | MySQL FULLTEXT indexes on `programs.title` and `programs.description` — supports natural language search with relevance ranking |
| **Covering indexes where possible** | For frequently accessed queries where all needed columns are in the index, avoiding table lookups |
| **No over-indexing** | Indexes on tables expected to have low row counts (< 500 rows) like `categories` and `system_config` are limited to unique constraints only |

---

## 7. Constraints

### 7.1 Table-Level Constraints

| Table | Constraint | Type | Description |
|-------|-----------|------|-------------|
| `users` | `email` | UNIQUE | No two users can have the same email |
| `users` | `uuid` | UNIQUE | No two users can have the same UUID |
| `users` | `role` | CHECK | Must be one of: `LEARNER`, `INSTRUCTOR`, `ADMIN` |
| `users` | `status` | CHECK | Must be one of: `ACTIVE`, `SUSPENDED`, `DEACTIVATED` |
| `categories` | `name` | UNIQUE | No duplicate category names |
| `categories` | `slug` | UNIQUE | No duplicate category slugs |
| `programs` | `slug` | UNIQUE | No duplicate program slugs |
| `programs` | `skill_level` | CHECK | Must be one of: `BEGINNER`, `INTERMEDIATE`, `ADVANCED` |
| `programs` | `is_published` | CHECK | Boolean (0 or 1) |
| `programs` | `is_archived` | CHECK | Boolean (0 or 1) — if archived, `is_published` must be 0 |
| `programs` | `average_rating` | CHECK | 0.0 to 5.0 |
| `program_reviews` | `(program_id, user_id)` | UNIQUE | One review per learner per program |
| `program_reviews` | `rating` | CHECK | 1 to 5 |
| `enrollments` | `(user_id, program_id)` | UNIQUE | One enrollment per learner per program |
| `enrollments` | `status` | CHECK | Must be one of: `ACTIVE`, `WITHDRAWN`, `COMPLETED` |
| `enrollments` | `progress_percentage` | CHECK | 0.00 to 100.00 |
| `waitlist_entries` | `(user_id, program_id)` | UNIQUE | One waitlist entry per learner per program |
| `quizzes` | `passing_score_percent` | CHECK | 0.00 to 100.00 |
| `quiz_questions` | `question_type` | CHECK | Must be a valid question type from the ENUM |
| `quiz_attempts` | `(quiz_id, user_id, attempt_number)` | UNIQUE | No duplicate attempt numbers |
| `quiz_attempts` | `score_percentage` | CHECK | 0.00 to 100.00 if set |
| `certificates` | `certificate_uuid` | UNIQUE | No duplicate certificate IDs |
| `certificates` | `(user_id, program_id)` | UNIQUE | One certificate per learner per program |
| `system_config` | `config_key` | UNIQUE | No duplicate configuration keys |

### 7.2 Application-Level Constraints (Not Enforceable in MySQL)

These constraints are enforced by the **application service layer** (not by database constraints):

| Constraint | Module | Logic |
|-----------|--------|-------|
| **One active draft per program** | PM | The application checks that no un-published draft exists before creating a new one |
| **Enrollment cap enforcement** | ENR | Before creating an enrollment, the application checks `current_enrollment_count < max_enrollments` |
| **Sequential module unlocking** | LRN | The application determines which modules are unlocked based on prerequisites |
| **Certificate eligibility** | RPT | The application verifies 100 % module completion and passing all required assessments before issuing a certificate |
| **Rate limiting** | AUTH | 5 requests/minute/IP on auth endpoints (enforced at application layer) |

### 7.3 Trigger-Enforced Denormalization

| Trigger Event | Action |
|-------------|--------|
| `INSERT` on `program_reviews` | Increment `programs.review_count`; recalculate `programs.average_rating` |
| `UPDATE` on `program_reviews` | Recalculate `programs.average_rating` |
| `DELETE` on `program_reviews` | Decrement `programs.review_count`; recalculate `programs.average_rating` |
| `INSERT` on `enrollments` | Increment `programs.current_enrollment_count` |
| `UPDATE` on `enrollments` (status → WITHDRAWN) | Decrement `programs.current_enrollment_count` |
| `INSERT` on `learning_progress` (status → COMPLETED) | Recalculate `enrollments.progress_percentage` |
| `UPDATE` on `learning_progress` (status → COMPLETED) | Recalculate `enrollments.progress_percentage` |

---

## 8. Normalization

### 8.1 Normalization Status

The SkillEnroll schema is designed to **Third Normal Form (3NF)** with strategic denormalization for performance.

| Normal Form | Status | Evidence |
|-------------|--------|----------|
| **1NF** (Atomic columns) | ✅ Achieved | Every column contains a single atomic value. Multi-value attributes (e.g., notification preferences) are stored as JSON, which MySQL 8 treats as a single atomic value in 1NF terms |
| **2NF** (Full functional dependency) | ✅ Achieved | All non-key columns are functionally dependent on the entire primary key. No partial dependencies exist (all tables have single-column `id` PKs) |
| **3NF** (No transitive dependencies) | ✅ Achieved | Non-key columns depend only on the primary key. For example, `lessons.module_id` depends on `lessons.id`, and `modules.program_id` depends on `modules.id` — no transitive dependency through `lessons → module → program` |
| **BCNF** (Every determinant is a candidate key) | ✅ Achieved | All tables satisfy BCNF because every table has a single-column primary key and no overlapping candidate keys |

### 8.2 Strategic Denormalizations

Three **intentional denormalizations** are applied for performance:

| Table | Denormalized Column | Rationale |
|-------|-------------------|-----------|
| `programs` | `current_enrollment_count` | Avoids `COUNT(*)` query on `enrollments` for every catalog page load. Updated via INSERT/UPDATE trigger on `enrollments`. |
| `programs` | `average_rating` | Avoids `AVG(rating)` query on `program_reviews` for every catalog card. Updated via trigger. |
| `programs` | `review_count` | Avoids `COUNT(*)` query on `program_reviews`. Updated alongside `average_rating`. |
| `enrollments` | `progress_percentage` | Avoids aggregating `learning_progress` on every dashboard load. Updated via trigger on `learning_progress`. |

**Trade-off accepted:** Write operations on `enrollments` and `program_reviews` are slightly slower due to trigger overhead, but read operations on the catalog and dashboard (which are 100× more frequent) are significantly faster.

### 8.3 JSON Column Justification

Three tables use JSON columns for flexible semi-structured data:

| Table | JSON Column | Why Not a Separate Table? |
|-------|-------------|---------------------------|
| `user_preferences` | `email_notifications` | Notification types may be added without schema migration. A separate table would need a new row per preference, requiring JOINs on every read. |
| `user_preferences` | `ui_preferences` | Shape is entirely UI-driven and expected to evolve. A fixed schema would over-constrain the frontend. |
| `program_drafts` | `draft_data` | Contains a full nested snapshot of the curriculum (modules → lessons → materials). Normalizing this would require duplicating the entire curriculum table structure with versioning. |
| `quiz_questions` | `options`, `correct_answer` | Different question types (multiple choice, true/false, fill-in-blank) have different structures. A separate table per type would violate the Open/Closed Principle. |

---

## 9. MySQL Engine & Charset

### 9.1 Engine Configuration

The following configuration is applied uniformly to **all 27 tables** in the SkillEnroll database:

| Setting | Value | Rationale |
|---------|-------|-----------|
| **Storage Engine** | `InnoDB` | ACID compliance, row-level locking, foreign key enforcement, crash recovery, MVCC for concurrent reads/writes |
| **Character Set** | `utf8mb4` | Full Unicode support including 4-byte characters (emoji, CJK ideographs, mathematical symbols) |
| **Collation** | `utf8mb4_unicode_ci` | Case-insensitive, language-neutral sorting based on Unicode Collation Algorithm (UCA). Correctly handles accented characters and multi-language content |
| **Row Format** | `DYNAMIC` (InnoDB default) | Efficient storage for variable-length columns including TEXT and JSON types |

**Why `utf8mb4` over `utf8mb3`?** The obsolete `utf8mb3` (often aliased as `utf8` in MySQL) only supports Basic Multilingual Plane characters. `utf8mb4` is required for emoji (👍, 🎓, ✅) that users may include in bios, lesson feedback, and notification content.

### 9.2 Why These Choices

| Choice | Rationale |
|--------|-----------|
| **InnoDB** | ACID-compliant, row-level locking, foreign key support, crash recovery, MVCC for concurrent reads |
| **`utf8mb4`** | Supports all Unicode characters including emoji (👍, 🎓, ✅) which may appear in user bios, lesson content, and feedback |
| **`utf8mb4_unicode_ci`** | Case-insensitive, language-neutral sorting based on Unicode Collation Algorithm (UCA). Handles accented characters correctly (é = e for sorting) |
| **`DATETIME(3)`** | Millisecond precision for audit trails and activity tracking. `TIMESTAMP` could be used instead but has a 2038 year-2038 limit |

### 9.3 Connection Pool Configuration (HikariCP)

| Property | Value | Rationale |
|----------|-------|-----------|
| `maximumPoolSize` | 10 (default) | Sufficient for 500 concurrent users given typical request duration < 200 ms |
| `minimumIdle` | 5 | Keep minimum connections ready for traffic spikes |
| `connectionTimeout` | 30,000 ms | 30 seconds to establish a connection |
| `idleTimeout` | 600,000 ms | 10 minutes before closing idle connections |
| `maxLifetime` | 1,800,000 ms | 30 minutes max connection lifetime (rotate connections to handle network changes) |

### 9.4 Flyway Migration Strategy

| Migration | Name | Content |
|-----------|------|---------|
| `V1__init_schema.sql` | Initial schema | All 27 CREATE TABLE statements with PKs, FKs, indexes, and constraints |
| `V2__seed_data.sql` | Seed data | Default categories (Web Development, Data Science, DevOps, etc.), system config defaults, admin user |
| `V3__init_triggers.sql` | Triggers | Denormalization triggers for `average_rating`, `review_count`, `current_enrollment_count`, `progress_percentage` |
| `V4__init_views.sql` | Views | Instructor dashboard view, admin report views (optional — views can be managed separately) |
| `V5__plus` | Future migrations | Schema changes applied incrementally as the product evolves |

---

> **Next Document:** `05_SPRINT_ZERO_PLAN.md` — Bootstrap project initialization, tooling setup, and first sprint plan.

## Appendix A: Quick Reference — Module-to-Table Mapping

| Module | Tables |
|--------|--------|
| **AUTH** | `users`, `refresh_tokens`, `password_reset_tokens`, `email_verifications` |
| **USER** | `user_profiles`, `user_preferences`, `user_sessions` |
| **CAT** | `categories`, `programs`, `program_reviews` |
| **PM** | `programs`, `program_versions`, `program_drafts`, `modules`, `lessons`, `lesson_materials` |
| **ENR** | `enrollments`, `waitlist_entries` |
| **LRN** | `learning_progress`, `learner_notes` |
| **ASM** | `quizzes`, `quiz_questions`, `quiz_attempts`, `answers`, `assignment_submissions` |
| **RPT** | `certificates` |
| **NTF** | `notifications` |
| **ADM** | `audit_logs`, `system_config` |
