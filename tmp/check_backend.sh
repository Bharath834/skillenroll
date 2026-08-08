#!/bin/bash
set -u
code=000
for i in $(seq 1 60); do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/v3/api-docs --max-time 2 2>/dev/null)
  if [ "$code" = "200" ]; then break; fi
  sleep 2
done
echo "swagger /v3/api-docs -> HTTP $code"
if [ "$code" = "200" ]; then
  echo "---LESSON-PATHS---"
  curl -s http://localhost:8080/v3/api-docs | python -c "
import sys, json
d = json.load(sys.stdin)
print('lesson paths:', {k: sorted(v.keys()) for k, v in d['paths'].items() if 'lesson' in k})
print('lesson schemas:', [s for s in d['components']['schemas'] if 'Lesson' in s])
"
  echo "---NO-TOKEN-CHECK---"
  curl -s -o /dev/null -w "GET / -> HTTP %{http_code}\n" http://localhost:8080/
  curl -s -o /dev/null -w "GET /api/lessons -> HTTP %{http_code}\n" http://localhost:8080/api/lessons
else
  echo "---OUT-LOG-TAIL---"
  tail -30 /d/skillenroll/backend/target/backend-8080.log 2>/dev/null || echo "no out log"
  echo "---ERR-LOG-TAIL---"
  tail -30 /d/skillenroll/backend/target/backend-8080-err.log 2>/dev/null || echo "no err log"
fi
echo "---PID-ON-8080---"
netstat -ano | grep ':8080 ' | grep LISTENING | awk '{print "pid=" $NF}' | sort -u
