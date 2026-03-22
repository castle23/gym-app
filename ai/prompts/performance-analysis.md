# AI Performance Analysis Prompt

## Context

You are analyzing performance for the **Gym Platform API**, a Java 17+ / Spring Boot 3.x
system with 4 microservices running on JVM inside Docker containers, backed by PostgreSQL.

| Component      | Technology                     |
|----------------|--------------------------------|
| Runtime        | Java 17+, Spring Boot 3.x     |
| Database       | PostgreSQL 15+ with PgBouncer  |
| Connection Pool| HikariCP                       |
| Caching        | Caffeine (local), Redis (dist.)|
| Monitoring     | Prometheus + Grafana (ADR-005) |
| Containers     | Docker / Kubernetes            |

---

## Instructions

### Key Metrics to Collect

| Metric Category   | Metric              | Target (API)          |
|--------------------|---------------------|-----------------------|
| Latency            | p50 response time   | < 100ms               |
| Latency            | p95 response time   | < 500ms               |
| Latency            | p99 response time   | < 1s                  |
| Throughput         | Requests/sec        | Baseline per endpoint |
| JVM                | Heap usage          | < 80% of max          |
| JVM                | GC pause time       | < 50ms p99            |
| JVM                | Thread count        | Monitor for leaks     |
| Database           | Query time (avg)    | < 50ms                |
| Database           | Slow queries (>1s)  | 0 in steady state     |
| Database           | Connection pool util | < 80% of max size     |
| System             | CPU utilization     | < 70% sustained       |
| System             | Memory utilization  | < 80% of allocation   |

### JVM Tuning Guidelines

| Setting        | Dev              | Prod                                          |
|----------------|------------------|------------------------------------------------|
| Heap           | `-Xms512m -Xmx1g` | `-Xms2g -Xmx4g` (set Xms=Xmx)              |
| GC             | G1GC, 200ms pause| G1GC, 100ms pause, heap dump on OOM           |
| GC Logging     | Optional         | `-Xlog:gc*:file=/var/log/gc.log:time,uptime`  |

- **G1GC** recommended for all services (balanced throughput/latency).

### Database Optimization

**Slow query detection:** Use `pg_stat_statements` — query by `mean_exec_time DESC`.
**Indexing:** Index all FKs and WHERE/JOIN/ORDER BY columns. Use `EXPLAIN ANALYZE`.
**N+1 prevention:** `@EntityGraph` or `JOIN FETCH` in JPQL; `@BatchSize(size = 25)`.

### HikariCP Tuning

| Setting              | Value       | Notes                              |
|----------------------|-------------|------------------------------------|
| maximum-pool-size    | 8–32        | `(core_count * 2) + spindle_count` |
| connection-timeout   | 30s         | Fail fast on pool exhaustion       |
| max-lifetime         | 30min       | Recycle before DB-side timeout     |
| leak-detection       | 60s         | Detect unreturned connections      |

Monitor via `/actuator/metrics/hikaricp.connections.active`.

### Caching Strategy (ADR-012)

| Cache Type  | Technology | Use Case                      | TTL        |
|-------------|------------|-------------------------------|------------|
| Local       | Caffeine   | Reference data, user profiles | 5-15 min   |
| Distributed | Redis      | Session data, shared state    | 15-60 min  |
| HTTP        | ETag/304   | Infrequently changing data    | Varies     |

- Cache hit ratio target: > 80%. Monitor via Micrometer metrics.
- Invalidate on writes. Prefer cache-aside pattern.

### Diagnostic Tools

| Tool     | Command                          | Purpose                   |
|----------|----------------------------------|---------------------------|
| jstat    | `jstat -gcutil <pid> 1000`       | GC utilization every 1s   |
| jstack   | `jstack <pid> > thread-dump.txt` | Deadlock detection        |
| jmap     | `jmap -histo <pid>`              | Heap object analysis      |
| JMeter   | `jmeter -n -t plan.jmx -e -o report/` | Load testing        |

---

## Expected Output Format

```markdown
## Performance Report: [Scope — e.g., "Training Service under load"]

### Test Conditions
- Load profile: [concurrent users, ramp-up, duration]
- Environment: [dev/staging/prod, resource allocation]

### Results Summary
| Metric            | Value   | Target  | Status |
|-------------------|---------|---------|--------|
| p50 latency       | Xms     | <100ms  | PASS/FAIL |
| p95 latency       | Xms     | <500ms  | PASS/FAIL |
| Throughput         | X req/s | X req/s | PASS/FAIL |
| Error rate         | X%      | <1%     | PASS/FAIL |

### Bottlenecks Identified
1. [Description, evidence, impact]

### Recommendations
| Priority | Action                  | Expected Impact      |
|----------|-------------------------|----------------------|
| HIGH     | [Specific change]       | [Quantified benefit] |
| MEDIUM   | [Specific change]       | [Quantified benefit] |
```

---

## References

- [Performance Tuning Guide](../../docs/operations/05-performance-tuning.md)
