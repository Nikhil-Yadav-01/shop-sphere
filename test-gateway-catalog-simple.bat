@echo off
echo ========================================
echo CATALOG-GATEWAY INTEGRATION TEST
echo ========================================

echo.
echo [TEST 1] Gateway Health Check
curl -s http://localhost:8080/actuator/health
echo.

echo.
echo [TEST 2] Gateway Routes Configuration
curl -s http://localhost:8080/actuator/gateway/routes | findstr catalog
echo.

echo.
echo [TEST 3] Testing Catalog Route (Expected: Service Unavailable)
curl -s -w "Status: %%{http_code}\n" http://localhost:8080/catalog/api/v1/products
echo.

echo.
echo [TEST 4] Testing Direct Gateway Route Mapping
curl -s -w "Status: %%{http_code}\n" http://localhost:8080/catalog/health
echo.

echo.
echo ========================================
echo GATEWAY CONFIGURATION ANALYSIS
echo ========================================
echo Gateway is running on port 8080
echo Catalog service route: /catalog/** -> lb://CATALOG-SERVICE
echo Route includes stripPrefix(1) filter
echo.
echo Expected behavior:
echo - /catalog/api/v1/products -> CATALOG-SERVICE/api/v1/products
echo - Service unavailable (503) when catalog service is down
echo ========================================