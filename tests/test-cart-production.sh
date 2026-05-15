#!/bin/bash
set -e

# Use localhost for CI
IP="localhost"
USER_ID="cart-test-$(date +%s)"

echo "=== Testing Production-Ready Cart Service ==="
echo ""

# Helper to wait for a service
wait_for_service() {
    local url=$1
    local name=$2
    echo "Waiting for $name..."
    for i in $(seq 1 30); do
        if curl -sf "$url" > /dev/null 2>&1; then
            echo "$name is ready!"
            return 0
        fi
        sleep 2
    done
    echo "$name timed out!"
    return 1
}

# 1. Seed Catalog Service
echo "Seeding Catalog Service..."
PRODUCT_ID="prod-ci-1"
SKU="SKU-CI-1"
curl -s -X POST -H "Content-Type: application/json" \
    -d "{
        \"sku\": \"$SKU\",
        \"name\": \"CI Test Product\",
        \"description\": \"A product for CI testing\",
        \"price\": 49.99,
        \"categoryId\": \"cat-ci\",
        \"images\": [\"http://example.com/image.jpg\"]
    }" http://$IP:8083/api/v1/products > product_response.json

ACTUAL_PRODUCT_ID=$(cat product_response.json | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Created Product ID: $ACTUAL_PRODUCT_ID"

# 2. Seed Inventory Service
echo "Seeding Inventory Service..."
curl -s -X POST -H "Content-Type: application/json" \
    -d "{
        \"sku\": \"$SKU\",
        \"productId\": 1,
        \"quantity\": 10,
        \"location\": \"Warehouse CI\"
    }" http://$IP:8092/api/inventory

# 3. Test Add to Cart (Should work now with real data)
echo "3. Add Item to Cart (Real Data):"
ADD1=$(curl -s -X POST -H "X-User-Id: $USER_ID" -H "Content-Type: application/json" \
    -d "{\"productId\":\"$ACTUAL_PRODUCT_ID\",\"quantity\":2}" \
    http://$IP:8085/api/v1/cart/items)

echo "Response: $ADD1"
echo $ADD1 | grep -q "\"productName\":\"CI Test Product\"" || (echo "FAILED: ProductName mismatch"; exit 1)
echo $ADD1 | grep -q "\"price\":49.99" || (echo "FAILED: Price mismatch"; exit 1)
echo "SUCCESS: Real product data used."

# 4. Test Stock Validation (Insufficient stock)
echo "4. Test Stock Validation (Insufficient):"
ADD2=$(curl -s -X POST -H "X-User-Id: $USER_ID" -H "Content-Type: application/json" \
    -d "{\"productId\":\"$ACTUAL_PRODUCT_ID\",\"quantity\":20}" \
    http://$IP:8085/api/v1/cart/items)

echo "Response: $ADD2"
echo $ADD2 | grep -q "Insufficient stock" || (echo "FAILED: Stock validation did not trigger"; exit 1)
echo "SUCCESS: Stock validation working."

# 5. Test Redis TTL
echo "5. Verify Redis TTL:"
# In CI, we can use docker exec if running on same host
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

# 6. Test Feign Fallback (Stop catalog-service temporarily if possible, or simulate)
# In CI, we can't easily stop services mid-test without complexity.
# But we can test with a non-existent product ID which should trigger getProductByIdFallback (if we coded it that way)
# Actually, our fallback returns a default product.
echo "6. Test Feign Fallback (Non-existent product):"
ADD_FAIL=$(curl -s -X POST -H "X-User-Id: $USER_ID" -H "Content-Type: application/json" \
    -d '{"productId":"non-existent","quantity":1}' \
    http://$IP:8085/api/v1/cart/items)

echo "Response: $ADD_FAIL"
echo $ADD_FAIL | grep -q "Product temporarily unavailable" || (echo "FAILED: Fallback not triggered"; exit 1)
echo "SUCCESS: Feign fallback working."

echo "=== All Production-Ready Cart Tests Passed ==="
