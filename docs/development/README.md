# Development Documentation

This section contains guides for developers working on the Gym Platform API.

## Contents

- **01-getting-started.md** - Setup and environment configuration
- **02-development-environment.md** - Local development setup
- **03-coding-standards.md** - Code conventions and best practices
- **04-testing.md** - Testing strategies and test writing
- **05-api-development.md** - Adding new API endpoints
- **06-troubleshooting-development.md** - Common development issues

## Subdirectories

- **debug/** - Debugging tools and techniques
- **examples/** - Code examples and sample implementations

## Key Resources

### Getting Started
If you're new to the project, start with:
1. **01-getting-started.md** - Initial setup
2. **02-development-environment.md** - Local environment
3. Explore the [Stack Documentation](../stack/) to understand technologies used

### Development Workflow
1. Check **03-coding-standards.md** for code conventions
2. Review **05-api-development.md** when adding features
3. Follow **04-testing.md** for testing requirements

### Debugging
- Common issues? See **06-troubleshooting-development.md**
- Complex debugging? Check the [debug/](debug/) directory
- Stuck? See [Troubleshooting Guide](../troubleshooting/)

## Technology Stack

- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **Build Tool**: Maven
- **Database**: PostgreSQL
- **API Documentation**: OpenAPI/Swagger
- **Authentication**: JWT with Spring Security

For complete stack details, see [Stack Documentation](../stack/).

## Testing

All code must include tests. See **04-testing.md** for:
- Unit testing requirements
- Integration testing guidelines
- API testing procedures
- Test coverage standards

## Code Organization

The project uses a microservices structure:
```
├── auth-service/
├── training-service/
├── tracking-service/
├── notification-service/
├── common/              # Shared utilities
└── api-gateway/         # (Optional) API Gateway
```

For architecture details, see [Architecture Documentation](../arquitectura/).

## Contributing

When developing:
1. Create a feature branch
2. Follow coding standards
3. Write tests for all new code
4. Update API documentation
5. Submit a pull request

See [CONTRIBUTING.md](../../CONTRIBUTING.md) for detailed guidelines.
