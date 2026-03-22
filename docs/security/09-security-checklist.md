# Security Checklist

## Overview

The Security Checklist provides comprehensive pre-deployment, production deployment, and operational verification procedures for the Gym Platform. This guide ensures all security controls are in place, properly configured, and functioning correctly before each release. Use these checklists as part of the standard release process to verify security posture and meet compliance requirements.

## Table of Contents

- [Development Security Checklist](#development-security-checklist)
- [Pre-Deployment Security Checklist](#pre-deployment-security-checklist)
- [Production Deployment Checklist](#production-deployment-checklist)
- [Post-Deployment Verification](#post-deployment-verification)
- [Operational Security Checklist](#operational-security-checklist)
- [Compliance Verification](#compliance-verification)
- [Regular Audit Checklist](#regular-audit-checklist)
- [Automated Checks](#automated-checks)

---

## Development Security Checklist

### Code Development Phase

**Authentication & Authorization:**

- [ ] All user-facing endpoints require authentication
- [ ] All protected endpoints validate authorization (roles/permissions)
- [ ] JWT token validation is implemented and tested
- [ ] Session timeouts are configured (max 30 minutes for sensitive operations)
- [ ] Logout functionality clears all user state
- [ ] Password reset tokens expire after 15 minutes
- [ ] MFA (if applicable) is implemented and tested
- [ ] Service-to-service authentication uses API keys or mutual TLS

**Input Validation:**

- [ ] All user inputs are validated on the server side
- [ ] Request size limits are enforced (prevent DoS)
- [ ] File uploads are restricted by size and type
- [ ] SQL injection prevention (parameterized queries) is implemented
- [ ] XSS prevention (output encoding) is implemented
- [ ] CSRF tokens are implemented for state-changing operations
- [ ] Rate limiting is implemented on sensitive endpoints
- [ ] API endpoints validate request schema/format

**Data Protection:**

- [ ] Sensitive data is never logged (passwords, tokens, PII)
- [ ] Personally identifiable information (PII) is identified and protected
- [ ] Database fields containing sensitive data use encryption
- [ ] Encryption keys are not hardcoded in source code
- [ ] Secrets are managed via external configuration (environment variables, secret stores)
- [ ] Data retention policies are implemented
- [ ] Secure deletion is implemented for user data

**Error Handling:**

- [ ] Errors do not leak sensitive information (stack traces, paths)
- [ ] Generic error messages are returned to users
- [ ] Detailed errors are logged server-side
- [ ] Exception handling prevents application crashes
- [ ] Proper HTTP status codes are used (401 for auth failure, 403 for authorization failure)

**Dependencies:**

- [ ] No hardcoded dependencies with known vulnerabilities
- [ ] All dependencies are tracked in a dependency manifest (pom.xml, build.gradle)
- [ ] Dependency vulnerabilities are checked regularly
- [ ] Vulnerable versions are not used
- [ ] Third-party libraries are from reputable sources

**API Design:**

- [ ] CORS is properly configured (not * for production)
- [ ] API endpoints return appropriate HTTP status codes
- [ ] API documentation is complete and security requirements are noted
- [ ] Rate limiting headers are included in responses
- [ ] Pagination is implemented to prevent excessive data retrieval

**Code Quality & Testing:**

- [ ] Code review process is followed (all code reviewed by 2+ people)
- [ ] Security-specific code review criteria are applied
- [ ] Static security analysis (SonarQube/Semgrep) passes
- [ ] Unit tests cover happy path and error cases
- [ ] Security tests cover authentication, authorization, input validation
- [ ] No secrets are committed to version control
- [ ] Git history is clean (no sensitive data in commit history)

### Testing Phase

**Security Testing:**

- [ ] Authentication bypass attempts are tested
- [ ] Authorization bypass attempts are tested
- [ ] Input validation edge cases are tested (empty, null, long strings, special characters)
- [ ] SQL injection payloads are tested
- [ ] XSS payloads are tested
- [ ] CSRF attacks are simulated
- [ ] Rate limiting is tested (verify limits are enforced)
- [ ] Session management is tested (token refresh, invalidation)

**Integration Testing:**

- [ ] `X-User-Id` and `X-User-Roles` headers are injected correctly by the gateway
- [ ] Services reject requests missing required headers
- [ ] Database queries don't expose sensitive data
- [ ] Logging doesn't contain sensitive information

---

## Pre-Deployment Security Checklist

### Infrastructure Preparation

**Docker Compose Configuration:**

- [ ] Services run as non-root users in containers
- [ ] PostgreSQL port is not exposed externally in production (`docker-compose.prod.yml`)
- [ ] Environment variables use `.env` file (not hardcoded in compose file)
- [ ] `restart: always` is set for production services
- [ ] Health checks are configured for all services
- [ ] `SPRING_PROFILES_ACTIVE: production` is set

**Network Configuration:**

- [ ] Ingress TLS certificate is valid and properly installed
- [ ] HSTS header is configured (Strict-Transport-Security)
- [ ] Network policies restrict traffic between namespaces
- [ ] Egress rules restrict outbound connections
- [ ] DDoS protection is configured (if applicable)
- [ ] Web application firewall (WAF) rules are in place

**Database Configuration:**

- [ ] Database credentials are stored in secret manager (Vault, AWS Secrets Manager)
- [ ] Credentials are NOT in configuration files, environment variables, or code
- [ ] Database user has minimal required permissions (principle of least privilege)
- [ ] Database connections use encryption (SSL/TLS)
- [ ] Database backups are encrypted and tested
- [ ] Point-in-time recovery (PITR) is configured
- [ ] Database access is restricted by IP/network
- [ ] Audit logging is enabled on database

**Secrets Management:**

- [ ] All secrets are stored in external secret manager
- [ ] Secrets are not in version control (git history is clean)
- [ ] Secrets rotation policy is defined
- [ ] Access to secrets is logged and audited
- [ ] Secrets are encrypted at rest and in transit
- [ ] Secrets access is restricted by role

### Application Hardening

**Configuration:**

- [ ] Debug mode is DISABLED in production
- [ ] Verbose logging is DISABLED in production
- [ ] Default credentials are REMOVED
- [ ] Unnecessary services/ports are DISABLED
- [ ] Security headers are configured (see Security Headers section)
- [ ] Swagger/API documentation is DISABLED in production (or protected)

**Dependencies & Vulnerabilities:**

- [ ] Dependency check passes with no critical/high severity vulnerabilities
- [ ] Container image scan passes (Trivy/Aqua)
- [ ] SAST scan passes (SonarQube/Semgrep)
- [ ] No known CVEs in dependencies
- [ ] Patch level is current for all major dependencies
- [ ] License compliance is verified (no GPL/restricted licenses if not compatible)

**Certificate & TLS:**

- [ ] TLS certificate is valid (not expired, not self-signed)
- [ ] TLS version is 1.2 or higher
- [ ] Weak cipher suites are disabled
- [ ] Certificate is from trusted CA
- [ ] DNS names match certificate (no hostname mismatch warnings)
- [ ] CAA records are configured (if using DNS-based DNS01 challenge)

### Security Scanning

**Code Scanning:**

- [ ] SonarQube scan passes security quality gate
- [ ] Semgrep scan passes with no critical issues
- [ ] OWASP Dependency-Check passes
- [ ] No hardcoded secrets (git-secrets, TruffleHog)
- [ ] No SQL injection vulnerabilities
- [ ] No XSS vulnerabilities
- [ ] No CSRF vulnerabilities

**Container Scanning:**

- [ ] Trivy scan: no CRITICAL vulnerabilities
- [ ] Base image is from trusted source (Docker Hub official images)
- [ ] Minimal base image is used (Alpine, distroless if possible)
- [ ] No unnecessary packages in container
- [ ] Container runs as non-root user

**Compliance Scanning:**

- [ ] Compliance with security policy is verified
- [ ] Encryption at rest is configured and working
- [ ] Encryption in transit (TLS) is configured
- [ ] Access control is properly implemented
- [ ] Audit logging is enabled
- [ ] Retention policies are configured

### Documentation & Runbooks

**Documentation:**

- [ ] Security architecture documentation is complete
- [ ] Threat model is documented
- [ ] Data flow diagram shows security controls
- [ ] API security requirements are documented
- [ ] Authentication/authorization scheme is documented
- [ ] Incident response procedures are documented

**Runbooks:**

- [ ] Runbooks exist for common security issues
- [ ] Incident response playbook is documented
- [ ] Escalation procedures are documented
- [ ] Emergency access procedures are documented
- [ ] Rollback procedures are tested and documented

---

## Production Deployment Checklist

### Pre-Deployment Activities

**Final Reviews:**

- [ ] Security team has approved release
- [ ] All security checklist items above are completed
- [ ] Change management approval is obtained
- [ ] Deployment plan is documented and reviewed
- [ ] Rollback plan is tested and ready
- [ ] On-call team is notified and ready

**Communication:**

- [ ] Stakeholders are notified of deployment window
- [ ] Customer communication is prepared (if applicable)
- [ ] Support team is briefed on changes
- [ ] Monitoring team is briefed on expected behavior

**Backup & Recovery:**

- [ ] Database backup is verified and tested
- [ ] Application state backup is created
- [ ] Recovery time objective (RTO) is validated
- [ ] Recovery point objective (RPO) is validated
- [ ] Backup restoration procedure is tested

### Deployment Execution

**Deployment Process:**

- [ ] Traffic is routed to deployment environment
- [ ] Deployment commands are reviewed (not skipped)
- [ ] Blue-green or canary deployment strategy is used
- [ ] Health checks pass before traffic switch
- [ ] Rollback triggers are monitored
- [ ] No more than 10% traffic on new version initially

**Monitoring During Deployment:**

- [ ] Real-time monitoring dashboard is observed
- [ ] Application logs are monitored for errors
- [ ] Error rates are tracked
- [ ] Response times are monitored
- [ ] Database performance is monitored
- [ ] Security-related errors are monitored (401, 403, 500)

**Post-Deployment Immediate (0-15 minutes):**

- [ ] Application startup time is within SLA
- [ ] Health checks pass for all instances
- [ ] No critical errors in logs
- [ ] Error rate is not elevated
- [ ] Response time is within SLA
- [ ] Database connectivity is established

**Post-Deployment Early (15 minutes - 1 hour):**

- [ ] Application processes requests correctly
- [ ] Authentication/authorization working correctly
- [ ] Data encryption/decryption working
- [ ] Rate limiting is functioning
- [ ] Audit logging is working
- [ ] No security-related errors

---

## Post-Deployment Verification

### Smoke Tests

**Service Health:**

- [ ] All services respond to health check endpoints (`/<context-path>/actuator/health`)
- [ ] API Gateway (8080), Auth (8081), Training (8082), Tracking (8083), Notification (8084) are reachable
- [ ] Database connectivity verified

**Functional Tests:**

- [ ] User authentication works end-to-end
- [ ] User authorization works correctly
- [ ] Core business logic functions correctly
- [ ] API endpoints return expected responses
- [ ] Data is persisted correctly to database

**Security Tests:**

- [ ] Unauthenticated requests to protected endpoints are rejected (401)
- [ ] Requests with insufficient role are rejected (403)
- [ ] Invalid requests are rejected (400)
- [ ] CORS headers are correct

### Detailed Verification (1-4 hours post-deployment)

**Performance Monitoring:**

- [ ] Response times are consistent with baseline
- [ ] Resource utilization is within expected ranges
- [ ] No memory leaks detected
- [ ] Database query performance is acceptable
- [ ] External API calls are timing out correctly (no hangs)

**Security Monitoring:**

- [ ] No suspicious login attempts
- [ ] No unusual API access patterns
- [ ] No data access anomalies
- [ ] Firewall rules are working correctly
- [ ] Intrusion detection system (if present) has no alerts

**Compliance Monitoring:**

- [ ] Audit logging is functioning
- [ ] Encryption is working (TLS connections)
- [ ] Secrets are being accessed correctly
- [ ] Rate limiting is enforced
- [ ] Data retention policies are enforced

### Extended Verification (4-24 hours post-deployment)

**Stability Monitoring:**

- [ ] Error rate is stable and acceptable
- [ ] Response times are stable
- [ ] No gradual performance degradation
- [ ] No memory growth over time
- [ ] No connection pool exhaustion

**Compliance Audit:**

- [ ] Audit logs are being generated correctly
- [ ] Compliance violations are investigated (if any)
- [ ] Data access is logged properly
- [ ] Administrative actions are logged

---

## Operational Security Checklist

### Daily Operations

**Monitoring:**

- [ ] Security alerts are checked
- [ ] Intrusion detection system is monitored
- [ ] Failed login attempts are reviewed (>10 in 5 minutes?)
- [ ] API abuse patterns are reviewed
- [ ] Database access anomalies are checked
- [ ] Error rates are checked

**Log Review:**

- [ ] Application error logs are reviewed
- [ ] Authentication/authorization logs are reviewed
- [ ] Database logs are reviewed
- [ ] Infrastructure logs are reviewed
- [ ] Suspicious patterns are investigated

**Incident Response:**

- [ ] Critical alerts trigger incident response
- [ ] Incident response team is available
- [ ] On-call rotation is current
- [ ] Escalation procedures are tested

### Weekly Operations

**Vulnerability Management:**

- [ ] New CVEs are checked (NVD, vendor advisories)
- [ ] Dependency updates are evaluated
- [ ] Vulnerability dashboard is reviewed
- [ ] Patches are prioritized and scheduled
- [ ] Patch testing is planned

**Compliance Review:**

- [ ] Audit logs are reviewed for completeness
- [ ] Data retention policies are verified
- [ ] Access controls are verified
- [ ] Encryption status is verified

**Access Control Review:**

- [ ] User account list is reviewed for anomalies
- [ ] Service account permissions are reviewed
- [ ] Inactive accounts are identified
- [ ] Recently elevated privileges are reviewed
- [ ] Unnecessary permissions are revoked

### Monthly Operations

**Security Audit:**

- [ ] Complete security posture assessment
- [ ] Vulnerability scan is run
- [ ] Code quality metrics are reviewed
- [ ] Access control matrix is updated
- [ ] Compliance requirements are reviewed

**Penetration Testing & Red Teaming:**

- [ ] Red team exercises are scheduled
- [ ] Vulnerability assessment is conducted
- [ ] Threat hunting is performed
- [ ] Attack simulation is executed
- [ ] Findings are documented and remediated

**Backup & Recovery Testing:**

- [ ] Database backups are tested
- [ ] Restore procedures are tested
- [ ] Recovery time objective (RTO) is validated
- [ ] Recovery point objective (RPO) is validated
- [ ] Backup retention policy is verified

**Compliance Reporting:**

- [ ] Security incident report is generated
- [ ] Vulnerability report is generated
- [ ] Compliance report is generated
- [ ] Audit findings are documented
- [ ] Metrics are tracked and reported

### Quarterly Operations

**Comprehensive Security Assessment:**

- [ ] Full security architecture review
- [ ] Threat model update
- [ ] Penetration testing (comprehensive)
- [ ] Red team exercise
- [ ] Compliance audit
- [ ] Vulnerability assessment

**Policy & Procedure Updates:**

- [ ] Security policy is reviewed and updated
- [ ] Incident response procedure is reviewed
- [ ] Data retention policy is reviewed
- [ ] Access control policy is reviewed
- [ ] Patch management policy is reviewed
- [ ] Disaster recovery procedure is tested

**Training & Awareness:**

- [ ] Security training is conducted
- [ ] New team members receive onboarding
- [ ] Incident response drill is performed
- [ ] Security awareness campaign is launched
- [ ] Lessons learned from incidents are shared

---

## Compliance Verification

### GDPR Compliance

- [ ] Privacy notice is up to date and accessible
- [ ] Consent management is working correctly
- [ ] Data subject rights requests can be processed
- [ ] Data retention policies are enforced
- [ ] Data deletion is working correctly
- [ ] Data portability is implemented
- [ ] Breach notification procedures are in place
- [ ] Data Processing Agreements (DPA) are current
- [ ] Data protection impact assessments (DPIA) are completed
- [ ] Data processor oversight is maintained

### PCI DSS Compliance (if handling payments)

- [ ] Firewall configuration is verified
- [ ] Default credentials are changed
- [ ] Encryption is configured
- [ ] Access controls are enforced
- [ ] Vulnerability scanning is performed
- [ ] Intrusion detection system is active
- [ ] Logging is configured
- [ ] Data retention policy is enforced

### SOC 2 Compliance (if required)

- [ ] Access controls are in place and tested
- [ ] Encryption is configured and verified
- [ ] Audit logging is comprehensive
- [ ] Incident response procedures are documented
- [ ] Security training is completed
- [ ] Risk assessment is updated
- [ ] Change management process is followed
- [ ] Disaster recovery plan is tested

---

## Regular Audit Checklist

### Quarterly Audit

**Technical Audit:**

- [ ] Security architecture review
- [ ] Code review for security issues
- [ ] Configuration review for hardening
- [ ] Dependency vulnerability audit
- [ ] Encryption key rotation
- [ ] Certificate expiration review

**Access Control Audit:**

- [ ] User account review
- [ ] Service account review
- [ ] Role and permission review
- [ ] Administrative privilege review
- [ ] Access logs review
- [ ] Privileged access management (PAM) review

**Incident Audit:**

- [ ] Incident log review
- [ ] Incident response effectiveness
- [ ] False positive analysis
- [ ] Missed incident detection
- [ ] Incident metrics analysis

### Annual Audit

**Comprehensive Assessment:**

- [ ] Full security posture assessment
- [ ] Threat model comprehensive review
- [ ] Penetration testing (external)
- [ ] Red team exercise (comprehensive)
- [ ] Third-party security audit
- [ ] Compliance audit (external)

**Policy & Governance:**

- [ ] Security policy comprehensive review
- [ ] Compliance requirements update
- [ ] Risk management policy review
- [ ] Incident response plan update
- [ ] Business continuity plan update
- [ ] Disaster recovery plan test

**Metrics & Reporting:**

- [ ] Annual security metrics report
- [ ] Vulnerability trends analysis
- [ ] Incident trends analysis
- [ ] Compliance status report
- [ ] Executive summary to leadership
- [ ] Board-level security briefing

---

## Automated Checks

### CI/CD Pipeline Security Checks

```groovy
pipeline {
    agent any
    
    stages {
        stage('Security Checks') {
            parallel {
                stage('SAST') {
                    steps {
                        sh 'sonar-scanner ...'
                    }
                }
                stage('Dependency Check') {
                    steps {
                        sh 'gradlew dependencyCheckAnalyze'
                    }
                }
                stage('Container Scan') {
                    steps {
                        sh 'trivy image ${IMAGE_NAME}'
                    }
                }
                stage('Secret Scan') {
                    steps {
                        sh 'git-secrets --scan'
                        sh 'truffleHog filesystem .'
                    }
                }
                stage('DAST') {
                    steps {
                        sh 'owasp-zap baseline -t http://app:8080'
                    }
                }
            }
        }
    }
    
    post {
        always {
            publishHTML([
                reportDir: 'security-reports',
                reportFiles: 'index.html',
                reportName: 'Security Report'
            ])
        }
        failure {
            error 'Security checks failed - build aborted'
        }
    }
}
```

### Automated Compliance Checks

```bash
#!/bin/bash

echo "=== Security Checklist Automation ==="

# 1. Check health endpoints (actual context-paths)
echo "Checking service health..."
curl -f http://localhost:8080/actuator/health
curl -f http://localhost:8081/auth/actuator/health
curl -f http://localhost:8082/training/actuator/health
curl -f http://localhost:8083/tracking/actuator/health
curl -f http://localhost:8084/notifications/actuator/health

# 2. Check authentication enforcement
echo "Checking authentication..."
curl -v http://localhost:8080/training/routines 2>&1 | grep "401"

# 3. Check CORS configuration
echo "Checking CORS configuration..."
curl -H "Origin: https://evil.com" http://localhost:8080/auth/login

# 4. Check JWT_SECRET is set
echo "Checking secrets..."
[ -z "$JWT_SECRET" ] && echo "WARNING: JWT_SECRET not set" || echo "JWT_SECRET is set"

echo "=== Checklist Complete ==="
```

---

## Related Documentation

- [Security Overview](01-security-overview.md) - Security architecture
- [API Security](05-api-security.md) - API security patterns
- [Compliance](06-compliance.md) - GDPR and compliance requirements
- [Incident Response](07-incident-response.md) - Incident procedures
- [Deployment Guide](../deployment/01-production-deployment-guide.md) - Deployment procedures

## References

- [OWASP Release Quality Checklist](https://cheatsheetseries.owasp.org/cheatsheets/Release_Quality_Checklist.html)
- [OWASP Pre-Release Security Checklist](https://cheatsheetseries.owasp.org/cheatsheets/Production_Ready_Web_Application_Checklist.html)
- [CWE/SANS Top 25](https://cwe.mitre.org/top25/)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Security by Design](https://www.owasp.org/index.php/Security_by_Design)
