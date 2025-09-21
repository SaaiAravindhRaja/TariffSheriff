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
