# Gym Platform - Code Standards & Style Guide

Comprehensive guide for consistent coding standards across Java/Spring Boot, Bash, Python, and Infrastructure/IaC.

## Table of Contents

1. [Global Standards](#global-standards)
2. [Java/Spring Boot](#javaspring-boot)
3. [Bash Scripts](#bash-scripts)
4. [Python](#python)
5. [Infrastructure/IaC](#infrastructureiac)
6. [Tooling & Automation](#tooling--automation)

---

## Global Standards

### Comment Philosophy

**Why, not What. Code should be self-documenting.**

✅ **Good:**
```java
// Calculate discount if user has been active for > 1 year
if (userAgeInMonths > 12) {
    discount = 0.1;
}
```

❌ **Bad:**
```java
// Set discount to 0.1
if (userAgeInMonths > 12) {
    discount = 0.1;
}
```

### Naming Conventions (Universal)

- **Classes/Types**: PascalCase: `ExerciseService`, `UserProfile`
- **Functions/Methods**: camelCase: `getUserById`, `calculateDistance`
- **Constants**: UPPER_SNAKE_CASE: `MAX_RETRIES`, `DEFAULT_TIMEOUT`
- **Variables**: camelCase: `userId`, `isActive`
- **Files**: lowercase with hyphens (Python/Bash), camelCase (Java)

**Be specific and clear:**
```
✅ getUserById, validateEmail, logAuthFailure, calculateBMI
❌ get, validate, log, calc
```

### Error Handling

**Always handle errors explicitly. Never silently fail.**

Java:
```java
try {
    return exerciseService.findById(id);
} catch (NotFoundException e) {
    log.warn("Exercise not found: {}", id);
    throw new ExerciseException("Exercise not found: " + id);
}
```

Bash:
```bash
if ! command; then
    echo "Error: command failed" >&2
    return 1
fi
```

Python:
```python
try:
    return get_user(user_id)
except UserNotFound as e:
    logger.warning(f"User not found: {user_id}")
    raise UserException(f"User not found: {user_id}")
```

### Logging Standards

Use appropriate log levels:
- **ERROR**: System errors, failures, exceptions
- **WARN**: Unexpected conditions, recoverable issues
- **INFO**: Important business events, state changes
- **DEBUG**: Detailed debugging information
- **TRACE**: Very detailed tracing (rarely used)

```java
log.error("Failed to create exercise: {}", e.getMessage(), e);
log.warn("Database connection slow: {}ms", elapsedMs);
log.info("User registered: {}", userId);
log.debug("Processing exercise with id: {}", id);
```

### Code Organization

- One responsibility per file
- Group related functionality
- Keep methods/functions under 50 lines
- Order: constants, fields, constructors, public methods, private methods

### Security Considerations

- **No hardcoded secrets** (use environment variables/vaults)
- **Validate all inputs** (never trust user input)
- **Encrypt sensitive data** at rest and in transit
- **Use HTTPS/TLS** for all communication
- **Implement rate limiting** on public endpoints
- **Log security events** (login attempts, authorization failures)

---

## Java/Spring Boot

### Package Structure

```
com.gymplatform.training/
├── config/           # Spring configuration
├── controller/       # REST controllers
├── service/          # Business logic
├── repository/       # Data access
├── model/            # Domain models
├── exception/        # Custom exceptions
├── util/             # Utilities
└── dto/              # Data transfer objects
```

### Class Organization

```java
@RestController
@RequestMapping("/api/v1/exercises")
@Slf4j
public class ExerciseController {
    // 1. Constants
    private static final int MAX_PAGE_SIZE = 100;
    
    // 2. Dependencies (via constructor injection)
    private final ExerciseService exerciseService;
    
    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }
    
    // 3. Public methods
    @GetMapping("/{id}")
    public ResponseEntity<ExerciseDTO> getExercise(@PathVariable Long id) {
        // ...
    }
    
    // 4. Private methods
    private void validateInput(Long id) {
        // ...
    }
}
```

### Naming in Java

- **Classes**: Noun (ExerciseService, UserProfile)
- **Methods**: Verb (getUser, createExercise, calculateBMI)
- **Booleans**: is/has prefix (isActive, hasValidation)
- **Getters/Setters**: get/set prefix (getId, setName)

### Error Handling

```java
@ExceptionHandler(NotFoundException.class)
public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
    ErrorResponse error = new ErrorResponse("NOT_FOUND", e.getMessage());
    return ResponseEntity.status(404).body(error);
}
```

### Testing

- **Arrange-Act-Assert** pattern
- **One assertion** per test (or logically grouped)
- **Descriptive names**: `testGetUser_WithValidId_ReturnsUser`
- **Mock external dependencies**

```java
@Test
public void testCreateExercise_WithValidData_ReturnsCreatedExercise() {
    // Arrange
    CreateExerciseRequest request = new CreateExerciseRequest("Bench", "Chest");
    
    // Act
    ExerciseDTO result = service.createExercise(request);
    
    // Assert
    assertNotNull(result.getId());
    assertEquals("Bench", result.getName());
}
```

### Documentation

Use JavaDoc for public APIs:

```java
/**
 * Creates a new exercise.
 *
 * @param request the exercise creation request
 * @return the created exercise DTO
 * @throws ExerciseException if the exercise already exists
 */
public ExerciseDTO createExercise(CreateExerciseRequest request) {
    //...
}
```

---

## Bash Scripts

### Script Header

Every script should start with:

```bash
#!/bin/bash
# Description of what this script does
# Usage: ./script-name.sh [args]
# Example: ./backup-db.sh production

set -euo pipefail  # Exit on error, undefined vars, pipe failures
IFS=$'\n\t'        # Safe word splitting

# Script body...
```

### Error Handling

```bash
# Use trap for cleanup
cleanup() {
    rm -f "$temp_file"
    log "Cleanup completed"
}
trap cleanup EXIT

# Check commands
if ! command -v docker >/dev/null 2>&1; then
    log_error "Docker not installed"
    exit 1
fi

# Check file exists
if [[ ! -f "$config_file" ]]; then
    log_error "Config file not found: $config_file"
    exit 1
fi
```

### Variables

```bash
# Use meaningful names
readonly LOG_FILE="/var/log/backup.log"
readonly MAX_RETRIES=3
readonly TIMEOUT=300

# Quote variables
db_name="$1"    # Good
db_name=$1      # Bad - breaks with spaces
```

### Functions

```bash
# Log function
log() {
    local level="$1"
    shift
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] [$level] $@" | tee -a "$LOG_FILE"
}

log_error() { log "ERROR" "$@"; }
log_info() { log "INFO" "$@"; }

# Use functions for reusable logic
check_prerequisites() {
    if [[ ! -d "$data_dir" ]]; then
        log_error "Data directory not found: $data_dir"
        return 1
    fi
}

# Meaningful output
main() {
    log_info "Starting backup..."
    check_prerequisites || return 1
    # ... rest of script
}

main "$@"
```

### Portability

```bash
# Use POSIX where possible (not Bash-only)
# ❌ Array syntax (Bash-only)
declare -A config

# ✅ POSIX-compatible
config_file="/etc/config"

# Document shell requirement
# Only if you must use Bash-specific features
```

---

## Python

### PEP 8 Compliance

```python
# Line length: 88 characters (with Black formatter)
# Imports: sorted, grouped
import os
import sys
from typing import Optional

from flask import Flask
from sqlalchemy import create_engine
```

### Naming

```python
# Functions and variables: snake_case
def get_user_by_id(user_id: int) -> User:
    pass

# Classes: PascalCase
class ExerciseService:
    pass

# Constants: UPPER_SNAKE_CASE
MAX_RETRIES = 3
DEFAULT_TIMEOUT = 300
```

### Type Hints & Docstrings

```python
def create_exercise(name: str, discipline: str) -> Exercise:
    """
    Create a new exercise.
    
    Args:
        name: Exercise name (e.g., 'Bench Press')
        discipline: Muscle group (e.g., 'Chest')
    
    Returns:
        Created Exercise object
    
    Raises:
        ExerciseException: If exercise already exists
    """
    # Implementation
    pass
```

### Error Handling

```python
try:
    user = get_user(user_id)
except UserNotFound:
    logger.warning(f"User not found: {user_id}")
    raise UserException(f"Could not find user {user_id}")
except DatabaseError as e:
    logger.error(f"Database error: {e}")
    raise
```

### Testing

```python
def test_get_user_with_valid_id_returns_user():
    """Test that get_user returns user when valid ID provided."""
    # Arrange
    user_id = 1
    expected_user = User(id=1, email="test@example.com")
    
    # Act
    result = get_user(user_id)
    
    # Assert
    assert result.id == expected_user.id
    assert result.email == expected_user.email
```

---

## Infrastructure/IaC

### Terraform Organization

```
infrastructure/
├── main.tf              # Main resources
├── variables.tf         # Variable definitions
├── outputs.tf           # Output definitions
├── terraform.tfvars     # Variable values (local)
├── modules/
│   ├── networking/
│   ├── database/
│   └── compute/
└── environments/
    ├── development/
    ├── staging/
    └── production/
```

### Terraform Style

```hcl
# Meaningful names
resource "aws_eks_cluster" "main" {
  name    = var.cluster_name
  version = "1.24"
  
  vpc_config {
    subnet_ids = var.subnet_ids
  }
}

# Variables with descriptions
variable "cluster_name" {
  description = "Name of the EKS cluster"
  type        = string
}

# Outputs for reference
output "cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = aws_eks_cluster.main.endpoint
}
```

### Secrets Management

```hcl
# ❌ Never hardcode secrets
database_password = "supersecret123"

# ✅ Use variable or secret store
database_password = var.db_password  # from .tfvars (git-ignored)
# Or better: use AWS Secrets Manager / Vault
```

### DRY Principles

```hcl
# ✅ Use modules for reusability
module "database" {
  source = "./modules/database"
  
  environment = var.environment
  instance_type = var.db_instance_type
}

# ✅ Use locals for repeated values
locals {
  common_tags = {
    Environment = var.environment
    ManagedBy   = "Terraform"
    Project     = var.project_name
  }
}

resource "aws_instance" "app" {
  # ...
  tags = merge(
    local.common_tags,
    { Name = "app-server" }
  )
}
```

---

## Tooling & Automation

### Linters & Formatters

**Java:**
- Checkstyle (style enforcement)
- SpotBugs (bug detection)
- SonarQube (code quality)

**Python:**
- Black (formatting)
- Flake8 (linting)
- mypy (type checking)

**Bash:**
- ShellCheck (linting)

**Terraform:**
- terraform fmt (formatting)
- tflint (linting)

### Pre-Commit Hooks

```bash
#!/bin/bash
# .git/hooks/pre-commit

# Run code quality checks
mvn spotbugs:check || exit 1
mvn checkstyle:check || exit 1

# Run tests
mvn clean test || exit 1

# Check for hardcoded secrets
git diff --cached | grep -E "password|secret|token" && {
    echo "Potential secret found in commit!"
    exit 1
}
```

### CI/CD Checks

Every commit should pass:
- ✅ Code style linting
- ✅ Type checking
- ✅ Unit tests (>80% coverage)
- ✅ Static analysis
- ✅ Security scanning

---

## Code Review Checklist

Before submitting code:

- [ ] Code follows all standards in this guide
- [ ] All tests pass (unit + integration)
- [ ] Code coverage >= 80% for new code
- [ ] No hardcoded secrets or credentials
- [ ] No TODO comments without issues
- [ ] Documentation updated (JavaDoc, docstrings, comments)
- [ ] Commit messages follow Conventional Commits
- [ ] No unnecessary dependencies added
- [ ] Performance impact assessed

---

## Deprecation Process

When deprecating code:

```java
@Deprecated(since = "2.0", forRemoval = true)
@Scheduled(removal = "3.0")
public void oldMethod() {
    // Use newMethod() instead
    newMethod();
}
```

---

## Additional Resources

- **Testing Guide**: [tests/TESTING.md](../../tests/TESTING.md)
- **Contributing Guide**: [CONTRIBUTING.md](../../CONTRIBUTING.md)
- **Architecture Decisions**: [docs/adr/](../adr/)

---

**Last Updated:** March 21, 2026  
**Version:** 1.0  
**Status:** Active
