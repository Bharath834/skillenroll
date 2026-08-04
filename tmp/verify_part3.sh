#!/bin/bash
set -u
BASE=http://localhost:8080
EMAIL="part3$(date +%s)@gmail.com"
PHONE="8222333444"

echo "== 1. Register $EMAIL =="
REG=$(curl -s -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" \
  -d "{\"firstName\":\"Bharath\",\"lastName\":\"Kumar\",\"email\":\"$EMAIL\",\"phoneNumber\":\"$PHONE\",\"password\":\"Passw0rd!\"}")
echo "$REG" | head -c 120; echo
TOKEN_A=$(echo "$REG" | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
RT_A=$(echo "$REG" | python -c "import sys,json;print(json.load(sys.stdin)['data']['refreshToken'])")

echo "== 2. /me with A (expect 200) =="
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/api/users/me" -H "Authorization: Bearer $TOKEN_A"

echo "== 3. Refresh RT_A -> B (expect 200) =="
RF=$(curl -s -X POST "$BASE/api/auth/refresh" -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$RT_A\"}")
TOKEN_B=$(echo "$RF" | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
RT_B=$(echo "$RF" | python -c "import sys,json;print(json.load(sys.stdin)['data']['refreshToken'])")
echo "rotated OK: tokenB=${TOKEN_B:0:15}... rtB=${RT_B:0:8}..."

echo "== 4. Logout with RT_B + Bearer B (expect 200) =="
curl -s -w " [HTTP %{http_code}]\n" -X POST "$BASE/api/auth/logout" \
  -H "Authorization: Bearer $TOKEN_B" -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$RT_B\"}"

echo "== 5. /me with B after logout (expect 401 blacklisted) =="
curl -s -w " [HTTP %{http_code}]\n" "$BASE/api/users/me" -H "Authorization: Bearer $TOKEN_B"

echo "== 6. /me with A (pre-logout, not blacklisted -> expect 200) =="
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/api/users/me" -H "Authorization: Bearer $TOKEN_A"

echo "== 7. Refresh with revoked RT_B after logout (expect 409 reuse) =="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/auth/refresh" \
  -H "Content-Type: application/json" -d "{\"refreshToken\":\"$RT_B\"}"

echo "== 8. Fresh login -> C (new token works, expect 200 on /me) =="
LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"Passw0rd!\"}")
TOKEN_C=$(echo "$LOGIN" | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/api/users/me" -H "Authorization: Bearer $TOKEN_C"

echo "== 9. Database check =="
mysql -uroot -proot skillenroll -e "SELECT COUNT(*) AS blacklisted FROM blacklisted_tokens; SELECT token, revoked, expires_at FROM refresh_tokens ORDER BY id DESC LIMIT 4; SELECT token_hash, expires_at FROM blacklisted_tokens ORDER BY id DESC LIMIT 2;" 2>/dev/null || echo "(mysql CLI not available - will check via app behavior)"

echo "== 10. Eureka =="
curl -s http://localhost:8761/eureka/apps/SKILLENROLL-BACKEND | grep -o '<status>UP</status>' | head -1
