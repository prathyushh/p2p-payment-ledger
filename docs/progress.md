# Progress — P2P Payment Ledger

---

## Overall Status: IN PROGRESS

---

## Current Phase
**Phase:** PHASE-02 — Auth & Wallet Core — IN PROGRESS
**Current Task:** TASK-005 — login attempt tracking and account lockout
**Last Completed Task:** TASK-004 — user registration, JWT login, Spring Security config

---

## Phase & Task Status

| Phase | Status | Notes |
|---|---|---|
| PHASE-01: Project Setup | COMPLETE | All 3 tasks done and verified |
| PHASE-02: Auth & Wallet Core | IN PROGRESS | 1/4 tasks done (TASK-004) |
| PHASE-03: Simulated Accounts & Auto Reload | NOT STARTED | Depends on PHASE-02 |
| PHASE-04: P2P Transfers, Fraud & Settlement | NOT STARTED | Depends on PHASE-02 |
| PHASE-05: Admin Panel & Remaining Jobs | NOT STARTED | Depends on PHASE-04 |
| PHASE-06: Frontend | NOT STARTED | Depends on PHASE-05 |
| PHASE-07: Testing & Hardening | NOT STARTED | Depends on all previous |

---

## Completed Tasks

- [x] TASK-001 — Backend Spring Boot 3.4.1 initialized. mvn test (1/0/0) PASS.
- [x] TASK-002 — 14 Flyway migrations executed. mvn test (1/0/0) PASS.
- [x] TASK-003 — Frontend React + Vite + Axios. npm run build PASS.
- [x] TASK-004 — User/Wallet entities, JWT provider, SecurityConfig, AuthService, AuthController, AdminBootstrapService. mvn test (1/0/0) PASS.

---

## In-Progress Tasks

None — TASK-004 complete; ready for TASK-005.

---

## Tests Executed

| Task | Test | Result | Date |
|---|---|---|---|
| TASK-001 | mvn test — Spring context loads | PASSED: 1/0/0 | 2026-10-29 |
| TASK-002 | flyway:migrate — 14 migrations | PASSED: 14/14 tables | 2026-10-29 |
| TASK-003 | npm run build — Vite production | PASSED: 102 modules | 2026-10-29 |
| TASK-004 | mvn test — Spring context, admin bootstrap | PASSED: 1/0/0 | 2026-10-29 |

---

## Known Issues

- KI-05: jjwt 0.12.6 API changed. Fixed: Jwts.parser().verifyWith(key).build().parseSignedClaims().
- KI-06: Lombok @Builder.Default required on fields with defaults. Fixed in User and Wallet.
- KI-07: Mockito 5.x dynamic agent on Java 21 generates warnings but tests pass.

---

## Important Implementation Decisions

| Decision | Description |
|---|---|
| D-13 | jjwt 0.12.6 API: Jwts.parser().verifyWith(key).build().parseSignedClaims() |
| D-14 | Lombok @Builder.Default on fields with default values |

---

## TASK-004 Implementation Summary

**Completed:**
1. User entity (name, email, phone, password, PIN, role, enabled, accountLocked, lockedUntil, createdAt)
2. Wallet entity (balance, dailySpent, dailyLimit, status, createdAt, OneToOne User)
3. Repositories (UserRepository, WalletRepository)
4. JWT Provider (generateToken, validateToken, claim extraction)
5. Spring Security Config (CORS, CSRF disabled, stateless, JWT filter)
6. Auth Service (registerUser, authenticateUser)
7. Auth Controller (POST /api/auth/register, POST /api/auth/login)
8. Custom User Details Service (loadUserByUsername)
9. Admin Bootstrap Service (idempotent admin from env vars)
10. DTOs (RegisterRequest/Response, AuthRequest/Response, UserResponse)

**Verification:** mvn clean test PASS (1/0/0), admin bootstrap confirmed.

---

## Next Task

**TASK-005** — Login attempt tracking and 5-strike account lockout.