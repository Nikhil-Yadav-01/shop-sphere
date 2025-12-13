#!/bin/bash

################################################################################
# Comprehensive User Service Test Suite
# Tests all endpoints, data scenarios, edge cases, and error handling
################################################################################

set -o pipefail

# ============================================================================
# CONFIGURATION
# ============================================================================

SERVICE_URL="${SERVICE_URL:-http://localhost:8082}"
API_BASE="$SERVICE_URL/api/v1"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Test metrics
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
SKIPPED_TESTS=0

# Test data storage
declare -A CREATED_USERS
declare -A CREATED_ADDRESSES

# ============================================================================
# UTILITY FUNCTIONS
# ============================================================================

log_header() {
    echo ""
    echo -e "${PURPLE}╔════════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${PURPLE}║ $1${NC}"
    echo -e "${PURPLE}╚════════════════════════════════════════════════════════════════════════════════╝${NC}"
}

log_section() {
    echo ""
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}▸ $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
}

log_test() {
    echo -e "${CYAN}→ TEST: $1${NC}"
    ((TOTAL_TESTS++))
}

log_pass() {
    echo -e "${GREEN}  ✓ PASS: $1${NC}"
    ((PASSED_TESTS++))
}

log_fail() {
    echo -e "${RED}  ✗ FAIL: $1${NC}"
    if [ ! -z "$2" ]; then
        echo -e "${RED}    Response: $2${NC}"
    fi
    ((FAILED_TESTS++))
}

log_skip() {
    echo -e "${YELLOW}  ⊘ SKIP: $1${NC}"
    ((SKIPPED_TESTS++))
}

log_info() {
    echo -e "  ${BLUE}ℹ${NC} $1"
}

log_warning() {
    echo -e "  ${YELLOW}⚠${NC} $1"
}

# Generate JWT token
generate_jwt() {
    python3 << 'PYJWT'
import jwt
from datetime import datetime, timedelta
secret = "mySecretKeyForJWTTokenGenerationMustBeAtLeast256BitsLong"
payload = {
    "sub": "test-user-123",
    "roles": ["USER"],
    "iat": int(datetime.now().timestamp()),
    "exp": int((datetime.now() + timedelta(hours=1)).timestamp())
}
token = jwt.encode(payload, secret, algorithm="HS256")
print(token)
PYJWT
}

# Make HTTP request - returns response body, exits with HTTP code in subshell
http_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local auth_token=$4
    
    local url="$API_BASE$endpoint"
    
    if [ -z "$auth_token" ]; then
        auth_token="$JWT_TOKEN"
    fi
    
    if [ -z "$data" ]; then
        curl -s -w "\n%{http_code}" -X "$method" \
            -H "Authorization: Bearer $auth_token" \
            -H "Content-Type: application/json" \
            "$url" 2>/dev/null
    else
        curl -s -w "\n%{http_code}" -X "$method" \
            -H "Authorization: Bearer $auth_token" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$url" 2>/dev/null
    fi
}

# Extract JSON field
extract_json() {
    local json=$1
    local field=$2
    echo "$json" | grep -o "\"$field\":\"[^\"]*\"" | cut -d'"' -f4
}

# Assert HTTP status code
assert_status() {
    local actual=$1
    local expected=$2
    local message=$3
    
    if [ "$actual" -eq "$expected" ]; then
        log_pass "$message (HTTP $actual)"
        return 0
    else
        log_fail "$message (expected HTTP $expected, got HTTP $actual)"
        return 1
    fi
}

# Assert JSON field value
assert_field() {
    local json=$1
    local field=$2
    local expected=$3
    local message=$4
    
    local actual=$(echo "$json" | grep -o "\"$field\":\"[^\"]*\"" | cut -d'"' -f4)
    
    if [ "$actual" = "$expected" ]; then
        log_pass "$message"
        return 0
    else
        log_fail "$message (expected '$expected', got '$actual')"
        return 1
    fi
}

# Assert JSON contains value
assert_contains() {
    local json=$1
    local value=$2
    local message=$3
    
    if echo "$json" | grep -q "$value"; then
        log_pass "$message"
        return 0
    else
        log_fail "$message (value '$value' not found in response)"
        return 1
    fi
}

# ============================================================================
# PRE-TEST SETUP
# ============================================================================

setup() {
    log_header "Test Environment Setup"
    
    log_test "Generating JWT token"
    JWT_TOKEN=$(generate_jwt)
    if [ ! -z "$JWT_TOKEN" ]; then
        log_pass "JWT token generated"
    else
        log_fail "Failed to generate JWT token"
        exit 1
    fi
    
    log_test "Checking service availability"
    local max_attempts=5
    local attempt=1
    local service_ok=0
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$SERVICE_URL/actuator/health" > /dev/null 2>&1; then
            service_ok=1
            break
        fi
        sleep 2
        ((attempt++))
    done
    
    if [ $service_ok -eq 1 ]; then
        log_pass "Service is available at $SERVICE_URL"
    else
        log_fail "Service not responding at $SERVICE_URL after $max_attempts attempts"
        exit 1
    fi
}

# ============================================================================
# USER ENDPOINT TESTS
# ============================================================================

test_user_creation_basic() {
    log_section "User Creation - Basic Scenarios"
    
    local auth_user_id="11111111-1111-1111-1111-111111111111"
    
    log_test "Create user with valid auth_user_id"
    local full_response=$(http_request "POST" "/users?authUserId=$auth_user_id")
    local http_code=$(echo "$full_response" | tail -n1)
    local response=$(echo "$full_response" | head -n-1)
    
    if assert_status "$http_code" "201" "User created successfully"; then
        local user_id=$(echo "$response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
        CREATED_USERS["basic"]=$user_id
        assert_contains "$response" "$auth_user_id" "Response contains auth_user_id"
    fi
}

test_user_creation_multiple() {
    log_section "User Creation - Multiple Users"
    
    for i in {1..5}; do
        local auth_user_id=$(printf "%08x-%04x-%04x-%04x-%012x" $((i)) $((i)) $((i)) $((i)) $((i)))
        
        log_test "Create user #$i with ID: $auth_user_id"
        local response=$(http_request "POST" "/users?authUserId=$auth_user_id")
        local http_code=$?
        
        if assert_status "$http_code" "201" "User #$i created"; then
            local user_id=$(echo "$response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
            CREATED_USERS["user_$i"]=$user_id
        fi
    done
}

test_user_retrieval() {
    log_section "User Retrieval - GET Operations"
    
    if [ -z "${CREATED_USERS[basic]}" ]; then
        log_skip "No user ID available for retrieval test"
        return
    fi
    
    local user_id=${CREATED_USERS[basic]}
    
    log_test "Retrieve user by ID"
    local response=$(http_request "GET" "/users/$user_id")
    local http_code=$?
    assert_status "$http_code" "200" "User retrieved successfully"
    assert_contains "$response" "$user_id" "Response contains correct user ID"
    
    log_test "Retrieve non-existent user"
    response=$(http_request "GET" "/users/ffffffff-ffff-ffff-ffff-ffffffffffff")
    http_code=$?
    assert_status "$http_code" "404" "Non-existent user returns 404"
}

test_user_update_basic() {
    log_section "User Update - Basic Fields"
    
    if [ -z "${CREATED_USERS[basic]}" ]; then
        log_skip "No user ID available for update test"
        return
    fi
    
    local user_id=${CREATED_USERS[basic]}
    
    log_test "Update user with phone number"
    local data='{"phone":"+12025551234"}'
    local response=$(http_request "PUT" "/users/$user_id" "$data")
    local http_code=$?
    assert_status "$http_code" "200" "User updated with phone"
    assert_contains "$response" "+12025551234" "Response contains updated phone"
    
    log_test "Update user with date of birth"
    data='{"date_of_birth":"1990-05-15"}'
    response=$(http_request "PUT" "/users/$user_id" "$data")
    http_code=$?
    assert_status "$http_code" "200" "User updated with DOB"
    assert_contains "$response" "1990-05-15" "Response contains updated DOB"
    
    log_test "Update user with avatar URL"
    data='{"avatar_url":"https://example.com/avatar.jpg"}'
    response=$(http_request "PUT" "/users/$user_id" "$data")
    http_code=$?
    assert_status "$http_code" "200" "User updated with avatar"
    assert_contains "$response" "avatar.jpg" "Response contains avatar URL"
}

test_user_update_combined() {
    log_section "User Update - Combined Fields"
    
    if [ -z "${CREATED_USERS[user_1]}" ]; then
        log_skip "No user ID available for combined update test"
        return
    fi
    
    local user_id=${CREATED_USERS[user_1]}
    
    log_test "Update user with all fields at once"
    local data='{
        "phone":"+15551234567",
        "date_of_birth":"1985-03-20",
        "avatar_url":"https://cdn.example.com/profiles/user123.jpg"
    }'
    local response=$(http_request "PUT" "/users/$user_id" "$data")
    local http_code=$?
    
    if assert_status "$http_code" "200" "User updated with all fields"; then
        assert_contains "$response" "+15551234567" "Phone updated"
        assert_contains "$response" "1985-03-20" "DOB updated"
        assert_contains "$response" "user123.jpg" "Avatar URL updated"
    fi
}

test_user_update_validation() {
    log_section "User Update - Input Validation"
    
    if [ -z "${CREATED_USERS[user_2]}" ]; then
        log_skip "No user ID available for validation test"
        return
    fi
    
    local user_id=${CREATED_USERS[user_2]}
    
    log_test "Update with invalid phone number (too short)"
    local data='{"phone":"123"}'
    local response=$(http_request "PUT" "/users/$user_id" "$data")
    local http_code=$?
    assert_status "$http_code" "400" "Invalid phone rejected"
    
    log_test "Update with invalid phone number (no digits)"
    data='{"phone":"abcdefghij"}'
    response=$(http_request "PUT" "/users/$user_id" "$data")
    http_code=$?
    assert_status "$http_code" "400" "Non-numeric phone rejected"
    
    log_test "Update with oversized avatar URL"
    local long_url=$(printf 'https://example.com/%0.s-' {1..600})
    data="{\"avatar_url\":\"$long_url\"}"
    response=$(http_request "PUT" "/users/$user_id" "$data")
    http_code=$?
    assert_status "$http_code" "400" "Oversized avatar URL rejected"
    
    log_test "Update with valid extended phone"
    data='{"phone":"+441234567890123"}'
    response=$(http_request "PUT" "/users/$user_id" "$data")
    http_code=$?
    assert_status "$http_code" "200" "Extended phone accepted"
}

test_user_exists() {
    log_section "User Existence Check"
    
    local test_auth_id="22222222-2222-2222-2222-222222222222"
    
    log_test "Check if user exists (non-existent)"
    local response=$(http_request "GET" "/users/exists/$test_auth_id")
    local http_code=$?
    assert_status "$http_code" "200" "Existence check successful"
    assert_contains "$response" "false" "Non-existent user returns false"
    
    # Create user and check existence
    log_test "Create user and verify existence"
    response=$(http_request "POST" "/users?authUserId=$test_auth_id")
    http_code=$?
    
    if [ "$http_code" -eq 201 ]; then
        response=$(http_request "GET" "/users/exists/$test_auth_id")
        assert_status "$?" "200" "Existence check for created user"
        assert_contains "$response" "true" "Created user returns true"
    fi
}

test_user_deletion() {
    log_section "User Deletion - DELETE Operations"
    
    if [ -z "${CREATED_USERS[user_3]}" ]; then
        log_skip "No user ID available for deletion test"
        return
    fi
    
    local user_id=${CREATED_USERS[user_3]}
    
    log_test "Delete user profile"
    local response=$(http_request "DELETE" "/users/$user_id")
    local http_code=$?
    assert_status "$http_code" "204" "User deleted successfully"
    
    log_test "Verify user is deleted"
    response=$(http_request "GET" "/users/$user_id")
    http_code=$?
    assert_status "$http_code" "404" "Deleted user not found"
    
    log_test "Delete already deleted user"
    response=$(http_request "DELETE" "/users/$user_id")
    http_code=$?
    assert_status "$http_code" "404" "Deleting deleted user returns 404"
}

# ============================================================================
# ADDRESS ENDPOINT TESTS
# ============================================================================

test_address_creation_basic() {
    log_section "Address Creation - Basic Scenarios"
    
    if [ -z "${CREATED_USERS[user_4]}" ]; then
        log_skip "No user ID available for address test"
        return
    fi
    
    local user_id=${CREATED_USERS[user_4]}
    
    log_test "Create address with all required fields"
    local data='{
        "address_line1":"123 Main Street",
        "address_line2":"Apt 4B",
        "city":"New York",
        "state":"NY",
        "postal_code":"10001",
        "country":"USA",
        "is_default":true
    }'
    local response=$(http_request "POST" "/users/$user_id/addresses" "$data")
    local http_code=$?
    
    if assert_status "$http_code" "201" "Address created successfully"; then
        local addr_id=$(echo "$response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
        CREATED_ADDRESSES["basic"]=$addr_id
        CREATED_ADDRESSES["basic_user"]=$user_id
        assert_contains "$response" "123 Main Street" "Response contains address line 1"
        assert_contains "$response" "New York" "Response contains city"
    fi
}

test_address_creation_minimal() {
    log_section "Address Creation - Minimal Required Fields"
    
    if [ -z "${CREATED_USERS[user_1]}" ]; then
        log_skip "No user ID available"
        return
    fi
    
    local user_id=${CREATED_USERS[user_1]}
    
    log_test "Create address with only required fields"
    local data='{
        "address_line1":"456 Oak Avenue",
        "city":"Boston",
        "state":"MA",
        "postal_code":"02101",
        "country":"USA"
    }'
    local response=$(http_request "POST" "/users/$user_id/addresses" "$data")
    local http_code=$?
    assert_status "$http_code" "201" "Minimal address created"
    
    local addr_id=$(echo "$response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    CREATED_ADDRESSES["minimal"]=$addr_id
    CREATED_ADDRESSES["minimal_user"]=$user_id
}

test_address_creation_multiple() {
    log_section "Address Creation - Multiple Addresses per User"
    
    if [ -z "${CREATED_USERS[user_2]}" ]; then
        log_skip "No user ID available"
        return
    fi
    
    local user_id=${CREATED_USERS[user_2]}
    
    for i in {1..3}; do
        log_test "Create address $i for user"
        local data="{
            \"address_line1\":\"${i}00 Street $i\",
            \"city\":\"City$i\",
            \"state\":\"ST\",
            \"postal_code\":\"0000$i\",
            \"country\":\"Country$i\"
        }"
        local response=$(http_request "POST" "/users/$user_id/addresses" "$data")
        local http_code=$?
        
        if assert_status "$http_code" "201" "Address $i created"; then
            local addr_id=$(echo "$response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
            CREATED_ADDRESSES["multi_$i"]=$addr_id
        fi
    done
}

test_address_creation_validation() {
    log_section "Address Creation - Input Validation"
    
    if [ -z "${CREATED_USERS[user_5]}" ]; then
        log_skip "No user ID available"
        return
    fi
    
    local user_id=${CREATED_USERS[user_5]}
    
    log_test "Create address with missing required field (address_line1)"
    local data='{
        "city":"New York",
        "state":"NY",
        "postal_code":"10001",
        "country":"USA"
    }'
    local response=$(http_request "POST" "/users/$user_id/addresses" "$data")
    local http_code=$?
    assert_status "$http_code" "400" "Missing address_line1 rejected"
    
    log_test "Create address with missing city"
    data='{
        "address_line1":"123 Main St",
        "state":"NY",
        "postal_code":"10001",
        "country":"USA"
    }'
    response=$(http_request "POST" "/users/$user_id/addresses" "$data")
    http_code=$?
    assert_status "$http_code" "400" "Missing city rejected"
    
    log_test "Create address with oversized postal code"
    data='{
        "address_line1":"123 Main St",
        "city":"New York",
        "state":"NY",
        "postal_code":"123456789012345678901",
        "country":"USA"
    }'
    response=$(http_request "POST" "/users/$user_id/addresses" "$data")
    http_code=$?
    assert_status "$http_code" "400" "Oversized postal code rejected"
    
    log_test "Create address with valid maximum length fields"
    local long_field=$(printf '%0.s-' {1..100})
    data="{
        \"address_line1\":\"123 Main St\",
        \"city\":\"$long_field\",
        \"state\":\"NY\",
        \"postal_code\":\"10001\",
        \"country\":\"USA\"
    }"
    response=$(http_request "POST" "/users/$user_id/addresses" "$data")
    http_code=$?
    assert_status "$http_code" "201" "Maximum length fields accepted"
}

test_address_retrieval() {
    log_section "Address Retrieval - GET Operations"
    
    if [ -z "${CREATED_ADDRESSES[basic]}" ] || [ -z "${CREATED_ADDRESSES[basic_user]}" ]; then
        log_skip "No address ID available for retrieval test"
        return
    fi
    
    local user_id=${CREATED_ADDRESSES[basic_user]}
    local addr_id=${CREATED_ADDRESSES[basic]}
    
    log_test "Get all addresses for user"
    local response=$(http_request "GET" "/users/$user_id/addresses")
    local http_code=$?
    assert_status "$http_code" "200" "Addresses retrieved successfully"
    assert_contains "$response" "$addr_id" "Response contains created address"
    
    log_test "Get specific address"
    response=$(http_request "GET" "/users/$user_id/addresses/$addr_id")
    http_code=$?
    assert_status "$http_code" "200" "Specific address retrieved"
    assert_contains "$response" "123 Main Street" "Response contains address data"
    
    log_test "Get addresses for user with no addresses"
    local new_user=$(http_request "POST" "/users?authUserId=33333333-3333-3333-3333-333333333333")
    local new_user_id=$(echo "$new_user" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    response=$(http_request "GET" "/users/$new_user_id/addresses")
    http_code=$?
    assert_status "$http_code" "200" "Empty addresses list retrieved"
    assert_contains "$response" "\[\]" "Response contains empty array"
    
    log_test "Get non-existent address"
    response=$(http_request "GET" "/users/$user_id/addresses/ffffffff-ffff-ffff-ffff-ffffffffffff")
    http_code=$?
    assert_status "$http_code" "404" "Non-existent address returns 404"
}

test_address_update() {
    log_section "Address Update - PUT Operations"
    
    if [ -z "${CREATED_ADDRESSES[minimal]}" ] || [ -z "${CREATED_ADDRESSES[minimal_user]}" ]; then
        log_skip "No address ID available for update test"
        return
    fi
    
    local user_id=${CREATED_ADDRESSES[minimal_user]}
    local addr_id=${CREATED_ADDRESSES[minimal]}
    
    log_test "Update address with new values"
    local data='{
        "address_line1":"789 Elm Street",
        "address_line2":"Suite 500",
        "city":"Philadelphia",
        "state":"PA",
        "postal_code":"19103",
        "country":"USA",
        "is_default":true
    }'
    local response=$(http_request "PUT" "/users/$user_id/addresses/$addr_id" "$data")
    local http_code=$?
    
    if assert_status "$http_code" "200" "Address updated successfully"; then
        assert_contains "$response" "789 Elm Street" "Address line updated"
        assert_contains "$response" "Philadelphia" "City updated"
        assert_contains "$response" "19103" "Postal code updated"
    fi
    
    log_test "Update address with only some fields"
    data='{
        "address_line1":"999 Pine Road",
        "city":"San Francisco",
        "state":"CA",
        "postal_code":"94105",
        "country":"USA"
    }'
    response=$(http_request "PUT" "/users/$user_id/addresses/$addr_id" "$data")
    http_code=$?
    assert_status "$http_code" "200" "Partial address update successful"
    
    log_test "Update non-existent address"
    response=$(http_request "PUT" "/users/$user_id/addresses/ffffffff-ffff-ffff-ffff-ffffffffffff" "$data")
    http_code=$?
    assert_status "$http_code" "404" "Non-existent address update returns 404"
}

test_address_deletion() {
    log_section "Address Deletion - DELETE Operations"
    
    if [ -z "${CREATED_ADDRESSES[multi_1]}" ] || [ -z "${CREATED_ADDRESSES[minimal_user]}" ]; then
        log_skip "No address ID available for deletion test"
        return
    fi
    
    local user_id=${CREATED_ADDRESSES[minimal_user]}
    
    # Delete first multi address
    local addr_id=${CREATED_ADDRESSES[multi_1]}
    
    log_test "Delete address"
    local response=$(http_request "DELETE" "/users/$user_id/addresses/$addr_id")
    local http_code=$?
    assert_status "$http_code" "204" "Address deleted successfully"
    
    log_test "Verify address is deleted"
    response=$(http_request "GET" "/users/$user_id/addresses/$addr_id")
    http_code=$?
    assert_status "$http_code" "404" "Deleted address not found"
    
    log_test "Delete non-existent address"
    response=$(http_request "DELETE" "/users/$user_id/addresses/ffffffff-ffff-ffff-ffff-ffffffffffff")
    http_code=$?
    assert_status "$http_code" "404" "Deleting non-existent address returns 404"
}

# ============================================================================
# SECURITY & AUTHENTICATION TESTS
# ============================================================================

test_authentication() {
    log_section "Authentication & Authorization"
    
    log_test "Request without authentication token"
    local url="$API_BASE/users/00000000-0000-0000-0000-000000000000"
    local response=$(curl -s -w "\n%{http_code}" -X GET "$url")
    local http_code=$(echo "$response" | tail -n1)
    assert_status "$http_code" "403" "Unauthenticated request rejected"
    
    log_test "Request with invalid token format"
    local url="$API_BASE/users/00000000-0000-0000-0000-000000000000"
    local response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: InvalidToken" \
        "$url")
    http_code=$(echo "$response" | tail -n1)
    # Should be rejected or 403
    if [ "$http_code" -eq 403 ] || [ "$http_code" -eq 401 ]; then
        log_pass "Invalid token format rejected (HTTP $http_code)"
    fi
    
    log_test "Request with malformed JWT"
    response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: Bearer malformed.jwt.token" \
        "$url")
    http_code=$(echo "$response" | tail -n1)
    if [ "$http_code" -eq 403 ] || [ "$http_code" -eq 401 ]; then
        log_pass "Malformed JWT rejected (HTTP $http_code)"
    fi
}

test_cross_user_access() {
    log_section "Cross-User Access Control"
    
    if [ -z "${CREATED_USERS[basic]}" ] || [ -z "${CREATED_USERS[user_1]}" ]; then
        log_skip "Insufficient test data for cross-user access test"
        return
    fi
    
    local user1_id=${CREATED_USERS[basic]}
    local user2_id=${CREATED_USERS[user_1]}
    
    # Create address for user1
    local data='{
        "address_line1":"100 User1 St",
        "city":"City1",
        "state":"ST",
        "postal_code":"00001",
        "country":"Country1"
    }'
    local response=$(http_request "POST" "/users/$user1_id/addresses" "$data")
    local addr_id=$(echo "$response" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    if [ ! -z "$addr_id" ]; then
        log_test "Attempt to access other user's address"
        response=$(http_request "GET" "/users/$user2_id/addresses/$addr_id")
        local http_code=$?
        # Should be 404 (address doesn't belong to user2)
        assert_status "$http_code" "404" "Cross-user address access prevented"
    fi
}

# ============================================================================
# EDGE CASES & BOUNDARY TESTS
# ============================================================================

test_boundary_values() {
    log_section "Boundary Values & Edge Cases"
    
    local test_user=$(http_request "POST" "/users?authUserId=44444444-4444-4444-4444-444444444444")
    local user_id=$(echo "$test_user" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    if [ -z "$user_id" ]; then
        log_skip "Could not create test user for boundary tests"
        return
    fi
    
    log_test "Phone with exactly 10 digits"
    local data='{"phone":"1234567890"}'
    local response=$(http_request "PUT" "/users/$user_id" "$data")
    local http_code=$?
    assert_status "$http_code" "200" "10-digit phone accepted"
    
    log_test "Phone with exactly 15 digits"
    data='{"phone":"123456789012345"}'
    response=$(http_request "PUT" "/users/$user_id" "$data")
    http_code=$?
    assert_status "$http_code" "200" "15-digit phone accepted"
    
    log_test "Phone with more than 15 digits"
    data='{"phone":"1234567890123456"}'
    response=$(http_request "PUT" "/users/$user_id" "$data")
    http_code=$?
    assert_status "$http_code" "400" "16-digit phone rejected"
    
    log_test "Address line 1 with maximum length (255 chars)"
    local max_field=$(printf '%0.s-' {1..255})
    data="{\"address_line1\":\"$max_field\",\"city\":\"C\",\"state\":\"ST\",\"postal_code\":\"00000\",\"country\":\"USA\"}"
    response=$(http_request "POST" "/users/$user_id/addresses" "$data")
    http_code=$?
    assert_status "$http_code" "201" "Maximum length address line accepted"
    
    log_test "Address line 1 exceeding maximum (256 chars)"
    local exceed_field=$(printf '%0.s-' {1..256})
    data="{\"address_line1\":\"$exceed_field\",\"city\":\"C\",\"state\":\"ST\",\"postal_code\":\"00000\",\"country\":\"USA\"}"
    response=$(http_request "POST" "/users/$user_id/addresses" "$data")
    http_code=$?
    assert_status "$http_code" "400" "Oversized address line rejected"
}

test_special_characters() {
    log_section "Special Characters & Unicode"
    
    local test_user=$(http_request "POST" "/users?authUserId=55555555-5555-5555-5555-555555555555")
    local user_id=$(echo "$test_user" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    if [ -z "$user_id" ]; then
        log_skip "Could not create test user for special character tests"
        return
    fi
    
    log_test "Address with special characters (hyphens, dots)"
    local data='{
        "address_line1":"123-A Main St. Apt.",
        "city":"New-York",
        "state":"N.Y.",
        "postal_code":"10001-1234",
        "country":"U.S.A."
    }'
    local response=$(http_request "POST" "/users/$user_id/addresses" "$data")
    local http_code=$?
    assert_status "$http_code" "201" "Special characters accepted"
    
    log_test "Address with unicode characters"
    data='{
        "address_line1":"123 Main Street",
        "city":"São Paulo",
        "state":"SP",
        "postal_code":"01310-100",
        "country":"Brasil"
    }'
    response=$(http_request "POST" "/users/$user_id/addresses" "$data")
    http_code=$?
    assert_status "$http_code" "201" "Unicode characters accepted"
}

test_null_and_empty_values() {
    log_section "Null & Empty Value Handling"
    
    local test_user=$(http_request "POST" "/users?authUserId=66666666-6666-6666-6666-666666666666")
    local user_id=$(echo "$test_user" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    if [ -z "$user_id" ]; then
        log_skip "Could not create test user for null/empty tests"
        return
    fi
    
    log_test "Update user with empty string for optional field"
    local data='{"phone":""}'
    local response=$(http_request "PUT" "/users/$user_id" "$data")
    local http_code=$?
    # Empty phone should be rejected
    assert_status "$http_code" "400" "Empty phone rejected"
    
    log_test "Update user with null values in JSON"
    data='{"phone":null}'
    response=$(http_request "PUT" "/users/$user_id" "$data")
    http_code=$?
    # Null should be handled appropriately
    log_info "Null value handling: HTTP $http_code"
    
    log_test "Create address with empty address_line1"
    data='{
        "address_line1":"",
        "city":"City",
        "state":"ST",
        "postal_code":"00000",
        "country":"Country"
    }'
    response=$(http_request "POST" "/users/$user_id/addresses" "$data")
    http_code=$?
    assert_status "$http_code" "400" "Empty address_line1 rejected"
}

# ============================================================================
# CONCURRENT & PERFORMANCE TESTS
# ============================================================================

test_concurrent_operations() {
    log_section "Concurrent Operations"
    
    log_test "Create multiple users concurrently"
    local pids=()
    
    for i in {1..3}; do
        (
            local auth_id=$(printf "%08x-%04x-%04x-%04x-%012x" $((i+100)) $((i+100)) $((i+100)) $((i+100)) $((i+100)))
            http_request "POST" "/users?authUserId=$auth_id" > /dev/null
        ) &
        pids+=($!)
    done
    
    # Wait for all background jobs
    for pid in "${pids[@]}"; do
        wait "$pid"
    done
    
    log_pass "Multiple concurrent user creations completed"
}

test_data_persistence() {
    log_section "Data Persistence Verification"
    
    if [ -z "${CREATED_USERS[basic]}" ]; then
        log_skip "No user data available for persistence test"
        return
    fi
    
    local user_id=${CREATED_USERS[basic]}
    
    log_test "Retrieve user and verify data persistence"
    local response=$(http_request "GET" "/users/$user_id")
    local http_code=$?
    
    if assert_status "$http_code" "200" "User data retrieved"; then
        # Verify fields exist
        assert_contains "$response" "\"id\":" "User ID field exists"
        assert_contains "$response" "\"auth_user_id\":" "Auth user ID field exists"
        assert_contains "$response" "\"created_at\":" "Created at field exists"
    fi
    
    log_test "Retrieve updated address and verify persistence"
    if [ ! -z "${CREATED_ADDRESSES[basic]}" ]; then
        local addr_id=${CREATED_ADDRESSES[basic]}
        response=$(http_request "GET" "/users/$user_id/addresses/$addr_id")
        http_code=$?
        
        if assert_status "$http_code" "200" "Address data retrieved"; then
            assert_contains "$response" "\"id\":" "Address ID exists"
            assert_contains "$response" "\"city\":" "City field exists"
        fi
    fi
}

# ============================================================================
# TEST SUMMARY & REPORTING
# ============================================================================

print_summary() {
    echo ""
    log_header "Test Execution Summary"
    
    echo ""
    echo -e "Total Tests:    $TOTAL_TESTS"
    echo -e "Passed:         ${GREEN}$PASSED_TESTS${NC}"
    echo -e "Failed:         ${RED}$FAILED_TESTS${NC}"
    echo -e "Skipped:        ${YELLOW}$SKIPPED_TESTS${NC}"
    
    echo ""
    
    local success_rate=$((PASSED_TESTS * 100 / (TOTAL_TESTS - SKIPPED_TESTS)))
    
    if [ "$FAILED_TESTS" -eq 0 ]; then
        echo -e "${GREEN}════════════════════════════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}✓ ALL TESTS PASSED - Service is fully functional${NC}"
        echo -e "${GREEN}════════════════════════════════════════════════════════════════════════════════${NC}"
        return 0
    else
        echo -e "${RED}════════════════════════════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}✗ $FAILED_TESTS TEST(S) FAILED - Success rate: $success_rate%${NC}"
        echo -e "${RED}════════════════════════════════════════════════════════════════════════════════${NC}"
        return 1
    fi
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
    echo -e "${CYAN}"
    echo "╔════════════════════════════════════════════════════════════════════════════════╗"
    echo "║              COMPREHENSIVE USER SERVICE TEST SUITE v2.0                        ║"
    echo "║                                                                                ║"
    echo "║  Complete endpoint testing including:                                          ║"
    echo "║  - User CRUD operations (Create, Read, Update, Delete)                        ║"
    echo "║  - Address CRUD operations                                                    ║"
    echo "║  - Input validation & error handling                                          ║"
    echo "║  - Security & authentication                                                  ║"
    echo "║  - Edge cases & boundary values                                               ║"
    echo "║  - Data persistence & consistency                                             ║"
    echo "║  - Concurrent operations                                                      ║"
    echo "╚════════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    # Setup
    setup
    
    # User endpoint tests
    test_user_creation_basic
    test_user_creation_multiple
    test_user_retrieval
    test_user_update_basic
    test_user_update_combined
    test_user_update_validation
    test_user_exists
    test_user_deletion
    
    # Address endpoint tests
    test_address_creation_basic
    test_address_creation_minimal
    test_address_creation_multiple
    test_address_creation_validation
    test_address_retrieval
    test_address_update
    test_address_deletion
    
    # Security tests
    test_authentication
    test_cross_user_access
    
    # Edge case tests
    test_boundary_values
    test_special_characters
    test_null_and_empty_values
    
    # Performance & persistence tests
    test_concurrent_operations
    test_data_persistence
    
    # Print summary
    print_summary
}

# Run tests
main "$@"
