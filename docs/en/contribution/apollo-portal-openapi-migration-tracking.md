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
| OpenAPI contract | `apollo-portal` points to the released `apollo-openapi` `v0.3.4` tag after the final Portal UI management slice | Portal implementation, generated interfaces, and SDKs can drift | Run compatibility checks before changing the spec URL, and pin a clear tag or commit |
| Frontend calls | See the [frontend URL migration inventory](./apollo-portal-openapi-frontend-url-inventory.md): all 131 tracked frontend API URL entries now call OpenAPI, with 0 WebAPI entries and 0 prefix-path gaps | The remaining risk is regression in upload/download, SSO, permissions, and response shape behavior rather than URL coverage | Keep the inventory in CI-style validation until full UT and portal UI e2e pass |
| Authentication | `/openapi/**` first detects Portal sessions, then falls back to consumer token auth | Custom SSO integrations may return 401 if `/openapi/**` does not share the Portal login context | Document filter order and SSO requirements; add regression coverage |
| Authorization | `UnifiedPermissionValidator` dispatches by `USER` or `CONSUMER` | OpenAPI read behavior can differ from `configView.memberOnly.envs` | Keep token compatibility first, then add explicit read-permission policy |
| Models | Generated models, legacy `apollo-openapi` Java DTO/API classes, and Portal DTOs coexist | Maintaining three model layers increases conversion cost | Prefer generated `*ManagementApi` and `model.*` for new endpoints |

## Current Progress

- Phase 0 baseline is in place: migration tracking, frontend URL inventory, the OpenAPI compatibility checker, and the PR workflow have been added.
- Frontend prefix path gaps are currently zero: `ClusterService.js`, `ExportService.js`, and `NamespaceLockService.js` now consistently use `AppUtil.prefixPath()`.
- OpenAPI authentication flow is guarded by tests: the order and `/openapi/*` patterns for `PortalUserSessionFilter`, `ConsumerAuthenticationFilter`, and `UserTypeResolverFilter` are locked.
- `UserTypeResolverFilter` tests now cover the production implementation instead of a test classpath shadow class with the same fully qualified name.
- `UnifiedPermissionValidator` USER/CONSUMER dispatch coverage now includes namespace, application, hide-config, and create/delete related permission entry points.
- The App frontend domain now runs through OpenAPI: query, by-self, navtree/env-cluster info, load, create, update, delete, missing env, missing namespace, app-master role, and create-application role calls all use `/openapi/v1/...`. `ownerDisplayName` is enriched server-side for Portal UI compatibility.
- `/openapi/v1/apps/by-self` now preserves Portal USER semantics: Portal cookie requests reuse the old WebAPI user-role appId resolution, while token requests continue to use consumer-authorized appIds.
- `apollo-openapi` `v0.3.4` is now the current adaptation target. Compared with `v0.1.0`, the `v0.3.x` line adds Release/Branch/Instance/Permission/AccessKey operations, Portal USER management operations, removes or renames several `v0.1.0` paths, and tightens generated return types such as App create/update/delete, cluster delete, env cluster info, and missing envs.
- The current adaptation pass points the default POM URL to `apollo-openapi` `v0.3.4`. Remaining `v0.1.0` differences from the compatibility checker must stay documented as explicit compatibility exceptions or be handled by future aliases; they are not silently compatible.
- The already-migrated App frontend calls now follow the latest spec paths: `load_navtree` calls `/openapi/v1/apps/{appId}/env-cluster-info`, and `find_miss_envs` calls `/openapi/v1/apps/{appId}/miss-envs`. `AppService.js` normalizes the new array responses back into the legacy `entities/body` shape consumed by `AppUtil.collectData`.
- The legacy App OpenAPI paths `/openapi/v1/apps/{appId}/navtree` and `/openapi/v1/apps/{appId}/miss_envs` were published in `v0.1.0` but are confirmed unused. This pass treats them as explicit compatibility exceptions and does not keep aliases; do not generalize this exception to other published `v0.1.0` paths.
- The Namespace Core slice now runs through OpenAPI: namespace reads, associated public namespace reads, namespace lock lookup, missing namespace lookup/create, cluster delete, AppNamespace create/delete/load/list, namespace create/delete, release status, usage, and public namespace instances. Backend routes implement the generated management interfaces where the `v0.3.0` contract already exists, and Portal UI response adapters stay local to frontend services.
- Namespace and item `extendInfo` now carries the Portal-only view state needed by the UI, while text-mode item updates derive `namespaceId` server-side from the path so UI callers do not depend on legacy `baseInfo.id`.
- The Release/Branch/Instance slice now runs through OpenAPI: `ReleaseService.js`, `NamespaceBranchService.js`, and `InstanceService.js` all use `/openapi/v1/...`. `ReleaseController`, `NamespaceBranchController`, and `InstanceController` implement generated management interfaces, while small frontend adapters preserve instance `content`, instance count `{num}`, and release compare `{changes}` shapes.
- The released `v0.3.2` spec adds `toReleaseId` to rollback, relaxes branch delete/rule-update `operator` query parameters, and keeps release ids as `int64` so Portal USER requests can derive operators from the login session while token CONSUMER clients keep the existing payload/query operator behavior.
- The Permission/AccessKey slice now runs through OpenAPI: `PermissionService.js`, `SystemRoleService.js`, and `AccessKeyService.js` use `/openapi/v1/...`, and the frontend inventory script now scans direct `$http`, select2 `ajax.url`, and download URL calls outside `static/scripts/services`.
- The final Portal UI management pass adds Portal-session-only OpenAPI endpoints for user, consumer, audit, commit history, release history, page settings, favorites, global search, server config, system info, config import/export, app search, and namespace item import/export. The frontend inventory is now 131/131 OpenAPI entries.

## Migration Matrix

| Domain | WebAPI / frontend service | OpenAPI coverage | Migration strategy |
| --- | --- | --- | --- |
| Env / Organization | `EnvService.js`, `OrganizationService.js` | Basic read APIs exist | Keep OpenAPI paths and validate SSO plus prefix path |
| Cluster | `ClusterService.js` | get/create/delete exist | Frontend service now uses OpenAPI for create, read, and delete; keep USER/CONSUMER operator regression coverage |
| App | `AppService.js` | app query, create, update, delete, env cluster, missing env, missing namespace, and app-master role APIs exist | App frontend calls are migrated. Portal USER requests derive operators from the session where needed, and response normalization stays local to the frontend service |
| Namespace / AppNamespace / Lock | `NamespaceService.js`, `NamespaceLockService.js` | Namespace, AppNamespace, lock, usage, release status, and public instance APIs exist in `v0.3.0` | Namespace Core frontend service calls are migrated; continue with branch/release and any remaining response-shape hardening |
| Item | `ConfigService.js` | item CRUD, diff, sync, validation, revocation, and namespace read APIs are represented in the contract direction | Item and Namespace Core UI paths now use OpenAPI; continue observing key encoding, text mode, and broader e2e coverage |
| Release / Branch | `ReleaseService.js`, `NamespaceBranchService.js` | release, gray release, branch create/delete/merge/rules, compare, active releases, and rollback are covered in `v0.3.2` | Frontend service calls are migrated; keep dual-auth and Java client compatibility coverage |
| Instance | `InstanceService.js` | by-release, by-namespace, releases-not-in, and count APIs exist | Frontend service calls are migrated with local pagination/count adapters |
| Permission / AccessKey | `PermissionService.js`, `SystemRoleService.js`, `AccessKeyService.js` | Permission, system-role, and AccessKey contracts exist in `v0.3.3` | Frontend service calls are migrated; keep compatibility-first review for token callers |
| User / Consumer | `UserService.js`, `ConsumerService.js`, select2 user search directives | User and consumer Portal UI APIs are covered by Portal-session-only OpenAPI endpoints | Frontend calls are migrated; consumer token compatibility for existing public OpenAPI operations remains unchanged because these management endpoints require Portal USER sessions |
| Admin / Audit / Import / Export / Search / Favorites | `ServerConfigService.js`, `AuditLogService.js`, `CommonService.js`, `CommitService.js`, `ReleaseHistoryService.js`, `FavoriteService.js`, `GlobalSearchValueService.js`, `ConfigExportController.js`, import/export directives | Portal-session-only OpenAPI endpoints cover the remaining admin/read/download/upload surfaces | Frontend calls are migrated, including direct `$http`, select2 `ajax.url`, HEAD checks, and download links |

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

Running the compatibility checker from `v0.1.0` to `v0.3.0` already
shows incompatible changes that need explicit handling: several `operationId`
values changed, legacy paths such as `/openapi/v1/apps/{appId}/miss_envs`,
`/openapi/v1/apps/{appId}/navtree`, and item `batchUpdate`/`sync`/`validate`
paths are no longer present on `v0.3.0`, and old schemas such as `MultiResponseEntity`
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
