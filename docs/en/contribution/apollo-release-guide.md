# Apollo Release Guide (Skill-driven)

This guide is for teams using Codex / Claude Code to trigger release skills via natural language.
The goal is to minimize manual operations, standardize release quality, and keep human confirmation before critical external actions.

## 1. Scope and recommended order

The Apollo release process is covered by three skills:

1. `apollo-java-release` for `apolloconfig/apollo-java`
2. `apollo-release` for `apolloconfig/apollo`
3. `apollo-helm-chart-release` for `apolloconfig/apollo-helm-chart`

Recommended order:

1. Release `apollo-java-release` first (no dependency on other release flows)
2. Release `apollo-release` second (usually depends on the new Java SDK version)
3. Release `apollo-helm-chart-release` last

> If a release does not involve one repository, you can skip that sub-flow.

## 2. Preparation

### 2.1 Permissions and tools

- GitHub permissions for PR, Release, Workflow, Discussion, and Milestone operations
- Required local commands: `git`, `gh`, `python3`, `jq`
- Additional command for Helm chart release: `helm`

### 2.2 Repository and branch state

- Keep each target repository clean before starting
- Ensure base branches are up to date (`master` for `apollo`, default branch for other repos)

### 2.3 Release inputs to prepare

- Release version (for example `2.8.0`)
- Next development version (for example `2.9.0-SNAPSHOT`)
- Highlights PR list (for example `PR_ID_1,PR_ID_2,PR_ID_3`)

### 2.4 Install release skills (Codex / Claude Code)

Before using the flows in this guide, install these three skills first:

- `apollo-java-release`
- `apollo-release`
- `apollo-helm-chart-release`

Recommended approach (natural language):

1. Use `skill-installer` in a Codex session
2. Set the source to the full GitHub URL: `https://github.com/apolloconfig/apollo-skills`
3. Install the three skills above

Example natural-language request:

- “Use `skill-installer` to install `apollo-java-release`, `apollo-release`, and `apollo-helm-chart-release` from `https://github.com/apolloconfig/apollo-skills`.”

If you prefer manual setup, place these skill folders into your local skill directory (usually `$CODEX_HOME/skills` or `~/.codex/skills`) and restart the client.

## 3. Release Apollo Java (`apollo-java-release`)

### 3.1 How to trigger (natural language)

In the `apollo-java` workspace session, ask with a prompt like:

- "Use `apollo-java-release` to publish `X.Y.Z`, next version `A.B.C-SNAPSHOT`, and use these PRs for highlights: `...`"

### 3.2 What the skill automates

- Version bump PR (`revision` from SNAPSHOT to release)
- Prerelease creation
- Trigger `release.yml` and wait for Sonatype Central publish completion
- Announcement discussion publishing
- Post-release snapshot bump and post-release PR
- Prerelease promotion to official release

### 3.3 Checkpoint interaction model

- The skill pauses automatically before critical external actions and asks whether to continue
- You respond in chat to continue, or request edits before proceeding

## 4. Release Apollo Server (`apollo-release`)

### 4.1 How to trigger (natural language)

In the `apollo` workspace session, ask with a prompt like:

- "Use `apollo-release` to publish `X.Y.Z`, next version `A.B.C-SNAPSHOT`, with highlights PRs `...`"

### 4.2 What the skill automates

- Version bump PR (`pom.xml` `revision`)
- Release notes and announcement draft generation from `CHANGES.md`
- Prerelease creation (`vX.Y.Z`)
- Package build + checksum upload via GitHub Actions
- Docker publish workflow trigger
- Prerelease promotion to official release
- Announcement discussion publishing
- Post-release snapshot bump, `CHANGES.md` archive, milestone updates, and post-release PR

### 4.3 Checkpoint interaction model

Same model as `apollo-java-release`:

- System prompts appear at checkpoints
- You can continue or request adjustments (highlights, notes, parameters)

## 5. Release Helm Charts (`apollo-helm-chart-release`)

### 5.1 How to trigger (natural language)

In the `apollo-helm-chart` workspace session, ask with a prompt like:

- "Use `apollo-helm-chart-release` to publish the current chart version changes"

### 5.2 What the skill automates

- Chart version change detection and consistency checks
- `helm lint`, `helm package`, and `helm repo index`
- File whitelist checks to prevent accidental commits
- Branch and commit draft generation
- Pause before push / PR for explicit confirmation (no auto-push/no auto-PR by default)

## 6. Unified post-release verification

At minimum, verify:

1. Apollo release page includes 3 zip files and 3 sha1 files
2. Docker image tags are available
3. `apollo-java` artifacts are available in Maven Central
4. Helm repository `docs/index.yaml` includes the new chart versions
5. Core smoke tests pass (config publish, gray release, client fetch, portal core flows)

## 7. Common operations (skill usage perspective)

### 7.1 Resume after interruption

Ask directly in chat, for example:

- "Continue the previous release flow"
- "Resume from the next checkpoint"

The skill restores from state and does not repeat completed steps.

### 7.2 Dry run first

Request a dry run first, for example:

- "Run `apollo-release` in dry-run mode first so I can review the plan"

Then request the real run after confirmation.

### 7.3 Adjust highlights / wording

Before prerelease creation, ask for adjustments, for example:

- "Use these PRs for highlights: `...`, then regenerate release notes"

After review, continue to the next checkpoint.
