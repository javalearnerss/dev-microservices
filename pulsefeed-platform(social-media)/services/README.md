# PulseFeed Services

PulseFeed is a small Spring Boot microservices example. The User Service stores users and follow relationships, the Post Service stores posts, and the Feed Service combines them into a feed. The Feed Service also applies Resilience4j protections to calls to the Post Service.

## Services

| Service | Port | Context path | Responsibility |
|---|---:|---|---|
| User Service | 8081 | `/user-service` | Users and follow relationships |
| Post Service | 8082 | `/post-service` | Posts and the failure simulator |
| Feed Service | 8083 | `/feed-service` | Feed queries and protected Post Service calls |

The services use in-memory data stores and seed sample users and posts at startup. Changes are lost when a service restarts.

## Run locally

Open a terminal for each service and run its Gradle wrapper:

```powershell
# Terminal 1
cd user-service
./gradlew.bat bootRun

# Terminal 2
cd post-service
./gradlew.bat bootRun

# Terminal 3
cd feed-service
./gradlew.bat bootRun
```

On macOS or Linux, use `./gradlew bootRun` instead of `./gradlew.bat bootRun`. Start the User and Post Services before the Feed Service. The Feed Service uses `USER_SERVICE_URL` and `POST_SERVICE_URL` if set; otherwise it targets the local service URLs configured in `feed-service/src/main/resources/application.properties`.

## Quick check

Get Alex's feed (Alex follows Sam and Jordan, whose sample posts are seeded at startup):

```text
GET http://localhost:8083/feed-service/api/feed/user-alex
```

The Feed Service looks up Alex's following list in the User Service, then requests posts by those authors from the Post Service.

## API overview

All URLs below include the service context path.

### Feed Service — `http://localhost:8083/feed-service`

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/feed/{userId}` | Build a user's feed |
| GET | `/api/feed/user/{userId}/followers/count` | Get a user's follower count |
| GET | `/api/feed/posts/batch?ids=id1,id2` | Get posts by ID |
| POST | `/api/feed/management/post-service/slowness/start?delayMs=3000` | Turn on Post Service delay (default 500 ms) |
| POST | `/api/feed/management/post-service/slowness/stop` | Turn off Post Service delay |
| POST | `/api/feed/management/post-service/failure/start?count=3` | Make the next number of simulated Post Service calls fail |
| POST | `/api/feed/management/post-service/failure/stop` | Turn off failure simulation |
| GET | `/api/feed/management/post-service/status` | Read simulator state |
| POST | `/api/feed/management/post-service/reset` | Reset the Post Service simulator |

Feed Service management routes proxy controls to the Post Service. They do not pass through the resilience decorator.

### User Service — `http://localhost:8081/user-service`

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/users` | Create a user |
| GET | `/api/users` | List users |
| GET | `/api/users/{id}` | Get a user |
| PUT | `/api/users/{id}/following/{targetId}` | Follow a user |
| DELETE | `/api/users/{id}/following/{targetId}` | Unfollow a user |
| GET | `/api/users/{id}/following` | List followed users |
| DELETE | `/api/users/{id}` | Delete a user |
| GET | `/api/users/{id}/profile` | Get a user profile |
| GET | `/api/users/profiles?ids=id1,id2` | Get multiple profiles |
| GET | `/api/users/{id}/followers/count` | Get follower count |
| GET | `/api/users/{id}/exists` | Check whether a user exists |

### Post Service — `http://localhost:8082/post-service`

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/posts` | Create a post |
| GET | `/api/posts` | List posts; optional `authorIds=id1,id2` filter |
| GET | `/api/posts/{id}` | Get a post |
| DELETE | `/api/posts/{id}` | Delete a post |
| GET | `/api/posts/{id}/detail` | Get a post through the simulated-call path |
| GET | `/api/posts/batch?ids=id1,id2` | Get posts by ID through the simulated-call path |
| GET | `/api/posts/author/{authorId}/count` | Count an author's posts through the simulated-call path |
| GET | `/api/posts/author/{authorId}/posts` | Get an author's posts through the simulated-call path |
| POST | `/api/management/failure-simulator/slowness/start?delayMs=3000` | Turn on simulated delay |
| POST | `/api/management/failure-simulator/slowness/stop` | Turn off simulated delay |
| POST | `/api/management/failure-simulator/failure/start?count=3` | Fail the next number of simulated calls |
| POST | `/api/management/failure-simulator/failure/stop` | Turn off simulated failures |
| GET | `/api/management/failure-simulator/status` | Read simulator state |
| POST | `/api/management/failure-simulator/reset` | Reset the simulator |

## Monitoring

The Feed Service exposes Spring Boot Actuator endpoints under `http://localhost:8083/feed-service/actuator`. Useful paths include `/health`, `/metrics`, `/circuitbreakers`, `/retries`, `/bulkheads`, and `/timelimiters`. User and Post Services expose `/actuator/health`, `/actuator/info`, and `/actuator/metrics` under their context paths.

## More detail

See [RESILIENCE-GUIDE.md](RESILIENCE-GUIDE.md) for the actual resilience configuration, failure-simulation examples, and implementation notes. The repository also includes `test-resilience-patterns.ps1` and `test-resilience-patterns.sh`; review the guide's current-implementation notes before relying on their expected outcomes.
