#!/bin/bash

BASE_URL="http://localhost:8092/payment"

echo "=== Testing Payment Service ==="

# Test 1: Process payment
echo "1. Processing new payment..."
PAYMENT_RESPONSE=$(curl -s -X POST "$BASE_URL/process" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "customerId": 100,
    "amount": 1000.00,
    "currency": "USD",
    "method": "CREDIT_CARD",
    "paymentMethodDetails": "4111111111111111"
  }')

echo "$PAYMENT_RESPONSE" | jq .
PAYMENT_ID=$(echo "$PAYMENT_RESPONSE" | jq -r '.id')
TRANSACTION_ID=$(echo "$PAYMENT_RESPONSE" | jq -r '.transactionId')

echo -e "\n"

# Test 2: Get payment by ID
echo "2. Getting payment by ID ($PAYMENT_ID)..."
curl -s -X GET "$BASE_URL/$PAYMENT_ID" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 3: Get payment by transaction ID
echo "3. Getting payment by transaction ID ($TRANSACTION_ID)..."
curl -s -X GET "$BASE_URL/transaction/$TRANSACTION_ID" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 4: Get payments by order ID
echo "4. Getting payments by order ID (1)..."
curl -s -X GET "$BASE_URL/order/1" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 5: Get payments by customer ID
echo "5. Getting payments by customer ID (100)..."
curl -s -X GET "$BASE_URL/customer/100" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 6: Get payments by status
echo "6. Getting payments by status (SUCCESS)..."
curl -s -X GET "$BASE_URL/status/SUCCESS" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 7: Process another payment with different method
echo "7. Processing payment with DEBIT_CARD..."
DEBIT_PAYMENT=$(curl -s -X POST "$BASE_URL/process" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 2,
    "customerId": 101,
    "amount": 500.50,
    "currency": "USD",
    "method": "DEBIT_CARD",
    "paymentMethodDetails": "5555555555554444"
  }')

echo "$DEBIT_PAYMENT" | jq .
DEBIT_PAYMENT_ID=$(echo "$DEBIT_PAYMENT" | jq -r '.id')

echo -e "\n"

# Test 8: Get all payments
echo "8. Getting all payments..."
curl -s -X GET "$BASE_URL" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 9: Update payment status to REFUNDED (for first payment)
echo "9. Refunding payment (transaction: $TRANSACTION_ID)..."
REFUND_RESPONSE=$(curl -s -X POST "$BASE_URL/refund" \
  -H "Content-Type: application/json" \
  -d "{
    \"transactionId\": \"$TRANSACTION_ID\",
    \"reason\": \"Customer requested refund\"
  }")

echo "$REFUND_RESPONSE" | jq .

echo -e "\n"

# Test 10: Get refunded payment
echo "10. Getting refunded payment by transaction ID..."
curl -s -X GET "$BASE_URL/transaction/$TRANSACTION_ID" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 11: Process payment with UPI
echo "11. Processing payment with UPI..."
UPI_PAYMENT=$(curl -s -X POST "$BASE_URL/process" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 3,
    "customerId": 102,
    "amount": 250.75,
    "currency": "USD",
    "method": "UPI",
    "paymentMethodDetails": "9876543210@upi"
  }')

echo "$UPI_PAYMENT" | jq .

echo -e "\n"

# Test 12: Update payment status
echo "12. Updating payment status to PROCESSING..."
curl -s -X PUT "$BASE_URL/$DEBIT_PAYMENT_ID/status?status=PROCESSING" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 13: Test validation - missing required field
echo "13. Testing validation - missing currency..."
curl -s -X POST "$BASE_URL/process" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 4,
    "customerId": 103,
    "amount": 100.00,
    "method": "CREDIT_CARD",
    "paymentMethodDetails": "4111111111111111"
  }' | jq .

echo -e "\n"

# Test 14: Test validation - invalid amount
echo "14. Testing validation - invalid amount (negative)..."
curl -s -X POST "$BASE_URL/process" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 5,
    "customerId": 104,
    "amount": -100.00,
    "currency": "USD",
    "method": "CREDIT_CARD",
    "paymentMethodDetails": "4111111111111111"
  }' | jq .

echo -e "\n"

# Test 15: Get payments for customer 100
echo "15. Getting all payments for customer 100..."
curl -s -X GET "$BASE_URL/customer/100" \
  -H "Content-Type: application/json" | jq .

echo -e "\n=== Payment Service Tests Complete ==="
