# DevPulseX Backend Go-Live Checklist

Use this checklist before every production release.

## 1) Production Environment Validation

Set and verify all required production variables:

- `DATABASE_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS` (public domains only, comma-separated)
- `GITHUB_CLIENT_ID`
- `GITHUB_CLIENT_SECRET`
- `GITHUB_REDIRECT_URI`
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `GOOGLE_REDIRECT_URI`
- `TRELLO_KEY`
- `TRELLO_ENC_SECRET`
- `TRELLO_REDIRECT_URI`

Notes:

- Do not use `*`, `localhost`, or `127.0.0.1` in `CORS_ALLOWED_ORIGINS` for production.
- Startup is expected to fail fast in `prod` profile if required values are missing.

## 2) Pre-Deploy Release Gate

Run from `backend/`:

```powershell
.\mvnw.cmd "-Dtest=AuthIntegrationTest,UserEndpointIntegrationTest,AccessScopeLayer1L1Test,AccessScopeExtendedLayer1L1Test" test
.\mvnw.cmd -q -DskipTests package
```

Run from `frontend/`:

```powershell
npm run build
```

Expected outcome:

- All commands complete successfully with no test failures.

## 3) Post-Deploy Smoke Checks

Verify:

- `GET /actuator/health` returns `UP`
- Register and login succeed
- OAuth callback flow returns authenticated session/token
- Team/project/task/issue create and list APIs work
- Cross-team access to project/task/issue/commit/deployment is denied (403)

## 4) Monitoring and Logs

After deployment, monitor:

- Application startup logs (confirm no missing config)
- Authentication errors (JWT, OAuth callback failures)
- 5xx error spikes in first 15-30 minutes

## 5) Rollback Procedure

If release is unstable:

1. Roll back to last known good deployment in hosting platform.
2. Confirm `GET /actuator/health` is `UP` after rollback.
3. Re-run smoke checks for login and core CRUD paths.
4. Capture incident summary (trigger, impact, recovery time, follow-up fix).

## 6) Sign-Off Template

- Build gate: PASS/FAIL
- Smoke tests: PASS/FAIL
- Health endpoint: PASS/FAIL
- Observability checks: PASS/FAIL
- Final decision: GO/NO-GO
- Approved by: <name>
- Timestamp (UTC): <yyyy-mm-dd hh:mm>
