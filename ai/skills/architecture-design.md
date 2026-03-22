# Skill: Architecture Design

## Description
Design new components or services within the existing microservices architecture, producing an ADR and component diagram.

## Prerequisites
- Understanding of the 12 existing ADRs in `docs/adr/`
- Knowledge of microservice boundaries and domain contexts
- Familiarity with `ai/rules/project-overview.md`

## Service Map
| Service      | Port | Domain                          | Schema        |
|--------------|------|---------------------------------|---------------|
| Auth         | 8081 | Users, roles, JWT, profiles     | auth_schema   |
| Training     | 8082 | Trainings, trainers, trainees   | training_schema |
| Tracking     | 8083 | Metrics, progress, workouts     | tracking_schema |
| Notification | 8084 | Alerts, emails, notifications   | notification_schema |

## Steps

1. **Review Related ADRs**
   - Read all ADRs in `docs/adr/` relevant to the feature domain
   - Identify constraints and prior decisions that apply
   - Note any ADRs that may need updating or superseding

2. **Identify Domain and Bounded Context**
   - Determine which service owns the feature
   - If it spans services, define clear ownership per entity
   - Map entities to their owning service's bounded context

3. **Define Service Boundaries**
   - Each service owns its database schema exclusively
   - No direct DB access across services
   - Cross-service communication via REST only
   - Define which service is the source of truth for each entity

4. **Design Data Model**
   - Define entities with JPA annotations
   - Define database schema (tables, columns, constraints, indexes)
   - Plan migration scripts (Flyway `V[N]__description.sql`)
   - Consider data that must be duplicated across services (eventual consistency)

5. **Define API Contract**
   - Design endpoints following REST conventions
   - Define request/response DTOs with `@Schema` annotations
   - Plan pagination for list endpoints (`Pageable`)
   - Document error responses and status codes
   - Version APIs under `/api/v1/`

6. **Document in ADR Format**
   - Use ADR template from `ai/skills/documentation-writing.md`
   - Number sequentially after existing ADRs
   - Include context, decision, consequences, and alternatives considered

## Architecture Principles
- **No cross-service DB access** — services communicate via REST
- **Each service owns its schema** — no shared tables
- **Loose coupling** — services can be deployed independently
- **REST for sync communication** — standard HTTP/JSON
- **Eventual consistency** — accept temporary inconsistency between services
- **API-first design** — define contract before implementation
- **Security at every layer** — JWT validation, `@PreAuthorize`, input validation

## Component Design Checklist
- [ ] Fits within a single service's bounded context (or has clear cross-service plan)
- [ ] Entities use `@Entity`, `@Table(schema = "[service]_schema")`
- [ ] Repository extends `JpaRepository<Entity, Long>`
- [ ] Service layer handles business logic and transactions
- [ ] Controller is thin — delegates to service
- [ ] DTOs separate API contract from internal entities
- [ ] Flyway migration for schema changes
- [ ] Security annotations on protected endpoints

## Output
- ADR draft saved to `docs/adr/ADR-NNN-title.md`
- Component diagram description (text-based, Mermaid, or PlantUML)
- List of files to create/modify with package paths

## References
- `docs/adr/` — existing architecture decisions
- `ai/rules/project-overview.md`
- `ai/rules/coding-standards.md`
- `ai/skills/documentation-writing.md` — ADR template
