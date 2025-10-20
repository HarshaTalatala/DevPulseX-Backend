Testing DevPulseX API with Postman

Prerequisites
- Java and Maven installed
- PostgreSQL running locally if you run against your dev DB (default app profile)
- DevPulseX Backend running locally on http://localhost:8080
- Postman installed

Start the backend
1. From the project root, run: mvn spring-boot:run
2. Ensure the app started on http://localhost:8080
3. Swagger UI (optional): http://localhost:8080/swagger-ui.html

Quick flow overview
- Register a user at POST /api/auth/register
- Login at POST /api/auth/login to receive a JWT token
- Use Bearer <token> in the Authorization header for protected endpoints

Base URL
- http://localhost:8080

Register
- Method: POST
- URL: {{baseUrl}}/api/auth/register
- Body (JSON):
  {
    "name": "Admin User",
    "email": "admin@example.com",
    "password": "Admin@123",
    "role": "ADMIN"
  }
- Expected: 200 OK with JSON containing token and user

Login
- Method: POST
- URL: {{baseUrl}}/api/auth/login
- Body (JSON):
  {
    "email": "admin@example.com",
    "password": "Admin@123"
  }
- Tests (Postman → Tests tab):
  pm.test("Store JWT token", function () {
    const json = pm.response.json();
    pm.environment.set("token", json.token);
  });
- Expected: 200 OK and environment variable token set

Using the token
- For protected requests, set the Authorization header in Postman:
  Key: Authorization
  Value: Bearer {{token}}
- Or set the request Authorization tab to type: Bearer Token with value: {{token}}

Example protected endpoint: Get users
- Method: GET
- URL: {{baseUrl}}/api/users
- Headers: Authorization: Bearer {{token}}
- Expected:
  - 200 OK when token belongs to ADMIN or MANAGER
  - 403 Forbidden without token or with insufficient role

Public GET endpoints (no token needed)
- GET {{baseUrl}}/api/projects
- GET {{baseUrl}}/api/teams
- GET {{baseUrl}}/api/tasks
- GET {{baseUrl}}/api/issues
- GET {{baseUrl}}/api/deployments
- GET {{baseUrl}}/api/commits

Common pitfalls
- 403 vs 401: Depending on security settings, unauthenticated access may return 403 Forbidden. Use a valid token with proper role.
- Token missing/expired: Ensure you have logged in recently and stored the token to the {{token}} variable.
- CORS: Not applicable to Postman, but relevant for browser clients.

Ready-to-import Postman collection
- Import the JSON file at postman/DevPulseX.postman_collection.json
- Create (or let Postman create) an environment with variables:
  - baseUrl: http://localhost:8080
  - email: admin@example.com
  - password: Admin@123
  - token: (left blank initially; will be set by the Login request test)

Notes on roles
- /api/users endpoints require roles as annotated in code:
  - GET /api/users → ADMIN or MANAGER
  - GET /api/users/{id} → ADMIN, MANAGER, or DEVELOPER
  - POST /api/users → ADMIN
  - PUT /api/users/{id} → ADMIN or MANAGER
  - DELETE /api/users/{id} → ADMIN

Troubleshooting
- Verify the app is running and DB connection details in src/main/resources/application.properties are valid for your environment.
- Check logs for errors. Security logs at INFO; app logs at DEBUG under com.devpulsex.
