# Gym Platform API - Production Deployment Guide

## Table of Contents

1. [Overview](#overview)
2. [Prerequisites](#prerequisites)
3. [Pre-Deployment Checklist](#pre-deployment-checklist)
4. [Deployment Architecture](#deployment-architecture)
5. [Database Setup](#database-setup)
6. [Microservices Configuration](#microservices-configuration)
7. [Docker Container Deployment](#docker-container-deployment)
8. [SSL/TLS Configuration](#ssltls-configuration)
9. [Load Balancing](#load-balancing)
10. [Monitoring & Logging](#monitoring--logging)
11. [Backup & Disaster Recovery](#backup--disaster-recovery)
12. [Rollback Procedures](#rollback-procedures)

---

## Overview

The Gym Platform API is a microservices-based architecture consisting of four independent services:

- **Auth Service** (Port 8081): Authentication and user management
- **Training Service** (Port 8082): Exercise and routine management
- **Tracking Service** (Port 8083): Progress and objective tracking
- **Notification Service** (Port 8084): Email and push notifications

All services share a single PostgreSQL database (`gym_db`) with separate schemas per service. JWT authentication is validated centrally by the API Gateway — individual services trust the `X-User-Id` and `X-User-Roles` headers injected by the gateway. The deployment targets a production environment using Docker Compose on a dedicated server.

### Architecture Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                    Host Machine                              │
│  Client → Port 8080 (API Gateway)                           │
└──────────────────────┬───────────────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────────────┐
│                Docker Network (gym-network)                  │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐    │
│  │  API Gateway (8080)                                 │    │
│  │  - Validates JWT                                    │    │
│  │  - Injects X-User-Id, X-User-Roles headers          │    │
│  │  - Routes to downstream services                    │    │
│  └──────┬──────────┬──────────┬──────────┬─────────────┘    │
│         │          │          │          │                   │
│  ┌──────▼──┐ ┌─────▼───┐ ┌───▼─────┐ ┌──▼──────────┐       │
│  │  Auth   │ │Training │ │Tracking │ │Notification │       │
│  │  (8081) │ │ (8082)  │ │ (8083)  │ │   (8084)    │       │
│  └──────┬──┘ └─────┬───┘ └───┬─────┘ └──┬──────────┘       │
│         └──────────┴──────────┴──────────┘                   │
│                           │                                  │
│                  ┌────────▼────────┐                         │
│                  │  PostgreSQL 15  │                         │
│                  │  (Port 5432)    │                         │
│                  │  gym_db         │                         │
│                  └─────────────────┘                         │
└──────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

### System Requirements

- **Operating System**: Linux (CentOS 7+, Ubuntu 18.04+) or Windows Server 2019+
- **CPU**: Minimum 4 cores (8+ recommended for production)
- **RAM**: Minimum 8GB (16GB+ recommended)
- **Disk Space**: Minimum 50GB (100GB+ recommended for growth)
- **Network**: Stable internet connection with 1Gbps+ bandwidth

### Software Requirements

- **Docker**: Version 20.10+ ([Installation Guide](https://docs.docker.com/install/))
- **Docker Compose**: Version 1.29.0+ ([Installation Guide](https://docs.docker.com/compose/install/))
- **Java JDK**: Version 17+ (for local development/troubleshooting)
- **PostgreSQL Client**: psql 13+ (for database management)
- **Git**: Version 2.25+ (for version control)
- **OpenSSL**: Latest stable version (for SSL/TLS certificate generation)

### Access Requirements

- Administrator/sudo access for Docker operations
- Access to domain registrar (for SSL certificates)
- Access to email service (for sending notifications)
- Access to cloud storage (for backups - optional but recommended)

---

## Pre-Deployment Checklist

Before deploying to production, verify the following:

### Infrastructure
- [ ] Server provisioned with required specifications
- [ ] Network connectivity verified (ping external services)
- [ ] Firewall rules configured (ports 8081-8084, 5432 for internal)
- [ ] DNS records updated to point to server IP
- [ ] SSL/TLS certificates obtained from CA (Let's Encrypt recommended)
- [ ] Backup storage configured (AWS S3, Google Cloud, or local)
- [ ] Monitoring tools installed (Prometheus, Grafana - optional)

### Software & Dependencies
- [ ] Docker installed and verified (`docker --version`)
- [ ] Docker Compose installed and verified (`docker-compose --version`)
- [ ] Docker daemon configured to start on boot
- [ ] All required environment variables documented
- [ ] Database credentials generated and secured
- [ ] JWT secrets generated and stored securely
- [ ] SMTP credentials for email notifications configured

### Database
- [ ] Database backups scheduled (daily recommended)
- [ ] Database monitoring configured
- [ ] Connection pooling parameters tuned
- [ ] Initial data loaded and verified
- [ ] Data retention policies defined

### Code & Configuration
- [ ] Code merged to production branch
- [ ] All tests passing locally
- [ ] Environment configuration files prepared
- [ ] Secrets stored in secure vault (not in Git)
- [ ] Application versions documented
- [ ] Release notes prepared

### Documentation
- [ ] Deployment runbook prepared
- [ ] Architecture diagrams documented
- [ ] API documentation updated
- [ ] Troubleshooting guide available
- [ ] Team trained on deployment procedures

---

## Deployment Architecture

### Service Communication Pattern

All client requests go through the API Gateway. There is no direct service-to-service communication.

```
Client Request
      ↓
   8080 (API Gateway)
      │
      ├─→ Validate JWT (gateway only)
      ├─→ Inject X-User-Id, X-User-Roles headers
      └─→ Proxy to target service (auth/training/tracking/notifications)
            │
            ├─→ Read X-User-Id / X-User-Roles headers
            ├─→ Query own schema in gym_db
            └─→ Return response
```

Each service maintains its own connection to the shared database with appropriate schema isolation:

- **auth_schema**: User authentication, roles, permissions
- **training_schema**: Exercises, routines, programs
- **tracking_schema**: Plans, objectives, progress
- **notification_schema**: Email logs, push tokens, templates

### Service Dependencies

```
API Gateway (8080)
├── Depends on: postgres (healthcheck)
└── Validates JWT for all downstream services

Auth Service (8081)
├── Depends on: postgres (healthcheck)
└── No inter-service communication

Training Service (8082)
├── Depends on: postgres (healthcheck)
└── No inter-service communication

Tracking Service (8083)
├── Depends on: postgres (healthcheck)
└── No inter-service communication

Notification Service (8084)
├── Depends on: postgres (healthcheck)
├── Requires: firebase-config.json mounted at /app/firebase-config.json
└── No inter-service communication
```

All services start after postgres passes its healthcheck (`pg_isready -U gym_admin`).

---

## Database Setup

### Initial Database Creation

> **Note**: When using Docker Compose, the database is created automatically by the `postgres` container using the `POSTGRES_USER`, `POSTGRES_PASSWORD`, and `POSTGRES_DB` environment variables. The initialization scripts in `dba/initialization/` are mounted and executed on first start. Manual setup is only needed if deploying PostgreSQL outside of Docker Compose.

1. **Connect to PostgreSQL**

```bash
psql -h localhost -U gym_admin -d gym_db
```

2. **Create Database and User** (manual setup only)

```sql
-- Create gym database
CREATE DATABASE gym_db
  OWNER gym_admin
  ENCODING 'UTF8';

-- Create application user
CREATE USER gym_admin WITH PASSWORD 'your_secure_password_here';

-- Grant privileges
GRANT CONNECT ON DATABASE gym_db TO gym_admin;
GRANT CREATE ON DATABASE gym_db TO gym_admin;

--
