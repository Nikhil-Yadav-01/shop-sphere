#!/bin/bash

# Test Returns Service API

BASE_URL="http://localhost:8090"
CONTENT_TYPE="Content-Type: application/json"

echo "=========================================="
echo "Testing Returns Service API"
echo "=========================================="

# Test 1: Welcome endpoint
echo ""
echo "1. Testing Welcome Endpoint (GET /):"
curl -s -X GET "$BASE_URL/" -H "$CONTENT_TYPE"
echo ""
echo ""

# Test 2: Health Check
echo "2. Testing Health Check (GET /actuator/health):"
curl -s -X GET "$BASE_URL/actuator/health" -H "$CONTENT_TYPE" | jq .
echo ""
echo ""

# Test 3: Create a Return
echo "3. Creating a Return (POST /api/v1/returns):"
RETURN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/returns" \
  -H "$CONTENT_TYPE" \
  -d '{
    "orderId": "order-001",
    "customerId": "customer-001",
    "reason": "Product Defect",
    "description": "Product stopped working after 2 days",
    "itemIds": ["item-001", "item-002"],
    "refundAmount": 150.00
  }')

echo "$RETURN_RESPONSE" | jq .
RETURN_ID=$(echo "$RETURN_RESPONSE" | jq -r '.id' 2>/dev/null || echo "")
echo ""
echo ""

# Test 4: Get Return by ID
if [ -n "$RETURN_ID" ] && [ "$RETURN_ID" != "null" ]; then
  echo "4. Getting Return by ID (GET /api/v1/returns/$RETURN_ID):"
  curl -s -X GET "$BASE_URL/api/v1/returns/$RETURN_ID" \
    -H "$CONTENT_TYPE" | jq .
  echo ""
  echo ""

  # Test 5: Update Return Status
  echo "5. Updating Return Status (PUT /api/v1/returns/$RETURN_ID):"
  curl -s -X PUT "$BASE_URL/api/v1/returns/$RETURN_ID" \
    -H "$CONTENT_TYPE" \
    -d '{
      "status": "APPROVED",
      "trackingNumber": "TRACK123456",
      "description": "Return approved by admin"
    }' | jq .
  echo ""
  echo ""
fi

# Test 6: Get All Returns (with pagination)
echo "6. Getting All Returns (GET /api/v1/returns?page=0&size=10):"
curl -s -X GET "$BASE_URL/api/v1/returns?page=0&size=10" \
  -H "$CONTENT_TYPE" | jq .
echo ""
echo ""

echo "=========================================="
echo "Returns Service API Tests Completed"
echo "=========================================="
