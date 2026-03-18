# Gym Management Platform

Microservices-based gym management platform with centralized authentication, training tracking, meal planning, and push notifications.

## Services

- **API Gateway**: Central entry point for all client requests
- **Auth Service**: User registration, JWT authentication, role management
- **Training Service**: Exercise management, routines, sessions
- **Tracking Service**: Measurement tracking, plans, recommendations
- **Notification Service**: In-app and push notifications

## Quick Start

```bash
docker-compose up
```

Services will be available at:
- API Gateway: http://localhost:8080
- Auth Service: http://localhost:8081
- Training Service: http://localhost:8082
- Tracking Service: http://localhost:8083
- Notification Service: http://localhost:8084

## Architecture

- **Framework**: Spring Boot 3.2.0
- **Database**: PostgreSQL (single instance, separate schemas per service)
- **Authentication**: JWT (centralized in API Gateway)
- **Tracing**: Distributed tracing with Trace ID correlation
- **Frontend**: Flutter (mobile)

## Documentation

See `docs/` for detailed specifications and API documentation.
