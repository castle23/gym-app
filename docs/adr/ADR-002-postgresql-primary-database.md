# ADR-002: PostgreSQL as Primary Database

## Status
Accepted

## Date
2026-03-21

## Context

The Gym Platform needed to choose a primary database technology. The requirements were:

1. **Multi-service Support**: Each microservice needs its own database, but with consistent technology
2. **ACID Guarantees**: Financial and health data require strong consistency
3. **Complex Queries**: Reporting and analytics need powerful querying
4. **Scalability**: Need to handle millions of users and workouts
5. **Open Source**: Wanted open-source with good community support
6. **Cost**: Needed cost-effective solution

The team evaluated:
- SQL: PostgreSQL, MySQL, MariaDB
- NoSQL: MongoDB, Cassandra, Dynamo
- Graph: Neo4j
- Time-series: InfluxDB, TimescaleDB

## Decision

We chose **PostgreSQL 13+** as the primary database for all microservices.

## Rationale

### 1. ACID Compliance
PostgreSQL provides true ACID (Atomicity, Consistency, Isolation, Durability) guarantees, critical for:
- User account data
- Workout history
- Diet logs
- Financial transactions (if added)

### 2. Powerful Query Language
SQL is expressive and well-suited for:
- Complex reports (workout analytics, progress tracking)
- Aggregations (monthly stats, comparisons)
- Joins across related data
- Full-text search

### 3. Advanced Features
PostgreSQL includes:
- JSON/JSONB for semi-structured data
- Array types for storing collections
- Window functions for analytics
- Partitioning for large tables
- Replication for HA

### 4. Scalability
PostgreSQL can handle:
- Millions of rows efficiently
- Connection pooling (PgBouncer - ADR-008)
- Read replicas for scaling reads
- Partitioning for large tables

### 5. Ecosystem
- Excellent tooling (pgAdmin, DBeaver)
- Strong community and docs
- Multiple cloud hosting options (RDS, Heroku, DigitalOcean)
- Good backup/recovery tools

### 6. Cost
- Free and open source
- Lower operational costs than proprietary databases
- Efficient resource usage

## Consequences

### Positive
- ✅ ACID guarantees for data integrity
- ✅ Powerful querying for analytics
- ✅ Strong community and tooling
- ✅ Good scalability options
- ✅ Cost effective

### Negative
- ❌ Not designed for high write throughput like NoSQL
- ❌ Vertical scaling limitations (needs good hardware)
- ❌ Schema migrations can be complex
- ❌ Requires DBA expertise for optimal configuration
- ❌ Backup/recovery requires planning

## Alternatives Considered

### 1. MongoDB (NoSQL)
- **Pros**: Schema flexibility, horizontal scaling, good for unstructured data
- **Cons**: No ACID, slower queries, eventual consistency, JSON complexity
- **Why not**: Need ACID guarantees and relational queries

### 2. MySQL
- **Pros**: Similar to PostgreSQL, slightly faster in some scenarios
- **Cons**: Fewer advanced features, less extensible
- **Why not**: PostgreSQL is more feature-rich for same price

### 3. Cassandra (NoSQL)
- **Pros**: Extreme horizontal scalability, highly available
- **Cons**: Complex, eventual consistency, not suitable for transactional work
- **Why not**: Overkill for our use case, wrong consistency model

### 4. TimescaleDB (Time-Series)
- **Pros**: Built on PostgreSQL, optimized for time-series data
- **Cons**: More complex, overkill for our current needs
- **Why not**: Standard PostgreSQL sufficient; can upgrade to TimescaleDB later if needed

## Related ADRs

- **Depends on**: ADR-001 (Microservices need independent databases)
- **Related to**: ADR-008 (PgBouncer for connection pooling)
- **Related to**: ADR-010 (HA strategy for databases)
- **Related to**: ADR-011 (Encryption at rest and in transit)

## Mitigation Strategies

1. **Schema Management**: Use migration tools (Flyway, Liquibase) to manage schema changes
2. **Backup Strategy**: Regular automated backups to S3 (ADR-009)
3. **Monitoring**: Prometheus exporters for database metrics (ADR-005)
4. **Connection Pooling**: PgBouncer to optimize connections (ADR-008)
5. **Read Replicas**: For scaling read-heavy operations
6. **Encryption**: Enable encryption at rest and in transit (ADR-011)

## Future Considerations

If needs change:
- Can add TimescaleDB for time-series optimization
- Can add Redis for caching layer (ADR-012)
- Can add specialized stores (e.g., Elasticsearch for search)
- But PostgreSQL will remain primary for transactional data
