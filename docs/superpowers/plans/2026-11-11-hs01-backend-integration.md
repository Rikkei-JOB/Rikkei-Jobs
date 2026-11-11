# HS-01 backend integration implementation plan

**Goal:** Finish HS-01 backend after NT-01 merge at `17a7f2e`, retaining the tested calculation core and adding authenticated APIs, persistent inputs, privacy checks, migrations and runnable samples. User requested execution; keep FE and other tasks out of scope.

**Architecture:** Reuse `common-core` JWT and errors. Canonical controller `/profiles`, gateway `/api/v1/profiles`. Profile-owned tables/migration history in PostgreSQL schema `profile` to coexist with identity's public schema/history. `external_records` is the documented HS-01 fallback input until NT-05; do not implement NT-05's network synchronization.

## Constraints and decisions

- GET `/profiles/me/skills` and POST `/profiles/me/skills/recompute` require CANDIDATE; UUID always comes from JWT.
- GET `/profiles/{userId}/skills` accepts signed-in users. Owner may read; others require separate consent and approval flags. Private and absent profiles both give common 404. No role-based private-profile bypass.
- Do not add auth bypass, public verified-import endpoint or publication workflow. Publication state starts private; future consent/review task owns its transitions.
- Keep `/profile/ping` and gateway route ID. Add `/api/v1/profiles/**` to existing route.
- SQL migrations V1 core, V2 catalog, V3 `skill_profile_states` and `external_records` under own `profile` schema; no identity migration edits or production sample users.
- Each external record has user UUID, source LMS/EXAM/PM, sourceCode, sourceRef, title, raw score and positive scoreMax, original recordedAt, verified flag. Only verified records become evidences. Normalize `score / scoreMax * 100`; preserve timestamps at microsecond precision.
- Recompute synchronizes imported evidence from current stored external records, removes revoked/deleted/unmapped imported evidence and preserves SELF/MENTOR. Legacy trusted `importVerifiedRecords` remains an internal incremental method.
- Serialize writes per candidate using a persisted profile-state row lock. No source/score can be supplied by the HTTP recompute request.
- Demo seeder is opt-in dev only, resolves real user UUIDs by `public.users.lms_user_id` after NT-01, seeds 3 candidates and SELF-only evidence; repeat is idempotent and publication stays private. Normal startup never creates users or contacts mock/identity.
- Keep `self_score` independent; no multi-SELF aggregation or editing endpoints in HS-01.
- Validate with H2 + actual disposable PostgreSQL, preserving the user's databases. No commit/push without request.

## Tasks and verification

- [x] Baseline after merge: resolve new JWT dependencies and identify failing pre-existing ping context.
- [x] Write RED persistence tests for Flyway startup and external-record recompute (normalization, update/revoke/delete, isolation, preserved SELF and rollback), then implement external record/state/repositories, migrations and guarded access service.
- [x] Write RED HTTP/gateway tests, then implement the 3 endpoints and gateway forwarding. Test missing/invalid/expired JWT, must-change-password, wrong role, owner, private/missing/consent-only/approval-only/public/revoked visibility and malicious userId.
- [x] Add opt-in repeatable dev samples linked to NT-01 UUIDs, plus documented import/recompute walkthrough and OpenAPI/Postman collection.
- [x] Verify real PostgreSQL migrations alongside identity history; HTTP SSO JWT from real identity to profile through gateway; repeat seed, add result, recompute, SELF-only and private404.
- [x] Run clean verify across backend, fresh-context review, resolve material findings and leave code reviewable.

Final evidence: full reactor verify 12/12 modules, 142 tests, 0 failures/errors/skips, including 80 profile-service cases. PostgreSQL-only snapshot regression is enabled by HS01_TEST_DB_URL; read isolation fixed with REPEATABLE_READ after a deterministic mixed-score/evidence RED case. Postman live SSO/gateway run:18 requests/31 assertions,0 failures. Owned application processes and disposable PostgreSQL cluster were stopped and its temporary data removed.

The existing score formula and 49 core tests remain binding. Record verification and any limitations in `docs/hs01-independent-backend.md` (updated to final backend state).
