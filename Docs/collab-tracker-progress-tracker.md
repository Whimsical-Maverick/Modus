# Real-Time Collaborative Task Tracker — Build Tracker

How to use this: check items off as you go. Each phase ends with a "Break it / Understand it" section —
don't skip these, they're where the actual learning happens, not the checkbox above them.

Progress: `0 / 7 phases complete`

---

## Phase 0 — Setup
- [ ] Init Spring Boot project (Web, Security, Data JPA, PostgreSQL Driver, Validation, WebSocket)
- [ ] Init React app (Vite)
- [ ] `docker-compose.yml` with Postgres + pgAdmin
- [ ] Set up Flyway, write `V1__init.sql`
- [ ] Git repo + README with project scope
- [ ] App boots, connects to Dockerized Postgres, migration runs

**Exit check:** app boots clean, migration applied, nothing hardcoded that should be an env var.

---

## Phase 1 — Core Domain & Auth
### Domain modeling
- [ ] Entities: User, Workspace, WorkspaceMember (role), Project, Board, BoardList, Card, Comment
- [ ] Migration for all tables — FKs, indexes on `card.board_id`, `card.status`, etc.

### Auth
- [ ] JWT access token + refresh token (rotated, stored hashed)
- [ ] Register / login / refresh / logout endpoints
- [ ] BCrypt password hashing
- [ ] Workspace-scoped authorization (not just global roles)

### CRUD
- [ ] Workspace → Project → Board → List → Card, scoped to membership
- [ ] DTOs + mappers (no raw entities exposed)
- [ ] Global exception handler → RFC 7807 style errors

**Build → Break → Understand:**
- [ ] Trigger an N+1 query on purpose (log SQL, count queries), then fix with `JOIN FETCH`
- [ ] Decode your own JWT on jwt.io, explain what's in the payload and why
- [ ] Turn on `show-sql`, compare Hibernate's generated SQL to what you'd hand-write
- [ ] One-line bug log entry for anything that broke this phase

**Exit check:** register → login → create workspace/board/card → all auth-checked, via Postman.

---

## Phase 2 — React Frontend on REST
- [ ] Login/register pages, token stored in memory
- [ ] Axios refresh-token interceptor
- [ ] Board view: lists as columns, cards inside
- [ ] Drag-and-drop (dnd-kit) against plain REST (`PATCH /cards/{id}/move`)
- [ ] Workspace/project navigation

**Build → Break → Understand:**
- [ ] Deliberately fire two simultaneous requests that both 401 — watch what happens to your refresh logic, fix the race
- [ ] Note: did you reach for a state library? Was it actually necessary, or did prop drilling work fine?
- [ ] Bug log entry

**Exit check:** full app usable end-to-end via UI, no real-time yet.

---

## Phase 3 — Real-Time Layer
- [ ] WebSocket/STOMP endpoint, JWT-secured handshake
- [ ] Event schema: CARD_CREATED, CARD_MOVED, CARD_UPDATED, CARD_DELETED, COMMENT_ADDED
- [ ] Server publishes to `/topic/board/{boardId}` after DB commit
- [ ] Client subscribes, applies events to local state (no full refetch)
- [ ] Fix the "echo" problem (your own event bouncing back to you)

**Build → Break → Understand:**
- [ ] Open devtools, watch raw STOMP frames on the wire — read one end to end
- [ ] Explain in your own words why you solved the echo problem the way you did (idempotency / event sourcing angle)
- [ ] Bug log entry

**Exit check:** two browser tabs, move a card in one, see it update live in the other.

---

## Phase 4 — Caching & Background Processing
### Redis
- [ ] Cache board state keyed by `board:{id}`
- [ ] Pick invalidate-then-refetch OR write-through — implement one

### Background jobs
- [ ] Async activity log on every mutation (`@Async` or queue)
- [ ] Async "mentioned in comment" notification
- [ ] (Optional, if time allows) swap `@Async` for RabbitMQ

**Build → Break → Understand:**
- [ ] Implement naive cache invalidation first, feel the cache-miss storm, then upgrade — don't skip straight to the good version
- [ ] Deliberately misconfigure the async thread pool (tiny pool size), watch requests queue, then fix it
- [ ] Bug log entry

**Exit check:** board reads hit Redis (verify in logs), activity log fills without slowing card mutations.

---

## Phase 5 — API Hardening & Tests
- [ ] Pagination + filtering on list endpoints
- [ ] Rate limiting on auth endpoints (bucket4j)
- [ ] API versioning (`/api/v1/...`)
- [ ] Input validation edge cases, correct HTTP status codes
- [ ] Unit tests for services
- [ ] Integration tests with **Testcontainers** (real Postgres, not H2)

**Build → Break → Understand:**
- [ ] Find one place H2 would've lied to you vs real Postgres (case sensitivity, JSON columns, etc.) — write it down even if hypothetical
- [ ] Bug log entry

**Exit check:** `mvn test` runs clean in CI against a real Postgres container.

---

## Phase 6 — Deployment & Observability
- [ ] GitHub Actions: test → build image → push
- [ ] Deploy via Docker Compose to VPS, or Railway/Render
- [ ] Actuator + Micrometer → Prometheus → Grafana dashboard
- [ ] Structured JSON logging with correlation/request IDs across the async/WS flow

**Build → Break → Understand:**
- [ ] Deliberately break the deploy (bad env var / wrong port), find it using only your own logs/metrics — no guessing
- [ ] Trace one request end-to-end through the correlation ID, across the async boundary
- [ ] Bug log entry

**Exit check:** app live at a public URL, dashboard shows real metrics, one full request trace visible end-to-end.

---

## Running logs (keep these updated throughout, not just at the end)

### Bugs I hit
| Phase | What broke | Why | Fix |
|---|---|---|---|
| | | | |

### Phase retros (one line: "what would a senior engineer criticize here?")
| Phase | Note |
|---|---|
| | |
