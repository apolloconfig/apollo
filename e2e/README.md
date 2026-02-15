# Apollo E2E Tests

This directory contains end-to-end (E2E) UI tests and related CI entrypoints.

## Test Suites

### Portal UI E2E

- Location: `e2e/portal-e2e`
- Runtime: Playwright + Chromium
- Tags:
  - `@smoke`: core user journeys
  - `@regression`: extended scenarios
- CI runs both tags together.

Current cases:

1. `login flow works @smoke`
2. `create app flow works @smoke`
3. `create item and first release works @smoke`
4. `update item and second release works @smoke`
5. `rollback latest release works @smoke`
6. `release history contains publish and rollback records @smoke`
7. `duplicate app creation is rejected @regression`
8. `cluster and namespace pages support creation flow @regression`
9. `config export and instance view paths are reachable @regression`
10. `published, gray published and rolled back configs are readable from config service @regression`
11. `properties, yaml and json namespaces are readable from config service @regression`
12. `namespace role page supports grant and revoke operations @regression`
13. `text mode edit and publish are readable from config service @regression`
14. `linked public namespace supports association and override @regression`
15. `grayscale ui supports create rule publish merge and discard @regression`

High-priority user-guide coverage (via `portal-priority.spec.js`):

1. Namespace permission management (grant/revoke role in namespace role page).
2. Text-mode editing path (switch to text, submit edits, publish, verify from Config Service).
3. Public namespace association and override in linked namespace.
4. Grayscale UI chain: create branch, maintain rules, gray publish, merge-to-master publish, and discard branch.

Config Service full-chain coverage (via `portal-configservice.spec.js`):

Covered controllers:

1. `ConfigController` (`/configs/**`)
2. `ConfigFileController` (`/configfiles/**`)
3. `NotificationControllerV2` (`/notifications/v2`)

Covered behaviors:

1. Normal publish result is readable from Config Service.
2. Gray release result is readable and isolated by client IP.
3. Rollback result is readable from Config Service after rollback.
4. Namespace formats `properties`, `yaml`, and `json` are all verifiable via Config Service APIs.
5. Notification polling returns namespace updates with increasing notification IDs after publish/gray/rollback.

## Local Run

```bash
cd e2e/portal-e2e
npm ci
npx playwright install --with-deps chromium
BASE_URL=http://127.0.0.1:8070 npm run test:e2e
```

CI mode command:

```bash
cd e2e/portal-e2e
BASE_URL=http://127.0.0.1:8070 npm run test:e2e:ci
```

Run only Config Service full-chain regression:

```bash
cd e2e/portal-e2e
BASE_URL=http://127.0.0.1:8070 npm run test:e2e:ci -- tests/portal-configservice.spec.js
```

## CI Workflow

- Workflow file: `.github/workflows/portal-ui-e2e.yml`
- Job/check name: `portal-ui-e2e`
- PR trigger paths:
  - `apollo-portal/**`
  - `apollo-assembly/**`
  - `e2e/portal-e2e/**`
  - `scripts/sql/**`
  - `.github/workflows/portal-ui-e2e.yml`

## Maintenance Notes

- Prefer stable selectors (`id`, stable attributes, deterministic CSS) over UI text.
- Test data should use unique app ids to avoid collisions.
- Keep assertions focused on behavior: URL transition, API response status, and visible success/failure signals.
