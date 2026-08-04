#!/bin/bash
set -u
BASE=http://localhost:8080
EMAIL="final$(date +%s)@gmail.com"
PHONE="8555666777"

echo "== Swagger UI =="
curl -s -o /dev/null -w "GET /swagger-ui/index.html -> HTTP %{http_code}\n" "$BASE/swagger-ui/index.html"
curl -s -o /dev/null -w "GET /v3/api-docs        -> HTTP %{http_code}\n" "$BASE/v3/api-docs"
curl -s "$BASE/v3/api-docs" | python -c "import sys,json;d=json.load(sys.stdin);print('openapi:',d.get('openapi'),'| paths:',len(d.get('paths',{})))" 2>/dev/null

echo "== Register =="
REG=$(curl -s -X POST "$BASE/api/auth/register" -H "Content-Type: application/json" \
  -d "{\"firstName\":\"Bharath\",\"lastName\":\"Kumar\",\"email\":\"$EMAIL\",\"phoneNumber\":\"$PHONE\",\"password\":\"Passw0rd!\"}")
echo "$REG" | head -c 90; echo
TOKEN=$(echo "$REG" | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
RT=$(echo "$REG" | python -c "import sys,json;print(json.load(sys.stdin)['data']['refreshToken'])")

echo "== /me (expect 200) =="
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/api/users/me" -H "Authorization: Bearer $TOKEN"

echo "== Refresh (expect 200, new pair) =="
RF=$(curl -s -X POST "$BASE/api/auth/refresh" -H "Content-Type: application/json" -d "{\"refreshToken\":\"$RT\"}")
echo "$RF" | head -c 60; echo
NEW_RT=$(echo "$RF" | python -c "import sys,json;print(json.load(sys.stdin)['data']['refreshToken'])")

echo "== Reuse old refresh (expect 409) =="
curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST "$BASE/api/auth/refresh" -H "Content-Type: application/json" -d "{\"refreshToken\":\"$RT\"}"

echo "== Logout (expect 200) =="
LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"$EMAIL\",\"password\":\"Passw0rd!\"}")
TOKEN2=$(echo "$LOGIN" | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
RT2=$(echo "$LOGIN" | python -c "import sys,json;print(json.load(sys.stdin)['data']['refreshToken'])")
curl -s -w " [HTTP %{http_code}]\n" -X POST "$BASE/api/auth/logout" \
  -H "Authorization: Bearer $TOKEN2" -H "Content-Type: application/json" -d "{\"refreshToken\":\"$RT2\"}"

echo "== /me with logged-out token (expect 401 revoked) =="
curl -s -w " [HTTP %{http_code}]\n" "$BASE/api/users/me" -H "Authorization: Bearer $TOKEN2"

echo "== Protected API without token (expect 401) =="
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/api/users"

echo "== Eureka =="
curl -s http://localhost:8761/eureka/apps/SKILLENROLL-BACKEND | grep -o '<status>UP</status>' | head -1

echo "== Swagger login via /v3/api-docs security scheme =="
curl -s "$BASE/v3/api-docs" | python -c "import sys,json;d=json.load(sys.stdin);print('securitySchemes:',list(d.get('components',{}).get('securitySchemes',{}).keys()))"
