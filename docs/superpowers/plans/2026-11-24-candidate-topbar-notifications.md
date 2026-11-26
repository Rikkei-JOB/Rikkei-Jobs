# Candidate Topbar Notifications Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Candidate-only top navigation with a real persisted notification inbox.
**Architecture:** Small JDBC service/controller in recruitment-service; transactional event hooks in existing status/interview services; Base UI topbar and TanStack Query inbox scoped to the current session.
**Tech Stack:** Existing Spring Boot/PostgreSQL/Flyway/JUnit and Next.js/React/Base UI/Tailwind/Playwright.
**Spec:** ../specs/2026-11-24-candidate-topbar-notifications-design.md

## Global constraints

- User explicitly selected full backend functionality. Implement and verify without another scope confirmation.
- Preserve existing email delivery, application transitions and non-candidate shells. No new runtime dependencies.
- Never use existing development databases for tests. Create a dedicated cluster on 55447 in `.runtime`.
- Recipients come from authenticated ownership/business rows; never trust client-supplied user IDs or external hrefs.

## Review focus

1. Cross-account list/read operations and stale frontend query/mutation completion cannot reveal previous account data.
2. Deduplication and rollback keep inbox and business state consistent, without depending on email success.
3. Offer links identify the real offer; cancelled and rescheduled interviews remain understandable.
4. Badge, pagination and read-all remain consistent after writes, background updates and failures.
5. Responsive menus, popup focus and mobile sticky application controls work together, other roles unchanged.

## Tasks

- [x] Add failing backend tests for contract/ownership/status, offer and interview generation; run RED against current code.
- [x] Add V8 migration, `CandidateNotificationService` (typed Feed/Item records) and `CandidateNotificationController`. Implement transactional publish/status hooks and interview proposal/confirm/cancel/reminder hooks; run GREEN and recruitment tests.
- [x] Add failing browser tests for candidate navigation and inbox. Create candidate shell/topbar, notification client and bell/panel; retain old shell for every other role. Use API auth client and Base UI primitives; run GREEN.
- [x] Capture local responsive screens, inspect, fix material issues, run full frontend suite/type-check/lint/build and backend suite.
- [x] Request independent final review, fix material issues with regression tests, document contract/results and leave changes for review.

## Interfaces

- Backend: `/recruitment/me/notifications` GET returns `Feed(List<Item> items,long total,int page,int size,long unreadCount)`; `Item(UUID id,String type,String title,String body,String href,Instant createdAt,Instant readAt)`; POST `/{id}/read` and `/read-all` return 204.
- Event writer: `publish(UUID userId,UUID applicationId,String eventKey,String type,String title,String body,String href,Instant now)` under existing transaction. Status writer receives `Application`, history UUID, target status and timestamp. No email delivery dependency.
- Frontend service uses `/recruitment/me/notifications`; useQuery key includes current user ID/token and page. Shared AppShell delegates candidate only to new shell, passing user/children.

## Record

- Preflight: status transitions and interview lifecycle already share transactions; notification publisher has no service dependency that could create a Spring cycle. Offer row is flushed before OFFER transition, so its ID can be selected transactionally.

- Database: native PostgreSQL executables were absent; used an isolated Docker postgres:16-alpine container on 127.0.0.1:55447 with tmpfs, not existing dev databases.
- Backend RED: four notification contract tests returned missing endpoint 404 before implementation. Final recruitment suite: 154 tests, 0 failures/errors; upstream modules pass; includes real HTTP server/SQL event test.
- Frontend RED: horizontal navigation and Escape handling missing. Final full suite: 71 passed, 1 live test skipped. Added delayed read/logout/next-session regression and fixed partial user display-name handling exposed by UT-07 tests.
- Production build in isolated output, type-check, lint (0 errors/1 pre-existing warning) pass. Nine new screenshots inspected. OpenAPI parsed. Legacy screenshots restored.
- Independent review: no material introduced findings. Browser push/backfill/email redesign/other-role shell redesign remain outside approved scope. User's new banner request is a proposal only; no banner code added.
- No commit, push, stack restart or deployment performed.
