# Task Checklist — P2P Payment Ledger

**Status:** IN PROGRESS
**Last updated:** TASK-011 complete (2026-10-29)
**Rule:** Only mark \[x]\ after implementation is complete AND verification (build + tests) has passed.

---

## PHASE-01: Project Setup & Infrastructure

- [x] TASK-001 — Initialize backend Spring Boot project with all dependencies
- [x] TASK-002 — Create all 14 database tables via Flyway migrations (V1-V14; no V15 admin seed — handled by AdminBootstrapService in TASK-004)
- [x] TASK-003 — Initialize frontend React + Vite project with axios and router

---

## PHASE-02: Auth, Users & Wallet Core

- [x] TASK-004 — Implement user registration, JWT login, Spring Security config
- [x] TASK-005 — Implement login attempt tracking and 5-strike account lockout
- [x] TASK-006 — Implement wallet status management (deactivate/reactivate/freeze/unfreeze)
- [x] TASK-007 — Implement GET /api/wallet (balance, status, daily spending)

---

## PHASE-03: Simulated Accounts, Top-Up & Auto Reload

- [x] TASK-008 — Implement simulated bank account CRUD and Add Demo Funds
- [x] TASK-009 — Implement atomic manual wallet top-up from simulated account
- [x] TASK-010 — Implement Auto Reload configuration (GET/PUT settings)
- [x] TASK-011 — Implement Auto Reload behavior, concurrency guard, and anti-recursion

---

## PHASE-04: P2P Transfers, Fraud & Settlement

- [ ] TASK-012 — Implement send money with idempotency, PIN validation, and delayed settlement
- [ ] TASK-013 — Implement fraud detection (daily limit, duplicate, high-value)
- [ ] TASK-014 — Implement ACH Settlement Scheduler (hourly, with logging)
- [ ] TASK-015 — Implement admin fraud review (block/dismiss)
- [ ] TASK-016 — Implement transaction history, mini-statement, detail view, and stats

---

## PHASE-05: Remaining Jobs, Admin Panel & Notifications

- [ ] TASK-017 — Implement Daily Limit Reset, Account Auto-Unlock, Daily Summary/Reconciliation jobs
- [ ] TASK-018 — Implement complete notifications system (all 12 events + query endpoints)
- [ ] TASK-019 — Implement admin dashboard and user management endpoints
- [ ] TASK-020 — Implement admin transaction monitoring and settlement monitor
- [ ] TASK-021 — Implement admin job monitor and reconciliation endpoints

---

## PHASE-06: Frontend

- [ ] TASK-022 — Build login and registration pages with JWT auth flow
- [ ] TASK-023 — Build user dashboard and wallet page (balance, top-up, status)
- [ ] TASK-024 — Build simulated bank accounts page (create, view, demo funds, deactivate)
- [ ] TASK-025 — Build Auto Reload configuration page
- [ ] TASK-026 — Build send money page (recipient search, PIN confirmation, idempotency)
- [ ] TASK-027 — Build transaction history page with pagination and filters
- [ ] TASK-028 — Build notifications (bell icon, unread count, mark as read)
- [ ] TASK-029 — Build all admin pages (dashboard chart, users, transactions, settlement, fraud, jobs, reconciliation)

---

## PHASE-07: Integration, Testing & Hardening

- [ ] TASK-030 — Run full end-to-end integration test suite (TestContainers)
- [ ] TASK-031 — Concurrency and edge case hardening (Auto Reload, daily limit)
- [ ] TASK-032 — Security hardening (authorization, input validation, error responses)
- [ ] TASK-033 — Final cleanup, README update with verified instructions

---

## Summary

| Phase | Total Tasks | Completed |
|---|---|---|
| PHASE-01 | 3 | 3 |
| PHASE-02 | 4 | 4 |
| PHASE-03 | 4 | 4 |
| PHASE-04 | 5 | 0 |
| PHASE-05 | 5 | 0 |
| PHASE-06 | 8 | 0 |
| PHASE-07 | 4 | 0 |
| **Total** | **33** | **11** |