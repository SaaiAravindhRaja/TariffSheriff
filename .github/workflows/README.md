# CI Pipeline Documentation

## Overview

The TariffSheriff CI pipeline is a comprehensive GitHub Actions workflow that automatically validates, builds, tests, and scans code on every pull request and push to the main branch. The pipeline is designed for efficiency, security, and developer experience.

**Key Features:**
- Parallel job execution for fast feedback
- Intelligent path filtering to skip unnecessary jobs
- Dependency caching for faster builds
- Security vulnerability scanning
- Clear error messages and troubleshooting guidance

## Workflow Triggers

The CI pipeline runs on:
- **Pull Requests**: When opened, synchronized (new commits), or reopened
- **Push to Main**: When commits are pushed directly to the main branch

**Concurrency Control**: When a new commit is pushed to a PR, the previous CI run is automatically cancelled to save resources and provide faster feedback.

## Jobs Overview

### Validation Jobs

#### 1. Validate Configuration
**Purpose**: Ensures environment configuration files are present and valid

**When it runs**: On all workflow runs

**What it checks**:
- `apps/backend/.env.example` exists and contains required Spring/database variables
- `apps/frontend/.env.example` exists and contains VITE_ prefixed variables

**Timeout**: 5 minutes

---

### Backend Jobs

#### 2. Backend Build and Test
**Purpose**: Compiles backend code and runs all tests with database migrations

**When it runs**: When backend files change (`apps/backend/**`) or workflow changes

**What it does**:
- Sets up Java 17 (Temurin distribution)
- Restores Maven dependency cache
- Enables Docker for Testcontainers
- Runs `mvn -B verify` (compile + test)
- Automatically provisions PostgreSQL container via Testcontainers
- Applies Flyway migrations
- Uploads test results on failure

**Environment**:
- Runner: `ubuntu-latest`
- Java: 17 (Temurin)
- Maven cache enabled

**Timeout**: 15 minutes

---

#### 3. Backend Security Scan
**Purpose**: Scans backend dependencies for known security vulnerabilities

**When it runs**: When `apps/backend/pom.xml` changes

**What it does**:
- Runs OWASP Dependency Check
- Fails on CVSS score ≥ 7.0 (high/critical vulnerabilities)
- Uses suppression file for false positives
- Uploads security report as artifact (30-day retention)

**Timeout**: 10 minutes

---

#### 4. Backend Package
**Purpose**: Creates production-ready JAR artifact

**When it runs**: Only on pushes to main branch (when backend files change)

**What it does**:
- Runs `mvn -B package -DskipTests`
- Creates JAR file in `target/` directory
- Uploads artifact with commit SHA naming
- Verifies artifact size is reasonable

**Timeout**: 15 minutes

---

### Frontend Jobs

#### 5. Frontend Lint and Type Check
**Purpose**: Validates code quality and TypeScript types

**When it runs**: When frontend files change (`apps/frontend/**`) or workflow changes

**What it does**:
- Sets up Node.js 20.19.0
- Restores npm dependency cache
- Runs `npm ci` to install dependencies
- Runs `npm run lint --workspace=frontend` (ESLint)
- Runs `npm run type-check --workspace=frontend` (TypeScript)

**Environment**:
- Runner: `ubuntu-latest`
- Node.js: 20.19.0
- npm cache enabled

**Timeout**: 15 minutes

---

#### 6. Frontend Test
**Purpose**: Runs all frontend unit and integration tests

**When it runs**: When frontend files change

**What it does**:
- Sets up Node.js with npm cache
- Runs `npm run test --workspace=frontend -- --run`
- Sets `NODE_ENV=test`
- Uploads test results on failure

**Timeout**: 15 minutes

---

#### 7. Frontend Build
**Purpose**: Creates production-optimized build artifacts

**When it runs**: When frontend files change

**What it does**:
- Runs `npm run build --workspace=frontend`
- Verifies `apps/frontend/dist` directory exists and contains files
- Uploads build artifacts with commit SHA naming (7-day retention)
- Reports build size

**Timeout**: 15 minutes

---

#### 8. Frontend Security Scan
**Purpose**: Scans frontend dependencies for vulnerabilities

**When it runs**: When `apps/frontend/package-lock.json` changes

**What it does**:
- Runs `npm audit --audit-level=high`
- Parses JSON output to count vulnerabilities
- Fails if high or critical vulnerabilities found
- Uploads audit report as artifact

**Timeout**: 10 minutes

---

#### 9. PR Summary
**Purpose**: Posts a comprehensive summary comment on pull requests

**When it runs**: On all pull requests, after all other jobs complete

**What it does**:
- Collects results from all CI jobs
- Categorizes jobs as passed, warnings, or failed
- Shows which parts of the codebase changed (backend/frontend)
- Posts or updates a summary comment on the PR
- Provides quick links to detailed results

**Comment includes**:
- Overall build status (✅ passed, ⚠️ warnings, ❌ failed)
- List of passed jobs
- List of non-blocking warnings
- List of failed jobs (if any)
- Skipped jobs (collapsed)
- Direct link to detailed CI results

**Benefits**:
- See all CI results at a glance without leaving the PR
- Understand what needs attention immediately
- Track CI status across multiple commits (comment updates automatically)

**Timeout**: 5 minutes

---

## Path Filtering

The pipeline uses intelligent path filtering to skip unnecessary jobs:

**Backend jobs skip when**:
- Only frontend files changed
- Only documentation changed
- Only workflow files changed (except the CI workflow itself)

**Frontend jobs skip when**:
- Only backend files changed
- Only documentation changed
- Only workflow files changed (except the CI workflow itself)

**Path patterns**:
- Backend: `apps/backend/**`
- Frontend: `apps/frontend/**`
- Workflow: `.github/workflows/ci.yml`

---

## Caching Strategy

### Maven Cache
- **Cache key**: `${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}`
- **Cached paths**: `~/.m2/repository`
- **Expected speedup**: 2-3 minutes per run

### NPM Cache
- **Cache key**: `${{ runner.os }}-node-${{ hashFiles('**/package-lock.json') }}`
- **Cached paths**: `~/.npm`, `node_modules`
- **Expected speedup**: 1-2 minutes per run

---

## Artifacts

### Build Artifacts
- **Backend JAR**: `backend-${{ github.sha }}.jar` (7-day retention)
- **Frontend Build**: `frontend-dist-${{ github.sha }}.zip` (7-day retention)

### Test Reports
- **Test Results**: Uploaded on failure for debugging
- **Security Reports**: 30-day retention for compliance

---

## Troubleshooting Guide

### Common Issues and Solutions

#### ❌ Backend Tests Failed

**Error**: Test failures in Maven verify

**Solution**:
1. Run tests locally to reproduce:
   ```bash
   cd apps/backend
   mvn clean verify
   ```

2. Check if Testcontainers is working:
   ```bash
   # Ensure Docker is running
   docker ps
   
   # Check Docker permissions
   docker run hello-world
   ```

3. Review test logs in the CI job output
4. Download test result artifacts from the failed job

**Common causes**:
- Database migration issues (check Flyway scripts)
- Missing environment variables
- Docker not available (local only)
- Test data conflicts

---

#### ❌ Frontend Build Failed

**Error**: Build errors or empty dist directory

**Solution**:
1. Run build locally to reproduce:
   ```bash
   cd apps/frontend
   npm ci
   npm run build
   ```

2. Check for TypeScript errors:
   ```bash
   npm run type-check
   ```

3. Check for linting errors:
   ```bash
   npm run lint
   ```

4. Verify environment variables in `.env.example`

**Common causes**:
- TypeScript type errors
- Missing dependencies
- Invalid environment variable references
- Import path issues

---

#### ❌ Security Scan Failed

**Error**: High or critical vulnerabilities detected

**Solution for Backend**:
1. Review the security report artifact
2. Update vulnerable dependencies:
   ```bash
   cd apps/backend
   mvn versions:display-dependency-updates
   mvn versions:use-latest-versions
   ```

3. If it's a false positive, add to suppression file:
   ```bash
   # Edit apps/backend/dependency-check-suppressions.xml
   ```

**Solution for Frontend**:
1. Review npm audit output:
   ```bash
   cd apps/frontend
   npm audit
   ```

2. Fix vulnerabilities:
   ```bash
   npm audit fix
   # or for breaking changes
   npm audit fix --force
   ```

3. Update specific packages:
   ```bash
   npm update package-name
   ```

---

#### ❌ Configuration Validation Failed

**Error**: Missing or invalid .env.example files

**Solution**:
1. Ensure `.env.example` files exist:
   - `apps/backend/.env.example`
   - `apps/frontend/.env.example`

2. Backend .env.example should contain:
   ```
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tariffsheriff
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=password
   JWT_SECRET=your-secret-key
   OPENAI_API_KEY=your-api-key
   ```

3. Frontend .env.example should contain VITE_ prefixed variables:
   ```
   VITE_API_URL=http://localhost:8080
   VITE_APP_NAME=TariffSheriff
   ```

---

#### ⏱️ Job Timeout

**Error**: Job exceeded timeout limit

**Solution**:
1. Check if the job is stuck on a specific step
2. Review logs for hanging processes
3. For backend tests, ensure Testcontainers cleanup is working
4. Consider optimizing slow tests

**Timeout limits**:
- Validation jobs: 5 minutes
- Build/Test jobs: 15 minutes
- Security scans: 10 minutes

---

#### 🔄 Cache Issues

**Error**: Slow builds despite caching

**Solution**:
1. Check if cache is being restored (look for "Cache restored" in logs)
2. Clear cache by updating cache key (modify pom.xml or package-lock.json)
3. Verify cache size isn't exceeding GitHub limits (10GB per repo)

```bash
# Force cache refresh by updating lock files
cd apps/backend && mvn clean
cd apps/frontend && npm ci
```

---

## Testing Workflow Changes Locally

### Using Act (GitHub Actions Local Runner)

1. **Install Act**:
   ```bash
   # macOS
   brew install act
   
   # Linux
   curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash
   ```

2. **Run specific job**:
   ```bash
   # Run all jobs
   act pull_request
   
   # Run specific job
   act pull_request -j validate-branch-name
   act pull_request -j backend-build-test
   ```

3. **Test with secrets**:
   ```bash
   # Create .secrets file
   echo "GITHUB_TOKEN=your_token" > .secrets
   act pull_request --secret-file .secrets
   ```

4. **Dry run (check syntax)**:
   ```bash
   act pull_request --dryrun
   ```

### Manual Testing

1. **Create a test branch**:
   ```bash
   git checkout -b feat/test-ci-pipeline
   ```

2. **Make a small change**:
   ```bash
   echo "# Test" >> README.md
   git add README.md
   git commit -m "feat: test CI pipeline"
   ```

3. **Push and create PR**:
   ```bash
   git push origin feat/test-ci-pipeline
   # Create PR on GitHub
   ```

4. **Monitor the workflow**:
   - Go to Actions tab on GitHub
   - Watch jobs execute in real-time
   - Review logs for any issues

### Testing Path Filters

1. **Backend-only change**:
   ```bash
   # Should skip frontend jobs
   touch apps/backend/src/main/java/Test.java
   git add . && git commit -m "feat: test backend"
   ```

2. **Frontend-only change**:
   ```bash
   # Should skip backend jobs
   touch apps/frontend/src/Test.tsx
   git add . && git commit -m "feat: test frontend"
   ```

3. **Documentation-only change**:
   ```bash
   # Should skip all build/test jobs
   echo "# Update" >> docs/README.md
   git add . && git commit -m "docs: update readme"
   ```

---

## Performance Metrics

**Expected CI run times**:
- Full run (all jobs): 6-8 minutes
- Backend-only changes: 4-5 minutes
- Frontend-only changes: 3-4 minutes
- Documentation-only: <1 minute (validation only)

**Cache hit rates**:
- Maven cache: >80% hit rate
- NPM cache: >90% hit rate

---

## Workflow Permissions

The CI workflow has minimal required permissions:
- `contents: read` - Read repository code
- `pull-requests: write` - Post status comments
- `checks: write` - Create check runs

No secrets are required for CI execution. Testcontainers provisions ephemeral databases automatically.

---

## Migration from Old Workflows

The new CI pipeline replaces:
- `.github/workflows/ci.yml.disabled` (old unified workflow)
- `.github/workflows/backend-ci.yml.disabled` (old backend-specific workflow)

**Key improvements**:
- Faster execution with parallel jobs
- Intelligent path filtering
- Better error messages
- Security scanning
- Comprehensive caching

---

## Future Enhancements

Planned improvements:
- Code coverage reporting (Codecov/Coveralls)
- Performance testing (Lighthouse CI)
- Visual regression testing (Percy/Chromatic)
- Deployment preview environments
- CodeQL security analysis
- Slack notifications for main branch failures

---

## Support

For issues or questions about the CI pipeline:
1. Check this documentation first
2. Review the troubleshooting guide above
3. Check recent workflow runs for similar issues
4. Open an issue with the `ci` label
5. Contact the DevOps team

---

## Related Documentation

- [Contributing Guidelines](../../CONTRIBUTING.md)
- [Requirements Document](../../.kiro/specs/ci-pipeline/requirements.md)
- [Design Document](../../.kiro/specs/ci-pipeline/design.md)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
