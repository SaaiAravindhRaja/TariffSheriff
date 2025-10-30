# TariffSheriff

[![Live Demo](https://img.shields.io/badge/demo-vercel-000000?logo=vercel&style=for-the-badge)](https://tariffsheriff-frontend.vercel.app/) [![CI](https://github.com/SaaiAravindhRaja/TariffSheriff/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/SaaiAravindhRaja/TariffSheriff/actions) [![version](https://img.shields.io/badge/version-1.0.4-blue?style=for-the-badge)](https://github.com/SaaiAravindhRaja/TariffSheriff/releases)

A full-stack web application that helps businesses calculate and analyze import tariffs and fees across countries, with a focus on the **Electric Vehicle (EV) industry**.

---

<p align="center">

<img src="https://github.com/user-attachments/assets/aafe9ae4-9f11-47c1-998c-7012a57c0e72" alt="TariffSheriff App Icon" width="150"/>

</p>

---

## Features

- **Accurate Tariff Calculations** - Real-time import duty calculations across multiple countries
- **EV Industry Focus** - Specialized data for Electric Vehicle trade compliance  
- **Route Optimization** - Find the most cost-effective shipping routes
- **Transparent Pricing** - Detailed breakdowns with legal citations
- Optional features (AI assistant, admin/ops tooling) are not enabled by default in this branch to keep the core small for class use. AI can still be enabled via backend profile `ai`.

---

## Development Quickstart

**Prerequisites:** Node.js 18+, npm 9+, Java 17, Docker (Docker Desktop or Colima).

```bash
# 1. Install dependencies (from repo root)
npm ci

# 2. Start PostgreSQL once (reuse this container for future runs)
docker run -d --name tariffsheriff-postgres \
  -e POSTGRES_USER=tariff_sheriff \
  -e POSTGRES_PASSWORD=tariff_sheriff \
  -e POSTGRES_DB=tariffsheriff \
  -p 5432:5432 \
  postgres:16

# 3. Start the backend (Flyway will auto-run migrations and seed data)
# export DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')"  # only required for Colima/rootless Docker
(cd apps/backend && mvn spring-boot:run)

# 4. In another terminal (from repo root), start the frontend dev server
npm run dev --workspace=frontend

# Dev servers
#   Frontend: http://localhost:3000
#   Backend : http://localhost:8080

# 5. Automated checks before committing
mvn test                     # backend (Testcontainers)
npm run test --workspace=frontend

# 6. AI Assistant (optional): run with profile
# (cd apps/backend && mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.profiles.active=ai")

# 7. CI builds and tests are handled by GitHub Actions (.github/workflows/ci.yml)
```

> Need a clean database? Stop the backend, remove any local Postgres container (e.g. `docker rm -f tariffsheriff-postgres`), then rerun `mvn spring-boot:run` to replay Flyway migrations and the mock seed.

---

## Live Demo

[https://tariffsheriff-frontend.vercel.app/](https://tariffsheriff-frontend.vercel.app/)
___


## System Architecture

```mermaid
flowchart TB
    subgraph clients [" Client Layer "]
        web[Web Dashboard<br/>React + TypeScript + Tailwind]
        mobile[Mobile PWA<br/>Responsive Design]
    end

    subgraph gateway [" API Gateway "]
        lb[Load Balancer<br/>SSL + CORS]
    end

    subgraph backend [" Backend Services "]
        auth[Authentication<br/>JWT + Spring Security]
        calc[Tariff Calculator<br/>HS Code + MFN/Pref Rates]
        docs[API Docs<br/>Swagger/OpenAPI]
    end

    subgraph storage [" Data Storage "]
        db[(PostgreSQL<br/>Tariff Rules + Users)]
        cache[(Redis<br/>Session + Query Cache)]
    end

    subgraph external [" External APIs "]
        wits[WITS<br/>Trade Stats]
        hscode[HS Codes<br/>Classification]
        regional[Regional<br/>Country Data]
    end

    subgraph cicd [" CI/CD Pipeline "]
        actions[GitHub Actions<br/>Build + Test + Deploy]
    end

    web --> lb
    mobile --> lb

    lb --> auth
    lb --> calc
    lb --> docs

    auth --> db
    auth --> cache

    calc --> db
    calc --> cache
    calc -.-> wits
    calc -.-> hscode
    calc -.-> regional

    actions -.-> backend
    actions -.-> storage

    classDef clientClass fill:#e3f2fd,stroke:#1976d2,stroke-width:2px
    classDef gatewayClass fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px
    classDef backendClass fill:#e8f5e9,stroke:#388e3c,stroke-width:2px
    classDef storageClass fill:#fff3e0,stroke:#f57c00,stroke-width:2px
    classDef externalClass fill:#fce4ec,stroke:#c2185b,stroke-width:2px
    classDef cicdClass fill:#f1f8e9,stroke:#689f38,stroke-width:2px

    class web,mobile clientClass
    class lb gatewayClass
    class auth,calc,docs backendClass
    class db,cache storageClass
    class wits,hscode,regional externalClass
    class actions cicdClass
```

## Project Structure

```
apps/
	backend/    # Spring Boot backend API
	frontend/   # React + Vite frontend UI
packages/     # Shared libraries (types, utils)
docs/         # Documentation
infrastructure/ # Docker, K8s, CI/CD configs
```

## Data Flow & Business Logic

```mermaid
sequenceDiagram
    participant U as User
    participant F as Frontend
    participant A as Auth Service
    participant T as Tariff Engine
    participant D as Database
    participant E as External APIs

    U->>F: Access application
    F->>A: Authenticate user
    A->>D: Validate credentials
    D-->>A: User data
    A-->>F: JWT token

    U->>F: Input calculation request<br/>(HS code, origin, destination)
    F->>T: Request tariff calculation

    T->>D: Query HS product
    D-->>T: Product details

    T->>D: Query MFN rates
    D-->>T: MFN rate data

    T->>D: Query preferential rates<br/>(if applicable)
    D-->>T: Preferential rate data

    T->>D: Query VAT rates
    D-->>T: VAT data

    opt External Data Needed
        T->>E: Request additional trade data
        E-->>T: Trade statistics & classifications
    end

    T->>T: Apply calculation logic:<br/>• Rules of Origin (RVC)<br/>• Trade agreements<br/>• MFN vs Preferential

    T-->>F: Return calculation:<br/>• Applied rate<br/>• Total duty<br/>• Breakdown<br/>• Citations

    F-->>U: Display results with<br/>transparent breakdown
```

## Core Business Logic

### Tariff Calculation Engine
- **Input Processing**: Product category (HS code), origin/destination countries, transaction details
- **Rule Matching**: Applies appropriate tariff rules based on trade agreements and validity periods
- **Rate Calculation**: Supports percentage-based and flat fee structures
- **Citation Generation**: Provides transparent rule references for compliance

### Key Features
- **HS Code Resolution**: Automatic product classification using harmonized system codes
- **Multi-Country Support**: Handles bilateral and multilateral trade agreements
- **Time-Sensitive Rules**: Applies correct rates based on transaction dates
- **MFN Treatment**: Most-Favored-Nation rate calculations
- **Certificate Handling**: Processes origin certificates and special conditions

---

## Key Features & Capabilities

- **Accurate Tariff Calculations**: Real-time import duty calculations with support for MFN and preferential rates
- **Smart Rate Selection**: Automatic Rules of Origin (RVC) calculations to determine optimal tariff rates
- **Comprehensive Database**: Extensive trade agreement data with validity windows and legal citations
- **Hosted Database Support**: Full compatibility with cloud-hosted PostgreSQL (Neon, AWS RDS, Google Cloud SQL)
- **Modern Authentication**: Secure JWT-based authentication with axios API client integration
- **Enhanced User Experience**: Streamlined calculator interface with improved form validation and error handling
- **API Documentation**: Complete OpenAPI/Swagger documentation for all endpoints
- **Progressive Web App**: PWA capabilities for mobile-friendly access and offline support
- **CI/CD Integration**: Automated testing and deployment via GitHub Actions
- **Optional AI Assistant**: Intelligent tariff lookup and compliance analysis (enable via backend profile `ai`)

---

## Getting Started

```bash
# 1. Install dependencies (from repo root)
npm ci

# 2. Start the backend (Spring Boot)
cd apps/backend && ./mvnw spring-boot:run

# 3. Start the frontend (Vite)
npm run dev --workspace=frontend

# 4. Build frontend for production (from repo root)
npm run build
```

### Deployment (Vercel)

- If you deploy the monorepo to Vercel, the frontend app is in `apps/frontend`.
- Place a `vercel.json` inside `apps/frontend/` with:

```json
{
    "outputDirectory": "dist"
}
```

- Alternatively, in the Vercel project settings set the **Root Directory** to `apps/frontend` and the **Output Directory** to `dist`.
- The root-level `vercel.json` is intentionally minimal to avoid schema validation errors — per-app configs in subfolders are recommended for monorepos.

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.1 with Java 17
- **Security**: Spring Security + JWT for stateless authentication
- **Database**: PostgreSQL with JPA/Hibernate
- **API Documentation**: Swagger/OpenAPI 3.0
- **Caching**: Redis for session management and query optimization
- **Testing**: JUnit 5 + Mockito

### Frontend
- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite for fast development and building
- **Styling**: Tailwind CSS + Radix UI components
- **State Management**: TanStack Query for server state
- **Forms**: React Hook Form with Zod validation
- **Charts**: Recharts for data visualization
- **Animation**: Framer Motion for smooth interactions

### Infrastructure & DevOps
- **Containerization**: Docker + Docker Compose
- **Orchestration**: AWS ECS (Elastic Container Service)
- **CI/CD**: GitHub Actions for automated testing and deployment
- **Monitoring**: AWS CloudWatch for logs and metrics
- **Load Balancing**: AWS Application Load Balancer

### External Integrations
- **Trade Data**: World Integrated Trade Solution (WITS) API
- **Product Classification**: HS Code lookup services
- **Regional Data**: Country-specific trade portals


---

## Contributing

- See the [`docs/`](docs/) folder for guidelines and architecture decisions.
- Use atomic, logical commits for all changes.

---


## Contributors

<table>
	<tr>
		<td align="center">
			<a href="https://github.com/SaaiAravindhRaja">
				<img src="https://github.com/SaaiAravindhRaja.png" width="80" alt="Saai"/><br/>
				<sub><b>Saai</b></sub>
			</a>
		</td>
		<td align="center">
			<a href="https://github.com/thanh913">
				<img src="https://github.com/thanh913.png" width="80" alt="Billy"/><br/>
				<sub><b>Billy</b></sub>
			</a>
		</td>
		<td align="center">
			<a href="https://github.com/minyiseah">
				<img src="https://github.com/minyiseah.png" width="80" alt="Min yi"/><br/>
				<sub><b>Min yi</b></sub>
			</a>
		</td>
		<td align="center">
			<a href="https://github.com/LSH-Tech-tp">
				<img src="https://github.com/LSH-Tech-tp.png" width="80" alt="Sing Ho"/><br/>
				<sub><b>Sing Ho</b></sub>
			</a>
		</td>
		<td align="center">
			<a href="https://github.com/GarvitSobti">
				<img src="https://github.com/GarvitSobti.png" width="80" alt="Garvit"/><br/>
				<sub><b>Garvit</b></sub>
			</a>
		</td>
		<td align="center">
			<a href="https://github.com/nathan11474">
				<img src="https://github.com/nathan11474.png" width="80" alt="Nathan"/><br/>
				<sub><b>Nathan</b></sub>
			</a>
		</td>
	</tr>
</table>

---

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
