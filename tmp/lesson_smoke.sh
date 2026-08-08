#!/bin/bash
set -u
cd /d/skillenroll/backend

# Boot the packaged app on a dedicated port (falls back to main application.yml:
# local MySQL + Eureka, both already running locally).
java -jar target/skillenroll-backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=test --server.port=18080 \
  > /tmp/lesson-smoke.log 2>&1 &
APP_PID=$!

code=000
for i in $(seq 1 40); do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:18080/v3/api-docs --max-time 2 2>/dev/null)
  if [ "$code" = "200" ]; then break; fi
  sleep 2
done
echo "== app pid=$APP_PID, swagger /v3/api-docs -> HTTP $code =="

if [ "$code" = "200" ]; then
  BASE=http://localhost:18080
  STAMP=$(date +%s)
  EMAIL="lesson$STAMP@gmail.com"
  PHONE=$((8000000000 + STAMP % 1000000000))

  echo "== lesson paths in openapi =="
  curl -s $BASE/v3/api-docs | python -c "
import sys, json
d = json.load(sys.stdin)
for k in sorted(d['paths']):
    if 'lesson' in k:
        print(' ', k, sorted(d['paths'][k].keys()))
print('schemas:', [s for s in d['components']['schemas'] if 'Lesson' in s])
"

  echo "== POST /api/lessons without token (expect 401) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/api/lessons \
    -H "Content-Type: application/json" -d '{"courseId":1,"title":"x","lessonOrder":1,"durationMinutes":10}'

  echo "== register =="
  REG=$(curl -s -X POST $BASE/api/auth/register -H "Content-Type: application/json" \
    -d "{\"firstName\":\"Bharath\",\"lastName\":\"Kumar\",\"email\":\"$EMAIL\",\"phoneNumber\":\"$PHONE\",\"password\":\"Passw0rd!\"}")
  TOKEN=$(echo "$REG" | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
  USER_ID=$(echo "$REG" | python -c "import sys,json;print(json.load(sys.stdin)['data']['user']['id'])")
  echo "token: ${TOKEN:0:15}... user id: $USER_ID"

  echo "== create course =="
  COURSE=$(curl -s -X POST $BASE/api/courses -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"title\":\"Python Full Stack $STAMP\",\"category\":\"Programming\",\"price\":49.99,\"duration\":20,\"instructorName\":\"Jane Smith\"}")
  COURSE_ID=$(echo "$COURSE" | python -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
  echo "course id: $COURSE_ID"

  echo "== POST /api/lessons (expect 201) =="
  L1=$(curl -s -X POST $BASE/api/lessons -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"courseId\":$COURSE_ID,\"title\":\"Introduction to Python\",\"description\":\"Python fundamentals\",\"lessonOrder\":1,\"durationMinutes\":45}")
  echo "$L1" | head -c 220; echo
  L1_ID=$(echo "$L1" | python -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")

  echo "== create lessons 2,3 =="
  L2=$(curl -s -X POST $BASE/api/lessons -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"courseId\":$COURSE_ID,\"title\":\"Python Basics\",\"lessonOrder\":2,\"durationMinutes\":30}")
  L2_ID=$(echo "$L2" | python -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
  curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/api/lessons -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"courseId\":$COURSE_ID,\"title\":\"Advanced Python\",\"lessonOrder\":3,\"durationMinutes\":60}"

  echo "== GET /api/lessons/$L1_ID (expect 200) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/api/lessons/$L1_ID -H "Authorization: Bearer $TOKEN"

  echo "== GET /api/lessons/course/$COURSE_ID?page=0&size=2 (expect 2 of 3) =="
  curl -s "$BASE/api/lessons/course/$COURSE_ID?page=0&size=2" -H "Authorization: Bearer $TOKEN" | python -c "
import sys,json
d=json.load(sys.stdin)['data']
print('content:', len(d['content']), '| page:', d['page'], '| size:', d['size'], '| totalElements:', d['totalElements'], '| totalPages:', d['totalPages'])
"

  echo "== GET ...?sort=lessonOrder,desc (expect 3,2,1) =="
  curl -s "$BASE/api/lessons/course/$COURSE_ID?sort=lessonOrder,desc" -H "Authorization: Bearer $TOKEN" | python -c "
import sys,json
print([x['lessonOrder'] for x in json.load(sys.stdin)['data']['content']])
"

  echo "== GET ...?sort=lessonOrder,asc (expect 1,2,3) =="
  curl -s "$BASE/api/lessons/course/$COURSE_ID?sort=lessonOrder,asc" -H "Authorization: Bearer $TOKEN" | python -c "
import sys,json
print([x['lessonOrder'] for x in json.load(sys.stdin)['data']['content']])
"

  echo "== PUT /api/lessons/$L1_ID (expect 200) =="
  curl -s -w " [HTTP %{http_code}]\n" -X PUT $BASE/api/lessons/$L1_ID -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"courseId\":$COURSE_ID,\"title\":\"Python Basics renamed\",\"lessonOrder\":1,\"durationMinutes\":50}" | head -c 260

  echo "== duplicate lessonOrder same course (expect 409) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/api/lessons -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"courseId\":$COURSE_ID,\"title\":\"Duplicate\",\"lessonOrder\":1,\"durationMinutes\":10}"

  echo "== duplicate lessonOrder different course (expect 201) =="
  COURSE2=$(curl -s -X POST $BASE/api/courses -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"title\":\"Kubernetes $STAMP\",\"category\":\"DevOps\",\"price\":79.99,\"duration\":30,\"instructorName\":\"John Doe\"}")
  COURSE2_ID=$(echo "$COURSE2" | python -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
  curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/api/lessons -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"courseId\":$COURSE2_ID,\"title\":\"K8s Intro\",\"lessonOrder\":1,\"durationMinutes\":20}"

  echo "== non-existing course (expect 404) =="
  curl -s -w " [HTTP %{http_code}]\n" -X POST $BASE/api/lessons -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d '{"courseId":999999,"title":"x","lessonOrder":1,"durationMinutes":10}'

  echo "== invalid lessonOrder 0 (expect 400) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/api/lessons -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"courseId\":$COURSE_ID,\"title\":\"x\",\"lessonOrder\":0,\"durationMinutes\":10}"

  echo "== invalid durationMinutes 0 (expect 400) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/api/lessons -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"courseId\":$COURSE_ID,\"title\":\"x\",\"lessonOrder\":5,\"durationMinutes\":0}"

  echo "== unknown sort field (expect 400) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/api/lessons/course/$COURSE_ID?sort=foo,asc" -H "Authorization: Bearer $TOKEN"

  echo "== DELETE /api/lessons/$L2_ID (expect 200, then 404 on GET) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" -X DELETE $BASE/api/lessons/$L2_ID -H "Authorization: Bearer $TOKEN"
  curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/api/lessons/$L2_ID -H "Authorization: Bearer $TOKEN"

  echo "== REGRESSION: GET /api/courses still works (expect 200) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" $BASE/api/courses -H "Authorization: Bearer $TOKEN"

  echo "== REGRESSION: POST /api/enrollments still works (expect 201) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/api/enrollments -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"userId\":$USER_ID,\"courseId\":$COURSE_ID}"

  echo "== REGRESSION: POST /api/progress still works (expect 201) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" -X POST $BASE/api/progress -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
    -d "{\"userId\":$USER_ID,\"courseId\":$COURSE_ID,\"progressPercentage\":10.00}"

  echo "== LessonRequest schema in swagger =="
  curl -s $BASE/v3/api-docs | python -c "
import sys, json
d = json.load(sys.stdin)
print(json.dumps(d['components']['schemas']['LessonRequest'], indent=1)[:500])
"
else
  echo "== startup log tail =="
  tail -40 /tmp/lesson-smoke.log
fi

kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null
echo "== app stopped =="
