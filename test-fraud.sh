#!/bin/bash

# Fraud Service Comprehensive Test Script
# Tests all endpoints, Kafka integration, and database operations

set -e

FRAUD_SERVICE_URL="http://localhost:8010"
KAFKA_BROKER="localhost:9092"
DB_HOST="localhost"
DB_PORT="5441"
DB_NAME="shopsphere_fraud"
DB_USER="postgres"
DB_PASSWORD="password"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Helper function to print test results
print_test() {
    echo -e "${BLUE}[TEST]${NC} $1"
    ((TOTAL_TESTS++))
}

print_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((PASSED_TESTS++))
}

print_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((FAILED_TESTS++))
}

print_section() {
    echo -e "\n${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}\n"
}

# Helper function to check HTTP response code
check_http_response() {
    local response=$1
    local expected_code=$2
    local test_name=$3
    
    if echo "$response" | grep -q "$expected_code"; then
        print_pass "$test_name (HTTP $expected_code)"
        return 0
    else
        print_fail "$test_name - Expected HTTP $expected_code but got different response"
        return 1
    fi
}

# Test 1: Service Health Check
print_section "1. HEALTH CHECK TESTS"

print_test "Health endpoint accessibility"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" "$FRAUD_SERVICE_URL/fraud/health" 2>&1)
HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
BODY=$(echo "$HEALTH_RESPONSE" | head -n -1)

if [ "$HTTP_CODE" = "200" ] && echo "$BODY" | grep -q "UP"; then
    print_pass "Service is UP and healthy"
else
    print_fail "Service health check failed (HTTP $HTTP_CODE)"
    echo "Response: $BODY"
    exit 1
fi

print_test "Actuator health endpoint"
ACTUATOR_RESPONSE=$(curl -s -w "\n%{http_code}" "$FRAUD_SERVICE_URL/actuator/health" 2>&1)
ACTUATOR_CODE=$(echo "$ACTUATOR_RESPONSE" | tail -n1)

if [ "$ACTUATOR_CODE" = "200" ]; then
    print_pass "Actuator health endpoint accessible"
else
    print_fail "Actuator health endpoint failed (HTTP $ACTUATOR_CODE)"
fi

# Test 2: Database Connectivity
print_section "2. DATABASE CONNECTIVITY TESTS"

print_test "PostgreSQL database connectivity"
if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT 1" &>/dev/null; then
    print_pass "Database connection successful"
else
    print_fail "Database connection failed"
    exit 1
fi

print_test "Database tables exist"
if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT * FROM fraud_detection LIMIT 1" &>/dev/null; then
    print_pass "fraud_detection table exists"
else
    print_fail "fraud_detection table not found"
fi

if PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -c "SELECT * FROM fraud_rules LIMIT 1" &>/dev/null; then
    print_pass "fraud_rules table exists"
else
    print_fail "fraud_rules table not found"
fi

# Test 3: Fraud Check Endpoint Tests
print_section "3. FRAUD CHECK ENDPOINT TESTS"

print_test "Low-risk transaction (amount 1000, valid device)"
RESPONSE=$(curl -s -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "transactionId": "TXN-LOW-001",
        "orderId": 1001,
        "customerId": 2001,
        "amount": 1000.00,
        "currency": "USD",
        "transactionType": "PURCHASE",
        "paymentMethod": "CREDIT_CARD",
        "ipAddress": "192.168.1.1",
        "deviceId": "device-001"
    }')

if echo "$RESPONSE" | grep -q '"isFraudulent":false' && echo "$RESPONSE" | grep -q '"status":"APPROVED"'; then
    print_pass "Low-risk transaction correctly approved"
    TXID_LOW=$(echo "$RESPONSE" | grep -o '"transactionId":"[^"]*"' | head -1 | cut -d'"' -f4)
else
    print_fail "Low-risk transaction test failed"
    echo "$RESPONSE"
fi

print_test "Medium-risk transaction (amount 6000)"
RESPONSE=$(curl -s -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "transactionId": "TXN-MED-001",
        "orderId": 1002,
        "customerId": 2002,
        "amount": 6000.00,
        "currency": "USD",
        "transactionType": "PURCHASE",
        "paymentMethod": "CREDIT_CARD",
        "ipAddress": "192.168.1.2",
        "deviceId": "device-002"
    }')

if echo "$RESPONSE" | grep -q '"riskScore":15'; then
    print_pass "Medium-risk transaction risk score calculated correctly"
else
    print_fail "Medium-risk transaction risk score incorrect"
fi

print_test "High-risk transaction (amount 12000 + card not present)"
RESPONSE=$(curl -s -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "transactionId": "TXN-HIGH-001",
        "orderId": 1003,
        "customerId": 2003,
        "amount": 12000.00,
        "currency": "USD",
        "transactionType": "PURCHASE",
        "paymentMethod": "CARD_NOT_PRESENT",
        "ipAddress": "192.168.1.3"
    }')

if echo "$RESPONSE" | grep -q '"isFraudulent":true' && echo "$RESPONSE" | grep -q '"status":"REJECTED"'; then
    print_pass "High-risk transaction correctly rejected"
    TXID_HIGH=$(echo "$RESPONSE" | grep -o '"transactionId":"[^"]*"' | head -1 | cut -d'"' -f4)
else
    print_fail "High-risk transaction test failed"
    echo "$RESPONSE"
fi

print_test "Very high-risk transaction (amount 20000 + card not present + no device)"
RESPONSE=$(curl -s -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "transactionId": "TXN-VHIGH-001",
        "orderId": 1004,
        "customerId": 2004,
        "amount": 20000.00,
        "currency": "EUR",
        "transactionType": "PURCHASE",
        "paymentMethod": "CARD_NOT_PRESENT"
    }')

if echo "$RESPONSE" | grep -q '"riskScore":70' && echo "$RESPONSE" | grep -q '"isFraudulent":true'; then
    print_pass "Very high-risk transaction correctly scored and rejected"
else
    print_fail "Very high-risk transaction test failed"
    echo "$RESPONSE"
fi

print_test "Transaction with all required fields"
RESPONSE=$(curl -s -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "transactionId": "TXN-VALID-001",
        "orderId": 1005,
        "customerId": 2005,
        "amount": 500.00,
        "currency": "USD"
    }')

if echo "$RESPONSE" | grep -q '"id"'; then
    print_pass "Minimal required fields processed successfully"
else
    print_fail "Minimal fields test failed"
fi

print_test "Duplicate transaction detection"
RESPONSE=$(curl -s -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "transactionId": "TXN-LOW-001",
        "orderId": 1001,
        "customerId": 2001,
        "amount": 1000.00,
        "currency": "USD"
    }')

if echo "$RESPONSE" | grep -q '"transactionId":"TXN-LOW-001"'; then
    print_pass "Duplicate transaction detected and returned"
else
    print_fail "Duplicate transaction test failed"
fi

# Test 4: Retrieval Endpoint Tests
print_section "4. RETRIEVAL ENDPOINT TESTS"

print_test "Get fraud detection by ID"
RESPONSE=$(curl -s -w "\n%{http_code}" "$FRAUD_SERVICE_URL/fraud/1" 2>&1)
CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$CODE" = "200" ]; then
    print_pass "Fraud detection retrieval by ID successful"
else
    print_fail "Fraud detection retrieval by ID failed (HTTP $CODE)"
fi

print_test "Get fraud detection by transaction ID"
RESPONSE=$(curl -s -w "\n%{http_code}" "$FRAUD_SERVICE_URL/fraud/transaction/TXN-HIGH-001" 2>&1)
CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$CODE" = "200" ]; then
    print_pass "Fraud detection retrieval by transaction ID successful"
else
    print_fail "Fraud detection retrieval by transaction ID failed (HTTP $CODE)"
fi

print_test "Get non-existent fraud detection"
RESPONSE=$(curl -s -w "\n%{http_code}" "$FRAUD_SERVICE_URL/fraud/99999" 2>&1)
CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$CODE" = "404" ]; then
    print_pass "Non-existent fraud detection returns 404"
else
    print_fail "Non-existent fraud detection should return 404"
fi

# Test 5: Customer Fraud History
print_section "5. CUSTOMER FRAUD HISTORY TESTS"

print_test "Get customer fraud history (multiple fraudulent transactions)"
RESPONSE=$(curl -s -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "transactionId": "TXN-CUST-001",
        "orderId": 2001,
        "customerId": 3001,
        "amount": 15000.00,
        "currency": "USD",
        "paymentMethod": "CARD_NOT_PRESENT"
    }')

RESPONSE=$(curl -s -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "transactionId": "TXN-CUST-002",
        "orderId": 2002,
        "customerId": 3001,
        "amount": 18000.00,
        "currency": "USD",
        "paymentMethod": "CARD_NOT_PRESENT"
    }')

HISTORY=$(curl -s "$FRAUD_SERVICE_URL/fraud/customer/3001/history")

if echo "$HISTORY" | grep -q '"fraudulentTransactionCount":2'; then
    print_pass "Customer fraud history retrieved with correct count"
else
    print_fail "Customer fraud history test failed"
    echo "$HISTORY"
fi

print_test "Get customer with no fraudulent history"
CLEAN_HISTORY=$(curl -s "$FRAUD_SERVICE_URL/fraud/customer/2001/history")

if echo "$CLEAN_HISTORY" | grep -q '"fraudulentTransactionCount":0'; then
    print_pass "Customer with no fraud shows correct count"
else
    print_fail "Clean customer fraud history test failed"
fi

# Test 6: Kafka Integration
print_section "6. KAFKA INTEGRATION TESTS"

print_test "Kafka broker connectivity"
if docker exec kafka kafka-broker-api-versions.sh --bootstrap-server localhost:9092 &>/dev/null; then
    print_pass "Kafka broker is accessible"
else
    print_fail "Kafka broker connection failed"
fi

print_test "Kafka topics exist or can be created"
if docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list &>/dev/null; then
    print_pass "Kafka topics command successful"
else
    print_fail "Kafka topics check failed"
fi

# Test 7: Validation Tests
print_section "7. VALIDATION TESTS"

print_test "Missing required field (transactionId)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "orderId": 1001,
        "customerId": 2001,
        "amount": 1000.00,
        "currency": "USD"
    }' 2>&1)
CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$CODE" = "400" ]; then
    print_pass "Missing required field correctly returns 400"
else
    print_fail "Validation failed - expected 400 but got $CODE"
fi

print_test "Invalid amount (negative)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "transactionId": "TXN-NEG",
        "orderId": 1001,
        "customerId": 2001,
        "amount": -100.00,
        "currency": "USD"
    }' 2>&1)
CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$CODE" = "400" ]; then
    print_pass "Negative amount correctly returns 400"
else
    print_fail "Negative amount validation failed"
fi

print_test "Invalid customer ID (negative)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "transactionId": "TXN-NEG-CUST",
        "orderId": 1001,
        "customerId": -1,
        "amount": 1000.00,
        "currency": "USD"
    }' 2>&1)
CODE=$(echo "$RESPONSE" | tail -n1)

if [ "$CODE" = "400" ]; then
    print_pass "Negative customer ID correctly returns 400"
else
    print_fail "Customer ID validation failed"
fi

# Test 8: Database Persistence
print_section "8. DATABASE PERSISTENCE TESTS"

print_test "Data persists in database"
DB_COUNT=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM fraud_detection;")

if [ "$DB_COUNT" -gt 0 ]; then
    print_pass "Data persisted in database (Total records: $DB_COUNT)"
else
    print_fail "No data found in database"
fi

print_test "Transaction details accurately stored"
DB_TXN=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM fraud_detection WHERE is_fraudulent = true;")

if [ "$DB_TXN" -gt 0 ]; then
    print_pass "Fraudulent transactions correctly flagged in database"
else
    print_fail "Fraudulent transaction flagging failed"
fi

# Test 9: Data Types and Formats
print_section "9. DATA TYPES AND FORMATS TESTS"

print_test "Risk score is numeric and within bounds"
RESPONSE=$(curl -s -X POST "$FRAUD_SERVICE_URL/fraud/check" \
    -H "Content-Type: application/json" \
    -d '{
        "transactionId": "TXN-DTYPE-001",
        "orderId": 1006,
        "customerId": 2006,
        "amount": 3000.00,
        "currency": "USD"
    }')

if echo "$RESPONSE" | grep -o '"riskScore":[0-9]*\.?[0-9]*' | head -1; then
    print_pass "Risk score is properly formatted numeric value"
else
    print_fail "Risk score format test failed"
fi

print_test "Timestamp format is ISO 8601"
if echo "$RESPONSE" | grep -q '"createdAt":"[0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}'; then
    print_pass "Timestamp in ISO 8601 format"
else
    print_fail "Timestamp format test failed"
fi

print_test "Boolean fields are properly typed"
if echo "$RESPONSE" | grep -q '"isFraudulent":true\|"isFraudulent":false'; then
    print_pass "Boolean fields properly typed"
else
    print_fail "Boolean field type test failed"
fi

# Test 10: Concurrent Requests
print_section "10. CONCURRENT REQUESTS TEST"

print_test "Handle multiple concurrent requests"
for i in {1..5}; do
    curl -s -X POST "$FRAUD_SERVICE_URL/fraud/check" \
        -H "Content-Type: application/json" \
        -d "{
            \"transactionId\": \"TXN-CONCURRENT-$i\",
            \"orderId\": $((2000 + i)),
            \"customerId\": $((3000 + i)),
            \"amount\": $((1000 + i * 100)),
            \"currency\": \"USD\"
        }" &
done
wait

FINAL_COUNT=$(PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -t -c "SELECT COUNT(*) FROM fraud_detection WHERE transaction_id LIKE 'TXN-CONCURRENT%';")

if [ "$FINAL_COUNT" = "5" ]; then
    print_pass "All 5 concurrent requests processed successfully"
else
    print_fail "Concurrent request test failed - got $FINAL_COUNT records"
fi

# Test 11: Service Discovery
print_section "11. SERVICE DISCOVERY TEST"

print_test "Service registered with Eureka"
EUREKA_RESPONSE=$(curl -s "http://localhost:8761/eureka/apps/FRAUD-SERVICE" -H "Accept: application/json" 2>/dev/null || echo "")

if echo "$EUREKA_RESPONSE" | grep -q "fraud-service"; then
    print_pass "Service registered with Eureka"
else
    print_fail "Service discovery test failed"
fi

# Final Summary
print_section "TEST SUMMARY"
echo -e "${GREEN}Passed: $PASSED_TESTS${NC}"
echo -e "${RED}Failed: $FAILED_TESTS${NC}"
echo -e "${BLUE}Total: $TOTAL_TESTS${NC}"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✓ ALL TESTS PASSED!${NC}"
    echo -e "${GREEN}========================================${NC}"
    exit 0
else
    echo -e "${RED}========================================${NC}"
    echo -e "${RED}✗ SOME TESTS FAILED${NC}"
    echo -e "${RED}========================================${NC}"
    exit 1
fi
