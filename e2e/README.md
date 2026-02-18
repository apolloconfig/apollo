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

### Portal Auth Matrix E2E

- Location: `e2e/portal-e2e`
- Runtime: Playwright + Chromium + Dockerized auth providers
- Tag:
  - `@auth-matrix`: auth matrix scenarios (runs separately from `@smoke|@regression`)
- Modes:
  - `ldap`: OpenLDAP + group filter (`memberUid`) login checks
  - `oidc`: Keycloak OIDC interactive login checks

Covered behaviors:

1. Login success in the target auth mode.
2. Login failures:
   - non-existent user
   - wrong password
   - LDAP-only: blocked user rejected by group filter
3. Post-login user search for app creation:
   - user can be searched from auth provider-backed user source
   - selected option contains user id/name/email information
4. Namespace role page grant/revoke:
   - assign role after real Select2 user search
   - revoke assigned role successfully

Notes:

- LDAP e2e config uses `camelCase` keys under `ldap.mapping` and `ldap.group` (for example `objectClass`, `loginId`, `groupSearch`) to match the runtime placeholders.
- OIDC e2e fixture pre-creates users with `firstName`, `lastName`, and `emailVerified=true` to avoid Keycloak `VERIFY_PROFILE` redirect during first login.
- OIDC secondary user is warmed up once before assertions so Apollo local user search can find it (`OidcLocalUserService` behavior).

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

Run Portal auth matrix in LDAP mode:

```bash
cd /path/to/apollo
./e2e/portal-e2e/scripts/auth/setup-ldap.sh

SPRING_PROFILES_ACTIVE=github,database-discovery,ldap \
SPRING_SQL_CONFIG_INIT_MODE=always \
SPRING_SQL_PORTAL_INIT_MODE=always \
SPRING_CONFIG_DATASOURCE_URL="jdbc:h2:mem:apollo-config-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE" \
SPRING_PORTAL_DATASOURCE_URL="jdbc:h2:mem:apollo-portal-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE" \
SPRING_CONFIG_ADDITIONAL_LOCATION="file:/path/to/apollo/e2e/portal-e2e/config/application-ldap-e2e.yml" \
java -jar /path/to/apollo/apollo-assembly/target/apollo-assembly-*.jar

cd e2e/portal-e2e
npm ci
npx playwright install --with-deps chromium
PORTAL_AUTH_MODE=ldap BASE_URL=http://127.0.0.1:8070 npm run test:e2e:auth-matrix

cd /path/to/apollo
./e2e/portal-e2e/scripts/auth/teardown-auth.sh
```

Run Portal auth matrix in OIDC mode:

```bash
cd /path/to/apollo
./e2e/portal-e2e/scripts/auth/setup-oidc.sh

SPRING_PROFILES_ACTIVE=github,database-discovery,oidc \
SPRING_SQL_CONFIG_INIT_MODE=always \
SPRING_SQL_PORTAL_INIT_MODE=always \
SPRING_CONFIG_DATASOURCE_URL="jdbc:h2:mem:apollo-config-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE" \
SPRING_PORTAL_DATASOURCE_URL="jdbc:h2:mem:apollo-portal-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE" \
SPRING_CONFIG_ADDITIONAL_LOCATION="file:/path/to/apollo/e2e/portal-e2e/config/application-oidc-e2e.yml" \
java -jar /path/to/apollo/apollo-assembly/target/apollo-assembly-*.jar

cd e2e/portal-e2e
npm ci
npx playwright install --with-deps chromium
PORTAL_AUTH_MODE=oidc BASE_URL=http://127.0.0.1:8070 npm run test:e2e:auth-matrix

cd /path/to/apollo
./e2e/portal-e2e/scripts/auth/teardown-auth.sh
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

Portal auth matrix workflow:

- Workflow file: `.github/workflows/portal-login-e2e.yml`
- Job/check name: `portal-login-e2e (ldap|oidc)`
- Matrix:
  - `ldap`
  - `oidc`
- PR trigger paths:
  - `apollo-portal/**`
  - `apollo-assembly/**`
  - `e2e/portal-e2e/**`
  - `.github/workflows/portal-login-e2e.yml`

## Maintenance Notes

- Prefer stable selectors (`id`, stable attributes, deterministic CSS) over UI text.
- Test data should use unique app ids to avoid collisions.
- Keep assertions focused on behavior: URL transition, API response status, and visible success/failure signals.
