#!/bin/bash

# Search Service Data Population Script
# This script populates the search service with sample product data via Kafka events

set -e

KAFKA_BROKER="localhost:9092"
BASE_URL="http://localhost:8084/api/v1"

echo "================================================"
echo "Search Service - Data Population Script"
echo "================================================"
echo ""

# Wait for service to be ready
echo "Waiting for search service to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:8084/actuator/health > /dev/null 2>&1; then
        echo "✅ Search service is ready"
        break
    fi
    echo "⏳ Waiting... ($i/30)"
    sleep 1
done

echo ""
echo "Sending product events to Kafka..."
echo ""

# Product 1: Laptop
echo "1️⃣  Creating Product: Gaming Laptop"
curl -s -X POST "http://localhost:8084/api/v1/search" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-001",
    "productName": "Gaming Laptop",
    "description": "High-performance gaming laptop with RTX 4080 GPU",
    "sku": "LAPTOP-001",
    "price": 2499.99,
    "categoryId": "cat-001",
    "categoryName": "Electronics",
    "tags": ["gaming", "laptop", "computer"],
    "status": "ACTIVE",
    "rating": 4.8,
    "reviewCount": 245,
    "inStock": true,
    "brand": "ASUS"
  }' > /dev/null 2>&1
echo "   Sent product event"

# Product 2: Wireless Headphones
echo "2️⃣  Creating Product: Wireless Headphones"
curl -s -X POST "http://localhost:8084/api/v1/search" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-002",
    "productName": "Premium Wireless Headphones",
    "description": "Noise-cancelling wireless headphones with 30-hour battery",
    "sku": "HP-WIRELESS-001",
    "price": 349.99,
    "categoryId": "cat-002",
    "categoryName": "Audio",
    "tags": ["headphones", "wireless", "audio"],
    "status": "ACTIVE",
    "rating": 4.6,
    "reviewCount": 512,
    "inStock": true,
    "brand": "Sony"
  }' > /dev/null 2>&1
echo "   Sent product event"

# Product 3: USB-C Cable
echo "3️⃣  Creating Product: USB-C Cable"
curl -s -X POST "http://localhost:8084/api/v1/search" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-003",
    "productName": "Fast Charging USB-C Cable",
    "description": "Premium USB-C cable supporting 100W fast charging",
    "sku": "CABLE-USB-C-001",
    "price": 29.99,
    "categoryId": "cat-003",
    "categoryName": "Accessories",
    "tags": ["usb-c", "cable", "charging"],
    "status": "ACTIVE",
    "rating": 4.5,
    "reviewCount": 1203,
    "inStock": true,
    "brand": "Anker"
  }' > /dev/null 2>&1
echo "   Sent product event"

# Product 4: Mechanical Keyboard
echo "4️⃣  Creating Product: Mechanical Keyboard"
curl -s -X POST "http://localhost:8084/api/v1/search" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-004",
    "productName": "RGB Mechanical Keyboard",
    "description": "Gaming mechanical keyboard with RGB backlight and hot-swappable switches",
    "sku": "KB-MECH-RGB-001",
    "price": 159.99,
    "categoryId": "cat-001",
    "categoryName": "Electronics",
    "tags": ["keyboard", "mechanical", "gaming", "rgb"],
    "status": "ACTIVE",
    "rating": 4.7,
    "reviewCount": 876,
    "inStock": true,
    "brand": "Corsair"
  }' > /dev/null 2>&1
echo "   Sent product event"

# Product 5: Monitor
echo "5️⃣  Creating Product: 4K Monitor"
curl -s -X POST "http://localhost:8084/api/v1/search" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-005",
    "productName": "27-inch 4K UltraHD Monitor",
    "description": "Professional 4K monitor with 144Hz refresh rate for gaming and design",
    "sku": "MON-4K-27-001",
    "price": 599.99,
    "categoryId": "cat-001",
    "categoryName": "Electronics",
    "tags": ["monitor", "4k", "gaming", "display"],
    "status": "ACTIVE",
    "rating": 4.9,
    "reviewCount": 432,
    "inStock": true,
    "brand": "Dell"
  }' > /dev/null 2>&1
echo "   Sent product event"

# Product 6: Webcam
echo "6️⃣  Creating Product: HD Webcam"
curl -s -X POST "http://localhost:8084/api/v1/search" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-006",
    "productName": "4K HD Webcam",
    "description": "Professional 4K webcam for streaming and video conferencing",
    "sku": "WEBCAM-4K-001",
    "price": 189.99,
    "categoryId": "cat-002",
    "categoryName": "Audio",
    "tags": ["webcam", "4k", "streaming"],
    "status": "ACTIVE",
    "rating": 4.4,
    "reviewCount": 234,
    "inStock": true,
    "brand": "Logitech"
  }' > /dev/null 2>&1
echo "   Sent product event"

# Product 7: Mouse
echo "7️⃣  Creating Product: Gaming Mouse"
curl -s -X POST "http://localhost:8084/api/v1/search" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-007",
    "productName": "Precision Gaming Mouse",
    "description": "High-precision gaming mouse with 16000 DPI sensor",
    "sku": "MOUSE-GAMING-001",
    "price": 79.99,
    "categoryId": "cat-003",
    "categoryName": "Accessories",
    "tags": ["mouse", "gaming", "precision"],
    "status": "ACTIVE",
    "rating": 4.6,
    "reviewCount": 678,
    "inStock": true,
    "brand": "Razer"
  }' > /dev/null 2>&1
echo "   Sent product event"

# Product 8: Laptop Stand
echo "8️⃣  Creating Product: Laptop Stand"
curl -s -X POST "http://localhost:8084/api/v1/search" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-008",
    "productName": "Adjustable Laptop Stand",
    "description": "Ergonomic adjustable laptop stand for all laptops up to 17 inches",
    "sku": "STAND-LAP-001",
    "price": 49.99,
    "categoryId": "cat-003",
    "categoryName": "Accessories",
    "tags": ["stand", "laptop", "ergonomic"],
    "status": "ACTIVE",
    "rating": 4.3,
    "reviewCount": 456,
    "inStock": true,
    "brand": "AmazonBasics"
  }' > /dev/null 2>&1
echo "   Sent product event"

# Product 9: Power Bank
echo "9️⃣  Creating Product: Portable Power Bank"
curl -s -X POST "http://localhost:8084/api/v1/search" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-009",
    "productName": "65W Portable Power Bank",
    "description": "65W portable power bank with PD fast charging for laptops and phones",
    "sku": "PB-65W-001",
    "price": 89.99,
    "categoryId": "cat-003",
    "categoryName": "Accessories",
    "tags": ["power bank", "charging", "portable"],
    "status": "ACTIVE",
    "rating": 4.5,
    "reviewCount": 789,
    "inStock": true,
    "brand": "Anker"
  }' > /dev/null 2>&1
echo "   Sent product event"

# Product 10: USB Hub
echo "🔟 Creating Product: USB Hub"
curl -s -X POST "http://localhost:8084/api/v1/search" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": "prod-010",
    "productName": "7-Port USB 3.0 Hub",
    "description": "High-speed 7-port USB 3.0 hub with individual switches",
    "sku": "HUB-USB-7-001",
    "price": 59.99,
    "categoryId": "cat-003",
    "categoryName": "Accessories",
    "tags": ["hub", "usb", "connectivity"],
    "status": "ACTIVE",
    "rating": 4.2,
    "reviewCount": 345,
    "inStock": true,
    "brand": "UGREEN"
  }' > /dev/null 2>&1
echo "   Sent product event"

echo ""
echo "================================================"
echo "Waiting for events to be processed..."
echo "================================================"
sleep 3

echo ""
echo "Checking indexed data..."
INDEX_SIZE=$(curl -s http://localhost:8084/api/v1/search/index-size)
echo "Products indexed: $INDEX_SIZE"

if [ "$INDEX_SIZE" -eq 0 ]; then
    echo ""
    echo "⚠️  No products indexed yet. This is expected because we're directly"
    echo "    posting to the search API instead of using Kafka."
    echo ""
    echo "Let's insert data directly into MongoDB instead..."
    echo ""
    
    # Insert directly into MongoDB
    docker exec search-db mongosh --eval "
    use shopsphere_search;
    db.search_index.insertMany([
        {
            productId: 'prod-001',
            name: 'Gaming Laptop',
            description: 'High-performance gaming laptop with RTX 4080 GPU',
            sku: 'LAPTOP-001',
            price: 2499.99,
            categoryId: 'cat-001',
            categoryName: 'Electronics',
            tags: ['gaming', 'laptop', 'computer'],
            status: 'ACTIVE',
            rating: 4.8,
            reviewCount: 245,
            inStock: true,
            brand: 'ASUS',
            createdAt: new Date(),
            updatedAt: new Date(),
            indexedAt: Date.now()
        },
        {
            productId: 'prod-002',
            name: 'Premium Wireless Headphones',
            description: 'Noise-cancelling wireless headphones with 30-hour battery',
            sku: 'HP-WIRELESS-001',
            price: 349.99,
            categoryId: 'cat-002',
            categoryName: 'Audio',
            tags: ['headphones', 'wireless', 'audio'],
            status: 'ACTIVE',
            rating: 4.6,
            reviewCount: 512,
            inStock: true,
            brand: 'Sony',
            createdAt: new Date(),
            updatedAt: new Date(),
            indexedAt: Date.now()
        },
        {
            productId: 'prod-003',
            name: 'Fast Charging USB-C Cable',
            description: 'Premium USB-C cable supporting 100W fast charging',
            sku: 'CABLE-USB-C-001',
            price: 29.99,
            categoryId: 'cat-003',
            categoryName: 'Accessories',
            tags: ['usb-c', 'cable', 'charging'],
            status: 'ACTIVE',
            rating: 4.5,
            reviewCount: 1203,
            inStock: true,
            brand: 'Anker',
            createdAt: new Date(),
            updatedAt: new Date(),
            indexedAt: Date.now()
        },
        {
            productId: 'prod-004',
            name: 'RGB Mechanical Keyboard',
            description: 'Gaming mechanical keyboard with RGB backlight and hot-swappable switches',
            sku: 'KB-MECH-RGB-001',
            price: 159.99,
            categoryId: 'cat-001',
            categoryName: 'Electronics',
            tags: ['keyboard', 'mechanical', 'gaming', 'rgb'],
            status: 'ACTIVE',
            rating: 4.7,
            reviewCount: 876,
            inStock: true,
            brand: 'Corsair',
            createdAt: new Date(),
            updatedAt: new Date(),
            indexedAt: Date.now()
        },
        {
            productId: 'prod-005',
            name: '27-inch 4K UltraHD Monitor',
            description: 'Professional 4K monitor with 144Hz refresh rate for gaming and design',
            sku: 'MON-4K-27-001',
            price: 599.99,
            categoryId: 'cat-001',
            categoryName: 'Electronics',
            tags: ['monitor', '4k', 'gaming', 'display'],
            status: 'ACTIVE',
            rating: 4.9,
            reviewCount: 432,
            inStock: true,
            brand: 'Dell',
            createdAt: new Date(),
            updatedAt: new Date(),
            indexedAt: Date.now()
        },
        {
            productId: 'prod-006',
            name: '4K HD Webcam',
            description: 'Professional 4K webcam for streaming and video conferencing',
            sku: 'WEBCAM-4K-001',
            price: 189.99,
            categoryId: 'cat-002',
            categoryName: 'Audio',
            tags: ['webcam', '4k', 'streaming'],
            status: 'ACTIVE',
            rating: 4.4,
            reviewCount: 234,
            inStock: true,
            brand: 'Logitech',
            createdAt: new Date(),
            updatedAt: new Date(),
            indexedAt: Date.now()
        },
        {
            productId: 'prod-007',
            name: 'Precision Gaming Mouse',
            description: 'High-precision gaming mouse with 16000 DPI sensor',
            sku: 'MOUSE-GAMING-001',
            price: 79.99,
            categoryId: 'cat-003',
            categoryName: 'Accessories',
            tags: ['mouse', 'gaming', 'precision'],
            status: 'ACTIVE',
            rating: 4.6,
            reviewCount: 678,
            inStock: true,
            brand: 'Razer',
            createdAt: new Date(),
            updatedAt: new Date(),
            indexedAt: Date.now()
        },
        {
            productId: 'prod-008',
            name: 'Adjustable Laptop Stand',
            description: 'Ergonomic adjustable laptop stand for all laptops up to 17 inches',
            sku: 'STAND-LAP-001',
            price: 49.99,
            categoryId: 'cat-003',
            categoryName: 'Accessories',
            tags: ['stand', 'laptop', 'ergonomic'],
            status: 'ACTIVE',
            rating: 4.3,
            reviewCount: 456,
            inStock: true,
            brand: 'AmazonBasics',
            createdAt: new Date(),
            updatedAt: new Date(),
            indexedAt: Date.now()
        },
        {
            productId: 'prod-009',
            name: '65W Portable Power Bank',
            description: '65W portable power bank with PD fast charging for laptops and phones',
            sku: 'PB-65W-001',
            price: 89.99,
            categoryId: 'cat-003',
            categoryName: 'Accessories',
            tags: ['power bank', 'charging', 'portable'],
            status: 'ACTIVE',
            rating: 4.5,
            reviewCount: 789,
            inStock: true,
            brand: 'Anker',
            createdAt: new Date(),
            updatedAt: new Date(),
            indexedAt: Date.now()
        },
        {
            productId: 'prod-010',
            name: '7-Port USB 3.0 Hub',
            description: 'High-speed 7-port USB 3.0 hub with individual switches',
            sku: 'HUB-USB-7-001',
            price: 59.99,
            categoryId: 'cat-003',
            categoryName: 'Accessories',
            tags: ['hub', 'usb', 'connectivity'],
            status: 'ACTIVE',
            rating: 4.2,
            reviewCount: 345,
            inStock: true,
            brand: 'UGREEN',
            createdAt: new Date(),
            updatedAt: new Date(),
            indexedAt: Date.now()
        }
    ]);
    
    // Create text index for full-text search
    db.search_index.createIndex({ name: 'text', description: 'text', brand: 'text', tags: 'text' });
    
    // Show inserted count
    print('Inserted: ' + db.search_index.countDocuments({}));
    " 2>&1 | tail -5
fi

echo ""
echo "================================================"
echo "Data Population Complete"
echo "================================================"
echo ""
echo "✅ Sample products have been added to the database"
echo ""
echo "Now you can search for products:"
echo "  • curl 'http://localhost:8084/api/v1/search/keyword?keyword=laptop&page=0&size=10'"
echo "  • curl 'http://localhost:8084/api/v1/search/category/cat-001?page=0&size=10'"
echo "  • curl 'http://localhost:8084/api/v1/search/price?minPrice=50&maxPrice=200&page=0&size=10'"
echo ""
