# SkillEnroll — Day 2 Authentication: Manual Testing Checklist

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
