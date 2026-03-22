# Technology Stack

This section documents the technologies used in the Gym Platform API.

## Overview

The Gym Platform API is built with modern, production-ready technologies designed for scalability and maintainability.

## Core Technologies

- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Database**: PostgreSQL
- **API Documentation**: OpenAPI/Swagger
- **Authentication**: JWT with Spring Security
- **Containerization**: Docker
- **Orchestration**: Docker Compose

## Microservices

The platform consists of 5 services:
1. **API Gateway** (Port 8080) - JWT validation, request routing
2. **Auth Service** (Port 8081) - Authentication & User Management
3. **Training Service** (Port 8082) - Training Program Management
4. **Tracking Service** (Port 8083) - Progress Tracking & Analytics
5. **Notification Service** (Port 8084) - User Notifications

## Documentation Files

- **01-java-spring-boot.md** - Java and Spring Boot overview
- **02-database-postgresql.md** - PostgreSQL configuration
- **03-api-design-patterns.md** - API design and patterns
- **04-security-framework.md** - Security implementation
- **05-deployment-docker.md** - Docker and containerization

## Key Dependencies

### Spring Boot Starter Dependencies
- spring-boot-starter-web
- spring-boot-starter-data-jpa
- spring-boot-starter-validation
- gym-common (shared security, exception handling, MDC logging)

### Database
- spring-boot-starter-data-jpa (JPA/Hibernate)
- postgresql (PostgreSQL driver)

### API Documentation
- springdoc-openapi (Swagger/OpenAPI)

### Testing
- spring-boot-starter-test
- junit5
- mockito

### Build & Runtime
- Maven 3.8+
- Java 17+ runtime

## Development Tools

- IDE: IntelliJ IDEA (recommended)
- Version Control: Git
- Docker: Container runtime and development
- Postman: API testing
- pgAdmin: Database administration (optional)

## Environment Variables

Key configuration variables:
- Database URL and credentials
- Service ports
- JWT secret key
- Log levels
- API documentation settings

See `.env.example` for template.

## Configuration

- **Application Configuration**: `application.yml`
- **Pagination**: `application-pagination.yml`
- **Production**: Environment-specific overrides

## Docker Compose

Development and production Docker Compose files:
- `docker-compose.yml` - Development environment
- `docker-compose.prod.yml` - Production environment

Both include all services and PostgreSQL database.

## For More Information

- **Development**: See [Development Documentation](../development/)
- **Architecture**: See [Architecture Documentation](../arquitectura/)
- **Deployment**: See [Deployment Documentation](../deployment/)
