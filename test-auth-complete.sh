#!/bin/bash

# Test script for complete auth service functionality
BASE_URL="http://localhost:8081"

echo "🔐 Testing Complete Auth Service"
echo "================================"

# Test 1: Register a new user
echo "1. Testing user registration..."
REGISTER_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }')

echo "Register Response: $REGISTER_RESPONSE"

# Extract access token
ACCESS_TOKEN=$(echo $REGISTER_RESPONSE | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)
echo "Access Token: $ACCESS_TOKEN"

# Test 2: Login
echo -e "\n2. Testing user login..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')

echo "Login Response: $LOGIN_RESPONSE"

# Test 3: Validate token
echo -e "\n3. Testing token validation..."
VALIDATE_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/validate" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "Validate Response: $VALIDATE_RESPONSE"

# Test 4: Forgot password
echo -e "\n4. Testing forgot password..."
FORGOT_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/forgot-password" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }')

echo "Forgot Password Response: $FORGOT_RESPONSE"

# Test 5: Resend verification email
echo -e "\n5. Testing resend verification email..."
RESEND_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/resend-verification" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }')

echo "Resend Verification Response: $RESEND_RESPONSE"

# Test 6: Logout
echo -e "\n6. Testing logout..."
LOGOUT_RESPONSE=$(curl -s -X POST "$BASE_URL/auth/logout" \
  -H "Authorization: Bearer $ACCESS_TOKEN")

echo "Logout Response: $LOGOUT_RESPONSE"

echo -e "\n✅ Auth service testing completed!"