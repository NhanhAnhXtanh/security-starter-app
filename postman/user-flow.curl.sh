#!/usr/bin/env bash
# ============================================================================
# Smoke-test luồng user của db-react-springboot-be
# Endpoints: /api/auth/{register,login,refresh,change-password,logout}
#            + /api/authenticate (starter)
#
# Yêu cầu: curl, jq
# Chạy:    bash postman/user-flow.curl.sh
# Override: BASE_URL=... USERNAME=... PASSWORD=... bash postman/user-flow.curl.sh
# ============================================================================
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
USERNAME="${USERNAME:-testuser01}"
PASSWORD="${PASSWORD:-P@ssw0rd!}"
NEW_PASSWORD="${NEW_PASSWORD:-N3wP@ssw0rd!}"
EMAIL="${EMAIL:-testuser01@example.com}"

PASS_COUNT=0
FAIL_COUNT=0

green() { printf '\033[32m%s\033[0m' "$1"; }
red()   { printf '\033[31m%s\033[0m' "$1"; }
bold()  { printf '\n\033[1m== %s ==\033[0m\n' "$1"; }

expect_status() {
  local actual="$1" expected_csv="$2" label="$3"
  if [[ ",$expected_csv," == *",$actual,"* ]]; then
    echo "  $(green PASS) $label — HTTP $actual (expected $expected_csv)"
    PASS_COUNT=$((PASS_COUNT+1))
  else
    echo "  $(red FAIL) $label — HTTP $actual (expected $expected_csv)"
    FAIL_COUNT=$((FAIL_COUNT+1))
  fi
}

# -- 1. Register ---------------------------------------------------------------
bold "1) POST /api/auth/register"
HTTP=$(curl -s -o /tmp/reg.body -w '%{http_code}' \
  -X POST "$BASE_URL/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\",\"email\":\"$EMAIL\",\"firstName\":\"Test\",\"lastName\":\"User\"}")
echo "  body: $(cat /tmp/reg.body)"
expect_status "$HTTP" "201,409" "Register"

# -- 2. Login ------------------------------------------------------------------
bold "2) POST /api/auth/login"
HTTP=$(curl -s -o /tmp/login.body -w '%{http_code}' \
  -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")
cat /tmp/login.body | jq . 2>/dev/null || cat /tmp/login.body
expect_status "$HTTP" "200" "Login"
ACCESS=$(jq -r .accessToken /tmp/login.body 2>/dev/null || echo "")
REFRESH=$(jq -r .refreshToken /tmp/login.body 2>/dev/null || echo "")
if [[ -n "$ACCESS" && "$ACCESS" != "null" ]]; then
  echo "  $(green PASS) accessToken length=${#ACCESS}"; PASS_COUNT=$((PASS_COUNT+1))
else
  echo "  $(red FAIL) accessToken trống"; FAIL_COUNT=$((FAIL_COUNT+1))
fi

# -- 3. GET /api/authenticate (Bearer) -----------------------------------------
bold "3) GET /api/authenticate (Bearer)"
HTTP=$(curl -s -o /tmp/me.body -w '%{http_code}' \
  -H "Authorization: Bearer $ACCESS" \
  "$BASE_URL/api/authenticate")
echo "  body: $(cat /tmp/me.body)"
expect_status "$HTTP" "200" "Authenticate check"

# -- 4. Refresh ----------------------------------------------------------------
bold "4) POST /api/auth/refresh"
HTTP=$(curl -s -o /tmp/refresh.body -w '%{http_code}' \
  -X POST "$BASE_URL/api/auth/refresh" \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}")
cat /tmp/refresh.body | jq . 2>/dev/null || cat /tmp/refresh.body
expect_status "$HTTP" "200" "Refresh"
ACCESS=$(jq -r .accessToken /tmp/refresh.body 2>/dev/null || echo "")
REFRESH=$(jq -r .refreshToken /tmp/refresh.body 2>/dev/null || echo "")

# -- 5. Change password --------------------------------------------------------
bold "5) POST /api/auth/change-password"
HTTP=$(curl -s -o /tmp/cp.body -w '%{http_code}' \
  -X POST "$BASE_URL/api/auth/change-password" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $ACCESS" \
  -d "{\"currentPassword\":\"$PASSWORD\",\"newPassword\":\"$NEW_PASSWORD\"}")
expect_status "$HTTP" "204" "Change password"

# -- 5b. Login lại bằng mật khẩu mới -------------------------------------------
bold "5b) Login bằng mật khẩu mới"
HTTP=$(curl -s -o /tmp/login2.body -w '%{http_code}' \
  -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$NEW_PASSWORD\"}")
expect_status "$HTTP" "200" "Login với password mới"
ACCESS=$(jq -r .accessToken /tmp/login2.body 2>/dev/null || echo "")
REFRESH=$(jq -r .refreshToken /tmp/login2.body 2>/dev/null || echo "")

# -- 6. Logout -----------------------------------------------------------------
bold "6) POST /api/auth/logout"
HTTP=$(curl -s -o /dev/null -w '%{http_code}' \
  -X POST "$BASE_URL/api/auth/logout" \
  -H "Authorization: Bearer $ACCESS")
expect_status "$HTTP" "204" "Logout"

# -- 7. Negative: sai password -------------------------------------------------
bold "7) Negative — login sai password"
HTTP=$(curl -s -o /dev/null -w '%{http_code}' \
  -X POST "$BASE_URL/api/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USERNAME\",\"password\":\"wrong-password\"}")
expect_status "$HTTP" "401" "Login sai password"

# -- 8. Negative: refresh sau logout -------------------------------------------
bold "8) Negative — refresh với token đã revoke"
HTTP=$(curl -s -o /dev/null -w '%{http_code}' \
  -X POST "$BASE_URL/api/auth/refresh" \
  -H 'Content-Type: application/json' \
  -d "{\"refreshToken\":\"$REFRESH\"}")
expect_status "$HTTP" "400,401" "Refresh sau logout"

# -- Summary -------------------------------------------------------------------
bold "SUMMARY"
echo "  $(green "PASS=$PASS_COUNT")  $(red "FAIL=$FAIL_COUNT")"
[[ $FAIL_COUNT -eq 0 ]] && exit 0 || exit 1
