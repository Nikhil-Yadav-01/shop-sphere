#!/bin/bash

BASE_URL="http://localhost:8080/order"

echo "=== Testing Order Service ==="

# Test 1: Create new order
echo "1. Creating new order..."
ORDER_RESPONSE=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "customerId": 123,
    "items": [
      {
        "productId": 1,
        "productName": "Laptop",
        "quantity": 1,
        "unitPrice": 999.99,
        "totalPrice": 999.99
      },
      {
        "productId": 2,
        "productName": "Mouse",
        "quantity": 2,
        "unitPrice": 29.99,
        "totalPrice": 59.98
      }
    ],
    "totalAmount": 1059.97,
    "taxAmount": 106.00,
    "shippingAddress": "123 Main St, City, State 12345",
    "billingAddress": "123 Main St, City, State 12345"
  }')

echo "$ORDER_RESPONSE" | jq .
ORDER_ID=$(echo "$ORDER_RESPONSE" | jq -r '.id')
ORDER_NUMBER=$(echo "$ORDER_RESPONSE" | jq -r '.orderNumber')

echo -e "\n"

# Test 2: Get order by ID
echo "2. Getting order by ID ($ORDER_ID)..."
curl -s -X GET "$BASE_URL/$ORDER_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" | jq .

echo -e "\n"

# Test 3: Get order by order number
echo "3. Getting order by order number ($ORDER_NUMBER)..."
curl -s -X GET "$BASE_URL/number/$ORDER_NUMBER" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" | jq .

echo -e "\n"

# Test 4: Get orders by customer ID
echo "4. Getting orders by customer ID (123)..."
curl -s -X GET "$BASE_URL/customer/123" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" | jq .

echo -e "\n"

# Test 5: Get orders by status
echo "5. Getting orders by status (PENDING)..."
curl -s -X GET "$BASE_URL/status/PENDING" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" | jq .

echo -e "\n"

# Test 6: Update order status to CONFIRMED
echo "6. Updating order status to CONFIRMED..."
curl -s -X PUT "$BASE_URL/$ORDER_ID/status?status=CONFIRMED" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" | jq .

echo -e "\n"

# Test 7: Update order status to SHIPPED
echo "7. Updating order status to SHIPPED..."
curl -s -X PUT "$BASE_URL/$ORDER_ID/status?status=SHIPPED" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" | jq .

echo -e "\n"

# Test 8: Get all orders
echo "8. Getting all orders..."
curl -s -X GET "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" | jq .

echo -e "\n"

# Test 9: Create another order for customer 456
echo "9. Creating second order for customer 456..."
SECOND_ORDER=$(curl -s -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" \
  -d '{
    "customerId": 456,
    "items": [
      {
        "productId": 3,
        "productName": "Keyboard",
        "quantity": 1,
        "unitPrice": 149.99,
        "totalPrice": 149.99
      }
    ],
    "totalAmount": 149.99,
    "taxAmount": 15.00,
    "shippingAddress": "456 Oak Ave, City, State 54321",
    "billingAddress": "456 Oak Ave, City, State 54321"
  }')

echo "$SECOND_ORDER" | jq .
SECOND_ORDER_ID=$(echo "$SECOND_ORDER" | jq -r '.id')

echo -e "\n"

# Test 10: Get orders for customer 456
echo "10. Getting orders for customer 456..."
curl -s -X GET "$BASE_URL/customer/456" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" | jq .

echo -e "\n"

# Test 11: Update second order status to DELIVERED
echo "11. Updating second order status to DELIVERED..."
curl -s -X PUT "$BASE_URL/$SECOND_ORDER_ID/status?status=DELIVERED" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token" | jq .

echo -e "\n"

# Test 12: Delete order (optional - uncomment to test)
# echo "12. Deleting first order..."
# curl -s -X DELETE "$BASE_URL/$ORDER_ID" \
#   -H "Content-Type: application/json" \
#   -H "Authorization: Bearer test-token" | jq .

echo -e "\n=== Order Service Tests Complete ==="
