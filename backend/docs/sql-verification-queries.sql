-- ============================================================================
-- SkillEnroll - Day 3 Complete Authentication Verification Queries (MySQL 8)
-- Run against the 'skillenroll' database. Adjust credentials if needed:
--   mysql -uroot -proot skillenroll < docs/sql-verification-queries.sql
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. All registered users
-- ----------------------------------------------------------------------------
SELECT id, first_name, last_name, email, phone_number, role,
       LEFT(password, 20) AS password_hash_prefix,   -- NEVER select full hash in prod logs
       created_at, updated_at
FROM users
ORDER BY id;

-- ----------------------------------------------------------------------------
-- 2. Verify BCrypt password storage
--    Every password must start with the BCrypt marker $2a$ / $2b$ / $2y$
--    and be exactly 60 characters long. A raw/plain-text value = BUG.
-- ----------------------------------------------------------------------------
SELECT email,
       LENGTH(password)                                AS hash_length,      -- must be 60
       password NOT REGEXP '^\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}$' AS invalid_bcrypt -- must be 0
FROM users;

-- ----------------------------------------------------------------------------
-- 3. Verify role storage (enum values)
-- ----------------------------------------------------------------------------
SELECT role, COUNT(*) AS total
FROM users
GROUP BY role;

-- ----------------------------------------------------------------------------
-- 4. Verify unique constraints exist (email + phone_number)
-- ----------------------------------------------------------------------------
SHOW INDEX FROM users;

-- ----------------------------------------------------------------------------
-- 5. Verify created_at / updated_at are populated on every row
-- ----------------------------------------------------------------------------
SELECT COUNT(*) AS rows_missing_timestamps
FROM users
WHERE created_at IS NULL OR updated_at IS NULL;

-- ----------------------------------------------------------------------------
-- 6. Verify no duplicate emails or phone numbers slipped through
-- ----------------------------------------------------------------------------
SELECT email, COUNT(*) AS c FROM users GROUP BY email HAVING c > 1;
SELECT phone_number, COUNT(*) AS c FROM users GROUP BY phone_number HAVING c > 1;

-- ----------------------------------------------------------------------------
-- 7. Verify the full schema created by Hibernate (ddl-auto: update)
-- ----------------------------------------------------------------------------
SHOW CREATE TABLE users\G

-- ----------------------------------------------------------------------------
-- 8. Verify login flow end-to-end (BCrypt match check for a given user)
--    Replace the <email> / <plaintext> placeholders.
--    '1' means the supplied password matches the stored hash.
-- ----------------------------------------------------------------------------
-- SELECT password = ... -- NOT POSSIBLE on hash; use the app, or:
-- SELECT COUNT(*) AS match_count
-- FROM users
-- WHERE email = '<email>';

-- ----------------------------------------------------------------------------
-- 9. Cleanup test users (optional - run only on scratch data)
-- ----------------------------------------------------------------------------
-- DELETE FROM users WHERE email LIKE 'verify%@example.com';

-- ============================================================================
-- DAY 3 - REFRESH TOKENS (POST /api/auth/refresh, rotation + reuse detection)
-- ============================================================================

-- 10. All refresh tokens with ownership + lifecycle state
--     revoked=1  -> rotated (refresh) or logged out
--     revoked=0  -> currently active (usable for a refresh)
-- ----------------------------------------------------------------------------
SELECT rt.id, u.email, rt.token, rt.expires_at, rt.revoked, rt.created_at
FROM refresh_tokens rt
JOIN users u ON u.id = rt.user_id
ORDER BY rt.id DESC
LIMIT 20;

-- 11. Expired-but-unrevoked refresh tokens (they will be rejected by the
--     service even though revoked=0) - candidates for a cleanup job.
-- ----------------------------------------------------------------------------
SELECT id, user_id, expires_at, revoked
FROM refresh_tokens
WHERE revoked = 0 AND expires_at < NOW();

-- 12. Rotation check: a used refresh token must have revoked=1. The number of
--     active (revoked=0) tokens per user shows how many concurrent sessions
--     exist (login issues one token per session).
-- ----------------------------------------------------------------------------
SELECT u.email, COUNT(*) AS active_tokens
FROM refresh_tokens rt
JOIN users u ON u.id = rt.user_id
WHERE rt.revoked = 0
GROUP BY u.email;

-- ============================================================================
-- DAY 3 - JWT BLACKLIST (logout) - SHA-256 hashes of logged-out access tokens
-- ============================================================================

-- 13. Blacklisted (logged-out) access tokens. token_hash is the SHA-256 hex of
--     the raw JWT (raw token is never stored - it embeds profile claims).
--     expires_at mirrors the JWT 'exp' claim.
-- ----------------------------------------------------------------------------
SELECT id, token_hash, expires_at, created_at
FROM blacklisted_tokens
ORDER BY id DESC
LIMIT 20;

-- 14. Count of blacklisted tokens still within their validity window (i.e.
--     rows that are actually doing work - a rejected request).
-- ----------------------------------------------------------------------------
SELECT COUNT(*) AS active_blacklist_entries
FROM blacklisted_tokens
WHERE expires_at > NOW();

-- 15. Sanity: token_hash must be exactly 64 hex characters.
-- ----------------------------------------------------------------------------
SELECT COUNT(*) AS invalid_hash_lengths
FROM blacklisted_tokens
WHERE LENGTH(token_hash) <> 64;
