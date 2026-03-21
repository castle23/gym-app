# Architecture Documentation

This section contains the system architecture and design documentation for the Gym Platform API.

## Contents

- **01-overview.md** - High-level system overview
- **02-microservices-architecture.md** - Microservices design and component breakdown
- **03-database-schema.md** - Database schema and data models
- **04-api-design.md** - API design principles and patterns
- **05-security-architecture.md** - Security architecture and design

## Subdirectories

- **diagrams/** - Architecture diagrams and visual representations
- **decisions/** - Architecture Decision Records (ADRs)

## Key Concepts

The Gym Platform API is built on a microservices architecture with 4 core services:
1. **Auth Service** - Authentication and authorization (RBAC)
2. **Training Service** - Training program management
3. **Tracking Service** - Progress tracking and analytics
4. **Notification Service** - User notifications

All services are backed by a shared PostgreSQL database and communicate through REST APIs.

## For Developers

Start with **01-overview.md** to understand the overall system design, then dive into specific service documentation.

## For DevOps

Review **02-microservices-architecture.md** and **03-database-schema.md** for deployment planning.

## For Security

Refer to **05-security-architecture.md** for security design considerations.
