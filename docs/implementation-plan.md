# Implementation Plan — P2P Payment Ledger

**Status:** PLANNED  
**Timeline:** 1 week  
**Total Phases:** 7

---

## Phase Overview

| Phase | Name | Est. Duration |
|---|---|---|
| PHASE-01 | Project Setup & Infrastructure | Day 1 (half) |
| PHASE-02 | Auth, Users & Wallet Core | Day 1–2 |
| PHASE-03 | Simulated Accounts & Wallet Top-Up | Day 2–3 |
| PHASE-04 | P2P Transfers, Fraud & Settlement Job | Day 3–4 |
| PHASE-05 | Admin Panel & Scheduled Jobs | Day 4–5 |
| PHASE-06 | Frontend | Day 5–6 |
| PHASE-07 | Integration, Testing & Hardening | Day 7 |

**Critical Path:** PHASE-01 → PHASE-02 → PHASE-04 → PHASE-05 → PHASE-06 → PHASE-07

---

## PHASE-01: Project Setup & Infrastructure

### TASK-001 — Initialize Backend Project
- **Objective:** Create Spring Boot 3.x Maven project with all required dependencies.
- **Requirements covered:** All (prerequisite)
- **Dependencies:** None
- **Files affected:** `backend/pom.xml`, `backend/src/main/resources/application.yml`, `backend/src/main/java/com/p2ppayment/P2pPaymentApplication.java`
- **Implementation Steps:**
  1. Create Maven project with Spring Initializr (or manually).
  2. Add dependencies: Spring Web, Spring Data JPA, Spring Security, Spring Scheduler, PostgreSQL Driver, Flyway, Lombok, jjwt (JWT library), Validation.
  3. Configure `application.yml` with DB connection, JWT secret, scheduler settings.
  4. Configure Flyway migration directory.
  5. Create main application class.
- **Acceptance Criteria:** `mvn spring-boot:run` starts without errors.
- **Tests:** Application context loads (Spring Boot test).
- **Definition of Done:** Server starts, Flyway runs (empty migration), health endpoint responds.

### TASK-002 — Database Schema Migrations (Flyway)
- **Objective:** Create all 14 database tables via Flyway migrations.
- **Requirements covered:** REQ-005, REQ-007, REQ-010, REQ-012
- **Dependencies:** TASK-001
- **Files affected:** `backend/src/main/resources/db/migration/V1__*.sql` through `V14__*.sql`
- **Implementation Steps:**
  1. V1: Create `users` table.
  2. V2: Create `login_attempts` table.
  3. V3: Create `wallets` table with `balance >= 0` constraint and `version` column.
  4. V4: Create `transactions` table with `request_id` unique constraint.
  5. V5: Create `ledger_entries` table.
  6. V6: Create `fraud_alerts` table.
  7. V7: Create `notifications` table.
  8. V8: Create `audit_log` table.
  9. V9: Create `scheduled_job_log` table.
  10. V10: Create `daily_summary` table.
  11. V11: Create `settlement_batches` table.
  12. V12: Create `simulated_bank_accounts` table with `simulated_balance >= 0` and `version`.
  13. V13: Create `auto_reload_settings` table.
  14. V14: Create `simulated_account_transactions` table.
  15. V15: Seed default ADMIN user.
- **Acceptance Criteria:** `mvn flyway:migrate` completes successfully; all 14 tables exist in DB.
- **Tests:** DB constraints test (balance negative insert fails).
- **Definition of Done:** All tables created, constraints verified.

### TASK-003 — Initialize Frontend Project
- **Objective:** Create React + Vite project with base dependencies.
- **Requirements covered:** All frontend (prerequisite)
- **Dependencies:** None (parallel with TASK-001)
- **Files affected:** `frontend/package.json`, `frontend/vite.config.js`, `frontend/src/main.jsx`, `frontend/src/App.jsx`
- **Implementation Steps:**
  1. `npm create vite@latest frontend -- --template react`
  2. Install Axios, React Router v6, Recharts.
  3. Set up proxy to backend in vite.config.js.
  4. Create base App.jsx with router skeleton.
  5. Create `src/api/axiosClient.js` with JWT interceptor.
  6. Create `src/context/AuthContext.jsx`.
- **Acceptance Criteria:** `npm run dev` starts frontend dev server.
- **Tests:** App renders without errors.
- **Definition of Done:** Dev server running, axios client configured.

---

## PHASE-02: Auth, Users & Wallet Core

### TASK-004 — User Registration & JWT Login
- **Objective:** Implement registration, login, JWT generation, and Spring Security config.
- **Requirements covered:** REQ-001, REQ-002, REQ-004
- **Dependencies:** TASK-001, TASK-002
- **Files affected:** `SecurityConfig.java`, `JwtTokenProvider.java`, `JwtAuthenticationFilter.java`, `AuthController.java`, `AuthService.java`, `UserRepository.java`, `User.java`, `Wallet.java`, DTOs
- **Implementation Steps:**
  1. Create `User`, `Wallet` JPA entities.
  2. Create `UserRepository`, `WalletRepository`.
  3. Implement `UserDetailsServiceImpl`.
  4. Configure Spring Security: disable CSRF, stateless session, JWT filter.
  5. Implement `JwtTokenProvider` (generate + validate).
  6. Implement `AuthService.register()`: hash password + PIN, create user, create wallet atomically.
  7. Implement `AuthService.login()`: validate credentials, return JWT.
  8. Create `AuthController` with `/register` and `/login`.
  9. Create `/api/auth/me` endpoint.
- **Acceptance Criteria:** 
  - POST /api/auth/register returns 201 with JWT.
  - POST /api/auth/login returns JWT.
  - Wallet created with ₹0 balance.
  - Duplicate email/phone returns 409.
- **Tests:** Unit tests for AuthService. Integration test for registration flow.
- **Definition of Done:** Registration and login work end-to-end.

### TASK-005 — Login Attempt Tracking & Account Lock
- **Objective:** Track failed logins and lock accounts.
- **Requirements covered:** REQ-002, REQ-003
- **Dependencies:** TASK-004
- **Files affected:** `AuthService.java`, `LoginAttempt.java`, `LoginAttemptRepository.java`
- **Implementation Steps:**
  1. Create `LoginAttempt` entity and repository.
  2. In `AuthService.login()`: on failure, save LoginAttempt, count recent failures, lock if >= 5.
  3. On success: clear failure count (or just rely on recent-count query).
  4. Return 423 with `locked_until` when locked account attempts login.
  5. Create "account locked" notification.
- **Acceptance Criteria:** Account locked after 5 failures; 423 returned on login attempt.
- **Tests:** Unit test 5-failure lockout. Integration test lock → wait → try.
- **Definition of Done:** Lock/unlock flow working.

### TASK-006 — Wallet Status Management
- **Objective:** User can deactivate/reactivate wallet; admin can freeze/unfreeze.
- **Requirements covered:** REQ-005
- **Dependencies:** TASK-004
- **Files affected:** `WalletController.java`, `WalletService.java`, `AuditService.java`
- **Implementation Steps:**
  1. Implement `WalletService.deactivate()` and `reactivate()`.
  2. Implement admin `freeze()` and `unfreeze()` (writes to audit log).
  3. Create `WalletController` with POST endpoints.
  4. Admin endpoints protected with `@PreAuthorize("hasRole('ADMIN')")`.
- **Acceptance Criteria:** Status changes work; admin actions written to audit_log.
- **Tests:** Unit tests for all status transitions. Security test: USER cannot freeze.
- **Definition of Done:** All status transitions correct; audit log populated.

### TASK-007 — Wallet Balance & Today's Spending
- **Objective:** GET /api/wallet returns balance, status, and daily spending.
- **Requirements covered:** REQ-006
- **Dependencies:** TASK-004
- **Files affected:** `WalletController.java`, `WalletService.java`
- **Implementation Steps:**
  1. Implement `GET /api/wallet` endpoint.
  2. Return balance, status, daily_spent, daily_remaining.
- **Acceptance Criteria:** Returns correct figures.
- **Tests:** Unit test response DTO.
- **Definition of Done:** Endpoint returns accurate data.

---

## PHASE-03: Simulated Accounts, Top-Up & Auto Reload

### TASK-008 — Simulated Bank Account CRUD
- **Objective:** Users can create, view, and deactivate simulated bank accounts.
- **Requirements covered:** REQ-007, REQ-008
- **Dependencies:** TASK-004
- **Files affected:** `SimulatedBankAccount.java`, `SimulatedBankAccountRepository.java`, `BankAccountService.java`, `BankAccountController.java`
- **Implementation Steps:**
  1. Create `SimulatedBankAccount` entity with optimistic lock.
  2. Create repository.
  3. Implement `BankAccountService.create()`: generate unique demo account identifier.
  4. Implement `list()`, `deactivate()`.
  5. Implement Add Demo Funds: validate amount, update balance, create `SimulatedAccountTransaction` record, send notification.
  6. Create `BankAccountController`.
- **Acceptance Criteria:** CRUD works; balance never goes negative; user can't access others' accounts.
- **Tests:** Unit tests for all operations. Balance constraint test.
- **Definition of Done:** All endpoints working with correct ownership checks.

### TASK-009 — Manual Wallet Top-Up
- **Objective:** User adds money from simulated account to wallet atomically.
- **Requirements covered:** REQ-009
- **Dependencies:** TASK-008
- **Files affected:** `WalletService.java`, `TransactionService.java`, `LedgerEntry.java`
- **Implementation Steps:**
  1. Implement `WalletService.topUp()` in a `@Transactional` method.
  2. Lock simulated account row (`SELECT FOR UPDATE`).
  3. Validate sufficient funds.
  4. Deduct from simulated account, credit wallet.
  5. Create WALLET_TOP_UP transaction record.
  6. Create CREDIT ledger entry.
  7. Create SimulatedAccountTransaction record.
  8. Send notification.
  9. Trigger Auto Reload check (see TASK-011) — note top-up increases balance, no reload needed but this establishes the pattern.
- **Acceptance Criteria:** Atomic: both or neither. Insufficient funds returns 422.
- **Tests:** Integration test: top-up then verify both balances. Rollback test.
- **Definition of Done:** Atomic top-up working with ledger entries.

### TASK-010 — Auto Reload Configuration
- **Objective:** Users configure Auto Reload settings.
- **Requirements covered:** REQ-010
- **Dependencies:** TASK-008
- **Files affected:** `AutoReloadSettings.java`, `AutoReloadSettingsRepository.java`, `AutoReloadController.java`
- **Implementation Steps:**
  1. Create entity and repository.
  2. Implement GET and PUT endpoints.
  3. Validate: if enabled, simulated_account_id, threshold, and reload_amount must be provided and > 0.
  4. Wallet must be ACTIVE to enable.
- **Acceptance Criteria:** Settings saved/retrieved. Validation works.
- **Tests:** Unit tests for validation logic.
- **Definition of Done:** Configuration endpoints working.

### TASK-011 — Auto Reload Behavior
- **Objective:** Implement Auto Reload trigger logic, concurrency guard, and no-recursion guarantee.
- **Requirements covered:** REQ-011
- **Dependencies:** TASK-010
- **Files affected:** `AutoReloadService.java`, called from `WalletService` after every balance decrease
- **Implementation Steps:**
  1. In `AutoReloadService.attemptReload(walletId)`:
     a. Load settings, check enabled.
     b. Check wallet ACTIVE.
     c. Check balance < threshold.
     d. Try to acquire lock: `UPDATE auto_reload_settings SET reload_in_progress = TRUE WHERE wallet_id = ? AND reload_in_progress = FALSE` (check rows affected = 1).
     e. If lock acquired: deduct simulated account, credit wallet using a flag to skip Auto Reload check (prevent recursion).
     f. Create AUTO_RELOAD transaction, ledger entries, SimulatedAccountTransaction.
     g. Release lock (set reload_in_progress = FALSE).
  2. Call `AutoReloadService.attemptReload()` after every wallet balance decrease in `WalletService` and `TransactionService`.
  3. Never call after a reload credit.
- **Acceptance Criteria:** Fires at most once per trigger event. Does not recurse. Failed reload does not affect triggering transaction.
- **Tests:** Concurrency test (10 concurrent triggers → 1 reload). Recursion test. Inactive wallet test.
- **Definition of Done:** All auto reload behaviors verified.

---

## PHASE-04: P2P Transfers, Fraud & Settlement

### TASK-012 — Send Money (P2P Transfer)
- **Objective:** Implement the full send money flow with idempotency and PIN validation.
- **Requirements covered:** REQ-012
- **Dependencies:** TASK-004, TASK-005
- **Files affected:** `TransactionController.java`, `TransactionService.java`, `Transaction.java`, `LedgerEntry.java`, `BusinessDayCalculator.java`
- **Implementation Steps:**
  1. Implement `BusinessDayCalculator.addBusinessDays(date, 3)`.
  2. Implement idempotency check: `SELECT FROM transactions WHERE request_id = ?`.
  3. Validate PIN: hash input and compare with stored hash.
  4. Check sender wallet ACTIVE.
  5. Check sufficient balance.
  6. Deduct sender balance + increment daily_spent (atomic: `UPDATE wallets SET balance = balance - ?, daily_spent = daily_spent + ? WHERE id = ? AND balance >= ? AND daily_spent + ? <= 50000`).
  7. Create DEBIT ledger entry.
  8. Create PENDING transaction with settlement_due_date.
  9. Send "transfer initiated" notification to sender.
  10. Trigger fraud checks (TASK-013).
  11. Trigger Auto Reload check for sender.
- **Acceptance Criteria:** Sender debited immediately. PENDING status. settlement_due_date = +3 business days. Duplicate request_id returns same result.
- **Tests:** Full integration test. Idempotency test. Invalid PIN test.
- **Definition of Done:** P2P send working with correct ledger and notifications.

### TASK-013 — Fraud Detection
- **Objective:** Implement three fraud checks: daily limit, duplicate, high-value.
- **Requirements covered:** REQ-013a, REQ-013b, REQ-013c
- **Dependencies:** TASK-012
- **Files affected:** `FraudService.java`, `FraudAlert.java`, `FraudAlertRepository.java`, `NotificationService.java`
- **Implementation Steps:**
  1. `FraudService.checkDailyLimit(senderId, amount)`: query sum of today's sent amounts, reject if + amount > 50,000. Create DAILY_LIMIT_EXCEEDED alert. Notify sender.
  2. `FraudService.checkDuplicate(senderId, receiverId, amount)`: query transactions with same parameters in last 2 min. If found, block. Create DUPLICATE_TRANSACTION alert.
  3. `FraudService.checkHighValue(transactionId, amount)`: if amount >= 8,000, create HIGH_VALUE fraud alert. Notify admin.
  4. Run all three checks in order before committing transfer in `TransactionService`.
  5. Blocks (daily limit, duplicate) prevent transaction. High-value creates alert but does not block.
- **Acceptance Criteria:** All three checks working with correct alerts and notifications.
- **Tests:** Unit tests for each check with edge cases.
- **Definition of Done:** Fraud pipeline complete and tested.

### TASK-014 — ACH Settlement Scheduler
- **Objective:** Background job that settles due PENDING transactions every hour.
- **Requirements covered:** REQ-016a
- **Dependencies:** TASK-012, TASK-006
- **Files affected:** `AchSettlementJob.java`, `SettlementService.java`, `ScheduledJobLog.java`, `SettlementBatch.java`
- **Implementation Steps:**
  1. `@Scheduled(cron = "0 0 * * * *")` — every hour.
  2. Create ScheduledJobLog record (status = RUNNING).
  3. Query PENDING transactions with `settlement_due_date <= today`.
  4. For each (in its own nested transaction):
     a. Check receiver wallet status.
     b. If INACTIVE/FROZEN: status → DECLINED, refund sender, notify sender.
     c. If ACTIVE: credit receiver, create CREDIT ledger entry, status → SUCCESS, notify both.
  5. Update ScheduledJobLog (SUCCESS/FAILED, counts).
  6. Create SettlementBatch record.
- **Acceptance Criteria:** Correct transactions settled. Failures don't abort batch. Log created.
- **Tests:** Unit test selection query. Integration test: settle due transaction, skip future one.
- **Definition of Done:** Job runs and settles correctly.

### TASK-015 — Admin Fraud Review (Block/Dismiss)
- **Objective:** Admin can block or dismiss HIGH_VALUE fraud alerts.
- **Requirements covered:** REQ-013c, REQ-017e
- **Dependencies:** TASK-013
- **Files affected:** `AdminFraudController.java`, `FraudService.java`
- **Implementation Steps:**
  1. Block: mark transaction FAILED, refund sender, notify both, mark alert BLOCKED, write audit log.
  2. Dismiss: mark alert DISMISSED, write audit log.
  3. Read-only guard: if transaction already settled, return 409.
  4. Admin fraud alert notification already in TASK-013.
- **Acceptance Criteria:** Block refunds sender and marks transaction FAILED. Dismiss leaves transaction PENDING. Settled alerts are read-only.
- **Tests:** Integration tests for both actions.
- **Definition of Done:** Both actions work with correct audit log entries.

### TASK-016 — Transaction History & Stats
- **Objective:** Implement transaction list, mini-statement, detail view, and stats endpoints.
- **Requirements covered:** REQ-014
- **Dependencies:** TASK-012
- **Files affected:** `TransactionController.java`, `TransactionService.java`
- **Implementation Steps:**
  1. Implement paginated GET with filters (type, status, date range).
  2. Implement mini-statement (last 10).
  3. Implement detail endpoint with ledger entries.
  4. Implement stats (total sent, received, this month's count).
- **Acceptance Criteria:** Pagination, filters, and detail view all work.
- **Tests:** Pagination tests with various filter combinations.
- **Definition of Done:** All history endpoints working.

---

## PHASE-05: Remaining Jobs, Admin Panel & Notifications

### TASK-017 — Remaining Scheduled Jobs
- **Objective:** Implement Daily Limit Reset, Account Auto-Unlock, and Daily Summary/Reconciliation jobs.
- **Requirements covered:** REQ-016b, REQ-016c, REQ-016d
- **Dependencies:** TASK-005, TASK-014
- **Files affected:** `DailyLimitResetJob.java`, `AccountAutoUnlockJob.java`, `DailySummaryJob.java`
- **Implementation Steps:**
  1. `DailyLimitResetJob` (`@Scheduled(cron = "0 0 0 * * *")`): `UPDATE wallets SET daily_spent = 0`.
  2. `AccountAutoUnlockJob` (`@Scheduled(cron = "0 */5 * * * *")`): find locked accounts past `locked_until`, unlock, send notification.
  3. `DailySummaryJob` (`@Scheduled(cron = "0 0 23 * * *")`): aggregate day's stats, run reconciliation, create `DailySummary` record, flag mismatch.
- **Acceptance Criteria:** All jobs run on schedule, create log entries, correct behavior.
- **Tests:** Unit tests for each job's logic.
- **Definition of Done:** All jobs working.

### TASK-018 — Notifications System
- **Objective:** Complete notification generation for all events; build notification endpoints.
- **Requirements covered:** REQ-015
- **Dependencies:** TASK-004, TASK-012, TASK-014
- **Files affected:** `NotificationService.java`, `NotificationController.java`, `Notification.java`
- **Implementation Steps:**
  1. Verify `NotificationService.create()` is called in all required places (most already done in previous tasks).
  2. Implement GET /api/notifications with pagination.
  3. Implement GET /api/notifications/unread-count.
  4. Implement PUT /api/notifications/{id}/read.
  5. Implement PUT /api/notifications/read-all.
- **Acceptance Criteria:** All 12 notification events generate correct notifications.
- **Tests:** Integration test: trigger event, verify notification in DB.
- **Definition of Done:** All notifications created and query endpoints working.

### TASK-019 — Admin Dashboard & User Management
- **Objective:** Admin dashboard stats and user management endpoints.
- **Requirements covered:** REQ-017a, REQ-017b
- **Dependencies:** TASK-006, TASK-018
- **Files affected:** `AdminDashboardController.java`, `AdminUserController.java`
- **Implementation Steps:**
  1. Dashboard: total users, today's transfers (initiated + settled), today's volume, pending settlement total, pending fraud alerts count, 30-day daily volume data.
  2. User list with pagination and search.
  3. View user's recent transactions.
  4. Freeze/unfreeze wallet (already in TASK-006 — wire to admin endpoints).
- **Acceptance Criteria:** Dashboard returns correct figures. All admin actions audited.
- **Tests:** Integration tests with test data.
- **Definition of Done:** Admin user management working.

### TASK-020 — Admin Transaction Monitoring & Settlement Monitor
- **Objective:** Admin views all transactions, settlement queue, and can manually trigger settlement.
- **Requirements covered:** REQ-017c, REQ-017d
- **Dependencies:** TASK-016, TASK-014
- **Files affected:** `AdminTransactionController.java`, `AdminSettlementController.java`
- **Implementation Steps:**
  1. All transactions with filters (date, status, amount range).
  2. Pending transactions grouped by settlement date (due today, tomorrow, overdue).
  3. Manual settlement trigger for specific eligible pending transaction (audited).
- **Acceptance Criteria:** Filters work. Manual settlement processes the transaction correctly.
- **Tests:** Integration tests.
- **Definition of Done:** Admin monitoring endpoints working.

### TASK-021 — Admin Job Monitor & Reconciliation
- **Objective:** Admin views job logs and reconciliation data.
- **Requirements covered:** REQ-017f, REQ-017g
- **Dependencies:** TASK-014, TASK-017
- **Files affected:** `AdminJobController.java`, `AdminReconciliationController.java`
- **Implementation Steps:**
  1. GET /api/admin/jobs: last 20 job runs.
  2. GET /api/admin/reconciliation: today's debit/credit totals, mismatch flag.
- **Acceptance Criteria:** Returns correct data from scheduled_job_log and daily_summary.
- **Definition of Done:** Both endpoints working.

---

## PHASE-06: Frontend

### TASK-022 — Auth Pages (Login & Register)
- **Objective:** Login and register pages with JWT storage.
- **Requirements covered:** REQ-001, REQ-002
- **Dependencies:** TASK-003, TASK-004
- **Files affected:** `LoginPage.jsx`, `RegisterPage.jsx`, `AuthContext.jsx`
- **Implementation Steps:**
  1. Login form: email + password, show error on failure, show lock message on 423.
  2. Register form: all fields with validation.
  3. On success: store JWT in localStorage, update AuthContext, redirect to dashboard.
  4. Protected route wrapper.

### TASK-023 — User Dashboard & Wallet Page
- **Objective:** Dashboard with wallet balance, status, today's spending, and top-up.
- **Requirements covered:** REQ-005, REQ-006, REQ-009
- **Dependencies:** TASK-022
- **Files affected:** `DashboardPage.jsx`, `WalletPage.jsx`
- **Implementation Steps:**
  1. Fetch and display wallet balance, status, daily spending.
  2. Top-up form: select simulated account, enter amount, submit.
  3. Wallet deactivate/reactivate button.

### TASK-024 — Simulated Bank Accounts Page
- **Objective:** Create, view, add demo funds, deactivate simulated accounts.
- **Requirements covered:** REQ-007, REQ-008
- **Dependencies:** TASK-022
- **Files affected:** `BankAccountsPage.jsx`

### TASK-025 — Auto Reload Configuration Page
- **Objective:** Enable/disable Auto Reload and configure settings.
- **Requirements covered:** REQ-010
- **Dependencies:** TASK-024
- **Files affected:** `AutoReloadPage.jsx`

### TASK-026 — Send Money Page
- **Objective:** Search recipient, enter amount, remark, PIN, submit.
- **Requirements covered:** REQ-012
- **Dependencies:** TASK-022
- **Files affected:** `SendMoneyPage.jsx`
- **Implementation Steps:**
  1. Search by phone or email (debounce).
  2. Amount, remark, PIN fields.
  3. Generate `request_id` (UUID) on page load.
  4. Show result (pending status, settlement date).

### TASK-027 — Transaction History Page
- **Objective:** Full transaction history with pagination, filters, and detail view.
- **Requirements covered:** REQ-014
- **Dependencies:** TASK-022
- **Files affected:** `TransactionsPage.jsx`

### TASK-028 — Notifications
- **Objective:** Bell icon with unread count, notifications list, mark as read.
- **Requirements covered:** REQ-015
- **Dependencies:** TASK-022
- **Files affected:** `Header.jsx`, `NotificationsPage.jsx`

### TASK-029 — Admin Frontend
- **Objective:** All admin pages: dashboard (with chart), users, transactions, settlement, fraud, jobs, reconciliation.
- **Requirements covered:** REQ-017
- **Dependencies:** TASK-022
- **Files affected:** All `pages/admin/*.jsx`
- **Implementation Steps:**
  1. Admin dashboard with Recharts line chart (30-day volume).
  2. User list with freeze/unfreeze buttons.
  3. Transaction monitoring table.
  4. Settlement monitor with overdue flagging.
  5. Fraud alert queue with block/dismiss actions.
  6. Job monitor table.
  7. Reconciliation view with mismatch alert.

---

## PHASE-07: Integration, Testing & Hardening

### TASK-030 — End-to-End Integration Testing
- **Objective:** Run full integration test suite with TestContainers.
- **Requirements covered:** All
- **Dependencies:** All previous tasks
- **Files affected:** `src/test/java/**`
- **Tests:** All scenarios in testing-strategy.md.
- **Definition of Done:** All tests pass, no regressions.

### TASK-031 — Concurrency & Edge Case Hardening
- **Objective:** Verify concurrency safety for Auto Reload, daily limit, and balance operations.
- **Requirements covered:** REQ-011, REQ-013a
- **Dependencies:** TASK-030
- **Tests:** Concurrent Auto Reload, concurrent daily limit, negative balance prevention.

### TASK-032 — Security Hardening
- **Objective:** Verify all authorization checks, input validation, and error responses.
- **Requirements covered:** REQ-004, Security section
- **Dependencies:** TASK-030
- **Tests:** All security tests in testing-strategy.md.

### TASK-033 — Final Cleanup & README Update
- **Objective:** Clean up, verify all documentation, update README with verified setup steps.
- **Dependencies:** TASK-030
- **Files affected:** `README.md`, `docs/progress.md`, `docs/task-checklist.md`

---

## Risk & Dependency Map

```
TASK-001 ──► TASK-002 ──► TASK-004 ──► TASK-005, TASK-006, TASK-007
                                    ├──► TASK-008 ──► TASK-009 ──► TASK-011
                                    │              └──► TASK-010 ──► TASK-011
                                    └──► TASK-012 ──► TASK-013, TASK-014, TASK-016
                                                   └──► TASK-015
TASK-003 ──► TASK-022 ──► TASK-023 through TASK-029

TASK-014, TASK-017 ──► TASK-019, TASK-020, TASK-021
All backend ──► TASK-030 ──► TASK-031, TASK-032, TASK-033
```

## Key Risks

| Risk | Mitigation |
|---|---|
| Auto Reload concurrency | DB-level `reload_in_progress` flag + test |
| Idempotency under load | Unique `request_id` constraint + `ON CONFLICT` |
| 3-business-day edge cases | Unit tests for BusinessDayCalculator with multiple date scenarios |
| Settlement batch partial failure | Each settlement in its own try/catch; job continues on error |
| JWT expiry UX | Axios interceptor auto-redirects to login on 401 |
