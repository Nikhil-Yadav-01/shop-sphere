#!/bin/bash

# ─────────────────────────────────────────────────────────────────────────────
# seed-data.sh
# 1. Register a new user
# 2. Authorize as ADMIN (via direct DB update)
# 3. Insert multiple products into Catalog Service
# 4. Initialize inventory for those products
# ─────────────────────────────────────────────────────────────────────────────

set -e

IP="localhost"
BASE_URL="https://$IP"
ADMIN_EMAIL="admin-seller-$(date +%s)@example.com"
PASSWORD="AdminPass!123"

echo "🚀 Starting Full Data Seeding Process..."

# 1. Register User
echo "Step 1: Registering user..."
REG_RESP=$(curl -k -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$ADMIN_EMAIL\",
    \"password\": \"$PASSWORD\",
    \"firstName\": \"Master\",
    \"lastName\": \"Seller\"
  }")

# 2. Verify Email (Directly from DB for speed)
echo "Step 2: Auto-verifying email in DB..."
docker exec -i auth-db psql -U postgres -d shopsphere_auth -c \
  "UPDATE email_verification_tokens SET used = true WHERE user_id = (SELECT id FROM users WHERE email = '$ADMIN_EMAIL');" > /dev/null
docker exec -i auth-db psql -U postgres -d shopsphere_auth -c \
  "UPDATE users SET email_verified = true WHERE email = '$ADMIN_EMAIL';" > /dev/null

# 3. Elevate to ADMIN
echo "Step 3: Elevating user to ADMIN..."
docker exec -i auth-db psql -U postgres -d shopsphere_auth -c \
  "UPDATE users SET role = 'ADMIN' WHERE email = '$ADMIN_EMAIL';" > /dev/null

# 4. Login to get Token
echo "Step 4: Logging in to get access token..."
LOGIN_RESP=$(curl -k -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$ADMIN_EMAIL\", \"password\": \"$PASSWORD\"}")

ACCESS_TOKEN=$(echo $LOGIN_RESP | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "❌ Failed to obtain access token"
    echo "Response: $LOGIN_RESP"
    exit 1
fi

echo "✅ Authorized. Token acquired."

# 5. Create Products & Inventory
echo "Step 5: Seeding Products and Inventory..."

# 5a. Curated Products
declare -a PRODUCTS=(
    "Laptop|ThinkPad X1|The ultimate business laptop|1499.99|electronics|https://images.unsplash.com/photo-1588872657578-7efd1f1555ed"
    "Smartphone|iPhone 15|Latest Apple smartphone|999.00|electronics|https://images.unsplash.com/photo-1695048133142-1a20484d2569"
    "Headphones|Sony WH-1000XM5|Noise cancelling headphones|348.00|audio|https://images.unsplash.com/photo-1675243935987-3c616195e3ad"
    "Coffee|Arabica Beans|Freshly roasted organic beans|18.50|grocery|https://images.unsplash.com/photo-1559056199-641a0ac8b55e"
    "Chair|Office Ergonomic|High back mesh office chair|245.00|furniture|https://images.unsplash.com/photo-1505797149-43b007662c21"
)

# 5b. Generate 100+ additional products
echo "Generating 105 bulk products..."
CATEGORIES=("electronics" "home" "grocery" "fashion" "audio" "furniture" "toys" "sports")

for i in {1..105}; do
    CAT_INDEX=$((i % ${#CATEGORIES[@]}))
    CATEGORY=${CATEGORIES[$CAT_INDEX]}
    PRICE=$(echo "scale=2; 10 + ($i * 1.5)" | bc)
    PRODUCTS+=("Bulk Product $i|Product Item $i|High quality item from category $CATEGORY|$PRICE|$CATEGORY|https://picsum.photos/seed/$i/400/300")
done

for prod in "${PRODUCTS[@]}"; do
    IFS="|" read -r NAME SHORT_NAME DESC PRICE CAT IMG <<< "$prod"
    SKU="SKU-$(echo $SHORT_NAME | tr '[:lower:]' '[:upper:]' | tr ' ' '-')"
    
    echo "  Creating Product: $SHORT_NAME ($SKU)..."
    
    # Create Product
    PROD_RESP=$(curl -k -s -X POST "$BASE_URL/catalog/api/v1/products" \
      -H "Authorization: Bearer $ACCESS_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"sku\": \"$SKU\",
        \"name\": \"$SHORT_NAME\",
        \"description\": \"$DESC\",
        \"price\": $PRICE,
        \"categoryId\": \"$CAT\",
        \"images\": [\"$IMG\"]
      }")
    
    PROD_ID=$(echo $PROD_RESP | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    if [ -n "$PROD_ID" ]; then
        echo "    ✓ Product created (ID: $PROD_ID)"
        
        # Initialize Inventory
        echo "    Initializing stock for $SKU..."
        INV_RESP=$(curl -s -X POST "http://$IP:8092/api/inventory" \
          -H "Content-Type: application/json" \
          -d "{
            \"sku\": \"$SKU\",
            \"productId\": 1,
            \"quantity\": 100,
            \"reorderLevel\": 10,
            \"warehouseLocation\": \"Main Warehouse\"
          }")
        echo "    ✓ Inventory set up."
    else
        echo "    ✗ Failed to create product: $SHORT_NAME"
        echo "    Response: $PROD_RESP"
    fi
done

echo ""
echo "✨ Seeding Complete! System is ready for testing."
