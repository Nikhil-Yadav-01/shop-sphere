#!/bin/bash
IP="${1:-localhost}"
echo "=== Testing Gateway Routes ==="
echo ""
echo "1. Gateway health:"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/actuator/health
echo ""
echo "2. Gateway routes:"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/actuator/gateway/routes
echo ""
echo "3. /auth/actuator/health:"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/auth/actuator/health
echo ""
echo "4. /auth/login:"
curl -s -o /dev/null -w "%{http_code}" -X POST http://$IP:8080/auth/login -H "Content-Type: application/json" -d '{"email":"test@test.com","password":"test"}'
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
