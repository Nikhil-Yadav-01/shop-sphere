#!/bin/bash
IP="16.171.12.89"
echo "=== Testing Gateway Routes ==="
echo ""
echo "1. /auth/actuator/health:"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/auth/actuator/health
echo ""
echo "2. /auth/login:"
curl -s -o /dev/null -w "%{http_code}" -X POST http://$IP:8080/auth/login -H "Content-Type: application/json" -d '{"email":"test@test.com","password":"test"}'
echo ""
echo "3. /admin/actuator/health:"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/admin/actuator/health
echo ""
echo "4. /admin/api/v1/dashboard/overview:"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/admin/api/v1/dashboard/overview
echo ""
echo "5. /catalog/api/v1/categories:"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/catalog/api/v1/categories
echo ""
echo "6. /api/v1/categories:"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8080/api/v1/categories
echo ""
echo "7. Direct catalog (8083) /api/v1/categories:"
curl -s -o /dev/null -w "%{http_code}" http://$IP:8083/api/v1/categories
echo ""
