# Git Workflow

> Branching, commit, and release rules for the Gym Platform API.

## Branching Strategy

1. `main` — Production-ready code. Protected. Merges via PR only.
2. `develop` — Integration branch. All feature branches merge here first.
3. Feature branches: `feat/<ticket>-<short-description>` (e.g., `feat/GYM-42-password-reset`).
4. Bugfix branches: `fix/<ticket>-<short-description>`.
5. Other types: `docs/`, `refactor/`, `test/`, `chore/`, `ci/` — same pattern.
6. Use lowercase and hyphens only. No underscores, spaces, or uppercase.
   > See also: `../rules/documentation-standards.md` rules 19-21 for branch naming details.

## Commit Messages

7. Format: `<type>(<scope>): <subject>`.
8. **Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `ci`.
9. **Scope**: The affected service or module — `auth`, `training`, `tracking`, `notification`, `common`, `gateway`.
10. **Subject**: Imperative mood, lowercase, no trailing period.
    ```
    feat(auth): add password reset endpoint
    fix(tracking): correct calorie calculation for rest days
    test(training): add controller tests for exercise CRUD
    docs(common): update exception handling guide
    ```
11. Optional body: Explain **why** the change was made, not what changed. Wrap at 72 characters.
12. Optional footer: Reference issues (`Closes #42`, `Refs #15`).

## Commit Discipline

13. Commit **after each significant, self-contained task** — not at the end of the day.
14. Each commit must leave the codebase in a buildable state.
15. **All tests must pass before committing**: `mvn clean test`.
    > See also: `../rules/testing-standards.md` rules 19-23 for test commands.
16. Never commit with failing tests. Use `git stash` if you need to switch context.

## Pre-Commit Checklist

17. Code compiles without errors.
18. All existing tests pass.
19. New code has corresponding tests.
20. No hardcoded secrets or credentials in any file.
    > See also: `../rules/security-standards.md` rules 22-25 for secrets management.
21. No `TODO` or `FIXME` comments without a linked ticket number.
22. All documentation (`docs/api/`, `docs/database/`, `ai/memory/`, `ai/plans/`) and test registries are up-to-date.
23. Code follows standards in `../rules/coding-standards.md`.

## Code Review

23. All merges to `develop` and `main` require a pull request.
24. PR description must include: what changed, why, how to test, and any migration steps.
25. Reviewers check for: correctness, test coverage, security, naming, and adherence to standards.
26. Address all review comments before merging. Resolve, don't dismiss.

## Semantic Versioning

27. Format: `MAJOR.MINOR.PATCH`.
    - **MAJOR** — Breaking API changes (incompatible contract changes).
    - **MINOR** — New features, backward-compatible.
    - **PATCH** — Bug fixes, backward-compatible.
28. Tag releases in git: `git tag -a v1.2.0 -m "Release v1.2.0"`.
29. Push tags: `git push origin v1.2.0`.

## Changelog

30. Maintain `CHANGELOG.md` at the project root.
31. Categories: **Added**, **Changed**, **Deprecated**, **Removed**, **Fixed**, **Security**.
32. Update the changelog in the same PR as the code change, not retroactively.
33. Each entry references the PR or commit hash.
    > See also: `../rules/documentation-standards.md` rules 22-24.

## Release & Deployment

34. Pre-deployment checklist:
    - All tests pass (`mvn clean test`).
    - No hardcoded secrets.
    - `.env` configured for target environment.
    - Database backup completed (if schema changes are involved).
35. **Database backup is mandatory** before any migration or schema-altering deployment.
36. Rollback plan must exist before deploying to production.

## Breaking Changes

37. Breaking changes require:
    - Deprecation notice in the **previous** release.
    - Migration guide in the PR description and docs.
    - Timeline communicated to consumers (minimum one release cycle).
38. Mark deprecated endpoints with `@Deprecated` and `@Operation(deprecated = true)`.
39. Remove deprecated features only after the announced timeline expires.
