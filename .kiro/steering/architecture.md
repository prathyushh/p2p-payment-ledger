---
inclusion: always
---

# Architecture Conventions

## Backend Package: com.p2ppayment

```
config/         - SecurityConfig, JwtConfig, SchedulerConfig
security/       - JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl
controller/     - REST controllers (flat for user; admin/ subdirectory for admin)
controller/admin/
service/        - Business logic (one service per major domain)
scheduler/      - Spring Scheduler jobs (4 jobs)
model/          - JPA entities (one per table)
repository/     - Spring Data JPA repositories (one per entity)
dto/request/    - Inbound DTOs
dto/response/   - Outbound DTOs
exception/      - Custom exceptions + GlobalExceptionHandler (@ControllerAdvice)
util/           - BusinessDayCalculator, AccountIdentifierGenerator
```

## Database Conventions
- Table names: snake_case, plural (users, wallets, transactions, ledger_entries, etc.)
- Column names: snake_case
- Primary keys: BIGSERIAL id
- Timestamps: TIMESTAMP NOT NULL DEFAULT NOW() for created_at and updated_at
- Soft deletes: use is_active boolean, never hard-delete user data
- All balance columns: NUMERIC(15,2) with CHECK >= 0 constraint
- Optimistic lock: version BIGINT on wallets and simulated_bank_accounts
- 14 tables: users, login_attempts, wallets, transactions, ledger_entries, fraud_alerts, notifications, audit_log, scheduled_job_log, daily_summary, settlement_batches, simulated_bank_accounts, auto_reload_settings, simulated_account_transactions
- Migrations: Flyway, files in src/main/resources/db/migration/, named V{n}__{description}.sql
- Never modify a migration that has already been applied

## API Conventions
- Base path: /api
- User endpoints: /api/wallet, /api/transactions, /api/bank-accounts, /api/auto-reload, /api/notifications
- Admin endpoints: /api/admin/**
- Auth: /api/auth/register, /api/auth/login, /api/auth/me
- Auth header: Authorization: Bearer <JWT>
- HTTP status codes: 200 OK, 201 Created, 400 validation, 401 unauthenticated, 403 forbidden, 404 not found, 409 conflict, 422 business rule violation, 423 locked, 500 server error
- Error response: { "error": "ERROR_CODE", "message": "...", "timestamp": "..." }
- Pagination: page (0-based), size (default 20, max 100)

## Security Architecture
- Spring Security stateless (no sessions)
- JWT filter on every request before controllers
- @PreAuthorize("hasRole('ADMIN')") on all admin endpoints
- Service layer checks ownership for user data (user can only see their own wallet, accounts, transactions)
- PIN verified by BCrypt compare, never stored plain

## Frontend Conventions
- Entry: frontend/src/main.jsx
- Router: React Router v6
- API calls: frontend/src/api/axiosClient.js (JWT interceptor auto-attaches token, 401 redirects to login)
- Per-domain API files: authApi.js, walletApi.js, transactionApi.js, bankAccountApi.js, notificationApi.js, adminApi.js
- Context: AuthContext (user + JWT), NotificationContext (unread count)
- Admin pages: frontend/src/pages/admin/
- User pages: frontend/src/pages/user/
