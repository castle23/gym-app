# Templates

## Overview

Collection of reusable templates for common documentation and operational tasks in the Gym Platform. These templates ensure consistency across the codebase, documentation, and operational procedures. Use these as starting points for creating new documents, tickets, and procedures.

## Table of Contents

- [Documentation Templates](#documentation-templates)
- [Git Templates](#git-templates)
- [Issue & PR Templates](#issue--pr-templates)
- [Runbook Templates](#runbook-templates)
- [Change Request Templates](#change-request-templates)
- [Testing Templates](#testing-templates)
- [Configuration Templates](#configuration-templates)

---

## Documentation Templates

### API Documentation Template

```markdown
# API Endpoint Name

## Overview

Brief description of what this endpoint does.

## HTTP Method & URL

```
METHOD /api/v1/resource/{id}
```

## Authentication

- Required: Yes/No
- Method: JWT/OAuth/API Key
- Scopes: scope1, scope2

## Request Parameters

### Path Parameters

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | Resource identifier (BIGSERIAL) |

### Query Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| limit | Integer | No | 100 | Result limit |
| offset | Integer | No | 0 | Result offset |

### Request Body

```json
{
  "field1": "value1",
  "field2": "value2"
}
```

## Response

### Success Response

**Status Code:** 200 OK

```json
{
  "id": 1,
  "field1": "value1",
  "field2": "value2",
  "createdAt": "2026-03-21T10:00:00Z",
  "updatedAt": "2026-03-21T10:00:00Z"
}
```

### Error Response

**Status Code:** 400 Bad Request

```json
{
  "error": {
    "code": "INVALID_REQUEST",
    "message": "The request is invalid",
    "details": {
      "field1": "Required field missing"
    }
  }
}
```

## Examples

### cURL Example

```bash
curl -X GET http://localhost:8080/training/resource/1 \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json"
```

### Python Example

```python
import requests

response = requests.get(
    'http://localhost:8080/api/v1/resource/123',
    headers={'Authorization': f'Bearer {jwt_token}'}
)
print(response.json())
```

### JavaScript Example

```javascript
fetch('/api/v1/resource/123', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${jwtToken}`,
    'Content-Type': 'application/json'
  }
})
.then(response => response.json())
.then(data => console.log(data));
```

## Notes

- Additional important information
- Performance considerations
- Related endpoints
```

### Feature Documentation Template

```markdown
# Feature Name

## Overview

Brief description of the feature and its purpose in Gym Platform.

## Motivation

Why this feature was needed and business value it provides.

## Implementation

### Architecture

How the feature is implemented at high level.

### Components

Key classes, services, or modules involved.

### Databases

Tables, schemas, or data structures involved.

### Configuration

Configuration options and defaults.

## Usage

### For End Users

How users interact with the feature.

### For Developers

Code examples for developers using the feature.

## Testing

Test scenarios and expected behavior.

## Performance Considerations

Impact on system performance and optimization notes.

## Security Considerations

Security implications and protections.

## Troubleshooting

Common issues and resolution steps.

## Related Documentation

Links to related features and documentation.
```

### Runbook Template

```markdown
# Runbook: [Process Name]

## Overview

Brief description of the process.

## Prerequisites

- Prerequisite 1
- Prerequisite 2

## Steps

### Step 1: [Step Name]

1. Detailed action 1
2. Detailed action 2
3. Verify completion

### Step 2: [Step Name]

1. Detailed action 1
2. Detailed action 2
3. Verify completion

## Verification

How to verify the process completed successfully.

## Rollback

How to undo if something goes wrong.

## Troubleshooting

Common issues and solutions.

## Related Runbooks

Links to related procedures.
```

---

## Git Templates

### Commit Message Template

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation change
- `style`: Code style change
- `refactor`: Code refactoring
- `perf`: Performance improvement
- `test`: Test addition or update
- `chore`: Build or dependency change
- `ci`: CI/CD configuration change

**Example:**

```
feat(auth): add JWT token refresh mechanism

Implement automatic JWT token refresh for better user experience.
Tokens now refresh 5 minutes before expiration.

- Add RefreshTokenFilter to Spring Security chain
- Implement TokenRefreshService for token generation
- Add refresh endpoint to AuthController

Fixes #123
Related to #456
```

### Branch Naming Convention

```
<type>/<jira-ticket>-<short-description>

Examples:
- feature/GYM-123-add-workout-tracking
- bugfix/GYM-456-fix-auth-token-expiry
- hotfix/GYM-789-database-connection-pool
- docs/GYM-999-update-api-documentation
```

---

## Issue & PR Templates

### Bug Report Template

```markdown
## Description

Brief description of the bug.

## Steps to Reproduce

1. Step 1
2. Step 2
3. Step 3

## Expected Behavior

What should happen.

## Actual Behavior

What actually happens.

## Environment

- Service: Auth Service / Training Service / etc.
- Version: x.y.z
- Java Version: 17
- PostgreSQL Version: 14
- Kubernetes: Yes/No

## Screenshots/Logs

If applicable, add screenshots or log excerpts.

## Additional Context

Any other relevant information.

## Acceptance Criteria

- [ ] Bug is reproducible
- [ ] Root cause identified
- [ ] Fix tested in staging
- [ ] Documentation updated
```

### Feature Request Template

```markdown
## Description

Brief description of the desired feature.

## Motivation

Why this feature is needed.

## Proposed Solution

How you would like it implemented.

## Alternatives

Other solutions considered.

## Acceptance Criteria

- [ ] Feature implemented and tested
- [ ] Documentation added
- [ ] Tests pass
- [ ] Code reviewed and approved
- [ ] Deployed to staging

## Estimated Effort

- Small / Medium / Large
```

### Pull Request Template

```markdown
## Description

Brief description of the changes.

## Related Issues

Fixes #123
Related to #456

## Changes Made

- Change 1
- Change 2
- Change 3

## Testing

Description of tests performed.

### Test Scenarios

- [ ] Scenario 1
- [ ] Scenario 2

## Performance Impact

Performance considerations and measurements.

## Security Considerations

Any security implications.

## Checklist

- [ ] Code follows style guidelines
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No breaking changes
- [ ] Tested in local environment
- [ ] Tested in staging environment

## Screenshots

If applicable, add screenshots of changes.

## Deployment Notes

Any special deployment considerations.
```

---

## Runbook Templates

### Incident Response Runbook

```markdown
# Incident Runbook: [Incident Type]

## Detection

How to detect this incident:
- Alert: [Alert name]
- Symptoms: [What to look for]

## Assessment (< 5 minutes)

1. Verify the issue
   ```bash
   [Command to verify]
   ```

2. Assess severity
   - P1: Complete outage, users affected
   - P2: Degraded performance
   - P3: Minor issue, workaround available

3. Notify on-call team
   - Slack: #incidents
   - PagerDuty: Create incident

## Mitigation (< 15 minutes)

Immediate steps to mitigate:

1. Step 1
   ```bash
   [Command]
   ```

2. Step 2
   ```bash
   [Command]
   ```

## Root Cause Analysis (Post-incident)

1. Investigation steps
2. Identifying root cause
3. Review logs and metrics

## Resolution

Permanent fix:

1. Code changes
2. Configuration changes
3. Infrastructure changes

## Verification

Confirm issue is resolved:

1. Monitoring checks
2. User testing
3. Performance metrics

## Post-Incident

- [ ] Incident report created
- [ ] Root cause documented
- [ ] Follow-up items created
- [ ] Timeline documented
- [ ] Team review scheduled

## Related Incidents

Link to similar incidents and lessons learned.
```

### Deployment Runbook

```markdown
# Deployment Runbook: [Service Name] v[Version]

## Pre-Deployment

- [ ] Code reviewed and approved
- [ ] All tests passing (`mvn test`)
- [ ] Database backup created
- [ ] Rollback plan documented

## Deployment Steps

### 1. Pre-deployment Checks

```bash
# Verify current state
docker-compose ps
docker-compose logs --since=1h | grep -i error
```

### 2. Build and Deploy

```bash
git pull origin main
docker-compose up -d --build
```

### 3. Verification

```bash
# Check all containers healthy
docker-compose ps

# Check health endpoints
curl http://localhost:8081/auth/actuator/health
curl http://localhost:8082/training/actuator/health
curl http://localhost:8083/tracking/actuator/health
curl http://localhost:8084/notifications/actuator/health
```

- [ ] All containers in "Up (healthy)" state
- [ ] No errors in logs
- [ ] API endpoints responding

## Rollback

```bash
git checkout <previous-commit>
docker-compose up -d --build
```

## Post-Deployment

- [ ] Monitor logs for 15 minutes
- [ ] Verify smoke test passes
- [ ] Document any issues
```

---

## Change Request Templates

### Change Request Template

```markdown
# Change Request: [Title]

## Executive Summary

Brief summary of the change and its business impact.

## Details

### What is Changing

- Service/Component: [Name]
- Version: [From X.Y.Z to A.B.C]
- Type: Feature / Bug Fix / Maintenance

### Why is This Changing

Business justification and motivation.

### Impact Analysis

- Affected Systems: [List]
- Affected Users: [Estimate]
- Data Migration: Yes / No
- Downtime: [Expected downtime or "Zero-downtime"]

## Risk Assessment

### Technical Risks

- Risk 1 and mitigation
- Risk 2 and mitigation

### Operational Risks

- Risk 1 and mitigation
- Risk 2 and mitigation

## Testing

- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Staging deployment tested
- [ ] Performance testing done
- [ ] Security review completed

## Deployment Plan

Detailed deployment procedure.

## Rollback Plan

Steps to rollback if needed.

## Communication

- Stakeholder notification: [Date/Time]
- User notification: [Yes/No/Method]
- Documentation updates: [Yes/No]

## Sign-off

- [ ] Technical Lead Approval
- [ ] Product Manager Approval
- [ ] Operations Lead Approval

## Approvers

- Name 1 (Role)
- Name 2 (Role)
```

---

## Testing Templates

### Test Plan Template

```markdown
# Test Plan: [Feature/Service Name]

## Overview

Objective and scope of testing.

## Test Environment

- Hardware specifications
- Software versions
- Data volumes
- Network setup

## Test Cases

### Test Case 1: [Name]

- **Objective:** What is being tested
- **Preconditions:** Initial state
- **Steps:** Test steps
- **Expected Result:** What should happen
- **Actual Result:** [To be filled during testing]
- **Status:** Pass / Fail

## Test Data

- Data volumes
- Data sources
- Refresh strategy

## Success Criteria

- Criteria 1
- Criteria 2
- Criteria 3

## Risks & Mitigations

| Risk | Mitigation |
|------|-----------|
| Risk 1 | Mitigation 1 |
| Risk 2 | Mitigation 2 |

## Sign-off

- [ ] QA Lead Review
- [ ] Product Manager Sign-off
```

---

## Configuration Templates

### Service Configuration Template

```yaml
# Service Configuration for [Service Name]
# Environment: [Development/Staging/Production]

# Application Settings
app:
  name: gym-service
  version: 1.0.0
  environment: production

# Database Configuration
database:
  url: jdbc:postgresql://postgres:5432/gym_db
  username: gym_admin
  password: ${DB_PASSWORD}
  pool:
    size: 20
    max_lifetime: 1800000

# Server Configuration
server:
  port: 8081
  servlet:
    context-path: /auth   # /auth | /training | /tracking | /notifications

# Spring Boot Settings
spring:
  application:
    name: gym-service
  profiles:
    active: docker
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        default_schema: auth_schema

# Logging Configuration
logging:
  level:
    root: INFO
    com.gym: DEBUG

# Monitoring
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
```

---

**Related Resources:**
- [01-glossary.md](01-glossary.md) - Term definitions
- [02-abbreviations.md](02-abbreviations.md) - Acronym reference
- [03-links-references.md](03-links-references.md) - External resources
- [05-best-practices.md](05-best-practices.md) - Recommended practices
