# HS-01 independent backend implementation plan

> **For agentic workers:** Use the agreed scope below. The user explicitly requested implementation now and deferred unsettled NT-01 integration; no further design approval is needed.

**Goal:** Implement and test the independent Skill Profile data and calculation core while NT-01 is pending.

**Architecture:** JPA entities and repositories in `profile-service`, a deterministic weighted-score calculator, and a transactional service for importing normalized verified results, recomputing scores, and returning profile DTOs. SQL is prepared under `db/hs01/`, outside Flyway discovery, until migration ownership is agreed. Test fixtures stand in for NT-05; they do not define its production schema.

**Tech Stack:** Existing Java 21, Spring Boot 3.3.4, JPA, PostgreSQL; H2 only for tests; JUnit 5.

**Spec:** HS-01 and NT-01 task files supplied by the user; `docs/conventions.md`. Current request narrows delivery to the agreed independent backend work.

## Global constraints

- Only HS-01 code and its own tests/docs/build setup. Keep existing framework and packages.
- Do not implement or merge NT-01, create production users, add login, expose unsecured profile endpoints, or change frontend/gateway.
- Four HS-01 tables; `user_id` is UUID, distinct from LMS IDs. Defer FK to users, shared Flyway setup and production external_records schema.
- Verified score is weighted mean with age factor 1 within one calendar year in UTC, otherwise 0.5; exactly one year is recent. Score range 0–100 and positive weight.
- Level uses the unrounded weighted mean: BASIC <40, FAIR <70, GOOD <85, EXCELLENT otherwise; no verified evidence means null score/level.
- SELF never contributes to verified score. Preserve separately stored self_score; defer the unresolved multi-SELF aggregation/edit flow.
- Imported results are normalized, final/verified internal inputs; adapters must establish trust and normalize 0–10 to 0–100 later. PM maps to INTERNSHIP evidence.
- Preserve source_ref and recorded_at. Store mapping weight with generated evidence. Re-importing the same source reference/skill updates rather than duplicates evidence.
- Existing application datasource, security, routing and migration startup behavior remain unchanged.

## Review focus

- Rounded score near 40/70/85 must not promote its level.
- Evidence updates must not cross candidate boundaries or overwrite SELF/MENTOR data.
- Invalid input must roll back all imported data and scores.
- SQL seed must be repeatable; constraints reject invalid score/weight and verified SELF.
- Recompute must preserve self scores and remove stale verified scores when no verified evidence remains.

## Task 1: calculation core

Files: `service/SkillScoreCalculator.java`, `entity/SkillLevel.java`, corresponding unit test.

Interfaces: `calculate(List<ScoreEvidence>, Instant asOf)` returns `CalculatedSkillScore(verifiedScore, level)`; records are public nested types. Evidence has `BigDecimal score`, `BigDecimal weight`, `Instant recordedAt`, `boolean verified`. Return score at six decimal places; compare numerator against denominator times each threshold before rounding.

- [x] Write and run failing tests for weighted/aged mean, exact-year cutoff, missing verified evidence, boundaries and invalid input.
- [x] Implement calculator; run unit tests successfully.

## Task 2: persistence and recompute

Files: four entities plus score ID and source enums, repositories, `dto/VerifiedSkillRecord.java`, `dto/SkillProfileView.java`, `service/SkillProfileService.java`, `config/SkillProfileConfiguration.java`, SQL scripts `db/hs01/schema.sql` and `catalog.sql`, JPA integration tests.

Interfaces: service `importVerifiedRecords(UUID, List<VerifiedSkillRecord>)`, `recomputeSkills(UUID)`, `getSkills(UUID)` return `List<SkillProfileView>`. Input record carries mapping source/code, stable reference, title, normalized score and original recordedAt. Read methods are internal, without authorization; expose only after privacy/auth integration.

- [x] Write integration tests using real H2/JPA and scripts, observe failure before implementation.
- [x] Implement four-table persistence, catalog/mapping seed and transactional import/recompute. Reuse calculator from Task 1.
- [x] Test catalog idempotence, source/skill deduplication, record updates, ownership isolation, rollback, SELF preservation and empty/missing mapped results.

## Task 3: reproducible samples and handoff

Files: test fixtures for three candidates, `docs/hs01-independent-backend.md`, profile-service README.

- [x] Exercise three sample candidates through the real persistence service, with candidate1 having at least five skills and sourced/dated evidence.
- [x] Verify adding a result changes its score, re-import does not duplicate evidence, and SELF-only has null verified score and level.
- [x] Document test/build commands, hand calculation, internal inputs and all deferred integration points.
- [x] Run module and reactor verification; obtain fresh-context code review, fix material findings and check Git diff.

Implementation progress and verification evidence are recorded in `docs/hs01-independent-backend.md`. Do not commit or push until requested; leave changes reviewable on the feature branch.
