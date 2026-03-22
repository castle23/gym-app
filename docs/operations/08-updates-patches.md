# Updates & Patches

## Overview

Update procedures, patch management, dependency updates, security patches, and rollback plans for Gym Platform microservices.

## Update Strategy

### Update Types

| Type | Frequency | Risk | Testing | Rollback |
|------|-----------|------|---------|----------|
| **Security Patches** | ASAP | Low-Medium | Quick | Fast (< 5min) |
| **Bug Fixes** | Weekly | Low | Standard | Standard |
| **Feature Updates** | Bi-weekly | Medium | Extended | Planned |
| **Major Versions** | Quarterly | High | Extensive | Pre-planned |
| **Dependency Updates** | Monthly | Low-Medium | Full suite | Automated |

## Patch Management Process

### 1. Identify Patches

```bash
#!/bin/bash
# scripts/updates/check-available-patches.sh

echo "=== Checking Available Patches ==="

# Maven dependencies
echo "Java dependency updates:"
./mvnw versions:display-dependency-updates -DprocessDependencyManagement=false

# Docker base images
echo "Docker base image updates:"
docker pull alpine:latest && docker image inspect alpine:latest | grep -i version

# System packages
echo "System package updates:"
apt list --upgradable 2>/dev/null | head -10

# Security vulnerabilities
echo "Security vulnerabilities:"
docker scan gym-auth-service:latest 2>/dev/null | grep -i "vulnerability\|critical"

# Maven dependency check
echo "Checking Maven dependencies for vulnerabilities:"
mvn dependency-check:check -q 2>/dev/null || echo "Scan completed"
```

### 2. Test Patches

```bash
#!/bin/bash
# scripts/updates/test-patches.sh

echo "=== Testing Patches in Development ==="

# Create feature branch
git checkout -b feature/patch-testing-$(date +%Y%m%d)

# Apply patches
./mvnw clean install

# Run unit tests
./mvnw test

# Run integration tests
./mvnw verify

# Run security tests
./mvnw dependency-check:check

# Build Docker images
docker-compose build --no-cache

# Start test environment
docker-compose -f docker-compose.test.yml up -d

# Run E2E tests
./scripts/tests/smoke-tests.sh http://localhost:8081

# Stress test
./scripts/tests/load-test.sh

# Verify no regressions
echo "All tests passed, ready for production"
```

### 3. Deploy Patches

```bash
#!/bin/bash
# scripts/updates/deploy-patches.sh

PATCH_VERSION=$1
ENVIRONMENT=${2:-prod}

if [ -z "$PATCH_VERSION" ]; then
    echo "Usage: $0 <patch-version> [environment]"
    exit 1
fi

echo "Deploying patch $PATCH_VERSION to $ENVIRONMENT"

# Create release tag
git tag -a "v$PATCH_VERSION" -m "Patch release $PATCH_VERSION"
git push origin "v$PATCH_VERSION"

# Build and push Docker images
docker build -f auth-service/Dockerfile -t gym-auth-service:$PATCH_VERSION .
docker tag gym-auth-service:$PATCH_VERSION gym-auth-service:latest

docker push gym-auth-service:$PATCH_VERSION
docker push gym-auth-service:latest

# Deploy using blue-green strategy
./scripts/operational/blue-green-deploy.sh $PATCH_VERSION

# Verify deployment
for service in auth training tracking notification; do
    if ! curl -f http://localhost:8081/actuator/health > /dev/null; then
        echo "Service verification failed!"
        ./scripts/operational/blue-green-rollback.sh
        exit 1
    fi
done

echo "Patch deployed successfully"
```

### 4. Rollback if Needed

```bash
#!/bin/bash
# scripts/updates/rollback-patch.sh

ROLLBACK_VERSION=$1

if [ -z "$ROLLBACK_VERSION" ]; then
    echo "No rollback version specified"
    echo "Current version: $(cat version.txt)"
    exit 1
fi

echo "Rolling back to version $ROLLBACK_VERSION"

# Switch back to previous version
git checkout $ROLLBACK_VERSION
git pull origin

# Rebuild and restart services
docker-compose build --no-cache
docker-compose down
docker-compose up -d

# Verify services
sleep 30
curl -f http://localhost:8081/actuator/health || exit 1

echo "Rollback completed successfully"
```

## Security Patch Process

### Critical Security Vulnerabilities

**Response Time:**
- P0 (Remote code execution): 4 hours
- P1 (Data exposure): 24 hours  
- P2 (High severity): 72 hours

**Procedure:**
```bash
# 1. Assess impact
# 2. Create hotfix branch
git checkout -b hotfix/security-CVE-2024-12345

# 3. Apply fixes
# 4. Test thoroughly (security + functionality)
./mvnw clean test security:check

# 5. Build images
docker build -f auth-service/Dockerfile -t gym-auth-service:hotfix .

# 6. Deploy immediately
docker-compose down
docker-compose up -d

# 7. Verify
curl http://localhost:8081/actuator/health

# 8. Communicate
# Post to status page, email, Slack
```

## Dependency Update Strategy

### Java Dependencies

```xml
<!-- In pom.xml: Define versions -->
<properties>
    <spring-boot.version>3.1.5</spring-boot.version>
    <spring-security.version>6.1.4</spring-security.version>
    <postgresql.version>42.7.1</postgresql.version>
    <jjwt.version>0.12.3</jjwt.version>
    <logstash-logback.version>7.4</logstash-logback.version>
</properties>

<!-- Regular updates script -->
# scripts/updates/update-java-deps.sh
#!/bin/bash
cd auth-service
./mvnw versions:display-dependency-updates -DprocessDependencyManagement=false
./mvnw versions:use-latest-releases -DallowMajorUpdates=false
./mvnw clean test
```

### Docker Base Images

```dockerfile
# Regular updates: scan and update
FROM eclipse-temurin:17-jre-alpine  # Latest LTS version
```

```bash
# scripts/updates/scan-docker-images.sh
#!/bin/bash

# Scan for vulnerabilities
docker scan gym-auth-service:latest
docker scan gym-training-service:latest
docker scan gym-tracking-service:latest
docker scan gym-notification-service:latest

# Automatically update minor versions
for service in auth training tracking notification; do
    docker pull eclipse-temurin:17-jre-alpine
    docker build -f gym-${service}/Dockerfile -t gym-${service}-service:latest .
done
```

### Maven Dependencies

```bash
# Check for updates
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates

# Update to latest versions
mvn versions:update-properties

# Check for security vulnerabilities
mvn dependency-check:check

# Test
mvn clean test

# Commit and deploy
git add pom.xml
git commit -m "chore: update maven dependencies"
```

## Version Management

### Semantic Versioning

```
MAJOR.MINOR.PATCH

Examples:
- 1.0.0 - Initial release
- 1.1.0 - New features (backwards compatible)
- 1.1.1 - Bug fix
- 2.0.0 - Breaking changes

Usage in Docker tags:
- gym-auth-service:1.1.0      (Specific version)
- gym-auth-service:1.1        (Latest patch of 1.1)
- gym-auth-service:1           (Latest minor of 1)
- gym-auth-service:latest     (Latest version)
```

### Version Control

```bash
# Tag releases
git tag -a v1.1.0 -m "Release 1.1.0"
git push origin v1.1.0

# View all versions
git tag --list

# Get current version
git describe --tags

# Compare versions
git log v1.0.0..v1.1.0 --oneline
```

## Automated Update Pipeline

### GitHub Actions for Updates

```yaml
name: Automated Updates

on:
  schedule:
    # Run daily at 1 AM UTC
    - cron: '0 1 * * *'

jobs:
  update-dependencies:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Check Maven updates
        run: ./mvnw versions:display-dependency-updates

      - name: Check Docker updates
        uses: dependabot/fetch-metadata@v1.3.6

      - name: Create Pull Request
        if: steps.dependabot.outputs.alert-count > 0
        uses: peter-evans/create-pull-request@v4
        with:
          commit-message: 'chore: update dependencies'
          title: 'chore: automated dependency updates'
          body: 'Automated dependency update'
          branch: chore/auto-updates

  test-updates:
    runs-on: ubuntu-latest
    needs: update-dependencies
    steps:
      - uses: actions/checkout@v3

      - name: Run tests
        run: ./mvnw clean test

      - name: Build Docker images
        run: docker-compose build

      - name: Run integration tests
        run: ./scripts/tests/integration-test.sh
```

## Patch Verification

### Post-Patch Verification

```bash
#!/bin/bash
# scripts/updates/verify-patch.sh

echo "=== Post-Patch Verification ==="

# 1. Health check
echo "Health check:"
for port in 8081 8082 8083 8084; do
    STATUS=$(curl -s -f http://localhost:$port/actuator/health | jq -r '.status')
    echo "  Port $port: $STATUS"
done

# 2. Functionality test
echo "API functionality:"
curl -X POST http://localhost:8081/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}' | jq

# 3. Performance check
echo "Performance baseline:"
./scripts/tests/performance-test.sh

# 4. Error rate check
echo "Error rate:"
curl -s 'http://localhost:9090/api/v1/query?query=rate(http_requests_total{status=~"5.."}[5m])' \
    | jq '.data.result[0].value[1]'

# 5. Log analysis
echo "Recent errors:"
docker-compose logs --tail=100 | grep ERROR | wc -l

echo "Verification complete"
```

## Patch Documentation

### Patch Release Notes Template

```markdown
# Version 1.1.0 - 2024-01-20

## Features
- New user activity tracking endpoint
- Improved error messages with error codes
- Add caching for frequently accessed data

## Bug Fixes
- Fixed user timezone handling in fitness tracking
- Corrected pagination offset calculation
- Fixed database connection leak in batch operations

## Security
- Updated Jackson to 2.15.2 (CVE-2023-46604)
- Enforced HTTPS for all API endpoints
- Added rate limiting to auth endpoints

## Performance
- Optimized user lookup query (50% faster)
- Implemented connection pooling optimization
- Added database query caching

## Upgrading
```bash
docker pull gym-auth-service:1.1.0
docker-compose down
docker-compose up -d
```

## Breaking Changes
None for this version.

## Known Issues
- PDF export may timeout for > 10,000 records (fixed in 1.1.1)

## Roadmap
- Message queue integration (v1.2)
- Advanced analytics dashboard (v1.2)
```

## Update Checklist

- [ ] All tests passing locally
- [ ] Security scan completed (no critical vulnerabilities)
- [ ] Dependencies updated and tested
- [ ] Version number updated in pom.xml
- [ ] Release notes written
- [ ] Docker images built and scanned
- [ ] Deployment plan documented
- [ ] Rollback plan ready
- [ ] Stakeholders notified
- [ ] Monitoring configured for new version
- [ ] Post-deployment verification completed

## Key References

- [Semantic Versioning](https://semver.org/)
- [Dependabot Documentation](https://docs.github.com/en/code-security/dependabot)
- [OWASP Dependency Updates](https://owasp.org/www-community/Component_Analysis)
- [Spring Boot Security Updates](https://spring.io/security)
- See also: [docs/deployment/02-deployment-strategies.md](../deployment/02-deployment-strategies.md)
- See also: [docs/operations/06-incident-management.md](06-incident-management.md)
