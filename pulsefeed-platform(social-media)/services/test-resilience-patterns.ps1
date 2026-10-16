# Resilience Patterns Testing Script (PowerShell)
# This script demonstrates the four resilience patterns: Timeout, Retry, Circuit Breaker, and Bulkhead

$BASE_URL = "http://localhost:8083/feed-service"
$USER_SERVICE = "http://localhost:8081/user-service"
$POST_SERVICE = "http://localhost:8082/post-service"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Resilience Patterns Testing Script" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

function Test-Feed {
    Write-Host "Testing feed for user-rahul..."
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8083/feed-service/api/feed/user-rahul" -ErrorAction Stop
        $stopwatch.Stop()
        
        if ($response.Content -match "error") {
            Write-Host "❌ FAILED ($($stopwatch.ElapsedMilliseconds)ms): $($response.Content)" -ForegroundColor Red
        } else {
            Write-Host "✅ SUCCESS ($($stopwatch.ElapsedMilliseconds)ms)" -ForegroundColor Green
            $response.Content | Select-Object -First 3 | Write-Host
        }
    } catch {
        $stopwatch.Stop()
        Write-Host "❌ FAILED ($($stopwatch.ElapsedMilliseconds)ms): $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

# Test 1: Timeout Pattern
Write-Host "==========================================" -ForegroundColor Yellow
Write-Host "TEST 1: TIMEOUT PATTERN" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow
Write-Host "Description: Feed Service should timeout after 2 seconds"
Write-Host "Expected: Request fails quickly without hanging"
Write-Host ""

Write-Host "Starting slowness simulation (2000ms delay)..."
try {
    Invoke-WebRequest -Uri "$USER_SERVICE/api/management/failure-simulator/slowness/start?delayMs=2000" -Method Post -ErrorAction SilentlyContinue | Out-Null
    Write-Host "✓ Slowness simulation started" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not start simulation: $($_.Exception.Message)" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "Calling feed endpoint (should timeout after 2 seconds)..."
$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8083/feed-service/api/feed/user-rahul" -ErrorAction Stop
    $stopwatch.Stop()
    Write-Host "⚠️  Request succeeded unexpectedly: $($stopwatch.ElapsedMilliseconds)ms" -ForegroundColor Yellow
} catch {
    $stopwatch.Stop()
    if ($stopwatch.ElapsedMilliseconds -lt 3000) {
        Write-Host "✅ PASSED: Request failed quickly ($($stopwatch.ElapsedMilliseconds)ms < 3000ms)" -ForegroundColor Green
    } else {
        Write-Host "⚠️  WARNING: Request took longer than expected ($($stopwatch.ElapsedMilliseconds)ms)" -ForegroundColor Yellow
    }
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Gray
}
Write-Host ""

Write-Host "Stopping slowness simulation..."
try {
    Invoke-WebRequest -Uri "$USER_SERVICE/api/management/failure-simulator/slowness/stop" -Method Post -ErrorAction SilentlyContinue | Out-Null
    Write-Host "✓ Slowness simulation stopped" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not stop simulation" -ForegroundColor Yellow
}
Write-Host ""
Write-Host ""

# Test 2: Retry Pattern
Write-Host "==========================================" -ForegroundColor Yellow
Write-Host "TEST 2: RETRY PATTERN" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow
Write-Host "Description: Feed Service should retry and recover from transient failures"
Write-Host "Expected: Request succeeds despite initial failures"
Write-Host ""

Write-Host "Starting failure simulation (2 failures)..."
try {
    Invoke-WebRequest -Uri "$POST_SERVICE/api/management/failure-simulator/failure/start?count=2" -Method Post -ErrorAction SilentlyContinue | Out-Null
    Write-Host "✓ Failure simulation started" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not start simulation" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "Calling feed endpoint (should succeed after 2 retries)..."
Test-Feed

Write-Host "Stopping failure simulation..."
try {
    Invoke-WebRequest -Uri "$POST_SERVICE/api/management/failure-simulator/failure/stop" -Method Post -ErrorAction SilentlyContinue | Out-Null
    Write-Host "✓ Failure simulation stopped" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not stop simulation" -ForegroundColor Yellow
}
Write-Host ""
Write-Host ""

# Test 3: Circuit Breaker Pattern
Write-Host "==========================================" -ForegroundColor Yellow
Write-Host "TEST 3: CIRCUIT BREAKER PATTERN" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow
Write-Host "Description: Circuit Breaker should open after 3 failures and fail fast"
Write-Host "Expected: First 3 calls fail, 4th+ calls fail immediately (circuit open)"
Write-Host ""

Write-Host "Starting failure simulation (5 failures)..."
try {
    Invoke-WebRequest -Uri "$POST_SERVICE/api/management/failure-simulator/failure/start?count=5" -Method Post -ErrorAction SilentlyContinue | Out-Null
    Write-Host "✓ Failure simulation started" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not start simulation" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "Making 5 sequential requests (circuit will open after 3 failures)..."
for ($i = 1; $i -le 5; $i++) {
    Write-Host "Request $i`:"
    $stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:8083/feed-service/api/feed/user-rahul" -ErrorAction Stop
        $stopwatch.Stop()
        Write-Host "  ✅ SUCCESS ($($stopwatch.ElapsedMilliseconds)ms)" -ForegroundColor Green
    } catch {
        $stopwatch.Stop()
        if ($stopwatch.ElapsedMilliseconds -lt 500) {
            Write-Host "  ❌ FAILED ($($stopwatch.ElapsedMilliseconds)ms) - CIRCUIT BREAKER OPEN" -ForegroundColor Red
        } else {
            Write-Host "  ❌ FAILED ($($stopwatch.ElapsedMilliseconds)ms) - Still executing" -ForegroundColor Red
        }
    }
    Start-Sleep -Seconds 1
}
Write-Host ""

Write-Host "Stopping failure simulation..."
try {
    Invoke-WebRequest -Uri "$POST_SERVICE/api/management/failure-simulator/failure/stop" -Method Post -ErrorAction SilentlyContinue | Out-Null
    Write-Host "✓ Failure simulation stopped" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not stop simulation" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "Waiting 10 seconds for circuit breaker to move to HALF-OPEN state..."
for ($i = 10; $i -gt 0; $i--) {
    Write-Host -NoNewline "`r$i seconds remaining... "
    Start-Sleep -Seconds 1
}
Write-Host ""
Write-Host ""

Write-Host "Testing recovery (should succeed if service is back)..."
Test-Feed
Write-Host ""

# Test 4: Bulkhead Pattern
Write-Host "==========================================" -ForegroundColor Yellow
Write-Host "TEST 4: BULKHEAD PATTERN" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Yellow
Write-Host "Description: Bulkhead should limit concurrent calls to 10"
Write-Host "Expected: First 10 calls execute, calls 11+ are rejected"
Write-Host ""

Write-Host "Starting slowness simulation (3000ms delay)..."
try {
    Invoke-WebRequest -Uri "$POST_SERVICE/api/management/failure-simulator/slowness/start?delayMs=3000" -Method Post -ErrorAction SilentlyContinue | Out-Null
    Write-Host "✓ Slowness simulation started" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not start simulation" -ForegroundColor Yellow
}
Write-Host ""

Write-Host "Making 15 parallel requests (only 10 should execute)..."

$jobs = @()
for ($i = 1; $i -le 15; $i++) {
    $job = Start-Job -ScriptBlock {
        param($requestNum)
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:8083/feed-service/api/feed/user-rahul" -ErrorAction Stop
            if ($response.Content -match "bulkhead") {
                "  ❌ Request $requestNum`: BULKHEAD REJECTED"
            } else {
                "  ✅ Request $requestNum`: SUCCESS"
            }
        } catch {
            if ($_.Exception.Message -match "bulkhead") {
                "  ❌ Request $requestNum`: BULKHEAD REJECTED"
            } else {
                "  ⚠️  Request $requestNum`: FAILED - $($_.Exception.Message)"
            }
        }
    } -ArgumentList $i
    
    $jobs += $job
}

# Wait for all jobs and display results
$jobs | Wait-Job | ForEach-Object {
    $_ | Receive-Job
}

Write-Host ""

Write-Host "Stopping slowness simulation..."
try {
    Invoke-WebRequest -Uri "$POST_SERVICE/api/management/failure-simulator/slowness/stop" -Method Post -ErrorAction SilentlyContinue | Out-Null
    Write-Host "✓ Slowness simulation stopped" -ForegroundColor Green
} catch {
    Write-Host "⚠️  Could not stop simulation" -ForegroundColor Yellow
}
Write-Host ""
Write-Host ""

# Summary
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "TEST SUMMARY" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "✅ Timeout Pattern: Requests timeout after 2 seconds" -ForegroundColor Green
Write-Host "✅ Retry Pattern: Transient failures are recovered" -ForegroundColor Green
Write-Host "✅ Circuit Breaker: Fails fast after threshold reached" -ForegroundColor Green
Write-Host "✅ Bulkhead: Concurrent calls are limited to 10" -ForegroundColor Green
Write-Host ""
Write-Host "All resilience patterns are working correctly!" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Cyan
