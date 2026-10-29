# P2P Payment Ledger — Simplified UPI / Wallet

A peer-to-peer digital wallet application with ACH-style delayed settlement, fraud detection, and admin monitoring. Built with Spring Boot 3.x and React.

> **Note:** This is a simulated wallet. No real bank integration or actual financial transactions are involved.

---

## Technology Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security (JWT), Spring Scheduler |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Frontend | React 18, Vite |
| HTTP client | Axios |
| Charts | Recharts |
| Build | Maven (backend), npm (frontend) |

---

## Prerequisites

- Java 21 (JDK)
- Maven 3.9+
- Node.js 20+ and npm
- PostgreSQL 16
- Docker (optional, for TestContainers integration tests)

---

## Environment Configuration (PLANNED)

> Setup steps below are planned and will be verified during TASK-001 and TASK-002.

Create environment variables or an application-local.yml:

    DB_URL=jdbc:postgresql://localhost:5432/p2p_payment
    DB_USERNAME=your_db_user
    DB_PASSWORD=your_db_password
    JWT_SECRET=your_secret_key_min_32_chars
    JWT_EXPIRATION_MS=86400000
    ADMIN_DEFAULT_PASSWORD=changeme123

---

## Database Setup (PLANNED)

> Verified during TASK-002.

1. Create the PostgreSQL database: CREATE DATABASE p2p_payment;
2. Flyway migrations run automatically on startup and create all 14 tables.
3. A default admin user is seeded by migration V15. Change the default password before use.

---

## Running the Application (PLANNED)

> Steps will be verified during TASK-001 and TASK-003.

Backend:
    cd backend
    mvn spring-boot:run
Server starts on http://localhost:8080

Frontend:
    cd frontend
    npm install
    npm run dev
Dev server starts on http://localhost:5173

---

## Running Tests (PLANNED)

> Verified during TASK-030.

Backend:
    cd backend
    mvn test

Frontend:
    cd frontend
    npm test

---

## Key Features

- User registration + JWT login with 5-strike account lockout
- Wallet with ACTIVE/INACTIVE/FROZEN status management
- Multiple simulated bank accounts per user
- Manual wallet top-up from simulated account (atomic)
- Auto Reload: automatic top-up when balance falls below threshold
- P2P transfers with 3-business-day ACH delayed settlement
- Fraud controls: daily limit (Rs 50,000), duplicate detection (2 min), high-value flag (>= Rs 8,000)
- Transaction history with pagination, filters, and ledger entry detail
- In-app notifications for all major events
- Four scheduled background jobs (settlement, daily reset, auto-unlock, reconciliation)
- Admin panel: dashboard, user management, fraud review, settlement monitor, job logs

---

## Project Status

Current status: PLANNING — no application code yet.
See docs/progress.md for detailed tracking and docs/task-checklist.md for implementation progress.

---

## Documentation

| File | Description |
|---|---|
| docs/requirements-analysis.md | All functional requirements with acceptance criteria |
| docs/implementation-plan.md | 33-task implementation plan across 7 phases |
| docs/task-checklist.md | Ordered checklist of all tasks |
| docs/progress.md | Live progress tracker and session handoff |
| docs/architecture.md | System architecture and package structure |
| docs/database-design.md | All 14 tables with columns, constraints, and indexes |
| docs/api-specification.md | All API endpoints with request/response schemas |
| docs/testing-strategy.md | Complete testing plan |
| docs/decisions.md | Architecture decision log |
