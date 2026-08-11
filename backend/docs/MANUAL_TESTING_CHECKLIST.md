# SkillEnroll — Day 8 / T07: Razorpay Payment Flow — Manual Testing Checklist

Backend: Spring Boot 3.5 / Java 21 / MySQL / JWT / Eureka
Base URL: `http://localhost:8080` — Payment endpoints under `/api/payment`
Frontend: Vite dev server on `http://localhost:5173`

> **Two verification modes**
> 1. **Wiring-only (no real keys):** set `RAZORPAY_KEY_ID`/`RAZORPAY_KEY_SECRET` to
>    *any* non-empty placeholder (e.g. `rzp_test_xxxxxxxx`) so the app boots. Every
>    step except the real card payment can be verified; `create-order` will return
>    **502** because Razorpay rejects fake credentials — that 502 itself proves the
>    client → service → Razorpay wiring.
> 2. **Full card payment:** set real TEST keys (`rzp_test_...`) from the Razorpay
>    dashboard and use the Razorpay test card `4111 1111 1111 1111` (expiry any
>    future date, CVV any 3 digits, OTP `1234`) to complete the checkout.

## 0. Pre-flight
- [ ] MySQL 8 service RUNNING on `3306`
- [ ] Build: `mvn -DskipTests package` (backend) and `npm install` (frontend)
- [ ] Start backend with Razorpay env vars:
      `RAZORPAY_KEY_ID=rzp_test_xxx RAZORPAY_KEY_SECRET=xxx java -jar target/skillenroll-backend-0.0.1-SNAPSHOT.jar`
- [ ] Start frontend: `npm run dev` (Vite on `5173`)
- [ ] Backend logs show `Started SkillEnrollApplication` (no `ERROR` lines)
- [ ] `payment_orders` table is auto-created (Hibernate `ddl-auto: update`)

## 1. Startup fail-fast (no keys → app refuses to start)
- [ ] Start WITHOUT `RAZORPAY_KEY_ID`/`RAZORPAY_KEY_SECRET` → application aborts with:
      `Razorpay TEST credentials are not configured. Set the RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET environment variables...`
- [ ] Start WITH placeholders → boots normally, `RazorpayClient` bean created (no network call at startup)

## 2. Endpoint registration & security
- [ ] `GET /v3/api-docs` → 200; `paths` include `/api/payment/create-order`, `/api/payment/verify`, `/api/payment/re-verify`
- [ ] `POST /api/payment/create-order` WITHOUT token → **401** (JWT-protected by existing SecurityConfig)
- [ ] `POST /api/payment/verify` WITHOUT token → **401**
- [ ] `POST /api/payment/re-verify` WITHOUT token → **401**

## 3. Happy path — create order (real TEST keys required)
- [ ] Login → capture `token` (Bearer)
- [ ] `POST /api/payment/create-order` `{"courseId":<paidCourseId>}` with Bearer → **201**
- [ ] Response `data` contains: `orderId` (`order_...`), `amount` (paise, server-derived), `currency="INR"`, `receipt="course-<id>-<hex>"`, `status="created"`, `keyId` (public), `createdAt`
- [ ] Response does **NOT** contain the key secret
- [ ] Body has no `amount` field — verify the amount in `data.amount` equals `course.price × 100` (client cannot tamper)

### 3a. Wiring-only substitute (placeholder keys)
- [ ] Same request with placeholder keys → **502** `Payment gateway error...` (Razorpay rejects fake creds). This is EXPECTED and proves the full chain to Razorpay.
- [ ] Backend log contains the Razorpay error with NO secret printed

## 4. Error mapping — create-order
- [ ] `{"courseId":9999}` (missing course) → **404** `Course not found with id: 9999`
- [ ] `{"courseId":<freeCourseId>}` → **400** `This course is free and does not require payment`
- [ ] `{}` (missing courseId) → **400** validation error
- [ ] `{"courseId":-1}` → **400** validation error
- [ ] Same user+course called twice quickly → **second call reuses** the existing CREATED order (`data.orderId` identical, no new Razorpay call — check log `Reusing existing Razorpay order`)

## 5. Payment in the browser (real TEST keys) — Course Details page
- [ ] Open `http://localhost:5173/courses/<paidCourseId>` while logged in → sidebar shows price + **Enroll now**
- [ ] Click **Enroll now** → button becomes *Enrolling…* → *Payment…* → Razorpay checkout modal opens (brand color `#4f46e5`, order id + amount prefilled, learner name/email prefilled)
- [ ] Pay with test card `4111 1111 1111 1111` → modal closes → button shows *Confirming…* → success Alert `Payment successful — you are enrolled...`
- [ ] Button becomes disabled **Enrolled**; enrollment status is ACTIVE (verify in DB: `SELECT status FROM enrollments`)
- [ ] `payment_orders` row updated: `status='PAID'`, `payment_id`, `signature`, `paid_at` set
- [ ] **My Enrollments** page shows the course as active

### 5a. Free course (no payment)
- [ ] Free course → **Enroll now** enrolls directly (no Razorpay modal), success Alert, button **Enrolled**

> **Note:** Free courses cannot be created via the API (`CourseRequest` requires `price > 0`). If none
> exists, seed one via SQL before testing:
> `INSERT INTO courses (title, description, category, price, duration, instructor_name) VALUES ('Free Sample', 'A free course', 'Sample', 0, 1, 'SkillEnroll');`

## 6. Failed / cancelled payment
- [ ] Open checkout then close the modal (X / dismiss) → error/neutral state, button returns to **Enroll now / Complete payment**, NO enrollment activation
- [ ] Use a failing test card (e.g. `4000 0000 0000 0002`) → `payment.failed` → error Alert `Payment failed...`, order stays CREATED in `payment_orders`, no activation
- [ ] Retry **Enroll now / Complete payment** → reuse path: same order id reused (no orphaned orders)

## 7. Verification endpoint — POST /api/payment/verify
- [ ] Valid signature payload (orderId/paymentId/signature/courseId from a real payment) → **200**, `verified: true`, `enrollmentStatus: "ACTIVE"`
- [ ] Tampered signature → **400** `Payment signature verification failed`; order stays CREATED
- [ ] Wrong user's order id → **400** `Payment order does not belong to the authenticated user`
      (register a second account and verify with ITS order id to exercise this owner check)
- [ ] `courseId` not matching the order's course → **400** `Payment order does not match the course`
- [ ] Unknown order id → **400** `Payment order not found`
- [ ] Missing/blank orderId/paymentId/signature → **400** validation error

### 7a. Wiring-only checks that always work (no real payment needed)
- [ ] Tampered-signature check, wrong-user, course-mismatch, unknown-order, validation errors (all 400) can be exercised with any authenticated session + a ledger order id from step 4

## 8. Re-verify endpoint — POST /api/payment/re-verify (double-charge guard)
- [ ] `{"orderId":<PAID order>,"courseId":...}` → **200**, `verified: true`, `enrollmentStatus: "ACTIVE"` (payment id + signature read from ledger — client sends only the order id)
- [ ] `{"orderId":<CREATED order>,"courseId":...}` → **409** `No verified payment exists for this order. Please complete a new payment.`
- [ ] Unknown order → **400** `Payment order not found`
- [ ] Wrong user → **400** `Payment order does not belong to the authenticated user` (needs a second account)
- [ ] **Double-charge guard (browser):** complete a payment, then simulate a lost confirmation (refresh the page before it confirms) → button shows **Confirm payment** → click it → enrollment activates WITHOUT opening a new Razorpay modal (check log: no `Razorpay order ... created`, only re-verify)

## 9. Frontend sessionStorage recovery (reload-safe)
- [ ] Start a payment, then in the verification failure path refresh the page → Course Details restores the pending payment from `sessionStorage` → button reads **Confirm payment** → clicking confirms the SAME payment without a new order
- [ ] After success, `sessionStorage` key `skillenroll:pending-payment:<courseId>` is removed

## 10. Database spot-check
- [ ] `SELECT order_id, user_id, course_id, amount_paise, currency, receipt, status, payment_id, created_at, paid_at FROM payment_orders;` → one CREATED→PAID row per successful payment
- [ ] No duplicate `order_id` rows (unique constraint `uk_payment_orders_order_id`)
- [ ] No `ERROR`/`Exception` stack traces in the backend log; no Razorpay secret appears in any log line

## 11. Regression
- [ ] Register → 201; Login → 200; `/api/users/me` → 200
- [ ] Courses list + Course Details load; free-course enroll works; My Enrollments + Progress unaffected
- [ ] Logout → 200 and access token revoked (401 on reuse)
- [ ] `mvn test` → all green (136 tests incl. `PaymentServiceImplTest` with 22 payment tests)

---

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
