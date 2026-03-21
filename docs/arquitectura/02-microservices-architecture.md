# Microservices Architecture

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Auth Service](#auth-service)
3. [Training Service](#training-service)
4. [Tracking Service](#tracking-service)
5. [Notification Service](#notification-service)
6. [Inter-Service Communication](#inter-service-communication)
7. [Deployment Architecture](#deployment-architecture)

## Architecture Overview

The Gym Platform API implements a **microservices architecture** where each service is independently deployable and scalable. Services communicate via RESTful HTTP APIs and share a common PostgreSQL database (with separate schemas for isolation).

### Service Interaction Model

```
┌──────────────────────────────────────────────────────┐
│              Client Applications                     │
│          (Web, Mobile, API Integrations)             │
└──────────────────────┬───────────────────────────────┘
                       │ REST/HTTP
        ┌──────────────┴──────────────┐
        ▼                             ▼
   ┌─────────────┐            ┌──────────────────┐
   │   Auth      │◄──────────►│   API Gateway    │
   │  Service    │            │   (Optional)     │
   └─────────────┘            └──────────────────┘
        │                             │
        │ JWT Validation              │
        │ Authorization               │
        │                             │
    ┌───┴───────────────────────────────┐
    │                                   │
    ▼                                   ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Training   │  │   Tracking   │  │Notification  │
│   Service    │  │   Service    │  │   Service    │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                 │
       └─────────────────┼─────────────────┘
                         │
               ┌─────────▼──────────┐
               │   PostgreSQL DB    │
               │  (gym_db)          │
               │                    │
               │ • auth_schema      │
               │ • training_schema  │
               │ • tracking_schema  │
               │ • notif_schema     │
               └────────────────────┘
```

## Auth Service

### Purpose
Central authentication and authorization service providing secure access control for all other services and client applications.

### Key Responsibilities

1. **User Authentication**
   - User registration with validation
   - Login with credential verification
   - Password hashing and security
   - Account lockout after failed attempts

2. **JWT Token Management**
   - Token generation on successful login
   - Token validation for all requests
   - Token refresh for session extension
   - Token revocation on logout

3. **Authorization & RBAC**
   - User role assignment (ADMIN, MANAGER, USER, TRAINER)
   - Permission definition and enforcement
   - Endpoint-level access control
   - Resource-level authorization checks

4. **Session Management**
   - Secure session handling
   - Token expiration policies
   - Concurrent session limits
   - Login history tracking

### API Endpoints

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|------|
| POST | `/api/v1/auth/login` | Authenticate user | No |
| POST | `/api/v1/auth/register` | Create new user | No |
| POST | `/api/v1/auth/refresh` | Refresh JWT token | Yes |
| POST | `/api/v1/auth/logout` | Invalidate token | Yes |
| GET | `/api/v1/auth/verify` | Verify token validity | Yes |
| GET | `/api/v1/auth/user` | Get current user info | Yes |
| GET | `/api/v1/auth/users` | List all users | Admin |
| POST | `/api/v1/auth/roles` | Create role | Admin |
| GET | `/api/v1/auth/roles` | List roles | Admin |
| POST | `/api/v1/auth/permissions` | Assign permission | Admin |

### Authentication Flow

```
1. Client sends credentials (email, password)
   └─► POST /api/v1/auth/login

2. Auth Service validates credentials
   └─► Query auth_schema.users
   └─► Compare hashed password

3. If valid, generate JWT token
   └─► Create token with user claims
   └─► Sign with private key
   └─► Set expiration (default: 1 hour)

4. Return token to client
   └─► Response: { token: "jwt...", expiresIn: 3600 }

5. Client includes token in subsequent requests
   └─► Header: Authorization: Bearer jwt...

6. Auth Service validates token on each request
   └─► Verify signature
   └─► Check expiration
   └─► Extract user claims
   └─► Allow/deny based on permissions
```

### Database Schema (auth_schema)

**Core Tables**:
- `users` - User accounts and credentials
- `roles` - Role definitions (ADMIN, MANAGER, USER, TRAINER)
- `permissions` - Granular permissions
- `user_roles` - User-to-role mappings
- `role_permissions` - Role-to-permission mappings
- `login_history` - Audit log of logins

### JWT Token Structure

```json
{
  "sub": "user@example.com",
  "userId": 123,
  "roles": ["MANAGER", "USER"],
  "permissions": ["CREATE_PROGRAM", "VIEW_TRACKING"],
  "iat": 1647900000,
  "exp": 1647903600,
  "iss": "gym-platform-api"
}
```

---

## Training Service

### Purpose
Manages all training programs, exercises, workouts, and training plans. Provides templates and tools for trainers and users to create structured fitness routines.

### Key Responsibilities

1. **Program Management**
   - Create and update training programs
   - Define program goals and duration
   - Associate exercises with programs
   - Track program versions and changes

2. **Exercise Library**
   - Maintain comprehensive exercise catalog
   - Describe proper form and technique
   - Categorize by muscle groups
   - Track equipment requirements
   - Store images/videos references

3. **Workout Creation**
   - Build workout templates from exercises
   - Define sets, reps, and rest periods
   - Create workout sequences
   - Manage difficulty levels
   - Schedule workouts in plans

4. **Training Plans**
   - Create multi-week training schedules
   - Progression planning
   - Periodization support
   - Load management

### API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/v1/training/programs` | List programs |
| POST | `/api/v1/training/programs` | Create program |
| GET | `/api/v1/training/programs/{id}` | Get program details |
| PUT | `/api/v1/training/programs/{id}` | Update program |
| GET | `/api/v1/training/exercises` | List exercises |
| POST | `/api/v1/training/exercises` | Add exercise |
| GET | `/api/v1/training/workouts` | List workouts |
| POST | `/api/v1/training/workouts` | Create workout |
| GET | `/api/v1/training/plans` | List training plans |
| POST | `/api/v1/training/plans` | Create plan |

### Database Schema (training_schema)

**Core Tables**:
- `programs` - Training program definitions
- `exercises` - Exercise catalog
- `exercise_categories` - Exercise categorization
- `workouts` - Workout templates
- `workout_sets` - Sets within workouts
- `training_plans` - Multi-week schedules
- `plan_workouts` - Workout assignments to plans

---

## Tracking Service

### Purpose
Records and analyzes user performance data. Provides metrics, progress tracking, analytics, and reporting capabilities.

### Key Responsibilities

1. **Workout Logging**
   - Record completed workout sessions
   - Track sets, reps, weight lifted
   - Monitor exercise form and technique
   - Time workout duration

2. **Performance Metrics**
   - Calculate one-rep max (1RM)
   - Track volume (total weight × reps)
   - Monitor exercise progression
   - Calculate calorie burn estimates

3. **Progress Analytics**
   - Generate strength trends
   - Identify weak areas
   - Recommend adjustments
   - Calculate personal bests

4. **Reporting**
   - Weekly/monthly progress reports
   - Performance comparisons
   - Goal achievement tracking
   - Visual charts and graphs

### API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/tracking/workouts/log` | Log workout completion |
| GET | `/api/v1/tracking/workouts` | Get logged workouts |
| GET | `/api/v1/tracking/metrics` | Get performance metrics |
| GET | `/api/v1/tracking/progress` | Get progress analytics |
| GET | `/api/v1/tracking/reports` | Generate reports |
| GET | `/api/v1/tracking/achievements` | List achievements |
| POST | `/api/v1/tracking/goals` | Set goals |

### Database Schema (tracking_schema)

**Core Tables**:
- `workout_logs` - Logged workout sessions
- `exercise_logs` - Individual exercise records
- `set_logs` - Set-level performance data
- `performance_metrics` - Calculated metrics
- `progress_analytics` - Aggregated analytics
- `reports` - Generated reports
- `achievements` - User achievements and milestones

---

## Notification Service

### Purpose
Delivers notifications to users via multiple channels (email, SMS, push notifications). Manages notification preferences, queues, and history.

### Key Responsibilities

1. **Notification Delivery**
   - Send notifications via email
   - Send SMS messages
   - Send push notifications
   - Track delivery status

2. **Template Management**
   - Create notification templates
   - Store template versions
   - Support template variables
   - Manage template testing

3. **User Preferences**
   - Define notification settings per user
   - Manage communication channel preferences
   - Frequency control (digest vs. real-time)
   - Opt-in/opt-out management

4. **Queue Management**
   - Queue notifications for delivery
   - Retry failed deliveries
   - Handle rate limiting
   - Batch notifications efficiently

### API Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/v1/notification/send` | Send notification |
| GET | `/api/v1/notification/history` | Get notification history |
| GET | `/api/v1/notification/preferences` | Get user preferences |
| PUT | `/api/v1/notification/preferences` | Update preferences |
| GET | `/api/v1/notification/templates` | List templates |
| POST | `/api/v1/notification/templates` | Create template |
| GET | `/api/v1/notification/queue` | View notification queue |

### Database Schema (notification_schema)

**Core Tables**:
- `notifications` - Sent notifications
- `notification_queue` - Pending notifications
- `notification_history` - Archive of sent notifications
- `templates` - Notification templates
- `template_versions` - Template version history
- `user_preferences` - User notification settings
- `delivery_logs` - Delivery attempt tracking

---

## Inter-Service Communication

### Service-to-Service Calls

Services communicate synchronously via RESTful HTTP calls when immediate responses are needed:

```java
// Example: Training Service calling Auth Service
RestTemplate restTemplate = new RestTemplate();
String token = request.getHeader("Authorization");
ResponseEntity<UserDto> response = restTemplate.exchange(
  "http://auth-service:8081/api/v1/auth/verify",
  HttpMethod.GET,
  new HttpEntity<>(new HttpHeaders() {{ set("Authorization", token); }}),
  UserDto.class
);
```

### Common Patterns

1. **Authentication Check**
   - Each service verifies JWT token with Auth Service
   - Auth Service caches validation results
   - Token claims trusted after validation

2. **Authorization Check**
   - Services check user roles/permissions locally
   - Auth Service provides role claims in JWT
   - No need for second Auth Service call

3. **Data Queries**
   - Services query their own schema only
   - Cross-schema queries only when necessary
   - Minimize coupling between services

### Error Handling

Services handle communication failures gracefully:

```java
try {
  // Call Auth Service
} catch (RestClientException e) {
  // Log error
  // Return 503 Service Unavailable
  // Or use cached/default response
}
```

---

## Deployment Architecture

### Container Layout

Each service runs in its own container:

```yaml
services:
  auth-service:
    image: gym-platform/auth-service:latest
    ports: ["8081:8081"]
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/gym_db
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_JPA_HIBERNATE_DDL_AUTO=validate
      - JWT_SECRET_KEY=your-secret-key
    depends_on:
      - postgres
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/health"]

  training-service:
    # Similar configuration
    ports: ["8082:8082"]
    depends_on:
      - postgres
      - auth-service

  tracking-service:
    # Similar configuration
    ports: ["8083:8083"]
    depends_on:
      - postgres
      - auth-service

  notification-service:
    # Similar configuration
    ports: ["8084:8084"]
    depends_on:
      - postgres
      - auth-service

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=gym_db
      - POSTGRES_PASSWORD=postgres
    volumes:
      - ./dba/initialization/schemas:/docker-entrypoint-initdb.d
      - postgres_data:/var/lib/postgresql/data
    ports: ["5432:5432"]
```

### Scaling Considerations

1. **Horizontal Scaling**
   - Each service can run multiple instances
   - Load balancer distributes traffic
   - Session state in database, not memory

2. **Database Scaling**
   - Read replicas for read-heavy operations
   - Connection pooling (HikariCP)
   - Query optimization with proper indexes

3. **Caching**
   - JWT validation caching (5-10 seconds)
   - Frequent queries cached (Redis ready)
   - Cache invalidation strategy

---

## Related Documentation

- [System Overview](01-overview.md)
- [Database Schema](03-database-schema.md)
- [API Design](04-api-design.md)
- [Security Architecture](05-security-architecture.md)
- [Deployment Guide](../deployment/)
