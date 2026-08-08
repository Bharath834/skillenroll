#!/bin/bash
set -u
cd /d/skillenroll/backend

# Boot the packaged app on the H2 test profile on a dedicated port.
java -jar target/skillenroll-backend-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=test --server.port=18080 \
  > /tmp/skillenroll-smoke.log 2>&1 &
APP_PID=$!

code=000
for i in $(seq 1 40); do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:18080/v3/api-docs --max-time 2 2>/dev/null)
  if [ "$code" = "200" ]; then break; fi
  sleep 2
done

echo "== app pid=$APP_PID, swagger /v3/api-docs -> HTTP $code =="

if [ "$code" = "200" ]; then
  echo "== progress paths in openapi =="
  curl -s http://localhost:18080/v3/api-docs | python -c "
import sys, json
d = json.load(sys.stdin)
for k in sorted(d['paths']):
    if 'progress' in k:
        print(' ', k, sorted(d['paths'][k].keys()))
print('schemas:', [s for s in d['components']['schemas'] if 'Progress' in s])
"

  echo "== POST /api/progress without token (expect 401) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" \
    -X POST http://localhost:18080/api/progress \
    -H "Content-Type: application/json" -d '{"userId":1,"courseId":1,"progressPercentage":10}'

  echo "== GET /api/progress/1 without token (expect 401) =="
  curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:18080/api/progress/1

  echo "== GET /v3/api-docs/schema for ProgressRequest =="
  curl -s http://localhost:18080/v3/api-docs | python -c "
import sys, json
d = json.load(sys.stdin)
print(json.dumps(d['components']['schemas']['ProgressRequest'], indent=1)[:800])
"
else
  echo "== startup log tail =="
  tail -40 /tmp/skillenroll-smoke.log
fi

kill $APP_PID 2>/dev/null
wait $APP_PID 2>/dev/null
echo "== app stopped =="
