---

When One Microservice Fails: How Do You Keep the Rest Alive?
Timeouts, retries, circuit breakers, and bulkheads explained through a real-world story
The complete implementation is available in the [PulseFeed resilience patterns repository](https://github.com/javalearnerss/dev-microservices/tree/main/spring-boot-resilience-patterns/services). Instructions for running and testing the services are at the end of this article.
Microservices make it possible to build and deploy applications as independent services. But independence has a limit: services still depend on one another to complete requests.
What happens when one service becomes slow? Should the services calling it keep waiting? Should they retry? And how do we stop one struggling service from bringing down the rest of the application?
Let's explore these questions through Pulse, a fictional social media application, and follow one request from the moment a user opens their feed to the moment a dependency failure threatens the entire system.
1. A Normal Morning on Pulse
At 9:02 AM, Rahul opened his phone. He was already late, but before leaving for work, he usually spent a few minutes on Pulse, a social media app where he followed friends, colleagues, and creators. He opened Pulse and tapped Feed.
Normally, his personalized feed appeared immediately. Today, there was only a loading spinner. He waited, refreshed, and checked his internet connection. Full signal. He tried again. Nothing.
Rahul wasn't alone. At almost exactly the same time, thousands of other Pulse users were seeing the same loading spinner.
Something inside the system had gone wrong.
But here was the strange part:
The Feed Service itself wasn't down.
So why couldn't Rahul see his feed?
To understand that, let's look inside Pulse and see what happens when Rahul taps Feed.
2. What Happens When Rahul Taps Feed?
Rahul sees a simple screen: a list of posts - a photo from a friend, a post from a colleague, or something from a creator he follows. What he doesn't see is the machinery behind that screen.
The moment Rahul taps Feed, his request starts moving through several different services. Let's follow that request.

The request first reaches the API Gateway, the front door of Pulse. It receives Rahul's request and forwards it to the Feed Service, which is responsible for building his feed.
The Feed Service needs to know more about Rahul before it can fetch the right posts. So, it calls the Profile Service to retrieve the people he follows and his feed preferences.
With that information, the Feed Service knows whose posts to look for. It now calls the Post Service and asks for posts from those users.
The Post Service retrieves the relevant posts from its database and returns them to the Feed Service. The Feed Service then combines the posts with the information it needs, prepares the response, and sends it back to Rahul's app.
And that's it. Rahul sees his feed without needing to know how many services worked behind the scenes to make it happen.
3. Except This Morning, the Request Didn't Finish
Let's go back to 9:02 AM.
The API Gateway, Profile Service, and Feed Service are healthy. But something has changed inside the Post Service. It hasn't gone down; it's simply getting slower.
Yesterday, a request to Post Service took around 50 milliseconds. This morning, it takes 500 milliseconds, then 2 seconds, and eventually 5 seconds.
The Feed Service sends its request and waits for the posts to arrive.
Meanwhile, more users are opening Pulse. Every feed request that needs posts adds another call to the struggling Post Service. Instead of processing requests quickly, Post Service takes longer to respond, and requests begin to pile up.
4. When One Service Slows Down, Another Follows
By 9:05 AM, thousands of users are opening Pulse, and every feed request needs data from Post Service. So the Feed Service continues sending requests, even though Post Service is already struggling.
Post Service is struggling, but Feed Service doesn't know when to stop calling it. Requests begin waiting, consuming resources such as threads and connections, and soon they start accumulating. Feed Service may still have enough capacity to build feeds, but more and more of its resources are tied up waiting for Post Service.
Then something unexpected happens: Feed Service itself starts slowing down.
The original problem was Post Service, but now Feed Service is becoming part of the problem.
This is how a problem that started in one microservice can begin affecting another.
5. Rahul Still Doesn't Know
Back on Rahul's phone, the spinner is still turning. He refreshes the screen, but nothing changes. He doesn't know that Post Service is struggling, that Feed Service is waiting, or that thousands of other requests are doing the same thing.
As far as Rahul is concerned:
"Pulse isn't working."
And that's the uncomfortable part of distributed systems. Services can be independently running, deployed, and responsible for different parts of the application, yet they can still affect one another because they depend on one another.
Feed Service depends on Post Service, and this morning that dependency has become a problem.
So what should Feed Service do when a service it depends on becomes unhealthy? Should it keep waiting and sending requests, or should it recognize the problem and temporarily stop calling Post Service?
Before we get to that answer, let's start with the simplest approach:
What if we just tell Feed Service to stop waiting after a certain amount of time?

---

6. What If We Just Add a Timeout?
We can tell Feed Service:
"If Post Service doesn't respond within two seconds, stop waiting."
When Rahul opens Pulse, Feed Service requests his posts. If Post Service takes longer than two seconds, Feed Service stops waiting and can return an appropriate error or, where possible, a fallback response.
That's an improvement. Feed Service no longer waits indefinitely.
But consider what happens when thousands of users open Pulse.
Request 1 → Post Service → 2 seconds → Timeout
Request 2 → Post Service → 2 seconds → Timeout
Request 3 → Post Service → 2 seconds → Timeout
Request 4 → Post Service → 2 seconds → Timeout
The timeout solves one problem: Feed Service no longer waits forever. 
In the Pulse sample, Feed Service gives the Post Service HTTP call a two-second read timeout:

```yaml
spring:
  cloud:
    openfeign:
      client:
        config:
          post-service:
            connectTimeout: 500
            readTimeout: 2000
```

This limits the Post Service call itself. The complete feed request can take longer because Feed Service first looks up the user's following list and may then retry a failed Post Service call.
But it doesn't stop Feed Service from sending new requests to a service that's already struggling.
We have limited how long we wait. Now we need to decide what to do when a request fails.

---

7. What If We Try Again After a Timeout?
The timeout solved one problem: Feed Service no longer waits indefinitely for Post Service.
But what happens when a request fails? Should Feed Service give up immediately, or try again?
A timeout doesn't necessarily mean Post Service is permanently unhealthy. Perhaps its database was briefly overloaded, an instance was restarting, or a network interruption delayed the response.
If the problem is temporary, another attempt might succeed.
8. Retry: Give a Temporary Failure Another Chance
Instead of giving up after the first timeout, Feed Service can wait briefly and retry the request.
If Post Service has recovered, the second attempt may return the posts successfully.
The distinction is simple:
Timeout limits how long Feed Service waits for an attempt.
Retry allows Feed Service to make another attempt after a failure. 
Here is a simplified Spring Boot example from the Feed Service. max-attempts: 3 means up to three total attempts: the original request plus two retries.

```yaml
resilience4j:
  retry:
    instances:
      post-service:
        max-attempts: 3
        wait-duration: 100ms
        enable-exponential-backoff: true
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - feign.FeignException$ServiceUnavailable
          - feign.FeignException$GatewayTimeout
```

```java
@Retry(name = "post-service", fallbackMethod = "findByAuthorIdsFallback")
public List<Post> findByAuthorIds(List<String> authorIds) {
    return postServiceClient.findByAuthorIds(authorIds);
}
```

Retries should be limited to suitable transient failures. The example retries read-only Post Service calls; retrying a write can create duplicate effects unless the operation is idempotent.

Retries are useful for temporary failures, but they need limits. In this example, Feed Service allows two additional attempts, for three total attempts, rather than retrying indefinitely.
But now we have to ask a more dangerous question:
What if Post Service isn't temporarily failing?
What if Post Service is genuinely overloaded?
In that situation, retrying every failed request can make the problem worse.

---

9. But What If Post Service Keeps Failing?
Now imagine that Post Service isn't recovering. Its database is still overloaded, requests are still timing out, and thousands of users are opening Pulse.
Each failed request can trigger another attempt. Instead of reducing the pressure on Post Service, Feed Service adds more work to a service that is already struggling.
With retries enabled, things can get even worse:
Even with a retry limit, thousands of original requests can generate thousands of additional attempts.
Retries can help a service recover from a temporary failure, but they can make an ongoing failure worse.

---

10. Two Different Questions
At this point, we've learned two things.
How long should Feed Service wait for Post Service? A timeout answers that:
"Wait at most two seconds."
But there's a second question:
Should we keep calling Post Service when it repeatedly fails?
When Post Service is healthy, requests should flow normally.
Feed Service
     │
     ▼
Post Service ✓
But when repeated failures occur, continuing to send requests may add more pressure to an already struggling service.
A timeout limits the waiting time for each attempt. It doesn't decide whether another attempt should be made at all.
Perhaps Feed Service needs a way to recognize that Post Service is unhealthy and temporarily stop calling it.

---

11. What If We Could Stop the Calls?
Imagine a small component between Feed Service and Post Service.
It keeps track of how Post Service is responding. If failures continue beyond a configured threshold, it temporarily blocks new calls.
Instead of repeatedly knocking on a door that isn't answering, Feed Service pauses and gives Post Service time to recover.
But the component needs to answer two questions:
When should it stop allowing requests?
How should it check whether Post Service has recovered?

This is where the Circuit Breaker comes in.

---

12. Meet the Circuit Breaker
We now have a small component sitting between Feed Service and Post Service. Its job is to decide when requests should pass through and when they should be blocked.
When Post Service is healthy, requests flow normally. When it starts failing repeatedly, the component stops Feed Service from calling it. Once Post Service recovers, requests can flow again.
This is exactly the problem the Circuit Breaker Pattern is designed to solve.
The idea is simple:
Allow requests when things are healthy. Stop requests when a dependency is failing. Then cautiously try again after some time.
It sits between the two services:
Think of it like the circuit breaker in your home. When everything is working normally, electricity flows. When the breaker detects a serious problem, it interrupts the flow.
The same idea applies here: when Post Service is healthy, requests flow through. When it starts failing repeatedly, the Circuit Breaker stops the flow of requests.
But unlike a home circuit breaker, this one doesn't stay open forever. It needs to understand three different situations.

1. When Everything Is Working: CLOSED
Let's go back to Rahul on a normal morning. Post Service is healthy, so when Rahul opens Pulse, Feed Service sends his request through the Circuit Breaker, which allows it to reach Post Service normally.
Post Service responds successfully, so the Circuit Breaker has nothing to worry about. This is the CLOSED state:
"Everything looks healthy. Let the requests through."
As long as Post Service continues responding normally, the circuit stays closed. Nothing special happens - and that's exactly what we want. A resilience mechanism shouldn't get in the way when everything is working.
2. Then the Failures Start
Now let's return to the morning when Rahul's feed stopped loading. Post Service starts becoming unhealthy, and the first request fails.
The Circuit Breaker records the failure but doesn't immediately block requests. A single failure could be temporary - a brief database hiccup or network problem - so the circuit remains CLOSED.
But then another request fails. And another. The Circuit Breaker keeps track of these failures until they reach a configured threshold.
For our example:
For this implementation, the breaker evaluates calls after at least three have been recorded and opens when the failure rate or slow-call rate reaches 50%. 
In this implementation, the breaker evaluates a rolling count of calls. It waits until at least three calls have been recorded, then opens if at least 50% failed or were slow (a slow call is one lasting more than one second). Once open, it waits ten seconds before allowing recovery checks.

```yaml
resilience4j:
  circuitbreaker:
    instances:
      post-service:
        sliding-window-type: COUNT_BASED
        sliding-window-size: 10
        minimum-number-of-calls: 3
        failure-rate-threshold: 50
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 1s
        wait-duration-in-open-state: 10s
```

The Feed Service applies it around the Post Service operation:

```java
@CircuitBreaker(name = "post-service")
public List<Post> findByAuthorIds(List<String> authorIds) {
    return postServiceClient.findByAuthorIds(authorIds);
}
```
So we get:
And now everything changes.

---

3. OPEN: Stop Calling Post Service
The circuit is now OPEN, which means:
"Stop sending requests to Post Service."
When Rahul refreshes his feed, the request still reaches Feed Service, but the Circuit Breaker blocks it before it reaches Post Service.
The Circuit Breaker immediately rejects the call. There's no waiting for Post Service, no new connection, and no additional request sent to an unhealthy dependency. Feed Service can now fail fast or return a fallback response.
And this is the important difference we were looking for.
Before the Circuit Breaker:
After the circuit opens:
We've stopped knocking on the door.
But what happens if Post Service recovers? The circuit can't stay open forever. We need a way to check whether the service is healthy again.
4. HALF-OPEN: Is Post Service Back?
The Circuit Breaker waits for a configured amount of time - for example, 10 seconds. It then moves from OPEN to HALF-OPEN and cautiously tests the dependency.
It's essentially saying:
"I think Post Service may have recovered. Let's try one request and see."
Notice what we're not doing: we're not immediately allowing thousands of requests through. We're testing carefully.
If the test request succeeds:
HALF-OPEN
     │
     │ success ✓
     ▼
  CLOSED
The test request succeeds, so the circuit closes again and normal traffic resumes.
But what if the test request fails?
HALF-OPEN
     │
     │ failure ❌
     ▼
   OPEN
The test request fails, so the circuit opens again. Post Service still isn't healthy, so we give it more time before trying again.

---

13. The Three States
Let's look at what this means for Rahul's feed.
When Post Service is healthy, Rahul's requests pass through normally. When repeated failures occur, the Circuit Breaker opens and stops sending new requests to Post Service. After waiting for some time, it cautiously tests whether Post Service has recovered.
The flow looks like this:
In simple terms:
CLOSED - Post Service appears healthy, so requests are allowed through.
OPEN - Post Service has repeatedly failed, so new calls are blocked temporarily.
HALF-OPEN - The Circuit Breaker cautiously tests Post Service to see whether it has recovered.

If the test succeeds, normal traffic resumes. If it fails, the circuit opens again.
For Rahul, this means Feed Service no longer keeps calling Post Service blindly. It can stop sending requests while the dependency is struggling and cautiously resume when recovery looks promising.
But protecting Feed Service from repeated failures is only part of the problem. What happens to requests that are already waiting for Post Service when the circuit opens? Could they still consume so many resources that Feed Service struggles to serve other requests?
Let's look at the next layer of protection.

---

14. But There Is Still a Problem
At this point, we have a Timeout so requests don't wait forever, a Retry for temporary failures, and a Circuit Breaker that stops calls when Post Service keeps failing.
That's a significant improvement - but there's still a problem before the circuit opens.
Remember Rahul's morning? Post Service is still slow, and hundreds of users are trying to load their feeds. Some requests are waiting for Post Service, others are retrying, and new requests keep arriving.
Even though each request has a timeout, many requests can still be in progress at the same time. Together, they compete for Feed Service's limited resources.
So what happens if Post Service consumes so much of Feed Service's capacity that it can no longer serve other requests?
We need another layer of protection.
15. What If One Dependency Consumes Everything?
Imagine a ship divided into several watertight compartments. If one compartment fills with water, the walls prevent the entire ship from flooding. The ship can lose one compartment and still stay afloat.
Microservices can use the same idea by isolating resources between dependencies. That's the basic idea behind a Bulkhead.
In Pulse, Feed Service depends on both Post Service and Profile Service. If Post Service becomes slow, we don't want its requests to consume all the resources Feed Service needs to do other work.
A Bulkhead limits how much of a service's resources one dependency or workload can consume, preventing a problem in one area from exhausting resources needed elsewhere. 
The Pulse Feed Service uses a semaphore bulkhead with at most ten concurrent Post Service operations. Additional calls can wait up to one second for a permit; after that, they are rejected.

```yaml
resilience4j:
  bulkhead:
    instances:
      post-service:
        max-concurrent-calls: 10
        max-wait-duration: 1s
```

```java
@Bulkhead(name = "post-service", type = Bulkhead.Type.SEMAPHORE)
public List<Post> findByAuthorIds(List<String> authorIds) {
    return postServiceClient.findByAuthorIds(authorIds);
}
```
16. Without a Bulkhead
Imagine Rahul opens Pulse, and Feed Service calls Post Service to retrieve his feed. Post Service is slow, so the request takes much longer than usual.
Meanwhile, more users open their feeds. Feed Service keeps making calls to Post Service, and those calls occupy more and more of its available resources.
Feed Service
Eventually, very little capacity may be left for anything else. That's dangerous because Feed Service depends on more than one service. In our example, it also calls Profile Service.
Even if Profile Service is healthy, its requests may struggle because slow Post Service calls are consuming the resources needed to process them.
A problem in Post Service can now affect completely unrelated work.
17. With a Bulkhead
Now imagine Feed Service has separate, limited pools of resources for calls to Post Service and Profile Service.
Instead of allowing Post Service calls to consume all available capacity, we set a limit on how many can be in progress at once.
If Post Service becomes slow, its limited pool can fill up. Once that happens, additional calls can be rejected rather than consuming unlimited resources.
The capacity available for other work remains protected, depending on how the Bulkhead is configured.
Rahul's feed may still be incomplete or unavailable. But a slow Post Service is less likely to prevent Feed Service from handling unrelated requests.
The failure is contained.
That's the key idea behind the Bulkhead pattern.
18. Circuit Breaker vs. Bulkhead
These two patterns may sound similar, but they solve different problems.
The Circuit Breaker asks:
"Should we call Post Service?"
The Bulkhead asks:
"How much of our capacity can Post Service consume?"
Suppose Post Service is slow, but the Circuit Breaker hasn't opened because the failure threshold hasn't been reached. Without a Bulkhead, many requests could still consume Feed Service resources while waiting for Post Service.
With a Bulkhead, only a controlled number of calls can be in flight for that dependency. Additional calls can fail fast or wait, depending on the configured policy.
The Circuit Breaker helps stop repeated calls to an unhealthy dependency. The Bulkhead limits how much capacity those calls can consume.
Together, they help ensure that when Post Service struggles, the rest of Pulse has a better chance of staying responsive.
19. Now We Have Four Layers of Protection
Let's return to Rahul's morning.
Post Service became slow, and Rahul couldn't load his feed. What started as a problem in one service threatened to affect the entire Pulse application.
We now have four mechanisms, each solving a different problem:
Timeout - "Don't wait forever." Limits how long an individual attempt can take.
Retry - "This failure might be temporary. Try again carefully." Gives transient failures another chance.
Circuit Breaker - "This dependency keeps failing. Stop calling it for now." Prevents repeated calls to an unhealthy dependency.
Bulkhead - "Don't let this dependency consume all our resources." Limits the impact of a slow dependency on other work.

These aren't competing solutions. They work together to protect the system in different ways.
20. What Happens When Post Service Fails Now?
Let's return to Rahul one last time.
He opens Pulse and taps Feed. Post Service begins struggling again, but this time Feed Service is better prepared.
The timeout prevents a request from waiting indefinitely. A limited retry gives a temporary failure another chance. If failures continue, the Circuit Breaker stops new calls. Meanwhile, the Bulkhead prevents calls to Post Service from consuming all the resources Feed Service needs.
The outcome is different from that first morning.
Post Service can still fail. Rahul's feed can still be affected. But the failure is less likely to exhaust Feed Service's resources and spread through the rest of Pulse.
Resilience doesn't prevent every failure. It limits how far a failure can spread.

---

One Microservice Failed
The original problem was simple: Post Service became slow.
Without resilience, that slowdown could exhaust Feed Service's resources and make Pulse appear unavailable.
With timeouts, limited retries, a Circuit Breaker, and a Bulkhead, Feed Service has ways to limit waiting, control additional attempts, stop repeated calls, and protect capacity.
Post Service may still be unhealthy. The difference is that one failing microservice no longer has to take the entire application down with it.

---

But What Does Rahul See?
Feed Service is protected, but Rahul still needs an answer.
Depending on the business requirements, Pulse might show cached posts, return a partial feed, or display a clear message that the feed is temporarily unavailable.
The right response depends on what the application can safely provide. Not every feature can degrade gracefully in the same way.
The important point is that protecting the system and protecting the user experience are two different responsibilities. The resilience patterns help contain the failure; the application's fallback behavior determines what Rahul sees.
That's graceful degradation: when one part of the system fails, the rest can continue providing whatever functionality is still safe and available.

---

What We Learned
Rahul's problem began with one slow microservice. But without protection, that slowdown could spread to Feed Service and eventually affect the entire Pulse application.
Each resilience pattern addresses a different part of the problem:
A Timeout limits waiting.
A Retry handles suitable temporary failures.
A Circuit Breaker stops repeated calls to an unhealthy dependency.
A Bulkhead protects resources needed for other work.

None of these patterns guarantees that a request will succeed. Their purpose is to prevent one dependency's failure from causing unnecessary damage elsewhere.
In a distributed system, the goal isn't to assume that services will never fail. It's to design the system so that when one does fail, the rest have a better chance of staying alive. 
---

A Compact Example from Pulse
----------------------------

In the Feed Service, these policies protect the same read-only call. Retry handles transient failures; the Circuit Breaker tracks the final outcome; the Bulkhead limits concurrent work. The application config sets the aspect order so the Circuit Breaker wraps the complete retry operation, and the Retry fallback translates exhausted failures into HTTP 503.

The aspect-order settings make the Circuit Breaker the outer layer and Retry the inner layer:

```yaml
resilience4j:
  retry:
    retryAspectOrder: 2
  circuitbreaker:
    circuitBreakerAspectOrder: 1
```

```java
@Retry(name = "post-service", fallbackMethod = "findByAuthorIdsFallback")
@CircuitBreaker(name = "post-service")
@Bulkhead(name = "post-service", type = Bulkhead.Type.SEMAPHORE)
public List<Post> findByAuthorIds(List<String> authorIds) {
    return postServiceClient.findByAuthorIds(authorIds);
}

private List<Post> findByAuthorIdsFallback(List<String> authorIds, Throwable cause) {
    throw new PostServiceUnavailableException(
        "Post Service is temporarily unavailable", cause);
}
```

To exercise the failure simulator, start a delay directly on Post Service, call Feed Service, then reset the simulator:

```bash
curl -X POST "http://localhost:8082/post-service/api/management/failure-simulator/slowness/start?delayMs=3000"
curl "http://localhost:8083/feed-service/api/feed/user-alex"
curl -X POST "http://localhost:8082/post-service/api/management/failure-simulator/reset"
```



You can also run the included shell script to exercise the resilience scenarios one at a time:

```bash
bash ./test-resilience-patterns.sh retry
```

Replace `retry` with `timeout`, `circuit-breaker`, or `bulkhead` to try another scenario. The script controls the failure simulator directly on Post Service and resets it when the run ends. Restart Feed Service before the bulkhead scenario if its Circuit Breaker is not closed.
