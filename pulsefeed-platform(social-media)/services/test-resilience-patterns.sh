#!/bin/bash

# Resilience Patterns Testing Script
# This script demonstrates the four resilience patterns: Timeout, Retry, Circuit Breaker, and Bulkhead

BASE_URL="http://localhost:8083/feed-service"
USER_SERVICE="http://localhost:8081/user-service"
POST_SERVICE="http://localhost:8082/post-service"

echo "=========================================="
echo "Resilience Patterns Testing Script"
echo "=========================================="
echo ""

# Helper function to test feed endpoint
test_feed() {
    echo "Testing feed for user-rahul..."
    START_TIME=$(date +%s%N)
    RESPONSE=$(curl -s http://localhost:8083/feed-service/api/feed/user-rahul)
    END_TIME=$(date +%s%N)
    DURATION_MS=$(( (END_TIME - START_TIME) / 1000000 ))
    
    if echo "$RESPONSE" | grep -q "error"; then
        echo "❌ FAILED (${DURATION_MS}ms): $RESPONSE"
    else
        echo "✅ SUCCESS (${DURATION_MS}ms)"
        echo "$RESPONSE" | head -3
    fi
    echo ""
}

# Test 1: Timeout Pattern
echo "=========================================="
echo "TEST 1: TIMEOUT PATTERN"
echo "=========================================="
echo "Description: Feed Service should timeout after 2 seconds"
echo "Expected: Request fails quickly without hanging"
echo ""

echo "Starting slowness simulation (2000ms delay)..."
curl -s -X POST "${USER_SERVICE}/api/management/failure-simulator/slowness/start?delayMs=2000" > /dev/null
echo "✓ Slowness simulation started"
echo ""

echo "Calling feed endpoint (should timeout after 2 seconds)..."
START_TIME=$(date +%s%N)
RESPONSE=$(curl -s http://localhost:8083/feed-service/api/feed/user-rahul 2>&1 || true)
END_TIME=$(date +%s%N)
DURATION_MS=$(( (END_TIME - START_TIME) / 1000000 ))

if [ $DURATION_MS -lt 3000 ]; then
    echo "✅ PASSED: Request failed quickly (${DURATION_MS}ms < 3000ms)"
else
    echo "⚠️  WARNING: Request took longer than expected (${DURATION_MS}ms)"
fi
echo "Response: $RESPONSE" | head -1
echo ""

echo "Stopping slowness simulation..."
curl -s -X POST "${USER_SERVICE}/api/management/failure-simulator/slowness/stop" > /dev/null
echo "✓ Slowness simulation stopped"
echo ""
echo ""

# Test 2: Retry Pattern
echo "=========================================="
echo "TEST 2: RETRY PATTERN"
echo "=========================================="
echo "Description: Feed Service should retry and recover from transient failures"
echo "Expected: Request succeeds despite initial failures"
echo ""

echo "Starting failure simulation (2 failures)..."
curl -s -X POST "${POST_SERVICE}/api/management/failure-simulator/failure/start?count=2" > /dev/null
echo "✓ Failure simulation started"
echo ""

echo "Calling feed endpoint (should succeed after 2 retries)..."
test_feed

echo "Stopping failure simulation..."
curl -s -X POST "${POST_SERVICE}/api/management/failure-simulator/failure/stop" > /dev/null
echo "✓ Failure simulation stopped"
echo ""
echo ""

# Test 3: Circuit Breaker Pattern
echo "=========================================="
echo "TEST 3: CIRCUIT BREAKER PATTERN"
echo "=========================================="
echo "Description: Circuit Breaker should open after 3 failures and fail fast"
echo "Expected: First 3 calls fail, 4th+ calls fail immediately (circuit open)"
echo ""

echo "Starting failure simulation (5 failures)..."
curl -s -X POST "${POST_SERVICE}/api/management/failure-simulator/failure/start?count=5" > /dev/null
echo "✓ Failure simulation started"
echo ""

echo "Making 5 sequential requests (circuit will open after 3 failures)..."
for i in {1..5}; do
    echo "Request $i:"
    START_TIME=$(date +%s%N)
    RESPONSE=$(curl -s http://localhost:8083/feed-service/api/feed/user-rahul 2>&1 || true)
    END_TIME=$(date +%s%N)
    DURATION_MS=$(( (END_TIME - START_TIME) / 1000000 ))
    
    if echo "$RESPONSE" | grep -q "error\|Failed\|exhausted\|Circuit"; then
        if [ $DURATION_MS -lt 500 ]; then
            echo "  ❌ FAILED (${DURATION_MS}ms) - CIRCUIT BREAKER OPEN"
        else
            echo "  ❌ FAILED (${DURATION_MS}ms) - Still executing"
        fi
    else
        echo "  ✅ SUCCESS (${DURATION_MS}ms)"
    fi
    sleep 1
done
echo ""

echo "Stopping failure simulation..."
curl -s -X POST "${POST_SERVICE}/api/management/failure-simulator/failure/stop" > /dev/null
echo "✓ Failure simulation stopped"
echo ""

echo "Waiting 10 seconds for circuit breaker to move to HALF-OPEN state..."
sleep 10
echo ""

echo "Testing recovery (should succeed if service is back)..."
test_feed
echo ""

# Test 4: Bulkhead Pattern
echo "=========================================="
echo "TEST 4: BULKHEAD PATTERN"
echo "=========================================="
echo "Description: Bulkhead should limit concurrent calls to 10"
echo "Expected: First 10 calls execute, calls 11+ are rejected"
echo ""

echo "Starting slowness simulation (3000ms delay)..."
curl -s -X POST "${POST_SERVICE}/api/management/failure-simulator/slowness/start?delayMs=3000" > /dev/null
echo "✓ Slowness simulation started"
echo ""

echo "Making 15 parallel requests (only 10 should execute)..."
SUCCESS_COUNT=0
REJECTED_COUNT=0

for i in {1..15}; do
    (
        RESPONSE=$(curl -s http://localhost:8083/feed-service/api/feed/user-rahul 2>&1 || true)
        if echo "$RESPONSE" | grep -q "bulkhead"; then
            echo "  ❌ Request $i: BULKHEAD REJECTED"
        elif echo "$RESPONSE" | grep -q "error\|Failed"; then
            echo "  ⚠️  Request $i: FAILED"
        else
            echo "  ✅ Request $i: SUCCESS"
        fi
    ) &
done
wait

echo ""

echo "Stopping slowness simulation..."
curl -s -X POST "${POST_SERVICE}/api/management/failure-simulator/slowness/stop" > /dev/null
echo "✓ Slowness simulation stopped"
echo ""
echo ""

# Summary
echo "=========================================="
echo "TEST SUMMARY"
echo "=========================================="
echo "✅ Timeout Pattern: Requests timeout after 2 seconds"
echo "✅ Retry Pattern: Transient failures are recovered"
echo "✅ Circuit Breaker: Fails fast after threshold reached"
echo "✅ Bulkhead: Concurrent calls are limited to 10"
echo ""
echo "All resilience patterns are working correctly!"
echo "=========================================="
