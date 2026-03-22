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
   │   Auth      │            │   API Gateway    │
   │  Service    │            │   Port: 8080     │
   └─────────────┘            └──────────────────┘
        │                             │
        │                             │ JWT validated here
        │                             │ X-User-Id + X-User-Roles injected
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
   - User role assignment (`ROLE_USER`, `ROLE_PROFESSIONAL`, `ROLE_ADMIN`)
   - Roles stored in `users` table, embedded in JWT
   - Gateway injects roles as `X-User-Roles` header

### API Endpoints

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|------|
| POST | `/register` | Register new user | No |
| POST | `/login` | Authenticate user | No |
| POST | `/verify` | Verify email address | No |
| POST | `/refresh` | Refresh JWT token | Yes |
| GET | `/profile` | Get authenticated user profile | Yes |

> Note: Auth service has context-path `/auth`. Via gateway: `/auth/register`, `/auth/login`, etc.

### Authentication Flow

```
1. Client → POST /auth/login  { email, password }
2. Auth Service validates credentials (BCrypt), returns { token, refreshToken, userId, email }
3. Client includes token: Authorization: Bearer <token>
4. API Gateway validates JWT, injects X-User-Id + X-User-Roles
5. Downstream services read headers via GymRoleInterceptor
```

**Database Schema (auth_schema)**

**Core Tables**:
- `users` - User accounts, credentials, roles, account status
- `verifications` - Email verification codes

### JWT Token Structure

```json
{
  "sub": "<userId>",
  "roles": "ROLE_USER",
  "iat": 1647900000,
  "exp": 1647986400
}
```

Token expiration: 24h (access), 7d (refresh). Configured via `jwt.expiration-ms` / `jwt.refresh-expiration-ms`.

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

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|----- |
| GET | `/api/v1/exercises/system` | List system exercises | No |
| GET | `/api/v1/exercises/discipline/{id}` | Exercises by discipline | No |
| GET | `/api/v1/exercises/my-exercises` | User's exercises | Yes |
| GET | `/api/v1/exercises/{id}` | Get exercise | Yes |
| POST | `/api/v1/exercises` | Create exercise | Yes |
| PUT | `/api/v1/exercises/{id}` | Update exercise | Yes |
| DELETE | `/api/v1/exercises/{id}` | Delete exercise | Yes |
| GET | `/api/v1/routine-templates/system` | System templates | No |
| GET | `/api/v1/routine-templates/my-templates` | User templates | Yes |
| POST | `/api/v1/routine-templates` | Create template | Yes |
| GET | `/api/v1/user-routines` | List user routines | Yes |
| POST | `/api/v1/user-routines/assign` | Assign routine | Yes |
| PATCH | `/api/v1/user-routines/{id}/deactivate` | Deactivate routine | Yes |
| GET | `/api/v1/exercise-sessions/routine/{id}` | Sessions by routine | Yes |
| POST | `/api/v1/exercise-sessions` | Log session | Yes |

> Context-path: `/training`. Via gateway prefix: `/training/api/v1/...`

### Database Schema (training_schema)

**Core Tables**:
- `exercises` - Exercise catalog
- `routine_templates` - Routine template definitions
- `user_routines` - User-assigned routines
- `exercise_sessions` - Logged exercise sessions

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

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|----- |
| GET | `/api/v1/measurements` | List measurements | Yes |
| POST | `/api/v1/measurements` | Record measurement | Yes |
| GET | `/api/v1/measurements/by-type/{id}` | By measurement type | Yes |
| POST | `/api/v1/measurements/types` | Create measurement type | Yes |
| GET | `/api/v1/objectives` | List user objectives | Yes |
| POST | `/api/v1/objectives` | Create objective | Yes |
| GET | `/api/v1/plans` | List user plans | Yes |
| POST | `/api/v1/plans` | Create plan | Yes |
| GET | `/api/v1/diet-logs` | List user diet logs | Yes |
| POST | `/api/v1/diet-logs` | Create diet log | Yes |
| GET | `/api/v1/diet-components/{id}` | Get diet component | Yes |
| POST | `/api/v1/diet-components` | Create diet component | Yes |
| GET | `/api/v1/training-components/{id}` | Get training component | Yes |
| POST | `/api/v1/training-components` | Create training component | Yes |
| GET | `/api/v1/recommendations/{id}` | Get recommendation | Yes |
| POST | `/api/v1/recommendations` | Create recommendation | Yes |

> Context-path: `/tracking`. Via gateway prefix: `/tracking/api/v1/...`

### Database Schema (tracking_schema)

**Core Tables**:
- `measurements` - Body measurements
- `measurement_types` - Measurement type definitions
- `objectives` - User objectives
- `plans` - Tracking plans
- `diet_logs` - Daily diet logs
- `diet_components` - Diet plan components
- `training_components` - Training plan components
- `recommendations` - System recommendations

---

## Notification Service

### Purpose
Delivers notifications to users via multiple channels (email, SMS, push notifications). Manages notification preferences, queues, and history.

### Key Responsibilities

1. **Notification Delivery**
   - Send in-app notifications
   - Firebase Cloud Messaging (FCM) push notifications
   - Track read/unread status
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

| Method | Endpoint | Purpose | Auth |
|--------|----------|---------|----- |
| GET | `/api/v1/notifications` | List user notifications | Yes |
| GET | `/api/v1/notifications/unread` | Unread notifications | Yes |
| GET | `/api/v1/notifications/unread/count` | Unread count | Yes |
| POST | `/api/v1/notifications` | Create notification | Yes |
| PUT | `/api/v1/notifications/{id}/read` | Mark as read | Yes |
| DELETE | `/api/v1/notifications/{id}` | Delete notification | Yes |
| POST | `/api/v1/push-tokens` | Register push token | Yes |
| GET | `/api/v1/push-tokens` | List push tokens | Yes |
| GET | `/api/v1/push-tokens/active` | Active push tokens | Yes |
| DELETE | `/api/v1/push-tokens` | Remove push token | Yes |

> Context-path: `/notifications`. Via gateway prefix: `/notifications/api/v1/...`

### Database Schema (notification_schema)

**Core Tables**:
- `notifications` - User notifications
- `push_tokens` - FCM push token registrations
- `notification_preferences` - User notification preferences

---

## Inter-Service Communication

Services do **not** call each other directly. The API Gateway handles JWT validation and injects user context headers before forwarding requests:

```
Client → API Gateway (JwtAuthFilter)
  → validates JWT
  → injects X-User-Id, X-User-Roles
  → forwards to downstream service

Downstream service (GymRoleInterceptor)
  → reads X-User-Id, X-User-Roles
  → populates UserContextHolder
  → @RequiresRole enforces RBAC
```

No service-to-service REST calls exist in the current implementation.

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
      - SPRING_DATASOURCE_USERNAME=gym_admin
      - SPRING_DATASOURCE_PASSWORD=gym_password
      - SPRING_JPA_HIBERNATE_DDL_AUTO=update
      - JWT_SECRET=${JWT_SECRET}
    depends_on:
      - postgres
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8081/auth/actuator/health"]

  training-service:
    # Similar configuration
    ports: ["8082:8082"]
    depends_on:
      - postgres
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8082/training/actuator/health"]

  tracking-service:
    # Similar configuration
    ports: ["8083:8083"]
    depends_on:
      - postgres
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8083/tracking/actuator/health"]

  notification-service:
    # Similar configuration
    ports: ["8084:8084"]
    depends_on:
      - postgres
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8084/notifications/actuator/health"]

  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=gym_db
      - POSTGRES_USER=gym_admin
      - POSTGRES_PASSWORD=gym_password
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
