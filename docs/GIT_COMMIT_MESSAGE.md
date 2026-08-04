# Suggested Git Commit — Day 3 Complete

```text
feat(auth): complete Day 3 - JWT enrichment, refresh tokens, logout blacklist, Swagger

JWT access tokens now embed userId/firstName/lastName/role claims (subject stays
the email) and GET /api/users/me resolves the caller from the SecurityContext.

Auth flows:
- POST /api/auth/refresh rotates DB-backed opaque refresh tokens (7-day expiry)
  with reuse detection that revokes all of a user's sessions on token theft
- POST /api/auth/logout (authenticated) revokes the refresh token and blacklists
  the access JWT (SHA-256 hash) so JwtAuthenticationFilter rejects it with 401
- Login/register now return an access token + refresh token pair

Security & docs:
- Admin-only user management (POST/PUT/DELETE /api/users now @PreAuthorize ADMIN)
- springdoc-openapi 2.9.0: /swagger-ui/index.html and /v3/api-docs public, all
  other APIs require a valid JWT (bearer security scheme wired for Swagger UI)

Quality:
- New SecurityUtils shared principal/token accessors (removed duplicated logic)
- 46 unit + MockMvc integration tests (H2): register, login, refresh, logout,
  protected API, role authorization; builds access tokens without redundant
  user re-fetch
- Removed dead code (unused EnrollmentRepository lookup, redundant timestamp set)
- Postman collection (14 requests) and MySQL verification queries updated
```

## Files touched (summary)

- `backend/src/main/java/com/skillenroll/**` - JwtService, CustomUserDetails,
  SecurityUtils, JwtAuthenticationFilter, AuthServiceImpl, UserController,
  SecurityConfig, OpenApiConfig, RefreshToken/BlacklistedToken entities +
  repositories + services, DTOs, exceptions, GlobalExceptionHandler
- `backend/src/test/**` - AuthFlowMockMvcTest (+ unit tests for JWT, auth,
  refresh tokens, blacklist, filter)
- `backend/pom.xml` - springdoc-openapi 2.9.0, H2 (test)
- `backend/docs/**` - Postman collection, SQL verification queries, checklists
