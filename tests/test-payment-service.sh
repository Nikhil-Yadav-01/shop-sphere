#!/bin/bash

# Payment Service Test Script
# Tests all endpoints of the Payment Service

PAYMENT_SERVICE_URL="http://localhost:8092"
BASE_ENDPOINT="/payment"
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Payment Service Test Suite"
echo "=========================================="
echo ""

# Test 1: Health Check
echo -e "${YELLOW}Test 1: Health Check${NC}"
curl -s "${PAYMENT_SERVICE_URL}/actuator/health" | jq . || echo "Health check failed"
echo ""

# Test 2: Process Payment
echo -e "${YELLOW}Test 2: Process Payment${NC}"
PAYMENT_RESPONSE=$(curl -s -X POST "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/process" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "customerId": 1,
    "amount": 100.50,
    "currency": "USD",
    "method": "CREDIT_CARD",
    "paymentMethodDetails": "4532-XXXX-XXXX-1234"
  }')

echo "$PAYMENT_RESPONSE" | jq .
PAYMENT_ID=$(echo "$PAYMENT_RESPONSE" | jq -r '.id' 2>/dev/null)
TRANSACTION_ID=$(echo "$PAYMENT_RESPONSE" | jq -r '.transactionId' 2>/dev/null)
echo "Payment ID: $PAYMENT_ID, Transaction ID: $TRANSACTION_ID"
echo ""

# Test 3: Get Payment by ID
if [ ! -z "$PAYMENT_ID" ] && [ "$PAYMENT_ID" != "null" ]; then
  echo -e "${YELLOW}Test 3: Get Payment by ID ($PAYMENT_ID)${NC}"
  curl -s "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/${PAYMENT_ID}" | jq .
  echo ""
fi

# Test 4: Get Payment by Transaction ID
if [ ! -z "$TRANSACTION_ID" ] && [ "$TRANSACTION_ID" != "null" ]; then
  echo -e "${YELLOW}Test 4: Get Payment by Transaction ID ($TRANSACTION_ID)${NC}"
  curl -s "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/transaction/${TRANSACTION_ID}" | jq .
  echo ""
fi

# Test 5: Get Payments by Order ID
echo -e "${YELLOW}Test 5: Get Payments by Order ID (1)${NC}"
curl -s "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/order/1" | jq .
echo ""

# Test 6: Get Payments by Customer ID
echo -e "${YELLOW}Test 6: Get Payments by Customer ID (1)${NC}"
curl -s "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/customer/1" | jq .
echo ""

# Test 7: Get Payments by Status
echo -e "${YELLOW}Test 7: Get Payments by Status (COMPLETED)${NC}"
curl -s "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/status/COMPLETED" | jq .
echo ""

# Test 8: Get All Payments
echo -e "${YELLOW}Test 8: Get All Payments${NC}"
curl -s "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}" | jq .
echo ""

# Test 9: Update Payment Status
if [ ! -z "$PAYMENT_ID" ] && [ "$PAYMENT_ID" != "null" ]; then
  echo -e "${YELLOW}Test 9: Update Payment Status to PENDING${NC}"
  curl -s -X PUT "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/${PAYMENT_ID}/status?status=PENDING" | jq .
  echo ""
fi

# Test 10: Process Refund
if [ ! -z "$PAYMENT_ID" ] && [ "$PAYMENT_ID" != "null" ]; then
  echo -e "${YELLOW}Test 10: Refund Payment${NC}"
  curl -s -X POST "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/refund" \
    -H "Content-Type: application/json" \
    -d "{
      \"paymentId\": ${PAYMENT_ID},
      \"refundAmount\": 50.00,
      \"reason\": \"Partial refund requested by customer\"
    }" | jq .
  echo ""
fi

# Test 11: Validation Test - Invalid Amount
echo -e "${YELLOW}Test 11: Validation Test - Invalid Amount (should fail)${NC}"
curl -s -X POST "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/process" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "customerId": 1,
    "amount": -100,
    "currency": "USD",
    "method": "CREDIT_CARD",
    "paymentMethodDetails": "4532-XXXX-XXXX-1234"
  }' | jq .
echo ""

# Test 12: Validation Test - Invalid Currency
echo -e "${YELLOW}Test 12: Validation Test - Invalid Currency (should fail)${NC}"
curl -s -X POST "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/process" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "customerId": 1,
    "amount": 100.50,
    "currency": "INVALID",
    "method": "CREDIT_CARD",
    "paymentMethodDetails": "4532-XXXX-XXXX-1234"
  }' | jq .
echo ""

echo "=========================================="
echo "Payment Service Tests Completed"
echo "=========================================="
