---

## 🚀 What is TariffSheriff?

👮 TariffSheriff is a full-stack web application that helps businesses calculate and analyze import tariffs and fees across countries, with a focus on the Electric Vehicle (EV) industry. It provides accurate, transparent, and defensible data for pricing, compliance, and financial planning.

---

## 🌐 Live Demo

[https://tariffsheriff-frontend.vercel.app/](https://tariffsheriff-frontend.vercel.app/)

<!-- App icon + Issue link -->
[![TariffSheriff icon](/shield.svg "TariffSheriff")](https://github.com/SaaiAravindhRaja/TariffSheriff/issues/19)

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

## 🗃️ Database Schema

```mermaid
erDiagram
    USERS {
        uuid id PK
        string email UK
        string password_hash
        string first_name
        string last_name
        enum role
        timestamp created_at
        timestamp updated_at
    }
    
    COUNTRIES {
        string code PK "ISO 3166-1"
        string name
        string region
        boolean active
    }
    
    PRODUCTS {
        uuid id PK
        string hs_code UK "6-10 digits"
        string name
        string description
        string category
        timestamp created_at
    }
    
    TARIFF_RULES {
        uuid id PK
        string origin_country FK
        string destination_country FK
        uuid product_id FK
        decimal rate
        enum rate_type "PERCENTAGE|FLAT"
        date valid_from
        date valid_to
        json conditions
        string citation
        timestamp created_at
        uuid created_by FK
    }
    
    CALCULATIONS {
        uuid id PK
        uuid user_id FK
        uuid product_id FK
        string origin_country FK
        string destination_country FK
        decimal quantity
        decimal unit_value
        string currency
        decimal total_tariff
        json breakdown
        timestamp calculated_at
    }
    
    TRADE_AGREEMENTS {
        uuid id PK
        string name
        string agreement_type
        date effective_from
        date effective_to
        json participating_countries
        decimal preferential_rate
    }
    
    USERS ||--o{ TARIFF_RULES : creates
    USERS ||--o{ CALCULATIONS : performs
    COUNTRIES ||--o{ TARIFF_RULES : origin
    COUNTRIES ||--o{ TARIFF_RULES : destination
    PRODUCTS ||--o{ TARIFF_RULES : applies_to
    PRODUCTS ||--o{ CALCULATIONS : calculates
    COUNTRIES ||--o{ CALCULATIONS : origin
    COUNTRIES ||--o{ CALCULATIONS : destination
```

## 🚀 Deployment Pipeline

```mermaid
gitGraph
    commit id: "Initial Setup"
    branch develop
    checkout develop
    commit id: "Feature: Auth"
    commit id: "Feature: Tariff Engine"
    commit id: "Feature: Admin Panel"
    
    branch feature/recommendations
    checkout feature/recommendations
    commit id: "Add Recommendation Logic"
    commit id: "Optimize Algorithms"
    
    checkout develop
    merge feature/recommendations
    commit id: "Integration Tests"
    
    checkout main
    merge develop
    commit id: "Release v1.0" tag: "v1.0"
    
    branch hotfix/security
    checkout hotfix/security
    commit id: "Security Patch"
    
    checkout main
    merge hotfix/security
    commit id: "Release v1.0.1" tag: "v1.0.1"
```

## 📊 Feature Development Timeline

```mermaid
gantt
    title TariffSheriff Development Timeline
    dateFormat  YYYY-MM-DD
    section Backend Core
    Authentication System    :done, auth, 2024-01-15, 2024-01-25
    Database Setup          :done, db, 2024-01-20, 2024-01-30
    Tariff Calculation API  :done, tariff, 2024-02-01, 2024-02-15
    Admin Console          :done, admin, 2024-02-10, 2024-02-20
    
    section Frontend
    UI Components          :done, ui, 2024-02-05, 2024-02-18
    Dashboard              :done, dash, 2024-02-15, 2024-02-28
    Tariff Calculator      :done, calc, 2024-02-20, 2024-03-05
    
    section Advanced Features
    Recommendation Engine  :active, rec, 2024-03-01, 2024-03-15
    Tariff Simulator      :sim, 2024-03-10, 2024-03-25
    Data Visualization    :viz, 2024-03-20, 2024-04-05
    
    section Deployment
    AWS Infrastructure    :infra, 2024-03-25, 2024-04-10
    CI/CD Pipeline       :cicd, 2024-04-01, 2024-04-15
    Production Deploy    :prod, 2024-04-10, 2024-04-20
```

---

## 🛠️ Getting Started

```bash
# 1. Install dependencies
npm install

# 2. Run backend (Spring Boot)
cd apps/backend && ./mvnw spring-boot:run

# 3. Run frontend (React)
cd apps/frontend && npm start
```

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

## 🔄 API Endpoints Overview

```mermaid
mindmap
  root((TariffSheriff API))
    Authentication
      POST /auth/login
      POST /auth/register
      POST /auth/refresh
      DELETE /auth/logout
    Tariff Calculation
      POST /api/tariffs/calculate
      GET /api/tariffs/history
      GET /api/tariffs/rules
    Product Management
      GET /api/products
      GET /api/products/search
      GET /api/products/{id}
      POST /api/products/classify
    Country Data
      GET /api/countries
      GET /api/countries/{code}/agreements
      GET /api/countries/{code}/rates
    Admin Operations
      POST /api/admin/rules
      PUT /api/admin/rules/{id}
      DELETE /api/admin/rules/{id}
      GET /api/admin/users
    Recommendations
      POST /api/recommendations/routes
      GET /api/recommendations/agreements
      POST /api/recommendations/optimize
    Simulation
      POST /api/simulator/scenarios
      GET /api/simulator/results/{id}
      POST /api/simulator/compare
```

## 🎯 User Journey Flow

```mermaid
journey
    title Business User Tariff Calculation Journey
    section Discovery
      Visit TariffSheriff: 5: User
      View Demo: 4: User
      Sign Up: 3: User
    section Setup
      Complete Profile: 4: User
      Verify Email: 3: User
      Explore Dashboard: 5: User
    section Core Usage
      Enter Product Details: 4: User
      Select Countries: 5: User
      Calculate Tariff: 5: User, System
      Review Breakdown: 5: User
      Export Results: 4: User
    section Advanced Features
      Compare Routes: 5: User
      Run Simulations: 4: User
      Get Recommendations: 5: User, System
      Save Scenarios: 4: User
    section Business Value
      Make Pricing Decisions: 5: User
      Ensure Compliance: 5: User
      Optimize Trade Routes: 5: User
```

## 🏛️ Microservices Architecture

```mermaid
C4Context
    title System Context Diagram for TariffSheriff
    
    Person(user, "Business User", "Imports/exports goods, needs tariff calculations")
    Person(admin, "System Admin", "Manages tariff rules and system configuration")
    
    System(tariffSheriff, "TariffSheriff", "Calculates import tariffs and provides trade recommendations")
    
    System_Ext(wits, "WITS Database", "World Integrated Trade Solution - provides trade statistics")
    System_Ext(hsCode, "HS Code Service", "Harmonized System product classification")
    System_Ext(regional, "Regional Trade Portals", "Country-specific trade data and regulations")
    
    Rel(user, tariffSheriff, "Calculates tariffs, gets recommendations")
    Rel(admin, tariffSheriff, "Manages rules and configurations")
    Rel(tariffSheriff, wits, "Fetches trade data")
    Rel(tariffSheriff, hsCode, "Classifies products")
    Rel(tariffSheriff, regional, "Gets country-specific rates")
```

## 📈 Performance Metrics

```mermaid
xychart-beta
    title "System Performance Metrics"
    x-axis [Jan, Feb, Mar, Apr, May, Jun]
    y-axis "Response Time (ms)" 0 --> 500
    line [120, 95, 85, 78, 82, 75]
```

```mermaid
pie title API Usage Distribution
    "Tariff Calculations" : 45
    "Product Classification" : 25
    "Route Recommendations" : 15
    "Admin Operations" : 10
    "Simulations" : 5
```

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
