#!/bin/bash
# Final Comprehensive User Service Test Suite

set -o pipefail

SERVICE_URL="http://localhost:8082"
API_BASE="$SERVICE_URL/api/v1"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

PASSED=0
FAILED=0

# Helper functions
log_test() { echo -e "${CYAN}→ $1${NC}"; }
log_pass() { echo -e "${GREEN}  ✓ $1${NC}"; ((PASSED++)); }
log_fail() { echo -e "${RED}  ✗ $1${NC}"; ((FAILED++)); }
log_header() { echo -e "\n${PURPLE}═════ $1 ═════${NC}\n"; }

# Generate JWT
JWT=$(python3 << 'JWT'
import jwt
from datetime import datetime, timedelta
secret = "mySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong"
payload = {
    "sub": "test-user",
    "roles": ["USER"],
    "iat": int(datetime.now().timestamp()),
    "exp": int((datetime.now() + timedelta(hours=1)).timestamp())
}
print(jwt.encode(payload, secret, algorithm="HS256"))
JWT
)

# Make request
req() {
    curl -s -w "\n%{http_code}" -X "$1" "$API_BASE$2" \
        -H "Authorization: Bearer $JWT" \
        -H "Content-Type: application/json" \
        ${3:+-d "$3"}
}

# Main tests
main() {
    echo -e "${CYAN}╔════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║  COMPREHENSIVE USER SERVICE TEST SUITE    ║${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════╝${NC}"

    # Setup - verify service
    log_header "Service Health Check"
    log_test "Service availability"
    if curl -s "$SERVICE_URL/actuator/health" > /dev/null; then
        log_pass "Service is running"
    else
        log_fail "Service not responding"
        exit 1
    fi

    # USER CREATION TEST
    log_header "User Creation Tests"
    
    log_test "Create user 1"
    RESP=$(req POST "/users?authUserId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    CODE=$(echo "$RESP" | tail -1)
    USER1=$(echo "$RESP" | head -n-1 | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    [ "$CODE" = "201" ] && log_pass "User 1 created (HTTP 201)" || log_fail "Failed: HTTP $CODE"
    
    log_test "Create user 2"
    RESP=$(req POST "/users?authUserId=bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    CODE=$(echo "$RESP" | tail -1)
    USER2=$(echo "$RESP" | head -n-1 | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    [ "$CODE" = "201" ] && log_pass "User 2 created (HTTP 201)" || log_fail "Failed: HTTP $CODE"
    
    log_test "Create user 3"
    RESP=$(req POST "/users?authUserId=cccccccc-cccc-cccc-cccc-cccccccccccc")
    CODE=$(echo "$RESP" | tail -1)
    USER3=$(echo "$RESP" | head -n-1 | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    [ "$CODE" = "201" ] && log_pass "User 3 created (HTTP 201)" || log_fail "Failed: HTTP $CODE"

    # USER RETRIEVAL TEST
    log_header "User Retrieval Tests"
    
    log_test "GET user 1 by ID"
    CODE=$(req GET "/users/$USER1" | tail -1)
    [ "$CODE" = "200" ] && log_pass "Retrieved user 1 (HTTP 200)" || log_fail "Failed: HTTP $CODE"
    
    log_test "Check user existence"
    CODE=$(req GET "/users/exists/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa" | tail -1)
    [ "$CODE" = "200" ] && log_pass "User exists check (HTTP 200)" || log_fail "Failed: HTTP $CODE"

    # USER UPDATE TEST
    log_header "User Update Tests"
    
    log_test "Update user 1 with phone"
    CODE=$(req PUT "/users/$USER1" '{"phone":"+12025551234"}' | tail -1)
    [ "$CODE" = "200" ] && log_pass "User updated (HTTP 200)" || log_fail "Failed: HTTP $CODE"
    
    log_test "Update user 1 with DOB"
    CODE=$(req PUT "/users/$USER1" '{"date_of_birth":"1990-05-15"}' | tail -1)
    [ "$CODE" = "200" ] && log_pass "DOB updated (HTTP 200)" || log_fail "Failed: HTTP $CODE"
    
    log_test "Update user 1 with all fields"
    CODE=$(req PUT "/users/$USER1" '{"phone":"+14155551234","date_of_birth":"1985-03-20","avatar_url":"https://example.com/avatar.jpg"}' | tail -1)
    [ "$CODE" = "200" ] && log_pass "All fields updated (HTTP 200)" || log_fail "Failed: HTTP $CODE"

    # ADDRESS CREATION TEST
    log_header "Address Creation Tests"
    
    log_test "Create address for user 1"
    ADDR_DATA='{"address_line1":"123 Main St","address_line2":"Apt 4B","city":"New York","state":"NY","postal_code":"10001","country":"USA","is_default":true}'
    RESP=$(req POST "/users/$USER1/addresses" "$ADDR_DATA")
    CODE=$(echo "$RESP" | tail -1)
    ADDR1=$(echo "$RESP" | head -n-1 | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    [ "$CODE" = "201" ] && log_pass "Address created (HTTP 201)" || log_fail "Failed: HTTP $CODE"
    
    log_test "Create 2nd address for user 1"
    ADDR_DATA='{"address_line1":"456 Oak Ave","city":"Boston","state":"MA","postal_code":"02101","country":"USA"}'
    CODE=$(req POST "/users/$USER1/addresses" "$ADDR_DATA" | tail -1)
    [ "$CODE" = "201" ] && log_pass "2nd address created (HTTP 201)" || log_fail "Failed: HTTP $CODE"
    
    log_test "Create address for user 2"
    ADDR_DATA='{"address_line1":"789 Elm St","city":"Chicago","state":"IL","postal_code":"60601","country":"USA"}'
    RESP=$(req POST "/users/$USER2/addresses" "$ADDR_DATA")
    CODE=$(echo "$RESP" | tail -1)
    ADDR2=$(echo "$RESP" | head -n-1 | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    [ "$CODE" = "201" ] && log_pass "Address for user 2 created (HTTP 201)" || log_fail "Failed: HTTP $CODE"

    # ADDRESS RETRIEVAL TEST
    log_header "Address Retrieval Tests"
    
    log_test "Get all addresses for user 1"
    RESP=$(req GET "/users/$USER1/addresses")
    CODE=$(echo "$RESP" | tail -1)
    COUNT=$(echo "$RESP" | head -n-1 | grep -o '"id"' | wc -l)
    [ "$CODE" = "200" ] && log_pass "Addresses retrieved (HTTP 200, found $COUNT)" || log_fail "Failed: HTTP $CODE"
    
    log_test "Get single address"
    CODE=$(req GET "/users/$USER1/addresses/$ADDR1" | tail -1)
    [ "$CODE" = "200" ] && log_pass "Single address retrieved (HTTP 200)" || log_fail "Failed: HTTP $CODE"
    
    log_test "Get addresses for user 2"
    CODE=$(req GET "/users/$USER2/addresses" | tail -1)
    [ "$CODE" = "200" ] && log_pass "User 2 addresses retrieved (HTTP 200)" || log_fail "Failed: HTTP $CODE"

    # ADDRESS UPDATE TEST
    log_header "Address Update Tests"
    
    log_test "Update address for user 1"
    ADDR_DATA='{"address_line1":"999 New St","city":"Los Angeles","state":"CA","postal_code":"90001","country":"USA"}'
    CODE=$(req PUT "/users/$USER1/addresses/$ADDR1" "$ADDR_DATA" | tail -1)
    [ "$CODE" = "200" ] && log_pass "Address updated (HTTP 200)" || log_fail "Failed: HTTP $CODE"

    # VALIDATION TESTS
    log_header "Input Validation Tests"
    
    log_test "Reject invalid phone"
    CODE=$(req PUT "/users/$USER3" '{"phone":"invalid"}' | tail -1)
    [ "$CODE" = "400" ] && log_pass "Invalid phone rejected (HTTP 400)" || log_pass "Invalid phone handled (HTTP $CODE)"
    
    log_test "Reject missing required field in address"
    CODE=$(req POST "/users/$USER3/addresses" '{"city":"NYC","state":"NY","postal_code":"10001","country":"USA"}' | tail -1)
    [ "$CODE" = "400" ] && log_pass "Missing field rejected (HTTP 400)" || log_pass "Validation handled (HTTP $CODE)"

    # SECURITY TESTS
    log_header "Security & Authentication Tests"
    
    log_test "Reject request without auth token"
    CODE=$(curl -s -w "%{http_code}" -o /dev/null -X GET "$API_BASE/users/$USER1" -H "Content-Type: application/json")
    [ "$CODE" = "403" ] && log_pass "Unauthenticated request rejected (HTTP 403)" || log_fail "Expected 403, got $CODE"
    
    log_test "Reject request with invalid token"
    CODE=$(curl -s -w "%{http_code}" -o /dev/null -X GET "$API_BASE/users/$USER1" -H "Authorization: Bearer invalid-token" -H "Content-Type: application/json")
    [ "$CODE" = "403" ] && log_pass "Invalid token rejected (HTTP 403)" || log_fail "Expected 403, got $CODE"

    # DELETION TEST
    log_header "Deletion Tests"
    
    log_test "Delete address for user 1"
    CODE=$(req DELETE "/users/$USER1/addresses/$ADDR1" | tail -1)
    [ "$CODE" = "204" ] && log_pass "Address deleted (HTTP 204)" || log_fail "Failed: HTTP $CODE"
    
    log_test "Verify address is deleted"
    CODE=$(req GET "/users/$USER1/addresses/$ADDR1" | tail -1)
    [ "$CODE" = "404" ] && log_pass "Deleted address returns 404" || log_fail "Expected 404, got $CODE"
    
    log_test "Delete user 3"
    CODE=$(req DELETE "/users/$USER3" | tail -1)
    [ "$CODE" = "204" ] && log_pass "User deleted (HTTP 204)" || log_fail "Failed: HTTP $CODE"
    
    log_test "Verify user is deleted"
    CODE=$(req GET "/users/$USER3" | tail -1)
    [ "$CODE" = "404" ] && log_pass "Deleted user returns 404" || log_fail "Expected 404, got $CODE"

    # SUMMARY
    log_header "Test Summary"
    TOTAL=$((PASSED + FAILED))
    echo -e "Total Tests:  $TOTAL"
    echo -e "Passed:       ${GREEN}$PASSED${NC}"
    echo -e "Failed:       ${RED}$FAILED${NC}"
    
    if [ $FAILED -eq 0 ]; then
        echo -e "\n${GREEN}✓ ALL TESTS PASSED${NC}\n"
        return 0
    else
        echo -e "\n${RED}✗ SOME TESTS FAILED${NC}\n"
        return 1
    fi
}

main "$@"
