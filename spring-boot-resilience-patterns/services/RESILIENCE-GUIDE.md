# Resilience and Failure Simulation Guide

This guide documents the behavior currently implemented in the services. It replaces overlapping endpoint lists and walkthroughs that described planned or older behavior.

## Request flow

```text
Client → Feed Service → User Service (who the user follows)
                     → Post Service (posts by those authors)
```

`FeedService` orchestrates requests. `ResilientPostService` applies resilience policies to the five Post Service business operations: posts by author IDs, post detail, posts by post IDs, post count by author, and posts by author. Post Service simulator controls are called directly on Post Service, outside Feed Service. User Service client calls are direct and currently do not have a failure simulator.

## Current resilience settings

Policy values are configured in `feed-service/src/main/resources/application.yml`. `ResilientPostService.java` applies Retry, CircuitBreaker, and Bulkhead annotations to Post Service business calls. The `ResilienceConfig.java` registry listeners, when enabled, only log policy additions and removals; they do not define policies or add metrics.

| Pattern | Current behavior |
|---|---|
| Retry | Up to 3 total attempts, starting with a 100 ms wait and exponential backoff multiplier of 2. Retries are configured for selected transient I/O, timeout, and HTTP 5xx exceptions; common client errors such as 400 and 404 are ignored. |
| Circuit breaker | Count-based window of 10 calls, minimum 3 calls, 50% failure and slow-call thresholds, and a 1-second slow-call threshold. It stays open for 10 seconds, then permits 3 half-open calls. |
| Bulkhead | Up to 10 concurrent calls; a caller can wait up to 1 second for a slot. |
| HTTP client timeouts | OpenFeign is configured with a 500 ms connection timeout and 2-second read timeout. These apply to HTTP calls independently of the Resilience4j TimeLimiter. |
| TimeLimiter | A 2-second policy is configured in `application.yml`, but the current Post Service decorator does not apply it. |

The resilience annotations are applied on the wrapper methods so FeedService stays focused on orchestration. The configured TimeLimiter is not wired into these synchronous Feign calls; the HTTP client's read timeout still limits an individual response wait. Total request duration can exceed two seconds when retries and backoff occur.

If retries are exhausted or the circuit is open, `ResilientPostService` returns a controlled HTTP 503 through Spring's `ResponseStatusException`. Downstream 4xx responses retain their status. This avoids making a service outage look like a valid empty feed or a zero count.

## Control the Post Service simulator directly

The failure simulator belongs to Post Service and is available at `http://localhost:8082/post-service/api/management/failure-simulator`. Feed Service has no proxy routes for these controls. The controller provides six operations:

| Method | HTTP route | Behavior |
|---|---|---|
| `startSlowness(delayMs)` | `POST /api/management/failure-simulator/slowness/start?delayMs=500` | Enables delay before simulated Post Service operations. `delayMs` defaults to 500. |
| `stopSlowness()` | `POST /api/management/failure-simulator/slowness/stop` | Disables simulated delay. |
| `startFailure(failureCount)` | `POST /api/management/failure-simulator/failure/start?count=5` | Fails the next `count` simulated calls. `count` defaults to 5. |
| `stopFailure()` | `POST /api/management/failure-simulator/failure/stop` | Disables simulated failures and clears the remaining failure count. |
| `getStatus()` | `GET /api/management/failure-simulator/status` | Returns simulator flags, delay, and total simulated requests. |
| `reset()` | `POST /api/management/failure-simulator/reset` | Turns off simulations, restores the 500 ms default delay, and resets counters. |

Use these direct calls to control simulation, then call a Feed Service business endpoint to observe its resilience behavior:

```bash
POST_SERVICE="http://localhost:8082/post-service/api/management/failure-simulator"

curl -X POST "$POST_SERVICE/failure/start?count=2"
curl "$POST_SERVICE/status"
curl "http://localhost:8083/feed-service/api/feed/posts/batch?ids=replace-with-a-real-post-id"
curl -X POST "$POST_SERVICE/failure/stop"
curl -X POST "$POST_SERVICE/reset"
```

Replace `replace-with-a-real-post-id` with an ID returned by `GET http://localhost:8082/post-service/api/posts`. The batch call may return an empty list for a nonexistent ID; it still exercises the simulated Post Service method.

## Run the supported Post Service experiments

The examples below control the Post Service simulator directly, then send business requests through Feed Service. Run one experiment at a time and reset the simulator afterward.

### Retry after injected failures

Configure two failures, then request posts. With the default three total attempts, the request can succeed on its third attempt, provided the circuit breaker has not opened from prior traffic.

```bash
curl -X POST "http://localhost:8082/post-service/api/management/failure-simulator/failure/start?count=2"
curl "http://localhost:8083/feed-service/api/feed/user-alex"
curl -X POST "http://localhost:8082/post-service/api/management/failure-simulator/reset"
```

The seeded post identifiers are generated at startup, so `post-1` is only an example and may not exist. A missing ID can still return an empty result after a successful call. Use `/api/posts` on the Post Service to discover the generated IDs.

### Circuit breaker

Inject repeated failures and make calls through a Feed Service endpoint. The breaker evaluates calls made by the resilient wrapper; retries may produce multiple downstream attempts for one outer request. Existing breaker state and traffic affect when the threshold is reached. Inspect `/feed-service/actuator/circuitbreakers` and allow the 10-second open interval before checking recovery.

```bash
curl -X POST "http://localhost:8082/post-service/api/management/failure-simulator/failure/start?count=20"
curl "http://localhost:8083/feed-service/api/feed/user-alex"
curl "http://localhost:8083/feed-service/actuator/circuitbreakers"
curl -X POST "http://localhost:8082/post-service/api/management/failure-simulator/reset"
```

### Bulkhead under concurrency

Add a delay and issue more than 10 concurrent Feed Service requests to a Post Service-backed operation. The bulkhead permits at most 10 active calls and waits up to one second for a slot; requests beyond that capacity may fail. Exact outcomes depend on timing and the current circuit-breaker state.

```bash
curl -X POST "http://localhost:8082/post-service/api/management/failure-simulator/slowness/start?delayMs=3000"
# In another shell, send concurrent calls to /feed-service/api/feed/user-alex
curl -X POST "http://localhost:8082/post-service/api/management/failure-simulator/reset"
```

Reset clears simulated delay and failures and restores the simulator's default delay. If a test is interrupted, reset before continuing.

## Implementation limits to keep in mind

- There is a failure simulator only in the Post Service. User Service has no management controller or simulated delay/failure behavior in the current source.
- Feed Service exposes three business routes: feed, follower count, and batch posts. Earlier guides that list eight Feed Service business routes do not match the current controller.
- `ResilientPostService` wraps Post Service business calls, not User Service calls or simulator-management requests.
- A two-second TimeLimiter policy is configured but is not connected to the decorator. The HTTP client separately has a two-second read timeout; retry attempts and backoff can make the overall call take longer.
- Simulated post delay and failure behavior is attached to post lookup operations used by the Feed Service. Basic list/create/delete routes do not all share that simulation path.
- Circuit-breaker state persists across requests until recovery or service restart. Resetting the failure simulator does not reset the circuit breaker.

## Troubleshooting

| Symptom | Check |
|---|---|
| Feed Service cannot reach a dependency | Confirm User and Post Services are running on ports 8081 and 8082, and check the configured service URLs. |
| Management request returns 404 | Include the service context path (`/feed-service` or `/post-service`) in the URL. |
| Feed is empty | The user may follow nobody, or no posts may exist for followed authors. Seed data includes Alex, Sam, Jordan, and sample posts. |
| Batch response is empty | Post IDs are generated UUIDs; list posts on the Post Service to obtain current IDs. |
| Calls fail fast after a failure test | The circuit breaker may be OPEN. Check the actuator state and wait through its 10-second open interval. |
| A request takes longer than two seconds overall | Each HTTP read has a two-second timeout, but retries and backoff can extend the overall duration; the configured TimeLimiter is not applied by the decorator. |

## Test scripts

Run `test-resilience-patterns.sh <timeout|retry|circuit-breaker|bulkhead>` to exercise one scenario at a time. It calls Post Service directly for simulator control and calls Feed Service only for the business request. Restart Feed Service before the circuit-breaker or bulkhead scenario if you need a clean breaker state.

