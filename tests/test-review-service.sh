#!/bin/bash

# Review Service Comprehensive Test Script
# Tests all endpoints and functionality

set -e

BASE_URL="http://localhost:8089"
RESULTS_FILE="review-service-test-results.txt"
PASSED=0
FAILED=0
TOTAL=0

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Initialize results file
> "$RESULTS_FILE"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Review Service - Comprehensive Test Suite${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Function to test endpoint
test_endpoint() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_code="$5"
    
    TOTAL=$((TOTAL + 1))
    
    echo -n "Test $TOTAL: $test_name ... "
    
    if [ "$method" == "GET" ]; then
        response=$(curl -s -w "\n%{http_code}" "$BASE_URL$endpoint")
    elif [ "$method" == "POST" ]; then
        response=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    elif [ "$method" == "PUT" ]; then
        response=$(curl -s -w "\n%{http_code}" -X PUT "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    elif [ "$method" == "DELETE" ]; then
        response=$(curl -s -w "\n%{http_code}" -X DELETE "$BASE_URL$endpoint")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" == "$expected_code" ]; then
        echo -e "${GREEN}✓ PASSED${NC} (HTTP $http_code)"
        echo "✓ $test_name (HTTP $http_code)" >> "$RESULTS_FILE"
        PASSED=$((PASSED + 1))
        echo "$body"
    else
        echo -e "${RED}✗ FAILED${NC} (Expected: $expected_code, Got: $http_code)"
        echo "✗ $test_name (Expected: $expected_code, Got: $http_code)" >> "$RESULTS_FILE"
        FAILED=$((FAILED + 1))
        echo "Response: $body" >> "$RESULTS_FILE"
    fi
    echo ""
}

# Function to extract ID from response
extract_id() {
    echo "$1" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null || echo ""
}

# ============================================
# 1. HEALTH & WELCOME CHECKS
# ============================================
echo -e "${YELLOW}=== 1. Health & Welcome Checks ===${NC}\n"

test_endpoint "Welcome Endpoint" "GET" "/api/v1/welcome" "" "200"

test_endpoint "Health Check" "GET" "/actuator/health" "" "200"

# ============================================
# 2. CREATE REVIEWS
# ============================================
echo -e "${YELLOW}=== 2. Create Reviews ===${NC}\n"

# Create Review 1
review1_data='{"productId":"prod-001","userId":"user-001","rating":5,"title":"Excellent Product","comment":"Best purchase ever!"}'
echo "Creating Review 1..."
response=$(curl -s -X POST "$BASE_URL/api/v1/reviews" \
    -H "Content-Type: application/json" \
    -d "$review1_data")
REVIEW_ID_1=$(extract_id "$response")
test_endpoint "Create Review 1" "POST" "/api/v1/reviews" "$review1_data" "201"

# Create Review 2
review2_data='{"productId":"prod-001","userId":"user-002","rating":4,"title":"Very Good","comment":"Meets expectations"}'
echo "Creating Review 2..."
response=$(curl -s -X POST "$BASE_URL/api/v1/reviews" \
    -H "Content-Type: application/json" \
    -d "$review2_data")
REVIEW_ID_2=$(extract_id "$response")
test_endpoint "Create Review 2" "POST" "/api/v1/reviews" "$review2_data" "201"

# Create Review 3
review3_data='{"productId":"prod-002","userId":"user-003","rating":3,"title":"Average","comment":"Not bad"}'
echo "Creating Review 3..."
response=$(curl -s -X POST "$BASE_URL/api/v1/reviews" \
    -H "Content-Type: application/json" \
    -d "$review3_data")
REVIEW_ID_3=$(extract_id "$response")
test_endpoint "Create Review 3" "POST" "/api/v1/reviews" "$review3_data" "201"

# Create Review 4
review4_data='{"productId":"prod-002","userId":"user-004","rating":2,"title":"Below Average","comment":"Could be better"}'
echo "Creating Review 4..."
response=$(curl -s -X POST "$BASE_URL/api/v1/reviews" \
    -H "Content-Type: application/json" \
    -d "$review4_data")
REVIEW_ID_4=$(extract_id "$response")
test_endpoint "Create Review 4" "POST" "/api/v1/reviews" "$review4_data" "201"

# ============================================
# 3. READ OPERATIONS
# ============================================
echo -e "${YELLOW}=== 3. Read Operations ===${NC}\n"

test_endpoint "Get Review by ID" "GET" "/api/v1/reviews/$REVIEW_ID_1" "" "200"

test_endpoint "Get All Reviews (Pagination)" "GET" "/api/v1/reviews?page=0&size=10" "" "200"

test_endpoint "Get Reviews by Product (prod-001)" "GET" "/api/v1/reviews/product/prod-001?page=0&size=10" "" "200"

test_endpoint "Get Reviews by Product (prod-002)" "GET" "/api/v1/reviews/product/prod-002?page=0&size=10" "" "200"

test_endpoint "Get Reviews by User (user-001)" "GET" "/api/v1/reviews/user/user-001?page=0&size=10" "" "200"

test_endpoint "Get Reviews by Status (PENDING)" "GET" "/api/v1/reviews/status/PENDING?page=0&size=10" "" "200"

# ============================================
# 4. UPDATE OPERATIONS
# ============================================
echo -e "${YELLOW}=== 4. Update Operations ===${NC}\n"

update_data='{"rating":5,"title":"Updated - Excellent!","comment":"After using it more, I give it 5 stars"}'
test_endpoint "Update Review" "PUT" "/api/v1/reviews/$REVIEW_ID_2" "$update_data" "200"

# ============================================
# 5. APPROVAL WORKFLOW
# ============================================
echo -e "${YELLOW}=== 5. Approval Workflow ===${NC}\n"

test_endpoint "Approve Review 1" "POST" "/api/v1/reviews/$REVIEW_ID_1/approve" "" "200"

test_endpoint "Approve Review 2" "POST" "/api/v1/reviews/$REVIEW_ID_2/approve" "" "200"

test_endpoint "Reject Review 3" "POST" "/api/v1/reviews/$REVIEW_ID_3/reject" "" "200"

# ============================================
# 6. FILTERED READS
# ============================================
echo -e "${YELLOW}=== 6. Filtered Reads ===${NC}\n"

test_endpoint "Get Approved Reviews (prod-001)" "GET" "/api/v1/reviews/product/prod-001/approved?page=0&size=10" "" "200"

test_endpoint "Get Reviews by Status (APPROVED)" "GET" "/api/v1/reviews/status/APPROVED?page=0&size=10" "" "200"

test_endpoint "Get Reviews by Status (REJECTED)" "GET" "/api/v1/reviews/status/REJECTED?page=0&size=10" "" "200"

# ============================================
# 7. DELETE OPERATIONS
# ============================================
echo -e "${YELLOW}=== 7. Delete Operations ===${NC}\n"

test_endpoint "Delete Review" "DELETE" "/api/v1/reviews/$REVIEW_ID_4" "" "204"

test_endpoint "Verify Review Deleted" "GET" "/api/v1/reviews/$REVIEW_ID_4" "" "500"

# ============================================
# 8. VALIDATION TESTS
# ============================================
echo -e "${YELLOW}=== 8. Validation Tests ===${NC}\n"

# Missing required field
invalid_data='{"userId":"user-005","rating":5,"title":"Test"}'
test_endpoint "Invalid - Missing productId" "POST" "/api/v1/reviews" "$invalid_data" "400"

# Invalid rating (out of range)
invalid_data2='{"productId":"prod-003","userId":"user-005","rating":6,"title":"Test","comment":"Test"}'
test_endpoint "Invalid - Rating out of range" "POST" "/api/v1/reviews" "$invalid_data2" "400"

# Invalid rating (below 1)
invalid_data3='{"productId":"prod-003","userId":"user-005","rating":0,"title":"Test","comment":"Test"}'
test_endpoint "Invalid - Rating below 1" "POST" "/api/v1/reviews" "$invalid_data3" "400"

# ============================================
# 9. PAGINATION TESTS
# ============================================
echo -e "${YELLOW}=== 9. Pagination Tests ===${NC}\n"

test_endpoint "Get Reviews - Page 0, Size 2" "GET" "/api/v1/reviews?page=0&size=2" "" "200"

test_endpoint "Get Reviews - Page 1, Size 2" "GET" "/api/v1/reviews?page=1&size=2" "" "200"

# ============================================
# SUMMARY
# ============================================
echo -e "\n${BLUE}========================================${NC}"
echo -e "${BLUE}Test Summary${NC}"
echo -e "${BLUE}========================================${NC}\n"

TOTAL_TESTS=$TOTAL
PASS_RATE=$((PASSED * 100 / TOTAL_TESTS))

echo "Total Tests: $TOTAL_TESTS"
echo -e "Passed: ${GREEN}$PASSED${NC}"
echo -e "Failed: ${RED}$FAILED${NC}"
echo "Pass Rate: $PASS_RATE%"

echo -e "\n${BLUE}Test results saved to: $RESULTS_FILE${NC}\n"

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}\n"
    exit 0
else
    echo -e "${RED}✗ Some tests failed${NC}\n"
    exit 1
fi
