#!/usr/bin/env bash

# Manually exercise Feed Service resilience while controlling the Post Service
# failure simulator directly. Run one scenario at a time from a clean Feed
# Service state, especially before circuit-breaker and bulkhead scenarios.

set -u

FEED_SERVICE="${FEED_SERVICE_URL:-http://localhost:8083/feed-service}"
POST_SERVICE="${POST_SERVICE_URL:-http://localhost:8082/post-service}"
POST_CONTROL="${POST_SERVICE}/api/management/failure-simulator"
FEED_REQUEST="${FEED_SERVICE}/api/feed/user-alex"
SCENARIO="${1:-help}"

usage() {
    cat <<'EOF'
Usage: ./test-resilience-patterns.sh <timeout|retry|circuit-breaker|bulkhead>

Feed request: GET /api/feed/user-alex.
Simulator control calls go directly to Post Service. The tested business call
goes through Feed Service and ResilientPostService.

For an isolated circuit-breaker or bulkhead demonstration, restart Feed Service
before running the scenario so its circuit-breaker state starts CLOSED.
EOF
}

post_control() {
    curl --silent --show-error --fail-with-body -X POST "$1"
}

get_status() {
    curl --silent --show-error "${POST_CONTROL}/status"
}

summarize_circuit_breaker() {
    local snapshot
    local state
    local blocked
    local slow_failed
    snapshot=$(curl --silent --show-error "${FEED_SERVICE}/actuator/circuitbreakers") || {
        echo "Could not read Circuit Breaker state from Feed Service."
        return
    }

    state=$(printf '%s\n' "$snapshot" | sed -n 's/.*"state"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
    blocked=$(printf '%s\n' "$snapshot" | sed -n 's/.*"notPermittedCalls"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p')
    slow_failed=$(printf '%s\n' "$snapshot" | sed -n 's/.*"slowFailedCalls"[[:space:]]*:[[:space:]]*\([0-9][0-9]*\).*/\1/p')
    state=${state:-UNKNOWN}
    blocked=${blocked:-0}
    slow_failed=${slow_failed:-0}

    echo "Circuit Breaker: ${state}"
    echo "Slow failed calls: ${slow_failed}"
    echo "Requests rejected by the open Circuit Breaker: ${blocked}"
    if [ "$state" = "OPEN" ] && [ "$blocked" -gt 0 ]; then
        echo "Interpretation: the Circuit Breaker rejected these requests before they could demonstrate the bulkhead."
    else
        echo "Interpretation: the Circuit Breaker did not report open-state rejections; inspect request timings and bulkhead metrics for this run."
    fi
}

call_feed() {
    echo "GET ${FEED_REQUEST}"
    curl --silent --show-error --write-out '\nHTTP %{http_code} in %{time_total}s\n' \
        "$FEED_REQUEST" || true
    echo
}

cleanup() {
    echo
    echo "Resetting Post Service failure simulator..."
    if post_control "${POST_CONTROL}/reset"; then
        printf "\n"
        echo "Post Service simulator restored to normal behavior (500 ms default delay; failures and slowness disabled)."
    else
        printf "\n"
        echo "Could not reset Post Service simulator. Check that Post Service is running, then call:"
        echo "  curl -X POST \"${POST_CONTROL}/reset\""
    fi
}

trap cleanup EXIT

case "$SCENARIO" in
    timeout)
        echo "Add 3 seconds of latency at Post Service; Feign uses a 2-second read timeout."
        echo "Retries may make the overall request last longer than 2 seconds."
        post_control "${POST_CONTROL}/slowness/start?delayMs=3000"
        echo
        call_feed
        echo
        get_status
        ;;

    retry)
        echo "Inject two Post Service 503 failures. The configured maximum is three attempts."
        post_control "${POST_CONTROL}/failure/start?count=2"
        echo
        call_feed
        echo "Post Service simulator status (request count shows downstream attempts):"
        get_status
        echo
        ;;

    circuit-breaker)
        echo "Inject repeated Post Service failures, then observe the Feed Service breaker."
        echo "Restart Feed Service first if you need to ensure the breaker starts CLOSED."
        post_control "${POST_CONTROL}/failure/start?count=20"
        echo
        for i in 1 2 3 4 5; do
            printf "\n================================================================\n"
            echo "Request ${i}"
            call_feed
            curl --silent --show-error \
                "${FEED_SERVICE}/actuator/circuitbreakers"
            echo
            printf "\n\n"
            get_status
            
            sleep 1
        done
        printf "\n\n"
        echo "Stopping simulated failures directly on Post Service."
        printf "\n"
        post_control "${POST_CONTROL}/failure/stop"
        echo
        echo "Waiting for the configured 10-second open interval, then probing recovery."
        sleep 10
        printf "\n"
        call_feed
        ;;

    bulkhead)
        echo "Add 3 seconds of Post Service latency and send 15 concurrent Feed requests."
        echo "Fast 503s may come from an open Circuit Breaker; slower 503s may follow Post Service timeouts and retries."
        post_control "${POST_CONTROL}/slowness/start?delayMs=3000"
        echo
        for i in $(seq 1 15); do
            (
                curl --silent --show-error --output /dev/null \
                    --write-out "Request ${i}: HTTP %{http_code}, %{time_total}s\n" \
                    "$FEED_REQUEST" || true
            ) &
        done
        wait
        echo
        echo "Resilience result:"
        summarize_circuit_breaker
        echo "Bulkhead actuator endpoint lists configured instances; it does not provide rejection counts."
        ;;

    *)
        usage
        ;;
esac
