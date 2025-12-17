@echo off
echo ========================================
echo CATALOG SERVICE - GATEWAY INTEGRATION TEST
echo ========================================

set GATEWAY_URL=http://localhost:8080
set CATALOG_DIRECT_URL=http://localhost:8083

echo.
echo [1] Testing Gateway Health...
curl -s %GATEWAY_URL%/actuator/health
if %errorlevel% neq 0 (
    echo ERROR: Gateway not responding
    exit /b 1
)

echo.
echo [2] Testing Catalog Service Direct Health...
curl -s %CATALOG_DIRECT_URL%/actuator/health
if %errorlevel% neq 0 (
    echo ERROR: Catalog service not responding directly
    exit /b 1
)

echo.
echo [3] Testing Gateway Routes...
curl -s %GATEWAY_URL%/actuator/gateway/routes

echo.
echo [4] Creating test product via Gateway...
curl -X POST %GATEWAY_URL%/catalog/api/v1/products ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Test Product\",\"description\":\"Gateway test product\",\"sku\":\"TEST-GW-001\",\"categoryId\":\"test-cat\",\"brand\":\"TestBrand\",\"status\":\"ACTIVE\"}"

echo.
echo [5] Getting all products via Gateway...
curl -s %GATEWAY_URL%/catalog/api/v1/products

echo.
echo [6] Searching products via Gateway...
curl -s "%GATEWAY_URL%/catalog/api/v1/products/search?keyword=Test"

echo.
echo [7] Testing product by SKU via Gateway...
curl -s %GATEWAY_URL%/catalog/api/v1/products/sku/TEST-GW-001

echo.
echo [8] Comparing Direct vs Gateway Response...
echo "Direct catalog service:"
curl -s %CATALOG_DIRECT_URL%/api/v1/products
echo.
echo "Via Gateway:"
curl -s %GATEWAY_URL%/catalog/api/v1/products

echo.
echo ========================================
echo INTEGRATION TEST COMPLETED
echo ========================================