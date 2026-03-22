# AI Bug Investigation Prompt

## Context

You are diagnosing a bug in the **Gym Platform API**, a Java 17+ / Spring Boot 3.x
system running as 4 Docker-containerized microservices:

| Service       | Port | Container          | DB Schema       |
|---------------|------|--------------------|-----------------|
| Auth          | 8081 | gym-auth           | gym_auth        |
| Training      | 8082 | gym-training       | gym_training    |
| Tracking      | 8083 | gym-tracking       | gym_tracking    |
| Notification  | 8084 | gym-notification   | gym_notification|
| PostgreSQL    | 5432 | gym-postgres       | —               |
| API Gateway   | 8080 | gym-gateway        | —               |

**Auth flow**: JWT tokens issued by Auth service, validated by all services via shared
secret or public key. User context passed via `X-User-Id` and `X-User-Role` headers.

---

## Instructions

Follow this methodology **in order**. Do not skip steps.

### Step 1: Reproduce
- Define the exact request (method, URL, headers, body) that triggers the bug.
- Confirm whether the bug is deterministic or intermittent.
- Note the exact error response (status code, body, timestamp).

### Step 2: Isolate the Service
- Determine which service returns the error. Trace the request path:
  Client → Gateway (8080) → Target Service → Database / Downstream Service.
- If the gateway returns 502/503, the target service may be down or unresponsive.

### Step 3: Check Logs
Use these commands to inspect each layer:

```bash
docker-compose logs -f [service-name]                         # Service logs
docker-compose logs --since="2025-01-15T10:00:00" auth-service # Filter by time
docker exec gym-postgres tail -100 /var/log/postgresql/postgresql.log
```

### Step 4: Health & Connectivity Checks
```bash
curl -s http://localhost:{8081..8084}/actuator/health | jq .  # Per-service health
docker exec gym-postgres psql -U gym_user -d gym_auth -c "SELECT 1;"
docker-compose ps                                              # Container status
```

### Step 5: Diagnose
Match symptoms to common root causes:

| Symptom                          | Likely Cause                        | Check                           |
|----------------------------------|-------------------------------------|---------------------------------|
| 401 on valid credentials         | JWT expired or clock skew           | Token `exp` claim vs server time|
| 403 on authorized user           | RBAC role mismatch                  | X-User-Role header, @PreAuthorize|
| 500 from service                 | Unhandled exception                 | Service logs, stack trace       |
| 502/503 from gateway             | Target service down                 | `docker-compose ps`, health     |
| Connection refused               | Port conflict or service not started| `netstat -tlnp`, container logs |
| HikariPool timeout               | DB connection pool exhausted        | HikariCP metrics, active queries|
| Timeout between services         | Cross-service call hanging          | Downstream health, circuit breaker|
| Table not found / schema error   | Flyway migration failed             | `flyway info`, migration logs   |
| Env var `null` in logs           | Missing environment variable        | `docker exec <c> env`, .env file|

### Step 6: Fix & Verify
- Implement the minimal fix. Explain what changed and why.
- Re-run the reproduction request. Confirm the error is resolved.

### Step 7: Regression Test
- Write or update a test that covers the bug scenario.
- Run the relevant test suite to ensure no regressions.

---

## Expected Output Format

```markdown
## Bug Report: [Short Title]

### Symptoms
[What was observed — error code, message, affected endpoint]

### Reproduction Steps
1. [Exact steps to reproduce]

### Root Cause
[Service, file, line if known. Technical explanation of why it fails.]

### Fix Recommendation
[Code change, config change, or infrastructure adjustment needed]

### Files Affected
- `path/to/File.java` — [what changes]

### Verification
[How to confirm the fix works — specific curl command or test to run]

### Regression Test
[Test class and method name, or description of test to add]
```

---

## References

- [Troubleshooting Guides](../../docs/troubleshooting/)
