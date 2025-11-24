# Trello Integration

Configure Trello in `src/main/resources/application.properties` (or env vars):

```
trello.api.key=${TRELLO_KEY:YOUR_TRELLO_KEY}
trello.api.token=${TRELLO_TOKEN:YOUR_TRELLO_TOKEN}
trello.api.base-url=https://api.trello.com/1
trello.rate.limit.requests=295
trello.rate.limit.window-seconds=10
```

Endpoints (all under `/api`):
- `GET /trello/boards/{userId}`: boards for Trello member
- `GET /trello/boards/{boardId}/lists`: lists on a board
- `GET /trello/lists/{listId}/cards`: cards in a list
- `GET /dashboard/trello/{projectId}`: aggregated board view by project’s `trelloBoardId`
- `GET /trello/project/{projectId}/sync` (Admin/Manager): fetch Trello data for project
- `POST /trello/project/{projectId}/sync-tasks` (Admin/Manager): create tasks from Trello cards

Project linking:
- `Project.trelloBoardId` is persisted and exposed in `ProjectDto`.
- Use `PUT /projects/{id}` with payload `{ name, teamId, trelloBoardId }` to link/unlink.

Rate limits:
- Client honors Trello headers `x-ratelimit-remaining` / `x-ratelimit-reset` and sleeps until reset.
- Soft window limiter ensures ~295 req / 10s max.

Errors:
- Trello API failures map to HTTP 502 with `TrelloApiException`.
