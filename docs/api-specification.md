# API Specification — P2P Payment Ledger

**Status:** PLANNED (not yet implemented)  
**Base URL:** `/api`  
**Auth:** `Authorization: Bearer <JWT>` on all protected endpoints

---

## Authentication

### POST `/api/auth/register`
**Auth:** Public

**Request:**
```json
{
  "name": "Alice Kumar",
  "email": "alice@example.com",
  "phone_number": "9876543210",
  "password": "SecurePass123!",
  "transaction_pin": "1234"
}
```

**Validation:** name required, email format, phone 10 digits, password min 8 chars, PIN exactly 4 digits.

**Response 201:**
```json
{
  "token": "eyJhbGci...",
  "user": { "id": 1, "name": "Alice Kumar", "email": "alice@example.com", "role": "USER" }
}
```

**Errors:** 400 (validation), 409 (email/phone already registered)

---

### POST `/api/auth/login`
**Auth:** Public

**Request:**
```json
{ "email": "alice@example.com", "password": "SecurePass123!" }
```

**Response 200:**
```json
{ "token": "eyJhbGci...", "user": { "id": 1, "name": "Alice Kumar", "role": "USER" } }
```

**Errors:** 401 (invalid credentials), 423 (account locked, includes unlock time)

---

### GET `/api/auth/me`
**Auth:** USER, ADMIN

**Response 200:**
```json
{ "id": 1, "name": "Alice Kumar", "email": "alice@example.com", "phone_number": "9876543210", "role": "USER" }
```

---

## Wallet

### GET `/api/wallet`
**Auth:** USER

**Response 200:**
```json
{
  "id": 1,
  "balance": 2400.00,
  "status": "ACTIVE",
  "daily_spent": 800.00,
  "daily_limit": 50000.00,
  "daily_remaining": 49200.00
}
```

---

### POST `/api/wallet/deactivate`
**Auth:** USER  
**Body:** None  
**Response 200:** `{ "status": "INACTIVE" }`  
**Errors:** 409 (already inactive)

---

### POST `/api/wallet/reactivate`
**Auth:** USER  
**Body:** None  
**Response 200:** `{ "status": "ACTIVE" }`

---

### POST `/api/wallet/top-up`
**Auth:** USER

**Request:**
```json
{ "simulated_account_id": 3, "amount": 1000.00 }
```

**Validation:** amount > 0, simulated account belongs to user, sufficient balance, wallet ACTIVE.

**Response 200:**
```json
{
  "transaction_id": 42,
  "new_wallet_balance": 3400.00,
  "new_account_balance": 7000.00
}
```

**Errors:** 422 (insufficient simulated funds), 400 (invalid wallet status)

---

## Simulated Bank Accounts

### GET `/api/bank-accounts`
**Auth:** USER  
**Response 200:** Array of accounts belonging to the logged-in user.

---

### POST `/api/bank-accounts`
**Auth:** USER

**Request:**
```json
{ "bank_name": "HDFC Bank", "account_nickname": "My HDFC", "initial_balance": 10000.00 }
```

**Response 201:**
```json
{
  "id": 3,
  "bank_name": "HDFC Bank",
  "account_nickname": "My HDFC",
  "account_identifier": "DEMO-84729301",
  "simulated_balance": 10000.00,
  "is_active": true
}
```

---

### POST `/api/bank-accounts/{id}/demo-funds`
**Auth:** USER

**Request:**
```json
{ "amount": 5000.00 }
```

**Response 200:**
```json
{ "new_balance": 15000.00 }
```

**Errors:** 400 (amount <= 0), 403 (not your account), 404 (account not found)

---

### PUT `/api/bank-accounts/{id}`
**Auth:** USER  
Update nickname or bank name.

---

### POST `/api/bank-accounts/{id}/deactivate`
**Auth:** USER  
**Errors:** 409 (account is linked to active Auto Reload)

---

## Auto Reload

### GET `/api/auto-reload`
**Auth:** USER  
**Response 200:** Current Auto Reload settings for the user's wallet.

---

### PUT `/api/auto-reload`
**Auth:** USER

**Request:**
```json
{
  "is_enabled": true,
  "simulated_account_id": 3,
  "threshold_amount": 500.00,
  "reload_amount": 2000.00
}
```

**Validation:** If enabled, all three fields required and > 0. Wallet must be ACTIVE.

**Response 200:** Updated settings.

---

## Transactions

### POST `/api/transactions/send`
**Auth:** USER

**Request:**
```json
{
  "request_id": "uuid-client-generated",
  "recipient_identifier": "9876543211",
  "amount": 500.00,
  "remark": "Dinner split",
  "transaction_pin": "1234"
}
```

**Notes:** `recipient_identifier` can be phone or email.

**Response 200:**
```json
{
  "transaction_id": 55,
  "status": "PENDING",
  "amount": 500.00,
  "settlement_due_date": "2025-01-06",
  "days_until_settlement": 3,
  "new_sender_balance": 1900.00
}
```

**Errors:** 400 (invalid PIN), 422 (insufficient funds, wallet inactive/frozen, daily limit exceeded), 409 (duplicate request_id returns original result with 200), 404 (recipient not found)

---

### GET `/api/transactions`
**Auth:** USER  
**Query Params:** `page` (0-based), `size` (default 20, max 100), `type` (P2P_TRANSFER/WALLET_TOP_UP/AUTO_RELOAD/all), `status` (PENDING/SUCCESS/FAILED/DECLINED/all), `from` (ISO date), `to` (ISO date)

**Response 200:**
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "total_elements": 150,
  "total_pages": 8
}
```

---

### GET `/api/transactions/mini-statement`
**Auth:** USER  
**Response 200:** Last 10 transactions (no pagination).

---

### GET `/api/transactions/{id}`
**Auth:** USER  
**Response 200:** Full transaction details including ledger entries.

---

### GET `/api/transactions/stats`
**Auth:** USER  
**Response 200:**
```json
{
  "total_sent": 12500.00,
  "total_received": 8000.00,
  "this_month_count": 7
}
```

---

## Notifications

### GET `/api/notifications`
**Auth:** USER  
**Query Params:** `page`, `size`, `unread_only` (boolean)

---

### GET `/api/notifications/unread-count`
**Auth:** USER  
**Response 200:** `{ "count": 3 }`

---

### PUT `/api/notifications/{id}/read`
**Auth:** USER

---

### PUT `/api/notifications/read-all`
**Auth:** USER

---

## Admin — Dashboard

### GET `/api/admin/dashboard`
**Auth:** ADMIN  
**Response 200:** Total users, today's transfer counts, volumes, pending settlement amount, pending fraud alerts, last 30-day chart data.

---

## Admin — Users

### GET `/api/admin/users`
**Auth:** ADMIN  
**Query Params:** `page`, `size`, `search` (name/email/phone)

---

### GET `/api/admin/users/{id}/transactions`
**Auth:** ADMIN  
**Response 200:** Recent transactions for the user.

---

### POST `/api/admin/users/{id}/wallet/freeze`
**Auth:** ADMIN  
**Audited.**

---

### POST `/api/admin/users/{id}/wallet/unfreeze`
**Auth:** ADMIN  
**Audited.**

---

## Admin — Transactions

### GET `/api/admin/transactions`
**Auth:** ADMIN  
**Query Params:** `page`, `size`, `status`, `type`, `from`, `to`, `min_amount`, `max_amount`

---

### GET `/api/admin/transactions/{id}`
**Auth:** ADMIN  
**Response 200:** Full details including ledger entries.

---

## Admin — Settlement

### GET `/api/admin/settlement/pending`
**Auth:** ADMIN  
**Response 200:** All pending transactions with settlement dates and overdue flags.

---

### POST `/api/admin/settlement/trigger/{transactionId}`
**Auth:** ADMIN  
**Response 200:** Settles the specific eligible pending transaction.  
**Audited.**

---

## Admin — Fraud

### GET `/api/admin/fraud-alerts`
**Auth:** ADMIN  
**Query Params:** `status` (PENDING/BLOCKED/DISMISSED/all)

---

### POST `/api/admin/fraud-alerts/{id}/block`
**Auth:** ADMIN  
**Audited.**

---

### POST `/api/admin/fraud-alerts/{id}/dismiss`
**Auth:** ADMIN  
**Audited.**

---

## Admin — Jobs

### GET `/api/admin/jobs`
**Auth:** ADMIN  
**Response 200:** Last 20 scheduled job runs.

---

## Admin — Reconciliation

### GET `/api/admin/reconciliation`
**Auth:** ADMIN  
**Response 200:** Today's debit/credit totals, pending settlement amount, mismatch flag.

---

## Common Error Response Format

```json
{
  "error": "INSUFFICIENT_FUNDS",
  "message": "Simulated account balance is insufficient for this operation.",
  "timestamp": "2025-01-01T12:00:00Z"
}
```

## HTTP Status Codes Used

| Code | Meaning |
|---|---|
| 200 | OK |
| 201 | Created |
| 400 | Bad request / validation error |
| 401 | Unauthenticated |
| 403 | Forbidden (wrong role or not your resource) |
| 404 | Resource not found |
| 409 | Conflict (duplicate, already exists) |
| 422 | Unprocessable entity (business rule violation) |
| 423 | Locked (account locked) |
| 500 | Internal server error |
