# Backend

Spring Boot application for TariffSheriff (REST API).

## Overview

The backend provides a RESTful API for tariff calculations, user authentication, and trade data management. Built with Spring Boot and PostgreSQL, it supports both local development and cloud-hosted database deployments.

## Database & Migrations

- Uses PostgreSQL with Flyway. The core schema lives in `src/main/resources/db/migration`.
- `V1__schema.sql` creates the canonical tariff tables (countries, HS products, tariff rates, VAT, etc.).
- `V2__seed_mock.sql` inserts a small EV-focused dataset (EU importer, Korea preference) for smoke testing.
- MFN rates always exist with `origin_id` `NULL` (applies to any origin). Preferential rates require an `agreement_id` and only replace MFN when RoO + certificate checks are satisfied in the UI/business logic.
- All rates are valid-date bounded (`valid_from`, optional `valid_to`) and VAT is stored separately in the `vat` table.

### Running locally

1. **Provision PostgreSQL** and create a database (default JDBC URI `jdbc:postgresql://localhost:5432/tariffsheriff`).

2. **Configure Environment Variables**: Create a `.env` file in `apps/backend/` with the following:

```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5432/tariffsheriff
DATABASE_USERNAME=your_db_user
DATABASE_PASSWORD=your_db_password

# JWT Configuration
JWT_SECRET=your-jwt-secret-here

# Server Configuration
SERVER_PORT=8080
```

3. **Start the application**: 
```bash
cd apps/backend
./mvnw spring-boot:run
```

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

### API (Minimal)

All endpoints use JSON and require a Bearer token, except `/api/auth/**` and health/docs.

- POST `/api/auth/register` → 200 `{ token, id, name, email, role, isAdmin }` or 400
- POST `/api/auth/login` → same as register
- POST `/api/auth/validate` → same as register
- GET `/api/countries?q=&page=&size=` → list of countries
- GET `/api/tariff-rate/lookup?importerIso3=&originIso3=&hsCode=` → `{ mfn, pref, agreement }`
- POST `/api/tariff-rate/calculate` → `{ basis, appliedRate, totalDuty, rvc, rvcThreshold }`

Example usage:

```bash
# Register
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com","password":"secret123"}'

# Login
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"secret123"}'

# Countries
curl -s http://localhost:8080/api/countries \
  -H "Authorization: Bearer YOUR_JWT"

# Lookup (seeded example)
curl -s "http://localhost:8080/api/tariff-rate/lookup?importerIso3=USA&originIso3=KOR&hsCode=870380" \
  -H "Authorization: Bearer YOUR_JWT"

# Calculator
curl -s -X POST http://localhost:8080/api/tariff-rate/calculate \
  -H 'Content-Type: application/json' -H "Authorization: Bearer YOUR_JWT" \
  -d '{"mfnRate":0.10,"prefRate":0.00,"rvcThreshold":40,"totalValue":1000,"materialCost":20,"labourCost":10,"overheadCost":10,"profit":5,"otherCosts":5,"fob":100}'

# Response example
{"basis":"PREF","appliedRate":0.00,"totalDuty":0.00,"rvc":50.00,"rvcThreshold":40}
```

OpenAPI/Swagger: http://localhost:8080/swagger-ui.html

### Docker Compose (backend + Postgres)

From `apps/backend`, you can run the backend together with Postgres using Docker Compose:

1. Start services
```bash
docker compose up --build -d
```

2. Access the API
- Backend: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html

3. Stop and remove containers
```bash
docker compose down -v
```

Notes
- Runs with `SPRING_PROFILES_ACTIVE=dev` for Swagger and relaxed CORS in development.
- A demo Base64 JWT secret is included for local use; change if needed.

### Connecting to Neon (hosted Postgres)

The backend reads its datasource settings from standard environment variables, so you can point it at a Neon database without code changes. Example:

```bash
# Required
export DATABASE_URL="jdbc:postgresql://YOUR_NEON_HOST:5432/tariffsheriff?sslmode=require"
export DATABASE_USERNAME="app_dev"
export DATABASE_PASSWORD="your-neon-password"

# Recommended tuning for serverless Postgres
export DATABASE_SSL=true                 # enables driver SSL flag
export DATABASE_SSLMODE=require          # redundant but explicit
export DATABASE_MAX_POOL_SIZE=5          # Neon limits session counts
export DATABASE_MIN_IDLE=0
```

Then start the backend as usual (`./mvnw spring-boot:run` or `mvn spring-boot:run`). Flyway will apply the migrations on first run; if you want to skip them for a pre-provisioned schema you can set `SPRING_FLYWAY_ENABLED=false`.
# Deployment test
# Trigger deployment
