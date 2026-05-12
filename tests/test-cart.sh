#!/bin/bash
set -e
IP="51.20.188.129"
USER_ID="cart-test-$(date +%s)"

echo "=== Testing Cart Service Comprehensively ==="
echo ""

echo "1. Health Check:"
curl -s http://$IP:8085/actuator/health | grep -o '"status":"[^"]*"'
echo ""

echo "2. Get Empty Cart:"
CART=$(curl -s -H "X-User-Id: $USER_ID" http://$IP:8085/api/v1/cart)
echo $CART | grep -o '"totalItems":[0-9]*'
echo ""

echo "3. Add First Item to Cart:"
ADD1=$(curl -s -X POST -H "X-User-Id: $USER_ID" -H "Content-Type: application/json" -d '{"productId":"prod-001","quantity":2}' http://$IP:8085/api/v1/cart/items)
echo $ADD1 | grep -o '"totalItems":[0-9]*'
echo ""

echo "4. Add Second Item to Cart:"
ADD2=$(curl -s -X POST -H "X-User-Id: $USER_ID" -H "Content-Type: application/json" -d '{"productId":"prod-002","quantity":3}' http://$IP:8085/api/v1/cart/items)
echo $ADD2 | grep -o '"totalItems":[0-9]*'
echo ""

echo "5. Add Same Item (Should Increase Quantity):"
ADD3=$(curl -s -X POST -H "X-User-Id: $USER_ID" -H "Content-Type: application/json" -d '{"productId":"prod-001","quantity":1}' http://$IP:8085/api/v1/cart/items)
echo $ADD3 | grep -o '"totalItems":[0-9]*'
echo ""

echo "6. Get Cart with Items:"
CART2=$(curl -s -H "X-User-Id: $USER_ID" http://$IP:8085/api/v1/cart)
echo $CART2 | grep -o '"totalItems":[0-9]*'
ITEM_COUNT=$(echo $CART2 | grep -o '"items":\[' | wc -l)
echo "Items in cart: $ITEM_COUNT"
echo ""

echo "7. Update Item Quantity:"
UPDATE=$(curl -s -X PUT -H "X-User-Id: $USER_ID" -H "Content-Type: application/json" -d '{"quantity":5}' http://$IP:8085/api/v1/cart/items/prod-001)
echo $UPDATE | grep -o '"totalItems":[0-9]*'
echo ""

echo "8. Remove Item from Cart:"
REMOVE=$(curl -s -X DELETE -H "X-User-Id: $USER_ID" http://$IP:8085/api/v1/cart/items/prod-002)
echo $REMOVE | grep -o '"totalItems":[0-9]*'
echo ""

echo "9. Verify Item Removed:"
CART3=$(curl -s -H "X-User-Id: $USER_ID" http://$IP:8085/api/v1/cart)
echo $CART3 | grep -o '"totalItems":[0-9]*'
echo ""

echo "10. Clear Cart:"
curl -s -X DELETE -H "X-User-Id: $USER_ID" http://$IP:8085/api/v1/cart -w "\nStatus: %{http_code}\n"
echo ""

echo "11. Verify Cart Cleared:"
CART4=$(curl -s -H "X-User-Id: $USER_ID" http://$IP:8085/api/v1/cart)
echo $CART4 | grep -o '"totalItems":[0-9]*'
echo ""

echo "12. Via API Gateway - Get Cart:"
curl -s -H "X-User-Id: gateway-user" http://$IP:8080/cart/api/v1/cart | grep -o '"userId":"[^"]*"'
echo ""

echo "13. Via API Gateway - Add Item:"
curl -s -X POST -H "X-User-Id: gateway-user" -H "Content-Type: application/json" -d '{"productId":"gw-prod","quantity":1}' http://$IP:8080/cart/api/v1/cart/items | grep -o '"totalItems":[0-9]*'
echo ""

echo "14. Redis Connection Check:"
docker exec redis redis-cli ping
echo ""

echo "15. Check Cart Keys in Redis:"
docker exec redis redis-cli --scan --pattern "cart:*" | wc -l
echo ""

echo "=== Cart Service Tests Complete ==="
