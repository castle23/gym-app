# AI Architecture Analysis Prompt

## Context

You are evaluating architecture decisions for the **Gym Platform API**, a Java 17+ /
Spring Boot 3.x microservices system. The project has **12 existing ADRs**:

| ADR   | Decision                        | Status   |
|-------|---------------------------------|----------|
| 001   | Microservices architecture       | Accepted |
| 002   | PostgreSQL as primary database   | Accepted |
| 003   | JWT for authentication           | Accepted |
| 004   | Docker & Kubernetes deployment   | Accepted |
| 005   | Prometheus & Grafana monitoring  | Accepted |
| 006   | Event-driven communication       | Accepted |
| 007   | API Gateway pattern              | Accepted |
| 008   | PgBouncer connection pooling     | Accepted |
| 009   | S3-compatible backups            | Accepted |
| 010   | Disaster Recovery & High Avail.  | Accepted |
| 011   | Security framework & RBAC        | Accepted |
| 012   | Caching strategy (Redis)         | Accepted |

**Services**: Auth (8081), Training (8082), Tracking (8083), Notification (8084).

---

## Instructions

### When Proposing a New ADR

Follow the project's ADR format exactly:

```markdown
# ADR-NNN: [Concise Title]
- **Status**: Proposed
- **Date**: YYYY-MM-DD
- **Context**: [What problem or requirement motivates this decision? Include current
  pain points, constraints, and relevant metrics if available.]
- **Decision**: [What is the chosen approach? Be specific about technologies, patterns,
  and boundaries.]
- **Rationale**: [Why this option over alternatives? Reference evaluation criteria.]
- **Consequences**:
  - Positive: [Benefits gained]
  - Negative: [Trade-offs accepted]
- **Alternatives Considered**: [At least 2 alternatives with brief pros/cons]
- **Mitigations**: [How negative consequences will be addressed]
- **Related ADRs**: [List ADR numbers that this decision affects or depends on]
```

### When Evaluating an Existing Decision

Score the decision against these **evaluation criteria** (1-5 scale each):

| Criterion              | Key Questions                                              |
|------------------------|------------------------------------------------------------|
| **Scalability**        | Does it support 10x growth? Horizontal scaling possible?    |
| **Data Consistency**   | How is consistency maintained? Eventual vs. strong?         |
| **Coupling**           | How tightly are services/components connected?              |
| **Fault Isolation**    | Can one failure cascade across the system?                  |
| **Operational Cost**   | What is the deployment, monitoring, and debugging overhead? |

### Architecture Principles (Non-Negotiable)

1. **Microservice boundaries** align with business domains — no shared databases.
2. **Single responsibility** — each service owns its data and logic.
3. **Loose coupling** — services communicate via APIs or events, never direct DB access.
4. **No cross-service database access** — if you need another service's data, call its API.
5. **Infrastructure as code** — all config reproducible via Docker Compose / K8s manifests.

### Analysis Guidance

- Check for **consistency** with existing ADRs. Flag contradictions.
- Identify **ripple effects** — which services and ADRs are impacted?
- Consider **migration path** — can we adopt incrementally or is it all-or-nothing?
- Assess **reversibility** — how costly is it to undo this decision?
- Verify **operational readiness** — can the team monitor, debug, and deploy this?

---

## Expected Output Format

### For New ADR Proposals
Return a complete ADR draft in the format above, ready for review and commit to
`docs/adr/`.

### For Architecture Evaluations
```markdown
## Architecture Evaluation: [Topic]

### Scores
| Criterion          | Score (1-5) | Notes                       |
|--------------------|-------------|-----------------------------|
| Scalability        | ?           | [Justification]              |
| Data Consistency   | ?           | [Justification]              |
| Coupling           | ?           | [Justification]              |
| Fault Isolation    | ?           | [Justification]              |
| Operational Cost   | ?           | [Justification]              |

### Strengths
- [What works well]

### Risks
- [What could go wrong]

### Recommendations
1. [Actionable improvement]
2. [Actionable improvement]

### Affected ADRs
- ADR-NNN: [How it is affected]
```

---

## References

- [Architecture Decision Records](../../docs/adr/)
