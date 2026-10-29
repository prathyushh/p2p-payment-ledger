# Testing Strategy — P2P Payment Ledger

**Status:** PLANNED (not yet implemented)

---

## Overview

Testing covers unit, integration, security, concurrency, idempotency, ledger consistency, settlement, Auto Reload, refund, scheduled job, and frontend integration scenarios.

---

## 1. Unit Tests

### 1.1 Service Layer Tests (JUnit 5 + Mockito)

**AuthService**
- Registration creates user and wallet atomically.
- Duplicate email/phone throws ConflictException.
- Login with correct credentials returns JWT.
- Login with wrong password increments failed attempt count.
- Login locks account after 5th failed attempt.
- Locked account login throws AccountLockedException.

**WalletService**
- Deactivate wallet changes status to INACTIVE.
- Reactivate wallet changes status to ACTIVE.
- Cannot deactivate a FROZEN wallet.
- Balance update enforces >= 0 constraint.
- `daily_spent` increments correctly on debit.

**TransactionService**
- Valid send money creates PENDING transaction and DEBIT ledger entry.
- Sender balance reduced immediately.
- Duplicate `request_id` returns existing transaction.
- Invalid PIN rejects transaction.
- Insufficient balance rejects transaction.
- INACTIVE wallet rejects send.
- FROZEN wallet rejects send.

**FraudService**
- Daily limit: allows ₹49,999 + ₹1 = ₹50,000 (exactly at limit).
- Daily limit: blocks ₹49,999 + ₹2 = ₹50,001.
- Duplicate detection: same sender + receiver + amount within 2 min → blocked.
- Duplicate detection: same sender + receiver + amount after 2 min → allowed.
- High-value ≥ ₹8,000 proceeds but creates fraud alert.
- High-value < ₹8,000 does not create alert.

**AutoReloadService**
- Auto reload fires when balance drops below threshold.
- Auto reload does not fire when balance is above threshold.
- Auto reload does not fire when wallet is INACTIVE.
- Auto reload does not fire when wallet is FROZEN.
- Auto reload does not fire when disabled.
- Insufficient simulated funds → reload fails, no balance changes.
- Reload result (credit) does not re-trigger another reload.

**SettlementService**
- Settlement credits receiver and creates CREDIT ledger entry.
- Settlement on INACTIVE receiver wallet → DECLINED, refund sender.
- Settlement on FROZEN receiver wallet → DECLINED, refund sender.
- Settlement failure marks transaction FAILED and refunds sender.

**BusinessDayCalculator**
- Saturday initiation → settlement_due_date skips weekend.
- Friday initiation → +3 business days is Wednesday.
- Monday initiation → Thursday.

### 1.2 Repository Layer Tests (DataJPA slice, H2 or TestContainers)

- Wallet balance `CHECK >= 0` constraint is enforced.
- `request_id` unique constraint prevents duplicates.
- `simulated_balance >= 0` enforced.
- Auto reload settings `UNIQUE` on `wallet_id`.

---

## 2. Integration Tests

**Framework:** Spring Boot Test + TestContainers (PostgreSQL)

### 2.1 Registration & Login Flow
- Full registration → auto-creates wallet with ₹0.
- Login returns valid JWT.
- Protected endpoint without JWT → 401.
- Protected endpoint with USER token accessing ADMIN endpoint → 403.

### 2.2 Manual Wallet Top-Up
- Full flow: create simulated account → top up → verify wallet balance increased and simulated balance decreased by same amount.
- Verify ledger entry created with correct before/after balances.
- Verify `simulated_account_transactions` record created.
- Insufficient simulated funds → 422, no balance changes.

### 2.3 Send Money End-to-End
- Alice sends ₹500 to Bob → Alice balance deducted, Bob not credited yet.
- Transaction status = PENDING.
- Settlement job runs → Bob credited, status = SUCCESS.
- Notifications created for both Alice and Bob.

### 2.4 Idempotency
- POST /api/transactions/send with same `request_id` twice → second call returns same response as first, no new transaction created, no double-debit.

### 2.5 Fraud Control
- Daily limit: 5 × ₹10,000 transactions → 6th blocked, fraud alert created.
- Duplicate: send ₹500 to same user twice within 2 min → second blocked.
- High-value: send ₹9,000 → proceeds with PENDING, fraud alert with type HIGH_VALUE created.

### 2.6 Wallet Status Flow
- User deactivates wallet → send attempt returns clear error.
- Admin freezes wallet → send attempt returns clear error.
- Pending settlement arrives for inactive wallet → DECLINED, sender refunded.

### 2.7 Auto Reload Integration
- Setup: wallet balance ₹400, threshold ₹500, reload ₹2,000, simulated account ₹10,000.
- Send ₹100 → balance drops to ₹300 → auto reload fires → wallet becomes ₹2,300, simulated account ₹8,000.
- Verify AUTO_RELOAD transaction and ledger entry created.
- Verify notification sent.

---

## 3. Concurrency Tests

### 3.1 Auto Reload Concurrency
- Simulate 10 concurrent balance-decreasing operations when wallet is below threshold.
- Verify exactly ONE auto reload fires (not 10).
- Use `reload_in_progress` flag to verify.

### 3.2 Concurrent Send Money
- Two simultaneous send requests from same wallet → only one succeeds if insufficient balance.
- No negative wallet balance possible.

### 3.3 Daily Limit Concurrency
- 10 concurrent ₹6,000 sends (total ₹60,000) → at most 8 should succeed (≤ ₹50,000).
- No race condition allows exceeding the daily limit.

---

## 4. Idempotency Tests

- Duplicate `request_id` returns same transaction result (200 not 409).
- No double-debit from duplicate `request_id`.
- Same result returned whether request_id was used 5 minutes ago or 5 days ago.

---

## 5. Ledger Consistency Tests

- For every settled transaction: sender DEBIT amount = receiver CREDIT amount.
- Sum of all DEBIT entries for settled transactions = sum of all CREDIT entries.
- No settled transaction without exactly one DEBIT and one CREDIT.
- `balance_before` + amount = `balance_after` for every ledger entry.

---

## 6. Settlement Tests

### 6.1 Settlement Timing
- Transaction with `settlement_due_date` = today → settled by job.
- Transaction with `settlement_due_date` = tomorrow → NOT settled yet.
- Transaction with `settlement_due_date` = yesterday (overdue) → settled.

### 6.2 Settlement with Status Changes
- Settlement on INACTIVE receiver → DECLINED, sender refunded.
- Settlement on FROZEN receiver → DECLINED, sender refunded.

### 6.3 Settlement Job Isolation
- If one transaction's settlement throws an exception, the rest of the batch continues.
- Failed transaction marked FAILED, not left in PENDING.

---

## 7. Auto Reload Tests

- Insufficient simulated funds: both balances remain unchanged.
- Disabled auto reload: no reload triggered regardless of balance.
- Reload does not re-trigger from its own wallet credit.
- Wallet INACTIVE: no reload.
- Wallet FROZEN: no reload.

---

## 8. Refund Tests

- Blocked fraud alert (HIGH_VALUE) → sender refunded, both notified.
- Settlement failure → sender refunded.
- Settlement DECLINED (inactive receiver) → sender refunded.
- After refund: wallet balance = pre-transaction balance.

---

## 9. Scheduled Job Tests

### 9.1 Unit Tests (mock repositories)
- ACH Settlement Job: correct transactions selected (PENDING, due today).
- Daily Limit Reset Job: `daily_spent` set to 0 for all wallets.
- Auto-Unlock Job: accounts locked > 30 min are unlocked.
- Daily Summary Job: correct aggregation of the day's data.

### 9.2 Integration Tests
- Settlement job creates `scheduled_job_log` and `settlement_batches` records.
- Unlock job sends notifications to unlocked users.
- Reconciliation job flags a mismatch when debits ≠ credits.

---

## 10. Security Tests

- Endpoint without JWT → 401.
- Expired JWT → 401.
- USER accessing `/api/admin/**` → 403.
- User accessing another user's wallet → 403.
- User accessing another user's simulated accounts → 403.
- Malformed JWT → 401.
- SQL injection in search params → sanitized (no DB error, no data leak).

---

## 11. Frontend Integration Tests (React Testing Library + Vitest)

- Login form: valid credentials → navigates to dashboard.
- Login form: wrong credentials → shows error message.
- Wallet page: displays current balance from API.
- Send money form: validates amount > 0, PIN 4 digits before submitting.
- Transaction list: renders paginated results.
- Notification bell: shows unread count badge.

---

## Tools & Configuration

| Layer | Tool |
|---|---|
| Unit tests | JUnit 5 + Mockito |
| Integration tests | Spring Boot Test + TestContainers (PostgreSQL) |
| Concurrency tests | JUnit 5 `@RepeatedTest` + `ExecutorService` |
| Frontend unit tests | Vitest + React Testing Library |
| Code coverage | JaCoCo (target: 80% line coverage for services) |
| Test DB | TestContainers PostgreSQL (integration), H2 (unit slices optional) |

---

## Test Naming Convention

```
given_[context]_when_[action]_then_[expected]()
```

Example: `given_walletInactive_when_sendMoney_then_returns422WithClearMessage()`
