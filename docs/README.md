# Gym Platform API - Documentation Hub

Welcome to the comprehensive documentation for the Gym Platform API. This documentation is organized to serve multiple roles and use cases.

## Quick Navigation

### For Developers
- **[Contributing Guide](../CONTRIBUTING.md)** - How to contribute code and follow Git Flow
- **[Code Standards Guide](development/02-code-standards-style-guide.md)** - Coding standards for Java, Bash, Python, IaC
- **[Integration Testing Guide](development/03-integration-testing-guide.md)** - Cross-service testing strategies
- **[Advanced Testing Guide](development/04-advanced-testing-guide.md)** - Load, performance, security, and chaos testing
- **[Development Guide](development/)** - Setup, debugging, best practices
- **[Architecture Decisions (ADRs)](adr/)** - Why we made key technical choices
- **[Architecture Overview](arquitectura/)** - System design and microservices
- **[API Documentation](api/)** - Endpoints, examples, integration guides
- **[Stack Documentation](stack/)** - Technology choices and configurations

### For DevOps / Infrastructure
- **[Deployment Runbook](deployment/01-deployment-runbook.md)** - Complete deployment procedures for all environments
- **[Changelog Procedures](deployment/02-changelog-procedures.md)** - Version management and release process
- **[Deployment Guide](deployment/)** - Deployment procedures and checklists
- **[Operations Runbook](operations/)** - Running and monitoring in production
- **[Scripts](../scripts/)** - Operational, development, and monitoring scripts

### For Database Administrators
- **[Data Dictionary](database/01-data-dictionary.md)** - Complete schema documentation for all 19 entities
- **[DBA Guide](../dba/)** - Database architecture, queries, procedures
- **[Database Documentation](database/)** - Schema, maintenance, troubleshooting

### For Security & Compliance
- **[Security Documentation](security/)** - Security practices, guidelines, procedures

### For Project Management
- **[Project Overview](project/)** - Completion reports, roadmap, phases
- **[Resources](resources/)** - Templates, glossary, general references

### For Troubleshooting
- **[Troubleshooting Guide](troubleshooting/)** - Debugging, common issues, diagnostics
- **[FAQ](troubleshooting/faqs/)** - Frequently asked questions

---

## Documentation Structure

```
docs/
├── adr/                    # Architecture Decision Records (12 ADRs)
├── arquitectura/           # System architecture and design
├── project/                # Project information and completion reports
├── stack/                  # Technology stack documentation
├── development/            # Developer guides and standards
├── deployment/             # DevOps and deployment procedures
├── operations/             # Operational runbooks and SOPs
├── troubleshooting/        # Debugging and issue resolution
├── api/                    # API documentation and examples
├── security/               # Security guidelines and procedures
├── database/               # Database documentation
└── resources/              # General resources and references

ai/                        # AI/Automation contexts
dba/                       # DBA-specific tools and procedures
tests/                     # Testing strategy and resources
scripts/                   # Operational scripts organized by purpose
```

---

## Key Features

### Single Source of Truth
All critical information is centralized to avoid duplication. Cross-references link related documents across sections.

### Role-Based Navigation
Find documentation tailored to your role - Developer, DevOps, DBA, Security, or Project Manager.

### AI-Friendly Context Layers
The `ai/` directory contains structured contexts for automation and AI tools, including:
- Memory: Project context and lessons learned
- Rules: Coding and documentation standards
- Prompts: Common review and analysis prompts
- Tasks: Reusable workflows and templates
- Plans: Roadmaps and sprint documentation

### Organized Scripts
The `scripts/` directory contains executable utilities organized by purpose:
- `operational/` - Deployment, health checks, monitoring
- `development/` - Build, test, and development utilities
- `database/` - Database maintenance and migration scripts
- `monitoring/` - Monitoring and alerting scripts

---

## Recent Documentation Changes

- **Phase 1 (Complete)**: Consolidated Postman collections (101 endpoints) ✅
- **Phase 1 (Complete)**: Comprehensive API Testing Guide (1,311 lines) ✅
- **Phase 2 (Complete)**: Contributing Guide (675 lines) ✅
- **Phase 2 (Complete)**: Architecture Decision Records - 12 ADRs ✅
- **Phase 2 (Complete)**: Code Standards Guide (1,500+ lines) ✅
- **Phase 3 (Complete)**: Data Dictionary (2,000+ lines) ✅
- **Phase 3 (Complete)**: Integration Testing Guide (1,200+ lines) ✅
- **Phase 3 (Complete)**: Deployment Runbook (1,400+ lines) ✅
- **Phase 4 (Complete)**: Advanced Testing Guide (850+ lines) ✅
- **Phase 4 (Complete)**: Changelog Procedures (550+ lines) ✅
- **Phase 4 (Complete)**: CHANGELOG.md template with v1.0.0 ✅

---

## Contributing

When adding new documentation:
1. Place files in the appropriate subdirectory
2. Number sequential files (01-, 02-, etc.)
3. Create/update README.md in the subdirectory
4. Add cross-references to related documentation
5. Update this main README if adding new sections

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

---

## Project Status

**Build Status**: ✅ All 7 modules compile (335MB total JAR size)
**Deployment Status**: ✅ 4 services running (ports 8081-8084)
**Production Status**: ✅ Production-ready

For detailed completion status, see [Project Completion Reports](project/completion-reports/).
