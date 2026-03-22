# Glossary

## Overview

Complete glossary of technical terms, acronyms, and concepts used throughout the Gym Platform documentation and codebase. This document serves as a reference for understanding specialized terminology related to microservices architecture, PostgreSQL databases, Kubernetes deployments, and DevOps operations specific to the Gym Platform.

## Table of Contents

- [Architecture & Infrastructure](#architecture--infrastructure)
- [Database Concepts](#database-concepts)
- [Kubernetes & Deployment](#kubernetes--deployment)
- [Microservices](#microservices)
- [Performance & Monitoring](#performance--monitoring)
- [Security & Authentication](#security--authentication)
- [Data Management](#data-management)
- [Development & DevOps](#development--devops)
- [Quality & Testing](#quality--testing)

---

## Architecture & Infrastructure

**API Gateway**
- Entry point for all client requests
- Validates JWT tokens and injects `X-User-Id` / `X-User-Roles` headers
- Routes requests to appropriate microservices
- Implements rate limiting and request validation
- In Gym Platform: Spring Boot service with custom `JwtAuthFilter`

**Microservices**
- Independent, loosely-coupled services
- Each service owns its data and database schema
- Communicates via REST API (no message queues)
- Deployed and scaled independently
- Gym Platform services: Auth Service, Training Service, Tracking Service, Notification Service

**Service Mesh**
- Infrastructure layer managing service-to-service communication
- Handles routing, load balancing, retries
- Provides observability and security
- Examples: Istio, Linkerd
- Enables sophisticated traffic management and observability

**Load Balancer**
- Distributes traffic across multiple instances
- Ensures high availability
- Can be hardware-based or software-based
- Performs health checks on backend instances

**Reverse Proxy**
- Forwards client requests to backend servers
- Acts as intermediary between client and server
- Can cache responses and balance load
- Examples: Nginx, HAProxy

---

## Database Concepts

**ACID Compliance**
- **Atomicity:** Transaction either fully succeeds or fully fails
- **Consistency:** Data remains valid before and after transaction
- **Isolation:** Concurrent transactions don't interfere
- **Durability:** Committed data persists despite failures
- PostgreSQL provides full ACID compliance

**Replication**
- Process of copying data from primary to replica databases
- Synchronous: Primary waits for replica to confirm
- Asynchronous: Primary doesn't wait for replica
- Used for high availability and read scaling
- Gym Platform: not currently configured (single PostgreSQL instance)

**Write-Ahead Logging (WAL)**
- PostgreSQL writes changes to log before applying to data files
- Enables recovery from failures
- Used for point-in-time recovery and replication
- Essential for data durability

**LSN (Log Sequence Number)**
- Position in the Write-Ahead Log
- Used to track replication progress
- Format: Logical/Physical (e.g., 0/01000000)
- Used to calculate replication lag

**Materialized View**
- Database object containing query results
- Must be manually refreshed
- Provides precomputed results for complex queries
- Can include indexes for fast access

**Btree Index**
- Balanced tree index structure (default in PostgreSQL)
- Efficient for range queries and equality comparisons
- Most commonly used index type
- Good general-purpose index for most use cases

**Hash Index**
- Hash table index structure
- Efficient for equality comparisons only
- Not suitable for range queries
- Less commonly used than Btree

**Composite Key**
- Primary key consisting of multiple columns
- Uniqueness enforced across all columns combined
- Example: (user_id, exercise_id) for workout exercises

**Foreign Key**
- Constraint enforcing referential integrity
- Links column(s) in one table to primary key in another
- Prevents orphaned records
- Can have cascade delete/update rules

**Constraint**
- Rule enforced by database to maintain data integrity
- Types: PRIMARY KEY, FOREIGN KEY, UNIQUE, CHECK, NOT NULL
- Violations prevent data modifications

**Transaction**
- Unit of work consisting of one or more SQL statements
- All-or-nothing execution (ACID compliance)
- Can be rolled back if error occurs
- Isolation level determines how concurrent transactions interact

---

## Kubernetes & Deployment

**Pod**
- Smallest deployable unit in Kubernetes
- Contains one or more containers (usually one)
- Containers in pod share network namespace
- Short-lived: replaced when updated

**Deployment**
- Kubernetes resource managing pod replicas
- Automatically replaces failed pods
- Enables rolling updates
- Specifies desired number of replicas

**StatefulSet**
- Kubernetes resource for stateful applications
- Maintains persistent identity for each pod
- Preserves pod order (consistent naming)
- Used for databases and other stateful services

**ConfigMap**
- Kubernetes object storing non-sensitive configuration
- Key-value pairs injected as environment variables or files
- Separate from application code
- Can be updated without redeploying pods

**Secret**
- Kubernetes object storing sensitive data
- Base64 encoded (not encrypted by default)
- Used for passwords, API keys, certificates
- Should use encryption-at-rest in production

**Service**
- Kubernetes abstraction exposing pods
- Provides stable IP and DNS name
- Types: ClusterIP, NodePort, LoadBalancer
- Enables pod discovery and load balancing

**Ingress**
- Kubernetes resource managing external access
- Routes HTTP(S) traffic to services
- Enables hostname-based routing
- Supports TLS/SSL termination

**DaemonSet**
- Kubernetes resource running pod on every node
- Used for monitoring, logging, networking agents
- One pod per node in cluster
- Automatically added to new nodes

**Namespace**
- Kubernetes logical isolation mechanism
- Allows multiple virtual clusters on one physical cluster
- Resource quotas enforced per namespace
- Default namespace used if not specified

---

## Microservices

**Auth Service**
- Handles user authentication and authorization
- Manages user accounts, roles, email verification
- Issues JWT tokens for API access
- Validates credentials and enforces access control
- Gym Platform service responsible for identity
- Roles: `ROLE_USER`, `ROLE_PROFESSIONAL`, `ROLE_ADMIN`

**Training Service**
- Manages exercises, disciplines, and routines
- Handles exercise library and routine creation
- Stores routine-exercise relationships
- Gym Platform service for training data

**Tracking Service**
- Records body measurements and weight logs
- Manages nutrition objectives and diet logs
- Tracks progress plans and recommendations
- Gym Platform service for health tracking data

**Notification Service**
- Sends notifications to users
- Handles emails, push notifications, SMS
- Manages notification templates and scheduling
- Tracks delivery status and retries
- Gym Platform service for user communications

**Saga Pattern**
- Distributed transaction pattern for microservices
- Sequences of local transactions with compensation
- Maintains consistency across services without distributed transactions
- Each step has rollback/compensation logic

---

## Performance & Monitoring

**Latency**
- Time elapsed between request and response
- Measured in milliseconds
- Target: < 100ms for user-facing API calls
- Affected by network, processing, and I/O time

**Throughput**
- Number of requests processed per unit time
- Measured in requests/second
- Target: > 5000 req/s for Gym Platform
- Limited by database and application capacity

**Query Plan**
- PostgreSQL's execution strategy for a query
- Shows operations like Seq Scan, Index Scan, Join
- Estimated cost and row counts
- Used to optimize queries

**Metrics**
- Quantifiable measurements of system behavior
- Examples: CPU usage, memory usage, request latency
- Collected and stored for analysis
- Used for alerting and capacity planning

**Logs**
- Record of events and errors in system
- Structured (JSON) or unstructured (text)
- Collected and aggregated for analysis
- Used for debugging and auditing

**Tracing**
- Following request path through distributed system
- Shows service calls, timing, and errors
- Enables bottleneck identification
- Tools: Jaeger, Zipkin

**Observability**
- Ability to understand system state from external outputs
- Consists of metrics, logs, and traces
- Essential for operating complex systems
- Enables rapid debugging and incident response

**Alert**
- Notification triggered when metric exceeds threshold
- Used to notify on-call team of issues
- Should be actionable and not fire excessively
- Prevents alert fatigue

---

## Security & Authentication

**JWT (JSON Web Token)**
- Stateless authentication token
- Contains encoded user information and signature
- Signed with secret key
- Token includes expiration time
- Gym Platform uses JWTs for API authentication

**OAuth 2.0**
- Authorization protocol for delegated access
- Enables third-party applications to access user data
- Separates authentication from authorization
- User data provider remains in control

**RBAC (Role-Based Access Control)**
- Authorization model based on user roles
- User assigned to role
- Gym Platform roles: `ROLE_USER`, `ROLE_PROFESSIONAL`, `ROLE_ADMIN`
- Role has permissions checked before allowing operations
- Enforced via `@RequiresRole` annotation in each service

**TLS/SSL**
- Protocols for encrypted communication
- Ensures data confidentiality in transit
- Uses certificates for server authentication
- HTTPS = HTTP + TLS

**Encryption**
- Transformation of plaintext to ciphertext
- Encryption at rest: data encrypted in storage
- Encryption in transit: data encrypted during transmission
- Symmetric (shared key) vs Asymmetric (public/private key)

**Hashing**
- One-way function producing fixed-size digest
- Cannot be reversed to get original value
- Used for password storage
- Same input always produces same hash

**Salt**
- Random data added to password before hashing
- Prevents rainbow table attacks
- Each user has different salt
- Increases security of password storage

**Authentication**
- Verifying user identity (proving who you are)
- Examples: password, API key, certificate
- Must be secure and tamper-resistant

**Authorization**
- Determining what authenticated user can do
- Based on roles, permissions, or policies
- Enforced at application and database level

---

## Data Management

**Schema**
- Structure and organization of database
- Defines tables, columns, relationships
- Gym Platform uses 4 schemas: `auth_schema`, `training_schema`, `tracking_schema`, `notification_schema`
- Different schemas for logical separation within a single `gym_db` database

**Soft Delete**
- Marking record as deleted instead of removing
- Record still exists in database
- filtered out by queries
- Enables recovery and audit trails
- Gym Platform uses soft deletes for data retention

**Hard Delete**
- Permanently removing record from database
- Cannot be recovered without backup
- Used for sensitive data (GDPR compliance)
- Should be rare in production systems

**Denormalization**
- Storing redundant data in tables
- Improves query performance
- Increases complexity and consistency risk
- Used cautiously for performance-critical queries

**Normalization**
- Organizing data to reduce redundancy
- 1NF through 3NF levels
- Improves data consistency
- May require more joins in queries

**Bloat**
- Wasted space in tables or indexes
- Caused by update/delete operations leaving space
- Increases I/O and reduces cache efficiency
- Addressed by VACUUM or REINDEX

**Materialized View**
- Precomputed query results stored as table
- Must be manually refreshed
- Provides performance benefits for complex queries
- Useful for reporting and analytics

---

## Development & DevOps

**CI/CD (Continuous Integration/Continuous Deployment)**
- CI: Automatically build and test code on every commit
- CD: Automatically deploy to production
- Reduces manual effort and errors
- Enables rapid feedback and deployment

**Git**
- Version control system for source code
- Tracks changes and enables collaboration
- Branches for feature development
- Pull requests for code review

**Docker**
- Containerization platform
- Packages application with dependencies
- Enables consistent environment across systems
- Faster startup than VMs, more isolated than processes

**Container**
- Lightweight virtualization of application
- Includes application and dependencies
- More efficient than VMs
- Can be rapidly started and stopped

**Image**
- Template for creating containers
- Contains application code and dependencies
- Built from Dockerfile
- Can be versioned and shared

**Repository**
- Central storage for Docker images
- Examples: Docker Hub, Amazon ECR
- Images retrieved from repository to run as containers

**Environment Variables**
- Dynamic configuration values
- Set at runtime, not hardcoded
- Different values for different environments
- Used for database URLs, API keys, feature flags

**Deployment Strategy**
- Plan for updating application in production
- Rolling: Replace instances one at a time
- Blue-green: Run two complete environments
- Canary: Route small percentage to new version
- Gym Platform: Docker Compose (`docker-compose up -d --build`)

---

## Quality & Testing

**Unit Test**
- Tests individual function or method
- Fast and isolated from other components
- Should have high code coverage
- Typically written by developers

**Integration Test**
- Tests multiple components working together
- Slower than unit tests
- Tests real database and APIs
- Validates interactions between services

**End-to-End Test**
- Tests entire system from user perspective
- Simulates real user workflows
- Slowest type of test
- Most closely mirrors production behavior

**Load Test**
- Applies heavy load to system to test capacity
- Identifies bottlenecks and breaking points
- Tools: JMeter, Locust, Apache Bench
- Results guide capacity planning

**Test Coverage**
- Percentage of code executed by tests
- Higher coverage reduces bug probability
- Target: 80-90% for application code
- Does not guarantee code quality

**Mock**
- Simulated object replacing real dependency
- Enables isolated unit testing
- Faster and more controlled than using real objects
- Useful for testing error scenarios

**Stub**
- Minimal implementation of interface
- Returns canned/test data
- Simpler than mock
- Used for specific return values

**Regression Test**
- Ensures previously fixed bugs don't reappear
- Tests edge cases and error scenarios
- Should be added when fixing bugs
- Prevents future regressions

---

**Related Resources:**
- [02-abbreviations.md](02-abbreviations.md) - Common acronyms
- [03-links-references.md](03-links-references.md) - External references
- See [docs/stack/](../stack/) for architectural details
- See [docs/database/](../database/) for database-specific terms
