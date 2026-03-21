# ADR-008: PgBouncer for Connection Pooling

## Status
Accepted

## Date
2026-03-21

## Context

PostgreSQL (ADR-002) has a limitation: each client connection requires a process on the server, which consumes significant memory (~10MB per connection). The challenges:

1. **Connection Exhaustion**: Thousands of application instances = thousands of connections
2. **Memory Overhead**: Each connection uses ~10MB on server
3. **Performance**: Too many connections degrades performance
4. **Microservices**: Each of 4 services connects independently

Example problem:
```
100 application instances × 4 services × 10 connections each = 4,000 connections
4,000 × 10MB = 40GB of server memory just for connections!
```

With Kubernetes auto-scaling, connection count becomes unpredictable.

## Decision

We implemented **PgBouncer** as a connection pooling proxy between applications and PostgreSQL:

1. **Connection Pool**: Maintain fixed pool of DB connections
2. **Multiplexing**: Share pool across many client connections
3. **Reduced Memory**: Dramatically reduce connections to PostgreSQL
4. **Per-Service Proxy**: Each microservice has own PgBouncer instance (or shared)

## Rationale

### 1. Connection Efficiency
PgBouncer multiplexes many client connections onto fewer database connections:

```
WITHOUT PgBouncer:
  100 app instances → 100 DB connections → PostgreSQL
  Memory: 100 × 10MB = 1GB on server

WITH PgBouncer:
  100 app instances → PgBouncer → 20 DB connections → PostgreSQL
  Memory: 20 × 10MB = 200MB on server
  Savings: 80% memory reduction!
```

### 2. Horizontal Scaling
PgBouncer enables:
- App instances can scale up/down without hitting connection limits
- Database stays stable (fixed connection pool)
- Kubernetes can auto-scale applications
- No "connection exhaustion" errors

### 3. Performance
Benefits:
- Reduced memory pressure on database
- Faster connection reuse (no new connection overhead)
- Lower latency (reused connections)
- Better CPU utilization

### 4. Resilience
PgBouncer provides:
- Connection timeout handling
- Graceful degradation when DB is slow
- Queue management (fair request ordering)
- Automatic connection reset on errors

### 5. Operational Simplicity
- Single point to reconfigure connections
- Monitor pool health in one place
- Debug connection issues centrally

## Consequences

### Positive
- ✅ Dramatic memory savings (80%+)
- ✅ Enables horizontal scaling
- ✅ Better performance
- ✅ Simple to configure
- ✅ Lightweight overhead
- ✅ Works with existing PostgreSQL

### Negative
- ❌ Additional component to run/monitor
- ❌ Adds latency (minimal, ~1-2ms)
- ❌ One more thing that can fail
- ❌ PgBouncer needs HA (high availability)
- ❌ Some PostgreSQL features unavailable (session state)
- ❌ Connection pool sizing requires tuning

## Alternatives Considered

### 1. Larger Database Server
- **Pros**: Simple, no new components
- **Cons**: Expensive, doesn't scale applications, vertical scaling limits
- **Why not**: Throws hardware at problem, wrong architecture

### 2. Read Replicas
- **Pros**: Scales reads, distributes load
- **Cons**: Doesn't reduce connections, adds complexity, replication lag
- **Why not**: Complementary but doesn't solve connection issue

### 3. Managed Database (AWS RDS)
- **Pros**: AWS handles connection pooling
- **Cons**: Vendor lock-in, less control, still costs
- **Why not**: We chose PostgreSQL on Kubernetes (ADR-004)

### 4. Application-Level Pooling
- **Pros**: Per-application control
- **Cons**: Each app manages own pool, inefficient, hard to coordinate
- **Why not**: Centralized pooling more efficient

## Related ADRs

- **Depends on**: ADR-002 (PostgreSQL database)
- **Related to**: ADR-004 (Runs in Kubernetes)
- **Related to**: ADR-005 (Monitoring PgBouncer health)

## Implementation Details

### PgBouncer Configuration

```ini
[databases]
gym_platform = host=postgres.default.svc.cluster.local port=5432 dbname=gym_platform

[pgbouncer]
pool_mode = transaction
max_client_conn = 1000
default_pool_size = 25
min_pool_size = 10
reserve_pool_size = 5
reserve_pool_timeout = 3
max_db_connections = 100
max_user_connections = 50
server_lifetime = 3600
server_idle_timeout = 600
```

### Pool Sizing Example

For a service expecting:
- 100-500 concurrent requests
- Average query time: 50ms
- Peak load factor: 2x

```
Pool Size = Concurrent Requests × (Query Time / Connection Time)
Pool Size = 500 × (50ms / 10ms) = 2500 connections needed

But with PgBouncer (transaction pooling):
Pool Size = 100 × (50ms / 10ms) = 500 total connections for entire fleet
Per instance pool: 25 connections (500 / 20 instances)
```

### Connection Modes

1. **Session Mode**: One connection per client (default, safe)
   ```
   Client connects → stays connected for session
   Good for: Most apps
   ```

2. **Transaction Mode**: Client uses connection for one transaction (recommended)
   ```
   Client sends transaction → connection released after commit/rollback
   Good for: Stateless APIs (like ours)
   Better connection reuse
   ```

3. **Statement Mode**: Client uses connection for one statement (aggressive)
   ```
   Each statement reuses connection
   Good for: Limited compatibility
   Risky: Can break multi-statement transactions
   ```

### Deployment Pattern

```
Service Pod
├── Application
├── PgBouncer sidecar
└── → Shared PostgreSQL

OR

PgBouncer StatefulSet
├── Instance 1 (25 connections)
├── Instance 2 (25 connections)
└── → PostgreSQL (100 connections total)
```

### Monitoring Metrics

```
pgbouncer_pools_clients_active         # Active client connections
pgbouncer_pools_server_active          # Active server connections
pgbouncer_pools_queries_duration       # Query latency
pgbouncer_pools_connection_pool_size   # Configured pool size
pgbouncer_stats_requests               # Total requests
pgbouncer_stats_errors                 # Connection errors
```

Alert when:
- Active clients close to max
- Server connections maxed out
- Connection errors increasing
- Query latency degrading

## Tuning Strategy

1. **Start Conservative**: Small pool, expand based on metrics
2. **Monitor**: Track connection usage patterns
3. **Adjust**: Tune pool_size, min_pool_size based on data
4. **Test**: Load test to find optimal pool size
5. **Document**: Record tuning decisions and rationale

## Future Considerations

- Consider Citus (PostgreSQL extension) if horizontal DB scaling needed
- Consider connection pooling at application level as well (dual-layer pooling)
- Consider dedicated high-performance pooling if we hit limits
