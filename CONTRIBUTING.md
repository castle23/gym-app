# Contributing to Gym Platform API

Thank you for contributing code to the Gym Platform API! This guide will help you make meaningful contributions to our codebase.

## Quick Start (5 Minutes)

Before reading the full guide, here's what you need to do to make your first contribution:

1. **Clone and setup:**
   ```bash
   git clone https://github.com/yourusername/gym-platform.git
   cd gym-platform
   git checkout develop
   git pull origin develop
   ```

2. **Create a feature branch:**
   ```bash
   git checkout -b feature/your-feature-name
   ```

3. **Make changes and test:**
   ```bash
   mvn clean test
   mvn spotbugs:check
   ```

4. **Commit with proper format:**
   ```bash
   git commit -m "feat: add new feature"
   ```

5. **Push and create PR:**
   ```bash
   git push origin feature/your-feature-name
   # Open PR on GitHub
   ```

That's it! Details below.

---

## Table of Contents

1. [Git Flow Workflow](#git-flow-workflow)
2. [Development Setup](#development-setup)
3. [Development Process](#development-process)
4. [Code Review](#code-review)
5. [Commit Message Format](#commit-message-format)
6. [Testing](#testing)
7. [Common Tasks](#common-tasks)
8. [Getting Help](#getting-help)

---

## Git Flow Workflow

We use **Git Flow** branching strategy. Here's how it works:

### Main Branches

- **main** - Production releases only. Always stable.
- **develop** - Integration branch for features. Should be stable but may have unreleased features.

### Supporting Branches

- **feature/*** - New features or significant changes
  - Branch from: `develop`
  - Merge back to: `develop` via Pull Request
  - Example: `feature/add-diet-logging`, `feature/improve-performance`

- **bugfix/*** - Bug fixes for develop branch
  - Branch from: `develop`
  - Merge back to: `develop` via Pull Request
  - Example: `bugfix/fix-auth-timeout`, `bugfix/handle-null-exercises`

- **hotfix/*** - Critical production fixes
  - Branch from: `main`
  - Merge back to: `main` AND `develop`
  - Example: `hotfix/security-patch`, `hotfix/database-connection-issue`

- **release/*** - Release preparation
  - Branch from: `develop`
  - Merge back to: `main` and `develop`
  - Example: `release/v1.2.0`

### Branch Naming Conventions

Use descriptive names in lowercase with hyphens:

```
✅ Good:
  feature/add-diet-logging
  feature/improve-exercise-search
  bugfix/fix-token-expiration
  hotfix/security-patch-jwt

❌ Bad:
  feature/new-feature
  bugfix/bug
  fix/stuff
  feature_add_diet
```

### Creating a Feature Branch

```bash
# 1. Make sure develop is up-to-date
git checkout develop
git pull origin develop

# 2. Create your feature branch
git checkout -b feature/your-feature-name

# 3. You're ready to make changes
```

### Workflow Example

```
develop: ---o---o---o
           /       \
feature:  o---o---o (feature/add-diet-logging)
           \     \
            \     PR created → reviewed → approved
             \
              Merge back to develop
```

---

## Development Setup

### Prerequisites

- **Java 17+** (OpenJDK or Oracle JDK)
- **Maven 3.8+** (for building)
- **Docker & Docker Compose** (for services and database)
- **PostgreSQL 13+** (if running without Docker)
- **Git 2.30+**

### Local Environment Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/gym-platform/gym-platform.git
   cd gym-platform
   ```

2. **Verify Java and Maven:**
   ```bash
   java -version
   mvn -version
   ```

3. **Start services with Docker Compose:**
   ```bash
   # From root directory
   docker-compose up -d
   ```

   Or build and run services individually:
   ```bash
   # Build all services
   mvn clean install

   # Terminal 1: Auth Service
   cd auth-service && mvn spring-boot:run

   # Terminal 2: Training Service
   cd training-service && mvn spring-boot:run

   # Terminal 3: Tracking Service
   cd tracking-service && mvn spring-boot:run

   # Terminal 4: Notification Service
   cd notification-service && mvn spring-boot:run
   ```

4. **Verify services are running:**
   ```bash
   curl http://localhost:8081/actuator/health
   curl http://localhost:8082/actuator/health
   curl http://localhost:8083/actuator/health
   curl http://localhost:8084/actuator/health
   ```

5. **Run tests:**
   ```bash
   mvn clean test
   ```

### Database Setup

The database is initialized automatically through Docker Compose. If you need to reset:

```bash
# Reset database
docker-compose exec postgres psql -U gym_user -d gym_platform -f scripts/db/reset.sql

# Or use the reset script
./scripts/database/reset-local.sh
```

See [Database Documentation](docs/database/README.md) for more details.

---

## Development Process

### Step-by-Step: Making a Change

#### 1. Create Your Feature Branch

```bash
git checkout develop
git pull origin develop
git checkout -b feature/your-feature-name
```

#### 2. Make Your Changes

- Make small, focused commits
- Don't mix features in one branch
- Follow code standards (see [Code Standards Guide](docs/development/02-code-standards-style-guide.md))
- Update tests as you go

#### 3. Write or Update Tests

Before committing, ensure your code has tests:

```bash
# Run tests for specific service
cd training-service
mvn clean test

# Run specific test class
mvn test -Dtest=ExerciseServiceTest

# Run with coverage report
mvn clean test jacoco:report
# View report at: target/site/jacoco/index.html

# Run integration tests
mvn verify
```

#### 4. Check Code Quality

```bash
# Run SpotBugs for potential bugs
mvn spotbugs:check

# Run Checkstyle for code style
mvn checkstyle:check

# Format code (if using Spotless)
mvn spotless:apply
```

#### 5. Commit with Conventional Format

Use **Conventional Commits** format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type:** One of:
- `feat` - New feature
- `fix` - Bug fix
- `refactor` - Code change without behavior change
- `perf` - Performance improvement
- `test` - Test changes
- `docs` - Documentation only
- `chore` - Dependencies, build tools, etc.

**Scope:** The component affected (optional but recommended):
- `auth` - Auth Service
- `training` - Training Service
- `tracking` - Tracking Service
- `notification` - Notification Service
- `api-gateway` - API Gateway
- `database` - Database layer
- `ci` - CI/CD

**Subject:** Concise description (50 chars max):
- Use imperative mood: "add" not "added" or "adds"
- Don't capitalize first letter
- No period at end

**Examples:**
```
✅ Good:
  feat(training): add exercise search by discipline
  fix(auth): handle token expiration gracefully
  refactor(tracking): simplify diet log calculations
  docs: update API reference
  chore(deps): upgrade spring-boot to 3.2.0

❌ Bad:
  Updated training service
  fixed bug
  Changes to API
  feat: implement new stuff
```

#### 6. Push Your Branch

```bash
git push origin feature/your-feature-name
```

#### 7. Create a Pull Request

On GitHub:

1. Go to the repository
2. Click "New Pull Request"
3. Base: `develop` | Compare: `feature/your-feature-name`
4. Fill in the PR template (see below)
5. Request reviewers
6. Address feedback

### Pull Request Template

```markdown
## Description
Brief description of what this PR does and why.

## Type of Change
- [ ] New feature
- [ ] Bug fix
- [ ] Breaking change
- [ ] Documentation

## Related Issue
Closes #123

## Testing
How to test this change?

- [ ] Unit tests added
- [ ] Integration tests added
- [ ] Manual test procedure: [steps]

## Checklist
- [ ] Code follows style guide
- [ ] Tests pass locally (`mvn clean test`)
- [ ] Code quality checks pass (`mvn spotbugs:check`, `mvn checkstyle:check`)
- [ ] No new warnings/errors in logs
- [ ] Documentation updated
- [ ] Database migrations tested (if applicable)

## Screenshots/Logs (if applicable)
```

---

## Code Review

### What Reviewers Look For

- ✅ **Correctness** - Does the code work as intended?
- ✅ **Clarity** - Is the code understandable?
- ✅ **Consistency** - Does it follow project standards?
- ✅ **Testing** - Is it properly tested?
- ✅ **Performance** - Any inefficiencies?
- ✅ **Security** - Any vulnerabilities?

### Addressing Feedback

1. Read all comments carefully
2. Ask for clarification if needed (in the comment)
3. Make changes in new commits (don't force-push)
4. Re-request review when changes are done
5. Continue until approved

### Approval & Merge

- Minimum 2 approvals required
- All CI/CD checks must pass
- Squash commits before merging (optional, depends on project preference)
- Delete feature branch after merge

See [Code Review Best Practices](docs/development/code-review-guide.md) for detailed guidance.

---

## Commit Message Format

### Format Specification

We follow **Conventional Commits** for all commits. This enables:
- Automatic changelog generation
- Semantic versioning
- Scanning commits for specific changes

### Full Format Example

```
feat(training): add bulk exercise import from CSV

Add ability to import multiple exercises from a CSV file. Includes
validation for required fields and conflict resolution for duplicates.

- Supports batch operations
- Validates discipline exists
- Logs warnings for skipped rows

Closes #456
```

### Format Breakdown

**Line 1 - Subject (required):**
- Max 50 characters
- Format: `type(scope): description`
- Use imperative mood

**Line 2 - Blank line (required for multi-line)**

**Lines 3+ - Body (optional):**
- Wrapped at 72 characters
- Explain WHAT and WHY, not HOW
- One blank line between paragraphs

**Footer:**
- Reference issues: `Closes #123`
- Note breaking changes: `BREAKING CHANGE: description`
- Co-author: `Co-authored-by: Name <email>`

### Examples

**Simple fix:**
```
fix(auth): return 401 instead of 500 for invalid tokens
```

**Feature with details:**
```
feat(tracking): add weight loss progress tracking

Users can now track weight loss goals and see progress charts.
Implemented with weekly averages to smooth data noise.

Closes #234
```

**Breaking change:**
```
refactor(api): rename exercise endpoint from /exercises to /v2/exercises

BREAKING CHANGE: Old /exercises endpoint removed. Use /v2/exercises
```

---

## Testing

### Requirements Before Creating PR

Every pull request MUST have:

- ✅ Unit tests for new functions/methods
- ✅ Integration tests for new endpoints
- ✅ All existing tests passing
- ✅ Test coverage >= 80% for new code

### Running Tests

```bash
# Run all tests
mvn clean test

# Run tests for specific service
cd auth-service && mvn clean test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run specific test method
mvn test -Dtest=UserServiceTest#testCreateUser

# Run with coverage report
mvn clean test jacoco:report

# Run integration tests
mvn verify

# Run with detailed output
mvn test -X
```

### Writing Tests

See [Testing Guide](tests/TESTING.md) for comprehensive testing documentation.

Example for Java/Spring Boot:

```java
@Test
public void testCreateExercise_WithValidData_ReturnsCreatedExercise() {
    // Arrange
    CreateExerciseRequest request = new CreateExerciseRequest();
    request.setName("Bench Press");
    request.setDiscipline("Chest");
    
    // Act
    ExerciseResponse response = exerciseService.createExercise(request);
    
    // Assert
    assertNotNull(response.getId());
    assertEquals("Bench Press", response.getName());
    assertEquals("Chest", response.getDiscipline());
}
```

### Test Naming Convention

Use clear, descriptive names:

```
✅ testCreateExercise_WithValidData_ReturnsCreatedExercise
✅ testLoginWithInvalidPassword_Returns401Unauthorized
✅ testGetUserProfile_WhenAuthTokenExpired_Returns401

❌ testExercise
❌ test1
❌ testCreateExerciseMethod
```

### API Testing

Use the Postman collection for API testing:

```bash
# Import collection in Postman
# Location: tests/collections/Gym-Platform-API-Master.postman_collection.json

# Or use Newman (CLI) to run tests
npm install -g newman
newman run tests/collections/Gym-Platform-API-Master.postman_collection.json \
  -e tests/environments/local.postman_environment.json
```

See [API Testing Guide](tests/TESTING.md) for details.

---

## Common Tasks

### Adding a New API Endpoint

1. Define the route and handler in controller
2. Create DTO classes for request/response
3. Write tests (before implementation - TDD)
4. Implement endpoint logic in service
5. Test manually with Postman collection
6. Update API documentation
7. Create PR

Example PR: `feat(training): add GET /api/training/exercises/search endpoint`

### Updating Dependencies

```bash
# Check for updates
mvn dependency:tree
mvn versions:display-dependency-updates

# Update specific dependency
mvn versions:use-dep-version -Dincludes=org.springframework.boot:spring-boot-starter:3.2.1

# Update all minor/patch versions (safe)
mvn versions:update-properties -DincludedProperties=spring-boot.version

# Always test after updating dependencies
mvn clean test
```

### Working with Database Migrations

1. Create migration file in `src/main/resources/db/migration/`:
   ```sql
   -- V2__AddNewColumn.sql
   ALTER TABLE exercises ADD COLUMN new_column VARCHAR(255);
   ```

2. Flyway automatically runs migrations on application startup

3. Test migration:
   ```bash
   # Reset database to test from scratch
   docker-compose exec postgres psql -U gym_user -d gym_platform \
     -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
   
   # Run application to trigger migration
   mvn spring-boot:run
   ```

4. Commit migration file with your changes

See [Database Migrations Guide](docs/database/migrations.md) for details.

### Debugging Across Microservices

```bash
# Check service logs
docker logs training-service
docker logs training-service -f  # Follow logs

# Check application logs with Spring Boot Actuator
curl http://localhost:8082/actuator/health
curl http://localhost:8082/actuator/env

# Enable debug logging
# Add to application.yml:
# logging.level.com.gym: DEBUG

# Use IDE debugger
# 1. Add breakpoint in code
# 2. Run with debug flag: mvn spring-boot:run -Dspring-boot.run.fork=false
# 3. Set IDE debugger to port 5005

# Check database directly
docker-compose exec postgres psql -U gym_user -d gym_platform
# Then run SQL queries like: SELECT * FROM users;
```

---

## Getting Help

### Common Questions

**Q: How long should a feature branch be?**
A: Keep it focused. If you're working on something for > 3 days, consider breaking into smaller PRs.

**Q: Can I commit directly to develop?**
A: No. All code goes through feature branches and PR review.

**Q: What if my PR gets rejected?**
A: That's normal! Ask clarifying questions, make improvements, and re-submit.

**Q: How do I update my branch if develop has changed?**
A:
```bash
git fetch origin
git rebase origin/develop
# If conflicts: resolve them, then `git rebase --continue`
```

### Getting Support

- **Questions about code?** Ask in PR comments
- **Need help debugging?** Check logs: `docker logs service-name`
- **Stuck on git?** See our [Git Troubleshooting Guide](docs/development/git-troubleshooting.md)
- **Architecture decisions?** See [ADRs](docs/adr/)
- **Code standards?** See [Code Standards Guide](docs/development/02-code-standards-style-guide.md)

### Escalation

If you're blocked or need urgent help:

1. Reach out to your team lead
2. Post in #engineering Slack channel
3. Check existing issues/PRs for solutions

---

## Code of Conduct

We expect all contributors to:

- Be respectful and professional
- Assume good intentions
- Provide constructive feedback
- Help others learn and grow
- Report problems constructively

See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for details.

---

## Additional Resources

- **Testing Guide**: [tests/TESTING.md](tests/TESTING.md) - Comprehensive API testing guide
- **API Reference**: [docs/api/](docs/api/) - Endpoint documentation
- **Architecture Decisions**: [docs/adr/](docs/adr/) - Why we made key technical choices
- **Code Standards**: [docs/development/02-code-standards-style-guide.md](docs/development/02-code-standards-style-guide.md) - Coding standards
- **Integration Testing**: [docs/development/03-integration-testing-guide.md](docs/development/03-integration-testing-guide.md) - Cross-service testing

---

**Last Updated**: March 21, 2026  
**Status**: Active  
**Maintainers**: Gym Platform Core Team
