# Resilience and Failure Simulation Guide

This guide documents the behavior currently implemented in the services. It replaces overlapping endpoint lists and walkthroughs that described planned or older behavior.

## Request flow

```text
Client → Feed Service → User Service (who the user follows)
                     → Post Service (posts by those authors)
```

The Feed Service applies its resilience decorator to the four Post Service client operations: posts by author IDs, posts by post IDs, post count by author, and posts by author. The User Service client calls are direct and currently do not have a failure simulator.

## Current resilience settings

The implementation is in `feed-service/src/main/java/in/javalearners/pulsefeed/feed/config/ResilienceConfig.java` and `ResilienceDecoratorConfig.java`.

| Pattern | Current behavior |
|---|---|
| Retry | Up to 3 total attempts, with exponential backoff starting at 100 ms; `TimeoutException` is excluded from retry. |
| Circuit breaker | Opens at a 50% failure or slow-call rate; minimum 3 calls, a 10-call sliding window, and 10 seconds open. It permits 3 calls in half-open state. |
| Bulkhead | Up to 10 concurrent calls; a caller can wait up to 1 second for a slot. |
| Time limiter | A 2-second `TimeLimiter` bean is configured, but the current Post Service decorator does not apply it. Do not expect a two-second timeout from that configuration as currently wired. |

The effective decorator nesting is Retry → Circuit Breaker → Bulkhead → downstream call. Each retry attempt therefore passes through the circuit breaker and bulkhead. The code does not currently make the synchronous Feign call cancellable through the configured TimeLimiter.

## Failure simulator controller reference

The Post Service controller is mounted at `/api/management/failure-simulator`; its base URL is `http://localhost:8082/post-service`. The Feed Service proxies the same controls under `/api/feed/management/post-service`; use `http://localhost:8083/feed-service` as its base URL. Every method in `FailureSimulatorController` is represented below:

| Controller method | HTTP route (Feed Service proxy) | Behavior |
|---|---|---|
| `startSlowness(delayMs)` | `POST /api/feed/management/post-service/slowness/start?delayMs=500` | Enables delay before simulated Post Service operations. `delayMs` defaults to 500. |
| `stopSlowness()` | `POST /api/feed/management/post-service/slowness/stop` | Disables simulated delay. |
| `startFailure(failureCount)` | `POST /api/feed/management/post-service/failure/start?count=5` | Enables failures for the next `count` simulated calls. `count` defaults to 5. |
| `stopFailure()` | `POST /api/feed/management/post-service/failure/stop` | Disables failure simulation and clears the remaining failure count. |
| `getStatus()` | `GET /api/feed/management/post-service/status` | Returns whether delay and failure simulation are enabled, the configured delay, and the total simulated calls received. |
| `reset()` | `POST /api/feed/management/post-service/reset` | Disables delay and failures, restores the 500 ms default delay, clears the failure count, and resets the simulator's request counter. |

The direct Post Service routes have the same suffixes below `/api/management/failure-simulator`. The Feed Service proxy routes call those management methods directly; they do not consume the resilience-wrapped business-call path.

### Exercise every simulator method

This complete lifecycle uses all six controls: start and stop delay, start and stop failures, inspect status, and reset. It also makes a request through a Feed Service business route so the simulated call counter changes.

```bash
BASE="http://localhost:8083/feed-service/api/feed/management/post-service"

# Start delay and confirm the simulator state.
curl -X POST "$BASE/slowness/start?delayMs=750"
curl "$BASE/status"

# Stop delay; then start and stop a finite failure simulation.
curl -X POST "$BASE/slowness/stop"
curl -X POST "$BASE/failure/start?count=1"
curl "$BASE/status"
curl "http://localhost:8083/feed-service/api/feed/posts/batch?ids=replace-with-a-real-post-id"
curl -X POST "$BASE/failure/stop"

# Inspect final state and restore all simulator fields and counters.
curl "$BASE/status"
curl -X POST "$BASE/reset"
curl "$BASE/status"
```

Replace `replace-with-a-real-post-id` with an ID returned by `GET http://localhost:8082/post-service/api/posts`. The batch call may return an empty list for a nonexistent ID; it still exercises the simulated Post Service method.

## Run the supported Post Service experiments

The examples below use the Feed Service's management routes to control the Post Service simulator. Feed requests go through the resilience decorator. Run one experiment at a time and reset the simulator afterward.

### Retry after injected failures

Configure two failures, then request posts. With the default three total attempts, the request can succeed on its third attempt, provided the circuit breaker has not opened from prior traffic.

```bash
curl -X POST "http://localhost:8083/feed-service/api/feed/management/post-service/failure/start?count=2"
curl "http://localhost:8083/feed-service/api/feed/posts/batch?ids=post-1"
curl -X POST "http://localhost:8083/feed-service/api/feed/management/post-service/reset"
```

The seeded post identifiers are generated at startup, so `post-1` is only an example and may not exist. A missing ID can still return an empty result after a successful call. Use `/api/posts` on the Post Service to discover the generated IDs.

### Circuit breaker

Inject repeated failures and make calls through a Feed Service endpoint. The breaker evaluates calls made by the Post Service decorator; because retries are enabled, a single outer request can produce multiple recorded failures. Existing breaker state and traffic affect when the threshold is reached. Inspect `/feed-service/actuator/circuitbreakers` and allow the 10-second open interval before checking recovery.

```bash
curl -X POST "http://localhost:8083/feed-service/api/feed/management/post-service/failure/start?count=20"
curl "http://localhost:8083/feed-service/api/feed/posts/batch?ids=post-1"
curl "http://localhost:8083/feed-service/actuator/circuitbreakers"
curl -X POST "http://localhost:8083/feed-service/api/feed/management/post-service/reset"
```

### Bulkhead under concurrency

Add a delay and issue more than 10 concurrent Feed Service requests to a Post Service-backed operation. The bulkhead permits at most 10 active calls and waits up to one second for a slot; requests beyond that capacity may fail. Exact outcomes depend on timing and the current circuit-breaker state.

```bash
curl -X POST "http://localhost:8083/feed-service/api/feed/management/post-service/slowness/start?delayMs=3000"
# In another shell, send concurrent calls to /feed-service/api/feed/posts/batch?ids=post-1
curl -X POST "http://localhost:8083/feed-service/api/feed/management/post-service/reset"
```

Reset clears simulated delay and failures and restores the simulator's default delay. If a test is interrupted, reset before continuing.

## Implementation limits to keep in mind

- There is a failure simulator only in the Post Service. User Service has no management controller or simulated delay/failure behavior in the current source.
- Feed Service exposes three business routes: feed, follower count, and batch posts. Earlier guides that list eight Feed Service business routes do not match the current controller.
- The Feed Service resilience decorator wraps Post Service calls, not User Service calls.
- A two-second TimeLimiter is created but is not connected to the decorator. Slow calls are not currently guaranteed to stop after two seconds.
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
| A slow request takes longer than two seconds | This is expected with the current wiring: the TimeLimiter bean is not applied to the synchronous client decorator. |

## Test scripts

`test-resilience-patterns.ps1` and `test-resilience-patterns.sh` are separate PowerShell and Bash entry points. They currently contain assumptions about User Service failure simulation and timeout behavior that do not match the implementation above. Treat their output as illustrative until those scripts are updated to match the current service behavior.
