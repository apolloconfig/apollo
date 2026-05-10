# Apollo Portal OpenAPI Migration Tracking (Temporary)

This document tracks the baseline, risks, and progress while Apollo Portal
migrates WebAPI usage toward OpenAPI. It is not a permanent design document.
After the migration is complete and the legacy WebAPI policy is clear, delete
this document and keep only stable OpenAPI contracts and contributor guidance.

## Background and Goal

Apollo Portal has historically maintained two API surfaces: WebAPI for the browser
UI and OpenAPI for third-party integrations. This creates duplicated work for UI,
SDK, CLI, and future MCP use cases, and it makes authentication, authorization,
and DTO evolution easier to drift.

The migration goal is to establish a sustainable migration path. `apolloconfig/apollo-openapi`
is the contract source of truth, while `apollo-portal` gradually implements the same
OpenAPI surface without breaking existing OpenAPI v1 token clients or regressing
Portal UI behavior.

## Current Baseline

| Area | Current state | Risk | Next step |
| --- | --- | --- | --- |
| OpenAPI contract | `apollo-portal` points to the `apollo-openapi` `v0.2.0` tag; future `apollo-openapi/main` changes may still move further | Portal implementation, generated interfaces, and SDKs can drift | Run compatibility checks before changing the spec URL, and pin a clear tag or commit |
| Frontend calls | See the [frontend URL migration inventory](./apollo-portal-openapi-frontend-url-inventory.md): 9 of the current 121 URL entries call OpenAPI, and 112 still call WebAPI | One-off migrations miss prefix path, SSO, permissions, and response shape details | Migrate by domain after backend dual-auth validation |
| Authentication | `/openapi/**` first detects Portal sessions, then falls back to consumer token auth | Custom SSO integrations may return 401 if `/openapi/**` does not share the Portal login context | Document filter order and SSO requirements; add regression coverage |
| Authorization | `UnifiedPermissionValidator` dispatches by `USER` or `CONSUMER` | OpenAPI read behavior can differ from `configView.memberOnly.envs` | Keep token compatibility first, then add explicit read-permission policy |
| Models | Generated models, legacy `apollo-openapi` Java DTO/API classes, and Portal DTOs coexist | Maintaining three model layers increases conversion cost | Prefer generated `*ManagementApi` and `model.*` for new endpoints |

## Current Progress

- Phase 0 baseline is in place: migration tracking, frontend URL inventory, the OpenAPI compatibility checker, and the PR workflow have been added.
- Frontend prefix path gaps are currently zero: `ClusterService.js`, `ExportService.js`, and `NamespaceLockService.js` now consistently use `AppUtil.prefixPath()`.
- OpenAPI authentication flow is guarded by tests: the order and `/openapi/*` patterns for `PortalUserSessionFilter`, `ConsumerAuthenticationFilter`, and `UserTypeResolverFilter` are locked.
- `UserTypeResolverFilter` tests now cover the production implementation instead of a test classpath shadow class with the same fully qualified name.
- `UnifiedPermissionValidator` USER/CONSUMER dispatch coverage now includes namespace, application, hide-config, and create/delete related permission entry points.
- The first App read-only frontend slice has moved to OpenAPI: `find_apps`, `find_app_by_self`, `load_navtree`, and `find_miss_envs`. `load_app` stays on WebAPI in the latest-spec adaptation pass to keep the PR focused; it can move in the next App-domain slice now that the latest local contract exposes `ownerDisplayName`.
- `/openapi/v1/apps/by-self` now preserves Portal USER semantics: Portal cookie requests reuse the old WebAPI user-role appId resolution, while token requests continue to use consumer-authorized appIds.
- `apollo-openapi` `v0.2.0` is now the current adaptation target. Compared with `v0.1.0`, it adds 50 operations, removes or renames several `v0.1.0` paths, and tightens generated return types such as App create/update/delete, cluster delete, env cluster info, and missing envs.
- This adaptation pass now points the default POM URL to `apollo-openapi` `v0.2.0` and passes Portal compile, full unit tests, and Portal e2e without a local spec override. Remaining `v0.1.0` differences from the compatibility checker must stay documented as explicit compatibility exceptions or be handled by future aliases; they are not silently compatible.
- The already-migrated App frontend calls now follow the latest spec paths: `load_navtree` calls `/openapi/v1/apps/{appId}/env-cluster-info`, and `find_miss_envs` calls `/openapi/v1/apps/{appId}/miss-envs`. `AppService.js` normalizes the new array responses back into the legacy `entities/body` shape consumed by `AppUtil.collectData`.
- The legacy App OpenAPI paths `/openapi/v1/apps/{appId}/navtree` and `/openapi/v1/apps/{appId}/miss_envs` were published in `v0.1.0` but are confirmed unused. This pass treats them as explicit compatibility exceptions and does not keep aliases; do not generalize this exception to other published `v0.1.0` paths.

## Migration Matrix

| Domain | WebAPI / frontend service | OpenAPI coverage | Migration strategy |
| --- | --- | --- | --- |
| Env / Organization | `EnvService.js`, `OrganizationService.js` | Basic read APIs exist | Keep OpenAPI paths and validate SSO plus prefix path |
| Cluster | `ClusterService.js` | get/create/delete exist | Use `AppUtil.prefixPath()` consistently; validate operator and USER/CONSUMER semantics |
| App | `AppService.js` | app query, create, update, delete, env cluster, and missing env APIs exist | Read-only endpoints are partially migrated; latest spec paths are used for nav tree and missing envs with frontend response normalization. Write endpoints still need operator contract/Portal user semantics alignment before UI migration |
| Namespace / AppNamespace / Lock | `NamespaceService.js`, `NamespaceLockService.js` | Partial spec exists on `apollo-openapi/main` | Migrate read and lock paths first, then create/delete |
| Item | `ConfigService.js` | item CRUD, diff, sync, validation, and revocation are represented in the contract direction | Confirm key encoding and text-mode behavior before UI migration |
| Release / Branch | `ReleaseService.js`, `NamespaceBranchService.js` | release, gray release, merge, and rollback are partially covered | Add dual-auth tests for branch and rollback flows |
| Instance | `InstanceService.js` | Several read APIs exist | Migrate as read-only while preserving pagination and response shape |
| Permission / AccessKey | `PermissionService.js`, `AccessKeyService.js` | New contracts exist on `apollo-openapi/main` | Validate the permission model before migrating management UI |
| User / Consumer / System | `UserService.js`, `ConsumerService.js`, `SystemRoleService.js` | User APIs still have open PRs | Keep out of the first migration wave until the contract stabilizes |
| Admin / Audit / Import / Export | `ServerConfigService.js`, `AuditLogService.js`, import/export services | OpenAPI coverage is incomplete | Design separately to avoid mixing admin and config surfaces |

## Contract Rules

OpenAPI v1 is the public compatibility surface for third-party clients, SDKs,
CLI, and future MCP/Agent tooling. The rules below apply only to published
OpenAPI contracts. They do not apply to internal WebAPI paths between Portal
frontend JavaScript and the Portal backend, because those paths move in the same
repository and release as long as both sides are updated together.

For existing OpenAPI clients, the following changes are breaking by default:

- removing an existing path or HTTP method;
- changing an existing `operationId`;
- removing an existing schema;
- adding a new required field to an existing schema.

If a published OpenAPI path should be replaced by a more RESTful path, the
legacy path must stay as an alias or the change must wait for v2 unless the path
is explicitly confirmed unused and documented as a compatibility exception. New
paths, optional fields, schemas, and operations are compatible by default. Portal
UI-only paths can migrate by domain and do not need compatibility aliases for
old JavaScript URLs.

Running the compatibility checker from `v0.1.0` to `v0.2.0` already
shows incompatible changes that need explicit handling: several `operationId`
values changed, legacy paths such as `/openapi/v1/apps/{appId}/miss_envs`,
`/openapi/v1/apps/{appId}/navtree`, and item `batchUpdate`/`sync`/`validate`
paths are no longer present on `v0.2.0`, and old schemas such as `MultiResponseEntity`
and `RichResponseEntity` were removed. Therefore later work must not treat the
failed check as naturally resolved; except for the confirmed-unused legacy App
paths, published contract differences still need aliases in `apollo-openapi` or
explicit compatibility exceptions.

## Authentication and Authorization

Portal UI requests to `/openapi/**` are still Portal user actions and must reuse
the original WebAPI user permissions. Third-party token requests continue to use
consumer roles. `UserIdentityContextHolder` identifies the request source, and
`UnifiedPermissionValidator` dispatches authorization accordingly.

OpenAPI token read APIs keep the historical compatibility behavior by default.
If `configView.memberOnly.envs` is enabled, Apollo should add an explicit setting
or release-note-backed policy before making token read APIs follow the same rule,
so existing clients are not silently broken.

## Rollout Order

1. Establish the baseline: commit this document, the frontend coverage matrix, and the OpenAPI compatibility checker.
2. Stabilize the platform layer: verify spec versioning, prefix path handling, SSO, USER/CONSUMER permission dispatch, and generated-source compilation.
3. Migrate by domain: add backend OpenAPI capability and dual-auth tests before changing each frontend service.
4. Add governance: include compatibility checks, portal compile, controller coverage, and SDK generation in the release flow.
5. Build downstream tooling: prioritize CLI after OpenAPI stabilizes; handle MCP/Agent features after the finer security model is designed.

## Verification Gates

- `python3 scripts/openapi/check_openapi_compatibility_test.py`
- `python3 scripts/openapi/check_openapi_compatibility.py --base <old-spec> --head <new-spec>`
- `./mvnw -pl apollo-portal -am -DskipTests compile`
- Per-domain `MockMvc` tests for Portal cookie, OpenAPI token, unauthenticated, and forbidden requests
- Frontend smoke tests for the corresponding UI path, error handling, expired sessions, and prefix-path deployments
