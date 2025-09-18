# Getting Started

This page helps new users get TariffSheriff running locally and understand the high-level workflow.

Prerequisites
- Node.js (LTS) and npm or pnpm
- Java 17+ and Maven (for backend)
- Docker (optional for running services)

Quick start (development)

1. Clone the repository:

```bash
git clone https://github.com/SaaiAravindhRaja/TariffSheriff.git
cd TariffSheriff
```

2. Install frontend deps and start dev server:

```bash
npm --prefix apps/frontend install
npm --prefix apps/frontend run dev
```

3. Start backend (from `apps/backend`):

```bash
# from repo root
npm --prefix apps/backend install
# build/run with mvn or your IDE
```

4. Open the frontend at `http://localhost:5173` (Vite default) and the backend on its configured port.

For full dev environment instructions see `Installation` and `Architecture` pages.
