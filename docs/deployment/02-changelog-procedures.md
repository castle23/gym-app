# Changelog Procedures

> **Target Audience:** Release managers, team leads, developers responsible for release management

This document defines how the Gym Platform team maintains the CHANGELOG.md and manages releases using semantic versioning.

---

## Table of Contents

1. [Semantic Versioning](#semantic-versioning)
2. [Writing Changelog Entries](#writing-changelog-entries)
3. [Maintaining Unreleased Section](#maintaining-unreleased-section)
4. [Release Process](#release-process)
5. [Version Management](#version-management)
6. [Breaking Changes & Migration](#breaking-changes--migration)

---

## Semantic Versioning

All releases follow [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html) format:

**MAJOR.MINOR.PATCH-PRERELEASE+BUILD**

Example: `1.2.3-beta.1+build.123`

### Understanding Each Component

#### MAJOR Version (Breaking Changes)

Increment when making incompatible API changes that require code changes by consumers.

**When to increment MAJOR:**
- Removing an API endpoint
- Changing the format of required response fields
- Changing authentication mechanism (e.g., moving from Bearer tokens to API keys)
- Requiring database migration (users must run migrations)
- Removing a documented feature

**Example breaking changes:**
- Change: Authentication header format from `Authorization: Bearer X` to `X-Auth-Token: X`
- Change: Workout response format removes `duration_seconds` field
- Change: Database schema requires manual migration (cannot auto-migrate)

**Current Version:** 1.0.0  
**Next MAJOR:** 2.0.0 (when we have breaking changes)

#### MINOR Version (New Features)

Increment when adding new features or capabilities in a backward-compatible manner.

**When to increment MINOR:**
- Adding new API endpoints
- Adding optional fields to responses
- Adding new services or modules
- New configuration options
- Performance improvements

**Example non-breaking additions:**
- Add new endpoint: `GET /api/training/routines/recommended`
- Add optional field: `weight_kg` to workout_exercises response
- Add new notification channel: SMS support

**Increment Pattern:** 1.0.0 → 1.1.0 → 1.2.0 → 1.3.0

#### PATCH Version (Bug Fixes)

Increment when fixing bugs or making internal improvements with no feature changes.

**When to increment PATCH:**
- Fixing bugs reported by users
- Performance optimizations
- Security patches (non-breaking)
- Documentation fixes
- Database query optimizations

**Example patches:**
- Fix: Incorrect weight calculation in progress tracking
- Fix: Rate limiting not working on login endpoint
- Security: Update vulnerable dependency

**Increment Pattern:** 1.0.0 → 1.0.1 → 1.0.2 → 1.0.3

### Pre-release Versions

Pre-release versions are used for testing before final release.

**Format:** `MAJOR.MINOR.PATCH-TYPE.NUMBER`

**Types:**
- `alpha` - Early testing, features may be incomplete
- `beta` - Feature complete, but may have bugs
- `rc` (release candidate) - Final testing before release

**Examples:**
```
1.1.0-alpha.1      # First alpha of v1.1.0
1.1.0-beta.1       # First beta
1.1.0-beta.2       # Second beta
1.1.0-rc.1         # Release candidate
1.1.0               # Final release
```

**Usage Timeline:**
```
Sprint 1-2: 1.1.0-alpha.1 → 1.1.0-alpha.2 (internal testing)
Sprint 3:   1.1.0-beta.1 → 1.1.0-beta.2 (customer testing)
Sprint 4:   1.1.0-rc.1 (final testing)
Release:    1.1.0 (production)
```

---

## Writing Changelog Entries

The CHANGELOG.md uses categories to organize changes. Each entry should be user-focused and clear.

### Categories

#### Added
New features or capabilities added to the system.

```markdown
### Added
- New endpoint: `GET /api/training/exercises/search?query=bench` for searching exercises
- Support for bulk operations: `POST /api/training/workouts/bulk-create`
- Real-time workout progress notifications via WebSocket
```

#### Changed
Changes to existing functionality (backward compatible improvements).

```markdown
### Changed
- Improved performance of workout list endpoint (now < 300ms p95)
- Updated error messages to be more user-friendly
- Database indexes optimized for common queries
- Changed notification delivery from immediate to batched (improves throughput)
```

#### Deprecated
Features that will be removed in a future version.

```markdown
### Deprecated
- Endpoint `GET /api/training/workouts` (use `GET /api/training/users/{id}/workouts` instead)
- Field `user_metadata` in user response (use separate `GET /api/auth/users/{id}/metadata`)
- HS256 JWT algorithm (switch to RS256 before v2.0.0)
```

#### Removed
Previously deprecated features that are now gone.

```markdown
### Removed
- Removed `/api/v1/` endpoints (upgrade to `/api/v2/`)
- Removed support for basic auth (use JWT only)
- Removed `body_fat` field from weight logs (use `metrics` table)
```

#### Fixed
Bug fixes and corrections.

```markdown
### Fixed
- Fixed calculation of daily calorie average (was excluding zero days)
- Fixed race condition in concurrent workout creation
- Fixed rate limiting not being applied to all endpoints
```

#### Security
Security improvements and vulnerability fixes.

```markdown
### Security
- Fixed SQL injection vulnerability in exercise search endpoint
- Updated vulnerable dependency: log4j from 2.14 to 2.17
- Improved JWT token expiration enforcement (now checks on every request)
```

### Writing Guidelines

#### Be Specific and User-Focused

✅ **Good:**
```
- Added GET /api/training/workouts/{id} endpoint for fetching individual workout details
```

❌ **Bad:**
```
- Added new endpoint
```

✅ **Good:**
```
- Fixed user authentication timeout when token refresh rate exceeded limits (#234)
```

❌ **Bad:**
```
- Fixed token bug
```

#### Include Context When Necessary

```markdown
### Added
- Support for custom workout routines: users can now create, save, and share custom workout programs
  (see CONTRIBUTING.md for API details)
```

#### Link to Related Issues/PRs

```markdown
### Fixed
- Fixed incorrect weight calculation in progress tracking (#456)
- Improved error handling in notification delivery (#457, #458)
```

#### Highlight Breaking Changes Clearly

```markdown
### Changed
- ⚠️ **BREAKING**: Authentication header format changed from `Authorization: Bearer X` to `X-Auth-Token: X`
  This is a breaking change. See MIGRATION_v2.0.0.md for migration steps.
```

### Common Entry Patterns

#### Adding a New Feature

```markdown
### Added
- New dashboard endpoint: `GET /api/tracking/dashboard` returns aggregated user statistics
  (workouts completed, average calories, weight trend)
```

#### Fixing a Bug

```markdown
### Fixed
- Fixed workout duration calculation when exercises have variable duration (#123)
- Fixed race condition in user registration flow
```

#### Performance Improvement

```markdown
### Changed
- Improved GET /api/training/exercises response time by 40% (from 800ms to 480ms)
  through database indexing optimization
```

#### Deprecation Notice

```markdown
### Deprecated
- Endpoint `GET /api/users/{id}/stats` is deprecated in v1.2.0
  Use `GET /api/tracking/dashboard` instead (provides same data with additional features)
  Will be removed in v2.0.0
```

---

## Maintaining Unreleased Section

During development, all changes should be documented in the `[Unreleased]` section of CHANGELOG.md.

### Process for Adding Entries

**1. When making a PR, add a changelog entry:**

```markdown
## [Unreleased]

### Added
- New endpoint: `GET /api/training/routines/recommended` returns AI-recommended routines (#456)
```

**2. PR review should check:**
- [ ] Changelog entry added
- [ ] Category is correct (Added, Fixed, etc.)
- [ ] Description is clear and user-focused
- [ ] Issue/PR number linked
- [ ] Spelling and grammar checked

**3. Merge PR with changelog entry**

### Preventing Duplicates

When multiple PRs contribute to the same feature:

❌ **Don't create multiple entries:**
```markdown
### Added
- Started work on workout recommendation engine (#100)
### Added
- Added ML model for workout recommendations (#110)
### Added
- Completed workout recommendation engine endpoint (#120)
```

✅ **Combine into one entry:**
```markdown
### Added
- New endpoint: `GET /api/training/routines/recommended` returns AI-recommended routines
  based on user's goals and history (#100, #110, #120)
```

### Verification Checklist

Before release, verify:
- [ ] All PRs since last release have changelog entries
- [ ] No duplicate entries for same feature
- [ ] All entries are user-focused (not implementation details)
- [ ] Breaking changes are clearly marked with ⚠️
- [ ] All category sections have content (remove empty sections)

---

## Release Process

### Timeline

```
Week 1-3: Development
  - Add entries to [Unreleased] as features are completed
  - Follow normal PR process with changelog requirement

Week 4: Freeze & Prepare (Monday of release week)
  - Code freeze - no new features accepted
  - Create release branch: git checkout -b release/v1.1.0
  - Update CHANGELOG.md: move [Unreleased] entries to [1.1.0]

Week 4: Testing (Tuesday-Wednesday)
  - Run full test suite in staging
  - Smoke tests in production-like environment
  - Security scan with OWASP ZAP

Week 4: Tag & Deploy (Thursday)
  - Update version numbers in code files
  - Create git tag: git tag -a v1.1.0 -m "Release v1.1.0"
  - Push tag to GitHub

Week 4: Release (Friday)
  - Deploy to production
  - Create GitHub release with release notes
  - Monitor for issues
  - Communicate with stakeholders
```

### Step-by-Step Release Instructions

#### Step 1: Prepare Release Branch

```bash
# From main/master branch, create release branch
git checkout -b release/v1.1.0

# Verify only intended changes are included
git diff main develop
```

#### Step 2: Update CHANGELOG.md

```markdown
## [Unreleased]
(empty - all changes moved to version below)

### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security

## [1.1.0] - 2026-03-28

### Added
- New endpoint for workout recommendations
- Support for multiple exercise variants

### Fixed
- Weight calculation in progress tracking
- Rate limiting on auth endpoints

### Security
- Updated vulnerable dependency: log4j
```

#### Step 3: Update Version Numbers

**Java (pom.xml):**
```xml
<version>1.1.0</version>
```

**Docker (Dockerfile):**
```dockerfile
LABEL version="1.1.0"
```

**Kubernetes (deployment.yaml):**
```yaml
image: gym-platform/training-service:1.1.0
```

**Configuration (application.yml):**
```yaml
app:
  version: 1.1.0
```

#### Step 4: Commit Release Changes

```bash
# Stage version changes
git add pom.xml Dockerfile docs/deployment/deployment.yaml

# Commit with clear message
git commit -m "chore(release): prepare v1.1.0"

# Stage CHANGELOG update
git add CHANGELOG.md

# Commit with separate message
git commit -m "docs(changelog): document v1.1.0 release"

# Verify commits
git log --oneline -5
```

#### Step 5: Create Git Tag

```bash
# Create annotated tag (with message)
git tag -a v1.1.0 -m "Release v1.1.0 - Workout recommendations and performance improvements"

# Verify tag
git show v1.1.0

# Push tag to GitHub
git push origin v1.1.0
```

#### Step 6: Create GitHub Release

From GitHub UI or CLI:

```bash
# Using GitHub CLI
gh release create v1.1.0 \
  --title "v1.1.0 - Workout Recommendations" \
  --notes "## Features\n- Workout recommendations\n- Performance improvements"
```

Or manually create from GitHub UI:
- Go to Releases
- Click "New Release"
- Select tag: v1.1.0
- Title: "v1.1.0 - Workout Recommendations"
- Copy CHANGELOG.md entries to release notes
- Click "Publish release"

#### Step 7: Deploy to Production

```bash
# Update deployment to use new image tag
kubectl set image deployment/training-service \
  training-service=gym-platform/training-service:v1.1.0 \
  -n gym-platform

# Monitor rollout
kubectl rollout status deployment/training-service -n gym-platform

# Verify health
curl https://api.gym-platform.com/health
```

#### Step 8: Post-Release Verification

```bash
# Check API version endpoint
curl https://api.gym-platform.com/api/auth/version
curl https://api.gym-platform.com/api/training/version
curl https://api.gym-platform.com/api/tracking/version
curl https://api.gym-platform.com/api/notifications/version

# Run smoke tests
./scripts/operational/smoke-tests.sh production

# Monitor error rates in Grafana dashboard
# Check logs for any anomalies: kubectl logs -f deployment/<service>
```

---

## Version Management

### Tracking Version in Code

Services should expose their version for verification:

```java
// AuthService.java
@RestController
@RequestMapping("/api")
public class InfoController {
  
  @GetMapping("/version")
  public VersionInfo getVersion() {
    return VersionInfo.builder()
      .version("1.1.0")
      .buildTime(LocalDateTime.now())
      .gitCommit(System.getenv("GIT_COMMIT"))
      .build();
  }
}
```

```bash
# Verify versions after deployment
curl https://api.gym-platform.com/api/auth/version
curl https://api.gym-platform.com/api/training/version
curl https://api.gym-platform.com/api/tracking/version
curl https://api.gym-platform.com/api/notifications/version
```

### Supported Versions

Maintain support matrix for bug fixes:

```
Version  | Release Date | End of Support | Status
---------|--------------|----------------|--------
1.0.0    | 2026-03-21   | 2027-09-21     | Active
1.1.0    | 2026-04-15   | 2027-10-15     | Active
1.2.0    | 2026-05-20   | 2027-11-20     | Active (Latest)
```

**Policy:**
- Active Support: 18 months from release
- Bug fixes for last 3 minor versions
- Security patches for last 2 minor versions
- Security patches only for oldest version (no new features)

---

## Breaking Changes & Migration

### Announcing Breaking Changes

Major version releases may include breaking changes. Clear communication is essential.

#### Step 1: Announce in Previous Minor Version

In v1.x.0-beta, announce upcoming breaking change in v2.0.0:

```markdown
## [1.x.0-beta] - 2026-XX-XX

### Deprecated
- Endpoint `GET /api/training/workouts` is deprecated and will be removed in v2.0.0
  Use `GET /api/training/users/{id}/workouts` instead
```

#### Step 2: Provide Migration Guide

Create dedicated migration document:

**File: MIGRATION_v2.0.0.md**

```markdown
# Migration Guide: v1.x to v2.0.0

## Breaking Changes

### 1. Workout Endpoints Changed

#### Before (v1.x):
```
GET /api/training/workouts
GET /api/training/workouts/{id}
POST /api/training/workouts
```

#### After (v2.0.0):
```
GET /api/training/users/{id}/workouts
GET /api/training/users/{id}/workouts/{workout_id}
POST /api/training/users/{id}/workouts
```

#### Migration Steps:
1. Replace all calls to `/api/training/workouts` with `/api/training/users/{user_id}/workouts`
2. Update workload identifiers to include user_id in path
3. Test thoroughly in staging
4. Deploy with v2.0.0

### 2. Authentication Header Format Changed

#### Before (v1.x):
```
Authorization: Bearer <token>
```

#### After (v2.0.0):
```
X-Auth-Token: <token>
Authorization: Bearer <token>  (deprecated but still supported)
```

#### Migration Steps:
1. Update all API clients to use `X-Auth-Token` header
2. Test new format in staging
3. Deploy changes
4. Monitor logs for any remaining Bearer token usage

### Timeline
- v1.3.0: Both formats supported (Bearer token + X-Auth-Token)
- v2.0.0: X-Auth-Token required, Bearer token support removed
```

#### Step 3: Communicate Timeline

```markdown
## Deprecation Timeline

- **v1.3.0** (March 2026): Old endpoint marked deprecated, new format supported
- **v2.0.0** (June 2026): Old endpoint removed, migration required
- **Support**: Customers have 3 months to migrate (March - June)

### Getting Help
- See MIGRATION_v2.0.0.md for step-by-step instructions
- Post questions in #api-support Slack channel
- Review example implementations in /examples
```

---

## References

- [Semantic Versioning Specification](https://semver.org/)
- [Keep a Changelog Standard](https://keepachangelog.com/)
- [GitHub Release Management](https://docs.github.com/en/repositories/releasing-projects-on-github)
- [Git Tagging](https://git-scm.com/book/en/v2/Git-Basics-Tagging)
