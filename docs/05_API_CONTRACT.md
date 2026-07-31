# 05_API_CONTRACT.md — SkillEnroll

> **Document Version:** 1.0  
> **Author:** Senior Software Architect  
> **Framework:** TrainingMug AI Development Framework  
> **Status:** ✅ Approved  
> **Related Docs:** [02_REQUIREMENTS.md](./02_REQUIREMENTS.md) · [03_ARCHITECTURE.md](./03_ARCHITECTURE.md) · [04_DATABASE.md](./04_DATABASE.md)

---

## Table of Contents

1. [API Conventions](#1-api-conventions)
2. [Auth API](#2-auth-api)
3. [User API](#3-user-api)
4. [Catalog API](#4-catalog-api)
5. [Category API](#5-category-api)
6. [Program Management API](#6-program-management-api)
7. [Curriculum API (Modules & Lessons)](#7-curriculum-api-modules--lessons)
8. [Enrollment API](#8-enrollment-api)
9. [Learning API](#9-learning-api)
10. [Assessment API](#10-assessment-api)
11. [Notification API](#11-notification-api)
12. [Reporting API](#12-reporting-api)
13. [Certificate API](#13-certificate-api)
14. [Admin API](#14-admin-api)
15. [API Reference Table](#15-api-reference-table)

---

## 1. API Conventions

### 1.1 Base URL

| Environment | Base URL |
|-------------|----------|
| **Local** | `http://localhost:8080/api` |
| **Staging** | `https://staging-api.skillenroll.app/api` |
| **Production** | `https://api.skillenroll.app/api` |

**Health Check (Spring Actuator):** `http://localhost:8080/actuator/health` (outside `/api` prefix, no auth)

### 1.2 Authentication Header

All authenticated endpoints require a JWT access token in the `Authorization` header:

```
Authorization: Bearer <access_token>
```

### 1.3 Standard Response Envelope

All API responses follow a consistent JSON envelope:

```json
{
  "success": true,
  "data": { ... },
  "message": "Operation completed successfully",
  "timestamp": "2026-07-28T10:30:00.123Z"
}
```

### 1.4 Standard Error Response

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": [
      { "field": "email", "message": "Email is already in use" }
    ]
  },
  "timestamp": "2026-07-28T10:30:00.123Z"
}
```

### 1.5 Standard Pagination

Paginated list endpoints accept and return:

**Request query params:** `?page=1&size=20&sort=created_at,desc`

**Response structure:**
```json
{
  "success": true,
  "data": [ ... ],
  "page": {
    "number": 1,
    "size": 20,
    "totalElements": 145,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

### 1.6 Standard Status Codes

| Code | Meaning |
|------|---------|
| `200 OK` | Successful GET, PUT, PATCH |
| `201 Created` | Successful POST (resource created) |
| `204 No Content` | Successful DELETE |
| `400 Bad Request` | Validation error, malformed request |
| `401 Unauthorized` | Missing or invalid JWT token |
| `403 Forbidden` | Authenticated but insufficient role |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Business rule violation (duplicate enrollment, etc.) |
| `422 Unprocessable Entity` | Business validation failure |
| `429 Too Many Requests` | Rate limit exceeded |
| `500 Internal Server Error` | Unexpected server error |

### 1.7 Rate Limiting

| Endpoint Group | Limit |
|----------------|-------|
| Auth endpoints (`/auth/*`) | 5 requests/min per IP |
| All other endpoints | 100 requests/min per user |
| Bulk operations (`/admin/bulk/*`) | 10 requests/min per admin |

---

## 2. Auth API

### 2.1 POST /auth/register — User Registration

**Description:** Register a new user account. Sends a 6-digit OTP to the provided email for verification.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/auth/register` |
| **Authentication** | None (public) |
| **Rate Limited** | Yes (5/min/IP) |

**Request Body:**
```json
{
  "email": "learner@example.com",
  "password": "SecurePass123!",
  "fullName": "John Doe",
  "role": "LEARNER"
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `email` | Required, valid email format (RFC 5322), max 255 chars, must be unique |
| `password` | Required, min 8 chars, max 128 chars, must contain at least 1 uppercase, 1 lowercase, 1 digit |
| `fullName` | Required, min 2 chars, max 150 chars |
| `role` | Required, must be `LEARNER` or `INSTRUCTOR` (Admin registration is admin-only) |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "learner@example.com",
    "message": "Registration successful. Please check your email for the OTP verification code."
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `400` | `VALIDATION_ERROR` | Invalid email format, weak password, missing fields |
| `409` | `EMAIL_ALREADY_EXISTS` | Email is already registered |
| `429` | `RATE_LIMIT_EXCEEDED` | Too many registration attempts from this IP |

---

### 2.2 POST /auth/verify-email — Verify Email with OTP

**Description:** Verify the user's email address using the 6-digit OTP sent during registration.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/auth/verify-email` |
| **Authentication** | None (public) |
| **Rate Limited** | Yes (5/min/IP) |

**Request Body:**
```json
{
  "email": "learner@example.com",
  "otp": "483921"
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `email` | Required, valid email, must exist and be unverified |
| `otp` | Required, exactly 6 digits, must match stored OTP for this email, must not be expired (15 min) |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Email verified successfully. You can now log in."
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `400` | `VALIDATION_ERROR` | Invalid OTP format |
| `400` | `INVALID_OTP` | OTP does not match (after max 5 failed attempts, OTP is invalidated) |
| `400` | `OTP_EXPIRED` | OTP has expired (15 min window) |
| `404` | `USER_NOT_FOUND` | No registration found for this email |
| `409` | `ALREADY_VERIFIED` | Email is already verified |

---

### 2.3 POST /auth/login — User Login

**Description:** Authenticate with email and password. Returns JWT access token and refresh token.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/auth/login` |
| **Authentication** | None (public) |
| **Rate Limited** | Yes (5/min/IP) |

**Request Body:**
```json
{
  "email": "learner@example.com",
  "password": "SecurePass123!"
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `email` | Required, valid email format |
| `password` | Required, not empty |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900,
    "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
    "tokenType": "Bearer",
    "user": {
      "id": 1,
      "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "email": "learner@example.com",
      "fullName": "John Doe",
      "role": "LEARNER",
      "isEmailVerified": true
    }
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `400` | `EMAIL_NOT_VERIFIED` | Email not yet verified — user must verify first |
| `401` | `INVALID_CREDENTIALS` | Email or password is incorrect |
| `403` | `ACCOUNT_SUSPENDED` | User account is suspended |
| `429` | `RATE_LIMIT_EXCEEDED` | Too many login attempts |

---

### 2.4 POST /auth/refresh — Refresh Access Token

**Description:** Exchange a valid refresh token for a new access token and a new refresh token (rotation).

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/auth/refresh` |
| **Authentication** | None (uses refresh token in body) |

**Request Body:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `refreshToken` | Required, must be a valid UUID, must exist in DB and not be revoked, must not be expired |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 900,
    "refreshToken": "660e8400-e29b-41d4-a716-446655440001",
    "tokenType": "Bearer"
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `401` | `INVALID_REFRESH_TOKEN` | Token not found or already revoked |
| `401` | `REFRESH_TOKEN_EXPIRED` | Token has expired (7 days) |
| `409` | `TOKEN_REUSE_DETECTED` | Token was already used (rotation detected potential theft) — all user sessions revoked |

---

### 2.5 POST /auth/logout — Logout

**Description:** Revoke the current refresh token, effectively logging out the user from this device.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/auth/logout` |
| **Authentication** | Authenticated |

**Request Body:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `refreshToken` | Required, must belong to the authenticated user |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Logged out successfully"
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `400` | `TOKEN_MISMATCH` | Token does not belong to the authenticated user |
| `401` | `UNAUTHORIZED` | Missing or invalid JWT |

---

### 2.6 POST /auth/forgot-password — Request Password Reset

**Description:** Send a password reset link to the user's email. The link contains a time-limited token.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/auth/forgot-password` |
| **Authentication** | None (public) |
| **Rate Limited** | Yes (3/min/IP) |

**Request Body:**
```json
{
  "email": "learner@example.com"
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `email` | Required, valid email format |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "If the email exists, a password reset link has been sent."
  }
}
```
> **Note:** The response is intentionally vague to prevent email enumeration attacks. The same message is returned whether or not the email exists.

---

### 2.7 POST /auth/reset-password — Reset Password

**Description:** Reset the user's password using the token from the password reset email.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/auth/reset-password` |
| **Authentication** | None (uses reset token) |

**Request Body:**
```json
{
  "token": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "newPassword": "NewSecurePass456!"
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `token` | Required, valid UUID, must exist in DB, must not be expired (1 hour) |
| `newPassword` | Required, min 8 chars, max 128, same complexity rules as registration |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Password reset successfully. You can now log in with your new password."
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `400` | `INVALID_TOKEN` | Token not found or already used |
| `400` | `TOKEN_EXPIRED` | 1-hour window has passed |
| `400` | `VALIDATION_ERROR` | Weak password |

---

### 2.8 GET /auth/sessions — List Active Sessions

**Description:** List all active sessions for the authenticated user.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/auth/sessions` |
| **Authentication** | Authenticated |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "sessionId": 101,
      "deviceName": "Chrome 128 on Windows 10",
      "ipAddress": "192.168.1.100",
      "lastActivityAt": "2026-07-28T10:29:00.000Z",
      "createdAt": "2026-07-20T08:00:00.000Z",
      "isCurrentSession": true
    },
    {
      "sessionId": 102,
      "deviceName": "Safari on iPhone iOS 18",
      "ipAddress": "10.0.0.5",
      "lastActivityAt": "2026-07-27T14:15:00.000Z",
      "createdAt": "2026-07-22T19:30:00.000Z",
      "isCurrentSession": false
    }
  ]
}
```

---

### 2.9 DELETE /auth/sessions/{sessionId} — Revoke Session

**Description:** Revoke a specific session (log out from a specific device).

| Attribute | Value |
|-----------|-------|
| **Method** | `DELETE` |
| **Endpoint** | `/auth/sessions/{sessionId}` |
| **Authentication** | Authenticated |

**Path Parameters:**

| Parameter | Rule |
|-----------|------|
| `sessionId` | Required, must belong to the authenticated user |

**Success Response:** `204 No Content`

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `403` | `SESSION_MISMATCH` | Session does not belong to this user |
| `404` | `SESSION_NOT_FOUND` | Session ID does not exist |

---

## 3. User API

### 3.1 GET /users/me — Get Current User Profile

**Description:** Get the authenticated user's profile information.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/users/me` |
| **Authentication** | Authenticated |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "email": "learner@example.com",
    "fullName": "John Doe",
    "role": "LEARNER",
    "isEmailVerified": true,
    "profile": {
      "bio": "Passionate about web development",
      "avatarUrl": "https://skillenroll.blob.core.windows.net/avatars/user-1.jpg",
      "contactNumber": "+1-555-0123",
      "timezone": "America/New_York"
    },
    "preferences": {
      "emailNotifications": {
        "enrollment": true,
        "grade": true,
        "newContent": false,
        "certificate": true
      }
    },
    "createdAt": "2026-07-01T08:00:00.000Z"
  }
}
```

---

### 3.2 PUT /users/me — Update Current User Profile

**Description:** Update the authenticated user's profile information.

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/users/me` |
| **Authentication** | Authenticated |

**Request Body:**
```json
{
  "fullName": "John Updated Doe",
  "profile": {
    "bio": "Full-stack developer and lifelong learner",
    "contactNumber": "+1-555-9999",
    "timezone": "America/Chicago"
  }
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `fullName` | Optional, min 2 chars, max 150 chars |
| `profile.bio` | Optional, max 2000 chars |
| `profile.contactNumber` | Optional, valid phone format |
| `profile.timezone` | Optional, must be valid IANA timezone (e.g., `America/New_York`) |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Profile updated successfully",
    "user": { "...updated user object..." }
  }
}
```

---

### 3.3 POST /users/me/avatar — Upload Avatar

**Description:** Upload or update the user's avatar image.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/users/me/avatar` |
| **Authentication** | Authenticated |
| **Content-Type** | `multipart/form-data` |

**Request Body (Form-Data):**

| Field | Type | Rule |
|-------|------|------|
| `avatar` | File | Required, JPEG or PNG only, max 2 MB, min 100x100 px, max 2048x2048 px |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "avatarUrl": "https://skillenroll.blob.core.windows.net/avatars/user-1-updated.jpg",
    "message": "Avatar updated successfully"
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `400` | `INVALID_FILE_TYPE` | File is not JPEG or PNG |
| `400` | `FILE_TOO_LARGE` | File exceeds 2 MB |
| `400` | `INVALID_DIMENSIONS` | Image dimensions out of allowed range |

---

### 3.4 PUT /users/me/preferences — Update Notification Preferences

**Description:** Update the user's notification preferences.

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/users/me/preferences` |
| **Authentication** | Authenticated |

**Request Body:**
```json
{
  "emailNotifications": {
    "enrollment": true,
    "grade": true,
    "newContent": false,
    "certificate": true
  }
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| All notification fields | Optional, boolean values only |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Notification preferences updated successfully"
  }
}
```

---

### 3.5 POST /users/me/deactivate — Deactivate Account

**Description:** Deactivate the user's own account. Admins can reactivate.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/users/me/deactivate` |
| **Authentication** | Authenticated |

**Request Body:**
```json
{
  "password": "SecurePass123!",
  "reason": "No longer need the platform"
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `password` | Required, must match the current password |
| `reason` | Optional, max 500 chars |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Account deactivated successfully. You have 90 days to reactivate by contacting support."
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `401` | `INVALID_PASSWORD` | Provided password does not match |

---

## 4. Catalog API

### 4.1 GET /catalog — Browse Public Catalog

**Description:** Browse all published, non-archived programs with pagination, search, filtering, and sorting.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/catalog` |
| **Authentication** | None (public) |

**Query Parameters:**

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `page` | Integer | No (default=1) | Page number | `page=1` |
| `size` | Integer | No (default=20, max=50) | Items per page | `size=20` |
| `search` | String | No | Full-text search term | `search=web development` |
| `categoryId` | Integer | No | Filter by category | `categoryId=3` |
| `skillLevel` | Enum | No | Filter by skill level | `skillLevel=BEGINNER` |
| `minDuration` | Integer | No | Min hours | `minDuration=10` |
| `maxDuration` | Integer | No | Max hours | `maxDuration=40` |
| `sort` | String | No (default=`created_at,desc`) | Sort field and direction | `sort=average_rating,desc` |
| `instructorId` | Integer | No | Filter by instructor | `instructorId=5` |

**Sort Options:**
- `created_at,asc` / `created_at,desc`
- `title,asc` / `title,desc`
- `average_rating,desc` (most popular)
- `current_enrollment_count,desc` (most enrolled)

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "Introduction to Web Development",
      "slug": "intro-to-web-development",
      "summary": "Learn HTML, CSS, and JavaScript from scratch",
      "thumbnailUrl": "https://skillenroll.blob.core.windows.net/thumbnails/program-1.jpg",
      "skillLevel": "BEGINNER",
      "durationHours": 24.5,
      "instructor": {
        "id": 5,
        "fullName": "Jane Smith",
        "avatarUrl": "https://...avatars/instructor-5.jpg"
      },
      "category": {
        "id": 3,
        "name": "Web Development"
      },
      "averageRating": 4.5,
      "reviewCount": 128,
      "currentEnrollmentCount": 1450,
      "createdAt": "2026-06-15T10:00:00.000Z"
    }
  ],
  "page": {
    "number": 1,
    "size": 20,
    "totalElements": 145,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

---

### 4.2 GET /catalog/{slug} — Get Program Details

**Description:** Get full details of a single program by its URL slug.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/catalog/{slug}` |
| **Authentication** | None (public) |

**Path Parameters:**

| Parameter | Rule |
|-----------|------|
| `slug` | Required, URL slug of the program, must match a published program |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "Introduction to Web Development",
    "slug": "intro-to-web-development",
    "summary": "Learn HTML, CSS, and JavaScript from scratch",
    "description": "<h2>Course Overview</h2><p>This comprehensive course covers...</p>",
    "skillLevel": "BEGINNER",
    "durationHours": 24.5,
    "prerequisites": "No prior programming experience required",
    "thumbnailUrl": "https://skillenroll.blob.core.windows.net/thumbnails/program-1.jpg",
    "previewVideoUrl": "https://www.youtube.com/watch?v=abc123",
    "instructor": {
      "id": 5,
      "fullName": "Jane Smith",
      "avatarUrl": "https://...avatars/instructor-5.jpg",
      "bio": "Senior web developer with 10 years of experience"
    },
    "category": {
      "id": 3,
      "name": "Web Development"
    },
    "syllabus": [
      {
        "id": 10,
        "title": "Getting Started",
        "description": "Set up your development environment",
        "sortOrder": 1,
        "lessons": [
          {
            "id": 100,
            "title": "Welcome to the Course",
            "contentType": "VIDEO",
            "durationMinutes": 10,
            "sortOrder": 1,
            "isFreePreview": true
          },
          {
            "id": 101,
            "title": "Installing VS Code",
            "contentType": "TEXT",
            "durationMinutes": 15,
            "sortOrder": 2,
            "isFreePreview": false
          }
        ]
      }
    ],
    "averageRating": 4.5,
    "reviewCount": 128,
    "currentEnrollmentCount": 1450,
    "isEnrolled": false,
    "createdAt": "2026-06-15T10:00:00.000Z",
    "updatedAt": "2026-07-20T14:30:00.000Z"
  }
}
```

> **Note:** `isEnrolled` is `false` for unauthenticated users or if the user is not enrolled.

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `404` | `PROGRAM_NOT_FOUND` | Slug does not match any published program |

---

### 4.3 GET /catalog/{slug}/reviews — Get Program Reviews

**Description:** Get paginated reviews for a program.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/catalog/{slug}/reviews` |
| **Authentication** | None (public) |

**Query Parameters:** `page`, `size`, `sort` (standard pagination)

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 55,
      "user": {
        "id": 10,
        "fullName": "Alice Johnson",
        "avatarUrl": "https://...avatars/user-10.jpg"
      },
      "rating": 5,
      "reviewText": "Excellent course! The instructor explains concepts very clearly.",
      "createdAt": "2026-07-20T10:00:00.000Z"
    }
  ],
  "page": { "...standard pagination..." }
}
```

---

## 5. Category API

### 5.1 GET /categories — List All Categories

**Description:** Get all program categories (for filter dropdowns).

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/categories` |
| **Authentication** | None (public) |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "Web Development",
      "slug": "web-development",
      "description": "HTML, CSS, JavaScript, React, Node.js and more",
      "iconUrl": "https://...icons/web-dev.png",
      "programCount": 45
    },
    {
      "id": 2,
      "name": "Data Science",
      "slug": "data-science",
      "description": "Python, Machine Learning, AI, Statistics",
      "iconUrl": "https://...icons/data-science.png",
      "programCount": 32
    }
  ]
}
```

---

### 5.2 POST /categories — Create Category (Admin)

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/categories` |
| **Authentication** | Admin only |

**Request Body:**
```json
{
  "name": "Cloud Computing",
  "slug": "cloud-computing",
  "description": "AWS, Azure, GCP, DevOps, Docker, Kubernetes",
  "iconUrl": "https://...icons/cloud.png",
  "displayOrder": 5
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `name` | Required, max 100 chars, must be unique |
| `slug` | Required, max 120 chars, must be unique, alphanumeric + hyphens only |
| `description` | Optional, max 500 chars |
| `iconUrl` | Optional, valid URL format |
| `displayOrder` | Optional, non-negative integer |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 7,
    "name": "Cloud Computing",
    "slug": "cloud-computing",
    "message": "Category created successfully"
  }
}
```

---

### 5.3 PUT /categories/{categoryId} — Update Category (Admin)

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/categories/{categoryId}` |
| **Authentication** | Admin only |

**Path Parameters:**

| Parameter | Rule |
|-----------|------|
| `categoryId` | Required, must reference an existing category |

**Request Body:**
```json
{
  "name": "Updated Cloud Computing",
  "description": "AWS, Azure, GCP, and multi-cloud strategies",
  "displayOrder": 3
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `name` | Optional, max 100 chars, must be unique if changed |
| `description` | Optional, max 500 chars |
| `iconUrl` | Optional, valid URL format |
| `displayOrder` | Optional, non-negative integer |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Category updated successfully"
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `404` | `CATEGORY_NOT_FOUND` | Category ID does not exist |
| `409` | `NAME_ALREADY_EXISTS` | Another category already has this name |

---

### 5.4 DELETE /categories/{categoryId} — Delete Category (Admin)

| Attribute | Value |
|-----------|-------|
| **Method** | `DELETE` |
| **Endpoint** | `/categories/{categoryId}` |
| **Authentication** | Admin only |

**Path Parameters:**

| Parameter | Rule |
|-----------|------|
| `categoryId` | Required, must reference an existing category |

**Behavior:** Programs assigned to this category will have their `category_id` set to NULL (SET NULL foreign key behavior).

**Success Response:** `204 No Content`

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `404` | `CATEGORY_NOT_FOUND` | Category ID does not exist |

---

## 6. Program Management API

### 6.1 GET /instructor/programs — List My Programs (Instructor)

**Description:** Get all programs owned by the authenticated instructor.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/instructor/programs` |
| **Authentication** | Instructor or Admin |

**Query Parameters:** `page`, `size`, `status` (DRAFT / PUBLISHED / ARCHIVED / ALL)

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "Introduction to Web Development",
      "status": "PUBLISHED",
      "approvalStatus": "APPROVED",
      "hasDraft": false,
      "currentEnrollmentCount": 1450,
      "averageRating": 4.5,
      "createdAt": "2026-06-15T10:00:00.000Z",
      "publishedVersion": 3
    }
  ],
  "page": { "...standard pagination..." }
}
```

---

### 6.2 POST /instructor/programs — Create Program

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/instructor/programs` |
| **Authentication** | Instructor or Admin |

**Request Body:**
```json
{
  "title": "Advanced React Patterns",
  "summary": "Master hooks, context, render props, and more",
  "description": "<h2>Course Description</h2><p>Deep dive into advanced React patterns...</p>",
  "categoryId": 3,
  "skillLevel": "ADVANCED",
  "durationHours": 18.0,
  "prerequisites": "Basic knowledge of React",
  "thumbnailUrl": "https://...thumbnails/program-new.jpg",
  "previewVideoUrl": "https://www.youtube.com/watch?v=xyz789"
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `title` | Required, max 255 chars, must be unique |
| `summary` | Required, max 300 chars |
| `description` | Optional, max 65535 chars |
| `categoryId` | Required, must reference an existing category |
| `skillLevel` | Required, must be `BEGINNER`, `INTERMEDIATE`, or `ADVANCED` |
| `durationHours` | Optional, decimal, max 9999.9 |
| `prerequisites` | Optional, max 2000 chars |
| `thumbnailUrl` | Optional, valid URL |
| `previewVideoUrl` | Optional, valid YouTube/Vimeo URL |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 10,
    "title": "Advanced React Patterns",
    "slug": "advanced-react-patterns",
    "status": "DRAFT",
    "message": "Program created as draft. Add modules and lessons before publishing."
  }
}
```

---

### 6.3 GET /instructor/programs/{programId} — Get My Program Details

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/instructor/programs/{programId}` |
| **Authentication** | Instructor (owner) or Admin |

**Path Parameters:**

| Parameter | Rule |
|-----------|------|
| `programId` | Required, must belong to the authenticated instructor |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 10,
    "title": "Advanced React Patterns",
    "slug": "advanced-react-patterns",
    "summary": "Master hooks, context, render props, and more",
    "description": "<h2>Course Description</h2>...",
    "categoryId": 3,
    "skillLevel": "ADVANCED",
    "durationHours": 18.0,
    "prerequisites": "Basic knowledge of React",
    "thumbnailUrl": "https://...thumbnails/program-10.jpg",
    "previewVideoUrl": "https://www.youtube.com/watch?v=xyz789",
    "status": "DRAFT",
    "approvalStatus": "PENDING",
    "publishedVersion": 0,
    "hasDraft": false,
    "maxEnrollments": null,
    "currentEnrollmentCount": 0,
    "createdAt": "2026-07-28T10:00:00.000Z",
    "updatedAt": "2026-07-28T10:00:00.000Z"
  }
}
```

---

### 6.4 PUT /instructor/programs/{programId} — Update Program

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/instructor/programs/{programId}` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:** Same schema as Create Program (all fields optional).

**Behavior:**
- If program is in `DRAFT` status → updates directly
- If program is `PUBLISHED` → creates a draft version; existing learners see the published version
- If a draft already exists → updates the existing draft

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 10,
    "message": "Program updated. Draft changes are saved. Publish to make them visible to learners."
  }
}
```

---

### 6.5 POST /instructor/programs/{programId}/publish — Publish Program

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/instructor/programs/{programId}/publish` |
| **Authentication** | Instructor (owner) or Admin |

**Validation:** Program must have:
- At least 1 module with at least 1 lesson
- Title and summary filled

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 10,
    "status": "PUBLISHED",
    "publishedVersion": 1,
    "message": "Program published successfully and is now visible to learners."
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `422` | `EMPTY_CURRICULUM` | Program has no modules or lessons |
| `422` | `MISSING_REQUIRED_FIELDS` | Title or summary is empty |

---

### 6.6 POST /instructor/programs/{programId}/archive — Archive Program

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/instructor/programs/{programId}/archive` |
| **Authentication** | Instructor (owner) or Admin |

**Behavior:** Archived programs are visible in the catalog but marked as "No longer accepting enrollments." Existing enrolled learners can still access content.

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Program archived. Existing learners can still access content."
  }
}
```

---

### 6.7 DELETE /instructor/programs/{programId} — Delete Program

| Attribute | Value |
|-----------|-------|
| **Method** | `DELETE` |
| **Endpoint** | `/instructor/programs/{programId}` |
| **Authentication** | Instructor (owner) or Admin |

**Behavior:**
- Draft programs → permanently deleted
- Published programs → archived instead (HTTP 200 with warning message)

**Success Response:** `204 No Content` (for draft deletion)

```json
// For published programs (archived instead):
{
  "success": true,
  "data": {
    "message": "Published programs cannot be deleted. The program has been archived instead."
  }
}
```

---

### 6.8 POST /instructor/programs/{programId}/thumbnail — Upload Thumbnail

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/instructor/programs/{programId}/thumbnail` |
| **Authentication** | Instructor (owner) or Admin |
| **Content-Type** | `multipart/form-data` |

**Request Body (Form-Data):**

| Field | Type | Rule |
|-------|------|------|
| `thumbnail` | File | Required, JPEG or PNG only, max 5 MB, min 400x300 px, max 1920x1080 px |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "thumbnailUrl": "https://skillenroll.blob.core.windows.net/thumbnails/program-10.jpg",
    "message": "Thumbnail uploaded successfully"
  }
}
```

---

## 7. Curriculum API (Modules & Lessons)

### 7.1 POST /instructor/programs/{programId}/modules — Add Module

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/instructor/programs/{programId}/modules` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:**
```json
{
  "title": "Getting Started with React",
  "description": "Set up your first React project",
  "isOptional": false
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `title` | Required, max 255 chars |
| `description` | Optional, max 5000 chars |
| `isOptional` | Optional, boolean, default false |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 50,
    "programId": 10,
    "title": "Getting Started with React",
    "sortOrder": 1,
    "isOptional": false,
    "message": "Module added successfully"
  }
}
```

---

### 7.2 PUT /instructor/modules/{moduleId} — Update Module

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/instructor/modules/{moduleId}` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:**
```json
{
  "title": "Updated Module Title",
  "description": "Updated description",
  "isOptional": true
}
```

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Module updated successfully"
  }
}
```

---

### 7.3 DELETE /instructor/modules/{moduleId} — Delete Module

| Attribute | Value |
|-----------|-------|
| **Method** | `DELETE` |
| **Endpoint** | `/instructor/modules/{moduleId}` |
| **Authentication** | Instructor (owner) or Admin |

**Behavior:** Also deletes all lessons within the module (CASCADE). If the program is published, this creates a draft.

**Success Response:** `204 No Content`

---

### 7.4 PUT /instructor/programs/{programId}/modules/reorder — Reorder Modules

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/instructor/programs/{programId}/modules/reorder` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:**
```json
{
  "moduleIds": [50, 52, 51, 53]
}
```

**Behavior:** The array defines the new order. The server updates `sort_order` for all modules and returns the updated list.

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Modules reordered successfully"
  }
}
```

---

### 7.5 POST /instructor/modules/{moduleId}/lessons — Add Lesson

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/instructor/modules/{moduleId}/lessons` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:**
```json
{
  "title": "What is React?",
  "contentType": "VIDEO",
  "contentBody": "",
  "contentUrl": "https://www.youtube.com/watch?v=abc123",
  "durationMinutes": 12,
  "isFreePreview": true
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `title` | Required, max 255 chars |
| `contentType` | Required, must be `TEXT`, `VIDEO`, `DOCUMENT`, `LINK`, `QUIZ`, or `ASSIGNMENT` |
| `contentBody` | Required if `contentType` is `TEXT`, max 65535 chars |
| `contentUrl` | Required if `contentType` is `VIDEO`, `DOCUMENT`, or `LINK`; valid URL |
| `durationMinutes` | Optional, max 9999 |
| `isFreePreview` | Optional, boolean, default false |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 200,
    "moduleId": 50,
    "title": "What is React?",
    "contentType": "VIDEO",
    "sortOrder": 1,
    "message": "Lesson added successfully"
  }
}
```

---

### 7.6 PUT /instructor/lessons/{lessonId} — Update Lesson

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/instructor/lessons/{lessonId}` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:** Same schema as Add Lesson (all fields optional).

**Success Response:** `200 OK`

---

### 7.7 DELETE /instructor/lessons/{lessonId} — Delete Lesson

| Attribute | Value |
|-----------|-------|
| **Method** | `DELETE` |
| **Endpoint** | `/instructor/lessons/{lessonId}` |
| **Authentication** | Instructor (owner) or Admin |

**Success Response:** `204 No Content`

---

### 7.8 PUT /instructor/modules/{moduleId}/lessons/reorder — Reorder Lessons

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/instructor/modules/{moduleId}/lessons/reorder` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:**
```json
{
  "lessonIds": [200, 203, 201, 202]
}
```

**Success Response:** `200 OK`

---

### 7.9 POST /instructor/lessons/{lessonId}/materials — Upload Lesson Material

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/instructor/lessons/{lessonId}/materials` |
| **Authentication** | Instructor (owner) or Admin |
| **Content-Type** | `multipart/form-data` |

**Request Body (Form-Data):**

| Field | Type | Rule |
|-------|------|------|
| `file` | File | Required, allowed types: PDF, JPEG, PNG, MP4, MP3, ZIP, DOCX. Max 50 MB |
| `displayName` | String | Optional, max 255 chars |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 300,
    "lessonId": 200,
    "fileName": "react-cheatsheet.pdf",
    "fileUrl": "https://skillenroll.blob.core.windows.net/materials/lesson-200/react-cheatsheet.pdf",
    "fileSizeBytes": 245000,
    "displayName": "React Quick Reference",
    "message": "Material uploaded successfully"
  }
}
```

---

### 7.10 DELETE /instructor/materials/{materialId} — Delete Lesson Material

| Attribute | Value |
|-----------|-------|
| **Method** | `DELETE` |
| **Endpoint** | `/instructor/materials/{materialId}` |
| **Authentication** | Instructor (owner) or Admin |

**Success Response:** `204 No Content`

---

## 8. Enrollment API

### 8.1 POST /enrollments — Enroll in Program

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/enrollments` |
| **Authentication** | Learner role required |

**Request Body:**
```json
{
  "programId": 1
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `programId` | Required, program must exist, must be published, must not be archived, must not be full, user must not already be enrolled |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 500,
    "programId": 1,
    "programTitle": "Introduction to Web Development",
    "status": "ACTIVE",
    "enrolledAt": "2026-07-28T10:30:00.000Z",
    "message": "Successfully enrolled! Start learning now."
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `400` | `ALREADY_ENROLLED` | User is already enrolled in this program |
| `400` | `PROGRAM_NOT_PUBLISHED` | Program is not published |
| `400` | `PROGRAM_FULL` | Program has reached maximum enrollment capacity |
| `400` | `EMAIL_NOT_VERIFIED` | User must verify email before enrolling |
| `404` | `PROGRAM_NOT_FOUND` | Program ID does not exist |

---

### 8.2 GET /enrollments — List My Enrollments (Learner)

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/enrollments` |
| **Authentication** | Authenticated (returns own enrollments only) |

**Query Parameters:** `page`, `size`, `status` (ACTIVE / COMPLETED / WITHDRAWN / ALL)

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 500,
      "programId": 1,
      "programTitle": "Introduction to Web Development",
      "programSlug": "intro-to-web-development",
      "thumbnailUrl": "https://...thumbnails/program-1.jpg",
      "instructorName": "Jane Smith",
      "status": "ACTIVE",
      "progressPercentage": 45.50,
      "lastAccessedAt": "2026-07-27T14:00:00.000Z",
      "enrolledAt": "2026-07-20T08:00:00.000Z"
    }
  ],
  "page": { "...standard pagination..." }
}
```

---

### 8.3 POST /enrollments/{enrollmentId}/withdraw — Unenroll / Withdraw

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/enrollments/{enrollmentId}/withdraw` |
| **Authentication** | Learner (owner) |

**Path Parameters:**

| Parameter | Rule |
|-----------|------|
| `enrollmentId` | Required, must belong to the authenticated user |

**Behavior:** Within 7-day cancellation window → enrollment is withdrawn. After 7 days → withdrawal is allowed but progress is not retained.

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "You have been unenrolled from the program. Progress data will be retained for 30 days."
  }
}
```

---

### 8.4 POST /enrollments/waitlist — Join Waitlist

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/enrollments/waitlist` |
| **Authentication** | Learner role required |

**Request Body:**
```json
{
  "programId": 1
}
```

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 50,
    "position": 3,
    "message": "You've been added to the waitlist at position 3."
  }
}
```

---

### 8.5 GET /instructor/programs/{programId}/enrollments — View Enrollment Roster (Instructor)

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/instructor/programs/{programId}/enrollments` |
| **Authentication** | Instructor (owner) or Admin |

**Query Parameters:** `page`, `size`, `status` (ACTIVE / COMPLETED / WITHDRAWN)

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 500,
      "user": {
        "id": 1,
        "fullName": "John Doe",
        "email": "learner@example.com",
        "avatarUrl": "https://...avatars/user-1.jpg"
      },
      "status": "ACTIVE",
      "progressPercentage": 45.50,
      "enrolledAt": "2026-07-20T08:00:00.000Z",
      "lastActivityAt": "2026-07-27T14:00:00.000Z"
    }
  ],
  "page": { "...standard pagination..." }
}
```

---

## 9. Learning API

### 9.1 GET /learning/{enrollmentId} — Get Learning View

**Description:** Get the full curriculum tree with progress indicators for an enrolled program.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/learning/{enrollmentId}` |
| **Authentication** | Learner (owner) |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "enrollmentId": 500,
    "programId": 1,
    "programTitle": "Introduction to Web Development",
    "instructorName": "Jane Smith",
    "progressPercentage": 45.50,
    "isCompleted": false,
    "bookmark": {
      "moduleId": 10,
      "lessonId": 104
    },
    "curriculum": [
      {
        "moduleId": 10,
        "title": "Getting Started",
        "description": "Set up your development environment",
        "sortOrder": 1,
        "status": "COMPLETED",
        "completedAt": "2026-07-22T10:00:00.000Z",
        "lessons": [
          {
            "lessonId": 100,
            "title": "Welcome to the Course",
            "contentType": "VIDEO",
            "durationMinutes": 10,
            "sortOrder": 1,
            "status": "COMPLETED",
            "completedAt": "2026-07-22T10:10:00.000Z"
          },
          {
            "lessonId": 101,
            "title": "What is Web Development?",
            "contentType": "TEXT",
            "durationMinutes": 15,
            "sortOrder": 2,
            "status": "COMPLETED",
            "completedAt": "2026-07-22T10:30:00.000Z"
          }
        ]
      },
      {
        "moduleId": 11,
        "title": "HTML Fundamentals",
        "sortOrder": 2,
        "status": "IN_PROGRESS",
        "lessons": [
          {
            "lessonId": 104,
            "title": "HTML Document Structure",
            "contentType": "TEXT",
            "durationMinutes": 20,
            "sortOrder": 1,
            "status": "IN_PROGRESS",
            "isCurrentLesson": true
          },
          {
            "lessonId": 105,
            "title": "HTML Elements and Tags",
            "contentType": "VIDEO",
            "durationMinutes": 25,
            "sortOrder": 2,
            "status": "LOCKED"
          }
        ]
      },
      {
        "moduleId": 12,
        "title": "CSS Styling",
        "sortOrder": 3,
        "status": "LOCKED",
        "lessons": []
      }
    ]
  }
}
```

---

### 9.2 GET /learning/{enrollmentId}/lessons/{lessonId} — Get Lesson Content

**Description:** Get the full content of a specific lesson (text, video URL, materials).

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/learning/{enrollmentId}/lessons/{lessonId}` |
| **Authentication** | Learner (owner) |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "lessonId": 104,
    "moduleId": 11,
    "title": "HTML Document Structure",
    "contentType": "TEXT",
    "contentBody": "<h2>HTML Document Structure</h2><p>Every HTML document follows a basic structure...</p><pre><code>&lt;!DOCTYPE html&gt;\n&lt;html&gt;\n  &lt;head&gt;\n    &lt;title&gt;My Page&lt;/title&gt;\n  &lt;/head&gt;\n  &lt;body&gt;\n    &lt;h1&gt;Hello World&lt;/h1&gt;\n  &lt;/body&gt;\n&lt;/html&gt;</code></pre>",
    "durationMinutes": 20,
    "materials": [
      {
        "id": 300,
        "displayName": "HTML Cheat Sheet",
        "fileUrl": "https://skillenroll.blob.core.windows.net/materials/lesson-104/html-cheatsheet.pdf",
        "fileSizeBytes": 120000,
        "materialType": "PDF"
      }
    ],
    "hasQuiz": false,
    "hasAssignment": false,
    "isCompleted": false,
    "notes": []
  }
}
```

---

### 9.3 POST /learning/{enrollmentId}/lessons/{lessonId}/complete — Mark Lesson Complete

**Description:** Mark a lesson as completed for the enrolled learner.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/learning/{enrollmentId}/lessons/{lessonId}/complete` |
| **Authentication** | Learner (owner) |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Lesson marked as completed",
    "moduleCompleted": false,
    "progressPercentage": 50.00,
    "nextLessonId": 105,
    "nextModuleId": null
  }
}
```

> **Note:** `moduleCompleted` is `true` if all lessons in the module are now complete. `nextLessonId` points to the next uncompleted lesson. `nextModuleId` is set if it's the first lesson in a new module.

---

### 9.4 GET /learning/{enrollmentId}/notes — Get Lesson Notes

**Description:** Get all notes created by the learner across all lessons in this enrollment.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/learning/{enrollmentId}/notes` |
| **Authentication** | Learner (owner) |

**Query Parameters:** `page`, `size`, `moduleId` (optional filter)

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "lessonId": 104,
      "lessonTitle": "HTML Document Structure",
      "noteText": "Remember: DOCTYPE must be the very first line",
      "highlightedText": "&lt;!DOCTYPE html&gt;",
      "color": "#FFFF00",
      "createdAt": "2026-07-25T14:00:00.000Z",
      "updatedAt": "2026-07-25T14:00:00.000Z"
    }
  ],
  "page": { "...standard pagination..." }
}
```

---

### 9.5 POST /learning/lessons/{lessonId}/notes — Create Note

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/learning/lessons/{lessonId}/notes` |
| **Authentication** | Authenticated (any learner) |

**Request Body:**
```json
{
  "noteText": "Remember: DOCTYPE must be the very first line",
  "highlightedText": "<!DOCTYPE html>",
  "color": "#FFFF00"
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `noteText` | Required, max 5000 chars |
| `highlightedText` | Optional, max 1000 chars |
| `color` | Optional, valid hex color (default: `#FFFF00`) |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 10,
    "message": "Note saved successfully"
  }
}
```

---

### 9.6 PUT /learning/notes/{noteId} — Update Note

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/learning/notes/{noteId}` |
| **Authentication** | Authenticated (owner) |

**Request Body:** Same schema as Create Note (all fields optional).

**Success Response:** `200 OK`

---

### 9.7 DELETE /learning/notes/{noteId} — Delete Note

| Attribute | Value |
|-----------|-------|
| **Method** | `DELETE` |
| **Endpoint** | `/learning/notes/{noteId}` |
| **Authentication** | Authenticated (owner) |

**Success Response:** `204 No Content`

---

## 10. Assessment API

### 10.1 POST /instructor/lessons/{lessonId}/quizzes — Create Quiz

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/instructor/lessons/{lessonId}/quizzes` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:**
```json
{
  "title": "HTML Basics Quiz",
  "instructions": "Test your understanding of HTML fundamentals",
  "timeLimitMinutes": 15,
  "passingScorePercent": 70.00,
  "maxAttempts": 3,
  "shuffleQuestions": true,
  "showResultsImmediately": true
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `title` | Required, max 255 chars |
| `instructions` | Optional, max 5000 chars |
| `timeLimitMinutes` | Optional, integer, min 1, max 480 (8 hours) |
| `passingScorePercent` | Optional, decimal 0.00–100.00, default 60.00 |
| `maxAttempts` | Optional, integer, min 1, max 100, default 1 |
| `shuffleQuestions` | Optional, boolean, default false |
| `showResultsImmediately` | Optional, boolean, default true |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 30,
    "lessonId": 104,
    "title": "HTML Basics Quiz",
    "totalPoints": 0,
    "message": "Quiz created. Add questions to it."
  }
}
```

---

### 10.2 PUT /instructor/quizzes/{quizId} — Update Quiz

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/instructor/quizzes/{quizId}` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:** Same schema as Create Quiz (all fields optional).

**Success Response:** `200 OK`

---

### 10.3 DELETE /instructor/quizzes/{quizId} — Delete Quiz

| Attribute | Value |
|-----------|-------|
| **Method** | `DELETE` |
| **Endpoint** | `/instructor/quizzes/{quizId}` |
| **Authentication** | Instructor (owner) or Admin |

**Behavior:** Also deletes all questions associated with the quiz.

**Success Response:** `204 No Content`

---

### 10.4 POST /instructor/quizzes/{quizId}/questions — Add Question

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/instructor/quizzes/{quizId}/questions` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body (Multiple Choice — Single):**
```json
{
  "questionType": "MULTIPLE_CHOICE_SINGLE",
  "questionText": "What does HTML stand for?",
  "options": [
    { "key": "A", "text": "Hyper Text Markup Language" },
    { "key": "B", "text": "High Tech Modern Language" },
    { "key": "C", "text": "Hyperlinks and Text Markup Language" },
    { "key": "D", "text": "Home Tool Markup Language" }
  ],
  "correctAnswer": "A",
  "points": 5,
  "explanation": "HTML stands for Hyper Text Markup Language"
}
```

**Request Body (Multiple Choice — Multi):**
```json
{
  "questionType": "MULTIPLE_CHOICE_MULTI",
  "questionText": "Which of the following are JavaScript data types?",
  "options": [
    { "key": "A", "text": "String" },
    { "key": "B", "text": "Integer" },
    { "key": "C", "text": "Boolean" },
    { "key": "D", "text": "Object" }
  ],
  "correctAnswer": ["A", "C", "D"],
  "points": 5,
  "explanation": "JavaScript has String, Boolean, Object, and Number (not Integer)"
}
```

**Request Body (True/False):**
```json
{
  "questionType": "TRUE_FALSE",
  "questionText": "JavaScript is a statically typed language.",
  "options": [
    { "key": "TRUE", "text": "True" },
    { "key": "FALSE", "text": "False" }
  ],
  "correctAnswer": "FALSE",
  "points": 2,
  "explanation": "JavaScript is dynamically typed"
}
```

**Request Body (Short Answer):**
```json
{
  "questionType": "SHORT_ANSWER",
  "questionText": "What method is used to print to the console in JavaScript?",
  "correctAnswer": "console.log",
  "points": 3
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `questionType` | Required, must be a valid type |
| `questionText` | Required, max 5000 chars |
| `options` | Required for MC and T/F types; must have at least 2 options |
| `correctAnswer` | Required; format varies by type (string, array, or string) |
| `points` | Optional, integer, min 0, max 1000, default 1 |
| `explanation` | Optional, max 2000 chars |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 100,
    "quizId": 30,
    "questionType": "MULTIPLE_CHOICE_SINGLE",
    "sortOrder": 1,
    "message": "Question added successfully"
  }
}
```

---

### 10.5 PUT /instructor/questions/{questionId} — Update Question

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/instructor/questions/{questionId}` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:** Same fields as Add Question (all optional).

**Success Response:** `200 OK`

---

### 10.6 DELETE /instructor/questions/{questionId} — Delete Question

| Attribute | Value |
|-----------|-------|
| **Method** | `DELETE` |
| **Endpoint** | `/instructor/questions/{questionId}` |
| **Authentication** | Instructor (owner) or Admin |

**Success Response:** `204 No Content`

---

### 10.7 GET /assessments/quizzes/{quizId} — Get Quiz Details (Learner)

**Description:** Get quiz metadata (not answers) before attempting. Used to show the start screen.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/assessments/quizzes/{quizId}` |
| **Authentication** | Authenticated (must be enrolled in the program) |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 30,
    "title": "HTML Basics Quiz",
    "instructions": "Test your understanding of HTML fundamentals",
    "timeLimitMinutes": 15,
    "passingScorePercent": 70.00,
    "maxAttempts": 3,
    "totalPoints": 20,
    "totalQuestions": 4,
    "shuffleQuestions": true,
    "previousAttempts": [
      {
        "id": 500,
        "attemptNumber": 1,
        "score": 12,
        "scorePercentage": 60.00,
        "isPassed": false,
        "submittedAt": "2026-07-26T10:00:00.000Z"
      }
    ],
    "remainingAttempts": 2
  }
}
```

---

### 10.8 POST /assessments/quizzes/{quizId}/start — Start Quiz Attempt

**Description:** Start a new quiz attempt. Returns the questions (shuffled if configured) and starts the timer.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/assessments/quizzes/{quizId}/start` |
| **Authentication** | Authenticated (must be enrolled) |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "attemptId": 501,
    "attemptNumber": 1,
    "startedAt": "2026-07-28T10:30:00.000Z",
    "timeLimitMinutes": 15,
    "endsAt": "2026-07-28T10:45:00.000Z",
    "questions": [
      {
        "id": 101,
        "questionType": "MULTIPLE_CHOICE_SINGLE",
        "questionText": "Which of the following is a JavaScript framework?",
        "options": [
          { "key": "A", "text": "React" },
          { "key": "B", "text": "Laravel" },
          { "key": "C", "text": "Django" },
          { "key": "D", "text": "Spring" }
        ],
        "points": 5
      }
    ]
  }
}
```

> **Note:** `correctAnswer` and `explanation` are NOT included in the response — they are only revealed after submission (if configured).

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `400` | `MAX_ATTEMPTS_REACHED` | No remaining attempts |
| `400` | `ACTIVE_ATTEMPT_EXISTS` | User has an in-progress attempt |

---

### 10.9 POST /assessments/attempts/{attemptId}/submit — Submit Quiz Attempt

**Description:** Submit answers for a quiz attempt. Auto-graded questions are scored immediately.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/assessments/attempts/{attemptId}/submit` |
| **Authentication** | Authenticated (owner) |

**Request Body:**
```json
{
  "answers": [
    {
      "questionId": 100,
      "answer": "A"
    },
    {
      "questionId": 101,
      "answer": ["A", "C"]
    },
    {
      "questionId": 102,
      "answer": "FALSE"
    },
    {
      "questionId": 103,
      "answer": "console.log"
    }
  ]
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `answers` | Required, must contain one entry per question in the quiz |
| `answer` | Format must match question type; not empty |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "attemptId": 501,
    "score": 15,
    "totalPoints": 20,
    "scorePercentage": 75.00,
    "isPassed": true,
    "isFullyGraded": false,
    "pendingManualGrading": 1,
    "results": [
      {
        "questionId": 100,
        "questionText": "Which of the following is a JavaScript framework?",
        "isCorrect": true,
        "pointsEarned": 5,
        "correctAnswer": "A",
        "explanation": "React is a JavaScript library for building user interfaces"
      },
      {
        "questionId": 103,
        "questionText": "What method is used to print to the console in JavaScript?",
        "isCorrect": null,
        "pointsEarned": null,
        "status": "AWAITING_GRADING"
      }
    ],
    "submittedAt": "2026-07-28T10:40:00.000Z"
  }
}
```

---

### 10.10 POST /assessments/attempts/{attemptId}/auto-submit — Auto-Submit on Timer Expiry

**Description:** Called by the frontend when the timer expires. Automatically submits whatever answers have been provided.

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/assessments/attempts/{attemptId}/auto-submit` |
| **Authentication** | Authenticated (owner) |

**Response:** Same as Submit Quiz Attempt. Unanswered questions are scored as zero.

---

### 10.11 GET /instructor/assessments/attempts/{attemptId} — View Attempt Detail (Instructor)

**Description:** View a learner's quiz attempt for grading.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/instructor/assessments/attempts/{attemptId}` |
| **Authentication** | Instructor (owner of the program) or Admin |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "attemptId": 501,
    "learner": {
      "id": 1,
      "fullName": "John Doe",
      "email": "learner@example.com"
    },
    "quizTitle": "HTML Basics Quiz",
    "attemptNumber": 1,
    "score": 15,
    "totalPoints": 20,
    "isPassed": true,
    "isFullyGraded": false,
    "answers": [
      {
        "questionId": 100,
        "questionType": "MULTIPLE_CHOICE_SINGLE",
        "questionText": "Which of the following is a JavaScript framework?",
        "submittedAnswer": "A",
        "isCorrect": true,
        "pointsEarned": 5
      },
      {
        "questionId": 103,
        "questionType": "SHORT_ANSWER",
        "questionText": "What method is used to print to the console in JavaScript?",
        "submittedAnswer": "console.log",
        "isCorrect": null,
        "pointsEarned": null,
        "status": "AWAITING_GRADING"
      }
    ],
    "startedAt": "2026-07-28T10:30:00.000Z",
    "submittedAt": "2026-07-28T10:40:00.000Z"
  }
}
```

---

### 10.12 PUT /instructor/assessments/answers/{answerId}/grade — Grade Answer

**Description:** Manually grade a short-answer question.

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/instructor/assessments/answers/{answerId}/grade` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:**
```json
{
  "pointsEarned": 3,
  "feedback": "Correct! console.log is the standard method."
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `pointsEarned` | Required, must be between 0 and the question's max points |
| `feedback` | Optional, max 2000 chars |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "answerId": 500,
    "pointsEarned": 3,
    "message": "Answer graded successfully"
  }
}
```

---

### 10.13 POST /assessments/lessons/{lessonId}/assignments/submit — Submit Assignment

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/assessments/lessons/{lessonId}/assignments/submit` |
| **Authentication** | Learner (must be enrolled in the program) |
| **Content-Type** | `multipart/form-data` |

**Request Body (Form-Data):**

| Field | Type | Rule |
|-------|------|------|
| `submissionText` | String | Optional if file is provided, max 50000 chars |
| `file` | File | Optional if text is provided, allowed: PDF, DOCX, ZIP, JPEG, PNG, max 50 MB |

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 700,
    "message": "Assignment submitted successfully"
  }
}
```

---

### 10.14 PUT /instructor/assignments/{submissionId}/grade — Grade Assignment

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/instructor/assignments/{submissionId}/grade` |
| **Authentication** | Instructor (owner) or Admin |

**Request Body:**
```json
{
  "grade": 85.00,
  "feedback": "Great work! Your understanding of the concepts is solid. Consider adding more examples in future assignments."
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `grade` | Required, decimal, min 0, must be ≤ `max_grade` |
| `feedback` | Optional, max 5000 chars |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Assignment graded successfully. Notification sent to learner."
  }
}
```

---

### 10.15 GET /assessments/grades — Get Grade Book (Learner)

**Description:** Get all assessment scores across all enrolled programs for the authenticated learner.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/assessments/grades` |
| **Authentication** | Learner |

**Query Parameters:** `programId` (optional filter)

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "programId": 1,
      "programTitle": "Introduction to Web Development",
      "assessments": [
        {
          "type": "QUIZ",
          "title": "HTML Basics Quiz",
          "score": 15,
          "totalPoints": 20,
          "percentage": 75.00,
          "isPassed": true,
          "attemptsUsed": 2,
          "submittedAt": "2026-07-28T10:40:00.000Z"
        },
        {
          "type": "ASSIGNMENT",
          "title": "Build a Personal Website",
          "score": 85.00,
          "maxGrade": 100.00,
          "percentage": 85.00,
          "feedback": "Great work!",
          "submittedAt": "2026-07-25T14:00:00.000Z",
          "gradedAt": "2026-07-26T09:00:00.000Z"
        }
      ]
    }
  ]
}
```

---

### 10.16 GET /instructor/programs/{programId}/grades — Get Grade Book (Instructor)

**Description:** View aggregate assessment scores for all learners in a program. Add `?status=AWAITING_GRADING` to filter only submissions that need manual grading.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/instructor/programs/{programId}/grades` |
| **Authentication** | Instructor (owner) or Admin |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "learnerId": 1,
      "learnerName": "John Doe",
      "email": "learner@example.com",
      "quizAverage": 82.50,
      "assignmentAverage": 88.00,
      "overallAverage": 85.25,
      "assessments": [
        {
          "title": "HTML Basics Quiz",
          "score": 15,
          "totalPoints": 20,
          "percentage": 75.00
        }
      ]
    }
  ]
}
```

---

## 11. Notification API

### 11.1 GET /notifications — List Notifications

**Description:** Get paginated notifications for the authenticated user.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/notifications` |
| **Authentication** | Authenticated |

**Query Parameters:** `page`, `size`, `isRead` (true/false filter), `type` (filter by notification type)

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 2000,
      "type": "GRADE",
      "title": "Grade Posted",
      "message": "Your assignment 'Build a Personal Website' has been graded: 85/100",
      "linkUrl": "/learning/500/assignments/700",
      "isRead": false,
      "createdAt": "2026-07-28T10:45:00.000Z"
    },
    {
      "id": 1999,
      "type": "ENROLLMENT",
      "title": "Enrollment Confirmed",
      "message": "You enrolled in 'Introduction to Web Development'",
      "linkUrl": "/learning/500",
      "isRead": true,
      "createdAt": "2026-07-20T08:00:00.000Z"
    }
  ],
  "page": { "...standard pagination..." },
  "unreadCount": 1
}
```

---

### 11.2 PUT /notifications/{notificationId}/read — Mark Notification as Read

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/notifications/{notificationId}/read` |
| **Authentication** | Authenticated (owner) |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Notification marked as read"
  }
}
```

---

### 11.3 PUT /notifications/read-all — Mark All as Read

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/notifications/read-all` |
| **Authentication** | Authenticated |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "All notifications marked as read"
  }
}
```

---

### 11.4 GET /notifications/unread-count — Get Unread Count

**Description:** Get the count of unread notifications (for badge display).

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/notifications/unread-count` |
| **Authentication** | Authenticated |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "unreadCount": 3
  }
}
```

---

## 12. Reporting API

### 12.1 GET /dashboard — Learner Dashboard

**Description:** Get the learner's personal dashboard summary.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/dashboard` |
| **Authentication** | Learner |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "summary": {
      "totalEnrolled": 4,
      "inProgress": 2,
      "completed": 1,
      "withdrawn": 1
    },
    "recentActivity": [
      {
        "type": "GRADE",
        "message": "Assignment 'Build a Personal Website' graded: 85/100",
        "programTitle": "Introduction to Web Development",
        "timestamp": "2026-07-28T10:45:00.000Z"
      },
      {
        "type": "LESSON_COMPLETED",
        "message": "Completed 'HTML Document Structure'",
        "programTitle": "Introduction to Web Development",
        "timestamp": "2026-07-27T14:00:00.000Z"
      }
    ],
    "upcomingDeadlines": [
      {
        "type": "ASSIGNMENT",
        "title": "CSS Layout Challenge",
        "programTitle": "Introduction to Web Development",
        "dueDate": "2026-08-01T23:59:00.000Z"
      }
    ],
    "certificates": [
      {
        "id": 50,
        "programTitle": "HTML Fundamentals",
        "issuedAt": "2026-07-15T10:00:00.000Z",
        "certificateUrl": "/certificates/50/download"
      }
    ],
    "inProgressPrograms": [
      {
        "enrollmentId": 500,
        "programId": 1,
        "title": "Introduction to Web Development",
        "slug": "intro-to-web-development",
        "thumbnailUrl": "https://...thumbnails/program-1.jpg",
        "instructorName": "Jane Smith",
        "progressPercentage": 45.50,
        "lastAccessedAt": "2026-07-27T14:00:00.000Z"
      }
    ]
  }
}
```

---

### 12.2 GET /instructor/reports/{programId}/analytics — Instructor Program Analytics

**Description:** Get detailed analytics for a specific program.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/instructor/reports/{programId}/analytics` |
| **Authentication** | Instructor (owner) or Admin |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "programId": 1,
    "programTitle": "Introduction to Web Development",
    "enrollmentStats": {
      "totalEnrolled": 1450,
      "activeLearners": 980,
      "completedLearners": 320,
      "withdrawnLearners": 150,
      "completionRate": 22.07
    },
    "engagementMetrics": {
      "averageProgress": 48.50,
      "averageQuizScore": 72.30,
      "averageTimeSpentHours": 12.5
    },
    "moduleBreakdown": [
      {
        "moduleId": 10,
        "moduleTitle": "Getting Started",
        "averageCompletion": 95.00,
        "totalLearnersCompleted": 1378
      },
      {
        "moduleId": 11,
        "moduleTitle": "HTML Fundamentals",
        "averageCompletion": 72.00,
        "totalLearnersCompleted": 1044,
        "dropOffRate": 23.00
      }
    ],
    "learnersAtRisk": [
      {
        "learnerId": 1,
        "learnerName": "John Doe",
        "email": "learner@example.com",
        "progressPercentage": 10.00,
        "lastActivityAt": "2026-07-01T08:00:00.000Z",
        "daysInactive": 27
      }
    ]
  }
}
```

---

### 12.3 GET /admin/reports/overview — Admin System Overview

**Description:** Get system-wide analytics (admin only).

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/admin/reports/overview` |
| **Authentication** | Admin only |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "platformStats": {
      "totalUsers": 5000,
      "activeLearners": 3500,
      "activeInstructors": 120,
      "totalPrograms": 145,
      "publishedPrograms": 120,
      "totalEnrollments": 25000,
      "completionRate": 28.50
    },
    "trends": {
      "newUsersThisMonth": 450,
      "newEnrollmentsThisMonth": 3200,
      "programsPublishedThisMonth": 8
    },
    "topPrograms": [
      {
        "id": 1,
        "title": "Introduction to Web Development",
        "enrollmentCount": 1450,
        "completionRate": 22.07,
        "averageRating": 4.5
      }
    ]
  }
}
```

---

### 12.4 GET /admin/reports/export — Export Report

**Description:** Export a report as CSV or PDF.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/admin/reports/export` |
| **Authentication** | Admin or Instructor (own programs) |

**Query Parameters:**

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `type` | Enum | Yes | `ENROLLMENTS`, `GRADES`, `PROGRESS`, `USERS` |
| `programId` | Integer | No | Filter by program (required for Instructors) |
| `format` | Enum | Yes | `CSV` or `PDF` |
| `startDate` | Date | No | Start of date range (ISO 8601) |
| `endDate` | Date | No | End of date range (ISO 8601) |

**Success Response:** `200 OK` (binary file download with appropriate Content-Type and Content-Disposition headers)

---

## 13. Certificate API

### 13.1 GET /certificates/{certificateUuid}/verify — Verify Certificate

**Description:** Public endpoint to verify a certificate's authenticity (no authentication required).

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/certificates/{certificateUuid}/verify` |
| **Authentication** | None (public) |

**Path Parameters:**

| Parameter | Rule |
|-----------|------|
| `certificateUuid` | Required, valid UUID |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "certificateUuid": "abc12345-6789-4def-abcd-ef1234567890",
    "isValid": true,
    "learnerName": "John Doe",
    "programTitle": "Introduction to Web Development",
    "completionDate": "2026-07-15T10:00:00.000Z",
    "durationHours": 24.5,
    "issuedBy": "SkillEnroll"
  }
}
```

**Error Responses:**

| Status | Code | Scenario |
|--------|------|----------|
| `404` | `CERTIFICATE_NOT_FOUND` | UUID does not match any certificate |
| `200` | (isValid: false, isRevoked: true) | Certificate was revoked |

---

### 13.2 GET /my-certificates — List My Certificates (Learner)

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/my-certificates` |
| **Authentication** | Authenticated (returns own certificates) |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 50,
      "certificateUuid": "abc12345-6789-4def-abcd-ef1234567890",
      "programTitle": "Introduction to Web Development",
      "programSlug": "intro-to-web-development",
      "instructorName": "Jane Smith",
      "issuedAt": "2026-07-15T10:00:00.000Z",
      "downloadUrl": "/certificates/abc12345-6789-4def-abcd-ef1234567890/download"
    }
  ]
}
```

---

### 13.3 GET /certificates/{certificateUuid}/download — Download Certificate PDF

**Description:** Download the certificate as a PDF file.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/certificates/{certificateUuid}/download` |
| **Authentication** | Authenticated (certificate owner) |

**Success Response:** `200 OK` — Binary PDF file stream with `Content-Type: application/pdf` and `Content-Disposition: attachment; filename="certificate-intro-to-web-development.pdf"`

---

## 14. Admin API

### 14.1 GET /admin/users — List Users

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/admin/users` |
| **Authentication** | Admin only |

**Query Parameters:** `page`, `size`, `search` (name or email), `role`, `status`, `sort`

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "uuid": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "email": "learner@example.com",
      "fullName": "John Doe",
      "role": "LEARNER",
      "status": "ACTIVE",
      "isEmailVerified": true,
      "lastLoginAt": "2026-07-28T10:30:00.000Z",
      "createdAt": "2026-07-01T08:00:00.000Z"
    }
  ],
  "page": { "...standard pagination..." }
}
```

---

### 14.2 POST /admin/users — Create User

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/admin/users` |
| **Authentication** | Admin only |

**Request Body:**
```json
{
  "email": "newuser@example.com",
  "password": "TempPass123!",
  "fullName": "New User",
  "role": "INSTRUCTOR",
  "isEmailVerified": true
}
```

**Validation Rules:** Same as registration, plus admin can set `isEmailVerified` to skip OTP.

**Success Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": 100,
    "email": "newuser@example.com",
    "message": "User created successfully"
  }
}
```

---

### 14.3 PUT /admin/users/{userId} — Update User

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/admin/users/{userId}` |
| **Authentication** | Admin only |

**Request Body:**
```json
{
  "fullName": "Updated Name",
  "role": "ADMIN",
  "status": "ACTIVE"
}
```

**Behavior:** All actions are logged to the audit log.

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "User updated successfully"
  }
}
```

---

### 14.4 POST /admin/users/{userId}/suspend — Suspend User

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/admin/users/{userId}/suspend` |
| **Authentication** | Admin only |

**Request Body:**
```json
{
  "reason": "Violation of platform terms of service"
}
```

**Success Response:** `200 OK`

---

### 14.5 POST /admin/users/{userId}/reactivate — Reactivate User

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/admin/users/{userId}/reactivate` |
| **Authentication** | Admin only |

**Success Response:** `200 OK`

---

### 14.6 DELETE /admin/users/{userId} — Delete User

| Attribute | Value |
|-----------|-------|
| **Method** | `DELETE` |
| **Endpoint** | `/admin/users/{userId}` |
| **Authentication** | Admin only |

**Behavior:** PII is anonymized (email replaced, name replaced, password hash removed). Related data (enrollments, progress) is retained for analytics but disassociated from the user identity.

**Success Response:** `204 No Content`

---

### 14.7 POST /admin/bulk/invite — Bulk Invite Users

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/admin/bulk/invite` |
| **Authentication** | Admin only |
| **Rate Limited** | Yes (10/min) |
| **Content-Type** | `multipart/form-data` |

**Request Body (Form-Data):**

| Field | Type | Rule |
|-------|------|------|
| `file` | File | Required, CSV file, max 5 MB |
| `role` | String | Required, default role for invited users |

**CSV Format:**
```csv
email,fullName
user1@example.com,User One
user2@example.com,User Two
```

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "totalProcessed": 50,
    "successCount": 48,
    "failureCount": 2,
    "failures": [
      { "row": 3, "email": "invalid-email", "reason": "Invalid email format" },
      { "row": 7, "email": "existing@example.com", "reason": "Email already exists" }
    ],
    "message": "48 users invited successfully. 2 failures."
  }
}
```

---

### 14.8 GET /admin/programs/pending — List Pending Programs for Approval

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/admin/programs/pending` |
| **Authentication** | Admin only |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 10,
      "title": "Advanced React Patterns",
      "instructor": {
        "id": 5,
        "fullName": "Jane Smith",
        "email": "jane@example.com"
      },
      "submittedAt": "2026-07-28T10:00:00.000Z",
      "moduleCount": 5,
      "lessonCount": 25
    }
  ]
}
```

---

### 14.9 POST /admin/programs/{programId}/approve — Approve Program

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/admin/programs/{programId}/approve` |
| **Authentication** | Admin only |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Program approved and published. Instructor has been notified."
  }
}
```

---

### 14.10 POST /admin/programs/{programId}/reject — Reject Program

| Attribute | Value |
|-----------|-------|
| **Method** | `POST` |
| **Endpoint** | `/admin/programs/{programId}/reject` |
| **Authentication** | Admin only |

**Request Body:**
```json
{
  "reason": "The program needs more detailed lesson content and assessment questions before approval."
}
```

**Validation Rules:**

| Field | Rule |
|-------|------|
| `reason` | Required, max 2000 chars |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Program rejected. Feedback has been sent to the instructor."
  }
}
```

---

### 14.11 GET /admin/audit-logs — View Audit Logs

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/admin/audit-logs` |
| **Authentication** | Admin only |

**Query Parameters:** `page`, `size`, `actorId` (filter by admin), `action` (filter by action type), `entityType`, `startDate`, `endDate`

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 5000,
      "actor": {
        "id": 2,
        "fullName": "Admin User"
      },
      "action": "USER_ROLE_CHANGED",
      "entityType": "USER",
      "entityId": 1,
      "details": {
        "previousRole": "LEARNER",
        "newRole": "INSTRUCTOR",
        "changedBy": "Admin User (ID: 2)"
      },
      "ipAddress": "10.0.0.1",
      "createdAt": "2026-07-28T09:00:00.000Z"
    }
  ],
  "page": { "...standard pagination..." }
}
```

---

### 14.12 GET /admin/config — Get System Configuration

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/admin/config` |
| **Authentication** | Admin only |

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "key": "platform.name",
      "value": "SkillEnroll",
      "description": "Display name of the platform",
      "updatedAt": "2026-07-01T08:00:00.000Z",
      "updatedBy": "Admin User"
    },
    {
      "key": "enrollment.cancellation_window_days",
      "value": 7,
      "description": "Number of days a learner can unenroll without losing progress",
      "updatedAt": "2026-07-01T08:00:00.000Z"
    },
    {
      "key": "certificate.template",
      "value": "default",
      "description": "Which certificate template to use for generated PDFs"
    }
  ]
}
```

---

### 14.13 PUT /admin/config/{configKey} — Update System Configuration

| Attribute | Value |
|-----------|-------|
| **Method** | `PUT` |
| **Endpoint** | `/admin/config/{configKey}` |
| **Authentication** | Admin only |

**Request Body:**
```json
{
  "value": "My Learning Platform"
}
```

**Success Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "message": "Configuration updated successfully"
  }
}
```

---

### 14.14 GET /admin/programs — List All Programs (Admin)

**Description:** Admin view of all programs across all instructors.

| Attribute | Value |
|-----------|-------|
| **Method** | `GET` |
| **Endpoint** | `/admin/programs` |
| **Authentication** | Admin only |

**Query Parameters:** `page`, `size`, `status`, `approvalStatus`, `instructorId`, `search`

**Success Response:** `200 OK` — Paginated list of all programs with admin metadata.

---

## 15. API Reference Table

| # | Method | Endpoint | Auth | Module |
|---|--------|----------|------|--------|
| 1 | POST | `/auth/register` | None | AUTH |
| 2 | POST | `/auth/verify-email` | None | AUTH |
| 3 | POST | `/auth/login` | None | AUTH |
| 4 | POST | `/auth/refresh` | None | AUTH |
| 5 | POST | `/auth/logout` | Auth | AUTH |
| 6 | POST | `/auth/forgot-password` | None | AUTH |
| 7 | POST | `/auth/reset-password` | None | AUTH |
| 8 | GET | `/auth/sessions` | Auth | AUTH |
| 9 | DELETE | `/auth/sessions/{sessionId}` | Auth | AUTH |
| 10 | GET | `/users/me` | Auth | USER |
| 11 | PUT | `/users/me` | Auth | USER |
| 12 | POST | `/users/me/avatar` | Auth | USER |
| 13 | PUT | `/users/me/preferences` | Auth | USER |
| 14 | POST | `/users/me/deactivate` | Auth | USER |
| 15 | GET | `/catalog` | None | CAT |
| 16 | GET | `/catalog/{slug}` | None | CAT |
| 17 | GET | `/catalog/{slug}/reviews` | None | CAT |
| 18 | GET | `/categories` | None | CAT |
| 19 | POST | `/categories` | Admin | CAT |
| 20 | PUT | `/categories/{categoryId}` | Admin | CAT |
| 21 | DELETE | `/categories/{categoryId}` | Admin | CAT |
| 22 | GET | `/instructor/programs` | Instr | PM |
| 23 | POST | `/instructor/programs` | Instr | PM |
| 24 | GET | `/instructor/programs/{programId}` | Instr | PM |
| 25 | PUT | `/instructor/programs/{programId}` | Instr | PM |
| 26 | POST | `/instructor/programs/{programId}/publish` | Instr | PM |
| 27 | POST | `/instructor/programs/{programId}/archive` | Instr | PM |
| 28 | DELETE | `/instructor/programs/{programId}` | Instr | PM |
| 29 | POST | `/instructor/programs/{programId}/thumbnail` | Instr | PM |
| 30 | POST | `/instructor/programs/{programId}/modules` | Instr | PM |
| 31 | PUT | `/instructor/modules/{moduleId}` | Instr | PM |
| 32 | DELETE | `/instructor/modules/{moduleId}` | Instr | PM |
| 33 | PUT | `/instructor/programs/{programId}/modules/reorder` | Instr | PM |
| 34 | POST | `/instructor/modules/{moduleId}/lessons` | Instr | PM |
| 35 | PUT | `/instructor/lessons/{lessonId}` | Instr | PM |
| 36 | DELETE | `/instructor/lessons/{lessonId}` | Instr | PM |
| 37 | PUT | `/instructor/modules/{moduleId}/lessons/reorder` | Instr | PM |
| 38 | POST | `/instructor/lessons/{lessonId}/materials` | Instr | PM |
| 39 | DELETE | `/instructor/materials/{materialId}` | Instr | PM |
| 40 | POST | `/enrollments` | Learner | ENR |
| 41 | GET | `/enrollments` | Auth | ENR |
| 42 | POST | `/enrollments/{enrollmentId}/withdraw` | Learner | ENR |
| 43 | POST | `/enrollments/waitlist` | Learner | ENR |
| 44 | GET | `/instructor/programs/{programId}/enrollments` | Instr | ENR |
| 45 | GET | `/learning/{enrollmentId}` | Learner | LRN |
| 46 | GET | `/learning/{enrollmentId}/lessons/{lessonId}` | Learner | LRN |
| 47 | POST | `/learning/{enrollmentId}/lessons/{lessonId}/complete` | Learner | LRN |
| 48 | GET | `/learning/{enrollmentId}/notes` | Learner | LRN |
| 49 | POST | `/learning/lessons/{lessonId}/notes` | Auth | LRN |
| 50 | PUT | `/learning/notes/{noteId}` | Auth | LRN |
| 51 | DELETE | `/learning/notes/{noteId}` | Auth | LRN |
| 52 | POST | `/instructor/lessons/{lessonId}/quizzes` | Instr | ASM |
| 53 | PUT | `/instructor/quizzes/{quizId}` | Instr | ASM |
| 54 | DELETE | `/instructor/quizzes/{quizId}` | Instr | ASM |
| 55 | POST | `/instructor/quizzes/{quizId}/questions` | Instr | ASM |
| 56 | PUT | `/instructor/questions/{questionId}` | Instr | ASM |
| 57 | DELETE | `/instructor/questions/{questionId}` | Instr | ASM |
| 58 | GET | `/assessments/quizzes/{quizId}` | Auth | ASM |
| 59 | POST | `/assessments/quizzes/{quizId}/start` | Auth | ASM |
| 60 | POST | `/assessments/attempts/{attemptId}/submit` | Auth | ASM |
| 61 | POST | `/assessments/attempts/{attemptId}/auto-submit` | Auth | ASM |
| 62 | GET | `/instructor/assessments/attempts/{attemptId}` | Instr | ASM |
| 63 | PUT | `/instructor/assessments/answers/{answerId}/grade` | Instr | ASM |
| 64 | POST | `/assessments/lessons/{lessonId}/assignments/submit` | Learner | ASM |
| 65 | PUT | `/instructor/assignments/{submissionId}/grade` | Instr | ASM |
| 66 | GET | `/assessments/grades` | Learner | ASM |
| 67 | GET | `/instructor/programs/{programId}/grades` | Instr | ASM |
| 68 | GET | `/notifications` | Auth | NTF |
| 69 | PUT | `/notifications/{notificationId}/read` | Auth | NTF |
| 70 | PUT | `/notifications/read-all` | Auth | NTF |
| 71 | GET | `/notifications/unread-count` | Auth | NTF |
| 72 | GET | `/dashboard` | Learner | RPT |
| 73 | GET | `/instructor/reports/{programId}/analytics` | Instr | RPT |
| 74 | GET | `/admin/reports/overview` | Admin | RPT |
| 75 | GET | `/admin/reports/export` | Admin/Instr | RPT |
| 76 | GET | `/certificates/{certificateUuid}/verify` | None | RPT |
| 77 | GET | `/my-certificates` | Auth | RPT |
| 78 | GET | `/certificates/{certificateUuid}/download` | Auth | RPT |
| 79 | GET | `/admin/users` | Admin | ADM |
| 80 | POST | `/admin/users` | Admin | ADM |
| 81 | PUT | `/admin/users/{userId}` | Admin | ADM |
| 82 | POST | `/admin/users/{userId}/suspend` | Admin | ADM |
| 83 | POST | `/admin/users/{userId}/reactivate` | Admin | ADM |
| 84 | DELETE | `/admin/users/{userId}` | Admin | ADM |
| 85 | POST | `/admin/bulk/invite` | Admin | ADM |
| 86 | GET | `/admin/programs/pending` | Admin | ADM |
| 87 | POST | `/admin/programs/{programId}/approve` | Admin | ADM |
| 88 | POST | `/admin/programs/{programId}/reject` | Admin | ADM |
| 89 | GET | `/admin/audit-logs` | Admin | ADM |
| 90 | GET | `/admin/config` | Admin | ADM |
| 91 | PUT | `/admin/config/{configKey}` | Admin | ADM |
| 92 | GET | `/admin/programs` | Admin | ADM |
| 93 | GET | `/actuator/health` | None | INFRA |
| 94 | GET | `/actuator/info` | None | INFRA |

---

> **Next Document:** `06_SPRINT_ZERO_PLAN.md` — Bootstrap project initialization, tooling setup, and first sprint plan.
