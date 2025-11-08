# TariffSheriff — Minimal End‑State

This document defines the clear, minimal shape of the codebase to serve the project purpose.

## Purpose

- Calculate import duties using HS codes, MFN vs preferential rates, and VAT.
- Provide transparent citations (validity windows, agreements).
- Offer a clean UI to search, calculate, and inspect core data.
- Include minimal, correct authentication and basic security.

## Scope

- Tariff lookup and calculator.
- Country/product search for inputs.
- Authentication (register/login/validate).

## Architecture

- Monorepo: Spring Boot backend + React frontend.
- PostgreSQL + Flyway migrations and seed data.
- Stateless REST over JSON; OpenAPI/Swagger exposed in dev.

## Repository Layout

```
apps/
  backend/
    src/main/java/com/tariffsheriff/backend
      auth/     # JWT + auth controllers/filters/DTOs
      tariff/   # entities, repos, services, controllers
      web/      # root, error handling, OpenAPI
      config/   # CORS, Jackson, Swagger, validation
    src/main/resources/db/migration
    application.properties
    application-dev.properties
  frontend/
    src/pages/        # Dashboard, Calculator, Database, Auth
    src/components/   # UI primitives, layout, charts
    src/contexts/     # auth and app settings
    src/hooks/        # API and UI hooks
    src/services/     # API client
    src/styles/       # global CSS
docs/
infrastructure/
```

## Backend

- Auth
  - Endpoints: `/api/auth/register`, `/api/auth/login`, `/api/auth/validate`.
  - Security: BCrypt for passwords; JWT (HS256). Protect `/api/**` except `/api/auth/**`, `/v3/api-docs/**`, `/swagger-ui/**`, and health.
  - Validation: email format, password min length 6; duplicate email returns 400 with `{ message }`.
- Tariff
  - Entities: `Country`, `Agreement`, `AgreementParty`, `HsProduct`, `TariffRate`.
  - Lookup algorithm (server returns both MFN and PREF candidates):
    1) Resolve importer by ISO3 (3 letters, uppercase). Resolve origin if provided (optional).
    2) Resolve product by `(destination_iso3, hs_version, hs_code)`. Default `hs_version = '2022'` unless specified.
    3) MFN candidate: fetch `(importer_iso3, product, basis='MFN')` with `origin_iso3 IS NULL`. If origin-specific MFN rows exist, prefer matching origin when provided.
    4) Preferential candidate: if origin provided, fetch `(importer_iso3, origin_iso3, product, basis='PREF')`. Agreements are assumed active; eligibility determined by RVC only.
    5) Response contains both MFN and PREF options so the client can compare using backend-computed RVC.
  - Calculator (server computes total duty and indicates basis):
    - Request carries: `{ totalValue, mfnRate, prefRate, rvcThreshold, materialCost, labourCost, overheadCost, profit, otherCosts, fob }`.
    - RVC formula (consistent with existing implementation):
      `RVC = (materialCost + labourCost + overheadCost + profit + otherCosts) / fob * 100` (percent).
    - Basis selection: if `RVC >= rvcThreshold` and `prefRate` present, apply `prefRate`, else `mfnRate`.
    - Rates are decimals (e.g., `0.10` for 10%). `totalDuty = totalValue * appliedRate`.
    - Response: `{ basis, appliedRate, totalDuty, rvc, rvcThreshold }`.
  - Endpoints:
    - `GET /api/tariff-rate/lookup?importerIso3=&originIso3=&hsCode=&date=` (date optional, default today).
    - `POST /api/tariff-rate/calculate` (RVC‑aware calculator).
  - Validation & errors:
    - `importerIso3/originIso3`: exactly 3 letters; 400 on invalid or unknown code.
    - `hsCode`: 4–10 digits; 400 on invalid; 404 if product not found.
    - Date filtering removed for now (dataset is static for 2022); future enhancement can reintroduce validity windows.
- Web/Config
  - Global exception handler: consistent JSON `{ error, message }` with appropriate status (400/404/500).
  - OpenAPI/Swagger configuration exposed in dev.
  - CORS allowlist for `http://localhost:3000` and `http://127.0.0.1:3000` with credentials.
  - Bean validation enabled; fail‑fast for invalid payloads.

## Database & Migrations

- V1 schema; V2 seed (small EV‑focused dataset: a few countries, HS lines, agreements, VAT).
- Integrity constraints:
  - `hs_product` unique `(destination_iso3, hs_version, hs_code)`.
  - `tariff_rate.basis` in (`'MFN'`, `'PREF'`).
  - For `tariff_rate`: `origin_iso3 IS NULL` when basis=`MFN`; `agreement_id IS NOT NULL AND origin_iso3 IS NOT NULL` when basis=`PREF`.
  - Validity windows omitted in this snapshot; each rate row is assumed current.
- Indexes:
  - `tariff_rate(importer_iso3, hs_product_id)`.
  - `tariff_rate(importer_iso3, origin_iso3, hs_product_id)`.
- Seeded admin/user passwords are BCrypt‑hashed (no plain text).

## API Surface

- Auth
  - `POST /api/auth/register` → 200 `{ token, id, name, email, role, isAdmin }` or 400 `{ message }`.
  - `POST /api/auth/login` → 200 `{ token, id, name, email, role, isAdmin }` or 400 `{ message }`.
  - `POST /api/auth/validate` → 200 `{ token, id, name, email, role, isAdmin }` or 400.
- Tariff
  - `GET /api/tariff-rate/lookup` → 200 `{ mfn: {...}, pref: {...}|null, agreement: {...}|null }` or 400/404.
  - `POST /api/tariff-rate/calculate` → 200 `{ basis, appliedRate, totalDuty, rvc, rvcThreshold }` or 400.
- Reference
  - `GET /api/countries` → basic list for selectors.
  - `GET /api/products?importerIso3=&q=` (only if UI requires text search for HS lines).

## Frontend

- Pages: Login, Dashboard, Calculator, Database.
- State: TanStack Query for server state; `AuthContext` persists token/user in localStorage; Authorization header on requests.
- Forms: client‑side validation (email, password length); error toasts on 4xx.
- Config: `VITE_API_BASE_URL` for backend.

## Development

- Start Postgres via Docker.
- Backend: `mvn spring-boot:run` (ensure JDK 17 and a 256‑bit Base64 `JWT_SECRET`).
- Frontend: `npm run dev --workspace=frontend`.
- Swagger UI at `/swagger-ui.html`.

## Testing

- Backend: unit tests (services), slice tests (repos), WebMvc tests (controllers).
- Frontend: component tests for calculator and auth forms.

## Deployment

- Backend container (port 8080); env‑injected DB and JWT secret.
- Frontend static build (Vercel/Netlify or Nginx).
- CI: run backend and frontend tests + build.

## Non‑Functional

- Fast setup, deterministic migrations, clear errors, minimal dependencies.
