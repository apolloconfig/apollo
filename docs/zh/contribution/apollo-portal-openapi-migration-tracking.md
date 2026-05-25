# Apollo Portal OpenAPI 统一迁移跟踪（临时）

本文档用于跟踪 Apollo Portal WebAPI 向 OpenAPI 统一迁移期间的基线、风险和进展。
它不是长期设计文档；迁移完成、旧 WebAPI 策略明确后，应删除本文档，只保留稳定的 OpenAPI
契约和贡献指南。

## 背景与目标

Apollo Portal 历史上同时维护面向浏览器的 WebAPI 和面向第三方系统的
OpenAPI。这个双轨设计让 UI、SDK、CLI、MCP 等调用面都需要重复补齐能力，也让权限、
认证和 DTO 演进更容易出现偏差。

本迁移的目标是建立一条可持续迁移路径：以 `apolloconfig/apollo-openapi` 作为契约源，
让 `apollo-portal` 逐步实现同一套 OpenAPI，同时保证现有 OpenAPI v1 token 用户继续
兼容，Portal UI 功能完整且行为不回退。

## 当前基线

| 领域 | 当前状态 | 风险 | 下一步 |
| --- | --- | --- | --- |
| OpenAPI 契约 | `apollo-portal` 在最后一批 Portal UI 管理面切片后已引用 `apollo-openapi` 发布的 `v0.3.4` tag | Portal 实现、生成接口和 SDK 容易漂移 | 每次更新 spec URL 前运行兼容性检查，记录明确 tag 或 commit |
| 前端调用 | 见 [前端 URL 迁移清单](./apollo-portal-openapi-frontend-url-inventory.md)，当前 131 个前端 API URL 条目全部走 OpenAPI，WebAPI 条目为 0，no-prefix 条目为 0 | 剩余风险已经从 URL 覆盖转为上传/下载、SSO、权限和 response shape 回归 | 在完整 UT 和 Portal UI e2e 通过前，继续把清单作为验证项 |
| 认证 | `/openapi/**` 先经过 Portal session 识别，再走 consumer token 认证 | 自定义 SSO 若没有让 `/openapi/**` 复用 Portal 登录态，会出现 401 | 明确 filter 顺序和 SSO 接入要求，补回归测试 |
| 权限 | `UnifiedPermissionValidator` 已按 `USER`/`CONSUMER` 分发 | OpenAPI 读接口历史上较开放，与 `configView.memberOnly.envs` 可能不一致 | 先保持 token 兼容，新增可控策略对齐只读权限 |
| 模型 | 生成模型、`apollo-openapi` Java artifact 旧 DTO/API、Portal DTO 并存 | 长期维护三套模型会持续放大转换成本 | 新接口优先实现 generated `*ManagementApi` 和 `model.*` |

## 当前进展

- Phase 0 基线已建立：迁移跟踪文档、前端 URL 清单、OpenAPI 兼容性检查脚本和 PR workflow 已补齐。
- 前端 prefix path 基础问题已清零：`ClusterService.js`、`ExportService.js` 和 `NamespaceLockService.js` 已统一使用 `AppUtil.prefixPath()`；清单中 no-prefix 条目为 0。
- OpenAPI 认证链路已加测试保护：`PortalUserSessionFilter`、`ConsumerAuthenticationFilter`、`UserTypeResolverFilter` 的顺序和 `/openapi/*` pattern 已由测试锁定。
- `UserTypeResolverFilter` 测试已改为覆盖生产实现，避免测试 classpath 中的同名 shadow class 掩盖真实行为。
- `UnifiedPermissionValidator` 的 USER/CONSUMER 分发测试已扩展到 namespace、application、hide-config 和 create/delete 相关入口。
- App 前端域已全部切到 OpenAPI：查询、by-self、navtree/env-cluster info、load、create、update、delete、missing env、missing namespace、app-master role 和 create-application role 都使用 `/openapi/v1/...`。Portal UI 需要的 `ownerDisplayName` 由后端补齐。
- `/openapi/v1/apps/by-self` 已补齐 Portal USER 语义：Portal cookie 请求复用原 WebAPI 的 user role 解析，token 请求继续使用 consumer 授权 appId。
- `apollo-openapi` `v0.3.4` 已作为当前适配目标。相比 `v0.1.0`，`v0.3.x` 系列新增 Release/Branch/Instance/Permission/AccessKey、Portal USER 管理面等 operation，删除或重命名了多个 `v0.1.0` path，并收紧了 App 创建/更新/删除、Cluster 删除、env cluster info、missing envs 等生成接口返回类型。
- 本轮适配已切换默认 POM URL 到 `apollo-openapi` `v0.3.4`。兼容性检查中剩余的 `v0.1.0` 差异需要作为明确兼容例外或后续 alias 处理，不能视为静默兼容。
- 已经切到 OpenAPI 的 App 前端调用改为最新 spec 路径：`load_navtree` 调用 `/openapi/v1/apps/{appId}/env-cluster-info`，`find_miss_envs` 调用 `/openapi/v1/apps/{appId}/miss-envs`。`AppService.js` 会把新的数组响应转换回 `AppUtil.collectData` 仍在消费的 `entities/body` 结构。
- `/openapi/v1/apps/{appId}/navtree` 和 `/openapi/v1/apps/{appId}/miss_envs` 虽然已在 `v0.1.0` 发布，但已确认没有外部用户使用。本轮把这两个 App 旧路径作为明确兼容例外，不保留 alias；这个例外不能泛化到其它已发布的 `v0.1.0` path。
- Item 和 Namespace 域已切到 OpenAPI。`ConfigService.js` 继续在前端 service 内保留旧数组返回、旧 diff shape 和 `orderBy` 兼容 adapter，namespace 形状读取和写入由 generated OpenAPI controller 承接。
- Namespace Core 切片已切到 OpenAPI：namespace 读取、associated public namespace 读取、namespace lock 查询、missing namespace 查询/创建、cluster 删除、AppNamespace 创建/删除/读取/列表、namespace 创建/删除、release status、usage 和 public namespace instances 都已走 OpenAPI。后端在 `v0.3.0` 已有契约的地方实现 generated management interfaces，Portal UI 需要的响应兼容只保留在前端 service 内。
- Namespace 和 item 的 `extendInfo` 已承载 UI 还原旧视图需要的状态；text mode item update 改为后端根据 path 派生 `namespaceId`，不再依赖 UI 从 legacy `baseInfo.id` 取值。
- Release/Branch/Instance 切片已切到 OpenAPI：`ReleaseService.js`、`NamespaceBranchService.js`、`InstanceService.js` 全部使用 `/openapi/v1/...`。`ReleaseController`、`NamespaceBranchController`、`InstanceController` 实现 generated management interfaces，前端仅在 service 内保留实例分页 `content`、实例数量 `{num}` 和 release compare `{changes}` 兼容 adapter。
- 发布的 `v0.3.2` spec 为 rollback 增加 `toReleaseId`，把 branch delete/rule-update 的 `operator` query 放宽为 optional，并保持 release id 为 `int64`，使 Portal USER 请求从登录态推导 operator，同时 token CONSUMER 客户端继续保留 payload/query operator 行为。
- Permission/AccessKey 切片已切到 OpenAPI：`PermissionService.js`、`SystemRoleService.js` 和 `AccessKeyService.js` 都已使用 `/openapi/v1/...`；前端清单脚本也已覆盖 `static/scripts/services` 之外的直接 `$http`、select2 `ajax.url` 和下载 URL 调用。
- 最后一批 Portal UI 管理面已补齐 Portal-session-only OpenAPI：user、consumer、audit、commit history、release history、page settings、favorites、global search、server config、system info、config import/export、app search 和 namespace item import/export 均已迁移。前端清单现在为 131/131 OpenAPI。

## 迁移矩阵

| 领域 | WebAPI / 前端 service | OpenAPI 覆盖 | 迁移策略 |
| --- | --- | --- | --- |
| Env / Organization | `EnvService.js`、`OrganizationService.js` | 已有基础只读接口 | 保持 OpenAPI 路径，验证 SSO 与 prefix path |
| Cluster | `ClusterService.js` | 已有 get/create/delete | 前端 service 已全部使用 OpenAPI 创建、读取和删除；继续保持 USER/CONSUMER operator 回归覆盖 |
| App | `AppService.js` | 已有 app 查询、创建、更新、删除、env cluster、missing env、missing namespace、app-master role API | App 前端调用已全部迁移；Portal USER 请求在需要时从登录态推导 operator，响应兼容仍保留在前端 service 内 |
| Namespace / AppNamespace / Lock | `NamespaceService.js`、`NamespaceLockService.js` | `v0.3.0` 已包含 Namespace、AppNamespace、lock、usage、release status、public instance APIs | Namespace Core 前端 service 调用已迁移；后续推进 branch/release 和剩余 response-shape hardening |
| Item | `ConfigService.js` | item CRUD、diff、sync、validation、revocation、namespace read 已有契约方向 | Item 和 Namespace Core UI 路径已走 OpenAPI；后续继续观察 key 编码、text mode 和更多 e2e 覆盖 |
| Release / Branch | `ReleaseService.js`、`NamespaceBranchService.js` | `v0.3.2` 已覆盖 release、gray release、branch 创建/删除/合并/rules、compare、active releases 和 rollback | 前端 service 已迁移；继续保留双认证和 Java client 兼容测试 |
| Instance | `InstanceService.js` | 已有 by-release、by-namespace、releases-not-in 和 count API | 前端 service 已迁移，并在本地 adapter 保留分页/count 旧形状 |
| Permission / AccessKey | `PermissionService.js`、`SystemRoleService.js`、`AccessKeyService.js` | Permission、system-role 和 AccessKey 契约已进入 `v0.3.3` | 前端 service 调用已迁移；后续 review 继续优先保护 token caller 兼容性 |
| User / Consumer | `UserService.js`、`ConsumerService.js`、select2 用户搜索 directive | User 和 consumer 的 Portal UI API 已由 Portal-session-only OpenAPI 覆盖 | 前端调用已迁移；这些管理接口只允许 Portal USER session，不改变既有公开 OpenAPI token 操作的兼容行为 |
| Admin / Audit / Import / Export / Search / Favorites | `ServerConfigService.js`、`AuditLogService.js`、`CommonService.js`、`CommitService.js`、`ReleaseHistoryService.js`、`FavoriteService.js`、`GlobalSearchValueService.js`、`ConfigExportController.js`、导入导出 directive | Portal-session-only OpenAPI 已覆盖剩余管理、读取、下载和上传面 | 前端调用已迁移，包括直接 `$http`、select2 `ajax.url`、HEAD 检查和下载链接 |

## 契约规则

OpenAPI v1 是第三方客户端、SDK、CLI 和后续 MCP/Agent 工具的对外兼容面。下面规则只约束
已经发布的 OpenAPI 契约，不约束 Portal 前端 JS 和 Portal 后端之间的内部 WebAPI 路径；
后者跟随同一个仓库、同一个发布节奏，只要前后端在同次变更中对齐即可。

对已有 OpenAPI 客户端而言，下面变化默认视为 breaking change：

- 删除已有 path 或 HTTP method。
- 修改已有 `operationId`。
- 删除已有 schema。
- 给已有 schema 增加新的 required 字段。

如果确实需要替换已经发布的 OpenAPI 旧路径，默认必须保留旧路径 alias，或延后到 v2；
只有明确确认无人使用并记录为兼容例外的 path 才能直接移除。新增 path、可选字段、新
schema 和新 operation 默认是兼容变化。Portal UI 自己消费的路径可以按领域迁移，
不需要为旧 JS 路径保留兼容入口。

当前用兼容性检查脚本对比 `v0.1.0` 和 `v0.3.0`，已经能看到需要处理的
不兼容项：部分 `operationId` 已变更，`/openapi/v1/apps/{appId}/miss_envs`、
`/openapi/v1/apps/{appId}/navtree`、item `batchUpdate`/`sync`/`validate` 等旧路径
在 `v0.3.0` 中不再存在，`MultiResponseEntity`、`RichResponseEntity` 等旧 schema 也被移除。
因此后续不能把检查失败当成已自然解决；除已确认无人使用的 App 旧路径外，其它已发布契约差异
仍必须先在 `apollo-openapi` 中补 alias 或明确兼容例外。

## 认证与权限策略

Portal UI 请求 `/openapi/**` 时仍然是 Portal 用户行为，必须复用原 WebAPI 的 user 权限；
第三方 token 请求继续使用 consumer role。两者通过 `UserIdentityContextHolder` 区分，
并由 `UnifiedPermissionValidator` 分发。

OpenAPI token 读接口默认保持历史兼容，不直接收紧读取权限。若实例启用了
`configView.memberOnly.envs`，后续应增加明确配置或版本说明来决定 token 读接口是否也
跟随该策略，避免无提示影响现有客户端。

## 推进顺序

1. 固定基线：提交本文档、前端 URL 覆盖矩阵和 OpenAPI 兼容性检查脚本。
2. 稳定平台层：验证 spec 版本、prefix path、SSO、USER/CONSUMER 权限分发和编译生成链路。
3. 小步迁移领域：每个领域先补后端 OpenAPI 能力和双认证测试，再切前端 service。
4. 沉淀治理：将兼容性检查、portal compile、controller coverage 和 SDK 生成纳入发布流程。
5. 下游工具：OpenAPI 稳定后优先建设 CLI，MCP/Agent 能力基于更细的安全模型再推进。

## 验证门槛

- `python3 scripts/openapi/check_openapi_compatibility_test.py`
- `python3 scripts/openapi/check_openapi_compatibility.py --base <old-spec> --head <new-spec>`
- `./mvnw -pl apollo-portal -am -DskipTests compile`
- 每个迁移领域的 `MockMvc` 测试覆盖 Portal cookie、OpenAPI token、无认证和权限拒绝场景
- 前端手工 smoke test 覆盖对应 UI 主路径、错误提示、会话过期和 prefix path 部署
