# Decision Log — P2P Payment Ledger

**Purpose:** Record all important architecture and implementation decisions, their rationale, and consequences.

---

## Approved Decisions

### DEC-001 — Add 14th Table: `simulated_account_transactions`
- **Date:** Planning phase
- **Context:** The PDF states "Simulated account balance changes must be traceable, including the amount, operation type, timestamp, and outcome." The existing `transactions` table tracks wallet movements. Simulated account balance changes (demo fund additions, wallet top-ups, auto reloads) need a separate per-account audit trail.
- **Decision:** Add `simulated_account_transactions` as a 14th table.
- **Rationale:** Keeps the simulated account ledger separate and queryable per account without polluting the wallet ledger. The PDF explicitly allows this: "This can be handled with a dedicated simulated-account transaction history table if required."
- **Consequences:** One extra table and repository; minimal added complexity.

### DEC-002 — Flyway for Database Migrations
- **Date:** Planning phase
- **Context:** Need a reliable, version-controlled approach to database schema management.
- **Decision:** Use Flyway with sequential versioned migration files (V1__create_users.sql, etc.).
- **Rationale:** Native Spring Boot integration, simple to use, migrations run automatically on startup.
- **Consequences:** Every schema change must go through a new migration file. Never modify existing migrations.

### DEC-003 — Optimistic Locking on Wallets and Simulated Accounts
- **Date:** Planning phase
- **Context:** Multiple concurrent requests can modify the same wallet or simulated account balance.
- **Decision:** Use `version` column (JPA `@Version`) on `wallets` and `simulated_bank_accounts` entities for optimistic locking. Use pessimistic locking (`SELECT FOR UPDATE`) only for critical atomic operations like top-up and auto reload.
- **Rationale:** Optimistic locking is appropriate for low-contention scenarios. Pessimistic locking prevents race conditions in the auto reload flow where we must read-then-write atomically.
- **Consequences:** Application must handle `OptimisticLockException` and retry or return a conflict error.

### DEC-004 — `reload_in_progress` Flag for Auto Reload Concurrency
- **Date:** Planning phase
- **Context:** Multiple concurrent wallet balance changes could each independently trigger Auto Reload, causing multiple reloads.
- **Decision:** Use a `reload_in_progress` boolean on `auto_reload_settings`. Attempt to set it to TRUE with `UPDATE ... WHERE reload_in_progress = FALSE` and check rows affected = 1. If 0 rows affected, another reload is already running — skip.
- **Rationale:** Atomic conditional update prevents duplicate reloads without requiring distributed locks.
- **Consequences:** Must always release the flag (set to FALSE) even if the reload fails, ideally in a `finally` block.

### DEC-005 — Business Days = Skip Weekends Only
- **Date:** Planning phase
- **Context:** "3 business days" settlement needs a concrete definition. The PDF does not specify a holiday calendar.
- **Decision:** Business days = Monday–Friday, excluding weekends only. No public holidays considered.
- **Rationale:** Simplest correct interpretation. No holiday calendar was specified. Can be upgraded later.
- **Consequences:** Settlement dates may land on public holidays, which is acceptable for a simulated system.

### DEC-006 — PIN Stored as BCrypt Hash
- **Date:** Planning phase
- **Context:** The PDF requires a 4-digit transaction PIN. Storing PINs in plain text is insecure.
- **Decision:** Hash the PIN using BCrypt, same as the login password.
- **Rationale:** Security best practice. The PIN is short (4 digits) so BCrypt is sufficient.
- **Consequences:** PIN is not recoverable. PIN reset is explicitly out of scope for this sprint (see DEC-014).

### DEC-007 — Admin Bootstrap: Environment-Driven, BCrypt-Hashed, Idempotent
- **Date:** Planning phase — UPDATED with owner direction
- **Context:** No admin registration flow is described. The system needs at least one admin to function.
- **Previous assumption (revised):** Seed admin via Flyway V15 with hardcoded plain-text password.
- **Decision:** On application startup, an `AdminBootstrapService` (run via `ApplicationRunner`) checks whether an ADMIN-role user exists. If none exists, it reads the admin email and password from environment variables (`ADMIN_BOOTSTRAP_EMAIL`, `ADMIN_BOOTSTRAP_PASSWORD`), hashes the password with BCrypt, and inserts the admin user with an active wallet. If an ADMIN already exists, the bootstrap is skipped. No credentials are committed to source control or migrations.
- **Rationale:**
  - Environment variables keep credentials out of source control and migration files.
  - The check-first pattern is idempotent — safe on every subsequent startup.
  - BCrypt hashing at runtime means no plaintext is ever persisted.
- **Consequences:**
  - `ADMIN_BOOTSTRAP_EMAIL` and `ADMIN_BOOTSTRAP_PASSWORD` must be set before first startup.
  - These variables must be added to `.env.example` (documented but not committed with real values).
  - Flyway V15 migration is not needed for admin seeding; it is removed from the migration plan.
  - The `AdminBootstrapService` is covered by an integration test that verifies: (a) admin is created on first run, (b) a second run does not create a duplicate.

### DEC-008 — Idempotency via `request_id` Unique Constraint
- **Date:** Planning phase
- **Context:** "Each payment request must carry a unique `request_id` generated by the client. If the same `request_id` is submitted again, the system returns the original result."
- **Decision:** `request_id` is a UNIQUE column on `transactions`. On duplicate submission, query by `request_id` and return existing transaction (HTTP 200, not 409).
- **Rationale:** Database constraint guarantees no double-processing even under concurrent requests. Returning 200 (not 409) follows idempotency conventions.
- **Consequences:** Client is responsible for generating a unique `request_id` (UUID recommended) per payment attempt.

### DEC-009 — Today's Spending Based on Initiated (Deducted) Amounts
- **Date:** Planning phase
- **Context:** The wallet displays "today's total spending." It's ambiguous whether this means initiated or settled transactions.
- **Decision:** "Today's spending" tracks amounts deducted from the sender's wallet today (initiated), not settled amounts.
- **Rationale:** The daily limit (Rs 50,000) operates on initiated amounts (the check happens at send time). Showing initiated spending is consistent with the daily limit counter.
- **Consequences:** A PENDING transaction counts toward today's spending even before it settles.

### DEC-010 — React + Vite, Recharts, Axios (No Redux)
- **Date:** Planning phase
- **Context:** React is required. Other frontend tooling is not specified.
- **Decision:** Use Vite (fast dev server), Recharts (admin chart), Axios (HTTP), React Context API (state).
- **Rationale:** Vite is the modern React standard. Recharts is lightweight and sufficient for the line chart. Context API avoids Redux overhead for this scope.
- **Consequences:** If the app grows significantly, Context may need to be replaced with Zustand or Redux.

### DEC-011 — Self-Transfers Are Blocked
- **Date:** Updated with owner direction (UQ-05)
- **Context:** The PDF does not explicitly state whether a user can send money to themselves.
- **Decision:** Block self-transfers. If the resolved recipient wallet ID equals the sender wallet ID, reject the request before any balance changes with HTTP 422 and a clear error message: "You cannot transfer money to yourself."
- **Rationale:** Self-transfers produce meaningless ledger entries and could mask accounting errors. The owner has confirmed this should be blocked.
- **Consequences:** Validated in `TransactionService` before fraud checks. Unit-tested explicitly.

### DEC-012 — Add Demo Funds: Simulated Ledger Only, Not Main Transactions
- **Date:** Updated with owner direction (UQ-10)
- **Context:** Should "Add Demo Funds" create a record in the main `transactions` table?
- **Decision:** Add Demo Funds does NOT create a record in the main `transactions` table because it does not affect the wallet balance. It is recorded in `simulated_account_transactions` (operation type: DEMO_FUND_ADDED) and in `audit_log`. An in-app notification is generated. Wallet top-ups and Auto Reload, which do affect the wallet, continue to be recorded in the main `transactions` table and `ledger_entries`.
- **Rationale:** Keeps the main transactions table semantically clean (wallet movements only). The owner confirmed this separation.
- **Consequences:** The `simulated_account_transactions` table must have adequate fields for all simulated operations. The audit_log provides a cross-cutting record of the demo funds action.

### DEC-013 — Transaction Types in `transactions` Table
- **Date:** Planning phase — confirmed
- **Context:** The `transactions` table must support wallet top-up and Auto Reload in addition to P2P transfers.
- **Decision:** Three transaction types: `P2P_TRANSFER`, `WALLET_TOP_UP`, `AUTO_RELOAD`. `DEMO_FUND_ADDED` is NOT a transaction type in this table (see DEC-012).
- **Rationale:** Matches PDF requirements. Demo funds do not touch the wallet, so they have no place in the wallet transaction ledger.
- **Consequences:** The `type` column on `transactions` is an enum with exactly three values.

### DEC-014 — PIN Reset: Out of Scope
- **Date:** Updated with owner direction (UDEC-003)
- **Context:** No PIN reset flow is described in the spec. The one-week timeline is constrained.
- **Decision:** PIN reset is explicitly out of scope for this sprint. No PIN reset API, no PIN reset UI, no email OTP infrastructure.
- **Rationale:** Owner confirmed out of scope. BCrypt hash means PIN cannot be recovered — users who forget their PIN cannot transact until scope is extended.
- **Consequences:** No PIN reset endpoint will be implemented or stubbed. A comment in the codebase notes it as a future enhancement.

---

## Remaining Assumptions — Awaiting Owner Confirmation

The following questions have assumed defaults applied during planning. The implementation will proceed with these defaults unless the owner overrides them. They are listed here with full context so the owner can make an informed decision.

### UQ-02 — "3 Business Days": Timezone and Holiday Calendar

**Question:** Which timezone is used to determine "today" for settlement date calculation, and does "business day" include public holidays?

**Assumed default:** UTC timezone; business days = Monday–Friday only, no public holiday calendar.

**Consequences of this assumption:**
- Settlement dates are calculated in UTC. A transaction initiated at 11 PM IST (5:30 PM UTC) on a Friday is initiated on Friday UTC, giving a settlement_due_date of Wednesday.
- No public holiday (Diwali, Christmas, etc.) is skipped. Settlement can fall on a public holiday.
- The `BusinessDayCalculator` utility only skips Saturday and Sunday.

**Alternative:** Use IST (UTC+5:30) as the reference timezone and/or maintain a holiday exclusion list. This adds meaningful complexity (holiday data, timezone handling) for a simulated system.

**Recommendation:** Proceed with the assumed default. Note it in code comments for future upgrade.

---

### UQ-03 — PIN Storage: BCrypt or Plain

**Question:** Should the 4-digit transaction PIN be stored as a BCrypt hash or in plain text?

**Assumed default:** BCrypt hash (same as the login password).

**Consequences of this assumption:**
- PIN is stored as a 60-character BCrypt hash, not the original 4-digit value.
- PIN verification requires `BCryptPasswordEncoder.matches(inputPin, storedHash)`.
- PIN is not recoverable — if forgotten, the user cannot transact (PIN reset is out of scope per DEC-014).
- This is the secure choice regardless of the simulated nature of the application.

**Alternative:** Store plain text. Easier to verify, but insecure and bad practice even in demo systems.

**Recommendation:** BCrypt is the correct choice. No change needed.

---

### UQ-04 — "Today's Total Spending": Initiated or Settled Amounts

**Question:** Does "today's total spending" on the wallet display represent amounts deducted (initiated) or amounts that have settled?

**Assumed default:** Initiated (deducted) amounts — the `daily_spent` counter on the wallet increments when a transfer is initiated, not when it settles.

**Consequences of this assumption:**
- The `daily_spent` figure shown to the user reflects money that has left their wallet today, including PENDING transactions that have not yet settled.
- This is consistent with the daily fraud limit (Rs 50,000), which is also checked at initiation time.
- A refund (FAILED or DECLINED settlement) does NOT decrement `daily_spent` — the daily limit still reflects the full initiated amount.

**Alternative:** Base spending on settled amounts only. This would show less than what was actually deducted, which is potentially misleading.

**Recommendation:** Proceed with initiated amounts. Consistent with the daily limit implementation.

---

### UQ-06 — Pagination Defaults

**Question:** What are the default and maximum page sizes for paginated API responses?

**Assumed default:** Default page size = 20, maximum page size = 100.

**Consequences of this assumption:**
- All list endpoints (`GET /api/transactions`, `GET /api/notifications`, `GET /api/admin/users`, etc.) use these defaults.
- A request with no `size` param returns up to 20 records.
- A request with `size=200` is clamped to 100.
- These values are reasonable for a one-week project and typical UX needs.

**Alternative:** Different defaults (e.g., 10/50 or 50/200). No functional impact, just UX preference.

**Recommendation:** Proceed with 20/100. Easy to change if needed.

---

### UQ-07 — Auto Reload Duplicate Prevention Mechanism

**Question:** Should the concurrency guard for Auto Reload use a DB-level flag, application-level locking, or optimistic locking?

**Assumed default:** DB-level `reload_in_progress` boolean flag on `auto_reload_settings`, updated with a conditional atomic `UPDATE ... WHERE reload_in_progress = FALSE`. Check affected rows = 1 before proceeding.

**Consequences of this assumption:**
- Prevents duplicate reloads even when multiple threads or requests concurrently check the balance threshold.
- The flag must be released (set to FALSE) in a `finally` block to avoid deadlock if the reload fails.
- Works correctly for a single application instance. For a multi-instance deployment, this approach is sufficient as long as all instances share the same PostgreSQL database.

**Alternative:** Use a distributed lock (e.g., Redis). Adds infrastructure dependency not justified for this sprint.

**Recommendation:** Proceed with DB-level flag. Correct and simple for this scope.

---

### UQ-08 — Simulated Account Traceability: Separate Table or Extend Existing

**Question:** Should simulated account balance changes be traced in a dedicated table, or by extending the existing `transactions` and `audit_log` tables?

**Assumed default:** Dedicated 14th table `simulated_account_transactions`. This is also DEC-001.

**Consequences of this assumption:**
- Clean separation: wallet ledger in `transactions`/`ledger_entries`; simulated account ledger in `simulated_account_transactions`.
- All three simulated operations (DEMO_FUND_ADDED, WALLET_TOP_UP debit, AUTO_RELOAD debit) are recorded here with before/after balances.
- One additional table and repository to implement.

**Alternative:** Use `audit_log` only (no dedicated table). Loses queryability and structure for per-account history.

**Recommendation:** Proceed with the dedicated table. Already DEC-001.

---

### UQ-09 — Transaction Types in the `transactions` Table

**Question:** What are the valid values for the `type` column in the `transactions` table?

**Assumed default:** Three types: `P2P_TRANSFER`, `WALLET_TOP_UP`, `AUTO_RELOAD`. `DEMO_FUND_ADDED` is NOT included (see DEC-012).

**Consequences of this assumption:**
- The `type` column is a string/enum with exactly three values.
- Wallet top-ups (manual) and Auto Reload operations both appear in the user's transaction history alongside P2P transfers.
- Demo fund additions do NOT appear in transaction history — they appear only in `simulated_account_transactions`.
- Transaction filters in the UI offer: "sent", "received", "wallet top-up", "auto reload", "all".

**Alternative:** Add a fourth type `DEMO_FUND_ADDED`. This would make demo fund additions visible in the main transaction list, which contradicts DEC-012.

**Recommendation:** Three types only. Consistent with DEC-012 and owner direction.
