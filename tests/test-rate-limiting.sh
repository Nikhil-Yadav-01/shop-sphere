#!/bin/bash
IP="${1:-localhost}"
BASE_URL="http://$IP:8080"

echo "=== Testing Rate Limiting ==="
echo ""

echo "1. Testing Auth Rate Limiting (10 req/s)..."
SUCCESS_COUNT=0
LIMIT_COUNT=0
OTHER_COUNT=0

for i in {1..20}
do
  CODE=$(curl -s -o /dev/null -w "%{http_code}" $BASE_URL/auth/health)
  if [ "$CODE" == "200" ]; then
    SUCCESS_COUNT=$((SUCCESS_COUNT+1))
  elif [ "$CODE" == "429" ]; then
    LIMIT_COUNT=$((LIMIT_COUNT+1))
  else
    OTHER_COUNT=$((OTHER_COUNT+1))
  fi
done

echo "Results: 200 OK: $SUCCESS_COUNT, 429 Too Many Requests: $LIMIT_COUNT, Other: $OTHER_COUNT"

if [ $LIMIT_COUNT -gt 0 ]; then
  echo "Rate limiting is WORKING (caught $LIMIT_COUNT '429' responses)."
else
  echo "Rate limiting might NOT be working (caught 0 '429' responses)."
  # Note: This might fail if the rate limiter state was empty and we are too slow.
  # But with 20 requests in a loop, it should trigger 429 if limit is 10.
fi

echo ""
echo "2. Testing User-ID based Rate Limiting (Simulation)..."
echo "Sending requests with different X-User-Id headers..."

# This is harder to test without a real backend that respects X-User-Id or by checking Redis keys.
# But we can at least verify that the gateway doesn't crash.

curl -s -o /dev/null -w "Request with User-1: %{http_code}\n" -H "X-User-Id: user-1" $BASE_URL/catalog/api/v1/categories
curl -s -o /dev/null -w "Request with User-2: %{http_code}\n" -H "X-User-Id: user-2" $BASE_URL/catalog/api/v1/categories

echo ""
echo "Rate limiting test complete."
