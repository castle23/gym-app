# Skill: Refactoring

## Description
Safely refactor Java code guided by tests, ensuring no behavior changes and maintaining coverage.

## Prerequisites
- Tests exist and pass for all affected code
- Current test coverage is known (run `mvn jacoco:report -pl [service]`)
- Working on a clean git branch (no uncommitted changes)

## Steps

1. **Identify Code Smell**
   - Long method (> 50 lines)
   - God class (too many responsibilities)
   - Duplicate code across classes
   - Feature envy (method uses another class's data more than its own)
   - Primitive obsession (raw types instead of value objects)
   - Deep nesting (> 3 levels)
   - Unclear naming

2. **Run Existing Tests (Must Pass)**
   ```bash
   mvn test -pl [service]
   ```
   - If tests fail, fix them FIRST — do not refactor broken code
   - Record current coverage percentage

3. **Plan Minimal Changes**
   - Define the specific refactoring to apply
   - List all files that will change
   - Ensure each change is small and reversible
   - Plan the order of changes to keep tests passing at each step

4. **Execute Refactoring**
   - Make one logical change at a time
   - Keep commits granular (one refactoring per commit)
   - Do NOT change behavior — only structure
   - Do NOT add features or fix bugs during refactoring

5. **Run Tests Again (Must Pass)**
   ```bash
   mvn test -pl [service]
   ```
   - Every test that passed before must still pass
   - If any test fails, revert and re-examine the change

6. **Verify Coverage**
   ```bash
   mvn jacoco:report -pl [service]
   ```
   - Coverage must remain >= 85%
   - If coverage dropped, understand why and add tests if needed

## Common Refactorings

| Smell                  | Refactoring             | Example                                  |
|------------------------|-------------------------|------------------------------------------|
| Long method            | Extract method          | Pull conditional block into named method |
| Duplicate code         | Extract shared method   | Move to utility or base class            |
| God class              | Extract service         | Split into focused `@Service` classes    |
| Unclear naming         | Rename                  | `process()` → `calculateMonthlyStats()` |
| Complex conditional    | Replace with polymorphism | Strategy or template method pattern    |
| Deep nesting           | Early return / guard clause | Invert `if` and return early          |
| Primitive obsession    | Introduce value object  | `String email` → `Email email`           |
| Feature envy           | Move method             | Move logic to the class that owns data   |

## Constraints

- **NEVER refactor without test coverage.** If coverage is insufficient:
  1. Write tests first (use `ai/skills/test-generation.md`)
  2. Verify tests pass
  3. THEN refactor
- **NEVER combine refactoring with behavior changes** in the same commit
- **NEVER refactor code you don't understand** — read and analyze first
- **Keep refactoring scope small** — one smell per pass

## Red Flags — Stop and Reassess
- Tests start failing after a change → revert immediately
- Coverage drops below 85% → add missing tests
- Change touches more than 5 files → break into smaller steps
- You need to change test assertions → you're changing behavior, not refactoring

## Output
- Refactored code with identical behavior
- All tests passing: `mvn test -pl [service]`
- Coverage >= 85%: `mvn jacoco:report -pl [service]`
- Clean commit: `refactor(scope): description`

## References
- `ai/rules/coding-standards.md`
- `ai/rules/testing-standards.md`
- `ai/skills/test-generation.md` — for writing missing tests before refactoring
- `ai/skills/code-analysis.md` — for identifying code smells
