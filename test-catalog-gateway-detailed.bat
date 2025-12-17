@echo off
setlocal enabledelayedexpansion

echo ========================================
echo CATALOG-GATEWAY INTEGRATION TEST SUITE
echo ========================================

set GATEWAY_URL=http://localhost:8080
set CATALOG_URL=http://localhost:8083
set TEST_PASSED=0
set TEST_FAILED=0

:: Function to test endpoint
:test_endpoint
set "endpoint=%~1"
set "description=%~2"
echo.
echo Testing: %description%
echo Endpoint: %endpoint%
curl -s -w "HTTP Status: %%{http_code}\n" %endpoint%
if %errorlevel% equ 0 (
    echo ✓ PASS: %description%
    set /a TEST_PASSED+=1
) else (
    echo ✗ FAIL: %description%
    set /a TEST_FAILED+=1
)
goto :eof

:: Start tests
echo.
echo === HEALTH CHECKS ===
call :test_endpoint "%GATEWAY_URL%/actuator/health" "Gateway Health"
call :test_endpoint "%CATALOG_URL%/actuator/health" "Catalog Service Health"

echo.
echo === GATEWAY CONFIGURATION ===
call :test_endpoint "%GATEWAY_URL%/actuator/gateway/routes" "Gateway Routes"

echo.
echo === CATALOG ENDPOINTS VIA GATEWAY ===
call :test_endpoint "%GATEWAY_URL%/catalog/api/v1/products" "Get Products via Gateway"

echo.
echo === DIRECT CATALOG ENDPOINTS ===
call :test_endpoint "%CATALOG_URL%/api/v1/products" "Get Products Direct"

echo.
echo === CREATE TEST DATA ===
echo Creating test product via Gateway...
curl -X POST %GATEWAY_URL%/catalog/api/v1/products ^
  -H "Content-Type: application/json" ^
  -d "{\"name\":\"Gateway Test Product\",\"description\":\"Testing gateway integration\",\"sku\":\"GW-TEST-001\",\"categoryId\":\"electronics\",\"brand\":\"TestBrand\",\"status\":\"ACTIVE\"}" ^
  -w "HTTP Status: %%{http_code}\n"

echo.
echo === CRUD OPERATIONS VIA GATEWAY ===
call :test_endpoint "%GATEWAY_URL%/catalog/api/v1/products/sku/GW-TEST-001" "Get Product by SKU"
call :test_endpoint "%GATEWAY_URL%/catalog/api/v1/products/search?keyword=Gateway" "Search Products"

echo.
echo === COMPARISON TEST ===
echo "Testing response consistency..."
curl -s %GATEWAY_URL%/catalog/api/v1/products > gateway_response.json
curl -s %CATALOG_URL%/api/v1/products > direct_response.json

fc /b gateway_response.json direct_response.json > nul
if %errorlevel% equ 0 (
    echo ✓ PASS: Gateway and Direct responses match
    set /a TEST_PASSED+=1
) else (
    echo ✗ FAIL: Gateway and Direct responses differ
    set /a TEST_FAILED+=1
)

del gateway_response.json direct_response.json 2>nul

echo.
echo ========================================
echo TEST RESULTS
echo ========================================
echo Tests Passed: %TEST_PASSED%
echo Tests Failed: %TEST_FAILED%
if %TEST_FAILED% equ 0 (
    echo ✓ ALL TESTS PASSED - Integration Working
    exit /b 0
) else (
    echo ✗ SOME TESTS FAILED - Check Configuration
    exit /b 1
)