# Candidate top bar and durable in-app notifications

User approved candidate-only top bar and full backend notification functionality on 2026-11-24. Implement inline; no changes to HR/staff/admin shells or browser push subscription.

## UI

Desktop ≥1200px: 64px white sticky bar, Rikkei Jobs logo, Việc làm, Ứng tuyển (Đơn ứng tuyển/Phỏng vấn/Offer), Hồ sơ, Phát triển (Lộ trình/Trợ lý AI), notification bell, account menu (name/role/logout). Active group follows nested route. Main content centered, max-width 1200px. Tablet/mobile: logo, bell, account and menu trigger; expanded navigation closes on route change/Escape. Touch targets ≥44px. Red #B4232D matches candidate career UI. Adjust detail sticky offset for the taller candidate header.

Bell shows unread count (99+ cap), opens latest notifications, unread styling, timestamps, per-item read and mark-all-read, pagination, loading/empty/error/retry and mutation-error feedback. Clicking a notification marks it read then navigates to its internal destination. Failed marking preserves unread state and offers retry. Base UI popover/menu provide focus and dismissal. Mobile panel fits viewport. Poll every 60s only in foreground; refetch on focus and panel open; query cache scoped to user ID and token, cancel on session changes. Server data is authoritative across devices; no localStorage notification persistence.

## Backend

Recruitment migration V8 adds `candidate_notifications` referencing application with cascade delete, user ID, deduplicating event key, type/title/body/internal href, created/read timestamps and indexes by recipient/date/unread. JDBC service joins existing transaction (MANDATORY) when recording events; read endpoints require CANDIDATE and derive owner from JWT. New event write failure rolls back business change. Existing email workers stay independent.

Events: successful application status changes; interview invitation/new proposal, confirmed selection, cancellation and 24h/1h reminders; OFFER status notification links to the actual offer detail. Do not expose HR notes or private email payloads. Deduplicate by status history/proposal/schedule+event type. Existing history is not backfilled. Read operations are idempotent. Read-all is a single SQL statement with normal PostgreSQL statement snapshot semantics.

## Contract

Base through gateway: `/api/v1/recruitment/me/notifications`.

- GET `?page=1&size=10&unreadOnly=false`: `{items:[{id,type,title,body,href,createdAt,readAt}],total,page,size,unreadCount}`. page ≥1, size 1–100. Newest first, UUID descending tie-break; unreadCount counts all unread for recipient, independently of page/filter.
- POST `/{id}/read`: 204, idempotent; missing/foreign notification returns 404.
- POST `/read-all`: 204, idempotent; affects caller only.
- Errors follow `{code,message}`: 400 invalid query/ID, 401 no session, 403 wrong role, 404 missing/foreign ID. No userId accepted from clients.

## Verification

Backend integration tests use a new disposable local PostgreSQL instance, never existing dev data: auth/ownership, pagination/counts, idempotency, rollback, independent email failure, status+offer links, interview duplicate picks/reschedule/cancel/reminder events. Run recruitment/common modules' tests. Browser tests use API fixtures for navigation, read actions/failure/pagination, isolation after account change, mobile keyboard/focus/overflow; preserve existing role suites. Capture desktop/tablet/mobile then inspect; run frontend type-check/lint/build/full E2E. Include contract and reproducible commands with delivery; retain uncommitted changes.
