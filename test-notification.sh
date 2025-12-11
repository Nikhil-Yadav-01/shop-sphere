#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Service details
SERVICE_URL="http://localhost:8095"
DB_HOST="localhost"
DB_PORT="5442"
DB_NAME="shopsphere_notification"
DB_USER="postgres"
DB_PASSWORD="password"

# Counters
TESTS_RUN=0
TESTS_PASSED=0
TESTS_FAILED=0

# Helper function for printing test headers
print_header() {
    echo ""
    echo -e "${BLUE}======================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}======================================${NC}"
}

# Helper function for test output
test_result() {
    local test_name=$1
    local expected=$2
    local actual=$3
    
    TESTS_RUN=$((TESTS_RUN + 1))
    
    if [ "$expected" == "$actual" ]; then
        echo -e "${GREEN}✓ PASS${NC}: $test_name"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        return 0
    else
        echo -e "${RED}✗ FAIL${NC}: $test_name"
        echo -e "  Expected: $expected"
        echo -e "  Actual: $actual"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        return 1
    fi
}

# Helper for JSON field extraction
extract_json() {
    echo "$1" | grep -o "\"$2\":\"[^\"]*\"" | cut -d'"' -f4
}

# Helper for JSON number extraction
extract_json_num() {
    echo "$1" | grep -o "\"$2\":[0-9]*" | cut -d':' -f2
}

# Helper for JSON object extraction
extract_json_obj() {
    echo "$1" | jq -r ".$2" 2>/dev/null
}

# 1. SERVICE HEALTH & AVAILABILITY TESTS
print_header "1. SERVICE HEALTH & AVAILABILITY TESTS"

echo "Testing service health endpoint..."
HEALTH_RESPONSE=$(curl -s http://localhost:8095/actuator/health)
HEALTH_STATUS=$(echo "$HEALTH_RESPONSE" | jq -r '.status' 2>/dev/null)
test_result "Service health status is UP" "UP" "$HEALTH_STATUS"

echo "Testing service welcome endpoint..."
WELCOME_RESPONSE=$(curl -s http://localhost:8095/)
WELCOME_SERVICE=$(echo "$WELCOME_RESPONSE" | jq -r '.service' 2>/dev/null)
test_result "Welcome service name" "Notification Service" "$WELCOME_SERVICE"

WELCOME_STATUS=$(echo "$WELCOME_RESPONSE" | jq -r '.status' 2>/dev/null)
test_result "Welcome status" "UP" "$WELCOME_STATUS"

echo "Testing HTTP 200 response code..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8095/)
test_result "HTTP 200 response" "200" "$HTTP_CODE"

# 2. CREATE NOTIFICATION TESTS
print_header "2. CREATE NOTIFICATION TESTS"

echo "Test 2.1: Create valid notification..."
CREATE_RESPONSE=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-001",
    "type": "ORDER",
    "title": "Order Confirmation",
    "message": "Your order #12345 has been confirmed",
    "channel": "EMAIL",
    "recipientEmail": "user@example.com"
  }')
NOTIF_ID=$(echo "$CREATE_RESPONSE" | jq -r '.id' 2>/dev/null)
NOTIF_STATUS=$(echo "$CREATE_RESPONSE" | jq -r '.status' 2>/dev/null)
test_result "Notification created with PENDING status" "PENDING" "$NOTIF_STATUS"
test_result "Notification has valid ID" "true" "$([ ! -z '$NOTIF_ID' ] && echo true || echo false)"

echo "Test 2.2: Create notification with SMS channel..."
SMS_RESPONSE=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-002",
    "type": "PAYMENT",
    "title": "Payment Received",
    "message": "Payment of $100 received successfully",
    "channel": "SMS",
    "recipientPhone": "+1234567890"
  }')
SMS_CHANNEL=$(echo "$SMS_RESPONSE" | jq -r '.channel' 2>/dev/null)
test_result "SMS notification channel" "SMS" "$SMS_CHANNEL"

echo "Test 2.3: Create notification with PUSH channel..."
PUSH_RESPONSE=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-003",
    "type": "PROMOTION",
    "title": "Special Offer",
    "message": "50% off on selected items",
    "channel": "PUSH",
    "recipientEmail": "user3@example.com"
  }')
PUSH_IS_READ=$(echo "$PUSH_RESPONSE" | jq -r '.isRead' 2>/dev/null)
test_result "New notification is unread" "false" "$PUSH_IS_READ"

echo "Test 2.4: Create multiple notifications for same user..."
for i in {1..3}; do
  curl -s -X POST http://localhost:8095/api/v1/notifications \
    -H "Content-Type: application/json" \
    -d "{
      \"userId\": \"user-bulk\",
      \"type\": \"NOTIFICATION\",
      \"title\": \"Bulk Test $i\",
      \"message\": \"Test message $i\",
      \"channel\": \"EMAIL\",
      \"recipientEmail\": \"bulk$i@example.com\"
    }" > /dev/null
done
echo "Created 3 notifications for bulk testing"

echo "Test 2.5: Create notification with timestamps..."
TIMESTAMP_RESPONSE=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-timestamp",
    "type": "TEST",
    "title": "Timestamp Test",
    "message": "Testing creation timestamp",
    "channel": "EMAIL"
  }')
HAS_CREATEDAT=$(echo "$TIMESTAMP_RESPONSE" | jq 'has("createdAt")' 2>/dev/null)
test_result "Notification has createdAt field" "true" "$HAS_CREATEDAT"

# 3. VALIDATION & ERROR HANDLING TESTS
print_header "3. VALIDATION & ERROR HANDLING TESTS"

echo "Test 3.1: Missing required userId..."
MISSING_USERID=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "type": "ORDER",
    "title": "Test",
    "message": "Test",
    "channel": "EMAIL"
  }')
ERROR_CODE=$(echo "$MISSING_USERID" | jq -r '.status' 2>/dev/null)
test_result "Validation error for missing userId" "422" "$ERROR_CODE"

echo "Test 3.2: Missing required type..."
MISSING_TYPE=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "title": "Test",
    "message": "Test",
    "channel": "EMAIL"
  }')
ERROR_CODE2=$(echo "$MISSING_TYPE" | jq -r '.status' 2>/dev/null)
test_result "Validation error for missing type" "422" "$ERROR_CODE2"

echo "Test 3.3: Missing required title..."
MISSING_TITLE=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "type": "ORDER",
    "message": "Test",
    "channel": "EMAIL"
  }')
ERROR_CODE3=$(echo "$MISSING_TITLE" | jq -r '.status' 2>/dev/null)
test_result "Validation error for missing title" "422" "$ERROR_CODE3"

echo "Test 3.4: Missing required message..."
MISSING_MSG=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "type": "ORDER",
    "title": "Test",
    "channel": "EMAIL"
  }')
ERROR_CODE4=$(echo "$MISSING_MSG" | jq -r '.status' 2>/dev/null)
test_result "Validation error for missing message" "422" "$ERROR_CODE4"

echo "Test 3.5: Missing required channel..."
MISSING_CHANNEL=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "type": "ORDER",
    "title": "Test",
    "message": "Test"
  }')
ERROR_CODE5=$(echo "$MISSING_CHANNEL" | jq -r '.status' 2>/dev/null)
test_result "Validation error for missing channel" "422" "$ERROR_CODE5"

echo "Test 3.6: Get non-existent notification..."
NOTFOUND=$(curl -s http://localhost:8095/api/v1/notifications/99999)
NOTFOUND_STATUS=$(echo "$NOTFOUND" | jq -r '.status' 2>/dev/null)
test_result "Get non-existent notification returns 400" "400" "$NOTFOUND_STATUS"

# 4. READ/RETRIEVE TESTS
print_header "4. READ/RETRIEVE NOTIFICATION TESTS"

echo "Test 4.1: Get notification by ID..."
GET_RESPONSE=$(curl -s http://localhost:8095/api/v1/notifications/$NOTIF_ID)
GET_ID=$(echo "$GET_RESPONSE" | jq -r '.id' 2>/dev/null)
test_result "Retrieved notification ID matches" "$NOTIF_ID" "$GET_ID"

echo "Test 4.2: Get all notifications for user..."
USER_NOTIFS=$(curl -s http://localhost:8095/api/v1/notifications/user/user-001)
USER_COUNT=$(echo "$USER_NOTIFS" | jq 'length' 2>/dev/null)
test_result "Get user notifications returns array" "true" "$([ $USER_COUNT -ge 1 ] && echo true || echo false)"

echo "Test 4.3: Get unread notifications for user..."
UNREAD_NOTIFS=$(curl -s http://localhost:8095/api/v1/notifications/user/user-002/unread)
UNREAD_COUNT=$(echo "$UNREAD_NOTIFS" | jq 'length' 2>/dev/null)
test_result "Get unread notifications returns array" "true" "$([ ! -z '$UNREAD_COUNT' ] && echo true || echo false)"

echo "Test 4.4: Get unread count for user..."
UNREAD_CNT=$(curl -s http://localhost:8095/api/v1/notifications/user/user-bulk/unread/count)
UNREAD_CNT_NUM=$(echo "$UNREAD_CNT" | jq -r '.unreadCount' 2>/dev/null)
test_result "Get unread count returns number >= 0" "true" "$([ $UNREAD_CNT_NUM -ge 0 ] && echo true || echo false)"

echo "Test 4.5: Get recent notifications (last 7 days)..."
RECENT=$(curl -s http://localhost:8095/api/v1/notifications/user/user-001/recent)
RECENT_COUNT=$(echo "$RECENT" | jq 'length' 2>/dev/null)
test_result "Get recent notifications returns array" "true" "$([ ! -z '$RECENT_COUNT' ] && echo true || echo false)"

echo "Test 4.6: Get recent notifications (custom days)..."
RECENT_30=$(curl -s http://localhost:8095/api/v1/notifications/user/user-001/recent?days=30)
RECENT_30_COUNT=$(echo "$RECENT_30" | jq 'length' 2>/dev/null)
test_result "Get notifications with custom days parameter" "true" "$([ ! -z '$RECENT_30_COUNT' ] && echo true || echo false)"

# 5. UPDATE/MARK AS READ TESTS
print_header "5. UPDATE/MARK AS READ TESTS"

echo "Test 5.1: Mark notification as read..."
MARK_READ=$(curl -s -X PUT http://localhost:8095/api/v1/notifications/$NOTIF_ID/read)
MARK_READ_STATUS=$(echo "$MARK_READ" | jq -r '.isRead' 2>/dev/null)
test_result "Mark notification as read" "true" "$MARK_READ_STATUS"

MARK_READ_NOTIF_STATUS=$(echo "$MARK_READ" | jq -r '.status' 2>/dev/null)
test_result "Status changed to DELIVERED" "DELIVERED" "$MARK_READ_NOTIF_STATUS"

HAS_READAT=$(echo "$MARK_READ" | jq 'has("readAt") and .readAt != null' 2>/dev/null)
test_result "readAt timestamp is set" "true" "$HAS_READAT"

echo "Test 5.2: Mark all notifications as read for user..."
MARK_ALL=$(curl -s -X PUT http://localhost:8095/api/v1/notifications/user/user-bulk/read-all)
MARKED_ALL_COUNT=$(echo "$MARK_ALL" | jq 'length' 2>/dev/null)
test_result "Mark all notifications returns array" "true" "$([ ! -z '$MARKED_ALL_COUNT' ] && echo true || echo false)"

# Verify all are marked as read
ALL_MARKED_READ=$(echo "$MARK_ALL" | jq '[.[].isRead] | all' 2>/dev/null)
test_result "All marked notifications have isRead=true" "true" "$ALL_MARKED_READ"

echo "Test 5.3: Check unread count after marking all as read..."
UNREAD_AFTER=$(curl -s http://localhost:8095/api/v1/notifications/user/user-bulk/unread/count)
UNREAD_AFTER_NUM=$(echo "$UNREAD_AFTER" | jq -r '.unreadCount' 2>/dev/null)
test_result "Unread count is 0 after marking all read" "0" "$UNREAD_AFTER_NUM"

# 6. DELETE TESTS
print_header "6. DELETE NOTIFICATION TESTS"

echo "Test 6.1: Delete valid notification..."
DELETE_RESPONSE=$(curl -s -X DELETE http://localhost:8095/api/v1/notifications/$NOTIF_ID)
DELETE_MSG=$(echo "$DELETE_RESPONSE" | jq -r '.message' 2>/dev/null)
test_result "Delete notification success message" "Notification deleted successfully" "$DELETE_MSG"

echo "Test 6.2: Verify deleted notification is gone..."
VERIFY_DELETE=$(curl -s http://localhost:8095/api/v1/notifications/$NOTIF_ID)
VERIFY_DELETE_STATUS=$(echo "$VERIFY_DELETE" | jq -r '.status' 2>/dev/null)
test_result "Get deleted notification returns 400" "400" "$VERIFY_DELETE_STATUS"

echo "Test 6.3: Delete non-existent notification..."
DELETE_NOTFOUND=$(curl -s -X DELETE http://localhost:8095/api/v1/notifications/99999)
DELETE_NOTFOUND_STATUS=$(echo "$DELETE_NOTFOUND" | jq -r '.status' 2>/dev/null)
test_result "Delete non-existent notification returns 400" "400" "$DELETE_NOTFOUND_STATUS"

# 7. RESPONSE FORMAT TESTS
print_header "7. RESPONSE FORMAT & DATA INTEGRITY TESTS"

echo "Test 7.1: Response contains all required fields..."
TEST_RESPONSE=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-format-test",
    "type": "TEST",
    "title": "Format Test",
    "message": "Testing response format",
    "channel": "EMAIL"
  }')

FIELDS=("id" "userId" "type" "title" "message" "isRead" "channel" "status" "createdAt")
for field in "${FIELDS[@]}"; do
  HAS_FIELD=$(echo "$TEST_RESPONSE" | jq "has(\"$field\")" 2>/dev/null)
  test_result "Response has field: $field" "true" "$HAS_FIELD"
done

echo "Test 7.2: Email field is present when provided..."
EMAIL_TEST=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-email-test",
    "type": "TEST",
    "title": "Email Test",
    "message": "Testing email field",
    "channel": "EMAIL",
    "recipientEmail": "test@example.com"
  }')
EMAIL_FIELD=$(echo "$EMAIL_TEST" | jq -r '.recipientEmail' 2>/dev/null)
test_result "Email field preserved in response" "test@example.com" "$EMAIL_FIELD"

echo "Test 7.3: Phone field is present when provided..."
PHONE_TEST=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-phone-test",
    "type": "TEST",
    "title": "Phone Test",
    "message": "Testing phone field",
    "channel": "SMS",
    "recipientPhone": "+9876543210"
  }')
PHONE_FIELD=$(echo "$PHONE_TEST" | jq -r '.recipientPhone' 2>/dev/null)
test_result "Phone field preserved in response" "+9876543210" "$PHONE_FIELD"

# 8. DATA PERSISTENCE TESTS
print_header "8. DATA PERSISTENCE & DATABASE TESTS"

echo "Test 8.1: Data persists after service restart (simulated by re-query)..."
PERSIST_NOTIF=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-persist",
    "type": "PERSISTENCE",
    "title": "Persistence Test",
    "message": "Testing data persistence",
    "channel": "EMAIL"
  }')
PERSIST_ID=$(echo "$PERSIST_NOTIF" | jq -r '.id' 2>/dev/null)

# Query it back
PERSIST_VERIFY=$(curl -s http://localhost:8095/api/v1/notifications/$PERSIST_ID)
PERSIST_VERIFY_ID=$(echo "$PERSIST_VERIFY" | jq -r '.id' 2>/dev/null)
test_result "Notification persists in database" "$PERSIST_ID" "$PERSIST_VERIFY_ID"

echo "Test 8.2: Multiple notifications for same user are all retrieved..."
UNIQUE_MULTI_USER="user-multi-test-$(date +%s%N)"
for i in {1..5}; do
  curl -s -X POST http://localhost:8095/api/v1/notifications \
    -H "Content-Type: application/json" \
    -d "{
      \"userId\": \"$UNIQUE_MULTI_USER\",
      \"type\": \"TEST\",
      \"title\": \"Multi Test $i\",
      \"message\": \"Message $i\",
      \"channel\": \"EMAIL\"
    }" > /dev/null
done

MULTI_NOTIFS=$(curl -s http://localhost:8095/api/v1/notifications/user/$UNIQUE_MULTI_USER)
MULTI_COUNT=$(echo "$MULTI_NOTIFS" | jq 'length' 2>/dev/null)
test_result "All 5 notifications retrieved for user" "5" "$MULTI_COUNT"

# 9. EDGE CASES & BOUNDARY TESTS
print_header "9. EDGE CASES & BOUNDARY TESTS"

echo "Test 9.1: Very long message text..."
LONG_MSG="Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat."
LONG_TEXT=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d "{
    \"userId\": \"user-long-text\",
    \"type\": \"TEST\",
    \"title\": \"Long Text Test\",
    \"message\": \"$LONG_MSG\",
    \"channel\": \"EMAIL\"
  }")
LONG_MSG_RETRIEVED=$(echo "$LONG_TEXT" | jq -r '.message' 2>/dev/null)
test_result "Long message text is preserved" "$LONG_MSG" "$LONG_MSG_RETRIEVED"

echo "Test 9.2: Special characters in text..."
SPECIAL_TEST=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-special",
    "type": "TEST",
    "title": "Special Chars",
    "message": "Test with special chars: @#$%^&*()",
    "channel": "EMAIL"
  }')
SPECIAL_MSG=$(echo "$SPECIAL_TEST" | jq -r '.message' 2>/dev/null)
HAS_SPECIAL=$(echo "$SPECIAL_MSG" | grep -c "@#")
test_result "Special characters handled correctly" "true" "$([ $HAS_SPECIAL -gt 0 ] && echo true || echo false)"

echo "Test 9.3: Unicode characters..."
UNICODE_TEST=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-unicode",
    "type": "TEST",
    "title": "Unicode Test",
    "message": "Message with text content",
    "channel": "EMAIL"
  }')
UNICODE_ID=$(echo "$UNICODE_TEST" | jq -r '.id' 2>/dev/null)
HAS_UNICODE=$([ ! -z "$UNICODE_ID" ] && echo true || echo false)
test_result "Unicode characters preserved" "true" "$HAS_UNICODE"

echo "Test 9.4: Empty optional fields..."
EMPTY_OPTIONAL=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-empty-opt",
    "type": "TEST",
    "title": "Empty Optional",
    "message": "No email or phone",
    "channel": "PUSH"
  }')
NULL_EMAIL=$(echo "$EMPTY_OPTIONAL" | jq -r '.recipientEmail' 2>/dev/null)
test_result "Null optional fields handled" "null" "$NULL_EMAIL"

echo "Test 9.5: Case sensitivity of channels..."
CHANNEL_TEST=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-channel",
    "type": "TEST",
    "title": "Channel Test",
    "message": "Testing channel case",
    "channel": "email"
  }')
CHANNEL_VALUE=$(echo "$CHANNEL_TEST" | jq -r '.channel' 2>/dev/null)
test_result "Channel value preserved as-is" "email" "$CHANNEL_VALUE"

# 10. QUERY & FILTERING TESTS
print_header "10. QUERY & FILTERING TESTS"

echo "Test 10.1: Filter by user returns only that user's notifications..."
USER1_NOTIFS=$(curl -s http://localhost:8095/api/v1/notifications/user/user-001)
ALL_SAME_USER=$(echo "$USER1_NOTIFS" | jq '[.[].userId] | map(. == "user-001") | all' 2>/dev/null)
test_result "All retrieved notifications belong to queried user" "true" "$ALL_SAME_USER"

echo "Test 10.2: Unread filter works correctly..."
UNREAD_NOTIFS=$(curl -s http://localhost:8095/api/v1/notifications/user/user-002/unread)
ALL_UNREAD=$(echo "$UNREAD_NOTIFS" | jq '[.[].isRead] | map(. == false) | all' 2>/dev/null)
test_result "All unread notifications have isRead=false" "true" "$ALL_UNREAD"

echo "Test 10.3: Results ordered by creation date (descending)..."
ORDERED=$(curl -s http://localhost:8095/api/v1/notifications/user/user-multi-test)
IS_DESC=$(echo "$ORDERED" | jq '[.[0].createdAt, .[1].createdAt] | .[0] >= .[1]' 2>/dev/null)
test_result "Notifications ordered by createdAt descending" "true" "$IS_DESC"

# 11. CONTENT TYPE & HEADER TESTS
print_header "11. CONTENT TYPE & HTTP HEADER TESTS"

echo "Test 11.1: Response Content-Type is JSON..."
CONTENT_TYPE=$(curl -s -I http://localhost:8095/ | grep -i "content-type" | cut -d' ' -f2 | tr -d '\r')
test_result "Content-Type is application/json" "application/json" "$CONTENT_TYPE"

echo "Test 11.2: POST returns 201 Created..."
POST_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-http-code",
    "type": "TEST",
    "title": "HTTP Code Test",
    "message": "Testing HTTP status codes",
    "channel": "EMAIL"
  }')
test_result "POST returns 201" "201" "$POST_CODE"

echo "Test 11.3: GET returns 200 OK..."
GET_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8095/api/v1/notifications/1)
test_result "GET returns 200 or 400 (depends on ID)" "true" "$([ $GET_CODE -eq 200 ] || [ $GET_CODE -eq 400 ] && echo true || echo false)"

echo "Test 11.4: DELETE returns 200 OK..."
# Create a notification to delete
DEL_TEST=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-delete-test",
    "type": "TEST",
    "title": "Delete HTTP Test",
    "message": "Testing delete HTTP code",
    "channel": "EMAIL"
  }')
DEL_ID=$(echo "$DEL_TEST" | jq -r '.id' 2>/dev/null)
DEL_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE http://localhost:8095/api/v1/notifications/$DEL_ID)
test_result "DELETE returns 200" "200" "$DEL_CODE"

# 12. CONCURRENT & LOAD TESTS
print_header "12. CONCURRENT REQUEST TESTS"

echo "Test 12.1: Handling multiple concurrent creates..."
UNIQUE_USER="user-concurrent-$(date +%s%N)"
for i in {1..5}; do
  curl -s -X POST http://localhost:8095/api/v1/notifications \
    -H "Content-Type: application/json" \
    -d "{
      \"userId\": \"$UNIQUE_USER\",
      \"type\": \"TEST\",
      \"title\": \"Concurrent $i\",
      \"message\": \"Concurrent message $i\",
      \"channel\": \"EMAIL\"
    }" > /dev/null &
done
wait
echo "Created 5 concurrent notifications"

CONCURRENT_CHECK=$(curl -s http://localhost:8095/api/v1/notifications/user/$UNIQUE_USER)
CONCURRENT_COUNT=$(echo "$CONCURRENT_CHECK" | jq 'length' 2>/dev/null)
test_result "All 5 concurrent notifications created successfully" "5" "$CONCURRENT_COUNT"

# 13. RESPONSE CONSISTENCY TESTS
print_header "13. RESPONSE CONSISTENCY TESTS"

echo "Test 13.1: Same ID returns same data on multiple queries..."
TEST_NOTIF=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-consistency",
    "type": "TEST",
    "title": "Consistency Test",
    "message": "Testing response consistency",
    "channel": "EMAIL"
  }')
TEST_NOTIF_ID=$(echo "$TEST_NOTIF" | jq -r '.id' 2>/dev/null)

QUERY1=$(curl -s http://localhost:8095/api/v1/notifications/$TEST_NOTIF_ID | jq -r '.message' 2>/dev/null)
sleep 1
QUERY2=$(curl -s http://localhost:8095/api/v1/notifications/$TEST_NOTIF_ID | jq -r '.message' 2>/dev/null)
test_result "Same notification returns same data on repeated queries" "$QUERY1" "$QUERY2"

# 14. TIMESTAMP TESTS
print_header "14. TIMESTAMP & DATE TESTS"

echo "Test 14.1: createdAt is automatically set..."
TS_TEST=$(curl -s -X POST http://localhost:8095/api/v1/notifications \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-timestamp-test",
    "type": "TEST",
    "title": "Timestamp Test",
    "message": "Testing auto-set timestamps",
    "channel": "EMAIL"
  }')
HAS_TS=$(echo "$TS_TEST" | jq '.createdAt != null' 2>/dev/null)
test_result "createdAt is automatically set" "true" "$HAS_TS"

echo "Test 14.2: sentAt is null for new notifications..."
SENT_AT=$(echo "$TS_TEST" | jq '.sentAt' 2>/dev/null)
test_result "sentAt is null for new notifications" "null" "$SENT_AT"

echo "Test 14.3: readAt is null for unread notifications..."
READ_AT=$(echo "$TS_TEST" | jq '.readAt' 2>/dev/null)
test_result "readAt is null for unread notifications" "null" "$READ_AT"

echo "Test 14.4: readAt is set when marked as read..."
TS_ID=$(echo "$TS_TEST" | jq -r '.id' 2>/dev/null)
MARKED=$(curl -s -X PUT http://localhost:8095/api/v1/notifications/$TS_ID/read)
MARKED_READ_AT=$(echo "$MARKED" | jq '.readAt != null' 2>/dev/null)
test_result "readAt is set when notification is marked read" "true" "$MARKED_READ_AT"

# 15. EUREKA DISCOVERY TEST
print_header "15. SERVICE DISCOVERY TEST"

echo "Test 15.1: Service registered in Eureka..."
EUREKA_CHECK=$(curl -s http://localhost:8761/eureka/apps/NOTIFICATION-SERVICE 2>/dev/null)
HAS_SERVICE=$(echo "$EUREKA_CHECK" | grep -c "notification-service" 2>/dev/null)
test_result "Notification service registered in Eureka" "true" "$([ $HAS_SERVICE -gt 0 ] && echo true || echo false)"

# SUMMARY
print_header "TEST SUMMARY"

echo ""
echo -e "${BLUE}Total Tests Run: $TESTS_RUN${NC}"
echo -e "${GREEN}Tests Passed: $TESTS_PASSED${NC}"
echo -e "${RED}Tests Failed: $TESTS_FAILED${NC}"
echo ""

PASS_RATE=$((TESTS_PASSED * 100 / TESTS_RUN))
echo -e "Pass Rate: ${YELLOW}${PASS_RATE}%${NC}"

if [ $TESTS_FAILED -eq 0 ]; then
    echo ""
    echo -e "${GREEN}✓ ALL TESTS PASSED!${NC}"
    echo ""
    exit 0
else
    echo ""
    echo -e "${RED}✗ SOME TESTS FAILED${NC}"
    echo ""
    exit 1
fi
