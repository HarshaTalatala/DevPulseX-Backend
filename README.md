# DevPulseX Backend

Spring Boot API for DevPulseX. The backend provides authentication, OAuth integrations, project and team management, task and issue tracking, GitHub analytics, deployments, and Trello synchronization.

## Stack

- Java 25
- Spring Boot 3.5
- Spring Web MVC and WebFlux
- Spring Data JPA and Hibernate
- Spring Security with JWT authentication
- PostgreSQL in production
- H2 in-memory database for local development by default
- Maven Wrapper
- Springdoc OpenAPI / Swagger UI

## Prerequisites

- JDK 25
- Git
- Optional: PostgreSQL for a persistent local database
- Optional: Docker Desktop for the container workflow

Verify Java:

```powershell
java -version
```

The Maven enforcer requires Java 25.

## Local Development

From the `backend/` directory, copy the environment template:

```powershell
Copy-Item .env.example .env
```

Set at least a long local `JWT_SECRET` in `.env`. OAuth credentials are required only for the providers you want to use. The default application configuration uses an in-memory H2 database when `SPRING_DATASOURCE_URL` is not set.

Start the API with the Maven Wrapper:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts on `http://localhost:8080` by default.

To use another port for a local run:

```powershell
$env:SERVER_PORT = "8081"
.\mvnw.cmd spring-boot:run
```

For Bash or macOS/Linux:

```bash
./mvnw spring-boot:run
```

The local H2 database is memory-only and is recreated when the application restarts. Use PostgreSQL when you need data to persist between runs.

## Configuration

Runtime configuration is supplied through environment variables. Do not commit `.env` or real credentials.

| Variable | Local purpose |
| --- | --- |
| `SERVER_PORT` | Local HTTP port. Defaults to `8080`. |
| `SPRING_DATASOURCE_URL` | Optional JDBC URL. If omitted, H2 is used. |
| `DB_USERNAME` / `DB_PASSWORD` | Database credentials. |
| `JWT_SECRET` | Required signing secret for JWTs. Use a long random value. |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins. |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | GitHub OAuth credentials. |
| `GITHUB_REDIRECT_URI` | GitHub callback URL, normally `http://localhost:3000/auth/callback`. |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth credentials. |
| `GOOGLE_REDIRECT_URI` | Google callback URL, normally `http://localhost:3000/auth/callback`. |
| `TRELLO_KEY` | Trello application key. |
| `TRELLO_SECRET` | Optional Trello application secret. |
| `TRELLO_REDIRECT_URI` | Trello callback URL. |
| `TRELLO_ENC_SECRET` | Secret used to protect stored Trello credentials. |

The source of truth for local defaults is `src/main/resources/application.properties`. Production overrides are in `src/main/resources/application-prod.properties`.

## API Checks

Health check:

```powershell
Invoke-WebRequest -UseBasicParsing http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

When Swagger is enabled, open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## API Surface

All application routes are prefixed with `/api`.

| Area | Main routes |
| --- | --- |
| Authentication | `/api/auth/register`, `/api/auth/login`, `/api/auth/github`, `/api/auth/google` |
| Users | `/api/users` |
| Teams | `/api/teams` |
| Projects | `/api/projects` |
| Tasks | `/api/tasks` |
| Issues | `/api/issues` |
| Commits | `/api/commits` |
| Deployments | `/api/deployments` |
| Dashboard analytics | `/api/dashboard/summary`, `/api/dashboard/projects`, `/api/dashboard/users` |
| GitHub analytics | `/api/github/insights`, `/api/github/repositories` |
| Trello | `/api/trello/boards`, `/api/trello/boards/{boardId}/lists`, `/api/trello/lists/{listId}/cards` |
| Trello sync | `/api/trello/project/{projectId}/sync-tasks` |

Most protected routes require a valid JWT supplied by the frontend authentication flow.

## Tests and Build

Run the test suite:

```powershell
.\mvnw.cmd test
```

Build the deployable JAR:

```powershell
.\mvnw.cmd clean package
```

Build without tests:

```powershell
.\mvnw.cmd clean package -DskipTests
```

The packaged application is written to `target/DevPulseX-Backend-1.0.0-mvp.jar`.

## Docker and Deployment

The repository root includes Docker Compose for the backend workflow:

```powershell
# From the repository root
docker compose build
docker compose up -d
```

The Docker deployment uses the production profile and PostgreSQL settings. Configure the required production environment variables before starting it. Stop the stack with:

```powershell
docker compose down
```

Render deployment uses the backend `Dockerfile`, `build.sh`, and `start.sh`. Production requires PostgreSQL and the OAuth, JWT, CORS, and Trello variables listed above. See `docs/GO_LIVE_CHECKLIST.md` before releasing.

## Related Documentation

- `docs/GITHUB_ANALYTICS_API.md`
- `docs/POSTMAN.md`
- `docs/TRELLO.md`
- `docs/GO_LIVE_CHECKLIST.md`
- `../frontend/README.md`
