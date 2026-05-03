#!/bin/bash

# Configuration
SERVICE_NAME="pricing-service"
PORT=8087

echo "Starting audit/test for $SERVICE_NAME (H2 Mode)..."

# Build
echo "Building $SERVICE_NAME..."
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

# Run in background
echo "Starting $SERVICE_NAME..."
 nohup java -jar target/$SERVICE_NAME-1.0.0.jar > pricing.log 2>&1 &
PID=$!

echo "Waiting for service to start on port $PORT..."
sleep 20

# Health check
echo "Checking health..."
curl -f http://localhost:$PORT/actuator/health
if [ $? -eq 0 ]; then
    echo "Service is UP!"
else
    echo "Service failed to start or is unhealthy. Check pricing.log"
    kill $PID
    exit 1
fi

# Clean up
echo "Stopping service..."
kill $PID
echo "Audit complete."
