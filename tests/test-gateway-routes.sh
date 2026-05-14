#!/bin/bash
IP="${1:-localhost}"
BASE_URL="http://$IP:8080"
ADMIN_EMAIL="test.admin@shopsphere.com"
ADMIN_PASSWORD="TestPassword123!"
CUSTOMER_EMAIL="test.customer@shopsphere.com"
CUSTOMER_PASSWORD="TestPassword123!"

echo "=== Testing Gateway Routes ==="
echo ""

echo "1. Gateway health:"
curl -s -o /dev/null -w "%{http_code}" $BASE_URL/actuator/health
echo ""

echo "2. Gateway routes:"
curl -s -o /dev/null -w "%{http_code}" $BASE_URL/actuator/gateway/routes
echo ""

echo "3. /auth/actuator/health (no auth - expect 401 or 403):"
curl -s -o /dev/null -w "%{http_code}" $BASE_URL/auth/actuator/health
echo ""

echo "4. Logging in as Customer..."
CUSTOMER_LOGIN=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$CUSTOMER_EMAIL\",\"password\":\"$CUSTOMER_PASSWORD\"}")
CUSTOMER_TOKEN=$(echo $CUSTOMER_LOGIN | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -n "$CUSTOMER_TOKEN" ]; then
    echo "Customer Login: 200 (Success)"
else
    echo "Customer Login: FAILED"
fi

echo "5. /auth/actuator/health (Customer role - expect 403):"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $CUSTOMER_TOKEN" $BASE_URL/auth/actuator/health
echo ""

echo "6. Logging in as Admin..."
ADMIN_LOGIN=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}")
ADMIN_TOKEN=$(echo $ADMIN_LOGIN | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -n "$ADMIN_TOKEN" ]; then
    echo "Admin Login: 200 (Success)"
else
    echo "Admin Login: FAILED"
fi

echo "7. /auth/actuator/health (Admin role - expect 200):"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" $BASE_URL/auth/actuator/health
echo ""

echo "8. /catalog/api/v1/categories:"
curl -s -o /dev/null -w "%{http_code}" $BASE_URL/catalog/api/v1/categories
echo ""

echo "9. Protected route (no auth - expect 401):"
curl -s -o /dev/null -w "%{http_code}" $BASE_URL/users/profile
echo ""
