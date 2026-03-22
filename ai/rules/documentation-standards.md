# Documentation Standards

> Rules for API documentation, code documentation, and commit messages.

## OpenAPI / Swagger Annotations

1. **All DTOs** must have `@Schema` annotations with `description` and `example` on every field.
   ```java
   @Schema(description = "User's email address", example = "john@example.com")
   private String email;
   ```
2. **All endpoints** must have `@Operation(summary, description)` and at least one `@ApiResponse`.
3. **All controllers** must have `@Tag(name, description)` at the class level.
4. Protected endpoints must include `@SecurityRequirement(name = "bearerAuth")`.
5. Swagger/OpenAPI paths (`/swagger-ui/**`, `/v3/api-docs/**`) must be excluded from security filters in `SecurityConfig`.
   > See also: `../rules/security-standards.md` rule on public endpoints.

## Documentation Style

6. Be **concise and technical**. Avoid filler words ("basically", "simply", "just").
7. Include code examples for anything non-obvious.
8. Use present tense and active voice ("Returns the user" not "The user will be returned").

## API Documentation Template

9. Every endpoint's documentation must cover:
   - **Endpoint overview** — One sentence: what it does.
   - **HTTP method and URL** — e.g., `POST /auth/register`.
   - **Authentication** — Required role(s) or "Public".
   - **Request parameters** — Path params, query params, request body with types.
   - **Response format** — Success and error response shapes.
   - **Example** — At least one request/response pair.

## Feature Documentation Template

10. Feature docs (in `ai/docs/` or design docs) must follow:
    - **Overview** — What the feature is.
    - **Motivation** — Why it exists and what problem it solves.
    - **Implementation** — Key classes, services, and data flow.
    - **Usage** — How to use it (API calls, configuration).
    - **Testing** — How to verify it works.

## Code Documentation

11. JavaDoc on **public API methods only** (controllers, public service interfaces).
12. Comments explain **why**, never restate **what** the code does.
    > See also: `../rules/coding-standards.md` rules 11-13.
13. README files must exist per service with: purpose, setup instructions, environment variables, and run commands.

## Commit Messages

14. Follow **Conventional Commits** format:
    ```
    <type>(<scope>): <subject>
    ```
15. Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `ci`.
16. Scope is the affected service or module: `auth`, `training`, `tracking`, `notification`, `common`, `gateway`.
17. Subject is imperative mood, lowercase, no period: `feat(auth): add password reset endpoint`.
18. Body (optional) explains **why**, not what. Footer references issues: `Closes #42`.
    > See also: `../rules/git-workflow.md` for branching and release workflow.

## Branch Naming

19. Format: `<type>/<ticket>-<short-description>`.
    - Examples: `feat/GYM-42-password-reset`, `fix/GYM-99-null-pointer-training`.
20. Types mirror commit types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.
21. Use lowercase and hyphens only. No underscores or uppercase.

## Changelog

22. Maintain a `CHANGELOG.md` with categories: **Added**, **Changed**, **Deprecated**, **Removed**, **Fixed**, **Security**.
23. Each entry references the commit or PR that introduced it.
24. Update the changelog as part of the PR, not retroactively.
