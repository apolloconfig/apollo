# Repository Guidelines

## Project Structure & Module Organization
- Multi-module Maven repo with root `pom.xml` and service modules like `apollo-configservice`, `apollo-adminservice`, and `apollo-portal`, shared libs in `apollo-common`, and packaging in `apollo-assembly`.
- `apollo-biz` is a shared business-logic module used by services like config/admin (not a standalone service).
- Build and release tooling lives in `scripts/` (e.g., `scripts/build.sh`) and `apollo-buildtools/` (code style configs).
- Database and schema assets are under `scripts/sql` and module `src/main/resources` folders.
- Tests follow standard Maven layout: `*/src/test/java` and `*/src/test/resources` inside each module.
- Documentation is in `docs/`.

## Build, Test, and Development Commands
- `./mvnw -DskipTests package` builds all modules.
- `./mvnw test` runs the full test suite via Surefire (may log warnings if local meta/admin services are not running).
- `./mvnw -pl apollo-configservice -am test` runs tests for a specific module and its dependencies.
- `./mvnw spotless:apply` formats code and must be run before opening a PR.
- `./scripts/build.sh` generates distributable packages (used for deployment workflows).

## Coding Style & Naming Conventions
- Follow Google Java Style Guide; 2-space indentation and standard Java conventions apply.
- Use the IDE configs in `apollo-buildtools/style/` (IntelliJ/Eclipse).
- New Java classes should include a short Javadoc describing the class purpose.
- Use standard Java naming: packages `lower.case`, classes `UpperCamelCase`, tests `*Test`.

## Testing Guidelines
- JUnit 5 is the default, with Vintage enabled for legacy JUnit 4 tests.
- Put new tests under the module’s `src/test/java` with `*Test` suffix.
- Add unit tests for new features or important bug fixes.

## Commit & Pull Request Guidelines
- Use Conventional Commits format (e.g., `feat:`, `fix:`).
- If a commit fixes an issue, append `Fixes #123` in the commit message.
- Rebase onto `master` and squash feature work into a single commit before merge.
- When merging a PR on GitHub: if it has a single commit, use rebase and merge; if it has multiple commits, use squash and merge.
- Non-trivial contributions require signing the CLA.
- Open a feature branch for your change and submit a PR using `.github/PULL_REQUEST_TEMPLATE.md`.
- PRs should include a clear description, tests run, and any relevant screenshots/logs; use GitHub issues for tracking.
- The PR checklist expects `mvn clean test`, `mvn spotless:apply`, and an update to `CHANGES.md`.
- For upstream contributions, open PRs against `apolloconfig/apollo` and fill the template with real content (not the raw template text).

## Security & Configuration Notes
- Apollo supports H2 in-memory for local development and MySQL for production; prefer H2 locally unless you need a real database.
- Review `scripts/sql` and environment config when using MySQL.
- Do not commit secrets or environment-specific credentials; use local overrides instead.
- Follow `SECURITY.md` for vulnerability reporting.
