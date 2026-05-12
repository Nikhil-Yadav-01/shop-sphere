#!/bin/bash
set -e
IP="51.20.189.129"
echo "=== Testing Batch Service ==="
echo ""

echo "1. Health Check:"
curl -s http://$IP:8091/actuator/health | grep -o '"status":"[^"]*"'
echo ""

echo "2. Get Failed Jobs:"
curl -s "http://$IP:8091/api/v1/batch/jobs/failed" | grep -o '"success":[^,]*'
echo ""

echo "3. Get Jobs by Status (COMPLETED):"
curl -s "http://$IP:8091/api/v1/batch/jobs?status=COMPLETED&page=0&size=10" | grep -o '"success":[^,]*'
echo ""

echo "4. Get Jobs by Status (PENDING):"
curl -s "http://$IP:8091/api/v1/batch/jobs?status=PENDING&page=0&size=10" | grep -o '"success":[^,]*'
echo ""

echo "5. Get Jobs by Name (NIGHTLY_REPORT):"
curl -s "http://$IP:8091/api/v1/batch/jobs/name/NIGHTLY_REPORT" | grep -o '"success":[^,]*'
echo ""

echo "6. Via API Gateway - Get Failed Jobs:"
curl -s "http://$IP:8080/batch/api/v1/batch/jobs/failed" | grep -o '"success":[^,]*'
echo ""

echo "7. Via API Gateway - Get Jobs by Status:"
curl -s "http://$IP:8080/batch/api/v1/batch/jobs?status=COMPLETED&page=0&size=10" | grep -o '"success":[^,]*'
echo ""

echo "=== Batch Service Tests Complete ==="
