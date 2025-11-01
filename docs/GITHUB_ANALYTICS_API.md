# GitHub Analytics API Testing Guide

## Endpoint Details

**URL:** `GET http://localhost:8080/api/github/insights`

**Authentication:** Required (Bearer JWT Token)

**Description:** Fetches GitHub analytics for the authenticated user including repository count, pull requests, and recent commits (last 7 days).

---

## Request Example

```http
GET http://localhost:8080/api/github/insights HTTP/1.1
Host: localhost:8080
Authorization: Bearer <your-jwt-token>
Accept: application/json
```

---

## Response Example

### Success Response (200 OK)

```json
{
  "username": "johndoe",
  "repoCount": 42,
  "totalPullRequests": 156,
  "recentCommits": 23,
  "fetchedAt": "2025-11-01T18:10:00Z",
  "avatarUrl": "https://avatars.githubusercontent.com/u/123456?v=4",
  "profileUrl": "https://github.com/johndoe"
}
```

### Error Responses

#### 401 Unauthorized
```json
{
  "status": 401,
  "message": "Unauthorized"
}
```

#### 400 Bad Request (No GitHub Token)
```json
{
  "status": 400,
  "message": "User has no GitHub access token"
}
```

#### 500 Internal Server Error
```json
{
  "status": 500,
  "message": "Error fetching GitHub insights"
}
```

---

## Testing with cURL

### Windows PowerShell
```powershell
$token = "your-jwt-token-here"
Invoke-RestMethod -Uri "http://localhost:8080/api/github/insights" `
  -Method Get `
  -Headers @{
    "Authorization" = "Bearer $token"
    "Accept" = "application/json"
  }
```

### Linux/Mac (bash)
```bash
curl -X GET "http://localhost:8080/api/github/insights" \
  -H "Authorization: Bearer your-jwt-token-here" \
  -H "Accept: application/json"
```

---

## Frontend Integration

The frontend automatically calls this endpoint when the Dashboard page loads:

```typescript
// Hook: useGithubInsights()
// Location: src/hooks/useGithub.ts
// Used in: src/app/dashboard/page.tsx

const { data: gh } = useGithubInsights();
```

The data is then displayed in:
1. **Stat Cards** - Shows commits (7d) and total PRs
2. **GitHub Analytics Card** - Dedicated section with:
   - Repository count
   - Recent commits (7 days)
   - Total pull requests
   - GitHub avatar
   - Link to GitHub profile

---

## Security Notes

1. **Authentication Required:** All requests must include a valid JWT token obtained via GitHub OAuth login
2. **GitHub Token Protected:** The user's GitHub access token is stored in the database but never exposed in API responses
3. **CORS Enabled:** Frontend origin (http://localhost:3000) is whitelisted in Spring Security configuration
4. **Rate Limiting:** GitHub API has rate limits (5000 requests/hour for authenticated users). The service includes error handling for API failures.

---

## Error Handling

The backend service includes comprehensive error handling:

1. **GitHub API Failures:** Returns zero values instead of failing completely
2. **Missing Token:** Returns 400 Bad Request with clear message
3. **Invalid User:** Returns 401 Unauthorized
4. **Network Issues:** Logs error and returns graceful fallback

All errors are logged with appropriate log levels for debugging.

---

## Monitoring & Logging

The following log messages are generated:

```
INFO  - Fetching GitHub insights for user: user@example.com
INFO  - Fetching GitHub insights for user: johndoe
INFO  - GitHub insights fetched successfully for johndoe: repos=42, PRs=156, commits=23
INFO  - Successfully fetched GitHub insights for user: user@example.com
```

Errors are logged at ERROR or WARN level with stack traces when applicable.
