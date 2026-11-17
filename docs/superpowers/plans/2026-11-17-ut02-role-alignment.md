# UT-02 Role Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Match the product role matrix for UT-01/UT-02 without exposing unrelated applications to mentors.

**Architecture:** Add narrow identity/profile lookups at the service boundary; enforce ownership in recruitment before returning data or mutating rounds; expose role-specific pages.

**Tech Stack:** Spring Boot 3, PostgreSQL, JUnit, Next.js 16, Playwright.

**Spec:** `docs/superpowers/specs/2026-11-17-ut02-role-alignment-design.md`

## Global Constraints

- Keep the unrelated local `.gitignore` edit untouched.
- Use `X-Internal-Key` for internal service calls; do not route `/internal/**` through the gateway.
- HR may add rounds only in `INTERVIEW`; STAFF and ADMIN stay read-only.

## Review Focus

- A mentor cannot see another mentor's round or the applicant's contact information.
- An email change or failed identity lookup cannot grant a mentor access.
- HR from another company still receives 404 before any contact lookup.
- ADMIN can read but cannot mutate; STAFF remains read-only.
- A missing phone stays null; an unavailable profile dependency fails clearly.

### Task 1: Backend role boundaries

**Files:** recruitment controllers/services/clients/tests, profile contact controller/tests.

- [x] Add failing API tests for ADMIN read-only and mentor assigned-only behavior.
- [x] Add profile contact and recruitment phone aggregation tests.
- [x] Implement narrow profile contact endpoint and recruitment client.
- [x] Implement mentor identity lookup, list, and guarded result update (the list includes all round data needed by the editor).
- [x] Run affected backend tests and full Maven test suite.

### Task 2: Frontend and contract

**Files:** recruitment UI/services/types/pages, Playwright specs, OpenAPI and API docs.

- [x] Add failing Playwright checks for ADMIN read-only and mentor assigned-round editing.
- [x] Implement the role-specific pages and phone display.
- [x] Update OpenAPI and role documentation.
- [x] Run type-check, lint, build, and E2E suite.

### Task 3: Final verification

- [x] Review authorization and dependency failure cases against the spec.
- [x] Check diff and preserve unrelated changes.
