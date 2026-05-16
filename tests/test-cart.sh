#!/bin/bash
set -e

# Use localhost for CI
IP="localhost"
USER_ID="cart-test-$(date +%s)"

echo "=== Testing Production-Ready Cart Service ==="
echo ""

# 1. Seed Catalog Service
echo "Seeding Catalog Service..."
PRODUCT_SKU="SKU-CART-CI-$(date +%s)"
curl -s -X POST -H "Content-Type: application/json" \
    -d "{
        \"sku\": \"$PRODUCT_SKU\",
        \"name\": \"CI Test Product\",
        \"description\": \"A product for CI testing\",
        \"price\": 49.99,
        \"categoryId\": \"cat-ci\",
        \"images\": [\"https://picsum.photos/seed/cart-test/400/300\"]
    }" http://$IP:8083/api/v1/products > product_response.json

ACTUAL_PRODUCT_ID=$(cat product_response.json | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Created Product ID: $ACTUAL_PRODUCT_ID"

if [ -z "$ACTUAL_PRODUCT_ID" ]; then
    echo "FAILED: Product creation failed"
    cat product_response.json
    exit 1
fi

# Wait for Eureka discovery propagation
echo "Waiting for service discovery (Eureka registration)..."
for i in {1..20}; do
    if curl -s http://$IP:8761/eureka/apps/CATALOG-SERVICE | grep -q "UP"; then
        echo "✓ Catalog Service registered in Eureka"
        break
    fi
    echo "  Still waiting for Catalog Service ($i/20)..."
    sleep 3
done

echo "Waiting for Ribbon/LoadBalancer cache refresh (45s)..."
sleep 45

# 2. Seed Inventory Service
echo "Seeding Inventory Service..."
curl -s -X POST -H "Content-Type: application/json" \
    -d "{
        \"sku\": \"$PRODUCT_SKU\",
        \"productId\": 1,
        \"quantity\": 10,
        \"reorderLevel\": 5,
        \"warehouseLocation\": \"Warehouse CI\"
    }" http://$IP:8092/api/inventory

# 3. Test Add to Cart (Should work now with real data)
echo "3. Add Item to Cart (Real Data):"
ADD1=$(curl -s -X POST -H "x-user-id: $USER_ID" -H "Content-Type: application/json" \
    -d "{\"productId\":\"$ACTUAL_PRODUCT_ID\",\"quantity\":2}" \
    http://$IP:8085/api/v1/cart/items)

echo "Response: $ADD1"
echo $ADD1 | grep -q "\"productName\":\"CI Test Product\"" || (echo "FAILED: ProductName mismatch"; exit 1)
echo $ADD1 | grep -q "\"price\":49.99" || (echo "FAILED: Price mismatch"; exit 1)
echo "SUCCESS: Real product data used."

# 4. Test Stock Validation (Insufficient stock)
echo "4. Test Stock Validation (Insufficient):"
ADD2=$(curl -s -X POST -H "x-user-id: $USER_ID" -H "Content-Type: application/json" \
    -d "{\"productId\":\"$ACTUAL_PRODUCT_ID\",\"quantity\":20}" \
    http://$IP:8085/api/v1/cart/items)

echo "Response: $ADD2"
echo $ADD2 | grep -q "Insufficient stock" || (echo "FAILED: Stock validation did not trigger"; exit 1)
echo "SUCCESS: Stock validation working."

# 5. Test Redis TTL
echo "5. Verify Redis TTL:"
if command -v docker &> /dev/null; then
    TTL=$(docker exec redis redis-cli ttl "cart:$USER_ID")
    echo "TTL for cart:$USER_ID: $TTL seconds"
    if [ "$TTL" -gt 0 ]; then
        echo "SUCCESS: Redis TTL is set."
    else
        echo "FAILED: Redis TTL is not set."
        exit 1
    fi
fi

# 6. Test Feign Fallback (Non-existent product)
echo "6. Test Feign Fallback (Non-existent product):"
ADD_FAIL=$(curl -s -X POST -H "x-user-id: $USER_ID" -H "Content-Type: application/json" \
    -d '{"productId":"non-existent","quantity":1}' \
    http://$IP:8085/api/v1/cart/items)

echo "Response: $ADD_FAIL"
echo $ADD_FAIL | grep -q "Product temporarily unavailable" || (echo "FAILED: Fallback not triggered"; exit 1)
echo "SUCCESS: Feign fallback working."

echo "=== Cart Service Tests Complete ==="
