# Database Design — P2P Payment Ledger

**Source:** docs/Project4.pdf (Pages 11–12) + requirements analysis  
**Status:** IMPLEMENTED — all 14 tables migrated successfully (2026-10-29)

---

## Overview

13 required tables plus 1 recommended addition for traceability.  
All balance-changing operations use database transactions with row-level locking.

---

## Table 1: `users`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| name | VARCHAR(100) | NOT NULL | |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Login identifier |
| phone_number | VARCHAR(20) | UNIQUE, NOT NULL | Used for recipient search |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt |
| pin_hash | VARCHAR(255) | NOT NULL | BCrypt — 4-digit transaction PIN |
| role | VARCHAR(10) | NOT NULL, DEFAULT 'USER' | USER or ADMIN |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| account_locked | BOOLEAN | NOT NULL, DEFAULT FALSE | Set after 5 failed logins |
| locked_until | TIMESTAMP | NULLABLE | Auto-unlock time |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:** `email` (unique), `phone_number` (unique)

---

## Table 2: `login_attempts`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| user_id | BIGINT | FK → users.id, NOT NULL | |
| attempt_time | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| success | BOOLEAN | NOT NULL | |
| ip_address | VARCHAR(45) | NULLABLE | For audit |

**Index:** `user_id`, `attempt_time DESC`

---

## Table 3: `wallets`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| user_id | BIGINT | FK → users.id, UNIQUE, NOT NULL | One wallet per user |
| balance | NUMERIC(15,2) | NOT NULL, DEFAULT 0, CHECK >= 0 | Available balance |
| status | VARCHAR(10) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE / INACTIVE / FROZEN |
| daily_spent | NUMERIC(15,2) | NOT NULL, DEFAULT 0 | Resets at midnight |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| version | BIGINT | NOT NULL, DEFAULT 0 | Optimistic lock |

**Indexes:** `user_id` (unique)

---

## Table 4: `transactions`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| request_id | VARCHAR(100) | UNIQUE, NOT NULL | Client-generated idempotency key |
| type | VARCHAR(20) | NOT NULL | P2P_TRANSFER, WALLET_TOP_UP, AUTO_RELOAD |
| sender_wallet_id | BIGINT | FK → wallets.id, NULLABLE | NULL for top-ups received |
| receiver_wallet_id | BIGINT | FK → wallets.id, NULLABLE | NULL for sent transfers before settlement |
| amount | NUMERIC(15,2) | NOT NULL, CHECK > 0 | |
| status | VARCHAR(10) | NOT NULL | PENDING / SUCCESS / FAILED / DECLINED |
| remark | VARCHAR(255) | NULLABLE | Optional note for P2P |
| settlement_due_date | DATE | NULLABLE | 3 business days; NULL for top-ups |
| simulated_account_id | BIGINT | FK → simulated_bank_accounts.id, NULLABLE | For top-ups and auto reload |
| initiated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| settled_at | TIMESTAMP | NULLABLE | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:** `request_id` (unique), `sender_wallet_id`, `receiver_wallet_id`, `status`, `settlement_due_date`, `initiated_at`

---

## Table 5: `ledger_entries`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| transaction_id | BIGINT | FK → transactions.id, NOT NULL | |
| wallet_id | BIGINT | FK → wallets.id, NOT NULL | |
| type | VARCHAR(6) | NOT NULL | DEBIT or CREDIT |
| amount | NUMERIC(15,2) | NOT NULL, CHECK > 0 | |
| balance_before | NUMERIC(15,2) | NOT NULL | Snapshot before change |
| balance_after | NUMERIC(15,2) | NOT NULL | Snapshot after change |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:** `transaction_id`, `wallet_id`, `created_at DESC`

---

## Table 6: `fraud_alerts`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| transaction_id | BIGINT | FK → transactions.id, NOT NULL | |
| alert_type | VARCHAR(30) | NOT NULL | DAILY_LIMIT_EXCEEDED, DUPLICATE_TRANSACTION, HIGH_VALUE |
| status | VARCHAR(10) | NOT NULL | PENDING, BLOCKED, DISMISSED |
| reviewed_by | BIGINT | FK → users.id, NULLABLE | Admin who acted |
| reviewed_at | TIMESTAMP | NULLABLE | |
| notes | TEXT | NULLABLE | Admin notes |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:** `transaction_id`, `status`, `alert_type`

---

## Table 7: `notifications`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| user_id | BIGINT | FK → users.id, NOT NULL | |
| type | VARCHAR(50) | NOT NULL | Event type enum |
| title | VARCHAR(255) | NOT NULL | |
| message | TEXT | NOT NULL | |
| is_read | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| related_transaction_id | BIGINT | FK → transactions.id, NULLABLE | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:** `user_id`, `is_read`, `created_at DESC`

---

## Table 8: `audit_log`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| actor_id | BIGINT | FK → users.id, NULLABLE | Admin user; NULL for system |
| action | VARCHAR(100) | NOT NULL | e.g., FREEZE_WALLET, BLOCK_TRANSACTION |
| entity_type | VARCHAR(50) | NOT NULL | e.g., WALLET, TRANSACTION, USER |
| entity_id | BIGINT | NOT NULL | ID of affected entity |
| details | JSONB | NULLABLE | Before/after snapshot |
| ip_address | VARCHAR(45) | NULLABLE | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:** `actor_id`, `entity_type`, `entity_id`, `created_at DESC`

---

## Table 9: `scheduled_job_log`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| job_name | VARCHAR(100) | NOT NULL | e.g., ACH_SETTLEMENT, DAILY_RESET |
| started_at | TIMESTAMP | NOT NULL | |
| ended_at | TIMESTAMP | NULLABLE | |
| status | VARCHAR(10) | NOT NULL | SUCCESS, FAILED, RUNNING |
| records_processed | INT | NULLABLE | |
| records_failed | INT | NULLABLE | |
| total_amount | NUMERIC(15,2) | NULLABLE | For settlement jobs |
| error_message | TEXT | NULLABLE | |

**Indexes:** `job_name`, `started_at DESC`

---

## Table 10: `daily_summary`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| summary_date | DATE | UNIQUE, NOT NULL | |
| total_transfers_initiated | INT | NOT NULL, DEFAULT 0 | |
| total_transfers_initiated_amount | NUMERIC(15,2) | NOT NULL, DEFAULT 0 | |
| total_transfers_settled | INT | NOT NULL, DEFAULT 0 | |
| total_transfers_settled_amount | NUMERIC(15,2) | NOT NULL, DEFAULT 0 | |
| fraud_alerts_raised | INT | NOT NULL, DEFAULT 0 | |
| new_user_registrations | INT | NOT NULL, DEFAULT 0 | |
| total_pending_settlement_amount | NUMERIC(15,2) | NOT NULL, DEFAULT 0 | |
| reconciliation_debit_sum | NUMERIC(15,2) | NULLABLE | |
| reconciliation_credit_sum | NUMERIC(15,2) | NULLABLE | |
| reconciliation_mismatch | BOOLEAN | NULLABLE | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:** `summary_date` (unique)

---

## Table 11: `settlement_batches`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| job_log_id | BIGINT | FK → scheduled_job_log.id, NULLABLE | Links to the job run |
| batch_date | DATE | NOT NULL | Date this batch ran |
| transactions_settled | INT | NOT NULL, DEFAULT 0 | |
| transactions_failed | INT | NOT NULL, DEFAULT 0 | |
| total_amount_settled | NUMERIC(15,2) | NOT NULL, DEFAULT 0 | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:** `batch_date`

---

## Table 12: `simulated_bank_accounts`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| user_id | BIGINT | FK → users.id, NOT NULL | |
| bank_name | VARCHAR(100) | NOT NULL | |
| account_nickname | VARCHAR(100) | NOT NULL | |
| account_identifier | VARCHAR(50) | UNIQUE, NOT NULL | System-generated demo account number |
| simulated_balance | NUMERIC(15,2) | NOT NULL, CHECK >= 0 | |
| is_active | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| version | BIGINT | NOT NULL, DEFAULT 0 | Optimistic lock |

**Indexes:** `user_id`, `account_identifier` (unique)

---

## Table 13: `auto_reload_settings`

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| wallet_id | BIGINT | FK → wallets.id, UNIQUE, NOT NULL | One config per wallet |
| is_enabled | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| simulated_account_id | BIGINT | FK → simulated_bank_accounts.id, NULLABLE | |
| threshold_amount | NUMERIC(15,2) | NULLABLE, CHECK > 0 | Reload when below this |
| reload_amount | NUMERIC(15,2) | NULLABLE, CHECK > 0 | Fixed amount to reload |
| reload_in_progress | BOOLEAN | NOT NULL, DEFAULT FALSE | Concurrency guard |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:** `wallet_id` (unique), `simulated_account_id`

---

## Table 14: `simulated_account_transactions` (Recommended Addition)

**Justification:** The PDF states "Simulated account balance changes must be traceable, including the amount, operation type, timestamp, and outcome." The main `transactions` table tracks wallet movements. A dedicated table provides a clean per-account audit trail for simulated balance changes (demo fund additions, wallet top-ups, auto-reloads).

| Column | Type | Constraints | Notes |
|---|---|---|---|
| id | BIGSERIAL | PK | |
| simulated_account_id | BIGINT | FK → simulated_bank_accounts.id, NOT NULL | |
| operation_type | VARCHAR(30) | NOT NULL | DEMO_FUND_ADDED, WALLET_TOP_UP, AUTO_RELOAD |
| amount | NUMERIC(15,2) | NOT NULL | Positive = credit; with sign for debit use direction |
| direction | VARCHAR(6) | NOT NULL | DEBIT or CREDIT |
| balance_before | NUMERIC(15,2) | NOT NULL | |
| balance_after | NUMERIC(15,2) | NOT NULL | |
| related_transaction_id | BIGINT | FK → transactions.id, NULLABLE | |
| outcome | VARCHAR(10) | NOT NULL | SUCCESS, FAILED |
| notes | VARCHAR(255) | NULLABLE | |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**Indexes:** `simulated_account_id`, `created_at DESC`

---

## Entity Relationships

```
users (1) ──── (1) wallets
users (1) ──── (N) simulated_bank_accounts
users (1) ──── (N) login_attempts
users (1) ──── (N) notifications
wallets (1) ──── (1) auto_reload_settings
wallets (1) ──── (N) transactions (as sender or receiver)
transactions (1) ──── (N) ledger_entries
transactions (1) ──── (0..1) fraud_alerts
transactions (1) ──── (N) simulated_account_transactions (via related_transaction_id)
simulated_bank_accounts (1) ──── (N) simulated_account_transactions
scheduled_job_log (1) ──── (N) settlement_batches
```

---

## Locking Strategy

| Operation | Strategy |
|---|---|
| Wallet balance update | Pessimistic row lock (`SELECT FOR UPDATE`) OR optimistic lock (`version` column) |
| Simulated account balance update | Pessimistic row lock (`SELECT FOR UPDATE`) |
| Auto Reload guard | `reload_in_progress = TRUE` flag + DB transaction |
| Idempotency check | `request_id` unique constraint with `INSERT ... ON CONFLICT` |
| Daily spending counter | Atomic `UPDATE wallets SET daily_spent = daily_spent + ? WHERE id = ? AND daily_spent + ? <= 50000` |

---

## Constraints Summary

- Wallet `balance >= 0`
- Wallet `daily_spent >= 0`
- Simulated account `simulated_balance >= 0`
- Transaction `amount > 0`
- Ledger entry `amount > 0`
- Auto reload `threshold_amount > 0`, `reload_amount > 0`
- `request_id` globally unique
- One wallet per user (`user_id` unique on wallets)
- One auto reload config per wallet (`wallet_id` unique on auto_reload_settings)
