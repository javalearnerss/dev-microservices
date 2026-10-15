# Pulsefeed Platform

Pulsefeed is a minimal Java/Spring Boot social feed composed of three independent services. Each service uses in-memory storage for now, so its data is lost when the process restarts.

## Services

| Service | Port | Endpoints |
| --- | ---: | --- |
| User service | 8081 | `POST /user-service/api/users`, `GET /user-service/api/users`, `GET /user-service/api/users/{id}`, `DELETE /user-service/api/users/{id}` |
| Post service | 8082 | `POST /post-service/api/posts`, `GET /post-service/api/posts`, `GET /post-service/api/posts/{id}`, `DELETE /post-service/api/posts/{id}` |
| Feed service | 8083 | `GET /feed-service/api/feed` |

Every URL starts with the owning service's name (`/user-service`, `/post-service`, `/feed-service`, set through `server.servlet.context-path`), so a load balancer can route by path prefix, e.g. `/user-service/**` to the user service. The prefix is not stripped; services expect it.

The feed service fetches the latest posts from the post service. Configure the post service base URL with `POST_SERVICE_URL` (default: `http://localhost:8082/post-service`; include the `/post-service` prefix, or point it at the load balancer).

## Run locally

Requires JDK 17+.

Open a terminal in each service directory and run the Gradle wrapper:

```powershell
.\gradlew.bat bootRun
```

Start the post and user services before using the feed service.

## Try it

Create a user:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8081/api/users `
  -ContentType 'application/json' -Body '{"name":"Ada","email":"ada@example.com"}'
```

Create a post using the returned user's `id`:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8082/api/posts `
  -ContentType 'application/json' -Body '{"authorId":"USER_ID","content":"Hello, Pulsefeed!"}'
```

Read the feed:

```powershell
Invoke-RestMethod -Uri http://localhost:8083/api/feed
```
