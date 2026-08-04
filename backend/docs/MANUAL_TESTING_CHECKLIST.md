# SkillEnroll — Day 2 Authentication: Manual Testing Checklist

Backend: Spring Boot 3.5 / Java 21 / MySQL / JWT / Eureka
Base URL: `http://localhost:8080` — Auth endpoints under `/api/auth`

---

# Day 3 Part 3 — JWT Blacklist on Logout: Manual Testing Checklist

> Every step below was executed and passed during the Day 3 Part 3 verification
> run (fresh `mvn -DskipTests package`, restarted jar, Eureka + MySQL running).
> Re-run to confirm after any further change.

## 1. Pre-flight
- [ ] MySQL 8 service is RUNNING on `3306`; Eureka RUNNING on `8761`
- [ ] Build: `mvn -DskipTests package`; Start: `java -jar target/skillenroll-backend-0.0.1-SNAPSHOT.jar`
- [ ] Logs show `Started SkillEnrollApplication`; `blacklisted_tokens` table is auto-created

## 2. Blacklist on logout
- [ ] Login → capture `token` + `refreshToken`
- [ ] `GET /api/users/me` with that token → **200** (works before logout)
- [ ] `POST /api/auth/logout` (Bearer access token + body refreshToken) → **200**
- [ ] Same access token used again on `GET /api/users/me` → **401** `JWT token has been revoked`

## 3. Old vs new tokens
- [ ] Refresh token → new access token; the pre-logout access token stays **401** (blacklisted)
- [ ] New access token works → **200**
- [ ] Expired token (e.g. restart with `JWT_EXPIRATION_MS=5000`) → **401** `JWT token has expired`

## 4. Database
- [ ] `SHOW TABLES;` → includes `blacklisted_tokens`, `refresh_tokens`
- [ ] `SELECT * FROM blacklisted_tokens;` → rows with 64-char `token_hash`, `expires_at`, `created_at`
- [ ] `SELECT token, revoked, expires_at FROM refresh_tokens;` → logged-out token has `revoked = 1`

## 5. Regression
- [ ] Register → **201**; Login → **200**; Refresh → **200**; `/me` → **200** with new token
- [ ] Eureka shows `SKILLENROLL-BACKEND` **UP**

## 6. Unit tests
- [ ] `mvn test` → all green (34 tests incl. `JwtAuthenticationFilterTest`, `BlacklistedTokenServiceImplTest`)

---

# Day 3 Part 2 — Refresh Tokens + Logout: Manual Testing Checklist

> Every step below was executed and passed during the Day 3 Part 2 verification
> run (fresh `mvn -DskipTests package`, restarted jar, Eureka + MySQL running).
> Re-run to confirm after any further change.

## 1. Pre-flight
- [ ] MySQL 8 service is RUNNING on `3306`
- [ ] Eureka server is RUNNING on `8761`
- [ ] Build: `mvn -DskipTests package`; Start: `java -jar target/skillenroll-backend-0.0.1-SNAPSHOT.jar`
- [ ] Logs show `Started SkillEnrollApplication`; `refresh_tokens` table is auto-created

## 2. Login / Register now return a refresh token
- [ ] `POST /api/auth/login` → **200**, `data.token` (JWT) **and** `data.refreshToken` (UUID string)
- [ ] `POST /api/auth/register` → **201**, same pair returned

## 3. Refresh — POST /api/auth/refresh (rotation)
- [ ] Body `{ "refreshToken": "<rt1>" }` → **200**, new `token` + new `refreshToken` returned
- [ ] Reusing `<rt1>` afterwards → **409** `Refresh token reuse detected. All active sessions for this user have been revoked.`
- [ ] Unknown token → **401** `Invalid refresh token`

## 4. Logout — POST /api/auth/logout (authenticated)
- [ ] With `Authorization: Bearer <access>` + body `{ "refreshToken": "<rt2>" }` → **200** `Logged out successfully`
- [ ] Without access token → **401** (logout is protected by SecurityConfig)
- [ ] Refresh token belonging to another user → **400** `Refresh token does not belong to the authenticated user`
- [ ] After logout, refreshing with the logged-out token → **409** reuse detection

## 5. Regression
- [ ] `GET /api/users/me` with a freshly rotated access token → **200**
- [ ] Wrong password login → **401**
- [ ] Eureka dashboard shows `SKILLENROLL-BACKEND` **UP** on `8080`

## 6. Unit tests
- [ ] `mvn test` → all green (`AuthServiceImplTest`, `RefreshTokenServiceImplTest`, `JwtServiceTest`, `UserServiceImplTest`)

---

# Day 3 Part 1 — JWT Enrichment + GET /api/users/me: Manual Testing Checklist

> Every step below was executed and passed during the Day 3 Part 1 verification
> run (fresh `mvn -DskipTests package`, restarted jar, Eureka + MySQL running).
> Re-run to confirm after any further change.

## 1. Pre-flight
- [ ] MySQL 8 service is RUNNING on `3306`
- [ ] Eureka server is RUNNING on `8761`
- [ ] Build the backend: `mvn -DskipTests package`
- [ ] Start the backend: `java -jar target/skillenroll-backend-0.0.1-SNAPSHOT.jar`
- [ ] Backend logs show `Started SkillEnrollApplication` (no `ERROR` lines)

## 2. Login still works (regression)
- [ ] `POST /api/auth/login` with correct credentials → **200 OK**, `success: true`, JWT + user profile returned
- [ ] Wrong password → **401** `Invalid email or password`

## 3. JWT carries the Day 3 claims
- [ ] Decode the token at jwt.io → header `alg: HS384`, payload has:
  - `sub` = user email (subject unchanged)
  - `userId` = numeric id
  - `firstName`
  - `lastName`
  - `role` = plain role name e.g. `"STUDENT"`  *(changed from the old `ROLE_STUDENT` authority value)*
  - `iat`, `exp`
- [ ] `expiresIn` in the login/register response still equals `86400` (24 h)

## 4. GET /api/users/me
- [ ] With `Authorization: Bearer <token>` → **200 OK**, `success: true`, `data` contains `id`, `firstName`, `lastName`, `email`, `phoneNumber`, `role` (no `password`)
- [ ] Without token → **401** `Unauthorized: authentication is required to access this resource`
- [ ] With garbage token → **401** `Invalid JWT token`
- [ ] Expired token → **401** `JWT token has expired`
- [ ] The endpoint accepts **no** path/query parameter — the caller is resolved from the `SecurityContext` only

## 5. Eureka registration
- [ ] Open `http://localhost:8761/` → `SKILLENROLL-BACKEND` shows **UP** on port `8080`

## 6. Unit tests
- [ ] `mvn test` → all green (`JwtServiceTest`, `AuthServiceImplTest`, `UserServiceImplTest`)

## 7. DB spot check after the run
- [ ] No `ERROR`/`Exception` stack traces in the backend log

---

# (Historical) Day 2 verification record

Backend: Spring Boot 3.5 / Java 21 / MySQL / JWT / Eureka
Base URL: `http://localhost:8080` — Auth endpoints under `/api/auth`

> Every step below was executed and passed during the Day 2 verification run
> (fresh `mvn package` build, restarted jar). Re-run to confirm.

## 1. Pre-flight
- [ ] MySQL 8 service is RUNNING on `3306`
- [ ] Eureka server is RUNNING on `8761`
- [ ] Build the backend: `mvn -DskipTests package`
- [ ] Start the backend: `java -jar target/skillenroll-backend-0.0.1-SNAPSHOT.jar`
- [ ] Backend logs show `Started SkillEnrollApplication` (no `ERROR` lines)

## 2. Eureka registration
- [ ] Open `http://localhost:8761/` → `SKILLENROLL-BACKEND` shows **UP**
- [ ] Backend log contains `Registering application SKILLENROLL-BACKEND with eureka with status UP`

## 3. Database connected
- [ ] `mysql -uroot -proot skillenroll -e "SHOW TABLES;"` → `users`, `courses`, `enrollments`
- [ ] `SHOW CREATE TABLE users` shows unique keys `uk_users_email`, `uk_users_phone_number`

## 4. Registration — POST /api/auth/register
- [ ] Valid payload → **201 Created**, body `success: true` with `token`, `user.role: "STUDENT"`
- [ ] Logs: `Registration started for email: ...` → `User saved with id ...` → `JWT generated ...` → `Registration successful`
- [ ] Duplicate email → **409** `User with email '...' already exists`
- [ ] Duplicate phone → **409** `User with phone number '...' already exists`
- [ ] Empty/invalid fields → **400** with per-field `data` errors
- [ ] Malformed JSON → **400**

## 5. Login — POST /api/auth/login
- [ ] Correct credentials → **200 OK**, `success: true`, JWT + user profile returned
- [ ] Logs: `Login successful for email: ...`
- [ ] Wrong password → **401** `Invalid email or password`
- [ ] Logs: WARN `Authentication failed for email: ... (BadCredentialsException)`

## 6. JWT generated
- [ ] Decode the token at jwt.io → header `alg: HS384`, payload has `sub` (email), `role: "ROLE_STUDENT"`, `iat`, `exp`
- [ ] `expiresIn` in response equals `86400` (24 h)

## 7. Protected APIs
- [ ] `GET /api/users` without token → **401** `Unauthorized: authentication is required...`
- [ ] `GET /api/users` with garbage token → **401** `Invalid JWT token`
- [ ] `GET /api/users` with `Bearer <valid token>` → **200 OK**
- [ ] Expired token → **401** `JWT token has expired` (wait for exp, or temporarily lower `expiration-ms`)

## 8. DB spot check after the run
- [ ] `SELECT email, role, LEFT(password,20), created_at FROM users;` → BCrypt `$2a$...`, role `STUDENT`, timestamps set
- [ ] No `ERROR`/`Exception` stack traces in the backend log
