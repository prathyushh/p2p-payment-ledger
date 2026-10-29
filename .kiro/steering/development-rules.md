---
inclusion: always
---

# Development Rules

## Before Starting Any Task
1. Read docs/progress.md to know the current state.
2. Read docs/task-checklist.md to identify the next incomplete task.
3. Read the relevant task in docs/implementation-plan.md.
4. Inspect actual source files and Git state. Do NOT trust progress.md if code contradicts it.
5. Run mvn test (backend) or npm test (frontend) to verify baseline before changes.

## Implementation Rules
- Implement ONE task at a time. Complete it fully before moving on.
- Respect task dependencies — never skip a dependency task.
- All balance-changing operations MUST be inside @Transactional methods.
- Auto Reload: always call attemptReload() after every wallet balance DECREASE. Never call it after a reload credit.
- Idempotency: always check request_id before processing a new send-money transaction.
- PIN validation: BCrypt compare, reject immediately if wrong, do not reveal which field is wrong in the error.
- Never store plain-text passwords or PINs.
- Never allow wallet or simulated account balance to go below zero.
- Log all admin actions to audit_log.

## Testing Rules
- Run all tests after each task: mvn test (backend), npm test (frontend).
- Do NOT mark a task complete in task-checklist.md unless tests pass.
- If tests fail, leave the task as [ ] and record the failure in docs/progress.md.
- Write unit tests for every service method. Write integration tests for every complete flow.
- Use TestContainers (PostgreSQL) for integration tests — not H2.

## Git Rules
- Commit after each completed and verified task.
- Commit message format: "TASK-XXX: short description of what was implemented"
- Never commit secrets, .env files, or application-local.yml.
- Never force-push to main.

## Progress Tracking
- Update docs/task-checklist.md: mark [x] only after implementation + tests pass.
- Update docs/progress.md after every task: current task, last completed, files changed, test results, next task.
- Record any new known issues or unresolved questions in docs/progress.md.

## Security Rules
- JWT secret must come from environment variable, not hardcoded.
- Admin endpoints must have @PreAuthorize("hasRole('ADMIN')").
- User endpoints must verify the requesting user owns the resource.
- Input validation on all DTOs with @Valid and Bean Validation annotations.
- Use parameterized queries (JPA) — never string concatenation in queries.

## Coding Standards
- Java: follow standard Spring Boot conventions. Use Lombok for boilerplate.
- Method names: camelCase. Entity/class names: PascalCase.
- No magic numbers — use named constants or configuration values.
- Exception handling: throw specific custom exceptions from service layer. Handle in GlobalExceptionHandler.
- DTOs: never return JPA entities directly from controllers — always map to response DTOs.

## Session Limits
- Before ending a session due to context limits: update docs/progress.md with exact current task status, files changed, next action.
- On resumption: re-read docs/progress.md and verify against actual code before continuing.
