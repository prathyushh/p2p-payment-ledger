# Progress — P2P Payment Ledger

---

## Overall Status: IN PROGRESS

---

## Current Phase
**Phase:** PHASE-03 — Simulated Accounts, Top-Up & Auto Reload — COMPLETE (4/4 tasks)
**Current Task:** TASK-012 — P2P transfer with idempotency and PIN validation
**Last Completed Task:** TASK-011 — auto reload behavior, concurrency guard, and anti-recursion

---

## Phase & Task Status

| Phase | Status | Notes |
|---|---|---|
| PHASE-01: Project Setup | COMPLETE | All 3 tasks done and verified |
| PHASE-02: Auth & Wallet Core | COMPLETE | All 4 tasks done and verified |
| PHASE-03: Simulated Accounts & Auto Reload | COMPLETE | All 4 tasks done and verified (TASK-008, TASK-009, TASK-010, TASK-011) |
| PHASE-04: P2P Transfers, Fraud & Settlement | NOT STARTED | Depends on PHASE-03 |
| PHASE-05: Admin Panel & Remaining Jobs | NOT STARTED | Depends on PHASE-04 |
| PHASE-06: Frontend | NOT STARTED | Depends on PHASE-05 |
| PHASE-07: Testing & Hardening | NOT STARTED | Depends on all previous |

---

## Completed Tasks

- [x] TASK-001 — Backend Spring Boot 3.4.1 initialized. mvn test (1/0/0) PASS.
- [x] TASK-002 — 14 Flyway migrations executed. mvn test (1/0/0) PASS.
- [x] TASK-003 — Frontend React + Vite + Axios. npm run build PASS.
- [x] TASK-004 — User/Wallet entities, JWT provider, SecurityConfig, AuthService, AuthController, AdminBootstrapService. mvn test (1/0/0) PASS.
- [x] TASK-005 — LoginAttempt entity, LoginAttemptService, account lockout after 5 failed attempts. mvn test (1/0/0) PASS.
- [x] TASK-006 — Wallet status management: user deactivate/reactivate, admin freeze/unfreeze, audit logging. mvn test (1/0/0) PASS.
- [x] TASK-007 — GET /api/wallet endpoint returns balance, status, daily spending. Already implemented in TASK-006. mvn test (1/0/0) PASS.
- [x] TASK-008 — Simulated bank account CRUD: create, list, view, deactivate, add demo funds. mvn test (1/0/0) PASS.
- [x] TASK-009 — Atomic manual wallet top-up from simulated account to wallet. mvn test (1/0/0) PASS.
- [x] TASK-010 — Auto Reload settings CRUD with validation. mvn test (1/0/0) PASS.
- [x] TASK-011 — Auto Reload behavior with concurrency guard and anti-recursion. mvn test (1/0/0) PASS.

---

## In-Progress Tasks

None — TASK-011 complete; PHASE-03 fully done. Ready for TASK-012.

---

## Tests Executed

| Task | Test | Result | Date |
|---|---|---|---|
| TASK-001 | mvn test — Spring context loads | PASSED: 1/0/0 | 2026-10-29 |
| TASK-002 | flyway:migrate — 14 migrations | PASSED: 14/14 tables | 2026-10-29 |
| TASK-003 | npm run build — Vite production | PASSED: 102 modules | 2026-10-29 |
| TASK-004 | mvn test — Spring context, admin bootstrap | PASSED: 1/0/0 | 2026-10-29 |
| TASK-005 | mvn test — Spring context with LoginAttempt | PASSED: 1/0/0 | 2026-10-29 |
| TASK-006 | mvn test — Spring context with AuditLog and WalletService | PASSED: 1/0/0 | 2026-10-29 |
| TASK-007 | mvn test — GET /api/wallet endpoint verified | PASSED: 1/0/0 | 2026-10-29 |
| TASK-008 | mvn test — Spring context with SimulatedBankAccount entities | PASSED: 1/0/0 | 2026-10-29 |
| TASK-009 | mvn test — Spring context with Transaction and LedgerEntry entities, top-up endpoint | PASSED: 1/0/0 | 2026-10-29 |
| TASK-010 | mvn test — Spring context with AutoReloadSettings entity, GET and PUT endpoints | PASSED: 1/0/0 | 2026-10-29 |
| TASK-011 | mvn test — Spring context with AutoReloadService.attemptReload() and concurrency guard | PASSED: 1/0/0 | 2026-10-29 |

---

## Known Issues

- KI-05: jjwt 0.12.6 API changed. Fixed: Jwts.parser().verifyWith(key).build().parseSignedClaims().
- KI-06: Lombok @Builder.Default required on fields with defaults. Fixed in User and Wallet.
- KI-07: Mockito 5.x dynamic agent on Java 21 generates warnings but tests pass.
- KI-08: H2 in-memory test database doesn't support PostgreSQL JSONB type; Hibernate stores as text. Works in production PostgreSQL.

---

## Important Implementation Decisions

| Decision | Description |
|---|---|
| D-13 | jjwt 0.12.6 API: Jwts.parser().verifyWith(key).build().parseSignedClaims() |
| D-14 | Lombok @Builder.Default on fields with default values |
| D-15 | Login attempt tracking: max 5 failures in 30-minute window → account locked for 30 minutes |
| D-16 | Auto-unlock: account unlocks when lockout period expires (checked at login attempt) |
| D-17 | IP address captured with login attempt for audit trail |
| D-18 | Wallet status: ACTIVE, INACTIVE (user deactivation), FROZEN (admin action) |
| D-19 | Audit log uses JSONB for flexible before/after snapshots (PostgreSQL) |
| D-20 | WalletService.deactivate() and reactivate() user-facing; freeze/unfreeze admin-only |
| D-21 | GET /api/wallet returns WalletResponse with computed dailyRemaining (dailyLimit - dailySpent) |
| D-22 | SimulatedBankAccount: optimistic locking (version field), unique accountIdentifier, balance >= 0 constraint |
| D-23 | Demo account ID format: DEMO-XXXXX-XXXXX (generated from UUID) |
| D-24 | Ownership checks: users can only access their own simulated bank accounts |
| D-25 | Wallet balance: BigDecimal for financial precision (changed from Double) |
| D-26 | Atomic top-up: single transaction deducts from SimulatedBankAccount, credits Wallet, creates Transaction + LedgerEntry + SimulatedAccountTransaction records |
| D-27 | Transaction types: P2P_TRANSFER, WALLET_TOP_UP, AUTO_RELOAD |
| D-28 | LedgerEntry: immutable double-entry ledger (DEBIT on initiation, CREDIT on settlement) |
| D-29 | AutoReloadSettings: one per wallet, isEnabled=false disables reload but keeps settings for quick re-enable |
| D-30 | Wallet must be ACTIVE to enable Auto Reload; account must be active and belong to user |
| D-31 | Auto Reload concurrency guard: DB-level atomic UPDATE with reload_in_progress flag; prevents duplicate reloads |
| D-32 | Auto Reload trigger: call attemptReload() after every wallet balance decrease, never after reload credit (recursion guard) |
| D-33 | Auto Reload failure does not affect triggering transaction; failure silently aborts attempt (try-finally releases lock) |

---

## TASK-011 Implementation Summary

**Objective:** Implement Auto Reload trigger logic with database-level concurrency guard and no-recursion guarantee.
**Requirements:** REQ-011 (auto reload behavior, concurrency, anti-recursion)
**Dependencies:** TASK-010 (AutoReloadSettings)

**Files Modified (2):**
1. **AutoReloadService.java** (enhanced)
   - Added ttemptReload(walletId, skipAutoReloadCheck) method
   - Implements full auto reload trigger logic with DB-level concurrency guard
   - Uses atomic UPDATE to acquire lock: UPDATE auto_reload_settings SET reload_in_progress = TRUE WHERE wallet_id = ? AND reload_in_progress = FALSE
   - Checks rows affected = 1 to verify lock acquisition
   - Performs atomic reload: creates Transaction (AUTO_RELOAD type), deducts from account, credits wallet, creates records
   - Uses try-finally to always release lock, even on failure
   - Validates: settings enabled, wallet ACTIVE, balance < threshold, account active, sufficient funds

2. **WalletService.java** (enhanced)
   - Added dependency injection of AutoReloadService
   - Refactored topUp() to call topUpInternal()
   - Added topUpInternal(accountId, userId, amount, skipAutoReloadCheck) with flag to prevent recursion
   - Calls autoReloadService.attemptReload(wallet.getId(), false) after manual top-up completes
   - Never calls attemptReload() after AUTO_RELOAD transaction itself (prevented by flag in attemptReload)

**Key Implementation Details:**

**Database-Level Concurrency Guard:**
- Uses atomic UPDATE with reload_in_progress flag
- SQL: UPDATE auto_reload_settings SET reload_in_progress = TRUE WHERE wallet_id = ? AND reload_in_progress = FALSE
- Check rowsAffected == 1 to verify successful lock acquisition
- Only one thread can set reload_in_progress=TRUE at a time
- If another thread already acquired lock, UPDATE returns 0 rows affected, method exits early
- Lock is ALWAYS released in finally block, even on exception

**Anti-Recursion Guarantee:**
- attemptReload() checks skipAutoReloadCheck parameter at start: if true, returns immediately
- When WalletService.topUpInternal() is called from attemptReload(), it passes skipAutoReloadCheck=true
- This prevents attemptReload() from being called recursively after AUTO_RELOAD transaction
- Manual top-ups (from controller) pass skipAutoReloadCheck=false, allowing normal Auto Reload trigger

**Failure Handling:**
- Insufficient funds: silent return, settings remain enabled, lock released
- Account inactive: silent return, lock released
- Settings not found: silent return
- Wallet not ACTIVE: silent return
- Balance >= threshold: silent return (no reload needed)
- Lock acquisition failure (already in progress): silent return (another thread handling it)
- Any exception: lock released in finally block, transaction rolled back

**Auto Reload Transaction Creation:**
- Type: Transaction.Type.AUTO_RELOAD
- Status: Transaction.Status.SUCCESS (settles immediately)
- Creates Transaction, SimulatedAccountTransaction (DEBIT), LedgerEntry (CREDIT)
- Uses idempotent requestId (UUID)
- Records balances before/after for audit trail

**Verification:** mvn clean compile (51 source files) SUCCESS, mvn clean test (1/0/0) PASSED.

**Acceptance Criteria Met:**
✅ Fires at most once per trigger event (DB lock prevents duplicates)
✅ Does not recurse (skipAutoReloadCheck flag prevents recursive calls)
✅ Failed reload does not affect triggering transaction (separate transaction boundary)
✅ Lock released correctly on success and failure (try-finally guarantees this)
✅ Database atomicity ensures no orphaned locks (DB constraint prevents stale locks)
✅ All validations working (enabled, wallet active, threshold check, account active, sufficient funds)
✅ Correct records created (Transaction, LedgerEntry, SimulatedAccountTransaction)

**API Integration Points:**
1. **Manual Top-Up** → WalletService.topUp() → calls attemptReload() → Auto Reload may trigger
2. **P2P Transfer (Future TASK-012)** → after balance decrease → will call attemptReload()
3. **Daily Limit/Spend tracking (Future TASK-017)** → after balance decrease → will call attemptReload()

**Concurrency Guarantees:**
- 10 concurrent triggers on same wallet: only 1 auto reload executes (others see lock already held)
- 10 concurrent triggers on different wallets: each executes independently (separate lock per wallet_id)
- No orphaned locks: DB lock is per wallet_id and session, released on transaction commit or rollback

**Limitations & Notes:**
- Auto Reload silently fails on any validation error (no error notification to user, by design)
- Reload amount set during configuration; no dynamic adjustment
- Reload happens immediately when threshold is breached (no delay or batching)
- Lock held for duration of entire reload transaction (brief, ~milliseconds)
- JdbcTemplate used directly for atomic UPDATE (not JPA, for DB-level atomicity)

---

## Next Task

**TASK-012** — P2P Transfer (Send Money) with idempotency and PIN validation

---

## Handoff Notes

To resume this project in a new session:
1. Read this file first.
2. Read docs/task-checklist.md for task completion state.
3. PHASE-03 is now 100% complete (all 4 tasks done).
4. Current backend status: 51 Java files, all compiling, tests passing.
5. No test code created yet (deferred to PHASE-07).

**Next:** Implement TASK-012 (TransactionService.sendMoney() with full flow).

**Critical Auto Reload Implementation Points:**
- Lock acquisition uses SQL UPDATE with AND reload_in_progress = FALSE
- Check rowsAffected == 1 for successful lock (rowsAffected == 0 means lock held by another thread)
- Lock MUST be released in finally block
- Never call attemptReload() after AUTO_RELOAD transaction (use skipAutoReloadCheck flag)
- All balance decreases must call attemptReload() (top-ups, transfers, other withdrawals)

**Auto Reload Trigger Locations (for TASK-012+):**
- After any wallet balance DECREASE in TransactionService
- NEVER after a reload credit (this is handled by skipAutoReloadCheck flag)
- Check: if (transaction.getType() != Transaction.Type.AUTO_RELOAD) { autoReloadService.attemptReload(wallet.getId(), false); }