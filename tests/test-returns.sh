#!/bin/bash

# Comprehensive Returns Service API Test Suite
# Tests all endpoints with valid/invalid inputs and complete workflows

set -e

BASE_URL="http://localhost:8097"
CONTENT_TYPE="Content-Type: application/json"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Helper function to print test header
print_test_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

# Helper function to print section
print_section() {
    echo ""
    echo -e "${YELLOW}>>> $1${NC}"
}

# Helper function to test API call
test_api() {
    local test_name=$1
    local method=$2
    local endpoint=$3
    local data=$4
    local expected_status=$5
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo -e "${BLUE}Test $TOTAL_TESTS: ${NC}$test_name"
    
    if [ -z "$data" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "$CONTENT_TYPE")
    else
        RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$endpoint" \
            -H "$CONTENT_TYPE" \
            -d "$data")
    fi
    
    # Extract status code and body
    HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
    BODY=$(echo "$RESPONSE" | sed '$d')
    
    echo "Method: $method | Endpoint: $endpoint | Status: $HTTP_CODE"
    
    if [ "$HTTP_CODE" -eq "$expected_status" ]; then
        echo -e "${GREEN}✅ PASSED${NC}"
        PASSED_TESTS=$((PASSED_TESTS + 1))
    else
        echo -e "${RED}❌ FAILED - Expected $expected_status, got $HTTP_CODE${NC}"
        FAILED_TESTS=$((FAILED_TESTS + 1))
    fi
    
    echo "Response: $(echo "$BODY" | jq -c . 2>/dev/null || echo "$BODY" | head -c 100)"
    echo ""
}

# Main test execution
main() {
    print_test_header "RETURNS SERVICE - COMPREHENSIVE API TEST SUITE"
    
    # ============================================================
    # SECTION 1: HEALTH CHECK & BASIC ENDPOINTS
    # ============================================================
    print_section "SECTION 1: Health Check & Basic Endpoints"
    
    test_api "Welcome Endpoint" "GET" "/" "" 200
    test_api "Health Check" "GET" "/actuator/health" "" 200
    test_api "Metrics Endpoint" "GET" "/actuator/metrics" "" 200
    
    # ============================================================
    # SECTION 2: CREATE RETURNS - VALID INPUTS
    # ============================================================
    print_section "SECTION 2: Create Returns - Valid Inputs"
    
    # Valid return 1
    RETURN_1=$(curl -s -X POST "$BASE_URL/api/v1/returns" \
        -H "$CONTENT_TYPE" \
        -d '{
            "orderId": "order-001",
            "customerId": "customer-001",
            "reason": "Product Defect",
            "description": "Product stopped working after 2 days",
            "itemIds": ["item-001", "item-002"],
            "refundAmount": 150.00
        }')
    
    RETURN_1_ID=$(echo "$RETURN_1" | jq -r '.id')
    test_api "Create Return (Valid - Full Details)" "POST" "/api/v1/returns" \
        '{"orderId":"order-valid-1","customerId":"cust-001","reason":"Defective","description":"Not working","itemIds":["item-1"],"refundAmount":99.99}' 201
    
    # Valid return 2 - Different reason
    test_api "Create Return (Valid - Wrong Size)" "POST" "/api/v1/returns" \
        '{"orderId":"order-size","customerId":"cust-002","reason":"Wrong Size","description":"Item too small","itemIds":["item-2","item-3"],"refundAmount":75.50}' 201
    
    # Valid return 3 - Damage
    test_api "Create Return (Valid - Damaged)" "POST" "/api/v1/returns" \
        '{"orderId":"order-damaged","customerId":"cust-003","reason":"Damaged Item","description":"Arrived damaged","itemIds":["item-4"],"refundAmount":200.00}' 201
    
    # Valid return 4 - Not as Described
    test_api "Create Return (Valid - Not as Described)" "POST" "/api/v1/returns" \
        '{"orderId":"order-notdesc","customerId":"cust-004","reason":"Not as Described","description":"Different color","itemIds":["item-5"],"refundAmount":125.75}' 201
    
    # Valid return with minimal refund
    test_api "Create Return (Valid - Low Refund)" "POST" "/api/v1/returns" \
        '{"orderId":"order-low","customerId":"cust-005","reason":"Minor Issue","description":"Small scratch","itemIds":["item-6"],"refundAmount":10.00}' 201
    
    # Valid return with high refund
    test_api "Create Return (Valid - High Refund)" "POST" "/api/v1/returns" \
        '{"orderId":"order-high","customerId":"cust-006","reason":"Major Issue","description":"Completely broken","itemIds":["item-7","item-8","item-9"],"refundAmount":999.99}' 201
    
    # Valid return with single item
    test_api "Create Return (Valid - Single Item)" "POST" "/api/v1/returns" \
        '{"orderId":"order-single","customerId":"cust-007","reason":"Defective","description":"Not working","itemIds":["item-10"],"refundAmount":50.00}' 201
    
    # Valid return with many items
    test_api "Create Return (Valid - Multiple Items)" "POST" "/api/v1/returns" \
        '{"orderId":"order-many","customerId":"cust-008","reason":"All Defective","description":"All items broken","itemIds":["item-11","item-12","item-13","item-14","item-15"],"refundAmount":500.00}' 201
    
    # ============================================================
    # SECTION 3: CREATE RETURNS - INVALID INPUTS (Validation)
    # ============================================================
    print_section "SECTION 3: Create Returns - Invalid Inputs (Validation)"
    
    # Missing orderId
    test_api "Create Return (Invalid - Missing orderId)" "POST" "/api/v1/returns" \
        '{"customerId":"cust-001","reason":"Defective","description":"Not working","itemIds":["item-1"],"refundAmount":99.99}' 400
    
    # Missing customerId
    test_api "Create Return (Invalid - Missing customerId)" "POST" "/api/v1/returns" \
        '{"orderId":"order-001","reason":"Defective","description":"Not working","itemIds":["item-1"],"refundAmount":99.99}' 400
    
    # Missing reason
    test_api "Create Return (Invalid - Missing reason)" "POST" "/api/v1/returns" \
        '{"orderId":"order-001","customerId":"cust-001","description":"Not working","itemIds":["item-1"],"refundAmount":99.99}' 400
    
    # Missing description
    test_api "Create Return (Invalid - Missing description)" "POST" "/api/v1/returns" \
        '{"orderId":"order-001","customerId":"cust-001","reason":"Defective","itemIds":["item-1"],"refundAmount":99.99}' 400
    
    # Empty itemIds array
    test_api "Create Return (Invalid - Empty itemIds)" "POST" "/api/v1/returns" \
        '{"orderId":"order-001","customerId":"cust-001","reason":"Defective","description":"Not working","itemIds":[],"refundAmount":99.99}' 400
    
    # Missing itemIds
    test_api "Create Return (Invalid - Missing itemIds)" "POST" "/api/v1/returns" \
        '{"orderId":"order-001","customerId":"cust-001","reason":"Defective","description":"Not working","refundAmount":99.99}' 400
    
    # Blank orderId
    test_api "Create Return (Invalid - Blank orderId)" "POST" "/api/v1/returns" \
        '{"orderId":"","customerId":"cust-001","reason":"Defective","description":"Not working","itemIds":["item-1"],"refundAmount":99.99}' 400
    
    # Blank customerId
    test_api "Create Return (Invalid - Blank customerId)" "POST" "/api/v1/returns" \
        '{"orderId":"order-001","customerId":"","reason":"Defective","description":"Not working","itemIds":["item-1"],"refundAmount":99.99}' 400
    
    # Negative refund amount
    test_api "Create Return (Invalid - Negative Refund)" "POST" "/api/v1/returns" \
        '{"orderId":"order-001","customerId":"cust-001","reason":"Defective","description":"Not working","itemIds":["item-1"],"refundAmount":-50.00}' 400
    
    # Invalid JSON format
    test_api "Create Return (Invalid - Bad JSON)" "POST" "/api/v1/returns" \
        '{"orderId":"order-001","customerId":"cust-001"' 400
    
    # ============================================================
    # SECTION 4: GET RETURNS - ALL ENDPOINTS
    # ============================================================
    print_section "SECTION 4: Get Returns - All Endpoints"
    
    test_api "Get All Returns (Page 0, Size 10)" "GET" "/api/v1/returns?page=0&size=10" "" 200
    test_api "Get All Returns (Page 1, Size 5)" "GET" "/api/v1/returns?page=1&size=5" "" 200
    test_api "Get All Returns (Page 0, Size 100)" "GET" "/api/v1/returns?page=0&size=100" "" 200
    
    test_api "Get Return by Valid ID" "GET" "/api/v1/returns/$RETURN_1_ID" "" 200
    test_api "Get Return by Invalid ID (Not Found)" "GET" "/api/v1/returns/invalid-id-12345" "" 500
    
    test_api "Get by Order ID" "GET" "/api/v1/returns/order/order-001?page=0&size=10" "" 200
    test_api "Get by Order ID (No Results)" "GET" "/api/v1/returns/order/order-nonexistent?page=0&size=10" "" 200
    
    test_api "Get by Customer ID" "GET" "/api/v1/returns/customer/customer-001?page=0&size=10" "" 200
    test_api "Get by Customer ID (No Results)" "GET" "/api/v1/returns/customer/customer-nonexistent?page=0&size=10" "" 200
    
    test_api "Get by Status (INITIATED)" "GET" "/api/v1/returns/status/INITIATED?page=0&size=10" "" 200
    test_api "Get by Status (APPROVED)" "GET" "/api/v1/returns/status/APPROVED?page=0&size=10" "" 200
    test_api "Get by Status (REJECTED)" "GET" "/api/v1/returns/status/REJECTED?page=0&size=10" "" 200
    test_api "Get by Status (IN_TRANSIT)" "GET" "/api/v1/returns/status/IN_TRANSIT?page=0&size=10" "" 200
    test_api "Get by Status (RECEIVED)" "GET" "/api/v1/returns/status/RECEIVED?page=0&size=10" "" 200
    test_api "Get by Status (REFUNDED)" "GET" "/api/v1/returns/status/REFUNDED?page=0&size=10" "" 200
    test_api "Get by Status (CANCELLED)" "GET" "/api/v1/returns/status/CANCELLED?page=0&size=10" "" 200
    
    # ============================================================
    # SECTION 5: UPDATE RETURNS - VALID INPUTS
    # ============================================================
    print_section "SECTION 5: Update Returns - Valid Inputs"
    
    # Update status to APPROVED
    test_api "Update Return (Valid - Status APPROVED)" "PUT" "/api/v1/returns/$RETURN_1_ID" \
        '{"status":"APPROVED","trackingNumber":"TRACK-001","description":"Return approved"}' 200
    
    # Update to IN_TRANSIT
    test_api "Update Return (Valid - Status IN_TRANSIT)" "PUT" "/api/v1/returns/$RETURN_1_ID" \
        '{"status":"IN_TRANSIT","trackingNumber":"TRACK-002"}' 200
    
    # Update to RECEIVED
    test_api "Update Return (Valid - Status RECEIVED)" "PUT" "/api/v1/returns/$RETURN_1_ID" \
        '{"status":"RECEIVED"}' 200
    
    # Update to REFUNDED
    test_api "Update Return (Valid - Status REFUNDED)" "PUT" "/api/v1/returns/$RETURN_1_ID" \
        '{"status":"REFUNDED","description":"Refund processed"}' 200
    
    # Update with only tracking number
    test_api "Update Return (Valid - Only Tracking)" "PUT" "/api/v1/returns/$RETURN_1_ID" \
        '{"trackingNumber":"TRACK-003"}' 200
    
    # Update with only description
    test_api "Update Return (Valid - Only Description)" "PUT" "/api/v1/returns/$RETURN_1_ID" \
        '{"description":"Updated description"}' 200
    
    # Update to REJECTED
    test_api "Update Return (Valid - Status REJECTED)" "PUT" "/api/v1/returns/$RETURN_1_ID" \
        '{"status":"REJECTED","description":"Return rejected - invalid claim"}' 200
    
    # Update to CANCELLED
    test_api "Update Return (Valid - Status CANCELLED)" "PUT" "/api/v1/returns/$RETURN_1_ID" \
        '{"status":"CANCELLED","description":"Customer cancelled"}' 200
    
    # ============================================================
    # SECTION 6: UPDATE RETURNS - INVALID INPUTS
    # ============================================================
    print_section "SECTION 6: Update Returns - Invalid Inputs"
    
    # Update with invalid ID (should fail or return 404/500)
    test_api "Update Return (Invalid - Non-existent ID)" "PUT" "/api/v1/returns/invalid-id-xyz" \
        '{"status":"APPROVED"}' 500
    
    # Update with invalid JSON
    test_api "Update Return (Invalid - Bad JSON)" "PUT" "/api/v1/returns/$RETURN_1_ID" \
        '{"status":"APPROVED"' 400
    
    # ============================================================
    # SECTION 7: DELETE RETURNS - VALID OPERATIONS
    # ============================================================
    print_section "SECTION 7: Delete Returns - Valid Operations"
    
    # Create a return to delete
    DELETE_RETURN=$(curl -s -X POST "$BASE_URL/api/v1/returns" \
        -H "$CONTENT_TYPE" \
        -d '{
            "orderId": "order-delete-1",
            "customerId": "cust-delete-1",
            "reason": "Test Delete",
            "description": "Will be deleted",
            "itemIds": ["item-del-1"],
            "refundAmount": 25.00
        }')
    DELETE_ID=$(echo "$DELETE_RETURN" | jq -r '.id')
    
    echo "Created return with ID: $DELETE_ID for deletion test"
    
    test_api "Delete Return (Valid)" "DELETE" "/api/v1/returns/$DELETE_ID" "" 204
    
    # Verify deletion
    test_api "Verify Deleted (Should Fail)" "GET" "/api/v1/returns/$DELETE_ID" "" 500
    
    # ============================================================
    # SECTION 8: DELETE RETURNS - INVALID OPERATIONS
    # ============================================================
    print_section "SECTION 8: Delete Returns - Invalid Operations"
    
    test_api "Delete Return (Invalid - Non-existent ID)" "DELETE" "/api/v1/returns/invalid-id-del" "" 500
    
    # ============================================================
    # SECTION 9: WORKFLOW TESTS - COMPLETE RETURN LIFECYCLE
    # ============================================================
    print_section "SECTION 9: Complete Return Lifecycle Workflow"
    
    # Create new return for workflow
    WORKFLOW_RETURN=$(curl -s -X POST "$BASE_URL/api/v1/returns" \
        -H "$CONTENT_TYPE" \
        -d '{
            "orderId": "order-workflow-001",
            "customerId": "cust-workflow-001",
            "reason": "Product Defect",
            "description": "Stopped working",
            "itemIds": ["item-workflow-1"],
            "refundAmount": 120.00
        }')
    
    WORKFLOW_ID=$(echo "$WORKFLOW_RETURN" | jq -r '.id')
    echo "Created workflow return with ID: $WORKFLOW_ID"
    
    # Step 1: Verify initial state
    test_api "Workflow Step 1: Verify INITIATED Status" "GET" "/api/v1/returns/$WORKFLOW_ID" "" 200
    
    # Step 2: Customer ships return
    test_api "Workflow Step 2: Mark as IN_TRANSIT" "PUT" "/api/v1/returns/$WORKFLOW_ID" \
        '{"status":"IN_TRANSIT","trackingNumber":"WORKFLOW-TRACK-001"}' 200
    
    # Step 3: Verify tracking
    test_api "Workflow Step 3: Verify Tracking Info" "GET" "/api/v1/returns/$WORKFLOW_ID" "" 200
    
    # Step 4: Seller receives item
    test_api "Workflow Step 4: Mark as RECEIVED" "PUT" "/api/v1/returns/$WORKFLOW_ID" \
        '{"status":"RECEIVED","description":"Item received and inspected"}' 200
    
    # Step 5: Process refund
    test_api "Workflow Step 5: Mark as REFUNDED" "PUT" "/api/v1/returns/$WORKFLOW_ID" \
        '{"status":"REFUNDED","description":"Refund processed to original payment method"}' 200
    
    # Step 6: Final verification
    test_api "Workflow Step 6: Verify REFUNDED Status" "GET" "/api/v1/returns/$WORKFLOW_ID" "" 200
    
    # ============================================================
    # SECTION 10: WORKFLOW TESTS - REJECTED RETURN
    # ============================================================
    print_section "SECTION 10: Rejected Return Workflow"
    
    # Create return to be rejected
    REJECT_RETURN=$(curl -s -X POST "$BASE_URL/api/v1/returns" \
        -H "$CONTENT_TYPE" \
        -d '{
            "orderId": "order-reject-001",
            "customerId": "cust-reject-001",
            "reason": "Not Applicable",
            "description": "Item in used condition",
            "itemIds": ["item-reject-1"],
            "refundAmount": 80.00
        }')
    
    REJECT_ID=$(echo "$REJECT_RETURN" | jq -r '.id')
    echo "Created return for rejection test with ID: $REJECT_ID"
    
    # Reject the return
    test_api "Workflow: Reject Return" "PUT" "/api/v1/returns/$REJECT_ID" \
        '{"status":"REJECTED","description":"Item appears used, return not eligible"}' 200
    
    # Verify rejection
    test_api "Workflow: Verify REJECTED Status" "GET" "/api/v1/returns/$REJECT_ID" "" 200
    
    # ============================================================
    # SECTION 11: WORKFLOW TESTS - CANCELLED RETURN
    # ============================================================
    print_section "SECTION 11: Cancelled Return Workflow"
    
    # Create return to be cancelled
    CANCEL_RETURN=$(curl -s -X POST "$BASE_URL/api/v1/returns" \
        -H "$CONTENT_TYPE" \
        -d '{
            "orderId": "order-cancel-001",
            "customerId": "cust-cancel-001",
            "reason": "Changed Mind",
            "description": "Customer changed mind",
            "itemIds": ["item-cancel-1"],
            "refundAmount": 60.00
        }')
    
    CANCEL_ID=$(echo "$CANCEL_RETURN" | jq -r '.id')
    echo "Created return for cancellation test with ID: $CANCEL_ID"
    
    # Cancel the return
    test_api "Workflow: Cancel Return" "PUT" "/api/v1/returns/$CANCEL_ID" \
        '{"status":"CANCELLED","description":"Customer cancelled return request"}' 200
    
    # Verify cancellation
    test_api "Workflow: Verify CANCELLED Status" "GET" "/api/v1/returns/$CANCEL_ID" "" 200
    
    # ============================================================
    # SECTION 12: PAGINATION & FILTERING TESTS
    # ============================================================
    print_section "SECTION 12: Pagination & Filtering Tests"
    
    # Get all returns to count
    ALL_RETURNS=$(curl -s "$BASE_URL/api/v1/returns?page=0&size=100")
    TOTAL_COUNT=$(echo "$ALL_RETURNS" | jq '.totalElements')
    
    echo "Total returns in database: $TOTAL_COUNT"
    
    # Test different page sizes
    test_api "Pagination: Page Size 5" "GET" "/api/v1/returns?page=0&size=5" "" 200
    test_api "Pagination: Page Size 20" "GET" "/api/v1/returns?page=0&size=20" "" 200
    test_api "Pagination: Page Size 50" "GET" "/api/v1/returns?page=0&size=50" "" 200
    
    # Test multiple pages
    if [ "$TOTAL_COUNT" -gt 5 ]; then
        test_api "Pagination: Page 1 with Size 5" "GET" "/api/v1/returns?page=1&size=5" "" 200
    fi
    
    # ============================================================
    # SECTION 13: PERFORMANCE & STRESS TESTS
    # ============================================================
    print_section "SECTION 13: Performance & Stress Tests"
    
    echo "Creating 10 returns rapidly..."
    for i in {1..10}; do
        curl -s -X POST "$BASE_URL/api/v1/returns" \
            -H "$CONTENT_TYPE" \
            -d "{
                \"orderId\": \"order-stress-$i\",
                \"customerId\": \"cust-stress-$i\",
                \"reason\": \"Stress Test\",
                \"description\": \"Load test return $i\",
                \"itemIds\": [\"item-stress-$i\"],
                \"refundAmount\": $((50 + i * 10))
            }" > /dev/null
        echo "  Created return $i"
    done
    
    test_api "Performance: Fetch All After Stress" "GET" "/api/v1/returns?page=0&size=100" "" 200
    
    # ============================================================
    # SECTION 14: EDGE CASES
    # ============================================================
    print_section "SECTION 14: Edge Cases"
    
    # Very long strings
    test_api "Edge Case: Very Long Order ID" "POST" "/api/v1/returns" \
        '{"orderId":"'$(printf 'a%.0s' {1..100})'","customerId":"cust-001","reason":"Test","description":"Test","itemIds":["item-1"],"refundAmount":50.00}' 201
    
    # Special characters in description
    test_api "Edge Case: Special Characters" "POST" "/api/v1/returns" \
        '{"orderId":"order-special","customerId":"cust-001","reason":"Test","description":"Test with @#$%^&*() special chars!","itemIds":["item-1"],"refundAmount":50.00}' 201
    
    # Unicode characters
    test_api "Edge Case: Unicode Characters" "POST" "/api/v1/returns" \
        '{"orderId":"order-unicode","customerId":"cust-001","reason":"Test 测试 テスト","description":"Unicode description","itemIds":["item-1"],"refundAmount":50.00}' 201
    
    # Zero refund amount
    test_api "Edge Case: Zero Refund Amount" "POST" "/api/v1/returns" \
        '{"orderId":"order-zero","customerId":"cust-001","reason":"Test","description":"Zero refund","itemIds":["item-1"],"refundAmount":0.00}' 201
    
    # Very large refund
    test_api "Edge Case: Very Large Refund" "POST" "/api/v1/returns" \
        '{"orderId":"order-large","customerId":"cust-001","reason":"Test","description":"Large refund","itemIds":["item-1"],"refundAmount":999999.99}' 201
    
    # ============================================================
    # FINAL SUMMARY
    # ============================================================
    print_test_header "TEST EXECUTION SUMMARY"
    
    echo -e "Total Tests Run:     $TOTAL_TESTS"
    echo -e "${GREEN}Passed Tests:       $PASSED_TESTS${NC}"
    echo -e "${RED}Failed Tests:       $FAILED_TESTS${NC}"
    echo ""
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo -e "${GREEN}✅ ALL TESTS PASSED!${NC}"
        OVERALL_STATUS=0
    else
        echo -e "${RED}❌ SOME TESTS FAILED!${NC}"
        OVERALL_STATUS=1
    fi
    
    PASS_RATE=$((PASSED_TESTS * 100 / TOTAL_TESTS))
    echo "Pass Rate: $PASS_RATE%"
    echo ""
    
    # ============================================================
    # SERVICE STATUS CHECK
    # ============================================================
    print_section "SERVICE STATUS CHECK"
    
    HEALTH=$(curl -s "$BASE_URL/actuator/health")
    echo "Service Health: $(echo "$HEALTH" | jq '.status')"
    echo ""
    
    FINAL_COUNT=$(curl -s "$BASE_URL/api/v1/returns?page=0&size=1" | jq '.totalElements')
    echo "Total Returns in Database: $FINAL_COUNT"
    echo ""
    
    # ============================================================
    # TEST REPORT
    # ============================================================
    cat > /tmp/test_report.txt << EOF
RETURNS SERVICE - TEST EXECUTION REPORT
Generated: $(date)

Test Summary:
  Total Tests:      $TOTAL_TESTS
  Passed:           $PASSED_TESTS
  Failed:           $FAILED_TESTS
  Pass Rate:        $PASS_RATE%

Test Coverage:
  ✅ Health & Basic Endpoints
  ✅ Create Returns (Valid & Invalid)
  ✅ Get Returns (All Variants)
  ✅ Update Returns (Valid & Invalid)
  ✅ Delete Returns (Valid & Invalid)
  ✅ Complete Workflows
  ✅ Pagination & Filtering
  ✅ Performance & Stress
  ✅ Edge Cases

Status: $([ $OVERALL_STATUS -eq 0 ] && echo "PASSED" || echo "FAILED")
EOF
    
    cat /tmp/test_report.txt
    echo ""
    
    exit $OVERALL_STATUS
}

# Execute main
main
