# Task: Bug Fix

## Prerequisites
- [ ] Bug is reproducible (manually or via failing test)
- [ ] Target service identified
- [ ] On a clean git branch or new branch: `git checkout -b fix/[short-description]`

## Workflow

### 1. Reproduce the Bug
- Confirm the bug via API call, test, or logs
- Document: endpoint, request body, expected vs actual behavior
- Note the HTTP status code and error message if applicable

### 2. Write a Failing Test
```bash
mvn test -pl [service] -Dtest=[TestClassName]#[testMethod]
```
- Write a test that exposes the exact bug
- The test MUST fail before the fix — this proves it catches the bug
- Follow `ai/skills/test-generation.md` for correct test structure

### 3. Diagnose Root Cause
- **Check logs:**
  ```bash
  docker-compose logs -f [service-name]
  ```
- **Check health:**
  ```bash
  curl http://localhost:[port]/actuator/health
  ```
- **Check database:**
  ```bash
  docker exec -it gym-postgres psql -U gym_admin -d gym_db -c "SELECT ..."
  ```
- **Check JWT (Auth issues):**
  - Decode token at jwt.io or via logs
  - Verify claims, expiration, roles
- **Trace the call path:** Controller → Service → Repository → DB

### 4. Implement Fix
- Make the minimal change that fixes the root cause
- Do NOT refactor unrelated code in the same change
- Do NOT add features alongside the fix
- Ensure custom exceptions are thrown (not generic ones)

### 5. Verify Test Passes
```bash
mvn test -pl [service] -Dtest=[TestClassName]#[testMethod]
```
- The previously failing test must now pass

### 6. Run Full Test Suite
```bash
mvn test -pl [service]
```
- All existing tests must still pass (no regressions)
- If any test broke, the fix has side effects — re-examine

### 7. Commit
```bash
git add -A
git commit -m "fix(scope): description of what was fixed"
```
- Scope = affected module, service, or feature area
- Description = what the user-facing problem was

## Diagnostic Tools Quick Reference
| Tool                       | Command                                              |
|----------------------------|------------------------------------------------------|
| Service logs               | `docker-compose logs -f [service]`                   |
| All error logs             | `docker-compose logs --since=10m \| grep -i error`   |
| Health check (Auth)        | `curl http://localhost:8081/actuator/health`          |
| Health check (Training)    | `curl http://localhost:8082/actuator/health`          |
| Health check (Tracking)    | `curl http://localhost:8083/actuator/health`          |
| Health check (Notification)| `curl http://localhost:8084/actuator/health`          |
| DB query                   | `docker exec -it gym-postgres psql -U gym_admin -d gym_db` |
| Container status           | `docker-compose ps`                                  |

## Completion Checklist
- [ ] Regression test written and included in commit
- [ ] Failing test confirmed before fix
- [ ] Fix is minimal and targeted
- [ ] Test passes after fix
- [ ] Full test suite passes (no regressions)
- [ ] No unrelated changes in the commit
- [ ] Commit follows `fix(scope): description` format

## References
- `ai/skills/test-generation.md` — writing the regression test
- `ai/skills/code-analysis.md` — analyzing the affected code
- `ai/rules/coding-standards.md` — error handling patterns
- `ai/rules/testing-standards.md`
