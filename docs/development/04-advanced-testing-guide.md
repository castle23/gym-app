# Advanced Testing Guide

> **Target Audience:** QA Engineers, DevOps, Performance Engineers, Security Team

This guide covers advanced testing strategies beyond unit and integration testing. These techniques ensure the Gym Platform API maintains high performance, security, and reliability under real-world conditions.

---

## Table of Contents

1. [Introduction](#introduction)
2. [Load Testing](#load-testing)
3. [Performance Testing](#performance-testing)
4. [Security Testing](#security-testing)
5. [Chaos Engineering](#chaos-engineering)
6. [Contract Testing](#contract-testing)
7. [Running & Reporting](#running--reporting)
8. [Troubleshooting](#troubleshooting)

---

## Introduction

Advanced testing goes beyond basic functional testing to validate system behavior under stress, attack, and failure conditions.

### When to Use Advanced Testing

| Technique | When to Use | Impact |
|-----------|-----------|--------|
| **Load Testing** | Before production release, after major changes | Prevents performance degradation |
| **Performance Testing** | Continuously in CI/CD, after optimization | Detects regressions early |
| **Security Testing** | Every release, after dependency updates | Prevents vulnerabilities |
| **Chaos Engineering** | Post-deployment, periodic resilience checks | Improves failure recovery |
| **Contract Testing** | Every microservice integration change | Prevents API incompatibilities |

### Integration with CI/CD

```yaml
# Example GitHub Actions workflow
name: Advanced Tests
on: [push, pull_request]

jobs:
  performance:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run performance tests
        run: k6 run tests/performance.js
      - name: Compare to baseline
        run: scripts/compare-baseline.sh

  security:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: OWASP ZAP scan
        run: docker run -v $(pwd):/zap -t owasp/zap2docker-stable zap-baseline.py

  chaos:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v2
      - name: Run chaos tests
        run: kubectl apply -f chaos/daily-tests.yaml
```

### Quality Gates

Establish thresholds for each testing type:

```
Performance Gate (must pass to merge):
- API response time p95 < 1s for GET endpoints
- API response time p95 < 2s for POST endpoints
- Error rate < 0.1%
- Throughput > 100 req/s under normal load

Security Gate (must pass):
- No critical vulnerabilities detected
- All OWASP Top 10 items tested
- No hardcoded secrets
- SSL/TLS properly configured

Chaos Gate (informational):
- Service recovers within defined time
- Zero permanent data loss
- Fallback mechanisms engaged
```

---

## Load Testing

Load testing determines how the system behaves under concurrent user load.

### Tools & Setup

#### k6 (Recommended for Gym Platform)

k6 is lightweight, fast, and integrates well with microservices.

```javascript
// tests/load/user-registration.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 100,                    // 100 virtual users
  duration: '5m',              // 5 minute test
  rampUp: {
    stages: [
      { duration: '1m', target: 50 },   // ramp up to 50 users
      { duration: '2m', target: 100 },  // then to 100
      { duration: '1m', target: 100 },  // stay at 100
      { duration: '1m', target: 0 },    // ramp down
    ],
  },
  thresholds: {
    'http_req_duration': ['p(95)<2000', 'p(99)<3000'],
    'http_req_failed': ['rate<0.01'],
  },
};

export default function() {
  // Register new user
  const url = `${__ENV.API_URL}/api/auth/register`;
  const payload = JSON.stringify({
    email: `user${Date.now()}@gym.test`,
    password: 'TestPassword123!',
    firstName: 'Test',
    lastName: 'User',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const response = http.post(url, payload, params);

  check(response, {
    'status is 201': (r) => r.status === 201,
    'token received': (r) => r.json('token') !== null,
    'response time < 2s': (r) => r.timings.duration < 2000,
  });

  sleep(1);
}
```

Run the test:
```bash
# Local testing
k6 run tests/load/user-registration.js \
  --vus 100 --duration 5m \
  -e API_URL=http://localhost:8080

# With results output
k6 run tests/load/user-registration.js \
  --out json=results.json

# Against staging
k6 run tests/load/user-registration.js \
  -e API_URL=https://staging.gym-api.com
```

#### Apache JMeter

For Java services like Training and Tracking, JMeter provides detailed analysis.

```jmx
<!-- Example JMeter test plan structure -->
<jmeterTestPlan>
  <hashTree>
    <!-- Thread Group -->
    <ThreadGroup name="Load Test">
      <numThreads>100</numThreads>
      <rampTime>60</rampTime>
      <duration>300</duration>
    </ThreadGroup>

    <!-- HTTP Request -->
    <HTTPSampler name="Create Workout">
      <elementProp name="HTTPsampler.Arguments">
        <HTTPArgument name="exercise_id" value="${exercise_id}"/>
        <HTTPArgument name="sets" value="3"/>
        <HTTPArgument name="reps" value="10"/>
      </elementProp>
    </HTTPSampler>

    <!-- Result Listener -->
    <ResultCollector name="View Results Tree"/>
  </hashTree>
</jmeterTestPlan>
```

### Load Test Scenarios

#### Scenario 1: User Registration Spike

**Purpose:** Validate registration endpoint under peak signup loads (e.g., app launch)

```
Configuration:
- Ramp-up: 100 users/minute for 5 minutes (500 total users)
- Sustained: 500 concurrent users for 10 minutes
- Expected results:
  - Response time p95: < 2s
  - Response time p99: < 3s
  - Error rate: < 0.1%
  - Throughput: > 50 registrations/second

What we're testing:
- Database connection pooling under load
- Password hashing performance
- Email validation service capacity
- Session creation speed
```

#### Scenario 2: Workout API Heavy Read

**Purpose:** Validate query performance for fetching workout history

```
Configuration:
- 500 concurrent users
- Each user fetches their workout history every 5 seconds
- Duration: 10 minutes

Expected results:
- Response time p95: < 500ms
- Database CPU: < 70%
- Memory usage: < 4GB
- Connection count: < 100 (with pooling)

Optimization targets:
- Verify indexes on user_id + created_at
- Check query optimization
- Validate caching layer (Redis)
```

#### Scenario 3: Notification Flood

**Purpose:** Ensure notification service handles bulk messages

```
Configuration:
- 1000 notifications queued within 10 seconds
- Multiple channels (email, push, SMS)
- Duration: 10 minutes

Expected results:
- Queue processing latency p95: < 30s
- Delivery success rate: > 99%
- Message loss: 0

What we're testing:
- Message queue capacity (RabbitMQ, Kafka)
- Rate limiting on email provider
- Error handling for failed deliveries
```

### Analyzing Load Test Results

#### Key Metrics

```
1. Response Time (Latency)
   - p50 (median): Normal operation
   - p95: 95% of requests faster than this (SLA target)
   - p99: 99% of requests faster than this

   Target for Gym API:
   - GET endpoints: p95 < 500ms
   - POST endpoints: p95 < 1s
   - Complex queries: p95 < 2s

2. Throughput
   - Requests per second (RPS)
   - What load our system handles
   - Example: 100 RPS means 360,000 requests/hour

3. Error Rate
   - Percentage of requests that fail
   - Target: < 0.1% for production
   - Investigate spike causes immediately

4. Resource Utilization
   - CPU usage
   - Memory consumption
   - Database connections
   - Disk I/O

   Targets:
   - CPU: < 80% (headroom for spikes)
   - Memory: < 70% (avoid OOM kills)
   - DB connections: < 80% of pool size
```

#### Example Results Analysis

```bash
# k6 test results summary
Scenarios (1 total)
 default: 50000 iterations (100 VUs, 5m max)

     data_received..................: 12 MB  40 kB/s
     data_sent.......................: 1.2 MB 4 kB/s
     http_req_blocked................: avg=5.2ms  min=1.1ms  max=92ms  p(95)=9.2ms p(99)=15ms
     http_req_connecting.............: avg=1.8ms  min=0s     max=45ms  p(95)=3.2ms p(99)=8.1ms
     http_req_duration...............: avg=198ms  min=102ms  max=5.2s  p(95)=450ms p(99)=820ms
     http_req_failed.................: 0.02%  (10 failed out of 50000)
     http_req_receiving..............: avg=4.1ms  min=0s     max=200ms p(95)=8.2ms p(99)=25ms
     http_req_sending................: avg=8.2ms  min=1.2ms  max=120ms p(95)=12ms  p(99)=45ms
     http_req_tls_handshaking........: avg=2.1ms  min=0s     max=55ms  p(95)=4.2ms p(99)=10ms
     http_req_waiting................: avg=185ms  min=95ms   max=5s    p(95)=420ms p(99)=780ms
     http_reqs........................: 50000  166.66/s
     iteration_duration..............: avg=1.2s   min=1.1s   max=6.2s  p(95)=1.45s p(99)=1.82s
     iterations.......................: 50000  166.66/s

# Analysis:
✅ p95 response time (450ms) is well below 500ms target
✅ Error rate (0.02%) is below 0.1% threshold
✅ Throughput (166.66 req/s) exceeds requirements
❌ p99 response time (820ms) slightly above target - investigate optimization
```

---

## Performance Testing

Performance testing measures system response under normal and peak loads to establish baselines and detect regressions.

### API Response Time Benchmarks

Establish baseline response times for each endpoint category:

```
Baseline Benchmarks (these should not regress):

Authentication Endpoints:
- POST /api/auth/register: < 1s (includes password hashing)
- POST /api/auth/login: < 500ms
- POST /api/auth/refresh: < 200ms
- GET /api/auth/me: < 100ms

Training Endpoints:
- GET /api/training/exercises: < 200ms (paginated)
- GET /api/training/workouts/{id}: < 500ms
- POST /api/training/workouts: < 1s (includes calculations)
- GET /api/training/workouts?user_id=X&limit=50: < 300ms

Tracking Endpoints:
- GET /api/tracking/weight-logs: < 200ms
- POST /api/tracking/diet-logs: < 500ms
- GET /api/tracking/goals: < 300ms
- GET /api/tracking/progress/{goal_id}: < 400ms

Notification Endpoints:
- GET /api/notifications: < 100ms (from cache)
- POST /api/notifications/preferences: < 200ms
- GET /api/notifications/{id}: < 50ms
```

### Measuring Performance

#### Using k6 with HTTP Timing Breakdown

```javascript
// tests/performance/baseline.js
import http from 'k6/http';
import { check } from 'k6';

export const options = {
  vus: 10,
  duration: '2m',
  thresholds: {
    'http_req_duration{staticAsset:no}': ['p(95)<500'],
  },
};

export default function() {
  // Measure individual endpoints
  const authResp = http.get('http://localhost:8080/api/auth/me', {
    tags: { endpoint: 'auth_me' },
  });

  check(authResp, {
    'auth /me response < 100ms': (r) => r.timings.duration < 100,
    'auth /me status 200': (r) => r.status === 200,
  });

  const workoutResp = http.get('http://localhost:8080/api/training/workouts', {
    tags: { endpoint: 'workouts_list' },
  });

  check(workoutResp, {
    'workouts list response < 300ms': (r) => r.timings.duration < 300,
    'workouts list status 200': (r) => r.status === 200,
  });

  // Analyze response time breakdown
  console.log(`
    Auth /me:
      - Total: ${authResp.timings.duration}ms
      - DNS: ${authResp.timings.blocked}ms
      - Connect: ${authResp.timings.connecting}ms
      - TLS: ${authResp.timings.tls_handshaking}ms
      - Server: ${authResp.timings.waiting}ms
  `);
}
```

#### Database Query Performance

```sql
-- Identify slow queries in PostgreSQL
SELECT query, calls, mean_time, max_time
FROM pg_stat_statements
WHERE mean_time > 100  -- queries averaging > 100ms
ORDER BY mean_time DESC;

-- Example slow query optimization
-- Before: Full table scan
SELECT w.* FROM workouts w
WHERE w.user_id = $1 AND w.completed = true
ORDER BY w.start_time DESC;

-- After: Using index
CREATE INDEX idx_workouts_user_completed_time 
ON workouts(user_id, completed, start_time DESC);

-- Measure improvement
EXPLAIN ANALYZE
SELECT w.* FROM workouts w
WHERE w.user_id = $1 AND w.completed = true
ORDER BY w.start_time DESC
LIMIT 20;
```

### Performance Regression Detection

```bash
#!/bin/bash
# scripts/check-performance-regression.sh

# Run performance test and save results
k6 run tests/performance/baseline.js -o json=current-results.json

# Compare to baseline
BASELINE=$(jq '.summary.http_req_duration["p(95)"]' baseline-results.json)
CURRENT=$(jq '.summary.http_req_duration["p(95)"]' current-results.json)

# Calculate percentage change
DIFF=$(echo "scale=2; (($CURRENT - $BASELINE) / $BASELINE) * 100" | bc)

echo "Performance Regression Check:"
echo "Baseline p95: ${BASELINE}ms"
echo "Current p95: ${CURRENT}ms"
echo "Change: ${DIFF}%"

# Fail if regression > 10%
if (( $(echo "$DIFF > 10" | bc -l) )); then
  echo "❌ Performance regression detected!"
  exit 1
else
  echo "✅ Performance within acceptable range"
  exit 0
fi
```

---

## Security Testing

Security testing validates that the system properly handles authentication, authorization, data protection, and resists common attacks.

### OWASP Top 10 Mapping for Gym Platform

#### 1. SQL Injection Prevention

The Gym Platform uses parameterized queries throughout. Verify this is enforced:

```java
// ✅ Good: Parameterized query (protected)
String sql = "SELECT * FROM users WHERE email = ?";
PreparedStatement stmt = connection.prepareStatement(sql);
stmt.setString(1, userEmail);
ResultSet rs = stmt.executeQuery();

// ❌ Bad: String concatenation (vulnerable)
String sql = "SELECT * FROM users WHERE email = '" + userEmail + "'";
// This allows: email = "' OR '1'='1" to bypass authentication
```

**Test:**
```javascript
// tests/security/sql-injection.js
import http from 'k6/http';
import { check } from 'k6';

export default function() {
  const injection = "' OR '1'='1";
  const response = http.post(
    'http://localhost:8080/api/auth/login',
    JSON.stringify({
      email: `user${injection}@test.com`,
      password: 'anything',
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(response, {
    'SQL injection rejected': (r) => r.status === 400 || r.status === 401,
    'No user data leaked': (r) => !r.body.includes('password'),
  });
}
```

#### 2. Authentication & JWT Validation

```javascript
// tests/security/jwt-validation.js
import http from 'k6/http';
import { check } from 'k6';

export default function() {
  // Test 1: Expired token rejection
  const expiredToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'; // expired
  const expiredResponse = http.get(
    'http://localhost:8080/api/auth/me',
    {
      headers: { Authorization: `Bearer ${expiredToken}` },
    }
  );

  check(expiredResponse, {
    'Expired token rejected': (r) => r.status === 401,
    'Error message present': (r) => r.body.includes('token') || r.body.includes('expired'),
  });

  // Test 2: Invalid token rejection
  const invalidToken = 'not.a.valid.jwt';
  const invalidResponse = http.get(
    'http://localhost:8080/api/auth/me',
    {
      headers: { Authorization: `Bearer ${invalidToken}` },
    }
  );

  check(invalidResponse, {
    'Invalid token rejected': (r) => r.status === 401,
  });

  // Test 3: Tampered token rejection
  const tamperedToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.TAMPERED';
  const tamperedResponse = http.get(
    'http://localhost:8080/api/auth/me',
    {
      headers: { Authorization: `Bearer ${tamperedToken}` },
    }
  );

  check(tamperedResponse, {
    'Tampered token rejected': (r) => r.status === 401,
  });
}
```

#### 3. Sensitive Data Protection

```javascript
// tests/security/sensitive-data.js
import http from 'k6/http';
import { check } from 'k6';

export default function() {
  // Test 1: Password not in response
  const loginResponse = http.post(
    'http://localhost:8080/api/auth/login',
    JSON.stringify({
      email: 'user@test.com',
      password: 'TestPassword123!',
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(loginResponse, {
    'Response contains token': (r) => r.body.includes('token'),
    'Response does NOT contain password': (r) => !r.body.includes('TestPassword123'),
    'Response does NOT contain password_hash': (r) => !r.body.includes('password_hash'),
  });

  // Test 2: HTTPS enforced (no sensitive data over HTTP)
  // This would be checked in staging/production deployments

  // Test 3: User cannot see other user's data
  const otherUserResponse = http.get(
    'http://localhost:8080/api/auth/users/other-user-id/profile',
    {
      headers: { Authorization: `Bearer ${loginResponse.json('token')}` },
    }
  );

  check(otherUserResponse, {
    'Access denied to other user': (r) => r.status === 403,
  });
}
```

#### 4. Rate Limiting on Auth Endpoints

```javascript
// tests/security/rate-limiting.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1,
  iterations: 20,
};

export default function() {
  // Attempt 20 failed logins rapidly
  for (let i = 0; i < 10; i++) {
    const response = http.post(
      'http://localhost:8080/api/auth/login',
      JSON.stringify({
        email: 'user@test.com',
        password: 'WrongPassword123!',
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    check(response, {
      'Failed login status 401': (r) => r.status === 401,
    });
  }

  // After N failures, should be rate limited
  const rateLimitedResponse = http.post(
    'http://localhost:8080/api/auth/login',
    JSON.stringify({
      email: 'user@test.com',
      password: 'CorrectPassword123!',
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  check(rateLimitedResponse, {
    'Rate limiting active': (r) => r.status === 429,
    'Retry-After header present': (r) => r.headers['Retry-After'] !== undefined,
  });
}
```

#### 5. Access Control (RBAC)

```javascript
// tests/security/rbac.js
import http from 'k6/http';
import { check } from 'k6';

export default function() {
  // Login as regular user
  const userToken = getToken('user@test.com', 'user');

  // Test 1: User cannot delete other users
  const deleteResponse = http.del(
    'http://localhost:8080/api/admin/users/other-user-id',
    null,
    { headers: { Authorization: `Bearer ${userToken}` } }
  );

  check(deleteResponse, {
    'User cannot delete users': (r) => r.status === 403,
  });

  // Test 2: User cannot modify system settings
  const settingsResponse = http.patch(
    'http://localhost:8080/api/admin/settings',
    JSON.stringify({ maxUsersPerOrg: 100 }),
    { headers: { Authorization: `Bearer ${userToken}` } }
  );

  check(settingsResponse, {
    'User cannot modify settings': (r) => r.status === 403,
  });

  // Test 3: Trainer can only manage assigned users
  const trainerToken = getToken('trainer@test.com', 'trainer');
  const assignedUserResponse = http.get(
    'http://localhost:8080/api/training/users/assigned-user-id/workouts',
    { headers: { Authorization: `Bearer ${trainerToken}` } }
  );

  check(assignedUserResponse, {
    'Trainer can view assigned user': (r) => r.status === 200,
  });

  const unassignedUserResponse = http.get(
    'http://localhost:8080/api/training/users/unassigned-user-id/workouts',
    { headers: { Authorization: `Bearer ${trainerToken}` } }
  );

  check(unassignedUserResponse, {
    'Trainer cannot view unassigned user': (r) => r.status === 403,
  });
}

function getToken(email, role) {
  // Helper to get auth token (implement based on your test setup)
  return 'token';
}
```

---

## Chaos Engineering

Chaos engineering intentionally injects failures to test system resilience and identify failure modes.

### What is Chaos Engineering

Chaos engineering is the practice of experimenting on a system to build confidence that the system can withstand turbulent and unexpected conditions in production.

**Goals:**
- Discover weaknesses before they affect users
- Build confidence in recovery procedures
- Improve operational procedures
- Educate team on system behavior under failure

**Principle:** Small, controlled experiments reveal systemic weakness.

### Common Chaos Scenarios for Gym Platform

#### Scenario 1: Service Pod Failure

**Chaos Test:** Kill Auth Service pod during traffic

```yaml
# chaos/auth-service-pod-failure.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: auth-service-kill
  namespace: gym-platform
spec:
  action: pod-kill
  mode: one  # kill only one pod at a time
  selector:
    namespaces:
      - gym-platform
    labelSelectors:
      app: auth-service
  scheduler:
    cron: "0 2 * * *"  # Run daily at 2 AM
    duration: "10m"
  duration: "2m"  # Kill pod for 2 minutes
---
apiVersion: chaos-mesh.org/v1alpha1
kind: PodChaos
metadata:
  name: auth-service-restart
  namespace: gym-platform
spec:
  action: pod-restart
  mode: one
  selector:
    namespaces:
      - gym-platform
    labelSelectors:
      app: auth-service
```

**Expected Behavior:**
- Kubernetes automatically restarts the pod
- Traffic fails over to remaining pods
- No permanent data loss
- Session tokens remain valid
- Recovery time < 30 seconds

**Validation Test:**
```javascript
// tests/chaos/pod-failure-recovery.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export default function() {
  // Establish baseline - successful request before chaos
  const beforeChaos = http.get('http://localhost:8080/api/auth/health');
  check(beforeChaos, {
    'Service healthy before chaos': (r) => r.status === 200,
  });

  // Trigger pod failure (in integration with chaos controller)
  console.log('Pod failure triggered...');

  // Immediately start making requests during chaos
  let recovered = false;
  const startTime = new Date();

  while (!recovered && new Date() - startTime < 60000) {
    const response = http.get('http://localhost:8080/api/auth/health');

    if (response.status === 200) {
      recovered = true;
      const recoveryTime = new Date() - startTime;

      check(response, {
        'Service recovered': (r) => r.status === 200,
        'Recovery < 30s': () => recoveryTime < 30000,
      });

      console.log(`Service recovered in ${recoveryTime}ms`);
    }

    sleep(1);
  }

  if (!recovered) {
    console.error('Service did not recover within 60 seconds');
  }
}
```

#### Scenario 2: High Latency Injection

**Chaos Test:** Add network latency to database connections

```yaml
# chaos/database-latency.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: database-latency
  namespace: gym-platform
spec:
  action: delay
  mode: all
  selector:
    namespaces:
      - gym-platform
    podSelector:
      matchLabels:
        app: training-service
  delay:
    latency: "2000ms"  # Add 2 second delay
    jitter: "100ms"    # Random variation
  duration: "5m"
  scheduler:
    cron: "0 3 * * *"  # Daily at 3 AM
    duration: "10m"
```

**Expected Behavior:**
- Requests take longer but eventually succeed
- Timeouts are handled gracefully
- Circuit breakers activate if latency is too severe
- User sees appropriate error messages

#### Scenario 3: Network Partition

**Chaos Test:** Simulate microservice unable to reach database

```yaml
# chaos/network-partition.yaml
apiVersion: chaos-mesh.org/v1alpha1
kind: NetworkChaos
metadata:
  name: tracking-service-partition
  namespace: gym-platform
spec:
  action: partition
  mode: one
  selector:
    namespaces:
      - gym-platform
    podSelector:
      matchLabels:
        app: tracking-service
  direction: both
  ipBlocks:
    - cidr: "10.0.0.0/8"  # Block all internal traffic
  duration: "3m"
  scheduler:
    cron: "0 4 * * *"  # Daily at 4 AM
    duration: "10m"
```

**Expected Behavior:**
- In-flight requests timeout gracefully
- Circuit breakers engage
- Fallback mechanisms activate (cache, degraded mode)
- Data loss: zero
- After partition heals: service resumes normally

### Running Chaos Tests

```bash
# Install Chaos Mesh
helm repo add chaos-mesh https://charts.chaos-mesh.org
helm install chaos-mesh chaos-mesh/chaos-mesh

# Apply chaos experiment
kubectl apply -f chaos/pod-failure.yaml

# Monitor impact
kubectl logs -f deployment/auth-service

# View chaos event
kubectl describe podchaos pod-failure -n gym-platform

# Remove chaos experiment
kubectl delete -f chaos/pod-failure.yaml
```

---

## Contract Testing

Contract testing ensures that microservices can communicate correctly without full integration.

### Why Contract Testing Matters

In a microservices architecture, small changes to one service can break others. Contract testing validates that service dependencies (contracts) are maintained.

### Example: Auth Service → Training Service Contract

```
Auth Service provides:
- GET /api/auth/users/{id} returns { id, email, name, roles[] }

Training Service expects:
- GET /api/auth/users/{id}
- Response includes `roles` array for permission checking
```

If Auth Service removes the `roles` field, Training Service breaks.

### Contract Test using Pact

```java
// tests/contracts/AuthServiceContractTest.java
@RunWith(PactRunner.class)
@PactBroker(host = "pactbroker.local")
public class AuthServiceContractTest {
  
  @Pact(consumer = "TrainingService", provider = "AuthService")
  public RequestResponsePact getUserContract(PactDslWithProvider builder) {
    return builder
      .given("user exists")
      .uponReceiving("a request for user details")
      .path("/api/auth/users/user-123")
      .method("GET")
      .willRespondWith()
      .status(200)
      .body(newJsonBody(body -> {
        body.stringValue("id", "user-123");
        body.stringValue("email", "user@test.com");
        body.stringValue("name", "John Doe");
        body.array("roles", array -> {
          array.stringValue("user");
        });
      }).build())
      .toPact();
  }

  @Test
  @PactTestFor(pactMethod = "getUserContract")
  public void testGetUser(MockServer mockServer) {
    Response response = webClient.get("/api/auth/users/user-123")
      .exchange()
      .block();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getBody()).contains("roles");
  }
}
```

---

## Running & Reporting

### Local Execution

```bash
# Run individual test suite
k6 run tests/load/user-registration.js
k6 run tests/performance/baseline.js
k6 run tests/security/sql-injection.js

# Run all load tests
for test in tests/load/*.js; do
  k6 run "$test"
done

# Run with custom environment variables
k6 run tests/load/user-registration.js \
  -e API_URL=http://localhost:8080 \
  -e VUS=100

# Generate HTML report
k6 run tests/load/user-registration.js \
  --out json=results.json && \
  npx k6 report results.json
```

### CI/CD Integration

```yaml
# .github/workflows/advanced-tests.yml
name: Advanced Testing

on:
  schedule:
    - cron: '0 2 * * *'  # Daily at 2 AM
  push:
    branches: [main, develop]

jobs:
  load-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run load tests
        run: docker run -v $(pwd):/scripts grafana/k6 run /scripts/tests/load/user-registration.js
      - name: Compare to baseline
        run: scripts/compare-baseline.sh

  security-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: OWASP ZAP scan
        run: |
          docker run -t owasp/zap2docker-stable zap-baseline.py \
            -t http://localhost:8080 \
            -r zap-report.html

  chaos-tests:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v2
      - name: Apply chaos tests
        run: kubectl apply -f chaos/
      - name: Wait for recovery
        run: sleep 300
      - name: Verify system health
        run: scripts/verify-system-health.sh
```

### Reporting Metrics

Store results in Prometheus/Grafana for trending:

```javascript
// tests/perf-report.js
import http from 'k6/http';

const options = {
  ext: {
    loadimpact: {
      projectID: 12345,
      name: 'Weekly Load Test'
    }
  }
};

export default function() {
  const response = http.get('http://localhost:8080/api/auth/me');
  // Metrics automatically sent to Grafana Cloud
}
```

---

## Troubleshooting

### Load Test Issues

**Problem:** Load test fails immediately with connection errors

```bash
# Check if services are running
curl -I http://localhost:8080/health

# Verify network connectivity
docker network ls
docker network inspect gym-network

# Check service logs
docker logs training-service
```

**Problem:** Performance degrades over time during load test

```
Causes:
1. Memory leak in application
2. Database connection pool exhaustion
3. Disk filling up (logs, temp files)

Debug:
- Monitor memory: `docker stats`
- Check DB connections: `select count(*) from pg_stat_activity;`
- Check disk: `df -h`
```

### Security Test False Positives

**Problem:** OWASP ZAP reports vulnerability that isn't actually an issue

```
Solution:
1. Review the specific finding
2. Verify parameterized queries in code
3. Add exception to ZAP config if legitimate
4. Document the exception
```

### Chaos Test Recovery Failures

**Problem:** Service doesn't recover after pod kill

```bash
# Check pod status
kubectl get pods -n gym-platform
kubectl describe pod auth-service-0

# Check events
kubectl get events -n gym-platform --sort-by='.lastTimestamp'

# Check logs
kubectl logs auth-service-0 --previous

# Verify readiness probe
kubectl get deployment auth-service -o yaml | grep -A 10 readinessProbe
```

---

## References

- [k6 Documentation](https://k6.io/docs/)
- [OWASP Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
- [Chaos Engineering Principles](https://principlesofchaos.org/)
- [Pact Contract Testing](https://docs.pact.foundation/)
- [Performance Testing Best Practices](https://kubernetes.io/docs/tasks/debug-application-cluster/resource-metrics-pipeline/)
