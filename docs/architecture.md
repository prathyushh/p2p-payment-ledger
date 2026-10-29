# Architecture — P2P Payment Ledger

**Status:** PLANNED (not yet implemented)

---

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        React Frontend                        │
│   (Vite + React 18 + React Router + Axios + Recharts)       │
└──────────────────────────┬──────────────────────────────────┘
                           │  HTTPS REST (JWT in Authorization header)
┌──────────────────────────▼──────────────────────────────────┐
│                  Spring Boot 3.x API Server                  │
│                                                              │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   Security Layer                        ││
│  │  Spring Security + JWT Filter + Role Guards             ││
│  └──────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   REST Controllers                      ││
│  │  Auth / Wallet / Transactions / BankAccounts /          ││
│  │  Notifications / Admin / Fraud                          ││
│  └──────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   Service Layer                         ││
│  │  Business logic, fraud checks, auto reload,             ││
│  │  settlement, idempotency                                ││
│  └──────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   Repository Layer                      ││
│  │  Spring Data JPA Repositories                           ││
│  └──────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Scheduled Jobs (Spring Scheduler)          ││
│  │  Settlement / DailyReset / AutoUnlock / DailySummary    ││
│  └──────────────────────────────────────────────────────────┘│
└──────────────────────────┬──────────────────────────────────┘
                           │  JDBC / JPA
┌──────────────────────────▼──────────────────────────────────┐
│                      PostgreSQL 16                           │
│                  (14 tables, see database-design.md)         │
└─────────────────────────────────────────────────────────────┘
```

---

## Backend Package Structure

```
backend/
└── src/main/java/com/p2ppayment/
    ├── P2pPaymentApplication.java          # Main entry point
    ├── config/
    │   ├── SecurityConfig.java             # Spring Security config
    │   ├── JwtConfig.java                  # JWT properties
    │   └── SchedulerConfig.java            # Task scheduler config
    ├── security/
    │   ├── JwtTokenProvider.java           # Generate/validate JWT
    │   ├── JwtAuthenticationFilter.java    # Request filter
    │   └── UserDetailsServiceImpl.java     # Load user by email
    ├── controller/
    │   ├── AuthController.java             # /api/auth/**
    │   ├── WalletController.java           # /api/wallet/**
    │   ├── TransactionController.java      # /api/transactions/**
    │   ├── BankAccountController.java      # /api/bank-accounts/**
    │   ├── NotificationController.java     # /api/notifications/**
    │   └── admin/
    │       ├── AdminUserController.java    # /api/admin/users/**
    │       ├── AdminTransactionController.java
    │       ├── AdminFraudController.java
    │       ├── AdminSettlementController.java
    │       ├── AdminJobController.java
    │       └── AdminDashboardController.java
    ├── service/
    │   ├── AuthService.java
    │   ├── WalletService.java              # Balance ops, status changes
    │   ├── TransactionService.java         # Send money, idempotency
    │   ├── SettlementService.java          # Settlement logic
    │   ├── FraudService.java               # Fraud check pipeline
    │   ├── AutoReloadService.java          # Reload logic
    │   ├── BankAccountService.java         # Simulated accounts
    │   ├── NotificationService.java        # Create/query notifications
    │   └── AuditService.java               # Write audit log entries
    ├── scheduler/
    │   ├── AchSettlementJob.java           # Every hour
    │   ├── DailyLimitResetJob.java         # Midnight
    │   ├── AccountAutoUnlockJob.java       # Every 5 minutes
    │   └── DailySummaryJob.java            # 11 PM
    ├── model/
    │   ├── User.java
    │   ├── LoginAttempt.java
    │   ├── Wallet.java
    │   ├── Transaction.java
    │   ├── LedgerEntry.java
    │   ├── FraudAlert.java
    │   ├── Notification.java
    │   ├── AuditLog.java
    │   ├── ScheduledJobLog.java
    │   ├── DailySummary.java
    │   ├── SettlementBatch.java
    │   ├── SimulatedBankAccount.java
    │   ├── AutoReloadSettings.java
    │   └── SimulatedAccountTransaction.java
    ├── repository/
    │   ├── UserRepository.java
    │   ├── LoginAttemptRepository.java
    │   ├── WalletRepository.java
    │   ├── TransactionRepository.java
    │   ├── LedgerEntryRepository.java
    │   ├── FraudAlertRepository.java
    │   ├── NotificationRepository.java
    │   ├── AuditLogRepository.java
    │   ├── ScheduledJobLogRepository.java
    │   ├── DailySummaryRepository.java
    │   ├── SettlementBatchRepository.java
    │   ├── SimulatedBankAccountRepository.java
    │   ├── AutoReloadSettingsRepository.java
    │   └── SimulatedAccountTransactionRepository.java
    ├── dto/
    │   ├── request/                        # Inbound DTOs
    │   └── response/                       # Outbound DTOs
    ├── exception/
    │   ├── GlobalExceptionHandler.java     # @ControllerAdvice
    │   ├── InsufficientFundsException.java
    │   ├── WalletInactiveException.java
    │   ├── WalletFrozenException.java
    │   ├── DailyLimitExceededException.java
    │   ├── DuplicateTransactionException.java
    │   └── IdempotencyConflictException.java
    └── util/
        ├── BusinessDayCalculator.java      # 3 business-day logic
        └── AccountIdentifierGenerator.java # Demo account number gen
```

---

## Frontend Structure

```
frontend/
├── public/
└── src/
    ├── main.jsx                    # Entry point
    ├── App.jsx                     # Router + layout
    ├── api/
    │   ├── axiosClient.js          # Axios instance with JWT interceptor
    │   ├── authApi.js
    │   ├── walletApi.js
    │   ├── transactionApi.js
    │   ├── bankAccountApi.js
    │   ├── notificationApi.js
    │   └── adminApi.js
    ├── context/
    │   ├── AuthContext.jsx         # JWT, user info
    │   └── NotificationContext.jsx # Unread count
    ├── components/
    │   ├── layout/
    │   │   ├── Header.jsx          # Bell icon, unread count
    │   │   ├── Sidebar.jsx
    │   │   └── ProtectedRoute.jsx  # Requires auth
    │   └── ui/                     # Shared UI components
    ├── pages/
    │   ├── auth/
    │   │   ├── LoginPage.jsx
    │   │   └── RegisterPage.jsx
    │   ├── user/
    │   │   ├── DashboardPage.jsx   # Wallet balance, quick actions
    │   │   ├── WalletPage.jsx      # Balance, status, top-up
    │   │   ├── SendMoneyPage.jsx   # Search, enter amount, PIN
    │   │   ├── TransactionsPage.jsx # History with filters
    │   │   ├── BankAccountsPage.jsx # Manage simulated accounts
    │   │   ├── AutoReloadPage.jsx  # Configure auto reload
    │   │   └── NotificationsPage.jsx
    │   └── admin/
    │       ├── AdminDashboardPage.jsx
    │       ├── AdminUsersPage.jsx
    │       ├── AdminTransactionsPage.jsx
    │       ├── AdminSettlementPage.jsx
    │       ├── AdminFraudPage.jsx
    │       ├── AdminJobsPage.jsx
    │       └── AdminReconciliationPage.jsx
    └── utils/
        ├── formatCurrency.js
        ├── formatDate.js
        └── constants.js
```

---

## Security Flow

```
1. User POSTs /api/auth/login
2. AuthController calls AuthService
3. AuthService validates credentials and checks account lock
4. On success: generates JWT (subject = userId, claim = role)
5. JWT returned to client

6. Client includes JWT in "Authorization: Bearer <token>" header
7. JwtAuthenticationFilter intercepts every request
8. Filter validates JWT signature and expiry
9. Sets SecurityContext with UserDetails + authorities
10. Controller-level @PreAuthorize("hasRole('ADMIN')") guards admin endpoints
11. Service layer verifies ownership for user-specific data
```

---

## Major Application Workflows

### Send Money Flow
```
Client → POST /api/transactions/send
  → Idempotency check (request_id already used?)
  → Validate PIN
  → Check sender wallet ACTIVE
  → Fraud check: duplicate detection (2 min window)
  → Fraud check: daily limit (₹50,000)
  → Deduct sender balance (atomic DB transaction)
  → Create DEBIT ledger entry
  → Create transaction record (PENDING, settlement_due_date = +3 business days)
  → If amount > ₹8,000: create HIGH_VALUE fraud alert
  → Create "Transfer initiated" notification for sender
  → Trigger Auto Reload check for sender
  → Return transaction response
```

### ACH Settlement Job Flow
```
@Scheduled(cron = "0 0 * * * *")  // every hour
  → Query transactions WHERE status=PENDING AND settlement_due_date <= today
  → Log job start
  → For each transaction:
      → Check receiver wallet status (ACTIVE / INACTIVE / FROZEN)
      → If INACTIVE or FROZEN: mark DECLINED, refund sender, notify sender
      → If ACTIVE: credit receiver balance, create CREDIT entry, status→SUCCESS
      → Notify sender and receiver
  → Log job end with counts
  → Create SettlementBatch record
```

### Auto Reload Flow
```
After any wallet balance decrease:
  → Is wallet ACTIVE? No → skip
  → Is Auto Reload enabled? No → skip
  → Is balance < threshold? No → skip
  → Acquire reload lock (set reload_in_progress = TRUE, fail if already TRUE)
  → Check simulated account has sufficient funds
  → If yes: deduct from simulated account, credit wallet, create AUTO_RELOAD transaction
  → Release reload lock
  → Create notification (success or failure)
  → Do NOT trigger another Auto Reload from the resulting credit
```

---

## Technology Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Backend framework | Spring Boot 3.x | Required by spec |
| ORM | Spring Data JPA + Hibernate | Required by spec |
| Auth | Spring Security + JWT | Required by spec |
| Scheduler | Spring Scheduler | Required by spec |
| Database | PostgreSQL 16 | Required by spec |
| Frontend | React 18 + Vite | Required by spec (React) |
| Migration | Flyway | Standard Spring Boot migration tool |
| Charts | Recharts | Lightweight React charting |
| HTTP client | Axios | Standard React HTTP client |
| State management | React Context API | Sufficient for this scope |
| Build tool | Maven (backend) | Standard Java build |
| API style | REST + JSON | Standard, matches JWT auth |
