@echo off
echo 📦 Testing Catalog Service
echo =========================

set BASE_URL=http://localhost:8083

echo.
echo 1. Testing Health...
curl -s %BASE_URL%/actuator/health
echo.

echo.
echo 2. Creating a product...
curl -s -X POST "%BASE_URL%/api/v1/products" -H "Content-Type: application/json" -d "{\"sku\": \"TEST-001\", \"name\": \"Test Product\", \"description\": \"A test product\", \"categoryId\": \"cat-1\", \"images\": [\"image1.jpg\"]}" > product_response.json
type product_response.json
echo.

echo.
echo 3. Getting product by ID...
for /f "tokens=2 delims=:" %%a in ('type product_response.json ^| findstr "\"id\""') do set PRODUCT_ID=%%a
set PRODUCT_ID=%PRODUCT_ID:"=%
set PRODUCT_ID=%PRODUCT_ID:,=%
curl -s %BASE_URL%/api/v1/products/%PRODUCT_ID%
echo.

echo.
echo 4. Creating a category...
curl -s -X POST "%BASE_URL%/api/v1/categories" -H "Content-Type: application/json" -d "{\"name\": \"Electronics\", \"description\": \"Electronic products\", \"active\": true}"
echo.

echo.
echo ✅ Catalog service testing completed!
echo.
del product_response.json 2>nul