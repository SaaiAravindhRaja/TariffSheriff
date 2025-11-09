# TariffSheriff

[![CI](https://github.com/SaaiAravindhRaja/TariffSheriff/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/SaaiAravindhRaja/TariffSheriff/actions)  
Modern trade-intelligence stack for calculating import duties, surfacing HS-product data, and exploring trade agreements with Auth0-protected APIs and a React dashboard.

---

## At a Glance

| Layer      | Key Tech                                                       |
|------------|----------------------------------------------------------------|
| Frontend   | React 18 + TypeScript, Vite, Tailwind, Radix UI, Framer Motion |
| Backend    | Spring Boot 3 (Java 17), JPA/Hibernate, Flyway, PostgreSQL     |
| Security   | Auth0 (OAuth2/OIDC, JWT resource server)                       |
| Tooling    | Maven, npm workspaces, Docker/Postgres, GitHub Actions CI      |

The current schema is ISO3-based (e.g. `USA`, `KOR`) for countries and treats tariff rows as immutable snapshots with MFN/preferential basis data plus non–ad-valorem metadata.

---

## Local Development

### Prerequisites
* Node.js 18+ / npm 9+
* Java 17 (JDK) + Maven 3.9+
* Docker (for the default Postgres dev DB)

### 1. Clone + Install Node deps
```bash
git clone https://github.com/SaaiAravindhRaja/TariffSheriff.git
cd TariffSheriff
npm ci
```

### 2. Backend environment
Create `apps/backend/.env` and point it at your Postgres + Auth0 tenant:
```dotenv
DATABASE_URL=jdbc:postgresql://localhost:5432/tariffsheriff
DATABASE_USERNAME=tariff_sheriff
DATABASE_PASSWORD=tariff_sheriff

# Auth0 Resource Server settings
AUTH0_ISSUER=https://<your-tenant>.us.auth0.com/
AUTH0_AUDIENCE=https://api.tariffsheriff.com

# Optional: AI assistant profile
OPENAI_API_KEY=<if running profile 'ai'>
```
Start Postgres however you like. Example Docker command:
```bash
docker run -d --name tariffsheriff-postgres \
  -e POSTGRES_USER=tariff_sheriff \
  -e POSTGRES_PASSWORD=tariff_sheriff \
  -e POSTGRES_DB=tariffsheriff \
  -p 5432:5432 postgres:16
```

Then boot the API (from repo root):
```bash
cd apps/backend
mvn spring-boot:run
```
Flyway automatically applies the ISO3 schema at startup.

### 3. Frontend environment
Create `apps/frontend/.env.local`:
```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
VITE_AUTH0_DOMAIN=<your-tenant>.us.auth0.com
VITE_AUTH0_CLIENT_ID=<spa-client-id>
VITE_AUTH0_AUDIENCE=https://api.tariffsheriff.com
VITE_AUTH0_REDIRECT_URI=http://localhost:3000
```
> Important: make sure the Auth0 application has the **TariffSheriff API permission/role** assigned; otherwise tokens will not have the scope required for `/api/tariff-rate/**` and the backend will reply with `403 insufficient_scope`.

Start the Vite dev server:
```bash
npm run dev --workspace=frontend
# -> http://localhost:3000
```

### 4. Useful npm / Maven scripts
```bash
# Frontend tests / build
npm run test --workspace=frontend
npm run build --workspace=frontend

# Backend tests
cd apps/backend
mvn test
```

---

## Repository Layout
```
apps/
  backend/   Spring Boot API + Flyway migrations
  frontend/  React dashboard
docs/        Product notes / architecture writeups
start-app.sh, stop-app.sh   convenience scripts
```

---

## Backend Highlights
* **ISO3-first schema** – see `apps/backend/src/main/resources/db/migration/V1__schema.sql`.
* **Auth0 resource server** – Spring Security validates JWTs against `AUTH0_ISSUER`/`AUTH0_AUDIENCE`. Any `/api/**` route requires an access token whose scope/permissions include the TariffSheriff API role.
* **Tariff services** – `TariffRateServiceImpl` resolves HS products, MFN vs PREF rates, and RVC thresholds; `TariffRateLookupDto` now returns `importerIso3`/`originIso3` plus non–ad-valorem metadata.
* **Trade agreements** – `AgreementController` exposes `/api/agreements` and `/api/agreements/by-country/{iso3}` with RVC thresholds only (legacy status/type fields were retired with the new schema).

---

## Frontend Highlights
* Auth0 React SDK wraps the app; Axios interceptors inject access tokens for every API call.
* Country data pulls directly from `/api/countries` (ISO3). All selectors and API payloads pass ISO3 codes.
* Calculator automatically selects MFN/PREF rates based on backend responses and live RVC math.

---

## Auth & Permissions
1. Create an Auth0 “Regular Web App” (SPA) for the frontend and a custom API (`https://api.tariffsheriff.com`).
2. Enable **RBAC** + **Add Permissions in the Access Token** for the API.
3. Create a role (e.g., `tariffsheriff-user`) that grants whatever permission name you configured (e.g., `read:tariffs`). Assign this role to test users **and** enable it in the SPA’s “API Permissions”.
4. During development, grab tokens via the browser by inspecting any request in the Network tab and copy the `Authorization: Bearer …` header.

Without that role the backend will answer `/api/tariff-rate/**` with:
```
WWW-Authenticate: Bearer error="insufficient_scope"
```

---

## Optional AI Assistant
The chatbot endpoints remain in the codebase but are disabled by default. To experiment:
```bash
cd apps/backend
mvn spring-boot:run -Dspring-boot.run.profiles=ai
```
Provide `OPENAI_API_KEY` plus any rate-limit overrides you need.

---

## Cleaning & Tooling Notes
* Legacy `/api/auth/**` endpoints have been removed in favor of Auth0-only flows.
* All country models / DTOs / repos now operate solely on ISO3 codes.
* If you change the schema, update the single Flyway migration and reapply it (drop/recreate DB in dev).

---

## Contributing
1. Branch off `main`.
2. Run `mvn test` and `npm run test --workspace=frontend`.
3. Submit a PR with screenshots / curl snippets if you touched API behavior.

Please see `CONTRIBUTING.md` and `SECURITY.md` for more policies.
