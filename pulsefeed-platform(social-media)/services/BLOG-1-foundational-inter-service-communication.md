# Building Microservices Communication from the Ground Up: A Practical Spring Boot Guide

*Blog 1 · The local sandbox*

The first time I asked PulseFeed for Alex’s timeline, the request looked ordinary:

```http
GET http://localhost:8083/feed-service/api/feed/user-alex
```

But there was no single database query waiting behind that URL. Alex’s feed lived across service boundaries. One service knew who Alex followed. Another knew what those people had posted. A third had to bring those answers together before it could respond.

That small request became my starting point for understanding microservice communication: what the happy path looks like, how a service finds another service, and what happens when one of those calls takes too long or fails.

## Meet the three services

PulseFeed is split into three Spring Boot applications. Each owns a small part of the problem:

| Service | Local port | What it knows |
| --- | ---: | --- |
| User Service | 8081 | Users and their follow relationships |
| Post Service | 8082 | Posts and their authors |
| Feed Service | 8083 | How to assemble the posts relevant to a user |

All three use Java 17 and Spring Boot 3.3.5. User and Post expose REST APIs. Feed uses Spring Cloud OpenFeign to call those APIs. For this local sandbox, the services keep data in memory and seed a few users, relationships, and posts when they start. That makes the scenario easy to run and reset; it is not intended to provide persistence.

## Following one feed request

I started with Feed Service because it is the service that has to answer the question: “What should Alex see?” Its controller receives the request at `/api/feed/{userId}`. The configured servlet context path adds `/feed-service`, giving us the full URL from the opening example.

Inside the application, `FeedService` takes over. First, it asks User Service for Alex’s following list:

```http
GET http://localhost:8081/user-service/api/users/user-alex/following
```

The seeded graph says Alex follows Sam and Jordan. Feed Service takes those IDs and asks Post Service for their posts:

```http
GET http://localhost:8082/post-service/api/posts?authorIds=user-sam&authorIds=user-jordan
```

Post Service filters its posts by those author IDs and returns the results newest first. Feed Service passes that list back to the original caller. Alex’s own posts are not included because the query is based on the accounts Alex follows.

There is a small branch in the flow: if User Service returns an empty following list, Feed Service skips the Post Service call and returns an empty list. That is a valid result, so there is no reason to make an unnecessary downstream request.

## Describing calls with OpenFeign

I wanted the remote calls to read like API contracts rather than hand-built HTTP plumbing. OpenFeign lets Feed Service express them as Java interfaces:

```java
@FeignClient(name = "user-service", url = "${services.user-service.url}")
public interface UserServiceClient {
    @GetMapping("/api/users/{userId}/following")
    List<User> findFollowing(@PathVariable("userId") String userId);
}
```

```java
@FeignClient(name = "post-service", url = "${services.post-service.url}")
public interface PostServiceClient {
    @GetMapping("/api/posts")
    List<Post> findByAuthorIds(@RequestParam("authorIds") List<String> authorIds);
}
```

Feed enables client discovery within its application using `@EnableFeignClients`. The controller stays focused on the incoming HTTP request, while `FeedService` coordinates the calls and contains the feed rule. The client interfaces capture the other half of the contract: which path to call and how to encode parameters.

## Giving the sandbox a try

I opened three PowerShell terminals and started one application in each. Run each command from the `services` workspace root:

```powershell
cd .\user-service
.\gradlew.bat bootRun
```

```powershell
cd ..\post-service
.\gradlew.bat bootRun
```

```powershell
cd ..\feed-service
.\gradlew.bat bootRun
```

Each application has its port and context path in `src/main/resources/application.properties`. Feed’s downstream base URLs default to localhost. They can be changed through environment variables; for example, in the Feed Service terminal:

```powershell
$env:USER_SERVICE_URL = 'http://localhost:8081/user-service'
$env:POST_SERVICE_URL = 'http://localhost:8082/post-service'
.\gradlew.bat bootRun
```

With all three running, I can peek at each step and then ask for the final feed:

```powershell
curl.exe http://localhost:8081/user-service/api/users/user-alex/following
curl.exe "http://localhost:8082/post-service/api/posts?authorIds=user-sam&authorIds=user-jordan"
curl.exe http://localhost:8083/feed-service/api/feed/user-alex
```

To see a change travel through the system, I can create a post as Sam:

```powershell
curl.exe -X POST http://localhost:8082/post-service/api/posts `
  -H "Content-Type: application/json" `
  -d '{"authorId":"user-sam","content":"Hello from the local sandbox!"}'
```

A new request to Alex’s feed should now include that post. Restarting Post Service clears its in-memory collection and reseeds the sample posts, which is a handy reset button while exploring.

## A service name is not service discovery

At first glance, the Feign annotation includes `name = "user-service"`, which sounds like discovery. But Feed’s configuration also supplies a concrete URL:

```properties
services.user-service.url=${USER_SERVICE_URL:http://localhost:8081/user-service}
services.post-service.url=${POST_SERVICE_URL:http://localhost:8082/post-service}
```

That is how this sandbox currently locates its dependencies: **static addresses in configuration**. It is a sensible first step on a laptop with three known ports. It also means Feed needs to be told when an address changes, and it cannot use that single configured address to select from multiple healthy instances.

The `name` gives the Feign client an identity; with the explicit `url` configured, it does not make a registry lookup happen. There is no Eureka server, service registration, or discovery configuration in the current project.

When the local flow feels familiar, service discovery is a natural next chapter. We could add a registry, register User and Post with it, bring in the appropriate Spring Cloud LoadBalancer support, and let Feign resolve logical service names to available instances. Then the physical host and port could change without hard-coding each one in Feed. For three local processes, the configured URLs are simpler; discovery becomes useful when instances move, multiply, or need health-aware selection.

## When the happy path stops being happy

A synchronous call is easy to follow because Feed waits for each answer before continuing. That also creates a dependency chain. If User Service is slow, Feed is slow before it even reaches Post Service. If either service is unavailable, Feed cannot build the complete result.

I tested the failure path by stopping Post Service and requesting Alex’s feed again. Feed can still reach User Service and find the author IDs, but the second call fails because no process is listening at the Post Service address. The failure belongs in the response and logs; returning `[]` would falsely tell the caller that nobody had posted anything.

There is one explicit error translation in the current code. If User Service returns 404 because the requested feed owner does not exist, `FeedService` catches Feign’s `NotFound` exception and returns a local 404. Connection failures, timeouts, and Post Service errors are not translated into a successful feed. They propagate and typically result in a server error response.

## Timeouts: deciding how long to wait

A downstream request should not be allowed to consume the caller’s entire response budget. The current project does not define custom Feign connection or read timeouts, so configuring finite limits is an important next experiment.

For example, Feed Service could start with named client settings like these, verifying the property names against the Spring Cloud version resolved by the build:

```properties
spring.cloud.openfeign.client.config.user-service.connectTimeout=500
spring.cloud.openfeign.client.config.user-service.readTimeout=1000
spring.cloud.openfeign.client.config.post-service.connectTimeout=500
spring.cloud.openfeign.client.config.post-service.readTimeout=1500
```

The connection timeout controls how long the client waits to establish a connection. The read timeout limits how long it waits for response data after connecting. These values are just illustrative; a real choice depends on the endpoint’s latency budget and the time left for Feed to finish its own response.

With a deliberate timeout in place, I can stop a dependency or add a test-only delay and observe where the request fails. This makes the failure predictable instead of leaving the caller waiting without a clear bound.

## Retries: a second chance with a cost

It is tempting to retry whenever a call fails. A retry can smooth over a brief network hiccup, but it also adds attempts, latency, and load at the moment a dependency may already be struggling.

The current project does not configure a custom retry policy. If we add one for the feed’s read calls, it should be bounded and limited to failures that might actually recover. It must also fit inside the overall request deadline. A post-creation request is different: retrying a `POST` can create duplicate content unless the API uses an idempotency mechanism. A retry policy should follow the operation’s semantics, not be applied blindly to every call.

An easy local experiment is to make Post Service unavailable and compare behavior before and after introducing a carefully bounded retry. Watch both the final response and the Feed Service logs. More attempts can change how long the caller waits, but they cannot make a persistently stopped service healthy.

## What this first build taught me

This little application makes a few lessons tangible. Each service owns its data and exposes an API. Feed Service composes data through synchronous calls because it needs the follow list before it can request matching posts. OpenFeign keeps those calls readable, while the configured addresses make the local network path explicit.

The same simplicity reveals the trade-off: the feed inherits the availability and latency of both dependencies. Static addresses are convenient for a laptop, but they are not discovery. Timeouts and retries need to be chosen intentionally, and a meaningful error should not be disguised as an empty feed.

From here, the sandbox is ready for its next steps: add discovery, set and observe latency limits, experiment with bounded retries, and eventually add metrics or tracing to see the full request path. The goal is not to make the first version complicated. It is to understand exactly what is happening before making it more resilient.
