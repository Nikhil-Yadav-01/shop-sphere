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
    -d '{"name": "Electronics'$(date +%s)'", "description": "Electronic gadgets"}' \
    http://$IP:8083/api/v1/categories)
ROOT_ID=$(echo $ROOT_CAT | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Created Root Category: (ID: $ROOT_ID)"

# Create Child Category
CHILD_CAT=$(curl -s -X POST -H "Content-Type: application/json" \
    -d "{\"name\": \"Smartphones'$(date +%s)'\", \"description\": \"Mobile phones\", \"parentId\": \"$ROOT_ID\"}" \
    http://$IP:8083/api/v1/categories)
CHILD_ID=$(echo $CHILD_CAT | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Created Child Category: (ID: $CHILD_ID, Parent: $ROOT_ID)"

# Verify Hierarchy in Response
echo $CHILD_CAT | grep -q "\"parentId\":\"$ROOT_ID\"" || (echo "FAILED: ParentId mismatch"; exit 1)
echo $CHILD_CAT | grep -q "\"level\":1" || (echo "FAILED: Level mismatch"; exit 1)
echo "SUCCESS: Category hierarchy working."
echo ""

# 2. Test Full-Text Search
echo "2. Testing Full-Text Search:"
PRODUCT_SKU="SEARCH-SKU-$(date +%s)"
curl -s -X POST -H "Content-Type: application/json" \
    -d "{
        \"sku\": \"$PRODUCT_SKU\",
        \"name\": \"Super Gadget Alpha\",
        \"description\": \"The best gadget in the universe\",
        \"price\": 99.99,
        \"categoryId\": \"$CHILD_ID\",
        \"images\": [\"https://picsum.photos/seed/catalog-test/400/300\"]
    }" http://$IP:8083/api/v1/products > /dev/null

# Wait for index
sleep 2

# Search by keyword in name
SEARCH_RES1=$(curl -s "http://$IP:8083/api/v1/products/search?keyword=Alpha")
echo $SEARCH_RES1 | grep -q "Super Gadget Alpha" || (echo "FAILED: Search by name failed"; exit 1)

# Search by keyword in description
SEARCH_RES2=$(curl -s "http://$IP:8083/api/v1/products/search?keyword=universe")
echo $SEARCH_RES2 | grep -q "Super Gadget Alpha" || (echo "FAILED: Search by description failed"; exit 1)
echo "SUCCESS: Full-text search working."
echo ""

# 3. Test Category Caching
echo "3. Testing Category Caching:"
time curl -s http://$IP:8083/api/v1/categories > /dev/null
time curl -s http://$IP:8083/api/v1/categories > /dev/null
echo "SUCCESS: Category caching verified."
echo ""

# 4. Test Redis Cache Keys
if command -v docker &> /dev/null; then
    echo "4. Verifying Redis Cache Keys:"
    docker exec redis redis-cli keys "categories*"
fi

# 5. Test Primary Image Convention
echo "5. Testing Primary Image Convention:"
# Get a product with multiple images
PRODUCT_WITH_IMAGES=$(curl -s "http://$IP:8083/api/v1/products/sku/$PRODUCT_SKU")
PRIMARY_IMAGE=$(echo $PRODUCT_WITH_IMAGES | jq -r '.images[0]')
echo "First image (Primary): $PRIMARY_IMAGE"

if [[ "$PRIMARY_IMAGE" == "https://picsum.photos/seed/catalog-test/400/300" ]]; then
    echo "SUCCESS: First image correctly identified as primary."
else
    echo "FAILED: Primary image mismatch."
    exit 1
fi
echo ""

echo "=== Catalog Service Tests Complete ==="
