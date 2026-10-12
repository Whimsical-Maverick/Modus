# Real-Time Collaborative Task Tracker — Phase-Wise Build Plan

Stack: Java, Spring Boot, PostgreSQL, React, Docker, Redis, WebSockets

---

## Phase 0 — Setup (2-3 days)
- Init Spring Boot project (Spring Initializr): Web, Security, Data JPA, PostgreSQL Driver, Validation, WebSocket
- Init React app (Vite), set up folder structure
- `docker-compose.yml` with Postgres + pgAdmin so DB is containerized from day 1
- Set up Flyway (or Liquibase) — write your first migration (`V1__init.sql`) before writing any entity
- Git repo, README with project scope, `.env` handling for secrets

**Exit criteria:** Spring Boot app boots, connects to Dockerized Postgres, Flyway runs a migration.

---

## Phase 1 — Core Domain & Auth (1.5–2 weeks)
**Domain modeling**
- Entities: `User`, `Workspace`, `WorkspaceMember` (role: owner/admin/member), `Project`, `Board`, `BoardList`, `Card`, `Comment`
- Write the migration for all tables with proper FKs, indexes on foreign keys and frequently-filtered columns (e.g. `card.board_id`, `card.status`)

**Auth**
- Spring Security + JWT: access token (short-lived) + refresh token (rotated, stored hashed in DB)
- Registration, login, refresh, logout endpoints
- Password hashing (BCrypt)
- Role-based authorization at the workspace-membership level (not just global roles) — this is the part that actually tests DB design skill

**CRUD**
- Workspace → Project → Board → List → Card, all scoped to workspace membership
- DTOs + mappers (don't expose entities directly)
- Global exception handler → consistent error responses (RFC 7807 `application/problem+json` is a nice touch)

**Exit criteria:** Can register, log in, create a workspace/project/board, add cards, all via Postman/curl with proper auth checks.

---

## Phase 2 — React Frontend, Wired to REST (1–1.5 weeks)
- Auth pages (login/register), token stored in memory + refresh flow
- Board view: lists as columns, cards inside them
- Drag-and-drop reordering (dnd-kit) — even before WebSockets, get this working against plain REST (`PATCH /cards/{id}/move`)
- Basic workspace/project navigation

**Exit criteria:** You can fully use the app through the UI — create boards, add cards, drag cards between lists — no real-time yet.

---

## Phase 3 — Real-Time Layer (1–1.5 weeks)
- Add WebSocket/STOMP endpoint, secure the handshake using the JWT (interceptor on `CONNECT` frame)
- Design event schema: `CARD_CREATED`, `CARD_MOVED`, `CARD_UPDATED`, `CARD_DELETED`, `COMMENT_ADDED`
- Server: on any card mutation, publish event to `/topic/board/{boardId}` after the DB write commits
- Client: subscribe to the current board's topic, apply incoming events to local state (avoid full refetch)
- Handle the "who sent this" problem — don't re-render the sender's own optimistic update when their own event echoes back

**Exit criteria:** Open the board in two browser tabs, move a card in one, watch it update live in the other.

---

## Phase 4 — Caching & Background Processing (1–1.5 weeks)
**Redis caching**
- Cache board state (lists + cards) keyed by `board:{id}` since reads dominate writes
- Invalidate/update cache on every mutation (write-through or invalidate-then-refetch — pick one and be able to explain why)

**Background jobs**
- Activity log: every mutation writes an audit entry — do this asynchronously (`@Async` + a dedicated thread pool, or push to a queue) so it never blocks the request
- Notifications: "you were mentioned in a comment" → async email (or just log-simulated email for portfolio purposes)
- Optional: swap `@Async` for RabbitMQ if you want real message-queue experience on your resume — worth it if time allows

**Exit criteria:** Board reads hit Redis (verify via logs/metrics), activity log fills up without slowing down card mutations.

---

## Phase 5 — API Hardening (3-5 days)
- Pagination + filtering on list endpoints (`?status=done&assignee=x&page=0&size=20`)
- Rate limiting on auth endpoints (bucket4j)
- API versioning (`/api/v1/...`)
- Input validation edge cases, proper HTTP status codes throughout
- Automated tests: unit tests for services, integration tests for controllers (Testcontainers with real Postgres, not H2)

**Exit criteria:** CI can run `mvn test` and everything passes against a real Postgres via Testcontainers.

---

## Phase 6 — Deployment & Observability (1 week)
- GitHub Actions: on push → run tests → build Docker image → push to registry
- Deploy: Docker Compose to a VPS, or Railway/Render for simplicity
- Spring Boot Actuator + Micrometer → Prometheus → Grafana dashboard (latency, request rate, error rate, JVM metrics)
- Structured JSON logging with correlation/request IDs — trace a request across the async/WebSocket flow, this is your strongest interview story

**Exit criteria:** App is live at a public URL, dashboard shows real metrics, a full request trace is visible end-to-end.

---

## Total estimate: ~6-8 weeks at a steady pace

## Suggested order recap
1. Auth + CRUD (boring, correct, first)
2. Dockerize + migrations (already done in Phase 0-1, don't retrofit)
3. React UI on plain REST
4. WebSocket live updates
5. Redis + background jobs
6. API hardening + tests
7. CI/CD + observability (last, once there's something worth monitoring)
