# Backend Compose Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Run the gateway, eight backend services and local LMS mock with `docker compose up -d --build` from the repository root.

**Architecture:** A root Compose file extends the four existing infrastructure services. Backend containers use Compose DNS, shared JWT/internal keys, health checks and persistent upload volumes. Existing manual infrastructure configuration remains usable.

**Tech Stack:** Docker Compose, Maven, Java 21, Node.js 24.

**Spec:** User accepted the proposed Gateway Dockerfile and full backend Compose configuration in this conversation.

## Global Constraints

- Preserve existing user changes and existing infrastructure volume names.
- This stack is for local development; use the dev Spring profile by default.
- Frontend remains on the host at localhost:3000.
- Configure fixed internal service ports independently of published host ports.

## Review Focus

- Root command must resolve all build contexts and volumes without extra flags.
- A missing .env must still resolve usable local defaults.
- Identity must finish migrations/seeding before Company/Profile start.
- Every runtime health check must use a tool available in its image.
- Non-root application users must be able to write persistent uploads.

## Tasks

- [x] Add root Compose, Gateway/mock Dockerfiles, and writable Company storage.
- [x] Validate the resolved Compose model, build and start the complete stack, inspect health and representative API calls.
- [x] Update README/backend Docker documentation with commands and verified limitations.

## Execution notes

- Work directly in the supplied checkout to preserve the existing uncommitted mock dependency files required by the image; do not create commits or change branches.
- Configuration-only change: use Compose validation and runtime integration checks instead of tests that duplicate YAML contents.
- Root project name is `infra`, matching the default project name of `infra/compose.yaml`, to reuse existing containers and database volumes.
- Verification: Compose config passes with the existing environment, .env.example and an empty env file. First full image build/start passed; all nine backend applications, mock and PostgreSQL reported healthy. All eight OpenAPI routes and SSO passed through Gateway; application users can write upload volumes.
- Review correction: forward dedicated recruitment key to mock as well as recruitment.
- Added readiness probes to ensure health dependencies wait until ApplicationRunner seeders finish.
- Existing-data finding: current HR login ID differs from fixed demo seed, causing company NOT_FOUND. An isolated fresh PostgreSQL database passed HR login + company profile; no existing records were changed.
- Final verification: `docker compose up -d --build --wait` exited 0 with all 14 services running (all configured health checks healthy). Default/example Compose config validation passed after the final changes. The independent review found no remaining material issues. Disposable smoke containers/volumes were removed; the main stack remains running.
