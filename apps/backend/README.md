# Backend

Spring Boot application for TariffSheriff.

## Database & Migrations

- Uses PostgreSQL with Flyway. The core schema lives in `src/main/resources/db/migration`.
- `V1__schema.sql` creates the canonical tariff tables (countries, HS products, tariff rates, VAT, etc.).
- `V2__seed_mock.sql` inserts a small EV-focused dataset (EU importer, Korea preference) for smoke testing.
- MFN rates always exist with `origin_id` `NULL` (applies to any origin). Preferential rates require an `agreement_id` and only replace MFN when RoO + certificate checks are satisfied in the UI/business logic.
- All rates are valid-date bounded (`valid_from`, optional `valid_to`) and VAT is stored separately in the `vat` table.

### Running locally

1. Provision PostgreSQL and create a database (default JDBC URI `jdbc:postgresql://localhost:5432/tariffsheriff`).
2. Set credentials via environment variables if they differ from the defaults in `application.properties` (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`).
3. Start the application: `./mvnw spring-boot:run`.

On startup Flyway runs the migrations automatically. You can verify the seed data with the contract queries shared in the design doc (resolve HS product, fetch MFN, fetch preferential, retrieve VAT).

```sql
-- Resolve HS product for EU importer, HS 870380 (version 2022)
SELECT id FROM hs_product
WHERE destination_id = 1 AND hs_version = '2022' AND hs_code = '870380';

-- MFN lookup on 2024-03-01
SELECT * FROM tariff_rate
WHERE importer_id = 1 AND hs_product_id = 1 AND basis = 'MFN'
  AND origin_id IS NULL AND valid_from <= DATE '2024-03-01'
  AND (valid_to IS NULL OR valid_to >= DATE '2024-03-01')
ORDER BY valid_from DESC
LIMIT 1;

-- Preferential lookup for Korea origin
SELECT * FROM tariff_rate
WHERE importer_id = 1 AND origin_id = 2 AND hs_product_id = 1 AND basis = 'PREF'
  AND valid_from <= DATE '2024-03-01'
  AND (valid_to IS NULL OR valid_to >= DATE '2024-03-01')
ORDER BY valid_from DESC
LIMIT 1;

-- VAT for EU importer
SELECT standard_rate FROM vat WHERE importer_id = 1;
```

### Running backend tests

These tests use Testcontainers to launch PostgreSQL automatically. Ensure Docker is running (Docker Desktop or Colima).

```bash
# if you are using Colima, expose the socket for JVM-based tools
export DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')"

cd apps/backend
mvn test
```

The Maven build disables Ryuk cleanup (via `TESTCONTAINERS_RYUK_DISABLED=true`) so Testcontainers works with rootless Docker setups such as Colima. Containers are still stopped at the end of the test run.

### Migrations: how to run and troubleshoot

Run migrations (Flyway runs on app startup)
- Maven (dev profile):
  ```powershell
  # set env vars first (replace with your values)
  $env:DATABASE_URL="jdbc:postgresql://<dev-host>:5432/tariffsheriff?sslmode=require"
  $env:DATABASE_USERNAME="app_dev"
  $env:DATABASE_PASSWORD="<dev-password>"
  cd apps/backend
  mvn spring-boot:run -Dspring-boot.run.profiles=dev
  ```
- Docker Compose (reads apps/backend/.env):
  ```powershell
  docker compose -f infrastructure/docker/docker-compose.yml up --build
  ```

Verify success
- Look for log lines similar to:
  - `Successfully validated X migrations`
  - `Schema "public" is up to date. No migration necessary.`
- Or run the smoke checks below (Flyway history, tables, indexes).

Troubleshooting
- Permission denied creating tables:
  - Your role needs DDL during migration. Temporarily grant and then revoke:
    ```sql
    GRANT CREATE ON SCHEMA public TO app_dev;  -- run migration
    REVOKE CREATE ON SCHEMA public FROM app_dev;  -- optional tighten afterwards
    ```
- SSL/TLS errors:
  - Ensure the JDBC URL includes `?sslmode=require`.
  - With Neon pooler hosts (`-pooler`), `\conninfo` shows SSL; `pg_stat_ssl` may be false (TLS terminates at the pooler).
- Wrong host/user/db:
  - Host should be your Neon branch endpoint, user `app_dev` (or `app_prod`), db `tariffsheriff`.
- Idempotency:
  - Restart and confirm logs show `No migration necessary.`

### Migration smoke checks

Use these quick queries to confirm Flyway ran and key objects exist (replace host/user/password as needed):

```powershell
$env:PGPASSWORD="<dev-password>"
psql -h <dev-host> -p 5432 -U app_dev -d tariffsheriff -c "select version, description, success, installed_on from flyway_schema_history order by installed_on desc limit 5;"

# Required tables
psql -h <dev-host> -p 5432 -U app_dev -d tariffsheriff -c "select table_name from information_schema.tables where table_schema='public' and table_name in ('country','agreement','agreement_party','hs_product','tariff_rate','vat','roo_rule') order by table_name;"

# Key indexes created by V1
psql -h <dev-host> -p 5432 -U app_dev -d tariffsheriff -c "select indexname from pg_indexes where schemaname='public' and tablename in ('hs_product','tariff_rate','roo_rule') order by indexname;"
```

Expected
- `flyway_schema_history` shows at least V1 applied successfully.
- All listed tables are returned.
- Index list includes: `uq_hs_product`, `idx_tariff_lookup`, `idx_pref_lookup`, `uq_roo_rule_agreement_product`.

### Database topology and ownership

This project uses Neon Postgres with two environments: dev and prod.

Branch → Database → Role mapping
- dev → `tariffsheriff` → `app_dev` (application), owner: `neondbowner`
- prod → `tariffsheriff` → `app_prod` (application), owner: `neondbowner`

What the roles do
- `app_*`: least-privilege LOGIN roles used by the running app. DML only (SELECT/INSERT/UPDATE/DELETE), can use sequences; no DDL.
- `neondbowner`: Neon’s project owner role; avoid for app runtime.

Note: If you later want stricter separation in prod, create a dedicated migration role (e.g., `migrate_prod`) for Flyway and keep `app_prod` DML-only.

How to retrieve connection info (Neon)
1. Open Neon Console → Project: TariffSheriff → choose branch (`dev` or `prod`).
2. Copy the hostname from Connection details. Database name is `tariffsheriff`.
3. Build the JDBC URL for Spring:
   - `jdbc:postgresql://<host>:5432/tariffsheriff?sslmode=require`
4. Use the appropriate role as username (`app_dev` or `app_prod`) and its password.

Environment variables used by Spring (see `src/main/resources/application.properties`)
- `DATABASE_URL` → JDBC URL
- `DATABASE_USERNAME` → DB role (e.g., `app_dev`, `app_prod`)
- `DATABASE_PASSWORD` → password for the role
- Aliases also supported: `DB_URL`, `DB_USER`, `DB_PASSWORD`

Secrets (GitHub Actions)
- Create repo secrets with these names so workflows can target each env:
  - `DATABASE_URL_DEV`, `DATABASE_USERNAME_DEV`, `DATABASE_PASSWORD_DEV`
  - `DATABASE_URL_PROD`, `DATABASE_USERNAME_PROD`, `DATABASE_PASSWORD_PROD`
- In your workflow job, map the per-env secrets to the env vars Spring expects:
  ```yaml
  env:
    DATABASE_URL: ${{ secrets.DATABASE_URL_DEV }}
    DATABASE_USERNAME: ${{ secrets.DATABASE_USERNAME_DEV }}
    DATABASE_PASSWORD: ${{ secrets.DATABASE_PASSWORD_DEV }}
  ```

Local development
- Copy this into `apps/backend/.env.example` (commit this file; placeholders only):
  ```properties
  DATABASE_URL=jdbc:postgresql://<dev-host>:5432/tariffsheriff?sslmode=require
  DATABASE_USERNAME=app_dev
  DATABASE_PASSWORD=CHANGE_ME
  ```
- Or export env vars before running. Real `.env*` files are ignored by git (see repo `.gitignore`).

Owners and access
- Product/DB owner: Nathan
- Operational owner (rotating on-call): Nathan
- To request access or changes: open a ticket and tag the owners above.

Neon console links
- Project: https://console.neon.tech/app/projects/bold-pond-35683769
- Dev branch: https://console.neon.tech/app/projects/bold-pond-35683769/branches/br-lively-recipe-a11rpxzb
- Prod branch: https://console.neon.tech/app/projects/bold-pond-35683769/branches/br-old-field-a1cdwcu3