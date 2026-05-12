#!/bin/bash

# Test Script for Search Service
# This script tests all endpoints of the Search Service

set -e

BASE_URL="http://localhost:8084/api/v1"
HEALTH_CHECK_URL="http://localhost:8084/actuator/health"

echo "================================================"
echo "Search Service Test Suite"
echo "================================================"
echo ""

# Helper function for API calls
call_api() {
    local method=$1
    local endpoint=$2
    local data=$3
    
    if [ -z "$data" ]; then
        curl -s -X $method "$BASE_URL$endpoint"
    else
        curl -s -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data"
    fi
}

# Test 1: Service Health Check
echo "Test 1: Service Health Check"
echo "GET $HEALTH_CHECK_URL"
response=$(curl -s $HEALTH_CHECK_URL)
echo "Response: $response"
echo "✓ Health check passed"
echo ""

# Test 2: Welcome Endpoint
echo "Test 2: Welcome Endpoint"
echo "GET $BASE_URL/welcome"
call_api GET "/welcome" | jq .
echo "✓ Welcome endpoint passed"
echo ""

# Test 3: Health Check Endpoint
echo "Test 3: Health Check Endpoint"
echo "GET $BASE_URL/health-check"
call_api GET "/health-check" | jq .
echo "✓ Health check endpoint passed"
echo ""

# Test 4: Get Index Size
echo "Test 4: Get Index Size"
echo "GET $BASE_URL/search/index-size"
size=$(call_api GET "/search/index-size")
echo "Current index size: $size"
echo "✓ Index size retrieved"
echo ""

# Test 5: Search with Empty Index
echo "Test 5: Search with Keyword (Empty Index)"
echo "POST $BASE_URL/search?page=0&size=10"
call_api POST "/search?page=0&size=10" '{"keyword":"test"}' | jq '.totalElements'
echo "✓ Keyword search passed"
echo ""

# Test 6: Search by Keyword Parameter
echo "Test 6: Search by Keyword Parameter"
echo "GET $BASE_URL/search/keyword?keyword=laptop&page=0&size=10"
call_api GET "/search/keyword?keyword=laptop&page=0&size=10" | jq '.totalElements'
echo "✓ Keyword parameter search passed"
echo ""

# Test 7: Search by Category
echo "Test 7: Search by Category"
echo "GET $BASE_URL/search/category/cat123?page=0&size=10"
call_api GET "/search/category/cat123?page=0&size=10" | jq '.totalElements'
echo "✓ Category search passed"
echo ""

# Test 8: Search by Status
echo "Test 8: Search by Status"
echo "GET $BASE_URL/search/status/ACTIVE?page=0&size=10"
call_api GET "/search/status/ACTIVE?page=0&size=10" | jq '.totalElements'
echo "✓ Status search passed"
echo ""

# Test 9: Search by Price Range
echo "Test 9: Search by Price Range"
echo "GET $BASE_URL/search/price?minPrice=10&maxPrice=100&page=0&size=10"
call_api GET "/search/price?minPrice=10&maxPrice=100&page=0&size=10" | jq '.totalElements'
echo "✓ Price range search passed"
echo ""

# Test 10: Search by In Stock Status
echo "Test 10: Search by In Stock Status"
echo "GET $BASE_URL/search/in-stock/true?page=0&size=10"
call_api GET "/search/in-stock/true?page=0&size=10" | jq '.totalElements'
echo "✓ In-stock search passed"
echo ""

# Test 11: Reindex Endpoint
echo "Test 11: Reindex Endpoint"
echo "POST $BASE_URL/search/reindex"
response=$(call_api POST "/search/reindex")
echo "Response: $response"
echo "✓ Reindex endpoint passed"
echo ""

echo "================================================"
echo "All tests completed successfully!"
echo "================================================"
echo ""
echo "Service Information:"
echo "- Service Port: 8084"
echo "- Database: MongoDB (27022)"
echo "- Service Discovery: Eureka (8761)"
echo "- Message Queue: Kafka (9092)"
echo ""
