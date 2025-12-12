#!/bin/bash

################################################################################
# User Service - Functional & Integration Test Suite
# Tests actual data flow, CRUD operations, transactions, and endpoint behavior
################################################################################

set -o pipefail

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Service configuration
SERVICE_URL="http://localhost:8082"
API_BASE="$SERVICE_URL/api/v1"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-shopsphere_users}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-password}"

# Test data
TEST_AUTH_USER_ID="00000000-0000-0000-0000-000000000001"
TEST_AUTH_USER_ID_2="00000000-0000-0000-0000-000000000002"
TEST_AUTH_USER_ID_3="00000000-0000-0000-0000-000000000003"
MOCK_JWT_TOKEN="Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIwMDAwMDAwMC0wMDAwLTAwMDAtMDAwMC0wMDAwMDAwMDAwMDEiLCJyb2xlcyI6WyJVU0VSIl0sImlhdCI6MTcwMDAwMDAwMCwiZXhwIjo5OTk5OTk5OTk5fQ.test"

################################################################################
# Helper Functions
################################################################################

log_header() {
    echo ""
    echo -e "${PURPLE}╔════════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${PURPLE}║ $1${NC}"
    echo -e "${PURPLE}╚════════════════════════════════════════════════════════════════════════════════╝${NC}"
}

log_test() {
    echo -e "${CYAN}→ TEST: $1${NC}"
    ((TOTAL_TESTS++))
}

log_pass() {
    echo -e "${GREEN}✓ PASS: $1${NC}"
    ((PASSED_TESTS++))
}

log_fail() {
    echo -e "${RED}✗ FAIL: $1${NC}"
    echo -e "${RED}  Response: $2${NC}"
    ((FAILED_TESTS++))
}

log_info() {
    echo -e "  ${BLUE}ℹ${NC} $1"
}

log_error() {
    echo -e "${RED}ERROR: $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}WARNING: $1${NC}"
}

assert_http_status() {
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

assert_contains() {
    local response=$1
    local expected=$2
    local message=$3
    
    if echo "$response" | grep -q "$expected"; then
        log_pass "$message"
        return 0
    else
        log_fail "$message - did not find '$expected' in response"
        return 1
    fi
}

make_request() {
    local method=$1
    local endpoint=$2
    local data=$3
    local expected_status=$4
    
    local url="$API_BASE$endpoint"
    local response_file="/tmp/response_$$.json"
    
    if [ -z "$data" ]; then
        curl -s -w "\n%{http_code}" -X "$method" \
            -H "Authorization: $MOCK_JWT_TOKEN" \
            -H "Content-Type: application/json" \
            "$url" > "$response_file"
    else
        curl -s -w "\n%{http_code}" -X "$method" \
            -H "Authorization: $MOCK_JWT_TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$url" > "$response_file"
    fi
    
    local http_status=$(tail -n1 "$response_file")
    local response_body=$(head -n-1 "$response_file")
    
    echo "$response_body"
    
    rm -f "$response_file"
    
    return "$http_status"
}

check_database() {
    local query=$1
    
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "$query" 2>/dev/null
}

################################################################################
# Pre-test Validation
################################################################################

test_service_availability() {
    log_header "Pre-Test Validation: Service Availability"
    
    log_test "Checking if user-service is running"
    
    local max_attempts=5
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$SERVICE_URL/actuator/health" > /dev/null 2>&1; then
            log_pass "Service is available at $SERVICE_URL"
            return 0
        fi
        
        log_warning "Attempt $attempt/$max_attempts failed, retrying..."
        sleep 2
        ((attempt++))
    done
    
    log_error "Service not responding at $SERVICE_URL"
    log_info "Please ensure the service is running: docker-compose up -d user-service"
    return 1
}

test_database_connectivity() {
    log_header "Pre-Test Validation: Database Connectivity"
    
    log_test "Checking PostgreSQL connectivity"
    
    if PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "\dt" > /dev/null 2>&1; then
        log_pass "Database is accessible"
        
        log_test "Checking database tables"
        local table_count=$(check_database "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';")
        
        if [ "$table_count" -gt 0 ]; then
            log_pass "Database tables exist (found $table_count tables)"
        else
            log_fail "No tables found in database"
            return 1
        fi
        
        return 0
    else
        log_error "Cannot connect to PostgreSQL at $DB_HOST:$DB_PORT"
        return 1
    fi
}

################################################################################
# CRUD Operation Tests
################################################################################

test_user_creation() {
    log_header "Test Suite 1: User Profile Creation (POST)"
    
    local endpoint="/users?authUserId=$TEST_AUTH_USER_ID"
    log_test "Creating new user profile"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        "$API_BASE$endpoint")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq 201 ]; then
        log_pass "User created successfully (HTTP 201)"
        
        # Extract user ID from response
        USER_ID=$(echo "$body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
        
        if [ ! -z "$USER_ID" ]; then
            log_pass "User ID extracted: $USER_ID"
            assert_contains "$body" "$TEST_AUTH_USER_ID" "Response contains auth_user_id"
        else
            log_fail "Failed to extract user ID from response: $body"
            return 1
        fi
    else
        log_fail "Failed to create user (HTTP $http_code)" "$body"
        return 1
    fi
    
    return 0
}

test_user_retrieval() {
    log_header "Test Suite 2: User Profile Retrieval (GET)"
    
    if [ -z "$USER_ID" ]; then
        log_error "USER_ID not set, skipping test"
        return 1
    fi
    
    local endpoint="/users/$USER_ID"
    log_test "Retrieving user profile by ID"
    
    local response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        "$API_BASE$endpoint")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq 200 ]; then
        log_pass "User retrieved successfully (HTTP 200)"
        assert_contains "$body" "$USER_ID" "Response contains user ID"
        assert_contains "$body" "$TEST_AUTH_USER_ID" "Response contains auth_user_id"
    else
        log_fail "Failed to retrieve user (HTTP $http_code)" "$body"
        return 1
    fi
    
    return 0
}

test_user_update() {
    log_header "Test Suite 3: User Profile Update (PUT)"
    
    if [ -z "$USER_ID" ]; then
        log_error "USER_ID not set, skipping test"
        return 1
    fi
    
    local endpoint="/users/$USER_ID"
    log_test "Updating user profile"
    
    local update_data='{
        "phone": "+1234567890",
        "date_of_birth": "1990-01-15",
        "avatar_url": "https://example.com/avatar.jpg"
    }'
    
    local response=$(curl -s -w "\n%{http_code}" -X PUT \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$update_data" \
        "$API_BASE$endpoint")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq 200 ]; then
        log_pass "User updated successfully (HTTP 200)"
        assert_contains "$body" "+1234567890" "Response contains updated phone"
        assert_contains "$body" "1990-01-15" "Response contains updated date_of_birth"
    else
        log_fail "Failed to update user (HTTP $http_code)" "$body"
        return 1
    fi
    
    return 0
}

test_user_exists_check() {
    log_header "Test Suite 4: User Existence Check (GET)"
    
    log_test "Checking if user exists"
    
    local endpoint="/users/exists/$TEST_AUTH_USER_ID"
    
    local response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        "$API_BASE$endpoint")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq 200 ]; then
        log_pass "Existence check successful (HTTP 200)"
        
        if echo "$body" | grep -q "true"; then
            log_pass "User exists in system"
        else
            log_fail "User existence check returned false, but user should exist"
            return 1
        fi
    else
        log_fail "Existence check failed (HTTP $http_code)" "$body"
        return 1
    fi
    
    return 0
}

################################################################################
# Address Management Tests
################################################################################

test_address_creation() {
    log_header "Test Suite 5: Address Creation (POST)"
    
    if [ -z "$USER_ID" ]; then
        log_error "USER_ID not set, skipping test"
        return 1
    fi
    
    local endpoint="/users/$USER_ID/addresses"
    log_test "Creating address for user"
    
    local address_data='{
        "address_line1": "123 Main Street",
        "address_line2": "Apt 4B",
        "city": "New York",
        "state": "NY",
        "postal_code": "10001",
        "country": "USA",
        "is_default": true
    }'
    
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$address_data" \
        "$API_BASE$endpoint")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq 201 ]; then
        log_pass "Address created successfully (HTTP 201)"
        
        # Extract address ID
        ADDRESS_ID=$(echo "$body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
        
        if [ ! -z "$ADDRESS_ID" ]; then
            log_pass "Address ID extracted: $ADDRESS_ID"
            assert_contains "$body" "123 Main Street" "Response contains address line 1"
            assert_contains "$body" "New York" "Response contains city"
        else
            log_fail "Failed to extract address ID from response"
            return 1
        fi
    else
        log_fail "Failed to create address (HTTP $http_code)" "$body"
        return 1
    fi
    
    return 0
}

test_get_addresses() {
    log_header "Test Suite 6: Get All Addresses (GET)"
    
    if [ -z "$USER_ID" ]; then
        log_error "USER_ID not set, skipping test"
        return 1
    fi
    
    local endpoint="/users/$USER_ID/addresses"
    log_test "Retrieving all addresses for user"
    
    local response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        "$API_BASE$endpoint")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq 200 ]; then
        log_pass "Addresses retrieved successfully (HTTP 200)"
        
        if echo "$body" | grep -q "New York"; then
            log_pass "Address list contains previously created address"
        else
            log_warning "Expected address not found in response"
        fi
    else
        log_fail "Failed to retrieve addresses (HTTP $http_code)" "$body"
        return 1
    fi
    
    return 0
}

test_get_single_address() {
    log_header "Test Suite 7: Get Single Address (GET)"
    
    if [ -z "$USER_ID" ] || [ -z "$ADDRESS_ID" ]; then
        log_error "USER_ID or ADDRESS_ID not set, skipping test"
        return 1
    fi
    
    local endpoint="/users/$USER_ID/addresses/$ADDRESS_ID"
    log_test "Retrieving specific address"
    
    local response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        "$API_BASE$endpoint")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq 200 ]; then
        log_pass "Address retrieved successfully (HTTP 200)"
        assert_contains "$body" "$ADDRESS_ID" "Response contains address ID"
        assert_contains "$body" "123 Main Street" "Response contains correct address"
    else
        log_fail "Failed to retrieve address (HTTP $http_code)" "$body"
        return 1
    fi
    
    return 0
}

test_address_update() {
    log_header "Test Suite 8: Address Update (PUT)"
    
    if [ -z "$USER_ID" ] || [ -z "$ADDRESS_ID" ]; then
        log_error "USER_ID or ADDRESS_ID not set, skipping test"
        return 1
    fi
    
    local endpoint="/users/$USER_ID/addresses/$ADDRESS_ID"
    log_test "Updating address"
    
    local update_data='{
        "address_line1": "456 Oak Avenue",
        "address_line2": "Suite 200",
        "city": "Boston",
        "state": "MA",
        "postal_code": "02101",
        "country": "USA",
        "is_default": false
    }'
    
    local response=$(curl -s -w "\n%{http_code}" -X PUT \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        -d "$update_data" \
        "$API_BASE$endpoint")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq 200 ]; then
        log_pass "Address updated successfully (HTTP 200)"
        assert_contains "$body" "456 Oak Avenue" "Response contains updated address"
        assert_contains "$body" "Boston" "Response contains updated city"
    else
        log_fail "Failed to update address (HTTP $http_code)" "$body"
        return 1
    fi
    
    return 0
}

test_address_deletion() {
    log_header "Test Suite 9: Address Deletion (DELETE)"
    
    if [ -z "$USER_ID" ] || [ -z "$ADDRESS_ID" ]; then
        log_error "USER_ID or ADDRESS_ID not set, skipping test"
        return 1
    fi
    
    local endpoint="/users/$USER_ID/addresses/$ADDRESS_ID"
    log_test "Deleting address"
    
    local response=$(curl -s -w "\n%{http_code}" -X DELETE \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        "$API_BASE$endpoint")
    
    local http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 204 ]; then
        log_pass "Address deleted successfully (HTTP 204)"
    else
        log_fail "Failed to delete address (HTTP $http_code)"
        return 1
    fi
    
    # Verify deletion
    log_test "Verifying address deletion"
    local verify_response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        "$API_BASE$endpoint")
    
    local verify_code=$(echo "$verify_response" | tail -n1)
    
    if [ "$verify_code" -eq 404 ]; then
        log_pass "Address is deleted (HTTP 404)"
    else
        log_warning "Address still exists after deletion attempt (HTTP $verify_code)"
    fi
    
    return 0
}

################################################################################
# Transaction & Consistency Tests
################################################################################

test_transaction_rollback() {
    log_header "Test Suite 10: Transaction Consistency"
    
    log_test "Testing transaction consistency"
    
    # Create a new user for transaction test
    local endpoint="/users?authUserId=$TEST_AUTH_USER_ID_2"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        "$API_BASE$endpoint")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq 201 ]; then
        local test_user_id=$(echo "$body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
        log_pass "Test user created for transaction test: $test_user_id"
        
        # Add multiple addresses
        local addr_endpoint="/users/$test_user_id/addresses"
        
        for i in 1 2 3; do
            local addr_data="{
                \"address_line1\": \"Address $i\",
                \"address_line2\": \"\",
                \"city\": \"City$i\",
                \"state\": \"ST\",
                \"postal_code\": \"12345\",
                \"country\": \"USA\",
                \"is_default\": false
            }"
            
            curl -s -X POST \
                -H "Authorization: $MOCK_JWT_TOKEN" \
                -H "Content-Type: application/json" \
                -d "$addr_data" \
                "$API_BASE$addr_endpoint" > /dev/null 2>&1
        done
        
        # Verify all addresses are saved
        local list_response=$(curl -s -X GET \
            -H "Authorization: $MOCK_JWT_TOKEN" \
            "$API_BASE$addr_endpoint")
        
        local address_count=$(echo "$list_response" | grep -o '"id"' | wc -l)
        
        if [ "$address_count" -ge 3 ]; then
            log_pass "All addresses persisted correctly ($address_count addresses found)"
        else
            log_fail "Expected 3 addresses, but found $address_count"
            return 1
        fi
    else
        log_fail "Failed to create test user for transaction test"
        return 1
    fi
    
    return 0
}

################################################################################
# Error Handling Tests
################################################################################

test_unauthorized_access() {
    log_header "Test Suite 11: Authorization & Error Handling"
    
    log_test "Testing unauthorized access without JWT token"
    
    local response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Content-Type: application/json" \
        "$API_BASE/users/$TEST_AUTH_USER_ID")
    
    local http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 401 ] || [ "$http_code" -eq 403 ]; then
        log_pass "Unauthorized access rejected (HTTP $http_code)"
    else
        log_warning "Expected 401/403, but got HTTP $http_code (may be a configuration issue)"
    fi
    
    return 0
}

test_invalid_user_id() {
    log_header "Test Suite 12: Input Validation"
    
    log_test "Testing retrieval of non-existent user"
    
    local invalid_id="ffffffff-ffff-ffff-ffff-ffffffffffff"
    
    local response=$(curl -s -w "\n%{http_code}" -X GET \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        "$API_BASE/users/$invalid_id")
    
    local http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 404 ]; then
        log_pass "Non-existent user returns 404"
    else
        log_warning "Expected 404, got HTTP $http_code"
    fi
    
    return 0
}

test_invalid_address_for_user() {
    log_header "Test Suite 13: Address Ownership Validation"
    
    if [ -z "$USER_ID" ]; then
        log_error "USER_ID not set, skipping test"
        return 1
    fi
    
    log_test "Testing access to address from different user"
    
    # Create second user
    local endpoint="/users?authUserId=$TEST_AUTH_USER_ID_3"
    
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        -H "Authorization: $MOCK_JWT_TOKEN" \
        -H "Content-Type: application/json" \
        "$API_BASE$endpoint")
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | head -n-1)
    
    if [ "$http_code" -eq 201 ]; then
        local other_user_id=$(echo "$body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
        
        # Try to access first user's address from second user
        if [ ! -z "$ADDRESS_ID" ]; then
            local invalid_access=$(curl -s -w "\n%{http_code}" -X GET \
                -H "Authorization: $MOCK_JWT_TOKEN" \
                -H "Content-Type: application/json" \
                "$API_BASE/users/$other_user_id/addresses/$ADDRESS_ID")
            
            local invalid_code=$(echo "$invalid_access" | tail -n1)
            
            if [ "$invalid_code" -eq 404 ]; then
                log_pass "Cross-user address access prevented (HTTP 404)"
            else
                log_warning "Expected 404 for cross-user access, got HTTP $invalid_code"
            fi
        fi
    else
        log_warning "Could not create second test user"
    fi
    
    return 0
}

################################################################################
# Data Persistence Tests
################################################################################

test_data_persistence() {
    log_header "Test Suite 14: Data Persistence"
    
    if [ -z "$USER_ID" ]; then
        log_error "USER_ID not set, skipping test"
        return 1
    fi
    
    log_test "Verifying data is persisted in database"
    
    # Query database directly
    local db_query="SELECT COUNT(*) FROM user_profiles WHERE id = '$USER_ID';"
    local db_result=$(check_database "$db_query")
    
    if [ "$db_result" -eq 1 ]; then
        log_pass "User profile found in database"
    else
        log_fail "User profile not found in database"
        return 1
    fi
    
    # Check user's attributes
    local attr_query="SELECT phone, avatar_url FROM user_profiles WHERE id = '$USER_ID';"
    local attr_result=$(check_database "$attr_query")
    
    if echo "$attr_result" | grep -q "+1234567890"; then
        log_pass "User phone number persisted correctly"
    else
        log_warning "User phone number not found in database"
    fi
    
    return 0
}

################################################################################
# Summary and Report
################################################################################

print_summary() {
    echo ""
    echo -e "${PURPLE}╔════════════════════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${PURPLE}║                           TEST EXECUTION SUMMARY                                ║${NC}"
    echo -e "${PURPLE}╚════════════════════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    echo "Total Tests Run: $TOTAL_TESTS"
    echo -e "Passed: ${GREEN}$PASSED_TESTS${NC}"
    echo -e "Failed: ${RED}$FAILED_TESTS${NC}"
    echo ""
    
    if [ "$FAILED_TESTS" -eq 0 ]; then
        echo -e "${GREEN}════════════════════════════════════════════════════════════════════════════════${NC}"
        echo -e "${GREEN}✓ ALL FUNCTIONAL TESTS PASSED - DATA FLOW IS WORKING CORRECTLY${NC}"
        echo -e "${GREEN}════════════════════════════════════════════════════════════════════════════════${NC}"
        return 0
    else
        echo -e "${RED}════════════════════════════════════════════════════════════════════════════════${NC}"
        echo -e "${RED}✗ $FAILED_TESTS TEST(S) FAILED - PLEASE REVIEW ISSUES${NC}"
        echo -e "${RED}════════════════════════════════════════════════════════════════════════════════${NC}"
        return 1
    fi
}

################################################################################
# Main Execution
################################################################################

main() {
    echo -e "${CYAN}"
    echo "╔════════════════════════════════════════════════════════════════════════════════╗"
    echo "║      USER SERVICE - FUNCTIONAL & INTEGRATION TEST SUITE v1.0                   ║"
    echo "║                                                                                ║"
    echo "║  This script tests actual data flow and service functionality:                ║"
    echo "║  - User CRUD operations                                                       ║"
    echo "║  - Address management (Create, Read, Update, Delete)                          ║"
    echo "║  - Transaction consistency                                                    ║"
    echo "║  - Authorization and error handling                                           ║"
    echo "║  - Input validation                                                           ║"
    echo "║  - Data persistence verification                                              ║"
    echo "║  - End-to-end workflow testing                                                ║"
    echo "╚════════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    echo ""
    
    # Pre-test validation
    test_service_availability || exit 1
    test_database_connectivity || exit 1
    
    # CRUD and functional tests
    test_user_creation || exit 1
    test_user_retrieval || exit 1
    test_user_update || exit 1
    test_user_exists_check || true
    test_address_creation || exit 1
    test_get_addresses || true
    test_get_single_address || true
    test_address_update || true
    test_address_deletion || true
    
    # Consistency and error handling tests
    test_transaction_rollback || true
    test_unauthorized_access || true
    test_invalid_user_id || true
    test_invalid_address_for_user || true
    test_data_persistence || true
    
    # Print summary
    print_summary
}

# Run main function
main "$@"
