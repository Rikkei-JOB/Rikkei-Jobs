# HR workspace implementation plan

> **For agentic workers:** Use superpowers:executing-plans to implement task by task.

**Goal:** Apply the approved HR top bar and recruitment workspace design.
**Architecture:** Dedicated HrShell selected in AppShell, existing shared business components retained with HR-specific presentation. URL owns filters and detail tab. No dependency additions or API changes.
**Tech Stack:** Next.js, React, TypeScript, Tailwind, Base UI, Playwright.
**Spec:** `docs/superpowers/specs/2026-11-25-hr-workspace-design.md` (approved 25/11/2026).

## Global constraints

- Top bar 64px; desktop breakpoint 1200px; content max-width 1200px; control targets 44px.
- Follow DESIGN.md tokens; preserve other role shells and business permissions.
- No HR notifications until separately specified backend supports HR recipients.
- Use existing API fields; job options load all pages and preserve unknown URL jobId.

## Review focus

- Long names and 360px viewport must not overflow.
- URL job filter absent from first JD page remains selected and readable.
- Filter request failures must not present stale rows as current results.
- Detail tab changes preserve entered forms; 409 reload keeps correct permissions.
- Return link accepts only internal HR recruitment list destinations; other roles retain their UI.

## Task 1: HR shell

Files: `components/layout/hr-shell.tsx`, `hr-shell.css`, `app-shell.tsx`, `config/menu.ts`, `globals.css`, `DESIGN.md`; test `e2e/hr-workspace.spec.ts`.
Interface: `HrShell({user,children,onLogout})`; selects only HR. Shared semantic tokens declared in globals, HR maps existing tokens without changing other roles.
- [x] Add browser checks for desktop navigation, account menu, mobile dismissal, no candidate notification calls, 360px long names.
- [x] Run test to establish failure, implement shell, rerun.

## Task 2: HR applications list

Files: `recruitment/application-table.tsx`, `recruitment/hr-job-filter.tsx`; tests in same suite.
Interface: `HrJobFilter({value,onChange,disabled})` loads `jobService.listMine({page,size:100})`, then selected detail if necessary.
- [x] Check named position selection from later page; URL/clear/filter restoration and loading/error/retry.
- [x] Implement labelled table/cards and returnTo links, preserve Staff/Admin list behavior.
- [x] Run suite for job filtering and return navigation.

## Task 3: Application detail

Files: `recruitment/application-workflow-detail.tsx`, `recruitment/application-navigation.ts`; tests `hr-workspace`, UT02/UT06/UT07.
Interface: safe list return URL helper; HR tabs use `tab` query param and mounted panels. Existing services, transitions, interviews, offers unchanged.
- [x] Add checks for tab URL, preserved input and safe return link.
- [x] Implement candidate-first heading, two columns, summary/action aside, four tabs.
- [x] Update existing HR tests to navigate relevant tab before interacting. Run workflow suites, including 409 and offer rules.

## Task 4: Supporting screens and validation

Files: HR jobs/company pages, job-table, hr-interview-calendar, DESIGN.md, screenshot suite.
- [x] Standardise page headings, cards, filters, table readability, company responsive breakpoint; retain form fields and actions.
- [x] Add mobile calendar agenda from existing weekly results, with today indication and week/list switch.
- [x] Capture Playwright fixture screenshots 1440×1000, 834×1112, 390×844; check 360px overflow and inspect images.
- [x] Run type-check, lint, build and full E2E; independent whole-change review, fix material findings.

## Execution notes

Native execution in current checkout on dedicated `codex/hr-workspace-topbar` branch; initial checkout had only the new design document untracked. No additional worktree or dependencies needed. User approved implementation; proceed without a second plan approval. No commit/push/merge requested.


## Verification results

- `npm run type-check`: passed.
- `npm run lint`: passed, zero errors; existing commitlint anonymous default export warning remains.
- `npm run build`: passed.
- `npx playwright test --workers=2`: 83 passed, 4 skipped (one existing live-stack test and three explicitly gated HR screenshot tests).
- `HR_SCREENSHOTS=1 npx playwright test e2e/hr-workspace.spec.ts --workers=2`: 11 passed, including 15 fixture screenshots across three viewports.
- Independent review found cached list status after mutation; reproduced with failing browser test, fixed with fresh mount fetch and loading state, regression now passes.
- Supporting form/business components reused; no backend changes or dependencies. Time of match computation omitted because current response has no timestamp. HR notification backend remains explicitly deferred by approved design.
- Source remains in local branch `codex/hr-workspace-topbar`; no push or merge performed.
