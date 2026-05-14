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
BASE_URL="https://localhost"

# Gmail + alias: each run gets a unique address (no DB conflict between runs)
# but all emails are delivered to nikhilyadav.d3v@gmail.com — visible in real inbox.
TEST_EMAIL="nikhilyadav.d3v+ci-$(date +%s)@gmail.com"
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
REGISTER_RESPONSE=$(curl -k -s -w "\nHTTP_STATUS:%{http_code}" -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$TEST_EMAIL\",
    \"password\": \"$TEST_PASSWORD\",
    \"firstName\": \"CI\",
    \"lastName\": \"Tester\"
  }") || true

HTTP_STATUS=$(echo "$REGISTER_RESPONSE" | grep "HTTP_STATUS:" | cut -d':' -f2)
REGISTER_BODY=$(echo "$REGISTER_RESPONSE" | sed '/HTTP_STATUS:/d')

if [ "$HTTP_STATUS" != "201" ] && [ "$HTTP_STATUS" != "200" ]; then
  red "Registration request failed with HTTP $HTTP_STATUS"
  yellow "Response body: $REGISTER_BODY"
  exit 1
fi

yellow "Register response: $REGISTER_BODY"
assert_contains "Registration returns accessToken" "$REGISTER_BODY" "accessToken"

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
VERIFY_RESPONSE=$(curl -k -s -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/auth/verify-email" \
  -H "Content-Type: application/json" \
  -d "{\"token\": \"$VERIFICATION_TOKEN\"}")

HTTP_CODE="$VERIFY_RESPONSE"
yellow "Verify response (HTTP $HTTP_CODE)"

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
LOGIN_RESPONSE=$(curl -k -sf -X POST "$BASE_URL/auth/login" \
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
VALIDATE_RESPONSE=$(curl -k -sf -X POST "$BASE_URL/auth/validate" \
  -H "Authorization: Bearer $ACCESS_TOKEN") \
  || { red "Token validation request failed (curl error)"; FAIL=$((FAIL+1)); }

yellow "Validate response: $VALIDATE_RESPONSE"
assert_contains "Token validation returns valid:true" "$VALIDATE_RESPONSE" "\"valid\":true"

# ─────────────────────────────────────────────────────────────────────────────
# Step 6 – Refresh token
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "6. Refreshing token..."
REFRESH_RESPONSE=$(curl -k -sf -X POST "$BASE_URL/auth/refresh" \
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
FORGOT_RESPONSE=$(curl -k -sf -o /dev/null -w "%{http_code}" \
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
RESEND_CODE=$(curl -k -s -o /dev/null -w "%{http_code}" \
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
LOGOUT_CODE=$(curl -k -sf -o /dev/null -w "%{http_code}" \
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
# Step 10 – Brute Force Lockout
# ─────────────────────────────────────────────────────────────────────────────
echo ""
echo "10. Testing brute-force lockout..."

# 5 failed attempts
for i in {1..5}; do
  curl -k -sf -o /dev/null -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\": \"$TEST_EMAIL\", \"password\": \"WrongPass!$i\"}" || true
done

# 6th attempt should be locked out (even with correct password)
LOCKOUT_RESPONSE=$(curl -k -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$TEST_EMAIL\", \"password\": \"$TEST_PASSWORD\"}")

yellow "Lockout response: $LOCKOUT_RESPONSE"
assert_contains "Account gets locked out after 5 attempts" "$LOCKOUT_RESPONSE" "locked"

# ─────────────────────────────────────────────────────────────────────────────
# Step 11 – Google Login
# ─────────────────────────────────────────────────────────────────────────────
echo ""
# Note: Google ID Tokens expire in 1 hour. This hardcoded token is for immediate CI testing.
TEST_GOOGLE_ID_TOKEN="eyJhbGciOiJSUzI1NiIsImtpZCI6ImY4ZTY2MjBkMzk3MTFhYTIxY2U4YTJiZjJmM2VlMDFiOTI0Y2IyZDAiLCJ0eXAiOiJKV1QifQ.eyJpc3MiOiJodHRwczovL2FjY291bnRzLmdvb2dsZS5jb20iLCJhenAiOiIzMzk3OTc0NTA5NDUtYTRmNDVpYXU1NDJmYTNna210aTJvaXByaGNkam9vc2MuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJhdWQiOiIzMzk3OTc0NTA5NDUtYTRmNDVpYXU1NDJmYTNna210aTJvaXByaGNkam9vc2MuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMTc0MTEzNDM0NjYwMzg0MTIyMDciLCJlbWFpbCI6Im5pa2hpbGRldnIuMDFAZ21haWwuY29tIiwiZW1haWxfdmVyaWZpZWQiOnRydWUsImF0X2hhc2giOiJjeDVBa0F6WWJUdEJYUVRPVjNnM2NBIiwibmFtZSI6Ik5pa2hpbCBZYWRhdiIsInBpY3R1cmUiOiJodHRwczovL2xoMy5nb29nbGV1c2VyY29udGVudC5jb20vYS9BQ2c4b2NMWUpoQXhuM0FyX1RaZVdzVGdsUjM3WjBSMGI2MnVjLXFSRS1vaXdEbzF3UFkzMHc9czk2LWMiLCJnaXZlbl9uYW1lIjoiTmlraGlsIiwiZmFtaWx5X25hbWUiOiJZYWRhdiIsImlhdCI6MTc3ODc2ODM3MSwiZXhwIjoxNzc4NzcxOTcxfQ.MP7FBpAINqJOdgXi3JNecjEB3DphqgFQEWW_1Ef-KbvasEBVDNFdWmGlO2AwQwqVLGCDH7DDMP3Ldn-1fBfEDyEhv8ur2FGLqqx3qhM3N-wKZpboFh_NNJFk_A-obU7SqpPNzLXkKjv6LuriyJNKnnU7C2dGYZMLOpYq_oQ-u_R-y90qsodDPW9tm4w2DiZRRd8QRIUlRAzdLpmTxKg3KGCzLYrha__zCHXYHO3fbHRJ7_xpesAe3CvwAGaXZp-kJC4SaSPb1fvrFSFrdQnzex-MlT0J8ZGZd6YtkMHwnoeq14ZEwZngw6t1MvWGeGEu4i2lOAGEHLM1xO-CkBSP4Q"
GOOGLE_TOKEN="${TEST_GOOGLE_ID_TOKEN:-invalid-google-token}"
if [ "$GOOGLE_TOKEN" == "invalid-google-token" ]; then
  echo "11. Testing Google login with invalid token..."
  EXPECTED_CODE=401
else
  echo "11. Testing Google login with valid token..."
  EXPECTED_CODE=200
fi

GOOGLE_RESPONSE=$(curl -k -s -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/auth/google" \
  -H "Content-Type: application/json" \
  -d "{\"idToken\": \"$GOOGLE_TOKEN\"}")

if [ "$GOOGLE_RESPONSE" -eq "$EXPECTED_CODE" ]; then
  green "Google login test passed (HTTP $GOOGLE_RESPONSE)"
  PASS=$((PASS + 1))
else
  red "Google login failed: Expected HTTP $EXPECTED_CODE but got $GOOGLE_RESPONSE"
  FAIL=$((FAIL + 1))
fi

# ─────────────────────────────────────────────────────────────────────────────
# Step 12 – Apple Login
# ─────────────────────────────────────────────────────────────────────────────
echo ""
APPLE_TOKEN="${TEST_APPLE_ID_TOKEN:-invalid-apple-token}"
if [ "$APPLE_TOKEN" == "invalid-apple-token" ]; then
  echo "12. Testing Apple login with invalid token..."
  EXPECTED_CODE=401
else
  echo "12. Testing Apple login with valid token..."
  EXPECTED_CODE=200
fi

APPLE_RESPONSE=$(curl -k -s -o /dev/null -w "%{http_code}" \
  -X POST "$BASE_URL/auth/apple" \
  -H "Content-Type: application/json" \
  -d "{\"idToken\": \"$APPLE_TOKEN\"}")

if [ "$APPLE_RESPONSE" -eq "$EXPECTED_CODE" ]; then
  green "Apple login test passed (HTTP $APPLE_RESPONSE)"
  PASS=$((PASS + 1))
else
  red "Apple login failed: Expected HTTP $EXPECTED_CODE but got $APPLE_RESPONSE"
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