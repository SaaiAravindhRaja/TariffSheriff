# 👮 TariffSheriff

[![Live Demo](https://img.shields.io/badge/demo-vercel-000000?logo=vercel&style=for-the-badge)](https://tariffsheriff-frontend.vercel.app/) [![CI](https://github.com/SaaiAravindhRaja/TariffSheriff/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/SaaiAravindhRaja/TariffSheriff/actions) [![version](https://img.shields.io/badge/version-1.0.0-blue?style=for-the-badge)](https://github.com/SaaiAravindhRaja/TariffSheriff/releases)

A full-stack web application that helps businesses calculate and analyze import tariffs and fees across countries, with a focus on the **Electric Vehicle (EV) industry**.

---

<p align="center">

<img src="https://github.com/user-attachments/assets/aafe9ae4-9f11-47c1-998c-7012a57c0e72" alt="TariffSheriff App Icon" width="150"/>

</p>

---

## 🚀 Features

- **Accurate Tariff Calculations** - Real-time import duty calculations across multiple countries
- **EV Industry Focus** - Specialized data for Electric Vehicle trade compliance  
- **Route Optimization** - Find the most cost-effective shipping routes
- **Transparent Pricing** - Detailed breakdowns with legal citations
- **Admin Dashboard** - Manage tariff rules and user permissions
- **API Integration** - Connect with WITS, HS Code services, and regional trade portals

---

## 🛠 Development Quickstart

**Prerequisites:** Node.js 18+, npm 9+, Java 17, Docker (Docker Desktop or Colima).

```bash
# 1. Install dependencies (from repo root)
npm ci

# 2. Start the backend (Flyway will auto-run migrations and seed data)
export DOCKER_HOST="$(docker context inspect --format '{{.Endpoints.docker.Host}}')"  # only required for Colima/rootless Docker
cd apps/backend
mvn spring-boot:run

# 3. In another terminal (from repo root), start the frontend dev server
npm run dev --workspace=frontend

# Dev servers
#   Frontend: http://localhost:3000
#   Backend : http://localhost:8080

# 4. Automated checks before committing
mvn test                     # backend (Testcontainers)
npm run test --workspace=frontend

# 5. Production deployment is handled by GitHub Actions (.github/workflows/ci.yml)
```

> Need a clean database? Stop the backend, remove any local Postgres container (e.g. `docker rm -f tariffsheriff-pg`), then rerun `mvn spring-boot:run` to replay Flyway migrations and the mock seed.

---

## 🌐 Live Demo

[https://tariffsheriff-frontend.vercel.app/](https://tariffsheriff-frontend.vercel.app/)
___


## 🏗️ System Architecture

```mermaid
graph TB
    %% Client Layer
    subgraph CLIENT["🌐 Client Applications"]
        WEB["� Web rDashboard<br/>React + TypeScript<br/>Tailwind CSS"]
        MOBILE["📱 Mobile App<br/>Responsive Design<br/>PWA Support"]
        API_CLIENT["🔌 API Clients<br/>Third-party Integrations"]
    end

    %% Load Balancer & Gateway
    subgraph GATEWAY["🚪 API Gateway Layer"]
        ALB["⚖️ Application Load Balancer<br/>AWS ALB<br/>SSL Termination"]
        RATE_LIMIT["�️e Rate Limiting<br/>DDoS Protection<br/>CORS Handling"]
    end

    %% Core Application Services
    subgraph BACKEND["🏛️ TariffSheriff Backend Services"]
        AUTH_SVC["🔐 Authentication Service<br/>JWT + Spring Security<br/>User Management"]
        
        CALC_ENGINE["📊 Tariff Calculation Engine<br/>Core Business Logic<br/>HS Code Processing"]
        
        ADMIN_SVC["👨‍💼 Admin Service<br/>Rule Management<br/>CRUD Operations"]
        
        REC_ENGINE["🎯 Recommendation Engine<br/>Route Optimization<br/>Cost Analysis"]
        
        SIM_ENGINE["🔬 Simulation Engine<br/>Policy Modeling<br/>Scenario Analysis"]
        
        SWAGGER_UI["📚 API Documentation<br/>Swagger/OpenAPI<br/>Interactive Testing"]
    end

    %% Data Storage Layer
    subgraph DATA_LAYER["💾 Data Management Layer"]
        PRIMARY_DB[("🐘 PostgreSQL<br/>Primary Database<br/>Tariff Rules & Users")]
        
        CACHE_LAYER[("⚡ Redis Cache<br/>Session Storage<br/>Query Optimization")]
        
        FILE_STORAGE[("📁 AWS S3<br/>Document Storage<br/>Export Files")]
    end

    %% External Data Sources
    subgraph EXTERNAL["📡 External Data Sources"]
        WITS_API["🌍 WITS Database<br/>World Trade Statistics<br/>Historical Data"]
        
        HS_CODE_API["🏷️ HS Code Services<br/>Product Classification<br/>SimplyDuty/Mooah API"]
        
        REGIONAL_API["🌏 Regional Trade Portals<br/>Country-Specific Data<br/>Legal Citations"]
        
        TRADE_AGREEMENTS["📜 Trade Agreement APIs<br/>Bilateral Agreements<br/>MFN Rates"]
    end

    %% Infrastructure & DevOps
    subgraph INFRA["☁️ AWS Cloud Infrastructure"]
        CONTAINER_REGISTRY["🏪 AWS ECR<br/>Container Registry<br/>Image Management"]
        
        ORCHESTRATION["�t AWS ECS<br/>Container Orchestration<br/>Auto Scaling"]
        
        MONITORING["📊 CloudWatch<br/>Logging & Metrics<br/>Performance Monitoring"]
        
        CICD["🔄 GitHub Actions<br/>CI/CD Pipeline<br/>Automated Testing"]
    end

    %% Data Flow Connections
    WEB --> ALB
    MOBILE --> ALB
    API_CLIENT --> ALB
    
    ALB --> RATE_LIMIT
    RATE_LIMIT --> AUTH_SVC
    RATE_LIMIT --> CALC_ENGINE
    RATE_LIMIT --> ADMIN_SVC
    RATE_LIMIT --> REC_ENGINE
    RATE_LIMIT --> SIM_ENGINE
    
    AUTH_SVC --> PRIMARY_DB
    AUTH_SVC --> CACHE_LAYER
    
    CALC_ENGINE --> PRIMARY_DB
    CALC_ENGINE --> CACHE_LAYER
    CALC_ENGINE --> WITS_API
    CALC_ENGINE --> HS_CODE_API
    CALC_ENGINE --> REGIONAL_API
    CALC_ENGINE --> TRADE_AGREEMENTS
    
    ADMIN_SVC --> PRIMARY_DB
    ADMIN_SVC --> FILE_STORAGE
    
    REC_ENGINE --> PRIMARY_DB
    REC_ENGINE --> CACHE_LAYER
    REC_ENGINE --> CALC_ENGINE
    
    SIM_ENGINE --> PRIMARY_DB
    SIM_ENGINE --> CALC_ENGINE
    SIM_ENGINE --> FILE_STORAGE
    
    CICD --> CONTAINER_REGISTRY
    CONTAINER_REGISTRY --> ORCHESTRATION
    ORCHESTRATION --> MONITORING
    
    %% Styling
    classDef clientStyle fill:#e3f2fd,stroke:#1976d2,stroke-width:2px,color:#000
    classDef gatewayStyle fill:#f3e5f5,stroke:#7b1fa2,stroke-width:2px,color:#000
    classDef backendStyle fill:#e8f5e8,stroke:#388e3c,stroke-width:2px,color:#000
    classDef dataStyle fill:#fff3e0,stroke:#f57c00,stroke-width:2px,color:#000
    classDef externalStyle fill:#fce4ec,stroke:#c2185b,stroke-width:2px,color:#000
    classDef infraStyle fill:#f1f8e9,stroke:#689f38,stroke-width:2px,color:#000
    
    class WEB,MOBILE,API_CLIENT clientStyle
    class ALB,RATE_LIMIT gatewayStyle
    class AUTH_SVC,CALC_ENGINE,ADMIN_SVC,REC_ENGINE,SIM_ENGINE,SWAGGER_UI backendStyle
    class PRIMARY_DB,CACHE_LAYER,FILE_STORAGE dataStyle
    class WITS_API,HS_CODE_API,REGIONAL_API,TRADE_AGREEMENTS externalStyle
    class CONTAINER_REGISTRY,ORCHESTRATION,MONITORING,CICD infraStyle
```

## 🗂️ Project Structure

```
apps/
	backend/    # Spring Boot backend API
	frontend/   # React + Vite frontend UI
packages/     # Shared libraries (types, utils)
docs/         # Documentation
infrastructure/ # Docker, K8s, CI/CD configs
```

## 🔄 Data Flow & Business Logic

```mermaid
sequenceDiagram
    participant U as 👤 User
    participant F as 🌐 Frontend
    participant A as 🔐 Auth Service
    participant T as 📊 Tariff Engine
    participant D as 🗄️ Database
    participant E as 📡 External APIs
    participant R as 🎯 Recommender

    U->>F: Input tariff calculation request
    F->>A: Authenticate user
    A-->>F: JWT token
    
    F->>T: Calculate tariff (product, origin, destination, date)
    T->>D: Query existing tariff rules
    T->>E: Fetch HS codes & trade data
    E-->>T: Return classification & rates
    
    T->>T: Apply business logic:<br/>• MFN rates<br/>• Trade agreements<br/>• Certificates<br/>• Defense measures
    
    T-->>F: Return calculation result:<br/>• Total charges<br/>• Rule citations<br/>• Breakdown
    
    opt Recommendation Request
        F->>R: Request optimal routes
        R->>D: Query all country pairs
        R->>T: Calculate multiple scenarios
        R-->>F: Recommend best routes
    end
    
    F-->>U: Display results & recommendations
```

## 🧠 Core Business Logic

### Tariff Calculation Engine
- **Input Processing**: Product category (HS code), origin/destination countries, transaction details
- **Rule Matching**: Applies appropriate tariff rules based on trade agreements and validity periods
- **Rate Calculation**: Supports percentage-based and flat fee structures
- **Citation Generation**: Provides transparent rule references for compliance

### Key Features
- 🔍 **HS Code Resolution**: Automatic product classification using harmonized system codes
- 🌍 **Multi-Country Support**: Handles bilateral and multilateral trade agreements
- 📅 **Time-Sensitive Rules**: Applies correct rates based on transaction dates
- 🏆 **MFN Treatment**: Most-Favored-Nation rate calculations
- 📜 **Certificate Handling**: Processes origin certificates and special conditions

---

## 🛠️ Getting Started (Monorepo)

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

## 🛠️ Technology Stack

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

## 🤝 Contributing

- See the [`docs/`](docs/) folder for guidelines and architecture decisions.
- Use atomic, logical commits for all changes.

---


## 👥 Contributors

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

## 📄 License

This project is private and not yet licensed for public use.
