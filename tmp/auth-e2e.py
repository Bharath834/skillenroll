"""Temporary E2E check for the auth flow the SkillEnroll frontend performs.

Mirrors the frontend exactly:
  register -> login -> GET /users/me -> logout -> 401 after logout,
plus the error paths the UI maps to friendly messages (401, 409, 400).

Run: python tmp/auth-e2e.py   (backend must be up on :8080)
"""
import json
import time
import urllib.error
import urllib.request

BASE = "http://localhost:8080/api"
EMAIL = f"phase2.{int(time.time())}@example.com"


def call(method, path, body=None, token=None):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode() if body is not None else None
    try:
        with urllib.request.urlopen(req, data) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {}


ok = True


def check(name, cond, detail=""):
    global ok
    if not cond:
        ok = False
    print(f"{'PASS' if cond else 'FAIL'}  {name}  {detail}")


# 1. Register (exact RegisterRequest fields the form submits)
s, r = call(
    "POST",
    "/auth/register",
    {
        "firstName": "Phase2",
        "lastName": "Tester",
        "email": EMAIL,
        "phoneNumber": "+1555012345",
        "password": "Phase2Pass123",
    },
)
check("register -> 201", s == 201, f"({s})")
jwt = r.get("data") or {}
check("register returns access token", bool(jwt.get("token")), "")
check("register returns refreshToken", bool(jwt.get("refreshToken")), "")
check("register user role is STUDENT", (jwt.get("user") or {}).get("role") == "STUDENT", f"role={(jwt.get('user') or {}).get('role')}")

# 2. Login
s, r = call("POST", "/auth/login", {"email": EMAIL, "password": "Phase2Pass123"})
check("login -> 200", s == 200, f"({s})")
login_jwt = r.get("data") or {}
check("login returns access token", bool(login_jwt.get("token")), "")
check("login returns user", bool(login_jwt.get("user")), "")

# 3. GET /users/me with the JWT
s, r = call("GET", "/users/me", token=login_jwt.get("token"))
check("GET /users/me -> 200", s == 200, f"({s})")
me = r.get("data") or {}
check("me.email matches registered email", me.get("email") == EMAIL, "")
check("me.firstName present", me.get("firstName") == "Phase2", "")

# 4. Logout (refresh token in body + Bearer access token)
s, r = call(
    "POST",
    "/auth/logout",
    {"refreshToken": login_jwt.get("refreshToken")},
    token=login_jwt.get("token"),
)
check("logout -> 200", s == 200, f"({s})")

# 5. The access token must now be rejected (blacklisted) -> 401
s, r = call("GET", "/users/me", token=login_jwt.get("token"))
check("me after logout -> 401 (token blacklisted)", s == 401, f"({s})")

# 6. Wrong password -> 401 (frontend maps to "Invalid email or password")
s, r = call("POST", "/auth/login", {"email": EMAIL, "password": "WrongPass123"})
check("login wrong password -> 401", s == 401, f"({s})")

# 7. Duplicate registration -> 409 (frontend maps to "already exists")
s, r = call(
    "POST",
    "/auth/register",
    {
        "firstName": "Phase2",
        "lastName": "Tester",
        "email": EMAIL,
        "phoneNumber": "+1555012345",
        "password": "Phase2Pass123",
    },
)
check("duplicate register -> 409", s == 409, f"({s})")

# 8. Invalid payload -> 400 (frontend validates first, backend confirms)
s, r = call(
    "POST",
    "/auth/register",
    {
        "firstName": "",
        "lastName": "Tester",
        "email": EMAIL,
        "phoneNumber": "+1555012345",
        "password": "Phase2Pass123",
    },
)
check("validation error -> 400", s == 400, f"({s})")

print("\n" + ("ALL E2E CHECKS PASSED" if ok else "SOME E2E CHECKS FAILED"))
raise SystemExit(0 if ok else 1)
