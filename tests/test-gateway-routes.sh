#!/bin/bash
IP="${1:-localhost}"
# Since Nginx proxy is used in CI, we use HTTPS for some tests if needed, 
# but Gateway itself is at 8080 (HTTP). 
# However, the user's log shows it's hitting 8080.
BASE_URL="http://$IP:8080"

echo "=== Testing Gateway Routes ==="
echo ""
echo "1. Gateway health:"
curl -s -o /dev/null -w "%{http_code}" $BASE_URL/actuator/health
echo ""
echo "2. Gateway routes:"
curl -s -o /dev/null -w "%{http_code}" $BASE_URL/actuator/gateway/routes
echo ""
echo "3. /auth/actuator/health:"
curl -s -o /dev/null -w "%{http_code}" $BASE_URL/auth/actuator/health
echo ""
echo "4. /auth/login (expect 401 due to invalid credentials, but not 404/403):"
curl -s -o /dev/null -w "%{http_code}" -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"nonexistent@test.com","password":"wrongpassword"}'
echo ""
echo "5. /admin/actuator/health:"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/admin/actuator/health
echo ""
echo "6. /admin/api/v1/dashboard/overview (no auth - expect 401):"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/admin/api/v1/dashboard/overview
echo ""
echo "7. /catalog/api/v1/categories:"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/catalog/api/v1/categories
echo ""
echo "8. Protected route (no auth - expect 401):"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/users/profile
echo ""
echo "9. Protected route (invalid JWT - expect 401):"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer invalid.jwt.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ" http://$IP:8080/users/profile
echo ""
echo "10. Protected route (valid JWT - expect 200 or 503 if user-service down):"
curl -s -o /dev/null -w "%{http_code}" -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0LXVzZXItaWQiLCJyb2xlcyI6WyJDVVNUT01FUiJdfQ.rS1OObR_6PfS9aT28rM7Bf5mE-gUakVQq8wGdHE5OeA" http://$IP:8080/users/profile
echo ""
