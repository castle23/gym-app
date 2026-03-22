# Task: Code Refactoring

## Prerequisites
- [ ] All tests passing for the target service
- [ ] On a clean git branch (no uncommitted changes)
- [ ] Current coverage known and >= 85%
- [ ] Code smell or improvement area identified

## Workflow

### 1. Identify Scope
- Determine the file(s) or module to refactor
- Name the specific code smell (see `ai/skills/refactoring.md`)
- Confirm the refactoring type (extract method, rename, extract service, etc.)

### 2. Run Tests — Baseline
```bash
mvn test -pl [service]
```
- All tests must pass before any changes
- If tests fail, fix them first — this is not a refactoring task

### 3. Check Coverage — Baseline
```bash
mvn jacoco:report -pl [service]
```
- Record the current coverage percentage
- If coverage < 85% on affected code, write tests first (`ai/skills/test-generation.md`)

### 4. Refactor
- Apply the planned refactoring (one logical change at a time)
- Do NOT change behavior — only improve structure
- Do NOT add features or fix bugs in the same change
- Reference `ai/skills/refactoring.md` for patterns

### 5. Run Tests — Verification
```bash
mvn test -pl [service]
```
- Every previously passing test must still pass
- If any test fails: revert, re-examine, try a smaller change

### 6. Verify Coverage
```bash
mvn jacoco:report -pl [service]
```
- Coverage must remain >= 85%
- If it dropped, investigate and add tests as needed

### 7. Code Review
- Review changes against `ai/rules/coding-standards.md`
- Verify naming conventions, SOLID principles, method sizes
- Use `ai/skills/code-analysis.md` for a structured review

### 8. Commit
```bash
git add -A
git commit -m "refactor(scope): description of structural change"
```
- Scope = affected module or class name
- Description should say WHAT changed structurally, not WHY (that goes in PR body)

## Completion Checklist
- [ ] All tests pass after refactoring
- [ ] Coverage >= 85%
- [ ] No behavioral regressions
- [ ] No feature changes mixed in
- [ ] Commit follows `refactor(scope): description` format
- [ ] Changes reviewed against coding standards

## References
- `ai/skills/refactoring.md` — refactoring patterns and constraints
- `ai/skills/code-analysis.md` — identifying code smells
- `ai/skills/test-generation.md` — writing missing tests
- `ai/rules/coding-standards.md`
- `ai/rules/testing-standards.md`
