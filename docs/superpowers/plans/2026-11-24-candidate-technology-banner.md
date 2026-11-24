# Candidate technology banner

User selected the proposed technology illustration on 2026-11-24. Replace the jobs title block with a compact wine-red gradient banner, white title “Bước tiếp theo cho sự nghiệp của bạn”, candidate profile link, and decorative vector CV/skills/connections. Keep the search immediately below and existing application shortcut accessible. No API changes, fake metrics, dependencies, raster assets or motion required.

- [x] Add an accessible banner component with decorative inline SVG and scoped responsive CSS (about 220px desktop, compact mobile).
- [x] Reuse existing profile and applications routes; retain search/filter behavior. Move the applications shortcut into the results header.
- [x] Use Playwright to capture desktop/tablet/mobile and 360px overflow; inspect actual images. Run relevant flow tests, type-check, lint and isolated production build.

Small presentation change: reuse existing flow checks and visual validation rather than adding tests for CSS constants. Preserve all uncommitted topbar/backend notification work. No commit or deployment.

Validation: 27 related browser checks passed; mobile focus regression 5 repeated passes. Build and type-check passed; lint 0 errors/1 pre-existing warning. Six PNGs inspected, measured 220px desktop/tablet and 180px mobile. Independent review found stale CV selection on modal reopen after lifecycle correction; regression observed RED and fixed by resetting loading/selection on close. See docs/design/candidate-banner/validation.md. No commit/push.
