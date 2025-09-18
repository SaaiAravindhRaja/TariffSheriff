# Architecture

TariffSheriff is composed of several coordinated components:

- `apps/frontend` — Vite + React + TypeScript single-page application. Uses Tailwind CSS and custom components.
- `apps/backend` — Java Maven service (Spring Boot or similar). Provides REST endpoints consumed by the frontend.
- `packages/shared-types` and `packages/shared-utils` — internal packages shared across apps.
- `infrastructure` — Docker/Kubernetes manifests and CI/CD helpers.

High-level data flow

1. User interacts with the frontend UI.
2. Frontend calls backend API endpoints to fetch tariff data, perform calculations, and log analytics.
3. Backend accesses any configured data stores or external APIs (document here if relevant).

Deployment

- The repository contains CI workflows under `.github/workflows` for building and testing frontend and backend.
- Deployments are handled via Vercel (frontend) and your backend deployment pipeline (CI / Kubernetes manifests).
