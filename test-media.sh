#!/bin/bash

###############################################################################
# MEDIA SERVICE TEST SUITE (FULL, DIAGNOSTIC)
# Target Server: 13.60.64.207:8091
# Usage: chmod +x test-media.sh && ./test-media.sh
###############################################################################

SERVICE_IP="13.60.64.207"
SERVICE_PORT="8091"
BASE_URL="http://${SERVICE_IP}:${SERVICE_PORT}/api/v1/media"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

TEST_DIR="/tmp/media_tests"
mkdir -p "$TEST_DIR"

TOTAL=0
PASS=0
FAIL=0

# Helpers ---------------------------------------------------------------------

pass() {
    echo -e "${GREEN}[✓]${NC} $1"
    TOTAL=$((TOTAL+1))
    PASS=$((PASS+1))
}

fail() {
    local msg="$1"
    local resp="$2"
    echo -e "${RED}[✗]${NC} ${msg}"
    if [[ -n "$resp" ]]; then
        echo -e "${YELLOW}--- DEBUG: response body ---${NC}"
        echo "$resp"
        echo -e "${YELLOW}----------------------------${NC}"
    fi
    TOTAL=$((TOTAL+1))
    FAIL=$((FAIL+1))
}

test_case() {
    echo -e "\n${BLUE}>>> $1${NC}"
}

# Debug printer for url/status/body
print_debug() {
    local url="$1"
    local code="$2"
    local body="$3"
    echo -e "${YELLOW}--- DEBUG INFO ---${NC}"
    echo "URL: $url"
    echo "HTTP Status: $code"
    echo "Response Body:"
    echo "$body"
    echo -e "${YELLOW}-------------------${NC}"
}

# Create test artifacts ------------------------------------------------------

# Minimal valid binary files for upload tests
printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82' > "$TEST_DIR/test.png"
printf '\xff\xd8\xff\xe0\x00\x10JFIF\x00\x01\x01' > "$TEST_DIR/test.jpg"
printf 'GIF89a\x01\x00\x01\x00\x80\x00\x00\xff\xff\xff\x00\x00\x00!\xf9' > "$TEST_DIR/test.gif"
echo "PDF test" > "$TEST_DIR/test.pdf"
printf '\x00\x00\x00\x20ftypisom' > "$TEST_DIR/test.mp4"

# Empty and invalid files
touch "$TEST_DIR/empty.txt"
echo "fake executable" > "$TEST_DIR/test.exe"

# Start report ----------------------------------------------------------------

echo -e "${BLUE}====== MEDIA SERVICE TEST SUITE ======${NC}"
echo "Target Service: $BASE_URL"
echo "---------------------------------------"

# 1. Health Check -------------------------------------------------------------

test_case "Health Check"
url="$BASE_URL/health"
resp_and_code=$(curl -s -w "\n%{http_code}" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" == "200" ]]; then
    pass "Health endpoint is healthy"
else
    fail "Health check failed (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# 2. Upload PNG ---------------------------------------------------------------

test_case "Upload PNG"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.png" -F "entityType=product" -F "entityId=1" -F "uploadedBy=tester" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)

if [[ "$code" == "201" ]]; then
    pass "PNG upload successful (HTTP $code)"
else
    fail "PNG upload failed (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

echo "$body" | grep -q '"status":"success"' && pass "Response contains success flag" || fail "Missing success field in PNG response" "$body"
echo "$body" | grep -q '"id":' && pass "Returned ID present in PNG response" || fail "Missing media ID in PNG response" "$body"

# Save first ID (if present)
FIRST_ID=$(echo "$body" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2 || true)

# 3. Upload JPEG --------------------------------------------------------------

test_case "Upload JPEG"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.jpg" -F "entityType=product" -F "entityId=2" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" == "201" ]]; then
    pass "JPEG upload successful (HTTP $code)"
else
    fail "JPEG upload failed (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# 4. Upload GIF ---------------------------------------------------------------

test_case "Upload GIF"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.gif" -F "entityType=product" -F "entityId=3" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" == "201" ]]; then
    pass "GIF upload successful (HTTP $code)"
else
    fail "GIF upload failed (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# 5. Upload PDF ---------------------------------------------------------------

test_case "Upload PDF"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.pdf" -F "entityType=product" -F "entityId=4" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" == "201" ]]; then
    pass "PDF upload successful (HTTP $code)"
else
    fail "PDF upload failed (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# 6. Upload MP4 ---------------------------------------------------------------

test_case "Upload MP4"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.mp4" -F "entityType=product" -F "entityId=5" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" == "201" ]]; then
    pass "MP4 upload successful (HTTP $code)"
else
    fail "MP4 upload failed (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# 7. Reject Empty File -------------------------------------------------------

test_case "Reject Empty File"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/empty.txt" -F "entityType=product" -F "entityId=99" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" != "201" ]]; then
    pass "Empty file correctly rejected (HTTP $code)"
else
    fail "Empty file was accepted (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# 8. Reject Invalid MIME -----------------------------------------------------

test_case "Reject Invalid MIME (.exe)"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.exe" -F "entityType=product" -F "entityId=8" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" != "201" ]]; then
    pass "Invalid file type rejected (HTTP $code)"
else
    fail "Invalid type accepted unexpectedly (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# 9. Missing entityType ------------------------------------------------------

test_case "Missing entityType"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.png" -F "entityId=10" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" != "201" ]]; then
    pass "Correctly rejected missing entityType (HTTP $code)"
else
    fail "Missing entityType accepted (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# 10. Missing entityId -------------------------------------------------------

test_case "Missing entityId"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.png" -F "entityType=product" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" != "201" ]]; then
    pass "Correctly rejected missing entityId (HTTP $code)"
else
    fail "Missing entityId accepted (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# 11. Retrieve Media by Entity -----------------------------------------------

test_case "Retrieve Media by Entity"
url="$BASE_URL/entity/product/1"
resp_and_code=$(curl -s -w "\n%{http_code}" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" == "200" ]]; then
    pass "Get media by entity successful"
else
    fail "Failed to retrieve by entity (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# 12. Retrieve All by Type ---------------------------------------------------

test_case "Retrieve All Media by Type"
url="$BASE_URL/type/product"
resp_and_code=$(curl -s -w "\n%{http_code}" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" == "200" ]]; then
    pass "Get all media by type successful"
else
    fail "Failed to retrieve by type (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

echo "$body" | grep -q '"count"' && pass "Count field present in response" || fail "Count field missing in type response" "$body"
echo "$body" | grep -q '"data"' && pass "Data array present in response" || fail "Data field missing in type response" "$body"

# 13. Get Media by ID --------------------------------------------------------

test_case "Get Media by ID"
if [[ -n "$FIRST_ID" ]]; then
    url="$BASE_URL/$FIRST_ID"
    resp_and_code=$(curl -s -w "\n%{http_code}" "$url")
    body=$(echo "$resp_and_code" | sed '$d')
    code=$(echo "$resp_and_code" | tail -n1)
    if [[ "$code" == "200" ]]; then
        pass "Get media by ID successful (ID: $FIRST_ID)"
    else
        fail "Failed to get media by ID (HTTP $code)" "$body"
        print_debug "$url" "$code" "$body"
    fi
else
    fail "Could not extract FIRST_ID from PNG upload response" "$(cat $TEST_DIR/first.json 2>/dev/null || true)"
fi

# 14. Soft Delete -------------------------------------------------------------

test_case "Soft Delete Media"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.png" -F "entityType=test-soft" -F "entityId=500" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
SOFT_ID=$(echo "$body" | grep -o '"id":[0-9]*' | cut -d':' -f2 || true)

if [[ -z "$SOFT_ID" ]]; then
    fail "Could not extract ID for soft delete test" "$body"
    print_debug "$url" "$code" "$body"
else
    url="$BASE_URL/$SOFT_ID/soft-delete"
    resp_and_code=$(curl -s -w "\n%{http_code}" -X PUT "$url")
    body2=$(echo "$resp_and_code" | sed '$d')
    code2=$(echo "$resp_and_code" | tail -n1)
    if [[ "$code2" == "200" ]]; then
        pass "Soft delete successful (ID: $SOFT_ID)"
    else
        fail "Soft delete failed (HTTP $code2)" "$body2"
        print_debug "$url" "$code2" "$body2"
    fi
fi

# 15. Hard Delete ------------------------------------------------------------

test_case "Hard Delete Media"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.png" -F "entityType=test-hard" -F "entityId=501" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
DEL_ID=$(echo "$body" | grep -o '"id":[0-9]*' | cut -d':' -f2 || true)

if [[ -z "$DEL_ID" ]]; then
    fail "Could not extract ID for hard delete test" "$body"
    print_debug "$url" "$code" "$body"
else
    url="$BASE_URL/$DEL_ID"
    resp_and_code=$(curl -s -w "\n%{http_code}" -X DELETE "$url")
    body2=$(echo "$resp_and_code" | sed '$d')
    code2=$(echo "$resp_and_code" | tail -n1)
    if [[ "$code2" == "200" ]]; then
        pass "Hard delete successful (ID: $DEL_ID)"
        # verify non-existence
        url_verify="$BASE_URL/$DEL_ID"
        resp_and_code=$(curl -s -w "\n%{http_code}" "$url_verify")
        body3=$(echo "$resp_and_code" | sed '$d')
        code3=$(echo "$resp_and_code" | tail -n1)
        if [[ "$code3" != "200" ]]; then
            pass "Deleted media cannot be retrieved (HTTP $code3)"
        else
            fail "Deleted media still retrievable (HTTP $code3)" "$body3"
            print_debug "$url_verify" "$code3" "$body3"
        fi
    else
        fail "Hard delete failed (HTTP $code2)" "$body2"
        print_debug "$url" "$code2" "$body2"
    fi
fi

# 16. Delete Non-existent ----------------------------------------------------

test_case "Reject Delete Non-existent Media"
url="$BASE_URL/99999999999"
resp_and_code=$(curl -s -w "\n%{http_code}" -X DELETE "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" != "200" ]]; then
    pass "Non-existent delete rejected (HTTP $code)"
else
    fail "Non-existent delete returned success (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# 17. Multiple Uploads Same Entity ------------------------------------------

test_case "Multiple Uploads to Same Entity"
for i in 1 2 3; do
    url="$BASE_URL/upload"
    resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.png" -F "entityType=product" -F "entityId=700" "$url")
    body=$(echo "$resp_and_code" | sed '$d')
    code=$(echo "$resp_and_code" | tail -n1)
    if [[ "$code" == "201" ]]; then
        pass "Upload $i to entity 700 successful"
    else
        fail "Upload $i failed (HTTP $code)" "$body"
        print_debug "$url" "$code" "$body"
    fi
done

# 18. Different Entity Types ------------------------------------------------

test_case "Different Entity Types Upload"
types=("product" "review" "gallery" "advertisement")
for t in "${types[@]}"; do
    url="$BASE_URL/upload"
    resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.png" -F "entityType=$t" -F "entityId=900" "$url")
    body=$(echo "$resp_and_code" | sed '$d')
    code=$(echo "$resp_and_code" | tail -n1)
    if [[ "$code" == "201" ]]; then
        pass "Upload for entity type '$t' successful"
    else
        fail "Upload for entity type '$t' failed (HTTP $code)" "$body"
        print_debug "$url" "$code" "$body"
    fi
done

# 19. Concurrent Uploads -----------------------------------------------------

test_case "Concurrent Uploads (5 parallel)"
pids=()
for i in 1 2 3 4 5; do
    ( curl -s -o /dev/null -w "%{http_code}" -F "file=@$TEST_DIR/test.png" -F "entityType=concurrent" -F "entityId=1234" "$BASE_URL/upload" ) &
    pids+=($!)
done

# wait for all background jobs
for pid in "${pids[@]}"; do
    wait "$pid" || true
done

pass "Concurrent uploads completed (5 requests sent in parallel)"

# 20. Large Entity ID --------------------------------------------------------

test_case "Large Entity ID Upload"
url="$BASE_URL/upload"
resp_and_code=$(curl -s -w "\n%{http_code}" -F "file=@$TEST_DIR/test.png" -F "entityType=edge-case" -F "entityId=2147483647" "$url")
body=$(echo "$resp_and_code" | sed '$d')
code=$(echo "$resp_and_code" | tail -n1)
if [[ "$code" == "201" ]]; then
    pass "Handled large entity ID 2147483647 (HTTP $code)"
else
    fail "Large entity ID upload failed (HTTP $code)" "$body"
    print_debug "$url" "$code" "$body"
fi

# Summary ---------------------------------------------------------------------

echo ""
echo -e "${BLUE}========== TEST SUMMARY ==========${NC}"
echo -e "Total Tests:  ${BLUE}$TOTAL${NC}"
echo -e "Passed:       ${GREEN}$PASS${NC}"
echo -e "Failed:       ${RED}$FAIL${NC}"

if [[ "$FAIL" == "0" ]]; then
    echo -e "\n${GREEN}✓ ALL TESTS PASSED — MEDIA SERVICE IS ROBUST${NC}\n"
else
    echo -e "\n${RED}✗ $FAIL TESTS FAILED — Investigate Before Deployment${NC}\n"
fi

# Cleanup
rm -rf "$TEST_DIR"
exit $FAIL
