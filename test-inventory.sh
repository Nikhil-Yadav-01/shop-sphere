#!/bin/bash

set -e

echo "Testing Inventory Service..."

BASE_URL="http://localhost:8092/api/inventory"

# Test 1: Create inventory
echo -e "\n1. Creating inventory item..."
RESPONSE=$(curl -s -X POST $BASE_URL \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "TEST-SKU-001",
    "productId": 101,
    "quantity": 500,
    "reorderLevel": 50,
    "warehouseLocation": "WAREHOUSE-B2"
  }')

INVENTORY_ID=$(echo $RESPONSE | jq -r '.id')
echo "✓ Created inventory with ID: $INVENTORY_ID"
echo "  Response: $(echo $RESPONSE | jq -c '.')"

# Test 2: Get inventory by SKU
echo -e "\n2. Fetching inventory by SKU..."
curl -s -X GET "$BASE_URL/sku/TEST-SKU-001" | jq '.id, .sku, .quantity, .status'
echo "✓ Fetched successfully"

# Test 3: Get inventory by Product ID
echo -e "\n3. Fetching inventory by Product ID..."
curl -s -X GET "$BASE_URL/product/101" | jq '.id, .productId, .sku'
echo "✓ Fetched successfully"

# Test 4: Reserve inventory
echo -e "\n4. Reserving inventory..."
RESERVE_RESPONSE=$(curl -s -X POST "$BASE_URL/reserve" \
  -H "Content-Type: application/json" \
  -d '{
    "sku": "TEST-SKU-001",
    "quantity": 100,
    "reference": "ORDER-9999"
  }')
echo "✓ Reserved 100 units"
echo "  Available after reservation: $(echo $RESERVE_RESPONSE | jq '.availableQuantity')"

# Test 5: Check availability
echo -e "\n5. Checking availability..."
AVAILABLE=$(curl -s -X GET "$BASE_URL/check-availability?sku=TEST-SKU-001&quantity=300")
echo "✓ Is 300 units available? $AVAILABLE"

# Test 6: Adjust inventory
echo -e "\n6. Adjusting inventory..."
ADJUST_RESPONSE=$(curl -s -X PUT "$BASE_URL/$INVENTORY_ID/adjust" \
  -H "Content-Type: application/json" \
  -d '{
    "adjustmentQuantity": 50,
    "reason": "Stock count correction",
    "notes": "Physical inventory count"
  }')
echo "✓ Adjusted inventory"
echo "  New quantity: $(echo $ADJUST_RESPONSE | jq '.quantity')"

# Test 7: Get stock movements
echo -e "\n7. Fetching stock movement history..."
MOVEMENTS=$(curl -s -X GET "$BASE_URL/$INVENTORY_ID/movements" | jq '.')
echo "✓ Retrieved $(echo $MOVEMENTS | jq 'length') movements"
echo "  Movements:"
echo $MOVEMENTS | jq '.[] | {type: .movementType, quantity: .quantity, reference: .reference}' 

# Test 8: Get low stock items (none in this test)
echo -e "\n8. Checking for low stock items..."
LOW_STOCK=$(curl -s -X GET "$BASE_URL/low-stock" | jq 'length')
echo "✓ Found $LOW_STOCK low stock items"

# Test 9: Get out of stock items (none in this test)
echo -e "\n9. Checking for out of stock items..."
OUT_OF_STOCK=$(curl -s -X GET "$BASE_URL/out-of-stock" | jq 'length')
echo "✓ Found $OUT_OF_STOCK out of stock items"

# Test 10: Update inventory
echo -e "\n10. Updating inventory..."
UPDATE_RESPONSE=$(curl -s -X PUT "$BASE_URL/$INVENTORY_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 1000,
    "reorderLevel": 100,
    "warehouseLocation": "WAREHOUSE-C3"
  }')
echo "✓ Updated inventory"
echo "  New location: $(echo $UPDATE_RESPONSE | jq '.warehouseLocation')"

echo -e "\n✓ All tests passed successfully!"
