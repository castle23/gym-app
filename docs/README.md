# Gym Platform API - Documentation Hub

Welcome to the comprehensive documentation for the Gym Platform API. This documentation is organized to serve multiple roles and use cases.

## Quick Navigation

### For Developers
- **[Development Guide](development/)** - Setup, coding standards, debugging
- **[Architecture Overview](arquitectura/)** - System design and microservices
- **[API Documentation](api/)** - Endpoints, examples, integration guides
- **[Stack Documentation](stack/)** - Technology choices and configurations

### For DevOps / Infrastructure
- **[Deployment Guide](deployment/)** - Deployment procedures and checklists
- **[Operations Runbook](operations/)** - Running and monitoring in production
- **[Scripts](../scripts/)** - Operational, development, and monitoring scripts

### For Database Administrators
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
├── arquitectura/          # System architecture and design
├── project/              # Project information and completion reports
├── stack/                # Technology stack documentation
├── development/          # Developer guides and standards
├── deployment/           # DevOps and deployment procedures
├── operations/           # Operational runbooks and SOPs
├── troubleshooting/      # Debugging and issue resolution
├── api/                  # API documentation and examples
├── security/             # Security guidelines and procedures
├── database/             # Database documentation
└── resources/            # General resources and references

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

- **Phase 1**: 80 API endpoints with @Schema annotations ✅
- **Phase 2**: Testing infrastructure deployed ✅
- **Phase 3**: Production documentation complete (31,000+ words) ✅
- **Current**: Documentation reorganization for scalability ✅

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
