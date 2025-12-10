#!/bin/bash

BASE_URL="http://localhost:8088/api/v1/coupons"

echo "=== Testing Enhanced Coupon Service ==="

# Test 1: Get all active coupons
echo "1. Getting all active coupons..."
curl -X GET "$BASE_URL" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 2: Get specific coupon
echo "2. Getting WELCOME10 coupon..."
curl -X GET "$BASE_URL/WELCOME10" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 3: Validate coupon
echo "3. Validating WELCOME10 coupon..."
curl -X POST "$BASE_URL/validate" \
  -H "Content-Type: application/json" \
  -d '{
    "couponCode": "WELCOME10",
    "userId": "user123",
    "orderAmount": 100.00
  }' | jq .

echo -e "\n"

# Test 4: Apply coupon
echo "4. Applying WELCOME10 coupon..."
curl -X POST "$BASE_URL/apply" \
  -H "Content-Type: application/json" \
  -d '{
    "couponCode": "WELCOME10",
    "userId": "user123",
    "orderId": "order123",
    "orderAmount": 100.00
  }' | jq .

echo -e "\n"

# Test 5: Create new coupon
TIMESTAMP=$(date +%s%N | cut -b1-13)
echo "5. Creating new coupon..."
curl -X POST "$BASE_URL" \
  -H "Content-Type: application/json" \
  -d '{
    "code": "TEST15_'$TIMESTAMP'",
    "description": "Test 15% discount",
    "discountType": "PERCENTAGE",
    "discountValue": 15.00,
    "minimumOrderAmount": 25.00,
    "maximumDiscountAmount": 50.00,
    "usageLimit": 10,
    "validFrom": "2025-01-01T00:00:00",
    "validUntil": "2025-12-31T23:59:59"
  }' | jq .

echo -e "\n"

# Test 6: Update coupon
echo "6. Updating TEST15 coupon..."
curl -X PUT "$BASE_URL/TEST15_$TIMESTAMP" \
  -H "Content-Type: application/json" \
  -d '{
    "description": "Updated 15% discount",
    "discountValue": 20.00
  }' | jq .

echo -e "\n"

# Test 7: Get all coupons with pagination
echo "7. Getting all coupons (paginated)..."
curl -X GET "$BASE_URL/all?page=0&size=5" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 8: Get user coupon history
echo "8. Getting user coupon history..."
curl -X GET "$BASE_URL/users/user123/history" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 9: Get coupon usage history
echo "9. Getting WELCOME10 usage history..."
curl -X GET "$BASE_URL/WELCOME10/usage-history" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 10: Get expired coupons
echo "10. Getting expired coupons..."
curl -X GET "$BASE_URL/expired" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 11: Get coupons expiring in 30 days
echo "11. Getting coupons expiring in 30 days..."
curl -X GET "$BASE_URL/expiring?days=30" \
  -H "Content-Type: application/json" | jq .

echo -e "\n"

# Test 12: Deactivate coupon
echo "12. Deactivating TEST15 coupon..."
curl -X PUT "$BASE_URL/TEST15_$TIMESTAMP/deactivate" \
  -H "Content-Type: application/json" | jq .

echo -e "\n=== Enhanced Coupon Service Tests Complete ==="