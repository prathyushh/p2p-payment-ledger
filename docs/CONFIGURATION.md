# P2P Payment Ledger — Configuration & Setup Guide

## Overview

The application has been configured with safe defaults to support development without mandatory environment variables.

## Configuration Status

**Applied Changes:**
- JWT secret now has development-safe default
- JWT expiration-ms has default (24 hours)
- Database connection has sensible local defaults
- Login attempt limits have defaults

**Files Modified:**
- backend/src/main/java/com/p2ppayment/util/JwtTokenProvider.java
- backend/src/main/resources/application.yml

## Backend Startup

### Spring Tool Suite
1. Import project: File > Import > Existing Maven Project
2. Select backend/ directory
3. Right-click > Run As > Spring Boot App
4. No environment variables required

### Maven Command Line
See CONFIGURATION.md setup section for command.

## Verification Results

- Backend: mvn clean compile -DskipTests ? BUILD SUCCESS
- Frontend build: npm run build ? SUCCESS
- Frontend lint: npm run lint ? OK (warnings only)

## Unresolved

- Database startup and Flyway migration verification (requires running app)
- Frontend-backend integration (requires running both services)
- Admin bootstrap (requires JWT secret environment)

See docs/progress.md for task status.
