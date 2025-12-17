@echo off
echo Testing Catalog Service via Gateway...

echo.
echo 1. Gateway Health:
curl -s http://localhost:8080/actuator/health

echo.
echo 2. Catalog Health:
curl -s http://localhost:8083/actuator/health

echo.
echo 3. Products via Gateway:
curl -s http://localhost:8080/catalog/api/v1/products

echo.
echo 4. Create Product via Gateway:
curl -X POST http://localhost:8080/catalog/api/v1/products ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Test Item\",\"description\":\"Gateway test\",\"sku\":\"TEST-001\",\"categoryId\":\"test\",\"brand\":\"Test\",\"status\":\"ACTIVE\"}"

echo.
echo 5. Verify Product Created:
curl -s http://localhost:8080/catalog/api/v1/products/sku/TEST-001