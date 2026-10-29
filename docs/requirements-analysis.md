# Requirements Analysis — P2P Payment Ledger (Simplified UPI / Wallet)

**Source:** docs/Project4.pdf (12 pages)
**Analyzed:** 2025-01-01
**Status:** Complete

---

## Overview

A peer-to-peer digital wallet application where users register, load money, transfer money to other users, and track transactions. Transfers follow an ACH-style delayed settlement model (3 business days). The system includes fraud detection, scheduled background jobs, admin monitoring, and in-app notifications.

**Technology Stack:** Spring Boot 3.x, Spring Data JPA, Spring Security (JWT), Spring Scheduler, PostgreSQL, React  
**Timeline:** 1 week  
**Scope:** Simulated wallet — no real bank integration or actual financial transactions.

---

## Functional Requirements

### REQ-001 — User Registration
- **Source:** Page 1
- **Description:** A user registers with name, email, phone number, password, and a 4-digit transaction PIN.
- **Business Rules:**
  - Transaction PIN is separate from the login password.
  - On successful registration, the system automatically creates a wallet for the user with ₹0 balance.
- **Acceptance Criteria:**
  - POST /api/auth/register accepts name, email, phone, password, PIN.
  - Returns 201 with JWT token.
  - Wallet is created atomically with user.
  - Duplicate email or phone returns 409.
  - Invalid/missing fields return 400.

### REQ-002 — User Login & JWT
- **Source:** Page 1
- **Description:** Users log in using email and password. The system issues a JWT token on successful login.
- **Business Rules:**
  - After 5 consecutive failed attempts, the account is locked for 30 minutes.
  - Locked account returns a clear error.
- **Acceptance Criteria:**
  - POST /api/auth/login returns JWT on success.
  - Returns 401 on wrong credentials.
  - Returns 423 (or 401 with message) on locked account.
  - Failed attempt count increments on each failure and resets on success.

### REQ-003 — Account Lock & Auto-Unlock
- **Source:** Pages 1, 7, 8
- **Description:** Accounts are locked after 5 failed login attempts for 30 minutes. They auto-unlock after 30 minutes.
- **Business Rules:**
  - Scheduled job runs every 5 minutes to unlock eligible accounts.
  - User is notified when account is locked (after 5th failed attempt).
  - User is notified when account is auto-unlocked.
- **Acceptance Criteria:**
  - Account locked notification sent after locking.
  - Unlock notification sent by scheduler.
  - Account is fully functional after unlock.

### REQ-004 — User Roles
- **Source:** Page 1
- **Description:** Two roles — USER and ADMIN. Regular users access only their own data. Admins access the admin panel.
- **Acceptance Criteria:**
  - USER role cannot access admin endpoints (returns 403).
  - ADMIN role can access all endpoints.
  - Role assigned at registration (default: USER). ADMIN is created via AdminBootstrapService on first startup using environment variables — not via normal registration. See DEC-007.

### REQ-005 — Wallet Lifecycle
- **Source:** Page 2
- **Description:** Every user has exactly one wallet, created automatically during registration with ACTIVE status.
- **Wallet Statuses:**
  - **ACTIVE:** Fully functional — send, receive, Auto Reload all work.
  - **INACTIVE:** User self-deactivated. All outgoing blocked. Incoming settlements declined, sender refunded, sender notified. Auto Reload disabled. User can reactivate at any time.
  - **FROZEN:** Admin-only action. All outgoing blocked. Incoming settlements declined, sender refunded, sender notified. Auto Reload blocked.
- **Business Rules:**
  - When sending from INACTIVE or FROZEN wallet, reject with clear error.
  - When settlement is due for INACTIVE/FROZEN receiver wallet, settlement job must decline, refund sender, notify sender.
- **Acceptance Criteria:**
  - Wallet created with ACTIVE status on registration.
  - User can deactivate/reactivate their own wallet.
  - Only admin can freeze/unfreeze.
  - Correct error messages returned for blocked operations.

### REQ-006 — Wallet Display
- **Source:** Page 2
- **Description:** Wallet displays available balance (spendable right now) and today's total spending.
- **Acceptance Criteria:**
  - GET /api/wallet returns current balance and today's spending.
  - Today's spending resets at midnight (Daily Limit Reset job).

### REQ-007 — Simulated Bank Accounts
- **Source:** Pages 2–3
- **Description:** Users can create and manage multiple simulated bank accounts as funding sources.
- **Fields:** Bank name, account nickname, generated demo account identifier, manually entered initial simulated balance.
- **Business Rules:**
  - Account balances must never become negative.
  - Users can only access their own accounts.
  - Balance changes must be traceable (amount, operation type, timestamp, outcome).
  - Users can deactivate an account if it is not being used in an active operation.
- **Acceptance Criteria:**
  - CRUD operations for simulated accounts.
  - Balance cannot go below zero (validated at service layer + DB constraint).
  - Deactivation blocked if account is linked to active Auto Reload.
  - All balance changes recorded.

### REQ-008 — Add Demo Funds
- **Source:** Page 3
- **Description:** Users can add additional demo funds to a simulated account via an "Add Demo Funds" operation.
- **Business Rules:**
  - Demo funding is simulation only — not a real bank deposit.
  - User receives an in-app notification on success.
- **Acceptance Criteria:**
  - POST /api/bank-accounts/{id}/demo-funds increases simulated balance.
  - Notification generated.
  - Amount must be positive.
  - Recorded in simulated_account_transactions (operation: DEMO_FUND_ADDED) and udit_log.
  - Does NOT create a record in the main 	ransactions table (does not affect wallet balance).

### REQ-009 — Manual Wallet Top-Up
- **Source:** Page 3
- **Description:** Users fund their wallet by selecting a simulated bank account and entering an amount.
- **Steps:**
  1. User selects simulated bank account.
  2. User enters amount.
  3. System validates amount > 0 and sufficient simulated funds.
  4. System deducts from simulated account and credits wallet.
  5. Both changes committed atomically.
  6. Recorded in transaction history and ledger; user notified.
- **Acceptance Criteria:**
  - Atomic transaction: if either side fails, both are rolled back.
  - Insufficient funds returns 422.
  - Wallet must be ACTIVE.
  - Transaction type: WALLET_TOP_UP.
  - Notification sent on success.

### REQ-010 — Auto Reload Configuration
- **Source:** Pages 3–4
- **Description:** Users configure automatic wallet top-up when balance falls below a threshold.
- **Configuration fields:**
  - Enabled/disabled.
  - Selected simulated bank account (funding source).
  - Minimum wallet balance threshold.
  - Fixed reload amount.
- **Acceptance Criteria:**
  - Users can enable/disable and update configuration at any time.
  - Auto Reload only available for ACTIVE wallets.
  - One Auto Reload config per wallet.

### REQ-011 — Auto Reload Behavior
- **Source:** Pages 3–4
- **Description:** After every successful wallet balance change, if balance falls below threshold and Auto Reload is enabled, system attempts reload.
- **Business Rules:**
  - Auto Reload must not execute if wallet is INACTIVE or FROZEN.
  - Must not create duplicate reloads for concurrent balance changes.
  - A failed reload must not affect the outcome of the triggering payment or wallet operation.
  - Must not trigger recursively from its own wallet credit.
  - Transaction type: AUTO_RELOAD.
  - User notified on success and failure.
- **Acceptance Criteria:**
  - Reload fires after any successful balance reduction below threshold.
  - Insufficient simulated funds → reload fails, neither balance changes, user notified.
  - No recursive reload (the credit from a reload does not trigger another reload).
  - Concurrent-safe: only one reload fires for simultaneous operations.

### REQ-012 — Send Money (ACH Delayed Settlement)
- **Source:** Pages 4–5
- **Description:** P2P transfer with 3-business-day delayed settlement.
- **Steps:**
  1. User searches recipient by phone or email.
  2. User enters amount, optional remark, confirms with 4-digit PIN.
  3. Sender's available balance reduced immediately.
  4. DEBIT ledger entry created for sender.
  5. Receiver not credited yet. Status = PENDING.
  6. `settlement_due_date` = 3 business days from initiation.
- **Settlement (background job):**
  - Receiver's balance increased.
  - CREDIT ledger entry created for receiver.
  - Status → SUCCESS.
  - Both parties notified.
- **Failure on settlement:**
  - Status → FAILED.
  - Sender refunded.
  - Sender notified.
- **Business Rules:**
  - Each request carries unique `request_id` (client-generated) for idempotency.
  - Duplicate `request_id` returns the original result without reprocessing.
- **Acceptance Criteria:**
  - POST /api/transactions/send requires valid PIN and `request_id`.
  - Idempotency: duplicate `request_id` returns 200 with existing result.
  - Sender can see PENDING transaction with settlement date.
  - Receiver not notified until settlement.

### REQ-013 — Fraud Controls
- **Source:** Pages 5–6
- **Description:** Three fraud checks on every send-money request.

#### REQ-013a — Daily Limit
- **Limit:** ₹50,000 per user per day.
- **On breach:** Transaction blocked, fraud alert created.
- **Acceptance Criteria:** Sum of today's sent amounts + new amount > 50,000 → blocked.

#### REQ-013b — Duplicate Detection
- **Window:** 2 minutes.
- **Condition:** Same sender, same recipient, same amount within 2 minutes.
- **On detection:** Transaction blocked, fraud alert created.
- **Acceptance Criteria:** Second identical transaction within 2 minutes → blocked.

#### REQ-013c — High-Value Flag
- **Threshold:** ₹8,000.
- **Behavior:** Transaction is NOT blocked — it proceeds to PENDING settlement. A fraud alert is created for admin review.
- **Admin actions on the alert:**
  - **Block:** Transaction → FAILED, sender refunded, both parties notified.
  - **Dismiss:** Alert dismissed, transaction continues to scheduled settlement.
- **Business Rules:**
  - Alerts for already-settled transactions are read-only.
  - Admin has until the settlement date to act.
- **Acceptance Criteria:**
  - Alert created, but transaction still enters PENDING.
  - Admin can block or dismiss before settlement.
  - Post-settlement alerts are read-only.

### REQ-014 — Transaction History
- **Source:** Pages 6–7
- **Description:** Users view transaction history with pagination and filters.
- **Features:**
  - Mini-statement: last 10 transactions.
  - Full history with pagination.
  - Filters: date range, type (sent/received/wallet top-up/auto reload/all), status (success/pending/failed/declined/all).
  - Pending transfers show: status, expected settlement date, days remaining.
  - Transaction detail view shows associated ledger entries.
  - Personal stats: total sent, total received, this month's transaction count.
- **Acceptance Criteria:**
  - Pagination with configurable page size.
  - All filter combinations work.
  - Wallet top-up and Auto Reload distinguishable from P2P transfers.

### REQ-015 — Notifications
- **Source:** Pages 6–8
- **Description:** In-app notifications for all major events.
- **Features:**
  - Unread count on bell icon in header.
  - View all notifications.
  - Mark as read.
- **Events and recipients:**

| Event | Recipient |
|---|---|
| Transfer initiated | Sender |
| Transfer settled | Sender |
| Transfer settled | Receiver |
| Transfer failed | Sender |
| Transaction blocked (daily limit) | Sender |
| Fraud alert raised (high-value) | Admin |
| Account locked | User |
| Account auto-unlocked | User |
| Wallet top-up successful | User |
| Auto Reload successful | User |
| Auto Reload failed | User |
| Demo funds added | User |

- **Acceptance Criteria:**
  - All events generate appropriate notifications.
  - Unread count accurate.
  - Mark-as-read works individually or all-at-once.

### REQ-016 — Scheduled Background Jobs
- **Source:** Pages 8–9

#### REQ-016a — ACH Settlement Processor (Every Hour)
- Finds PENDING transactions where `settlement_due_date` ≤ today.
- For each: credit receiver, create CREDIT entry, status → SUCCESS, notify both.
- If settlement fails for individual transaction: status → FAILED, refund sender, continue.
- Log: transactions settled, transactions failed, total amount settled.

#### REQ-016b — Daily Limit Reset (Midnight)
- Resets daily spending counters for all users.

#### REQ-016c — Account Auto-Unlock (Every 5 Minutes)
- Finds accounts locked > 30 minutes.
- Unlocks and sends account-unlocked notification.

#### REQ-016d — Daily Summary & Reconciliation (11 PM)
- Aggregates: transfers initiated (count + amount), transfers settled (count + amount), fraud alerts raised, new user registrations, total pending settlement amount.
- Reconciliation: sum of DEBIT ledger entries vs. sum of CREDIT ledger entries for settled transactions.
- Any mismatch → critical admin notification.
- **Acceptance Criteria:**
  - All four jobs run on schedule.
  - Each run logged in `scheduled_job_log` with start time, end time, success/failure, records processed, error message.

### REQ-017 — Admin Panel
- **Source:** Pages 9–10
- **Features:**

#### REQ-017a — Dashboard
- Total registered users, today's transfers initiated, today's transfers settled, today's volume, total pending settlement amount, pending fraud alerts count.
- Line chart: daily transaction volume for last 30 days.

#### REQ-017b — User List
- View all users with wallet balance, wallet status, registration date.
- View a user's recent transactions.
- Freeze or unfreeze a wallet.

#### REQ-017c — Transaction Monitoring
- All transactions with filters (date, status, amount range).
- Full transaction details and ledger entries.
- Distinguish pending, top-ups, auto reload.

#### REQ-017d — Settlement Monitor
- All pending transactions with expected settlement dates.
- Due today, due tomorrow, overdue counts.
- Manual trigger for eligible pending transaction.

#### REQ-017e — Fraud Review
- All pending fraud alerts.
- Block or dismiss eligible high-value alerts.
- Read-only view for already-settled alerts.

#### REQ-017f — Job Monitor
- Last 20 scheduled job runs with status and statistics.

#### REQ-017g — Reconciliation View
- Today's total debits vs. credits (settled transactions).
- Pending settlement amounts.
- Flag mismatches.

- **Acceptance Criteria:**
  - All admin actions written to `audit_log`.
  - 403 returned for non-admin access.

---

## Business Rules Summary

| Rule | Value |
|---|---|
| Daily transaction limit per user | ₹50,000 |
| Duplicate detection window | 2 minutes |
| High-value flag threshold | ₹8,000 |
| Settlement period | 3 business days |
| Account lock after failed logins | 5 attempts |
| Auto-unlock after | 30 minutes |
| Initial wallet balance | ₹0 |
| Transaction PIN | 4 digits |
| Transaction statuses | PENDING → SUCCESS / FAILED / DECLINED |
| Roles | USER, ADMIN |
| Auto Reload threshold | Configurable by user |
| Auto Reload amount | Fixed amount configurable by user |
| Simulated bank accounts per user | Multiple |
| Real bank integration | Not required |

---

## Database Requirements

**13 required tables (from Pages 11–12):**

1. `users`
2. `login_attempts`
3. `wallets`
4. `transactions`
5. `ledger_entries`
6. `fraud_alerts`
7. `notifications`
8. `audit_log`
9. `scheduled_job_log`
10. `daily_summary`
11. `settlement_batches`
12. `simulated_bank_accounts`
13. `auto_reload_settings`

**Optional 14th table (if needed):**  
`simulated_account_transactions` — per-account balance change history (if the existing transaction/audit tables are not sufficient to trace simulated account operations reliably).

---

## Security Requirements

- Spring Security with JWT for authentication and authorization.
- Transaction PIN required for every P2P transfer.
- Rate limiting on login attempts (account lock after 5 failures).
- Role-based access control (USER vs ADMIN).
- All admin actions audited.
- No credentials stored in plain text (BCrypt for passwords; PIN hashed separately).

---

## Non-Functional Requirements

- Atomic balance changes: all balance updates in database transactions with appropriate locking.
- Idempotency: duplicate `request_id` returns original result.
- Concurrency: Auto Reload safe against concurrent balance changes (no duplicate reloads).
- No negative balances: enforced at service and database level.
- Scheduled jobs: reliable, logged, error-isolated (one failure does not abort the whole batch).

---

## Decisions & Resolved Questions

| ID | Question | Resolution | Decided By | Decision Ref |
|---|---|---|---|---|
| UQ-01 | How is ADMIN role assigned? | `AdminBootstrapService` runs on startup, checks for existing admin, reads email + password from env vars (`ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD`), BCrypt-hashes the password, inserts admin user + wallet if none exists. Idempotent — safe on every restart. No credentials in source control or migrations. | Owner | DEC-007 |
| UQ-02 | "3 business days" — timezone and holiday calendar? | UTC timezone; weekdays only (Mon–Fri); no public holiday calendar. Settlement dates may fall on public holidays, which is acceptable for a simulated system. | Default (owner to confirm) | DEC-005 |
| UQ-03 | PIN storage: hashed or plain? | BCrypt hash, same as login password. PIN is not recoverable. | Default (owner to confirm) | DEC-006 |
| UQ-04 | "Today's spending" based on initiated or settled? | Based on initiated (deducted) amounts. Consistent with the daily fraud limit check at send time. | Default (owner to confirm) | DEC-009 |
| UQ-05 | Can a user send money to themselves? | Blocked. HTTP 422 with message "You cannot transfer money to yourself." Validated before fraud checks and balance changes. | Owner | DEC-011 |
| UQ-06 | Pagination defaults? | Default page size 20, maximum 100. Requests exceeding 100 are clamped. | Default (owner to confirm) | — |
| UQ-07 | Auto Reload duplicate prevention mechanism? | DB-level `reload_in_progress` boolean on `auto_reload_settings`. Conditional atomic `UPDATE ... WHERE reload_in_progress = FALSE`; check rows affected = 1. Flag released in `finally` block. | Default (owner to confirm) | DEC-004 |
| UQ-08 | Simulated account traceability — separate table or extend existing? | Dedicated 14th table `simulated_account_transactions`. | Default (owner to confirm) | DEC-001 |
| UQ-09 | Transaction types in `transactions` table? | Three types: `P2P_TRANSFER`, `WALLET_TOP_UP`, `AUTO_RELOAD`. `DEMO_FUND_ADDED` is excluded. | Default (owner to confirm) | DEC-013 |
| UQ-10 | Demo funds in the main `transactions` table? | No. Recorded in `simulated_account_transactions` (DEMO_FUND_ADDED) and `audit_log` only. Does not appear in wallet transaction history. | Owner | DEC-012 |

**Defaults awaiting owner confirmation:** UQ-02, UQ-03, UQ-04, UQ-06, UQ-07, UQ-08, UQ-09. Implementation will proceed with these defaults. See docs/decisions.md for full context on each.

