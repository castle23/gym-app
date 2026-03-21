# Testing Documentation & Resources

This section contains testing guides, test collections, and testing resources.

## Contents

- **01-testing-guide.md** - Comprehensive testing guide
- **02-testing-resources.md** - Testing tools and resources
- **03-postman-testing-guide.md** - API testing with Postman

## Test Collections

Postman collections for API testing:
- `Gym-Platform-Complete-API.postman_collection.json` - Full API test suite
- `Gym-Training-Service.postman_collection.json` - Training service tests
- `Gym_Platform_API.postman_collection.json` - Platform API tests
- `Gym_Platform_API_Testing_Environment.postman_environment.json` - Test environment configuration

## Subdirectories

- **fixtures/** - Test data and fixtures
- **results/** - Test execution results and reports
- **scenarios/** - Test scenarios and test cases

## Quick Start: Running Tests

### Using Postman
1. Import the collection: `Gym-Platform-Complete-API.postman_collection.json`
2. Import the environment: `Gym_Platform_API_Testing_Environment.postman_environment.json`
3. Run the collection from the Postman UI

### Using Command Line
```bash
./scripts/operational/run_postman_tests.sh
```

### Java/Unit Tests
See **01-testing-guide.md** for running unit and integration tests with Maven.

## Test Coverage

The project includes:
- **Unit Tests**: Individual component testing
- **Integration Tests**: Service interaction testing
- **API Tests**: Endpoint verification (via Postman)
- **RBAC Tests**: Permission and authorization testing
- **E2E Tests**: End-to-end workflow validation

## Testing Strategy

See **01-testing-guide.md** for:
- Test organization
- Test naming conventions
- Coverage requirements
- Mocking strategies
- CI/CD integration

## Postman Resources

For detailed Postman testing:
1. See **03-postman-testing-guide.md** for setup
2. Review test scenarios in **scenarios/**
3. Check previous test results in **results/**

## Environment Setup for Testing

The test environment includes:
- All 4 microservices running
- PostgreSQL database
- Test data seeded (see **fixtures/**)
- Health checks passing

## Continuous Integration

Tests run automatically on:
- Pull request creation
- Pre-commit (if hooks configured)
- Deployment pipeline

See CI configuration in the project root.

## Results & Reports

Previous test execution results are stored in **results/**.
Check these for:
- Test pass/fail status
- Coverage reports
- Performance metrics
- Issue discovery

## Troubleshooting Tests

If tests fail:
1. Check [Test Troubleshooting Guide](../troubleshooting/01-troubleshooting-guide.md)
2. Verify environment setup
3. Review test logs in **results/**
4. See **01-testing-guide.md** for debugging tips

## For More Information

- **API Documentation**: See [API Documentation](../docs/api/)
- **Development Guide**: See [Development Documentation](../development/)
- **Deployment Verification**: See [Deployment Guide](../deployment/)
