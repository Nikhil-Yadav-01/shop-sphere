#!/bin/bash

# Quick Payment Service Test
# Minimal tests for quick validation
IP="51.20.189.129"

PAYMENT_SERVICE_URL="http://$IP:8092"
BASE_ENDPOINT="/payment"

echo "=== Payment Service Quick Test ==="
echo ""

# Health Check
echo "1. Health Check:"
curl -s "${PAYMENT_SERVICE_URL}/actuator/health" | jq '.status'
echo ""

# Process Payment
echo "2. Process Payment:"
RESPONSE=$(curl -s -X POST "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/process" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 2,
    "customerId": 2,
    "amount": 250.99,
    "currency": "USD",
    "method": "DEBIT_CARD",
    "paymentMethodDetails": "5555-XXXX-XXXX-4444"
  }')

echo "$RESPONSE" | jq '{id, transactionId, status, amount, currency}'
PAYMENT_ID=$(echo "$RESPONSE" | jq -r '.id')
echo ""

# Get Payment
echo "3. Get Payment by ID ($PAYMENT_ID):"
curl -s "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}/${PAYMENT_ID}" | jq '{id, status, amount}'
echo ""

# Get All Payments
echo "4. Get All Payments:"
curl -s "${PAYMENT_SERVICE_URL}${BASE_ENDPOINT}" | jq 'length as $count | {total_payments: $count}'
echo ""

echo "=== Tests Complete ==="
