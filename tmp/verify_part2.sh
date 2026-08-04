#!/bin/bash
set -u
BASE=http://localhost:8080
EMAIL="part2d$(date +%s)@gmail.com"
PHONE="8111222333"

echo "== Register $EMAIL =="
REG=$(curl -s -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" \
  -d "{\"firstName\":\"Bharath\",\"lastName\":\"Kumar\",\"email\":\"$EMAIL\",\"phoneNumber\":\"$PHONE\",\"password\":\"Passw0rd!\"}")
echo "$REG" | head -c 220; echo
TOKEN1=$(echo "$REG" | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
RT1=$(echo "$REG" | python -c "import sys,json;print(json.load(sys.stdin)['data']['refreshToken'])")
echo "token1=${TOKEN1:0:20}... rt1=${RT1:0:8}..."

echo "== 1. refresh with RT1 (expect 200 + new pair) =="
RF=$(curl -s -X POST "$BASE/api/auth/refresh" -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$RT1\"}")
echo "$RF" | head -c 220; echo
TOKEN2=$(echo "$RF" | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
RT2=$(echo "$RF" | python -c "import sys,json;print(json.load(sys.stdin)['data']['refreshToken'])")
echo "token2=${TOKEN2:0:20}... rt2=${RT2:0:8}..."

echo "== 2. reuse RT1 after rotation (expect 409 reuse) =="
curl -s -w " [HTTP %{http_code}]\n" -X POST "$BASE/api/auth/refresh" -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$RT1\"}"

echo "== 3. logout with RT2 + Bearer TOKEN2 (expect 200) =="
curl -s -w " [HTTP %{http_code}]\n" -X POST "$BASE/api/auth/logout" \
  -H "Authorization: Bearer $TOKEN2" -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$RT2\"}"

echo "== 4. logout without access token (expect 401) =="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/auth/logout" \
  -H "Content-Type: application/json" -d "{\"refreshToken\":\"$RT2\"}"

echo "== 5. /me with rotated access token (expect 200) =="
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/api/users/me" -H "Authorization: Bearer $TOKEN2"

echo "== 6. eureka =="
curl -s http://localhost:8761/eureka/apps/SKILLENROLL-BACKEND | grep -o '<status>UP</status>' | head -1
