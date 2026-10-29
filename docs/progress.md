# Progress — P2P Payment Ledger

---

## Overall Status: IN PROGRESS

---

## Current Phase
**Phase:** PHASE-01 — Project Setup & Infrastructure (2 of 3 tasks complete)
**Current Task:** TASK-002 (next — Flyway DB migrations)
**Last Completed Task:** TASK-003 (frontend initialization)

---

## Phase & Task Status

| Phase | Status | Notes |
|---|---|---|
| PHASE-01: Project Setup | IN PROGRESS | TASK-001 and TASK-003 done; TASK-002 pending |
| PHASE-02: Auth & Wallet Core | NOT STARTED | Depends on PHASE-01 |
| PHASE-03: Simulated Accounts & Auto Reload | NOT STARTED | Depends on PHASE-02 |
| PHASE-04: P2P Transfers, Fraud & Settlement | NOT STARTED | Depends on PHASE-02 |
| PHASE-05: Admin Panel & Remaining Jobs | NOT STARTED | Depends on PHASE-04 |
| PHASE-06: Frontend | NOT STARTED | Depends on PHASE-01 (done) + PHASE-05 (backend) |
| PHASE-07: Testing & Hardening | NOT STARTED | Depends on all previous |

---

## Completed Tasks

- [x] TASK-001 — Backend Spring Boot project initialized. mvn compile and mvn test (1/0/0) — BUILD SUCCESS.
- [x] TASK-003 — Frontend React + Vite + Axios + React Router v7 + Recharts. npm run build (102 modules) — BUILD SUCCESS.

---

## In-Progress Tasks

None — TASK-001 and TASK-003 done; TASK-002 ready to start.

---

## Blocked Tasks

- B01: TASK-002 (Flyway migrations) requires PostgreSQL. DB must be running before starting TASK-002.
  - PostgreSQL 18 detected at C:\Program Files\PostgreSQL\18\bin\
  - Create database: CREATE DATABASE p2p_payment;
  - Set env vars: DB_URL, DB_USERNAME, DB_PASSWORD before running backend.

---

## Pending Tasks

TASK-002, TASK-004 through TASK-033 — see docs/task-checklist.md.

---

## Tests Executed

| Task | Test | Result | Date |
|---|---|---|---|
| TASK-001 | mvn test — Spring context loads (P2pPaymentApplicationTests) | PASSED: 1/0/0 | 2026-10-29 |
| TASK-003 | npm run build — Vite production build | PASSED: 102 modules, 0 errors | 2026-10-29 |

---

## Known Issues

- KI-01: Maven wrapper (`mvnw.cmd`) requires `java` from JDK 21 (Eclipse Adoptium). PATH must include the JDK bin directory. Alternative: use `java -cp .mvn/wrapper/maven-wrapper.jar ... MavenWrapperMain` directly.
- KI-02: React 19 + Vite 7 have a rollup resolution issue with `react-dom/client`. Resolved by using React 18 + React 18 types. react@18, react-dom@18 installed.
- KI-03: npm warns about react-dom@19.3.1 peer dep from recharts. Functional, non-blocking.

---

## Resolved Decisions (formerly Unresolved Questions)

| ID | Question | Status | Decision Ref |
|---|---|---|---|
| UQ-01 | How is ADMIN role assigned? | **RESOLVED** — AdminBootstrapService reads env vars, BCrypt-hashes, inserts admin on first startup only | DEC-007 |
| UQ-02 | "3 business days" — timezone and holiday calendar? | **DEFAULT** (awaiting confirmation) — UTC, weekdays only | DEC-005 |
| UQ-03 | PIN storage hashed or plain? | **DEFAULT** (awaiting confirmation) — BCrypt hash | DEC-006 |
| UQ-04 | "Today's spending" based on initiated or settled? | **DEFAULT** (awaiting confirmation) — initiated amounts | DEC-009 |
| UQ-05 | Can a user send to themselves? | **RESOLVED** — blocked, HTTP 422 before balance changes | DEC-011 |
| UQ-06 | Pagination defaults? | **DEFAULT** (awaiting confirmation) — page size 20, max 100 | — |
| UQ-07 | Auto Reload duplicate prevention mechanism? | **DEFAULT** (awaiting confirmation) — DB-level `reload_in_progress` flag | DEC-004 |
| UQ-08 | Simulated account traceability — separate table? | **DEFAULT** (awaiting confirmation) — 14th table `simulated_account_transactions` | DEC-001 |
| UQ-09 | Transaction types in transactions table? | **DEFAULT** (awaiting confirmation) — P2P_TRANSFER, WALLET_TOP_UP, AUTO_RELOAD | DEC-013 |
| UQ-10 | Demo funds in transactions table? | **RESOLVED** — NOT in main transactions; recorded in `simulated_account_transactions` + `audit_log` | DEC-012 |

**Defaults awaiting owner confirmation:** UQ-02, UQ-03, UQ-04, UQ-06, UQ-07, UQ-08, UQ-09.
See docs/decisions.md for full context on each default.

---

## Important Implementation Decisions

| Decision | Description |
|---|---|
| D-01 | 14 database tables (13 required + simulated_account_transactions for traceability) |
| D-02 | Flyway for DB migrations, one migration file per table |
| D-03 | Optimistic locking (`version` column) on wallets and simulated_bank_accounts |
| D-04 | `reload_in_progress` flag on auto_reload_settings for concurrency safety |
| D-05 | Request-level idempotency via `request_id` unique constraint |
| D-06 | Business days = weekdays only (no holiday calendar) |
| D-07 | Frontend: React 18 + Vite 7 + Recharts + React Router v7 + Axios |
| D-08 | PIN and password both BCrypt-hashed |
| D-09 | Admin created via AdminBootstrapService (env vars, idempotent) — no V15 migration |
| D-10 | Self-transfers blocked with HTTP 422 before balance changes |
| D-11 | Demo funds NOT in main transactions table; in simulated_account_transactions + audit_log |
| D-12 | PIN reset explicitly out of scope for this sprint |

---

## Database Migration Status

| Migration | File | Status |
|---|---|---|
| V1 | users | NOT CREATED |
| V2 | login_attempts | NOT CREATED |
| V3 | wallets | NOT CREATED |
| V4 | transactions | NOT CREATED |
| V5 | ledger_entries | NOT CREATED |
| V6 | fraud_alerts | NOT CREATED |
| V7 | notifications | NOT CREATED |
| V8 | audit_log | NOT CREATED |
| V9 | scheduled_job_log | NOT CREATED |
| V10 | daily_summary | NOT CREATED |
| V11 | settlement_batches | NOT CREATED |
| V12 | simulated_bank_accounts | NOT CREATED |
| V13 | auto_reload_settings | NOT CREATED |
| V14 | simulated_account_transactions | NOT CREATED |
| ~~V15~~ | ~~seed admin user~~ — REMOVED (replaced by AdminBootstrapService, DEC-007) | N/A |

---

## Files Changed in Latest Task (TASK-001 + TASK-003)

**TASK-001 (Backend):**
- backend/pom.xml (Spring Boot 3.4.1, Java 21, all required deps + H2 for tests)
- backend/src/main/java/com/p2ppayment/P2pPaymentApplication.java
- backend/src/main/resources/application.yml
- backend/src/main/resources/application-test.yml
- backend/src/test/java/com/p2ppayment/P2pPaymentApplicationTests.java
- backend/.mvn/wrapper/maven-wrapper.properties
- backend/.mvn/wrapper/maven-wrapper.jar (downloaded)
- backend/mvnw.cmd
- .env.example (new — documents required env vars)

**TASK-003 (Frontend):**
- frontend/package.json (react@18, react-dom@18, react-router-dom@7, recharts, axios)
- frontend/vite.config.js (proxy /api to localhost:8080, optimizeDeps)
- frontend/src/App.jsx (router skeleton with protected routes)
- frontend/src/main.jsx (unchanged from Vite scaffold)
- frontend/src/context/AuthContext.jsx
- frontend/src/context/NotificationContext.jsx
- frontend/src/pages/auth/LoginPage.jsx
- frontend/src/pages/auth/RegisterPage.jsx
- frontend/src/api/axiosClient.js
- frontend/src/api/authApi.js (stub)
- frontend/src/api/walletApi.js (stub)
- frontend/src/api/transactionApi.js
- frontend/src/api/bankAccountApi.js
- frontend/src/api/notificationApi.js
- frontend/src/api/adminApi.js
- frontend/src/utils/formatCurrency.js
- frontend/src/utils/formatDate.js
- frontend/src/utils/constants.js

---

## Next Task

**TASK-002** — Create all 14 database tables via Flyway migrations.

**Prerequisites before starting TASK-002:**
1. PostgreSQL must be running locally.
2. Create database: `psql -U postgres -c "CREATE DATABASE p2p_payment;"`
3. Set environment variables: DB_URL, DB_USERNAME, DB_PASSWORD.
4. Review docs/database-design.md for all table schemas.

---

## Handoff Notes

This file is the primary handoff document. Any AI session resuming work should:
1. Read this file first to determine the current task.
2. Read `docs/task-checklist.md` to see exactly which tasks are complete.
3. Read the relevant task in `docs/implementation-plan.md` for the current task.
4. Inspect actual source code files before making assumptions.
5. Run `java -cp backend/.mvn/wrapper/maven-wrapper.jar "-Dmaven.multiModuleProjectDirectory=backend" org.apache.maven.wrapper.MavenWrapperMain test` (backend) and `cd frontend && npm run build` (frontend) to verify current state before continuing.
