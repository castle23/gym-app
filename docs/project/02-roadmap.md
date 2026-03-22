# Roadmap

## Status: ✅ Complete — All 7 phases delivered

---

## Phase 1 — Foundation ✅
**Commit:** `656c14b`

- Git repository and parent Maven project
- Docker Compose with PostgreSQL + 5 services
- Database schema initialization scripts
- `.gitignore`, `.env`, root `README.md`

---

## Phase 2 — API Gateway ✅
**Commit:** `3467aac`

- Spring Cloud Gateway setup
- `TraceIdMdcFilter` — generates and propagates `X-Trace-Id`
- `JwtAuthFilter` — validates JWT, injects `X-User-Id` / `X-User-Roles`
- Route mapping to all 4 downstream services
- Local and Docker environment configuration

---

## Phase 3 — Auth Service ✅
**Commit:** `9b1a72d`

- Entities: `User`, `Verification`, `ProfessionalRequest`
- Services: `JwtService`, `EmailService`, `AuthService`
- Controller: `AuthController` (register / login / verify / professional request)
- Integration tests with TestContainers — 85%+ coverage

---

## Phase 4 — Training Service ✅

### 4a — Repositories, DTOs & Services
- 5 repositories: `Discipline`, `Exercise`, `RoutineTemplate`, `UserRoutine`, `ExerciseSession`
- 8 DTOs (request + response pairs)
- 4 services: `ExerciseService`, `RoutineTemplateService`, `UserRoutineService`, `ExerciseSessionService`
- 40+ unit tests with Mockito + TestContainers integration tests

### 4b — Controllers & Tests
- 4 controllers: `ExerciseController`, `RoutineTemplateController`, `UserRoutineController`, `ExerciseSessionController`
- 40+ controller tests with MockMvc (`@WebMvcTest`)
- 85%+ end-to-end coverage

---

## Phase 5 — Tracking Service ✅

### 5a — Repositories, DTOs & Services
- 8 repositories: `MeasurementType`, `MeasurementValue`, `Objective`, `Plan`, `TrainingComponent`, `DietComponent`, `Recommendation`, `DietLog`
- 16 DTOs (request + response pairs)
- 7 services: `MeasurementService`, `ObjectiveService`, `PlanService`, `TrainingComponentService`, `DietComponentService`, `RecommendationService`, `DietLogService`
- 60+ unit tests — 85%+ coverage

### 5b — Controllers & Tests
- 7 controllers covering all tracking domains
- 50+ controller tests with MockMvc
- 85%+ end-to-end coverage

---

## Phase 6 — Notification Service ✅
**Commit:** `8f0bc2d`

- Entities: `Notification`, `PushToken`, `NotificationPreference`, `NotificationType`
- 3 repositories with 11 custom query methods
- 6 DTOs with validation
- Firebase Cloud Messaging configuration (graceful degradation in dev)
- 3 services: `NotificationService` (6 methods), `PushTokenService` (7 methods), `NotificationPreferenceService` (6 methods)
- 2 controllers: `NotificationController` (6 endpoints), `PushTokenController` (4 endpoints)
- `GlobalExceptionHandler` — maps 6 exception types to HTTP status codes
- **71 tests total** — all passing, ~90% coverage

---

## Phase 7 — Integration & Deployment ✅
**Commits:** `692b6cb`, `a5e3734`, `c54857e`, `1667064`

- OpenAPI/Swagger `@Operation` + `@ApiResponse` annotations on all 80 endpoints
- Swagger paths excluded from security across all 4 services
- Docker deployment verified — all 6 containers running
- Postman collection and testing guide created
- 31,000+ words of production documentation across `docs/`

---

## Implemented Flows

All flows are documented with Mermaid diagrams in [`docs/arquitectura/diagrams/`](../arquitectura/diagrams/).

### Authentication & Authorization
- ✅ User registration (email + password, BCrypt hashing)
- ✅ Login → JWT access token (24h) + refresh token (7d)
- ✅ Token refresh
- ✅ Authenticated request via API Gateway (JWT validation → X-User-Id / X-User-Roles injection)
- ✅ RBAC enforcement via `@RequiresRole` (`ROLE_USER`, `ROLE_PROFESSIONAL`, `ROLE_ADMIN`)

### Training
- ✅ Browse system exercise catalog (public, paginated)
- ✅ Browse exercises by discipline (public, paginated)
- ✅ Create / update / delete custom exercises (owner-only)
- ✅ Browse system routine templates (public, paginated)
- ✅ Create custom routine templates
- ✅ Assign routine template to user
- ✅ Deactivate user routine
- ✅ Log exercise sessions (sets, reps, weight, duration)
- ✅ Query session history by routine or date

### Tracking
- ✅ Create custom measurement types
- ✅ Record body measurements over time
- ✅ Query measurement history by type
- ✅ Create fitness objectives
- ✅ Create diet & training plans with components
- ✅ Log daily diet entries
- ✅ Create and retrieve recommendations

### Notifications
- ✅ Create in-app notifications
- ✅ Deliver push notifications via Firebase FCM
- ✅ List / read / delete notifications
- ✅ Mark notifications as read
- ✅ Get unread count
- ✅ Register / deactivate push tokens (Android & iOS)
- ✅ Manage notification preferences per user

---

## Upcoming Flows

Flows not yet implemented, ordered by priority.

### High Priority
- ⬜ Email verification on registration (token sent, link clicked)
- ⬜ Password reset (forgot password → email → reset link)
- ⬜ Professional registration request & admin approval
- ⬜ Trainer assigns routine/plan to a client user
- ⬜ Automatic notification triggered on session logged or goal achieved

### Medium Priority
- ⬜ Progress analytics — strength trends, personal bests, volume over time
- ⬜ Recommendation engine — auto-generate recommendations based on tracking data
- ⬜ Plan templates — reusable diet/training plan templates
- ⬜ Scheduled push notifications (workout reminders, check-ins)
- ⬜ Pagination cursor-based (replace offset for large datasets)

### Future / Aspirational
- ⬜ Flutter mobile client — full auth + training + tracking + notification flows
- ⬜ Web dashboard — admin panel, analytics, user management
- ⬜ Social features — challenges, leaderboards, community feed
- ⬜ Payment & subscription flow (premium tiers)
- ⬜ CI/CD pipeline — automated build, test, deploy on push
- ⬜ Observability — Prometheus metrics, Grafana dashboards, ELK logs

---

## Future Phases (Not Started)

| Phase | Description |
|-------|-------------|
| Flutter mobile app | iOS/Android client |
| Web dashboard | React/Vue frontend |
| Advanced analytics | Progress tracking, ML recommendations |
| Social features | Communities, challenges |
| Payment integration | Premium subscriptions |

> Features listed as aspirational in docs (Kubernetes, Prometheus/Grafana, Redis, RabbitMQ, ELK, CI/CD, blue-green deployments, Flyway/Liquibase, PgBouncer) are **not implemented** in the current codebase.

---

## Completion Reports

Detailed per-phase reports are in [`completion-reports/`](completion-reports/):

| File | Contents |
|------|----------|
| `ALL_PHASES_COMPLETION_SUMMARY.md` | Executive summary across all phases |
| `PROJECT_COMPLETION_SUMMARY.md` | Swagger/OpenAPI implementation summary |
| `PHASE_1_ENHANCEMENTS_COMPLETED.md` | OpenAPI annotation details per service |
| `PHASE_2_TESTING_STATUS.md` | Testing infrastructure and deployment status |
| `PHASE_3_PRODUCTION_DOCUMENTATION_COMPLETE.md` | Production docs deliverables |
| `PHASE_4A_TRAINING_REPOSITORIES_SERVICES.md` | Training repos, DTOs, services (with full code) |
| `PHASE_4B_TRAINING_CONTROLLERS.md` | Training controllers and tests (with full code) |
| `PHASE_5A_IMPLEMENTATION_PLAN.md` | Tracking service implementation plan |
| `PHASE_5A_TRACKING_REPOSITORIES_SERVICES.md` | Tracking repos, DTOs, services |
| `PHASE_5B_TRACKING_CONTROLLERS.md` | Tracking controllers and tests |
| `PHASE_6_NOTIFICATION_SERVICE.md` | Notification service implementation plan |
| `PHASE_6_CONTROLLERS_TESTS_COMPLETE.md` | Notification controllers + 18 tests |
| `PHASE_6_COMPLETION_SUMMARY.md` | Phase 6 final summary (71 tests) |
| `PHASE_6_FINAL_VERIFICATION.md` | Detailed verification of all Phase 6 deliverables |
| `PHASE_6_QUICK_REFERENCE.md` | Quick reference for notification endpoints |
| `PHASE_7_INTEGRATION_DEPLOYMENT.md` | Integration and Docker deployment checklist |
| `VERIFICATION_REPORT.md` | Cross-service verification findings |
