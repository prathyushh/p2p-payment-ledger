---
inclusion: always
---

# Project: P2P Payment Ledger

## Purpose
A simulated peer-to-peer digital wallet. Users register, load money via simulated bank accounts, transfer money to other users, and track transactions. No real bank integration.

## Technology Stack
- **Backend:** Java 21, Spring Boot 3.x, Spring Data JPA, Spring Security (JWT), Spring Scheduler
- **Database:** PostgreSQL 16, Flyway migrations
- **Frontend:** React 18 + Vite, Axios, Recharts
- **Build:** Maven (backend), npm (frontend)

## Core Functional Areas
1. User registration + JWT login + account lockout (5 failures → 30-min lock)
2. Wallet (ACTIVE/INACTIVE/FROZEN) with balance and daily spending tracking
3. Simulated bank accounts (create, fund, deactivate)
4. Manual wallet top-up from simulated account (atomic)
5. Auto Reload (automatic top-up when balance < threshold; concurrency-safe)
6. P2P transfers with 3-business-day ACH delayed settlement
7. Fraud detection: daily limit (Rs 50,000), duplicate (2 min), high-value flag (>= Rs 8,000)
8. Transaction history with pagination and filters
9. In-app notifications (12 event types)
10. 4 scheduled jobs: settlement (hourly), daily limit reset (midnight), auto-unlock (every 5 min), daily summary/reconciliation (11 PM)
11. Admin panel: dashboard, user management, fraud review, settlement monitor, job logs

## Critical Business Rules
- Daily send limit: Rs 50,000 per user per day
- Duplicate window: 2 minutes (same sender + receiver + amount)
- High-value threshold: Rs 8,000 (alert only, does not block)
- Settlement: 3 business days (weekdays only)
- Account lock: 5 consecutive failures, 30-minute lock
- Wallet balance and simulated account balances MUST NEVER go negative
- Auto Reload must not create duplicate reloads (use reload_in_progress flag)
- Auto Reload must not trigger recursively from its own credit

## Unresolved Questions
See docs/requirements-analysis.md section "Unresolved Questions & Ambiguities" (UQ-01 through UQ-10).
Key: UQ-02 through UQ-09 have assumed defaults — see docs/decisions.md for full context. Owner must confirm before implementation proceeds beyond TASK-001/TASK-003.

