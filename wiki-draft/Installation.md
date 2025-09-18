# Installation

This repository contains a frontend (Vite/React/TypeScript), backend (Java/Maven), and shared packages. Follow the steps below to set up a development environment.

1. System requirements
   - macOS/Linux/Windows with WSL
   - Node.js LTS (18+ recommended)
   - Java 17+
   - Maven 3.8+
   - Docker & Docker Compose (optional)

2. Clone and install

```bash
git clone https://github.com/SaaiAravindhRaja/TariffSheriff.git
cd TariffSheriff

# install root dependencies if any (repo uses per-app installs)
npm --prefix apps/frontend install
npm --prefix apps/backend install # if backend has node tooling
npm --prefix packages/shared-utils install
```

3. Run services

- Frontend (Vite dev): `npm --prefix apps/frontend run dev`
- Backend: use your IDE or `mvn -f apps/backend/pom.xml spring-boot:run` (if a Spring Boot app)
- To run both with Docker Compose, see `infrastructure/docker/docker-compose.yml` (if configured)
