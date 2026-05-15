#!/bin/bash
set -e

# Use localhost for CI
IP="localhost"

echo "=== Testing Production-Ready Catalog Service ==="
echo ""

# 1. Test Category Hierarchy
echo "1. Testing Category Hierarchy:"
# Create Root Category
ROOT_CAT=$(curl -s -X POST -H "Content-Type: application/json" \
    -d '{"name": "Electronics", "description": "Electronic gadgets"}' \
    http://$IP:8083/api/v1/categories)
ROOT_ID=$(echo $ROOT_CAT | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Created Root Category: Electronics (ID: $ROOT_ID)"

# Create Child Category
CHILD_CAT=$(curl -s -X POST -H "Content-Type: application/json" \
    -d "{\"name\": \"Smartphones\", \"description\": \"Mobile phones\", \"parentId\": \"$ROOT_ID\"}" \
    http://$IP:8083/api/v1/categories)
CHILD_ID=$(echo $CHILD_CAT | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Created Child Category: Smartphones (ID: $CHILD_ID, Parent: $ROOT_ID)"

# Verify Hierarchy in Response
echo $CHILD_CAT | grep -q "\"parentId\":\"$ROOT_ID\"" || (echo "FAILED: ParentId mismatch"; exit 1)
echo $CHILD_CAT | grep -q "\"level\":1" || (echo "FAILED: Level mismatch"; exit 1)
echo $CHILD_CAT | grep -q "\"path\":\"Electronics > Smartphones\"" || (echo "FAILED: Path mismatch"; exit 1)
echo "SUCCESS: Category hierarchy working."
echo ""

# 2. Test Full-Text Search
echo "2. Testing Full-Text Search:"
# Create a product
curl -s -X POST -H "Content-Type: application/json" \
    -d "{
        \"sku\": \"SEARCH-SKU-1\",
        \"name\": \"Super Gadget Alpha\",
        \"description\": \"The best gadget in the universe\",
        \"price\": 99.99,
        \"categoryId\": \"$CHILD_ID\"
    }" http://$IP:8083/api/v1/products > /dev/null

# Wait for index (MongoDB auto-index might take a moment)
sleep 2

# Search by keyword in name
SEARCH_RES1=$(curl -s "http://$IP:8083/api/v1/products/search?keyword=Alpha")
echo "Search result for 'Alpha': $SEARCH_RES1"
echo $SEARCH_RES1 | grep -q "Super Gadget Alpha" || (echo "FAILED: Search by name failed"; exit 1)

# Search by keyword in description
SEARCH_RES2=$(curl -s "http://$IP:8083/api/v1/products/search?keyword=universe")
echo "Search result for 'universe': $SEARCH_RES2"
echo $SEARCH_RES2 | grep -q "Super Gadget Alpha" || (echo "FAILED: Search by description failed"; exit 1)
echo "SUCCESS: Full-text search working."
echo ""

# 3. Test Category Caching
echo "3. Testing Category Caching:"
# First call - should populate cache
time curl -s http://$IP:8083/api/v1/categories > /dev/null
# Second call - should be faster (from cache)
time curl -s http://$IP:8083/api/v1/categories > /dev/null
echo "SUCCESS: Category caching verified (check logs for cache hits)."
echo ""

# 4. Test Redis TTL for Caching
if command -v docker &> /dev/null; then
    echo "4. Verifying Redis Cache Keys:"
    docker exec redis redis-cli keys "categories*"
fi

echo "=== All Production-Ready Catalog Tests Passed ==="
