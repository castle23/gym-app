# Task: Documentation Creation

## Prerequisites
- [ ] Topic clearly defined
- [ ] Format chosen: ADR, API doc, development guide, or operations runbook
- [ ] Source material available (code, existing docs, team decisions)

## Workflow

### 1. Select Template
- Refer to `ai/rules/documentation-standards.md` for approved templates
- Choose based on document type:

| Type              | Template Source                        | Output Directory       |
|-------------------|----------------------------------------|------------------------|
| ADR               | `ai/skills/documentation-writing.md`   | `docs/adr/`            |
| API Documentation | `ai/skills/documentation-writing.md`   | `docs/api/`            |
| Development Guide | `ai/rules/documentation-standards.md`  | `docs/development/`    |
| Operations Runbook| `ai/rules/documentation-standards.md`  | `docs/operations/`     |

### 2. Research Topic
- Read relevant source code files
- Check existing documentation for overlap or references
- Review related ADRs if the topic involves architecture
- Note any gaps in current documentation

### 3. Write Draft
- Follow the selected template structure exactly
- Use consistent heading hierarchy (H1 title, H2 sections, H3 subsections)
- Include code examples with language-tagged fenced blocks
- Add tables for structured data (parameters, configurations, endpoints)
- Use relative links for cross-references to other docs

### 4. Review Against Standards
- Verify against `ai/rules/documentation-standards.md`:
  - Correct Markdown formatting
  - No broken links
  - Code examples are accurate and tested
  - All required sections present per template
  - Consistent terminology with existing docs

### 5. Place in Correct Directory
```
docs/
├── adr/              Architecture Decision Records
├── api/              API endpoint documentation
├── development/      Development guides, setup, conventions
└── operations/       Runbooks, deployment, monitoring
```
- File naming: lowercase, hyphen-separated (`user-authentication-api.md`)
- ADR naming: `ADR-NNN-title.md` (sequential numbering)

### 6. Commit
```bash
git add docs/[subdirectory]/[filename].md
git commit -m "docs(scope): description of documentation added"
```

## Document Type Guidelines

### ADR
- One decision per ADR
- Include context, decision, consequences (positive and negative)
- Reference related ADRs
- Status: Proposed → Accepted → Deprecated/Superseded

### API Documentation
- One file per service or domain area
- Every endpoint: method, path, auth, request, response, errors
- Include curl examples

### Development Guide
- Step-by-step instructions a new developer can follow
- Include prerequisites, setup steps, verification
- Keep up to date with codebase changes

### Operations Runbook
- Actionable steps for operational tasks
- Include commands that can be copy-pasted
- Cover normal operation and failure scenarios
- Include rollback procedures

## Completion Checklist
- [ ] Correct template used
- [ ] Placed in correct `docs/` subdirectory
- [ ] Consistent style with existing documentation
- [ ] All code examples are accurate
- [ ] No broken internal links
- [ ] Reviewed against documentation standards
- [ ] Commit follows `docs(scope): description` format

## References
- `ai/skills/documentation-writing.md` — templates and generation process
- `ai/rules/documentation-standards.md` — formatting and style rules
- `ai/rules/project-overview.md` — project context
