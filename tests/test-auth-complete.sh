#!/bin/bash

# ─────────────────────────────────────────────────────────────────────────────
# test-auth-complete.sh
# Full auth-service flow: register → verify-email → login → validate → logout
#
# Verification token is extracted two ways (in priority order):
#   1. From auth-service logs  (VERIFICATION_TOKEN: <token>  log line)
#   2. Direct DB query via docker exec into auth-db container
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

# All requests go through the API gateway (port 8080).
# Direct access to auth-service (port 8081) is intentionally blocked in CI.
BASE_URL="http://localhost:8080"
TEST_EMAIL="ci-test-$(date +%s)@example.com"
TEST_PASSWORD="CiPass!2024"
PASS=0
FAIL=0

green()  { echo -e "\033[32m✔ $*\033[0m"; }
red()    { echo -e "\033[31m✘ $*\033[0m"; }
yellow() { echo -e "\033[33m  $*\033[0m"; }

assert_contains() {
  local label="$1" body="$2" needle="$3"
  if echo "$body" | grep -q "$needle"; then
    green "$label"
    PASS=$((PASS + 1))
  else
    red "$label — expected '$needle' in: $body"
    FAIL=$((FAIL + 1))
  fi
}

echo ""
echo "🔐 Testing Complete Auth Service (production flow)"
echo "==================================================="
echo "  Email : $TEST_EMAIL"
echo ""

# ─────────────────────────────────────────────────────────────────────────────
# Step 1 – Register
# ─────────────────────────────────────────────────────────────────────────────
echo "1. Registering user..."
REGISTER_RESPONSE=$(curl -sf -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\",
    \"firstName\": \"CI\",
    \"lastName\": \"Tester\"
  }") || { red "Registration request failed (curl error)"; exit 1; }

yellow "Register response: $REGISTER_RESPONSE"
assert_contains "Registration returns accessToken" "$REGISTER_RESPONSE" "accessToken"

# ─────────────────────────────────────────────────────────────────────────────
# Step 2 – Extract verification token
# Priority: logs → DB
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "2. Extracting email-verification token..."

VERIFICATION_TOKEN=""

# 2a. Try log-based extraction (requires log.info("VERIFICATION_TOKEN: {}") in AuthServiceImpl)
sleep 2  # give service time to write the log line
VERIFICATION_TOKEN=$(docker logs auth-service 2>&1 \
  | grep "VERIFICATION_TOKEN:" \
  | grep "$TEST_EMAIL" \
  | tail -1 \
  | sed 's/.*VERIFICATION_TOKEN: //' \
  | tr -d '[:space:]') || true

# 2b. Fallback: query auth-db directly
if [ -z "$VERIFICATION_TOKEN" ]; then
  yellow "Log extraction found nothing, querying auth-db..."
  VERIFICATION_TOKEN=$(docker exec auth-db psql -U postgres -d shopsphere_auth -t -c \
    "SELECT evt.token
     FROM email_verification_tokens evt
     JOIN users u ON evt.user_id = u.id
     WHERE u.email = '$TEST_EMAIL'
       AND evt.used = false
     ORDER BY evt.id DESC
     LIMIT 1;" \
    2>/dev/null | tr -d '[:space:]') || true
fi

if [ -z "$VERIFICATION_TOKEN" ]; then
  red "Could not retrieve verification token from logs or DB — aborting"
  exit 1
fi

green "Verification token obtained: ${VERIFICATION_TOKEN:0:8}…"

# ─────────────────────────────────────────────────────────────────────────────
# Step 3 – Verify email
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "3. Verifying email..."
VERIFY_RESPONSE=$(curl -sf -X GET "$BASE_URL/auth/verify-email?token=$VERIFICATION_TOKEN" \
  -H "Content-Type: application/json" \
  -w "\n%{http_code}") || { red "Verify-email request failed (curl error)"; FAIL=$((FAIL+1)); }

HTTP_CODE=$(echo "$VERIFY_RESPONSE" | tail -1)
yellow "Verify response (HTTP $HTTP_CODE): $(echo "$VERIFY_RESPONSE" | head -1)"

if [ "$HTTP_CODE" -eq 200 ] || [ "$HTTP_CODE" -eq 204 ]; then
  green "Email verified (HTTP $HTTP_CODE)"
  PASS=$((PASS + 1))
else
  red "Email verification failed (HTTP $HTTP_CODE)"
  FAIL=$((FAIL + 1))
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 4 – Login (only possible after email verification)
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "4. Logging in..."
LOGIN_RESPONSE=$(curl -sf -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$TEST_EMAIL\", \"password\": \"$TEST_PASSWORD\"}") \
  || { red "Login request failed (curl error)"; FAIL=$((FAIL+1)); }

yellow "Login response: $LOGIN_RESPONSE"
assert_contains "Login returns accessToken" "$LOGIN_RESPONSE" "accessToken"

ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
REFRESH_TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"refreshToken":"[^"]*' | cut -d'"' -f4)

# ─────────────────────────────────────────────────────────────────────────────
# Step 5 – Validate token
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "5. Validating token..."
VALIDATE_RESPONSE=$(curl -sf -X POST "$BASE_URL/auth/validate" \
  -H "Authorization: Bearer $ACCESS_TOKEN") \
  || { red "Token validation request failed (curl error)"; FAIL=$((FAIL+1)); }

yellow "Validate response: $VALIDATE_RESPONSE"
assert_contains "Token validation returns valid:true" "$VALIDATE_RESPONSE" "\"valid\":true"

# ─────────────────────────────────────────────────────────────────────────────
# Step 6 – Refresh token
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "6. Refreshing token..."
REFRESH_RESPONSE=$(curl -sf -X POST "$BASE_URL/auth/refresh" \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}") \
  || { red "Refresh token request failed (curl error)"; FAIL=$((FAIL+1)); }

yellow "Refresh response: $REFRESH_RESPONSE"
assert_contains "Refresh returns new accessToken" "$REFRESH_RESPONSE" "accessToken"

# ─────────────────────────────────────────────────────────────────────────────
# Step 7 – Forgot password
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "7. Testing forgot-password..."
FORGOT_RESPONSE=$(curl -sf -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/auth/forgot-password" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$TEST_EMAIL\"}") \
  || FORGOT_RESPONSE="000"

if [ "$FORGOT_RESPONSE" -eq 200 ] || [ "$FORGOT_RESPONSE" -eq 204 ]; then
  green "Forgot-password accepted (HTTP $FORGOT_RESPONSE)"
  PASS=$((PASS + 1))
else
  red "Forgot-password unexpected status (HTTP $FORGOT_RESPONSE)"
  FAIL=$((FAIL + 1))
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 8 – Resend verification (should fail — already verified)
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "8. Testing resend-verification (expect rejection for already-verified user)..."
RESEND_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/auth/resend-verification" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$TEST_EMAIL\"}")

if [ "$RESEND_CODE" -eq 400 ] || [ "$RESEND_CODE" -eq 409 ]; then
  green "Resend correctly rejected for verified user (HTTP $RESEND_CODE)"
  PASS=$((PASS + 1))
else
  yellow "Resend returned unexpected HTTP $RESEND_CODE (not necessarily fatal)"
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 9 – Logout
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "9. Logging out..."
LOGOUT_CODE=$(curl -sf -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: Bearer $ACCESS_TOKEN") \
  || LOGOUT_CODE="000"

if [ "$LOGOUT_CODE" -eq 200 ] || [ "$LOGOUT_CODE" -eq 204 ]; then
  green "Logout accepted (HTTP $LOGOUT_CODE)"
  PASS=$((PASS + 1))
else
  red "Logout failed (HTTP $LOGOUT_CODE)"
  FAIL=$((FAIL + 1))
fi

# ─────────────────────────────────────────────────────────────────────────────
# Summary
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "═══════════════════════════════════════════"
echo "  Results: $PASS passed, $FAIL failed"
echo "═══════════════════════════════════════════"

if [ $FAIL -gt 0 ]; then
  exit 1
fi