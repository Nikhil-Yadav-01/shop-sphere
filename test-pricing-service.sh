#!/bin/bash

# Test script for pricing-service
# This script builds, runs, and tests the pricing service

set -e

echo "=== Pricing Service Test Script ==="

# Function to check if a port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null ; then
        echo "Port $port is already in use. Please stop the service using that port or choose a different port."
        exit 1
    fi
}

# Function to wait for service to be ready
wait_for_service() {
    local url=$1
    local max_attempts=30
    local attempt=1

    echo "Waiting for service to be ready at $url..."
    while [ $attempt -le $max_attempts ]; do
        if curl -s --max-time 5 "$url" >/dev/null 2>&1; then
            echo "Service is ready!"
            return 0
        fi
        echo "Attempt $attempt/$max_attempts: Service not ready yet..."
        sleep 2
        ((attempt++))
    done

    echo "Service failed to start within expected time"
    return 1
}

# Check if required tools are installed
command -v mvn >/dev/null 2>&1 || { echo "Maven is not installed. Please install Maven."; exit 1; }
command -v java >/dev/null 2>&1 || { echo "Java is not installed. Please install Java."; exit 1; }
command -v curl >/dev/null 2>&1 || { echo "curl is not installed. Please install curl."; exit 1; }

# Set default values
PORT=${PORT:-8087}
DB_HOST=${DB_HOST:-localhost}
DB_PORT=${DB_PORT:-5437}
DB_NAME=${DB_NAME:-pricing_db}
DB_USERNAME=${DB_USERNAME:-postgres}
DB_PASSWORD=${DB_PASSWORD:-password}
EUREKA_URI=${EUREKA_URI:-http://localhost:8761/eureka}
KAFKA_SERVERS=${KAFKA_SERVERS:-localhost:9092}

echo "Configuration:"
echo "  Port: $PORT"
echo "  Database: $DB_HOST:$DB_PORT/$DB_NAME"
echo "  Eureka: $EUREKA_URI"
echo "  Kafka: $KAFKA_SERVERS"

# Check if port is available
check_port $PORT

# Build the service
echo "Building pricing service..."
cd pricing-service
mvn clean package -DskipTests

# Set environment variables for the service
export SERVER_PORT=$PORT
export DB_HOST=$DB_HOST
export DB_PORT=$DB_PORT
export DB_NAME=$DB_NAME
export DB_USERNAME=$DB_USERNAME
export DB_PASSWORD=$DB_PASSWORD
export EUREKA_URI=$EUREKA_URI
export SPRING_KAFKA_BOOTSTRAP_SERVERS=$KAFKA_SERVERS

# Run the service in background
echo "Starting pricing service..."
java -jar target/pricing-service.jar > pricing-service.log 2>&1 &
SERVICE_PID=$!

echo "Service started with PID: $SERVICE_PID"

# Wait for service to be ready
if wait_for_service "http://localhost:$PORT/actuator/health"; then
    echo "Service health check passed!"

    # Run basic API tests
    echo "Running API tests..."

    # Test health endpoint
    echo "Testing health endpoint..."
    HEALTH_RESPONSE=$(curl -s "http://localhost:$PORT/actuator/health")
    if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
        echo "✓ Health check passed"
    else
        echo "✗ Health check failed: $HEALTH_RESPONSE"
        kill $SERVICE_PID 2>/dev/null || true
        exit 1
    fi

    # Test welcome endpoint
    echo "Testing welcome endpoint..."
    WELCOME_RESPONSE=$(curl -s "http://localhost:$PORT/api/v1/pricing/welcome")
    if echo "$WELCOME_RESPONSE" | grep -q "Welcome"; then
        echo "✓ Welcome endpoint passed"
    else
        echo "✗ Welcome endpoint failed: $WELCOME_RESPONSE"
        kill $SERVICE_PID 2>/dev/null || true
        exit 1
    fi

    # Test getting all active prices
    echo "Testing get all active prices..."
    PRICES_RESPONSE=$(curl -s "http://localhost:$PORT/api/v1/pricing/prices")
    if [ $? -eq 0 ]; then
        echo "✓ Get prices endpoint accessible"
    else
        echo "✗ Get prices endpoint failed"
        kill $SERVICE_PID 2>/dev/null || true
        exit 1
    fi

    # Test price calculation
    echo "Testing price calculation..."
    CALC_RESPONSE=$(curl -s -X POST "http://localhost:$PORT/api/v1/pricing/calculate" \
        -H "Content-Type: application/json" \
        -d '{"productId":"PROD001","quantity":5,"additionalDiscount":0}')
    if echo "$CALC_RESPONSE" | grep -q "totalPrice"; then
        echo "✓ Price calculation passed"
    else
        echo "✗ Price calculation failed: $CALC_RESPONSE"
        kill $SERVICE_PID 2>/dev/null || true
        exit 1
    fi

    echo "All tests passed! ✓"

else
    echo "Service failed to start properly"
    echo "Checking service logs..."
    tail -20 pricing-service.log
    kill $SERVICE_PID 2>/dev/null || true
    exit 1
fi

# Cleanup
echo "Stopping service..."
kill $SERVICE_PID 2>/dev/null || true
wait $SERVICE_PID 2>/dev/null || true

echo "Test completed successfully!"
cd ..
