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

## CI Pipeline

TariffSheriff uses a comprehensive CI pipeline that runs automatically on every pull request and push to `main`. The pipeline ensures code quality, runs tests, performs security scans, and validates project standards.

### What Runs on Pull Requests

When you open or update a PR, the following checks run automatically:

#### Validation Checks
- **Branch Name Validation**: Ensures your branch follows the naming convention (`feat/*`, `fix/*`, or `chore/*`)
- **Commit Message Validation**: Verifies your PR title follows Conventional Commits format
- **Configuration Validation**: Checks that `.env.example` files exist and contain required variables

#### Backend Checks (runs when `apps/backend/**` changes)
- **Build & Test**: Compiles Java code, runs all JUnit tests with Testcontainers (PostgreSQL + Flyway migrations)
- **Security Scan**: Runs OWASP Dependency Check to detect vulnerable dependencies
- **Package**: Creates production JAR artifact (main branch only)

#### Frontend Checks (runs when `apps/frontend/**` changes)
- **Lint & Type Check**: Runs ESLint and TypeScript compiler to catch code quality issues
- **Test**: Executes all Vitest tests
- **Build**: Creates optimized production build and verifies output
- **Security Scan**: Runs npm audit to detect vulnerable packages

### Branch Naming Requirements

Your branch name must follow one of these patterns:
- `feat/short-description` - for new features
- `fix/short-description` - for bug fixes
- `chore/short-description` - for maintenance tasks

**Examples**:
- ✅ `feat/add-tariff-calculator`
- ✅ `fix/resolve-hs-code-parsing`
- ✅ `chore/update-dependencies`
- ❌ `feature/new-calculator` (wrong prefix)
- ❌ `my-branch` (no prefix)

**Note**: Branch name validation is skipped for forks and Dependabot PRs.

### Commit Message Requirements

Your PR title must follow the Conventional Commits format:

```
<type>(<optional scope>): <description>

Examples:
✅ feat: add tariff calculator
✅ fix: resolve HS code parsing bug
✅ chore(deps): update Spring Boot to 3.2.0
✅ docs: update API documentation
```

**Valid types**: `feat`, `fix`, `chore`, `docs`, `style`, `refactor`, `test`, `build`

### CI Performance Features

The pipeline is optimized for speed:
- **Parallel Execution**: Backend and frontend jobs run simultaneously
- **Smart Caching**: Maven and npm dependencies are cached (typical speedup: 2-3 minutes)
- **Path Filtering**: Only runs relevant jobs based on changed files
- **Concurrency Control**: Cancels outdated runs when you push new commits

Expected CI runtime: 6-10 minutes for typical changes

### Troubleshooting Common CI Failures

#### Branch Name Validation Failed
**Error**: "Branch name does not follow required pattern"

**Solution**: Rename your branch to follow the pattern `feat/*`, `fix/*`, or `chore/*`
```bash
git branch -m feat/your-feature-name
git push origin -u feat/your-feature-name
```

#### Commit Message Validation Failed
**Error**: "PR title does not follow Conventional Commits format"

**Solution**: Edit your PR title to include a valid prefix (`feat:`, `fix:`, `chore:`, etc.)

#### Backend Tests Failed
**Error**: "Tests failed" or "Testcontainers could not start"

**Solutions**:
- Ensure your tests pass locally: `cd apps/backend && ./mvnw test`
- Check that Docker is available (Testcontainers requires Docker)
- Review test logs in the CI job output for specific failures
- Verify database migrations are valid (Flyway runs automatically)

#### Frontend Type Check Failed
**Error**: "Type errors found"

**Solutions**:
- Run type check locally: `npm run type-check --workspace=frontend`
- Fix TypeScript errors in your code
- Ensure all imports are correctly typed

#### Frontend Lint Failed
**Error**: "ESLint errors found"

**Solutions**:
- Run lint locally: `npm run lint --workspace=frontend`
- Fix linting errors or add appropriate ESLint disable comments if justified
- Follow existing code style patterns

#### Security Scan Failed
**Error**: "High/critical vulnerabilities detected"

**Solutions**:
- Review the security report artifact in the CI job
- Update vulnerable dependencies to patched versions
- For backend: Update `pom.xml` and run `./mvnw dependency:tree` to verify
- For frontend: Run `npm audit fix` or update specific packages in `package.json`
- If it's a false positive (backend only), add suppression to `apps/backend/dependency-check-suppressions.xml`

#### Build Failed
**Error**: "Compilation failed" or "Build failed"

**Solutions**:
- Ensure the build works locally:
  - Backend: `cd apps/backend && ./mvnw clean package`
  - Frontend: `npm run build --workspace=frontend`
- Check for syntax errors or missing dependencies
- Review the full error message in CI logs for specific file/line numbers

#### Configuration Validation Failed
**Error**: "Missing or invalid .env.example file"

**Solutions**:
- Ensure `apps/backend/.env.example` exists and contains all required Spring/database variables
- Ensure `apps/frontend/.env.example` exists and all variables are prefixed with `VITE_`
- Copy from `.env` if needed, but remove sensitive values

#### Job Timeout
**Error**: "Job exceeded timeout"

**Solutions**:
- This is rare but can happen with slow runners
- Re-run the job (GitHub Actions → Re-run failed jobs)
- If it persists, the job may have an infinite loop or hanging process

#### Cache Issues
**Error**: Build is slow or dependencies aren't cached

**Solutions**:
- First run after changing `pom.xml` or `package-lock.json` will be slower (cache miss)
- Subsequent runs should be faster
- If caching seems broken, maintainers can clear the cache from GitHub Actions settings

### Getting Help

If CI checks fail and you can't resolve the issue:
1. Check the specific job logs in the GitHub Actions tab
2. Try reproducing the failure locally using the same commands
3. Ask for help in your PR by tagging maintainers
4. Include the error message and what you've tried

### Deployment

- **Frontend**: Deployed to Vercel from `apps/frontend` — include any required `vercel.json` changes in the frontend folder only
- **Backend**: Deployment configuration coming soon (AWS-based)

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
