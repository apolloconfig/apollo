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
| OpenAPI 契约 | `apollo-portal` 当前引用 `apollo-openapi` 的 `v0.1.0` tag；本地 `apollo-openapi/main` 已有更多 path | Portal 实现、生成接口和 SDK 容易漂移 | 每次更新 spec URL 前运行兼容性检查，记录明确 tag 或 commit |
| 前端调用 | 见 [前端 URL 迁移清单](./apollo-portal-openapi-frontend-url-inventory.md)，当前 121 个 URL 条目中 10 个走 OpenAPI、111 个仍走 WebAPI | 零散切流会遗漏 prefix path、SSO、权限和 response shape | 按领域迁移，每个领域先完成后端双认证验证 |
| 认证 | `/openapi/**` 先经过 Portal session 识别，再走 consumer token 认证 | 自定义 SSO 若没有让 `/openapi/**` 复用 Portal 登录态，会出现 401 | 明确 filter 顺序和 SSO 接入要求，补回归测试 |
| 权限 | `UnifiedPermissionValidator` 已按 `USER`/`CONSUMER` 分发 | OpenAPI 读接口历史上较开放，与 `configView.memberOnly.envs` 可能不一致 | 先保持 token 兼容，新增可控策略对齐只读权限 |
| 模型 | 生成模型、`apollo-openapi` Java artifact 旧 DTO/API、Portal DTO 并存 | 长期维护三套模型会持续放大转换成本 | 新接口优先实现 generated `*ManagementApi` 和 `model.*` |

## 当前进展

- Phase 0 基线已建立：迁移跟踪文档、前端 URL 清单、OpenAPI 兼容性检查脚本和 PR workflow 已补齐。
- 前端 prefix path 基础问题已清零：`ClusterService.js`、`ExportService.js` 和 `NamespaceLockService.js` 已统一使用 `AppUtil.prefixPath()`；清单中 no-prefix 条目为 0。
- OpenAPI 认证链路已加测试保护：`PortalUserSessionFilter`、`ConsumerAuthenticationFilter`、`UserTypeResolverFilter` 的顺序和 `/openapi/*` pattern 已由测试锁定。
- `UserTypeResolverFilter` 测试已改为覆盖生产实现，避免测试 classpath 中的同名 shadow class 掩盖真实行为。
- `UnifiedPermissionValidator` 的 USER/CONSUMER 分发测试已扩展到 namespace、application、hide-config 和 create/delete 相关入口。
- App 域已完成第一批只读切流：`find_apps`、`find_app_by_self`、`load_navtree` 和 `find_miss_envs` 已指向 OpenAPI；`load_app` 暂留 WebAPI，因为当前 generated `OpenAppDTO` 还缺少 UI 消费的 `ownerDisplayName`。
- `/openapi/v1/apps/by-self` 已补齐 Portal USER 语义：Portal cookie 请求复用原 WebAPI 的 user role 解析，token 请求继续使用 consumer 授权 appId。

## 迁移矩阵

| 领域 | WebAPI / 前端 service | OpenAPI 覆盖 | 迁移策略 |
| --- | --- | --- | --- |
| Env / Organization | `EnvService.js`、`OrganizationService.js` | 已有基础只读接口 | 保持 OpenAPI 路径，验证 SSO 与 prefix path |
| Cluster | `ClusterService.js` | 已有 get/create/delete | 统一加 `AppUtil.prefixPath()`，补 operator 与 USER/CONSUMER 语义 |
| App | `AppService.js` | 已有 app 查询、创建、更新、删除、env cluster、missing env | 只读接口已部分切流；写接口等待 operator contract/Portal 用户语义对齐，`load_app` 等待 `ownerDisplayName` 契约补齐 |
| Namespace / AppNamespace / Lock | `NamespaceService.js`、`NamespaceLockService.js` | 部分 spec 已在 `apollo-openapi/main` | 优先迁移只读和 lock，再迁移创建/删除 |
| Item | `ConfigService.js` | item CRUD、diff、sync、validation、revocation 已有契约方向 | 先确认 key 编码和 text mode 行为，再切 UI |
| Release / Branch | `ReleaseService.js`、`NamespaceBranchService.js` | release、gray release、merge、rollback 部分已有 | 先补灰度分支和 rollback 双认证测试 |
| Instance | `InstanceService.js` | 部分只读接口已有 | 只读迁移，保留分页和 response shape |
| Permission / AccessKey | `PermissionService.js`、`AccessKeyService.js` | `apollo-openapi/main` 已有新增契约 | 先完成权限模型验证，再迁移管理 UI |
| User / Consumer / System | `UserService.js`、`ConsumerService.js`、`SystemRoleService.js` | 用户接口仍有未合并 PR | 不纳入第一批切流，等待契约稳定 |
| Admin / Audit / Import / Export | `ServerConfigService.js`、`AuditLogService.js`、导入导出 service | OpenAPI 覆盖不足 | 独立设计，避免把管理面和配置面混在一个 PR |

## 契约规则

OpenAPI v1 是第三方客户端、SDK、CLI 和后续 MCP/Agent 工具的对外兼容面。下面规则只约束
已经发布的 OpenAPI 契约，不约束 Portal 前端 JS 和 Portal 后端之间的内部 WebAPI 路径；
后者跟随同一个仓库、同一个发布节奏，只要前后端在同次变更中对齐即可。

对已有 OpenAPI 客户端而言，下面变化默认视为 breaking change：

- 删除已有 path 或 HTTP method。
- 修改已有 `operationId`。
- 删除已有 schema。
- 给已有 schema 增加新的 required 字段。

如果确实需要替换已经发布的 OpenAPI 旧路径，例如把 `miss_envs` 调整为 `miss-envs`，
必须保留旧路径 alias，或延后到 v2。新增 path、可选字段、新 schema 和新 operation
默认是兼容变化。Portal UI 自己消费的路径可以按领域迁移，不需要为旧 JS 路径保留兼容入口。

当前用兼容性检查脚本对比 `v0.1.0` 和 `apollo-openapi/main`，已经能看到需要处理的
不兼容项：部分 `operationId` 已变更，`/openapi/v1/apps/{appId}/miss_envs`、
`/openapi/v1/apps/{appId}/navtree`、item `batchUpdate`/`sync`/`validate` 等旧路径
在 `main` 中不再存在，`MultiResponseEntity`、`RichResponseEntity` 等旧 schema 也被移除。
因此 `apollo-portal` 不能直接把 spec URL 从 `v0.1.0` 切到 `main`，必须先在
`apollo-openapi` 中补 alias 或明确兼容例外。

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
