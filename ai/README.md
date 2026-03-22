# AI & Automation Context

Structured context for AI tools, automation, and intelligent systems working with the Gym Platform API project.

## Directory Structure

```
ai/
├── README.md              ← You are here
├── memory/                ← Project knowledge base (4 files)
├── rules/                 ← Coding, testing, security, documentation, git standards (5 files)
├── prompts/               ← Reusable prompts for code review, security, architecture (6 files)
├── agents/                ← AI agent definitions with roles and restrictions (6 files)
├── skills/                ← Actionable skills for analysis, generation, refactoring (5 files)
├── tasks/                 ← End-to-end task workflows (6 files)
└── plans/                 ← Implementation plans and roadmap (5 files)
```

**Total files**: 38 | **Last updated**: 2026-03-22

---

## Quick Start by Use Case

### Code Review
```
1. Load: prompts/code-review.md
2. Load: rules/coding-standards.md + rules/security-standards.md
3. Provide: Code to review
4. Output: Findings table with severity, location, issue, suggestion
```

### Write New Tests
```
1. Load: rules/testing-standards.md (CRITICAL: read the gotchas section)
2. Load: skills/test-generation.md
3. Identify: Class to test + test type (unit/controller/integration)
4. Output: Test file with correct annotations and assertions
```

### Implement a Feature
```
1. Load: tasks/feature-implementation.md
2. Load: rules/coding-standards.md
3. Follow: Branch → DTOs → Entity → Repository → Service → Controller → Tests → Commit
```

### Fix a Bug
```
1. Load: prompts/bug-investigation.md
2. Load: tasks/bug-fix.md
3. Follow: Reproduce → Failing test → Diagnose → Fix → Verify → Commit
```

### Deploy
```
1. Load: tasks/deployment.md
2. Follow: Pre-flight → Backup → Build → Health check → Smoke test → Monitor
```

### Architecture Decision
```
1. Load: prompts/architecture-analysis.md
2. Load: memory/03-decisions.md (12 existing ADRs)
3. Output: ADR draft following project format
```

### Security Audit
```
1. Load: prompts/security-review.md
2. Load: rules/security-standards.md
3. Output: Security findings report with OWASP mapping
```

---

## Contents

### memory/ — Project Knowledge Base
| File | Description |
|------|-------------|
| `01-project-context.md` | Architecture, services, endpoints, tech stack, quick start |
| `02-team-knowledge.md` | Expertise areas, design patterns, troubleshooting knowledge |
| `03-decisions.md` | 12 Architecture Decision Records (condensed) |
| `04-lessons-learned.md` | Phase-specific lessons, challenges, recommendations |

### rules/ — Standards & Conventions
| File | Description |
|------|-------------|
| `coding-standards.md` | Naming, SOLID, error handling, logging, package structure |
| `documentation-standards.md` | Swagger annotations, commit messages, doc templates |
| `testing-standards.md` | Test types, annotations, 85% coverage, critical gotchas |
| `security-standards.md` | JWT, RBAC, 5 security layers, input validation |
| `git-workflow.md` | Branching, semver, changelog, pre-commit checks |

### prompts/ — Reusable AI Prompts
| File | Description |
|------|-------------|
| `code-review.md` | Structured code review with severity levels |
| `documentation-writing.md` | Generate docs from source code |
| `architecture-analysis.md` | Evaluate and draft architecture decisions |
| `bug-investigation.md` | Systematic bug diagnosis methodology |
| `performance-analysis.md` | JVM, DB, and application performance tuning |
| `security-review.md` | OWASP Top 10 adapted to this project |

### agents/ — AI Agent Definitions
| File | Role |
|------|------|
| `code-agent.md` | Generate and modify Java/Spring Boot code |
| `docs-agent.md` | Create and update technical documentation |
| `test-agent.md` | Generate tests with correct annotations |
| `architect-agent.md` | Evaluate and propose architecture decisions |
| `review-agent.md` | Review code, docs, and designs |
| `ops-agent.md` | Deploy, monitor, and troubleshoot |

### skills/ — Actionable Procedures
| File | What It Does |
|------|-------------|
| `code-analysis.md` | Analyze code quality, generate structured report |
| `documentation-writing.md` | Generate Markdown docs from source code |
| `test-generation.md` | Generate tests with correct templates per type |
| `architecture-design.md` | Design new components within architecture |
| `refactoring.md` | Safe refactoring guided by tests |

### tasks/ — End-to-End Workflows
| File | Workflow For |
|------|-------------|
| `code-refactoring.md` | Refactoring with test verification |
| `feature-implementation.md` | Full feature from branch to merge |
| `bug-fix.md` | Bug diagnosis and fix with regression test |
| `documentation-creation.md` | Create docs with correct templates |
| `testing.md` | Write tests, verify coverage |
| `deployment.md` | Deploy, verify, rollback if needed |

### plans/ — Implementation Plans
| File | Description |
|------|-------------|
| `PLANS_INDEX.md` | Master index of all phases (all complete) |
| `plans/*.md` | Detailed phase implementation plans |

---

## How AI Context Works

### Loading Context
1. **Start with memory/**: Load `01-project-context.md` for project overview
2. **Load relevant rules**: Pick rules for your task domain
3. **Use a prompt or task**: Follow the structured workflow
4. **Reference agents**: For role-specific behavior constraints

### Context Priority
For any AI task, load in this order:
1. `memory/01-project-context.md` (always — project overview)
2. Relevant `rules/*.md` (standards to follow)
3. Relevant `prompts/*.md` or `tasks/*.md` (how to execute)
4. `memory/03-decisions.md` (if architecture-related)

### Maintaining This Context
- Update `memory/` when project evolves (new services, decisions, lessons)
- Update `rules/` when standards change
- Add new `prompts/` for recurring AI tasks
- Keep `plans/` updated with project progress

---

## Related Documentation

| Area | Location |
|------|----------|
| Full project docs | `docs/README.md` |
| Architecture | `docs/arquitectura/` |
| API reference | `docs/api/` |
| ADRs (full) | `docs/adr/` |
| Development guides | `docs/development/` |
| Operations | `docs/operations/` |
| Security | `docs/security/` |
| Troubleshooting | `docs/troubleshooting/` |
