# Modus frontend

This React/Vite client includes registration, login, automatic access-token renewal, logout, workspace/board browsing, list and card creation, and live card-move updates. The Workspace screen also shows members and pending invitations, lets owners rename or archive a workspace, and lets owners invite registered users or revoke pending invitations. The Invitations screen lets invitees accept or decline; Archived lets owners restore; Activity shows the last 50 card-move log entries for a board.

## Run locally

1. Start PostgreSQL and the backend (`D:\Modus\backend`) on port 8080. Restart the backend after pulling these changes so the new read endpoints are available. Flyway runs migrations at backend startup.
2. In `D:\Modus\frontend`, run `pnpm install`, then `pnpm dev`.
3. Open **http://localhost:5173** (use `localhost`, not `127.0.0.1`, so the backend's development Origin check matches).

Vite proxies `/api` and `/ws` to the backend. No API hostname or secret belongs in the frontend bundle.

## Authentication flow

- Login returns an access token in the response body and places the refresh token in an HttpOnly, SameSite=Strict cookie. The client keeps the access token only in JavaScript memory.
- A full page reload calls `/api/v1/auth/refresh` once to recover an access token from the cookie.
- A protected request that receives 401 triggers one refresh attempt and one retry. Requests in the same tab share one in-flight refresh call.
- Logout revokes the server-side refresh-token family, clears the cookie, and clears the in-memory access token. An already-issued access JWT remains valid until its short expiry.

The development cookie is not marked `Secure` because the local site uses HTTP. Use HTTPS and set `security.refresh-cookie-secure=true` in production. Set `security.frontend-origin` to the actual frontend origin and update the WebSocket allowed origin when deploying.

Opening multiple tabs is currently a limitation: refreshes in different tabs are not coordinated. Two tabs refreshing the same rotating cookie simultaneously can cause reuse detection to revoke that login session. A future multi-tab client should coordinate refresh calls across tabs (for example with a shared browser lock and notification channel).

## Checks

Run `pnpm build` for TypeScript and production bundling. Backend unit tests live in `D:\Modus\backend\src\test\java\com\tracker`.

## Try the management flow

1. Register two accounts (for example, an owner and a teammate). Sign in as the owner and create a workspace.
2. Open **Workspace** in the sidebar. Rename it, invite the teammate's registered email, and check the pending-invitation list. No email is sent by the current backend.
3. Log out and sign in as the teammate. Open **Invitations**, then accept. The workspace now appears in the teammate's sidebar. The owner sees them in **Workspace → Members**.
4. As the owner, archive the workspace. It disappears from active workspaces and is available in **Archived** for restoration.
5. Move a card, then open **Activity** for that board and refresh after the background listener has recorded the move.

Owners alone can send/revoke invitations, rename, archive, and restore. The backend checks those permissions; the frontend hides owner controls for ordinary members. The members endpoint is read-only—joining still requires invitation acceptance.
