# Contributing to TariffSheriff

Thanks for your interest in contributing. This file describes the recommended workflow, standards, and helpful commands to get changes merged quickly.

## Quick start
- Fork the repo and create a branch from `main`:
  - feature branches: `feat/<short-description>`
  - fix branches: `fix/<short-description>`
  - chore branches: `chore/<short-description>`

- Keep commits small and focused. Use conventional commit messages (e.g. `feat: add tariff calculator`, `fix: resolve HS code parsing bug`).

## Repo layout (important)
- Frontend: `apps/frontend` (React + Vite)
- Backend: `apps/backend` (Spring Boot)
- Shared libs: `packages/*`

## Local development
1. Install dependencies (repo root)
   ```bash
   npm ci
   ```
2. Start backend
   ```bash
   cd apps/backend
   ./mvnw spring-boot:run
   ```
3. Start frontend (from repo root)
   ```bash
   npm run dev --workspace=frontend
   ```

## Pre-commit checks

Run formatting and linting before opening a PR to keep diffs clean:

```bash
# run lint (if configured)
npm run lint || echo "no lint script configured"
# run formatter
npm run format || echo "no format script configured"
```

## Tests & checks
- Backend tests
  ```bash
  cd apps/backend
  ./mvnw test
  ```
- Frontend tests
  ```bash
  npm run test --workspace=frontend
  ```
- Type checks and build
  ```bash
  npm run build
  ```
Ensure tests and type checks pass locally before opening a PR.

## PR checklist
- [ ] Branch created from `main`
- [ ] Title and description explain the "why" and "what"
- [ ] All tests pass locally
- [ ] Lint/type checks run (if applicable)
- [ ] Relevant documentation updated (README, docs/)
- [ ] Changes are small and atomic; large changes staged into multiple PRs

## Code style & conventions
- Frontend: TypeScript + React patterns used across the codebase; prefer hooks and composable components.
- Formatting: follow existing project style (Prettier/ESLint if configured).
- Commit messages: follow Conventional Commits.

## CI / Deployment
- PRs run CI via GitHub Actions (`.github/workflows/ci.yml`). Ensure your branch passes the pipeline.
- Frontend is deployed to Vercel from `apps/frontend` — include any required `vercel.json` changes in the frontend folder only.

## Reporting issues
- Search existing issues and add detail (steps to reproduce, expected vs actual, logs).
- Reference relevant commits/PRs when applicable.

## Reviewing & merging
- Two approving reviews preferred for non-trivial changes.
- Rebase/squash commits before merge if requested by reviewers.
- Use GitHub’s squash-and-merge option where appropriate.

## Need help?
Open an issue or tag maintainers in a draft PR. For design/arch discussions, start a GitHub Discussion or add a design doc under `docs/`.

Thank you for helping improve TariffSheriff!
