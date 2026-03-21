# Contributing to Gym Platform API Documentation

Thank you for contributing to the Gym Platform API documentation. This guide will help you maintain consistency and quality.

## Documentation Structure

The documentation is organized into several main areas:

- **docs/** - Main documentation hub (role-specific and topic-based)
- **ai/** - AI/automation contexts and prompts
- **dba/** - DBA-specific tools and procedures
- **tests/** - Testing resources and guides
- **scripts/** - Operational and development scripts

See [docs/README.md](docs/README.md) for detailed structure.

## Before You Start

1. Review the existing documentation structure
2. Check if similar documentation already exists
3. Read [docs/resources/05-best-practices.md](docs/resources/05-best-practices.md)
4. Follow the coding and documentation standards

## Guidelines

### File Naming

- Use lowercase with hyphens: `01-file-name.md`
- Sequential numbering for related files: `01-`, `02-`, `03-`
- Descriptive names that indicate content
- No spaces in filenames

### File Organization

**New documentation should go in:**
- **Architecture/Design** → `docs/arquitectura/`
- **Development Guides** → `docs/development/`
- **API Documentation** → `docs/api/`
- **Deployment Procedures** → `docs/deployment/`
- **Operations/Runbooks** → `docs/operations/`
- **Troubleshooting** → `docs/troubleshooting/`
- **Database Info** → `docs/database/` or `dba/`
- **Security** → `docs/security/`
- **Testing** → `tests/`
- **AI Context** → `ai/`

### Markdown Style

Use these standards for consistency:

```markdown
# Main Title (H1)

Introduction paragraph with context.

## Section Heading (H2)

Content for this section.

### Subsection (H3)

More specific content.

- Bullet point
- Another point

1. Numbered item
2. Another item

**Bold** for emphasis
_Italic_ for additional emphasis
`code` for inline code
```

### Code Examples

Include complete, working examples:

```bash
# Good: Clear, complete example
./scripts/operational/health-check.sh

# Good: With explanation
# Check service health
curl http://localhost:8081/swagger-ui.html
```

### Links and References

Cross-reference related documents:

```markdown
See [Development Guide](../development/) for setup instructions.
See [API Documentation](../api/04-endpoints-reference.md) for endpoints.
```

Use relative paths for internal links.

### Metadata

Add metadata at the top of longer documents:

```markdown
# Document Title

**Last Updated**: 2024-03-21
**Author**: Your Name
**Status**: Active / Draft / Deprecated
**Tags**: api, deployment, security

---

Document content here...
```

### Table of Contents

For documents over 500 lines, add a TOC:

```markdown
# Document Title

## Table of Contents

1. [Section One](#section-one)
2. [Section Two](#section-two)

---

## Section One

Content...

## Section Two

Content...
```

## Adding New Documentation

### Step 1: Choose Location
Determine which directory best fits your documentation using the structure above.

### Step 2: Check Existing Docs
Ensure you're not duplicating existing documentation.

### Step 3: Create the File
- Use appropriate naming convention
- Add content following markdown style guidelines
- Include relevant links and references

### Step 4: Update README
Update the section's README.md with a reference to your new documentation.

### Step 5: Add Cross-References
Link to your new document from related documents.

### Step 6: Git Workflow
```bash
git add docs/section/your-document.md
git commit -m "docs: add documentation for [topic]"
git push
```

## Updating Existing Documentation

### For Minor Changes
- Fix typos, clarify wording
- Update code examples
- Fix broken links

### For Significant Updates
1. Review existing content carefully
2. Ensure changes don't break references
3. Update "Last Updated" date
4. Add context about what changed
5. Consider if cross-references need updating

### Deprecating Documents
If a document is no longer relevant:
1. Update the STATUS to "Deprecated"
2. Add a note about what replaces it
3. Keep the document for historical reference
4. Update cross-references

## Quality Checklist

Before submitting your changes:

- [ ] File is in correct directory
- [ ] Filename follows naming convention
- [ ] Content is accurate and complete
- [ ] Markdown formatting is clean
- [ ] Code examples work correctly
- [ ] Links are functional and use relative paths
- [ ] Related documents are cross-referenced
- [ ] README.md is updated if needed
- [ ] No spelling or grammar errors
- [ ] Document follows style guidelines
- [ ] Metadata is current (if included)

## Documentation Standards

### Clarity
- Write for your audience (developers, DBAs, ops)
- Use clear, concise language
- Explain technical terms
- Include examples

### Completeness
- Cover the full topic
- Include prerequisites
- Provide step-by-step instructions for procedures
- Include troubleshooting tips

### Currency
- Keep examples up-to-date
- Update version numbers
- Note deprecated features
- Add recent lessons learned

### Consistency
- Match the style of related documents
- Use consistent terminology
- Follow naming conventions
- Maintain organizational structure

## Role-Specific Documentation

### For Developers
Include:
- Setup instructions
- Code examples
- Best practices
- Testing guidance
- Links to relevant APIs

### For DevOps/Operations
Include:
- Step-by-step procedures
- Prerequisites and requirements
- Verification steps
- Troubleshooting
- Monitoring guidance

### For DBAs
Include:
- Database specifics
- Query examples
- Backup procedures
- Performance tuning
- Maintenance tasks

### For Project Managers
Include:
- Project status
- Completion metrics
- Timeline information
- Risk/issue tracking
- Resource allocation

## Collaboration

### Asking for Review
Tag documentation experts:
- @architects - for architecture/design docs
- @devops - for deployment/operations docs
- @dba-team - for database documentation
- @leads - for general review

### Providing Feedback
When reviewing documentation:
1. Check for accuracy
2. Verify completeness
3. Ensure clarity for the target audience
4. Look for broken links
5. Verify code examples work

### Resolving Conflicts
If multiple docs cover similar topics:
1. Consolidate into one authoritative document
2. Add "See also" references in related docs
3. Remove duplicate information
4. Update all cross-references

## Getting Help

Questions about contributing?
1. Check [docs/resources/](docs/resources/) for guidelines
2. Review similar documentation for examples
3. Ask in team communications
4. Consult with documentation maintainers

## Documentation Philosophy

Our documentation aims to:
- **Enable** - Help users accomplish their goals
- **Clarify** - Explain complex concepts simply
- **Guide** - Provide step-by-step procedures
- **Reassure** - Show best practices and examples
- **Scale** - Support users at all experience levels

## Contact & Questions

For documentation questions:
- Check [docs/resources/](docs/resources/) first
- Review existing documentation for patterns
- Ask team leads or documentation maintainers
- Submit suggestions through issue tracking

---

**Thank you for improving our documentation!**
