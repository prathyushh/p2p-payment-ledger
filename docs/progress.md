# Progress — P2P Payment Ledger

---

## Overall Status: IN PROGRESS

---

## Current Phase
**Phase:** PHASE-01 — Project Setup & Infrastructure — COMPLETE (3/3 tasks done)
**Current Task:** None — PHASE-01 complete; awaiting review before starting PHASE-02
**Last Completed Task:** TASK-002 — Flyway database migrations

---

## Phase & Task Status

| Phase | Status | Notes |
|---|---|---|
| PHASE-01: Project Setup | COMPLETE | All 3 tasks done and verified |
| PHASE-02: Auth & Wallet Core | NOT STARTED | Depends on PHASE-01 (done) |
| PHASE-03: Simulated Accounts & Auto Reload | NOT STARTED | Depends on PHASE-02 |
| PHASE-04: P2P Transfers, Fraud & Settlement | NOT STARTED | Depends on PHASE-02 |
| PHASE-05: Admin Panel & Remaining Jobs | NOT STARTED | Depends on PHASE-04 |
| PHASE-06: Frontend | NOT STARTED | Depends on PHASE-05 |
| PHASE-07: Testing & Hardening | NOT STARTED | Depends on all previous |

---

## Completed Tasks

- [x] TASK-001 — Backend Spring Boot 3.4.1 project initialized. mvn test (1/0/0) — BUILD SUCCESS.
- [x] TASK-002 — 14 Flyway migrations executed. All tables, constraints, indexes verified. mvn test still passes.
- [x] TASK-003 — Frontend React 18 + Vite 7 + Axios + React Router v7 + Recharts. npm run build (102 modules) — BUILD SUCCESS.

---

## In-Progress Tasks

None — PHASE-01 complete.

---

## Blocked Tasks

None.

---

## Tests Executed

| Task | Test | Result | Date |
|---|---|---|---|
| TASK-001 | mvn test — Spring context loads (P2pPaymentApplicationTests) | PASSED: 1/0/0 | 2026-10-29 |
| TASK-002 | flyway:migrate — 14 migrations applied to p2p_payment | PASSED: 14/14 tables | 2026-10-29 |
| TASK-002 | mvn test — context still loads after migrations added to classpath | PASSED: 1/0/0 | 2026-10-29 |
| TASK-003 | npm run build — Vite production build | PASSED: 102 modules, 0 errors | 2026-10-29 |

---

## Known Issues

- KI-01: Spring Boot does not auto-load `.env` files. Use `backend/run.ps1` for local development (it reads `backend/.env` and exports env vars before starting the app).
- KI-02: Maven wrapper (`mvnw.cmd`) is unreliable on this machine due to PATH issues. Use the Java invocation directly: `java -cp backend/.mvn/wrapper/maven-wrapper.jar "-Dmaven.multiModuleProjectDirectory=backend" org.apache.maven.wrapper.MavenWrapperMain <goal>`.
- KI-03: Flyway warns that PostgreSQL 18 is newer than its tested maximum (17). Migrations run successfully; this is a documentation warning only.
- KI-04: React 18 is used (not 19) due to a Vite 7 + rollup resolution issue with react-dom/client on React 19.

---

## Resolved Decisions

| ID | Question | Status | Decision Ref |
|---|---|---|---|
| UQ-01 | Admin bootstrap | **RESOLVED** — AdminBootstrapService (env vars, BCrypt, idempotent) | DEC-007 |
| UQ-02 | Business day timezone/holidays | **DEFAULT** — UTC, weekdays only | DEC-005 |
| UQ-03 | PIN storage | **DEFAULT** — BCrypt hash | DEC-006 |
| UQ-04 | Today's spending basis | **DEFAULT** — initiated amounts | DEC-009 |
| UQ-05 | Self-transfers | **RESOLVED** — blocked, HTTP 422 | DEC-011 |
| UQ-06 | Pagination defaults | **DEFAULT** — page 20, max 100 | — |
| UQ-07 | Auto Reload concurrency | **DEFAULT** — DB-level reload_in_progress flag | DEC-004 |
| UQ-08 | Simulated account traceability | **DEFAULT** — 14th table | DEC-001 |
| UQ-09 | Transaction types | **DEFAULT** — P2P_TRANSFER, WALLET_TOP_UP, AUTO_RELOAD | DEC-013 |
| UQ-10 | Demo funds in transactions table | **RESOLVED** — NOT in transactions; simulated_account_transactions + audit_log | DEC-012 |

---

## Important Implementation Decisions

| Decision | Description |
|---|---|
| D-01 | 14 database tables (13 required + simulated_account_transactions) |
| D-02 | Flyway migrations V1–V14; no V15 (admin bootstrap is application-level) |
| D-03 | Optimistic locking (version column) on wallets and simulated_bank_accounts |
| D-04 | reload_in_progress DB flag for Auto Reload concurrency |
| D-05 | request_id unique constraint for idempotency |
| D-06 | Business days = weekdays only (UTC) |
| D-07 | Frontend: React 18 + Vite 7 + React Router v7 + Recharts + Axios |
| D-08 | PIN and password both BCrypt-hashed |
| D-09 | Admin created via AdminBootstrapService at startup (not migration) |
| D-10 | Self-transfers blocked with HTTP 422 before balance changes |
| D-11 | Demo funds: simulated_account_transactions + audit_log only |
| D-12 | PIN reset out of scope for this sprint |

---

## Database Migration Status

| Migration | Table | Status |
|---|---|---|
| V1 | users | APPLIED |
| V2 | login_attempts | APPLIED |
| V3 | wallets | APPLIED |
| V4 | transactions | APPLIED |
| V5 | ledger_entries | APPLIED |
| V6 | fraud_alerts | APPLIED |
| V7 | notifications | APPLIED |
| V8 | audit_log | APPLIED |
| V9 | scheduled_job_log | APPLIED |
| V10 | daily_summary | APPLIED |
| V11 | settlement_batches | APPLIED |
| V12 | simulated_bank_accounts + transactions.simulated_account_id FK | APPLIED |
| V13 | auto_reload_settings | APPLIED |
| V14 | simulated_account_transactions | APPLIED |
| V15 | ~~admin seed~~ | REMOVED — handled by AdminBootstrapService |

---

## Environment & Configuration Notes

- `backend/.env` is excluded from Git (confirmed via `git check-ignore`).
- `.env.example` is tracked by Git (`.env.example` removed from `.gitignore`).
- Spring Boot reads credentials from OS environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD`).
- Local dev: run `backend/run.ps1` from the project root — it loads `backend/.env` and starts the app.
- IDE users: set the 7 environment variables in the run configuration before launching `P2pPaymentApplication`.

---

## Files Changed in TASK-002

- backend/src/main/resources/db/migration/V1__create_users.sql (NEW)
- backend/src/main/resources/db/migration/V2__create_login_attempts.sql (NEW)
- backend/src/main/resources/db/migration/V3__create_wallets.sql (NEW)
- backend/src/main/resources/db/migration/V4__create_transactions.sql (NEW)
- backend/src/main/resources/db/migration/V5__create_ledger_entries.sql (NEW)
- backend/src/main/resources/db/migration/V6__create_fraud_alerts.sql (NEW)
- backend/src/main/resources/db/migration/V7__create_notifications.sql (NEW)
- backend/src/main/resources/db/migration/V8__create_audit_log.sql (NEW)
- backend/src/main/resources/db/migration/V9__create_scheduled_job_log.sql (NEW)
- backend/src/main/resources/db/migration/V10__create_daily_summary.sql (NEW)
- backend/src/main/resources/db/migration/V11__create_settlement_batches.sql (NEW)
- backend/src/main/resources/db/migration/V12__create_simulated_bank_accounts.sql (NEW)
- backend/src/main/resources/db/migration/V13__create_auto_reload_settings.sql (NEW)
- backend/src/main/resources/db/migration/V14__create_simulated_account_transactions.sql (NEW)
- backend/run.ps1 (NEW — local dev launcher that loads .env)
- backend/src/main/resources/application.yml (UPDATED — added env loading documentation comment)
- .gitignore (UPDATED — removed .env.example from ignored list)
- docs/task-checklist.md (REBUILT — was corrupted; now clean)
- docs/database-design.md (UPDATED — status changed to IMPLEMENTED)
- docs/progress.md (UPDATED — this file)

---

## Next Task

**TASK-004** — Implement user registration, JWT login, Spring Security config, and AdminBootstrapService.

**Why TASK-004 is next:**
- PHASE-01 is complete (all 3 setup tasks done and verified).
- TASK-004 is the first task in PHASE-02 and has no unresolved dependencies.
- It produces the first working API endpoints and the JWT infrastructure that all subsequent tasks depend on.
- AdminBootstrapService (DEC-007) is implemented here so the admin user can be created before testing admin endpoints.

**Awaiting owner review before starting TASK-004.**

---

## Handoff Notes

To resume this project in a new session:
1. Read this file first.
2. Read `docs/task-checklist.md` for task completion state.
3. Read the relevant TASK entry in `docs/implementation-plan.md`.
4. Inspect actual source files — do not trust this file alone if code contradicts it.
5. Run: `java -cp backend/.mvn/wrapper/maven-wrapper.jar "-Dmaven.multiModuleProjectDirectory=backend" org.apache.maven.wrapper.MavenWrapperMain test` to verify backend state.
6. Run: `cd frontend && npm run build` to verify frontend state.
7. To verify DB migrations: run `flyway:migrate` with explicit `-Dflyway.url`, `-Dflyway.user`, `-Dflyway.password` (load from backend/.env first).