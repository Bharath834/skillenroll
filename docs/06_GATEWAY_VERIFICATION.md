# 06_GATEWAY_VERIFICATION.md — SkillEnroll

> **Document Version:** 1.0
> **Author:** Senior Software Architect
> **Framework:** TrainingMug AI Development Framework
> **Status:** ✅ Approved
> **Related Docs:** [01_PROJECT_CONTEXT.md](./01_PROJECT_CONTEXT.md) · [03_ARCHITECTURE.md](./03_ARCHITECTURE.md) · [05_API_CONTRACT.md](./05_API_CONTRACT.md)

---

## Table of Contents

1. [Overview](#1-overview)
2. [Verification Results](#2-verification-results)
3. [Postman Testing Checklist](#3-postman-testing-checklist)
4. [Common Troubleshooting Guide](#4-common-troubleshooting-guide)
5. [CORS Notes](#5-cors-notes)
6. [Browser End-to-End Test](#6-browser-end-to-end-test)
7. [Reference](#7-reference)

---

## 1. Overview

The **SkillEnroll API Gateway** (`skillenroll-api-gateway`) is the edge entry point for
the SkillEnroll microservices, built with **Spring Cloud Gateway** (reactive / Netty)
on **Java 21 / Spring Boot 3.5.16 / Spring Cloud 2025.0.3**.

| Service | Port | Eureka name | Role |
|---------|------|-------------|------|
| `skillenroll-eureka-server` | 8761 | — | Service discovery registry |
| `skillenroll-backend` | 8080 | `SKILLENROLL-BACKEND` | Business APIs + **single JWT authority** |
| `skillenroll-api-gateway` | 8081 | `SKILLENROLL-API-GATEWAY` | Edge routing, CORS, observability |

### 1.1 Routes

| Route | Predicate | URI | Access |
|-------|-----------|-----|--------|
| `skillenroll-auth-route` | `Path=/api/auth/**` | `lb://skillenroll-backend` | Public (backend `permitAll`, except `/api/auth/logout` which requires a JWT) |
| `skillenroll-users-route` | `Path=/api/users/**` | `lb://skillenroll-backend` | JWT-protected (enforced by the backend) |

Downstream URIs use **service discovery** (`lb://<application-name>`) — no backend
URLs are hardcoded. The backend has no `context-path`, so paths are forwarded as-is
(no rewrite).

### 1.2 Security Model

The backend is the **single JWT authority**: its `JwtAuthenticationFilter` validates
every `Authorization: Bearer <token>` and its `SecurityConfig` keeps `/api/auth/**`
public while protecting everything else. The gateway therefore:

- does **not** validate tokens, holds **no** signing secret, and adds **no** auth filter
  (no duplicate authentication, no new JWT service);
- forwards the `Authorization` header **verbatim** (no route applies
  `AddRequestHeader`/`RemoveRequestHeader` to it);
- preserves Bearer tokens end-to-end.

---

## 2. Verification Results

All checks executed live against `http://localhost:8081` with Eureka, backend and
MySQL running.

### 2.1 Infrastructure

| # | Check | Result | Evidence |
|---|-------|--------|----------|
| 1 | Gateway starts successfully | ✅ | `Netty started on port 8081`, `Started ApiGatewayApplication in ~8-10s`, 0 errors |
| 2 | Gateway registers with Eureka | ✅ | `SKILLENROLL-API-GATEWAY` → `UP` (`registration status: 204`) |
| 3 | Backend registers with Eureka | ✅ | `SKILLENROLL-BACKEND` → `UP` |
| 4 | Routing works | ✅ | `GET /actuator/gateway/routes` returns both routes; traffic reaches the backend |

### 2.2 Authentication flow (through the gateway)

| # | Check | Result |
|---|-------|--------|
| 5 | `POST /api/auth/register` | ✅ 201 — access token + refresh token issued |
| 6 | `POST /api/auth/login` | ✅ 200 — new token pair |
| 7 | `POST /api/auth/refresh` | ✅ 200 — rotated pair (old refresh token revoked) |
| 8 | `POST /api/auth/logout` (Bearer + matching refresh token) | ✅ 200 — access token blacklisted, refresh token revoked |
| 9 | Protected endpoints: `GET /api/users/me` **without** JWT → **401**; **with** valid JWT → **200** | ✅ |
| 10 | Invalid JWT (`Bearer not.a.jwt`) | ✅ 401 — `"Invalid JWT token"` |
| 11 | Expired JWT (signature-valid, `exp` in the past) | ✅ 401 — `"JWT token has expired"` |

### 2.3 CORS

| # | Check | Result |
|---|-------|--------|
| 12 | Preflight `OPTIONS` from allowed origin (`http://localhost:5173`) | ✅ 200 — single header set (`Access-Control-Allow-Origin`, `-Methods`, `-Headers`, `-Credentials: true`, `-Max-Age: 3600`) |
| 13 | Actual request with `Origin` header | ✅ 401 from backend + **exactly one** `Access-Control-Allow-Origin` + one `Access-Control-Allow-Credentials` |
| 14 | Preflight from disallowed origin (`http://evil.com`) | ✅ 403, no `Access-Control-Allow-Origin` |
| 15 | Non-browser requests (no `Origin`) | ✅ unaffected (Postman/curl behave as before) |

### 2.4 Browser end-to-end (real Chrome)

| # | Step | Result |
|---|------|--------|
| 16 | Register → Login → `GET /users/me` with JWT → Refresh → `GET /users/me` with refreshed JWT → Logout → `GET /users/me` after logout | ✅ **7/7 PASS** (201, 200, 200, 200, 200, 200, 401), **zero console/CORS errors** |

---

## 3. Postman Testing Checklist

**Setup:** Collection `SkillEnroll Gateway`, base URL `http://localhost:8081`.
Variables: `token`, `refreshToken`. Send `Content-Type: application/json` on all
requests. (The real response field is `token`, not `accessToken`.)

### Phase A — Infrastructure (no auth)

- [ ] `GET /actuator/health` → `{"status":"UP"}`
- [ ] `GET /actuator/gateway/routes` → JSON array with `skillenroll-auth-route`,
      `skillenroll-users-route`
- [ ] Browser: `http://localhost:8761` → both `SKILLENROLL-API-GATEWAY` and
      `SKILLENROLL-BACKEND` show `UP`

### Phase B — Auth flow (public)

- [ ] **Register** `POST /api/auth/register` — body
      `{"firstName":"John","lastName":"Doe","email":"john@example.com","phoneNumber":"+15550123","password":"SecurePass123!"}`
      → **201**. Save `data.token` → `{{token}}`, `data.refreshToken` → `{{refreshToken}}`
- [ ] **Duplicate register** (same email) → **409** `EMAIL_ALREADY_EXISTS`
- [ ] **Login** `POST /api/auth/login` —
      `{"email":"john@example.com","password":"SecurePass123!"}` → **200**; update variables
- [ ] **Login wrong password** → **401** `"Invalid email or password"`
- [ ] **Refresh** `POST /api/auth/refresh` — `{"refreshToken":"{{refreshToken}}"}` → **200**; update variables
- [ ] **Refresh with a revoked token** (re-use the pre-refresh one) → **401**

### Phase C — Protected endpoint matrix (`GET /api/users/me`)

- [ ] **Without** Authorization header → **401**
- [ ] **With valid JWT** — `Authorization: Bearer {{token}}` → **200** + profile JSON
- [ ] **With invalid JWT** — `Authorization: Bearer not.a.jwt` → **401** `"Invalid JWT token"`
- [ ] **With expired JWT** — decode a real token (jwt.io), set `exp` to a past
      timestamp, re-sign → **401** `"JWT token has expired"`

### Phase D — Logout (authenticated)

- [ ] **Logout** `POST /api/auth/logout` — header `Authorization: Bearer {{token}}`,
      body `{"refreshToken":"{{refreshToken}}"}` → **200**
- [ ] **Reuse access token after logout** — `GET /api/users/me` with the logged-out
      token → **401** (blacklisted)

### Phase E — Routing guard

- [ ] `GET /api/unknown` (no matching route) → **404** from the gateway

---

## 4. Common Troubleshooting Guide

| Symptom | Likely cause | Fix |
|---------|--------------|-----|
| `Connection refused` on 8081 | Gateway not started | `cd skillenroll-api-gateway && mvn spring-boot:run`; confirm `Netty started on port 8081` |
| Gateway starts but not on Eureka dashboard | Eureka down or wrong `defaultZone` | Start Eureka first; check `eureka.client.service-url.defaultZone` (default `http://localhost:8761/eureka/`) |
| `503 Service Unavailable` from gateway | `lb://skillenroll-backend` has no registered instance | Backend not running/unregistered — confirm `SKILLENROLL-BACKEND` shows `UP` on port 8761 |
| Whitelabel 404 on `/api/auth/**` | Route missing or path typo | `GET /actuator/gateway/routes`; confirm `Path=/api/auth/**` predicate |
| 401 on login/register | Wrong `JWT_SECRET` mismatch or stale token | Backend validates with its own secret; the gateway only forwards — clear Postman's cached `{{token}}` |
| 401 `"JWT token has expired"` | Token past `exp` (24 h default) | Call `/api/auth/refresh` with a valid refresh token |
| `BUILD FAILURE: ...jar is being used by another process` (Windows) | Running gateway locks `target/*.jar` | Stop the gateway first (Ctrl+C or `taskkill //F //PID <pid>`), then rebuild |
| `/actuator/gateway` returns 404 | Boot 3.4+ endpoint-access model resolves the deprecated `@RestControllerEndpoint` to NONE | Keep `management.endpoint.gateway.access: unrestricted` in `application.yml` |
| Requests reach the backend but everything returns 401 | Token not forwarded (a filter strips it) | Verify routes have `"filters":[]` via `/actuator/gateway/routes`; never add `RemoveRequestHeader=Authorization` |
| Browser blocked by CORS ("has been blocked by CORS policy") | Origin not in the allow list, or duplicate `Access-Control-Allow-Credentials` headers | Add the origin to `CORS_ALLOWED_ORIGINS`; do **not** remove the `RemoveRequestHeader=Origin` default filter (see §5.2) |
| Client IP looks like the gateway's | No forwarded headers | `spring.cloud.gateway.forwarded.enabled: true` (already set); backend must trust forwarded headers only behind the gateway |
| `CORS_ALLOWED_ORIGINS` set but origins rejected | Spaces after commas break the list binding | Set comma-separated values with **no spaces** |

---

## 5. CORS Notes

### 5.1 Configuration (`skillenroll-api-gateway/src/main/resources/application.yml`)

```yaml
spring:
  cloud:
    gateway:
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: ${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}
            allowedMethods: [GET, POST, PUT, DELETE, OPTIONS]
            allowedHeaders: '*'
            allowCredentials: true
            maxAge: 3600   # seconds - cache preflight responses for 1 hour
      default-filters:
        - RemoveRequestHeader=Origin
```

- **Allowed origins** mirror the backend's `CorsConfig` (the React SPA on Vite
  `:5173` or `:3000`). Override in production via `CORS_ALLOWED_ORIGINS`
  (comma-separated, no spaces, exact origins — `allowCredentials: true` forbids `*`).
- **`allowedHeaders: '*'`** with credentials is valid in modern browsers (the header
  is echoed as the requested headers); the `Authorization` header is therefore
  allowed on preflight.

### 5.2 Why `RemoveRequestHeader=Origin` (single CORS authority)

The backend's MVC `CorsConfig` also fires on proxied requests. If it did, responses
would carry **duplicate** `Access-Control-Allow-Origin` and
`Access-Control-Allow-Credentials` headers, and browsers reject the credentials
header when it combines to `"true, true"`.

A naive fix with `RemoveResponseHeader=Access-Control-*` default filters does **not**
work: Spring Cloud Gateway 4.x applies CORS via `RoutePredicateHandlerMapping`
**before** the route filter chain runs, so removing the headers also strips the
gateway's own set.

Stripping `Origin` before forwarding keeps the gateway as the **only** CORS
authority: preflight is answered by the gateway, the backend adds no CORS headers,
and every response carries exactly one header set.

### 5.3 Design trade-off (documented decision)

`Origin` is stripped on **all** routes (it is a `default-filters` entry). This is
safe today because the backend uses `Origin` for nothing else (CSRF is disabled, no
OAuth2). If a future backend feature needs `Origin` (OAuth2 redirects, anti-CSRF
origin checks) or a future route must forward it, this default filter must be
revisited/overridden for that route. The rationale is recorded in the YAML comment.

---

## 6. Browser End-to-End Test

A dedicated, re-runnable browser test lives at
`tmp/gateway-browser-e2e/index.html`. It runs the full flow from the SPA origin
(`http://localhost:5173`) against the gateway with cross-origin preflights.

```bash
# 1. Ensure services are up: Eureka (:8761), backend (:8080), gateway (:8081)
cd tmp/gateway-browser-e2e && python -m http.server 5173
# 2. Open http://localhost:5173/ in Chrome — 7 steps render PASS/FAIL with HTTP statuses
# 3. Ctrl+C to stop the server
```

Expected: 7 PASS lines — Register **201**, Login **200**, `GET /users/me` **200**,
Refresh **200**, `GET /users/me` (refreshed token) **200**, Logout **200**,
`GET /users/me` after logout **401** — and zero console/CORS errors.
Each run creates one test user in the local MySQL dev DB (unique
`gw-browser-<timestamp>@example.com` email).

---

## 7. Reference

**Files:**
- `skillenroll-api-gateway/pom.xml` — Boot 3.5.16 / Java 21 / Spring Cloud 2025.0.3;
  only gateway, eureka-client, actuator, validation (no security, no JWT)
- `skillenroll-api-gateway/src/main/resources/application.yml` — port 8081, Eureka
  registration, routes, forwarded headers, CORS, logging
- `skillenroll-api-gateway/src/main/java/com/skillenroll/apigateway/ApiGatewayApplication.java`
- `tmp/gateway-browser-e2e/index.html` — browser E2E test page

**Key environment variables:**

| Variable | Default | Purpose |
|----------|---------|---------|
| `SERVER_PORT` | `8081` | Gateway port |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | Eureka registry URL |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Cross-origin allow list |
| `GATEWAY_ROUTE_LOG_LEVEL` | `debug` | Route-matching log verbosity (`info` in production) |

---

> **Next:** `07_*` — Frontend (React SPA) integration, when available.
