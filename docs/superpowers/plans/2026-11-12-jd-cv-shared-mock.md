# Shared JD/CV Mock Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide shared HTTP mocks for UT-01's JD context and candidate-owned CV/PDF contracts.

**Architecture:** Extend the existing Express mock with a scoped JD/CV module, JSON fixtures and a private PDF fixture. Preserve legacy service-key routes and validate the new namespaces using their own credentials.

**Tech Stack:** Existing Express 4, Node.js 18+ built-ins, node:test; PyMuPDF for the static PDF fixture.

**Spec:** `docs/superpowers/specs/2026-11-12-jd-cv-shared-mock-design.md`

## Global Constraints

- Keep the 11 existing external endpoints and `/mock/*` behavior working.
- Match the UT-01 paths, response fields and credential types in the spec.
- No new npm dependency, no app runtime changes, no automatic push.
- Work in the current clean feature checkout already used in this conversation; no new worktree or branch is needed for these scoped edits.
- Preserve the distinction between a runnable consumer contract and producer approval.

## Review Focus

- JWT signature, expiry, future nbf, unsupported algorithm and invalid subject must fail closed.
- A candidate cannot list or download another candidate's CV; HR and password-change tokens are rejected.
- Internal keys must not authorize CV routes, and CV JWTs must not authorize JD context routes.
- PDF responses must contain a valid private PDF, not JSON or an unprotected static path.
- Legacy auth, mutable enrollment/member behavior, tools and CORS must remain isolated.

## Task 1: Shared HTTP mock

**Files:** `mock/test/jd-cv.test.js`, `mock/jd-cv.js`, `mock/candidate-token.js`, `mock/server.js`, `mock/data.json`, `mock/fixtures/cvs/sample.pdf`, `mock/package.json`.

**Interfaces:** `mountJdCv(app, getData)` mounts the contract routes; `getData()` returns the current fixture object after `/mock/reload`. `candidate-token.js [userId]` prints one expiring signed CANDIDATE JWT using the process JWT_SECRET. Tests spawn an isolated real mock process using an ephemeral local port.

- [x] Add HTTP tests first; run legacy regression to establish baseline and new tests to see missing-route failures.
- [x] Add jobs/CVs, JWT/role/ownership checks, CORS and PDF response handling, token CLI and npm test script.
- [x] Create/render/inspect the one-page sample PDF.
- [x] Run `npm.cmd test` in `mock/`; expect all HTTP contract and legacy tests to pass.

## Task 2: Team handoff and CI

**Files:** `mock/README.md`, `mock/requests.http`, `docs/api/UT-01-applications.md`, `.github/workflows/ci.yml`.

- [x] Document exact fixture IDs, auth/error cases, token generation, identity UUID binding, and opt-in environment overrides for backend and frontend.
- [x] Add copyable HTTP requests for success, invalid auth, ownership, missing IDs and JD conditions.
- [x] Add a CI job running `npm ci` and `npm test` under `mock/`.
- [x] Run full mock tests and `git diff --check`; obtain a scoped independent review, address material findings, report what was actually verified.
