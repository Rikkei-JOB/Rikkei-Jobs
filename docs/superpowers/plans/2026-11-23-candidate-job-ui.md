# Candidate Job UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Ship the approved candidate job list, detail and CV application UI with visual verification.

**Architecture:** Keep the authenticated AppShell and configured CandidateJob adapter. Split list presentation from detail, scope career tokens locally, and reuse ApplyButton/ApplyDialog submission and reconciliation logic. Use external API fixtures only in Playwright.

**Tech Stack:** Existing Next.js, React, TypeScript, Tailwind, Base UI, Lucide and Playwright; no new dependencies.

**Spec:** ../specs/2026-11-23-candidate-job-ui-design.md (approved by user 2026-11-23).

## Global Constraints

- Preserve existing authentication, CV snapshot, duplicate submission and uncertain-result handling.
- Keep AppShell navigation and other roles unchanged. Career primary #B4232D; Inter font.
- Only expose supported candidate fields; no fabricated salary, location, job description or matching score.
- Respect configured job/CV paths. Missing producer config keeps the existing integration placeholder.
- Validate 1440×1000, 834×1112, 390×844 and horizontal overflow at 360px.
- Preserve all existing user changes. Work directly in the authorized workspace; no worktree, commit, merge or push is needed for delivery.

## Review Focus

- Diacritic-insensitive search and URL history restore the visible form and results.
- Closed jobs and long titles never produce an enabled application action or horizontal overflow.
- CV errors and uncertain submissions preserve entered text and prevent accidental duplicate requests.
- Mobile fixed actions stay behind modal and do not cover page content or keyboard navigation.
- Modal focus returns to trigger and portal inherits career styling without changing other roles.

### Task 1: Searchable list and structured detail

**Files:** Modify `web/src/app/components/applications/candidate-jobs.tsx`; create `candidate-job-list.tsx`, `candidate-job-detail.tsx`, `career.css` in the same directory; add `web/e2e/candidate-career.spec.ts` and fixtures.

**Interfaces:** Consume `CandidateJob` and `candidateJobService` unchanged. `CandidateJobList({jobs}: {jobs: CandidateJob[]})`; `CandidateJobDetail({job}: {job: CandidateJob})`. `CandidateJobs({jobId?})` remains the route entry.

- [x] Write Playwright tests for search `Đặng` via `dang`, receiving state, refresh/back/reset preserving unrelated URL params, detail navigation, closed CTA and mobile fit.
- [x] Run the new tests against existing UI; expect missing search controls/structured detail assertions to fail.
- [x] Implement scoped tokens, list search form, state feedback, result cards and responsive detail with preparation content.
- [x] Run search/detail tests; expect passing behavior and no overflow.

### Task 2: Application modal and responsive actions

**Files:** Modify `apply-button.tsx`, `apply-dialog.tsx`, `apply-entry.tsx`; extend `career.css` and career Playwright tests.

**Interfaces:** ApplyButton retains existing required props; optional `jobTitle`, `companyName`, `className`. ApplyDialog retains existing callbacks; optional title/company context. One trigger per active responsive presentation, share its own popup state and hide sticky action with `:has([data-application-open])`.

- [x] Write tests for list→detail→application success, keyboard dismiss/focus return, disabled closed job, CV retry, preserved draft on API error, uncertain-result reconciliation and short viewport scrolling.
- [x] Run new behavior tests before implementation, record missing behavior separately from already-covered existing behavior.
- [x] Style popup with same career theme, 44px controls, contextual job summary, scrollable body and reachable footer. Preserve submit/reconcile logic.
- [x] Run UT-01 and new career tests; expect payload/navigation/error recovery unchanged.

### Task 3: Visual validation and delivery

**Files:** Add `web/e2e/candidate-career.visual.spec.ts`, capture output under `docs/design/candidate-local/`; update spec status and validation notes.

**Interfaces:** Shared API fixtures contain only CandidateJob fields and existing CV/application structures. Tests may use `NEXT_PUBLIC_JOB_API_PATH=/candidate-career-fixture/jobs` and `NEXT_PUBLIC_JOB_PAGE_PATH=/candidate/jobs` in the test process, never production fallback.

- [x] Capture list/detail/modal at all approved viewports and selected empty/error/closed states, with visible “API fixture” annotation added by test only.
- [x] Inspect screenshots and correct layout issues; check 360px overflow, keyboard and color contrast.
- [x] Run type-check, lint, production build and frontend test suite. Diagnose and report unrelated baseline failures explicitly.
- [x] Obtain independent final code review, address material findings and rerun relevant checks.
- [x] Deliver local screenshot links and note fixture-only integration scope; leave changes uncommitted for user review.

## Execution record

- Approved design received; proceeding inline without another approval gate, as authorized by the user.
- Pre-flight: Task 1 and 2 share ApplyButton; keep required props stable and add only optional presentation/context props. Task 3 fixtures mirror the unchanged CandidateJob contract.

- Task 1: RED missing search/detail; GREEN search/history/reset and detail behaviors verified.
- Task 2: CV success/retry, retained draft, uncertain-result reconciliation, closed CTA, 360px popup and focus return verified.
- Task 3: 42 local PNGs inspected across desktop/tablet/mobile; full E2E 59 passed/1 live test skipped. Type-check/build passed; lint has 0 errors and one pre-existing warning.
- Final review: independent reviewer found no introduced Critical/Important/Minor issues. Kept existing uncertain-result state lifetime within the modal; backend duplicate guard remains unchanged.
- Test configuration: process defaults isolate Playwright fixtures from local mock URLs; no production fallback. Restored screenshots regenerated by unrelated legacy tests. No commit/push performed.
