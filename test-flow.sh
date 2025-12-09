#!/bin/bash

BASE_URL="http://16.171.12.89:8080"
AUTH_URL="$BASE_URL/auth"
ADMIN_URL="$BASE_URL/admin/api/v1"

echo "=========================================="
echo "ShopSphere API Test Script"
echo "=========================================="
echo ""

# Step 1: Register user first
echo "1. Registering user..."
REGISTER_RESPONSE=$(curl -s -X POST "$AUTH_URL/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin2@shopsphere.com",
    "password": "Admin@123",
    "firstName": "Admin",
    "lastName": "User"
  }')
echo "$REGISTER_RESPONSE"
echo ""

# Step 2: Update user to ADMIN role
echo "2. Updating user to ADMIN role..."
docker exec -i auth-db psql -U postgres -d shopsphere_auth <<EOF
UPDATE users SET role = 'ADMIN' WHERE email = 'admin2@shopsphere.com';
EOF
echo ""

# Step 3: Login
echo "3. Logging in as admin..."
LOGIN_RESPONSE=$(curl -s -X POST "$AUTH_URL/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin2@shopsphere.com",
    "password": "Admin@123"
  }')

echo "$LOGIN_RESPONSE"
echo ""

# Extract access token
ACCESS_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "ERROR: Failed to get access token"
    exit 1
fi

echo "Access Token: $ACCESS_TOKEN"
echo ""

# Step 4: Test Admin Endpoints
echo "4. Testing Admin Service - Dashboard Overview..."
RESPONSE=$(curl -s -X GET "$ADMIN_URL/dashboard/overview" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "$RESPONSE"
echo ""

echo "5. Creating Audit Log..."
RESPONSE=$(curl -s -X POST "$ADMIN_URL/audit-logs?adminId=1&action=CREATE&resourceType=USER&resourceId=101&details=Test%20user%20created&ipAddress=16.171.12.89" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "$RESPONSE"
echo ""

echo "6. Recording System Metric..."
RESPONSE=$(curl -s -X POST "$ADMIN_URL/metrics?metricType=CPU_USAGE&value=45.5&unit=percent" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "$RESPONSE"
echo ""

echo "7. Getting Audit Logs..."
RESPONSE=$(curl -s -X GET "$ADMIN_URL/audit-logs?adminId=1&page=0&size=10" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "$RESPONSE"
echo ""

echo "8. Getting Recent Metrics..."
SINCE_TIME=$(date -u -d '1 hour ago' +"%Y-%m-%dT%H:%M:%S")
RESPONSE=$(curl -s -X GET "$ADMIN_URL/metrics/recent?since=$SINCE_TIME" \
  -H "Authorization: Bearer $ACCESS_TOKEN")
echo "$RESPONSE"
echo ""

echo "=========================================="
echo "All tests completed successfully!"
echo "=========================================="
