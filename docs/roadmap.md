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
  - Entities: `Country`, `Agreement`, `AgreementParty`, `HsProduct`, `TariffRate`, `Vat`, `RooRule`.
  - Lookup algorithm (server returns both MFN and PREF candidates):
    1) Resolve importer by `iso2` (2 letters, uppercase). Resolve origin if provided (optional).
    2) Resolve product by `(destination_id, hs_version, hs_code)`. Default `hs_version = '2022'` unless specified.
    3) MFN candidate: find most recent row effective on date D for `(importer, product, basis='MFN')` with `origin_id IS NULL`. If an origin‑specific MFN row exists and origin was provided, return it as `mfnOriginOverride` alongside the general MFN (client can ignore if not needed).
    4) Preferential candidate: if origin provided, find most recent row for `(importer, origin, product, basis='PREF')` effective on D; only include if the linked agreement is in force on D.
    5) Response contains both `mfn` and `pref` (if present) so the client can decide based on RVC.
  - Calculator (server computes total duty and indicates basis):
    - Request carries: `{ totalValue, mfnRate, prefRate, rvcThreshold, materialCost, labourCost, overheadCost, profit, otherCosts, fob }`.
    - RVC formula (consistent with existing implementation):
      `RVC = (materialCost + labourCost + overheadCost + profit + otherCosts) / fob * 100` (percent).
    - Basis selection: if `RVC >= rvcThreshold` and `prefRate` present, apply `prefRate`, else `mfnRate`.
    - Rates are decimals (e.g., `0.10` for 10%). `totalDuty = totalValue * appliedRate`.
    - Response: `{ basis, appliedRate, totalDuty, rvc, rvcThreshold }`.
  - Endpoints:
    - `GET /api/tariff-rate/lookup?importerIso2=&originIso2=&hsCode=&date=` (date optional, default today).
    - `POST /api/tariff-rate/calculate` (RVC‑aware calculator).
  - Validation & errors:
    - `importerIso2/originIso2`: exactly 2 letters; 400 on invalid or unknown code.
    - `hsCode`: 4–10 digits; 400 on invalid; 404 if product not found.
    - Date parsing: ISO‑8601 `YYYY‑MM‑DD`; defaults to `LocalDate.now()`.
- Web/Config
  - Global exception handler: consistent JSON `{ error, message }` with appropriate status (400/404/500).
  - OpenAPI/Swagger configuration exposed in dev.
  - CORS allowlist for `http://localhost:3000` and `http://127.0.0.1:3000` with credentials.
  - Bean validation enabled; fail‑fast for invalid payloads.

## Database & Migrations

- V1 schema; V2 seed (small EV‑focused dataset: a few countries, HS lines, agreements, VAT).
- Integrity constraints:
  - `hs_product` unique `(destination_id, hs_version, hs_code)`.
  - `tariff_rate.basis` in (`'MFN'`, `'PREF'`).
  - For `tariff_rate`: `origin_id IS NULL` when basis=`MFN`; `agreement_id IS NOT NULL AND origin_id IS NOT NULL` when basis=`PREF`.
  - Validity: `(valid_from <= D) AND (valid_to IS NULL OR valid_to >= D)`.
- Indexes:
  - `tariff_rate(importer_id, hs_product_id, basis, valid_from DESC)`.
  - `tariff_rate(importer_id, origin_id, hs_product_id, basis, valid_from DESC)`.
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
  - `GET /api/products?importerIso2=&q=` (only if UI requires text search for HS lines).

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
