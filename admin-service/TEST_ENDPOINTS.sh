#!/bin/bash

# Admin Service Testing Script
# Base URL for admin service
BASE_URL="http://16.171.12.89:8089/admin/api/v1"

echo "=========================================="
echo "Admin Service Endpoint Tests"
echo "=========================================="
echo ""

# 1. Health Check
echo "1. Testing Health Check"
echo "Command: curl -X POST $BASE_URL/health"
curl -X POST "$BASE_URL/health" | jq '.'
echo -e "\n"

# 2. Create Audit Log
echo "2. Creating Audit Log (User created)"
echo "Command: curl -X POST '$BASE_URL/audit-logs' -G -d adminId=1 -d action=CREATE -d resourceType=USER -d resourceId=101 -d details='New user created' -d ipAddress=192.168.1.1"
curl -X POST "$BASE_URL/audit-logs" -G \
  -d "adminId=1" \
  -d "action=CREATE" \
  -d "resourceType=USER" \
  -d "resourceId=101" \
  -d "details=New user created with email admin@test.com" \
  -d "ipAddress=192.168.1.1" | jq '.'
echo -e "\n"

# 3. Create Another Audit Log
echo "3. Creating Audit Log (Product updated)"
echo "Command: curl -X POST '$BASE_URL/audit-logs' -G -d adminId=1 -d action=UPDATE -d resourceType=PRODUCT -d resourceId=501 -d details='Product price changed' -d ipAddress=192.168.1.1"
curl -X POST "$BASE_URL/audit-logs" -G \
  -d "adminId=1" \
  -d "action=UPDATE" \
  -d "resourceType=PRODUCT" \
  -d "resourceId=501" \
  -d "details=Product price changed from 99.99 to 79.99" \
  -d "ipAddress=192.168.1.1" | jq '.'
echo -e "\n"

# 4. Create Audit Log (Order deleted)
echo "4. Creating Audit Log (Order cancelled)"
curl -X POST "$BASE_URL/audit-logs" -G \
  -d "adminId=2" \
  -d "action=DELETE" \
  -d "resourceType=ORDER" \
  -d "resourceId=1001" \
  -d "details=Fraudulent order cancelled" \
  -d "ipAddress=192.168.1.2" | jq '.'
echo -e "\n"

# 5. Record System Metric
echo "5. Recording System Metric (CPU Usage)"
echo "Command: curl -X POST '$BASE_URL/metrics' -G -d metricType=CPU_USAGE -d value=45.5 -d unit=percent"
curl -X POST "$BASE_URL/metrics" -G \
  -d "metricType=CPU_USAGE" \
  -d "value=45.5" \
  -d "unit=percent" | jq '.'
echo -e "\n"

# 6. Record Memory Metric
echo "6. Recording System Metric (Memory Usage)"
curl -X POST "$BASE_URL/metrics" -G \
  -d "metricType=MEMORY_USAGE" \
  -d "value=2048.75" \
  -d "unit=MB" | jq '.'
echo -e "\n"

# 7. Record Transaction Metric
echo "7. Recording System Metric (Daily Transactions)"
curl -X POST "$BASE_URL/metrics" -G \
  -d "metricType=DAILY_TRANSACTIONS" \
  -d "value=1250.0" \
  -d "unit=count" | jq '.'
echo -e "\n"

# 8. Get Audit Logs for Admin
echo "8. Getting Audit Logs for Admin ID=1"
echo "Command: curl -X GET '$BASE_URL/audit-logs?adminId=1&page=0&size=10'"
curl -X GET "$BASE_URL/audit-logs?adminId=1&page=0&size=10" | jq '.'
echo -e "\n"

# 9. Get Audit Logs by Action
echo "9. Getting Audit Logs by Action (CREATE)"
echo "Command: curl -X GET '$BASE_URL/audit-logs/action/CREATE?page=0&size=10'"
curl -X GET "$BASE_URL/audit-logs/action/CREATE?page=0&size=10" | jq '.'
echo -e "\n"

# 10. Get Audit Logs by Resource
echo "10. Getting Audit Logs by Resource (USER/101)"
echo "Command: curl -X GET '$BASE_URL/audit-logs/resource/USER/101'"
curl -X GET "$BASE_URL/audit-logs/resource/USER/101" | jq '.'
echo -e "\n"

# 11. Get System Metrics
echo "11. Getting System Metrics (CPU_USAGE from last hour)"
HOUR_AGO=$(date -u -d '1 hour ago' +"%Y-%m-%dT%H:%M:%S")
NOW=$(date -u +"%Y-%m-%dT%H:%M:%S")
echo "Command: curl -X GET '$BASE_URL/metrics?metricType=CPU_USAGE&startTime=$HOUR_AGO&endTime=$NOW'"
curl -X GET "$BASE_URL/metrics?metricType=CPU_USAGE&startTime=$HOUR_AGO&endTime=$NOW" | jq '.'
echo -e "\n"

# 12. Get Recent Metrics
echo "12. Getting Recent Metrics (last 30 minutes)"
THIRTY_MIN_AGO=$(date -u -d '30 minutes ago' +"%Y-%m-%dT%H:%M:%S")
echo "Command: curl -X GET '$BASE_URL/metrics/recent?since=$THIRTY_MIN_AGO'"
curl -X GET "$BASE_URL/metrics/recent?since=$THIRTY_MIN_AGO" | jq '.'
echo -e "\n"

# 13. Dashboard Overview
echo "13. Getting Dashboard Overview"
echo "Command: curl -X GET '$BASE_URL/dashboard/overview'"
curl -X GET "$BASE_URL/dashboard/overview" | jq '.'
echo -e "\n"

echo "=========================================="
echo "Testing Complete!"
echo "=========================================="
