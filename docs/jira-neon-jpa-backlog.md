## EPIC: Externalize the database to Neon Postgres

Description  
Move the application database to a managed Neon Postgres instance and wire environments so the backend connects securely. Migrations must run automatically and the app should work the same across dev/prod.

Acceptance Criteria
- Backend connects to Neon using environment-based JDBC URL, user, and password; SSL enabled.
- Flyway applies `apps/backend/src/main/resources/db/migration/V1__schema.sql` to an empty Neon database successfully.
- Distinct Neon branches or databases exist for dev and prod.
- Local developer startup works by pointing to Neon dev via env vars.
- Connection/secrets documented in `apps/backend/README.md`.

### Child work items (Stories)
- Provision Neon Postgres environments
- Configure secure connectivity and secrets management
- Apply initial schema with Flyway on Neon
- Migrate mock data into Neon databases
- Automate DB migrations in CI/CD
- Backups and retention policy for Neon
- Monitoring and basic alerts for Neon

---

### Story: Provision Neon Postgres environments

Description  
Create Neon Postgres projects for dev/prod so each environment has an isolated database.

Acceptance Criteria
- Neon project created with two environments (or branches/databases) and descriptive names.
- Connection strings captured for each environment.
- Access granted to the team members who need it.
- Connection info stored in secrets manager; no credentials in the repo.

Sub-tasks
- Create Neon project and two branches (dev, prod)  
  Done when branches exist and are visible in Neon console.
- Create database users/roles with least-privilege  
  Done when app user cannot create/drop schemas and has CRUD on app schema.
- Capture connection strings and parameters (host, port, db, user, SSL)  
  Done when values are written to secrets manager.
- Document environment topology and ownership  
  Done when `apps/backend/README.md` has a short section with links and contacts.

---

### Story: Configure secure connectivity and secrets management

Description  
Backend reads DB credentials from environment variables and connects with SSL to Neon.

Acceptance Criteria
- `application.properties` (or profile overrides) read `DB_URL`, `DB_USER`, `DB_PASSWORD`; SSL required.
- Local `.env.example` (or README) documents required variables.
- Secrets stored in CI/CD and deployment environments; verified not in git.
- A simple health check confirms successful app startup against Neon dev.

Sub-tasks
- Add Spring profile configuration for `dev`, `staging`, `prod`  
  Done when profiles map to different JDBC URLs.
- Wire env vars in container/orchestrator manifests (`docker-compose`/K8s)  
  Done when local run reads from env and connects.
- Add SSL parameters to JDBC URL and verify  
  Done when JDBC handshake succeeds with SSL required.
- Store credentials in CI/CD secrets and deployment secrets  
  Done when pipeline masks secrets and no plaintext appears in logs.

---

### Story: Apply initial schema with Flyway on Neon

Description  
Ensure the schema is managed and reproducible via Flyway against Neon.

Acceptance Criteria
- Starting the backend applies `V1__schema.sql` to Neon dev with no errors.
- Migrations are idempotent and re-runnable.
- Tables `country`, `agreement`, `agreement_party`, `hs_product`, `tariff_rate`, `vat`, `roo_rule` exist.

Sub-tasks
- Point Flyway to Neon dev and run `migrate`  
  Done when `flyway_schema_history` shows V1 success.
- Add a smoke SQL to validate key tables/indexes  
  Done when a script returns expected table counts.
- Record migration command and troubleshooting steps in README  
  Done when a “Migrations” section exists.

---

### Story: Migrate mock data into Neon databases

Description  
Move existing mock JSON/CSV files into Neon dev/prod aligned to the current schema.

Acceptance Criteria
- Mock data loaded into dev; row counts and key constraints validated.
- Repeatable load scripts checked in; docs updated.
- Prod loads gated and approved.

Sub-tasks
- Inventory mock data sources and owners  
  Done when a list of files, locations, and contacts is documented in `docs/` or `apps/backend/README.md`.
- Define field mappings to tables (`country`, `hs_product`, `tariff_rate`, `vat`, `roo_rule`)  
  Done when a mapping document exists with sample records and target columns.
- Write idempotent ETL/load scripts (psql/COPY or small importer)  
  Done when running twice does not create duplicates and scripts live under `scripts/` or `apps/backend`.
- Load into dev and validate counts/FKs/uniques  
  Done when counts match expectations and all FK/unique constraints pass.

---

### Story: Automate DB migrations in CI/CD

Description  
Migrations run automatically on deploy so environments stay in sync.

Acceptance Criteria
- CI/CD step runs Flyway migrate against prod using env-specific secrets.
- Pipeline fails if a migration fails; success visible in logs.
- Roll-forward documented; roll-back procedure documented (point-in-time restore reference).

Sub-tasks
- Add CI job to run Flyway with prod credentials behind approval  
  Done when successful logs appear in an approved deployment.
- Add manual approval or gated step for prod migration  
  Done when prod migration requires approval.
- Add notification on failure to team channel/email  
  Done when failures post an alert with run URL.
- Document rollback via Neon PITR/snapshot restore  
  Done when docs include timed steps and owners.

---

### Story: Backups and retention policy for Neon

Description  
Configure backups and retention to support recovery.

Acceptance Criteria
- Point-in-time recovery or snapshots enabled with defined retention window.
- Restore procedure tested to a temporary database.
- Access to backups restricted to admins.

Sub-tasks
- Configure retention and PITR settings in Neon  
  Done when settings match policy (e.g., 7–30 days).
- Test restore to a new branch and validate schema/data  
  Done when sanity queries pass on the restored DB.
- Restrict backup/restore permissions  
  Done when only admins can perform restores.
- Document RPO/RTO and runbook  
  Done when README/runbook notes targets and steps.

---

### Story: Monitoring and basic alerts for Neon

Description  
Provide visibility into DB health and alerts for critical issues.

Acceptance Criteria
- Metrics (connections, CPU, storage) viewable by the team.
- Alerts configured for connection saturation and storage thresholds.
- Connection error rate from the backend observed in logs/dashboards.

Sub-tasks
- Enable Neon metrics and integrate with chosen dashboard  
  Done when a basic dashboard link is shared.
- Configure alerts for connection pool usage and storage  
  Done when alerts fire on threshold breach in a test.
- Add backend logs/metrics panel for JDBC errors/timeouts  
  Done when recent errors are visible.
- Document dashboard/alert ownership  
  Done when contacts and escalation path are listed.

---

## EPIC: Implement CRUD layer with Spring Data JPA

Description  
Create JPA entities, repositories, and REST endpoints for the core schema so the application can create, read, update, and delete domain objects and support efficient tariff lookups.

Acceptance Criteria
- JPA entities map to all tables in `V1__schema.sql` with correct relationships and constraints.
- Repositories expose CRUD and key derived queries needed by the app.
- REST endpoints provide paginated list/get/create/update/delete operations.
- Integration tests prove mappings and queries using Testcontainers.
- API documented with examples.

### Child work items (Stories)
- Model JPA entities and relationships for the schema
- Create Spring Data repositories and key queries
- Implement service layer with validation and transactions
- As an API consumer, I want REST endpoints to manage and read tariff data
- Integration tests with Testcontainers and Flyway
- As an API consumer, I want API documentation and usage examples

---

### Story: Model JPA entities and relationships for the schema

Description  
Create entity classes for `country`, `agreement`, `agreement_party`, `hs_product`, `tariff_rate`, `vat`, and `roo_rule` so the ORM mirrors the DB.

Acceptance Criteria
- Entities created with proper `@Entity`, `@Table`, keys, and constraints.
- Relationships modeled (join tables and FKs as per schema).
- Enums modeled for `agreement.status`, `tariff_rate.basis`, `tariff_rate.rate_type`.
- Startup validation enabled with `spring.jpa.hibernate.ddl-auto=validate`.

Sub-tasks
- Create entity classes and enums with annotations  
  Done when compilation passes and tables validate.
- Map relationships and composite unique constraints  
  Done when `validate` passes and joins work in a smoke test.
- Configure converters for enums and numeric scales  
  Done when persisted values match expected domain.
- Add basic seed data loader for local dev (optional)  
  Done when local run has a few rows for testing.

---

### Story: Create Spring Data repositories and key queries

Description  
Provide repository interfaces for each entity and efficient domain queries.

Acceptance Criteria
- CRUD repositories for all entities with pagination on list operations.
- Domain queries include HS product lookup, MFN and preferential tariff lookups within validity window, and VAT retrieval.
- Queries verified with integration tests.

Sub-tasks
- Define `JpaRepository` interfaces per entity  
  Done when basic CRUD compiles and runs.
- Implement derived queries and needed `@Query` JPQL/SQL  
  Done when lookups return expected rows.
- Add pagination and sorting defaults  
  Done when list endpoints accept `page`, `size`, `sort`.
- Write repository-focused tests for key queries  
  Done when tests pass against Testcontainers.

---

### Story: Implement service layer with validation and transactions

Description  
Encapsulate domain logic and validation in services.

Acceptance Criteria
- Services wrap repositories with `@Transactional` boundaries.
- Validations for date ranges and basis-agreement consistency.
- Clear exceptions for 400/404 scenarios.

Sub-tasks
- Implement services for country, agreements, HS products, tariffs, VAT, ROO rules  
  Done when methods exist and are covered by unit tests.
- Add validation and exception mapping  
  Done when invalid inputs yield structured errors.
- Configure transaction boundaries and read-only ops  
  Done when write operations roll back on failure.

---

### Story (USER): As an API consumer, I want REST endpoints to manage and read tariff data

Description  
Expose REST controllers and DTOs so clients can create and query data required by the calculator.

Acceptance Criteria
- Endpoints for `countries`, `agreements`, `hs-products`, `tariff-rates`, `vat`, and `roo-rules` supporting GET (list/detail), POST, PUT/PATCH, DELETE.
- Specialized read endpoints:
  - `GET /tariffs/lookup?importer=..&origin=..&hsCode=..&hsVersion=..&date=..`
  - `GET /vat/{importerId}`
- DTOs and request validation; paginated list responses.
- HTTP status codes and error bodies consistent.

Sub-tasks
- Define DTOs and mappers (MapStruct or manual)  
  Done when controllers don’t expose entities directly.
- Implement controllers and routes with validation  
  Done when endpoints respond with 2xx for valid requests.
- Add global exception handler for consistent error shapes  
  Done when 4xx/5xx return structured JSON.
- Wire OpenAPI annotations and enable Swagger UI (read-only in prod)  
  Done when endpoints appear in the spec.

---

### Story: Integration tests with Testcontainers and Flyway

Description  
Automated tests prove mappings and queries work against a real Postgres.

Acceptance Criteria
- Testcontainers spins up Postgres; Flyway applies migrations.
- Repository and controller tests cover CRUD and key lookup queries.
- Tests run in CI and pass reliably.

Sub-tasks
- Configure Testcontainers with Flyway auto-migrate  
  Done when tests boot and schema exists.
- Write repository tests for MFN/PREF selection by date  
  Done when cases pass for overlapping validity windows.
- Write controller tests for CRUD and lookup endpoints  
  Done when HTTP tests pass with expected payloads.
- Add CI job to run tests in pipeline  
  Done when green build blocks merges on failure.

---

### Story (USER): As an API consumer, I want API documentation and usage examples

Description  
Concise docs enable clients to call the CRUD and lookup endpoints.

Acceptance Criteria
- OpenAPI generated and published per environment.
- README includes endpoint descriptions and sample requests/responses.
- Examples for tariff lookup and VAT retrieval included.

Sub-tasks
- Generate OpenAPI spec and host Swagger UI  
  Done when `/swagger-ui` shows endpoints locally.
- Add `docs/api/README.md` with curl examples  
  Done when examples work against dev.
- Publish Postman collection (optional)  
  Done when collection is linked in the README.
- Add versioning/change-log notes for breaking changes  
  Done when release notes reference API versions.


