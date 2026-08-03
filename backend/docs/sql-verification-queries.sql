-- ============================================================================
-- SkillEnroll - Day 2 Authentication Verification Queries (MySQL 8)
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
