# Apollo Portal Chrome UI 回归手册

本文件是发布前 UI 回归的唯一用例来源，包含 **60 组、160 个检查项**，以及准备、数据、执行顺序、预期结果、清理和现有自动化覆盖说明。以后直接维护本文，不另行维护 JSON 用例库或生成文档。

每次发布前由 Codex 执行全部检查，页面操作与结果通过 Chrome 核对；ADM-06涉及新凭据的写入默认使用已授权接口，再回到Chrome验证，不等待人工接管。结果只保存在本地并在对话中汇报，不上传结果，不接入 GitHub CI，也不要求运行记录生成或校验脚本。手册中的勾选框保持空白，历史通过状态不能带入下一版本。

范围为 Apollo Portal 桌面 Chrome、内置认证、单测试环境的页面功能，包含已发现问题对应的回归检查。多环境隔离、LDAP/OIDC、其他浏览器、性能、完整 OpenAPI 协议兼容性和 SDK 标签命中语义需要另加测试矩阵。现有自动化映射是参考，不能替代本次 Chrome 操作。

## 下次发布前直接使用的指令

> 按 `e2e/portal-e2e/release-regression/README.md`，通过 Chrome 对当前 Apollo 候选版本执行完整 UI 回归。使用我指定的测试环境和已授权的 qa3ui 测试数据范围，记录实际运行版本，执行全部 160 个检查项。按手册准备数据和真实 SDK，逐项核对预期，发现问题保留复现过程；新凭据按手册经接口录入，再通过Chrome验证页面和登录结果，记录实际执行方式，不因此阻塞其他检查。受阻时继续执行独立用例。测试结果和必要截图只保存在本地，不上传、不接入 CI。结束后清理本轮数据，报告通过、失败、未完成项及本地结果路径。

如果环境地址、候选版本或管理员身份与当前会话不同，在指令后补充实际信息即可。已有环境与数据操作授权继续有效；执行者先利用会话和页面中可确认的信息，不重复索取已提供的信息。

## 执行方式

1. 确认正在测试的 Portal/Admin/Config 服务地址、实际运行包版本或构建来源、认证方式、数据库和环境名。记录 Chrome 版本。源码 HEAD、未重启的服务和旧缓存可能对应不同代码，先确认页面加载的是本次候选版本。
2. 使用独立 `RUN_ID`（建议日期加轮次，例如 `20261001a`），在下表中确定本轮资源名称。新版本重新执行全部检查；同一版本因工具中断继续时，从本地未完成项接着做。
3. 在本目录的 `runs/<RUN_ID>/` 下保存本地结果和临时文件；`runs/` 已被 Git 忽略。创建 `result.md`、`fixtures/` 和需要时的 `screenshots/` 即可，不需要初始化工具。执行前把本文复制一份到该目录，留存本轮用例版本；在结果表中列出全部检查项 ID，初始状态均为未执行。
4. 使用 Chrome 真实点击、输入、文件选择、刷新和页面观察完成检查。每个编号的必需步骤及预期都验证后才能记为通过。ADM-06.01/06.02明确采用“Chrome+HTTP”方式，其余检查仍按页面步骤执行；仅接口返回成功、源码正确或单元测试通过，不能替代页面结果。
5. 按下方顺序准备数据。HTTP 可在已获授权时用于辅助准备，以及ADM-06明确允许的创建、编辑和资料恢复；使用页面同一组用户管理接口，记录实际执行方式。不得把接口写入记为浏览器表单提交成功，也不通过页面脚本注入新凭据。
6. 遇到失败立即记录编号、目标资源、步骤、预期和实际结果。缺少工具能力、身份或环境时记为受阻，继续不依赖该条件的用例；恢复后回到原项。既有缺陷的历史通过不自动成为本次结果。
7. 完成每组的清理和最终收尾，在本地核对所有编号都有结果。向用户报告实际通过、失败、受阻、未执行、不适用及保留资源；通过项注明纯Chrome或Chrome+HTTP及实际验证范围，不自动提交、上传或创建发布检查。

## 环境、身份与变量

主窗口使用管理员，隐身窗口使用本轮普通用户。普通用户初始不得拥有应用管理员、修改或发布角色；临时角色只授予本轮测试资源，结束后撤销。测试密码在本地为本轮账号随机生成或由用户安全提供，满足当前密码策略，不写入手册、截图或结果。

使用 Codex Chrome 扩展时，先确认真实文件选择和隐身窗口访问可用。本轮受新凭据输入限制影响的步骤是：ADM-06.01创建用户的密码/确认密码输入和提交、ADM-06.02编辑资料时要求的密码提交，以及清理时恢复资料的同类提交。默认按下节经接口完成这些写入，不将人工接管作为其他用例的前置条件。ADM-01.02空表单校验不输入新凭据；SESSION-01及ADM-06登录核对使用已经建立的凭据，仍通过Chrome执行。

接口认证或实际写入失败时，如实判定受阻或失败，继续独立用例，不能忽略失败或将未完成的写入记为通过。

`{{...}}` 是执行时替换的变量，不是要原样输入页面的文本。相对入口路径均拼接到实际 Portal 地址；环境名与部门以页面为准。

| 变量 | 本轮取值方式或示例 |
| --- | --- |
| `RUN_ID` | 日期加轮次，建议不超过12个小写字母/数字，例如 `20261001a` |
| `BASE_URL` | Portal 地址，例如 `http://localhost:8070` |
| `CONFIG_URL` | ConfigService/Meta 地址，例如 `http://localhost:8080` |
| `ENV` | 本次测试环境，例如 `LOCAL` |
| `ORG_ID` | 已存在的测试部门代码，例如 `TEST1` |
| `APP_ID` | `qa3ui-<RUN_ID>` |
| `CLIENT_APP_ID` | `qa3ui-client-<RUN_ID>`，用于关联公共 Namespace |
| `TEST_USER` | `qa3ui-user-<RUN_ID>` |
| `CLUSTER` | `qa3ui-cluster`，只在本轮应用内创建 |
| `PUBLIC_LOCAL_NAME` | `qa3uipub-<RUN_ID>` |
| `PUBLIC_NS` | `<ORG_ID>.qa3uipub-<RUN_ID>`，保持全局唯一 |
| `FIXTURES` | 本轮 `runs/<RUN_ID>/fixtures/` 的绝对路径，供文件选择器使用 |

测试资源必须在已授权、允许创建/修改/发布/清理的测试环境中。既有非测试应用只读，不使用历史轮次的账户、Token、密钥或配置作为默认前置条件。

### 新凭据的接口录入与页面核对

仅操作本轮`{{TEST_USER}}`或已登记的`qa3ui-user-{{RUN_ID}}-...`派生账号。先查询确认创建目标不存在；如本轮主账号已经准备好，另选唯一派生账号验证创建，禁止将创建失败改成更新已有非本轮账号。

本地HTTP客户端使用已授权管理员的现有登录凭据建立独立Portal会话，遵循当前环境认证要求。调用与[UserService.js](../../../apollo-portal/src/main/resources/static/scripts/services/UserService.js)相同的接口：

| 用途 | 方法与路径 | 关键字段 |
| --- | --- | --- |
| 创建 | `POST /openapi/v1/users?isCreate=true` | `username`、`userDisplayName`、`password`、`email`、`enabled=1` |
| 编辑/恢复资料 | `POST /openapi/v1/users?isCreate=false` | 同上；保留目标用户名和启用状态，传入本轮已建立的测试密码 |
| 查询 | `GET /openapi/v1/users?keyword=<用户名>&includeInactiveUsers=true` | URL编码查询值，按完整用户名核对结果 |

当前创建和更新接口均要求非空密码；更新会写入密码，因此编辑与恢复只使用本轮已知测试密码。请求应在本地HTTP客户端完成，不在浏览器页面中注入脚本。密码和会话只保存在进程内存；必须跨进程使用时，放入本轮权限为0600的临时文件并在收尾删除，不能出现在命令行参数、请求日志或结果正文。

HTTP写入后必须回到Chrome，退出未提交表单、刷新列表、按用户名查询并重新打开资料，核对显示名、邮箱、状态和持久化结果；在独立会话中实际登录确认身份。结果记录方法/路径、用户名、状态码、非敏感字段及页面观察，执行方式记为`Chrome+HTTP`。只得到200而没有页面和登录核对，不能记为通过。

## 执行顺序与数据恢复

编号保持稳定，不要求按字典序执行。前置条件是进入条件；依赖失败时将受影响项记为受阻，不跳过后默认为通过。

| 阶段 | 执行范围 | 进入下一阶段前保留的状态 |
| --- | --- | --- |
| 1. 环境与账号 | ADM-05、SESSION-01登录部分、ADM-01、ADM-06 | 管理员可用，普通用户已创建并启用，两窗口身份独立 |
| 2. 应用与基础数据 | APP-01/02/03、CLU-01/02、NS-01/02、CFG-01、REL-01 | 两应用、default和qa3ui-cluster；类型/特殊Key基线已发布 |
| 3. 页面与配置 | NAV、APP-04、NS-03/04/05、CFG、FMT | 文件Namespace已发布；公共源public-base、消费者覆盖client-override |
| 4. 身份与审计 | AUTH、CFG-05删除行、ADM-06启停、SESSION-01其余部分、AUD | 临时角色撤销；用户资料恢复；审计记录含区间内外操作 |
| 5. 发布与灰度 | REL-02/03/04、GRAY、INS-01/02 | 放弃灰度、停止SDK，主版本有明确基线，无无关草稿 |
| 6. 管理与数据交换 | KEY、TOK、OPEN、ADM-02/03/04、SEARCH、EXP | Token/consumer/密钥/参数清理，导入草稿撤销 |
| 7. 清理与报告 | DEL-01、最终登出、本地结果核对 | 本轮资源已清理，保留资源有明确原因 |

CFG-05需要普通用户临时修改权限，可与AUTH一起执行。ADM-06禁用测试结束后重新启用，以完成其他身份用例；最终清理时禁用一次性用户。INS-01的空实例检查在启动SDK之前执行。

发布、回滚、灰度和实例检查会有意更改配置。用例指定42/99/100/101时，先通过页面建立相应已发布值并记录版本，不假设上一用例恰好留下该值。导入测试统一使用 `{{CLUSTER}}/application` 的7项已发布基线，Number=99；777只作为冲突检查的临时草稿。

普通用户删除行测试在default/application另建 `qa3ui.audit-user` 并发布；公共源/消费者分别使用 `public-base` / `client-override`。检查后撤销删除，保持后续用例所需的数据。

## 本地测试文件

由执行本轮的 Codex 按下述内容在 `{{FIXTURES}}` 创建文件。可以使用本地文件工具或 Python 标准库写 JSON、ZIP，这只是准备上传数据；真正导入和页面验证仍通过 Chrome 完成。生成后先检查 ZIP 内路径、目标集群和类型，避免把数据错误当成产品缺陷。

### properties导入及ZIP

`normal.properties` 是 Apollo 的 JSON item 数组，扩展名虽为properties，内容不是传统的 `key=value`。各项 value 均为字符串；type为0/String、1/Number、2/Boolean、3/JSON。

```json
[
  {"key":"qa3ui.message","type":0,"value":"UI 回归 中文 & / \\ %","comment":"UI regression","lineNum":1},
  {"key":"qa3ui.number","type":1,"value":"99","comment":"UI regression","lineNum":2},
  {"key":"qa3ui.flag","type":2,"value":"true","comment":"UI regression","lineNum":3},
  {"key":"qa3ui.object","type":3,"value":"{\"qa\":true}","comment":"UI regression","lineNum":4},
  {"key":"qa3ui/path","type":0,"value":"slash","comment":"UI regression","lineNum":5},
  {"key":"qa3ui\\path","type":0,"value":"backslash","comment":"UI regression","lineNum":6},
  {"key":"qa3ui%2Fpath","type":0,"value":"percent","comment":"UI regression","lineNum":7}
]
```

`partial-failure.properties` 用于验证导入部分失败时的错误提示，必须保留一个合法项和一个非法类型项：

```json
[
  {"key":"qa3ui.import-ok","type":2,"value":"true","lineNum":20},
  {"key":"qa3ui.invalid-type","type":9,"value":"invalid","lineNum":21}
]
```

复制为 `corrected.properties`，仅将第二项type从9改为0。若未来9成为合法类型，应根据实际模型选择明确非法的类型并更新本文。部分失败可能已写入第一项；观察实际目标后撤销，不能看到错误就认为数据未变。

根据上述两个原文件构建4个ZIP；替换路径中的所有变量，ZIP内文件名统一为目标application的名字：

| 上传文件 | ZIP内唯一路径 | 内容 |
| --- | --- | --- |
| `normal-app.zip` | `{{APP_ID}}/{{ENV}}/{{APP_ID}}+{{CLUSTER}}+application.properties` | normal.properties |
| `normal-all.zip` | `apollo/{{APP_ID}}/{{ENV}}/{{APP_ID}}+{{CLUSTER}}+application.properties` | normal.properties |
| `partial-failure-app.zip` | `{{APP_ID}}/{{ENV}}/{{APP_ID}}+{{CLUSTER}}+application.properties` | partial-failure.properties |
| `partial-failure-all.zip` | `apollo/{{APP_ID}}/{{ENV}}/{{APP_ID}}+{{CLUSTER}}+application.properties` | partial-failure.properties |

这些ZIP只含配置，目标应用、集群、Namespace必须已经通过页面创建。它们不替代完整环境导出的元数据检查。

### 文件Namespace内容

`sample.yaml` 和 `sample.yml`：

```yaml
qa3ui:
  enabled: true
  message: '中文 & / %'
  servers:
    - local
```

`sample.json`：

```json
{"qa3ui":{"enabled":true,"message":"中文 & / %"}}
```

`sample.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<qa3ui enabled="true">中文 &amp; / %</qa3ui>
```

`sample.txt`：

```text
UI 回归
第二行 & / \ %
```

`invalid.json` 内容为未闭合的 `{"qa3ui": `；`invalid.yaml` 内容为未闭合的 `qa3ui: [unclosed`。保存前确保目标Namespace已有有效内容，失败后检查原内容保持。

### Namespace的18组边界输入

NS-03.02–19逐组测试6格式×私有/公共带部门前缀/公共不带前缀。部门前缀长度P包含末尾的点；私有和公共不带前缀时P=0。后缀长度S为点加格式名，properties则为0。

| 格式 | S | 无前缀的输入上限 | TEST1.前缀（P=6）时输入上限 |
| --- | ---: | ---: | ---: |
| properties | 0 | 32 | 26 |
| json / yaml | 5 | 27 | 21 |
| yml / xml / txt | 4 | 28 | 22 |

每组用本轮唯一短名称加x补足 `32-P-S` 个字符，先确认完整名称32字符可创建；再给输入名追加一个x，确认33字符在前端拒绝。名称中包含短轮次和组序号，避免公共名称全局重复；如果前缀太长，缩短测试名称种子。记录实际名称和两个边界结果，并清理成功创建的资源。长度校验提示文案另按NS-03.20核对，不因某一种格式通过就省略其他组合。

## 真实SDK实例准备

[只读SDK客户端](sdk-client/src/main/java/UiRegressionClient.java)用于INS-02，它读取本轮application并打印 `qa3ui.number` 初始值及变更，使Chrome实例页面有真实数据。它不会执行页面用例或修改配置。

默认依赖Apollo Java SDK 2.4.0，可用 `-Dapollo.client.version=...` 指定本次需要验证的版本，记录实际版本。已有合适的真实测试客户端时也可复用，但只连接本轮应用。

在仓库根目录构建：

```bash
./mvnw -f e2e/portal-e2e/release-regression/sdk-client/pom.xml \
  compile dependency:build-classpath -Dmdep.outputFile=target/classpath.txt
```

在SDK目录启动，替换AppId、地址及缓存目录中的轮次；保持标准输入可用，回车停止：

```bash
cd e2e/portal-e2e/release-regression/sdk-client
java -Dapp.id=qa3ui-20261001a \
  -Dapollo.meta=http://localhost:8080 \
  -Dapollo.cluster=default \
  -Dapollo.cacheDir=target/cache/20261001a \
  -cp "target/classes:$(cat target/classpath.txt)" UiRegressionClient
```

以Chrome实例页显示的真实IP设置灰度规则。先确认输出不是MISSING，按INS-02依次核对读取99、停止后发布100、灰度读取101、放弃后回到100，并在Chrome核对对应实例版本与时间。执行工具须保持交互式输入，避免stdin立即EOF导致客户端退出。结束后停止进程。

## 本地结果与收尾

本轮 `result.md` 顶部记录版本/实际构建、执行时间、Portal地址与环境、Chrome/SDK版本、RUN_ID和资源名称。正文按160个检查项逐项记录，格式如下；普通通过项可用简短文字，失败、边界和修复回归保存必要的本地截图或操作记录，无需每次点击都截图。

| 检查项ID | 状态 | 实际结果 | 本地证据/问题 |
| --- | --- | --- | --- |
| EXP-02.04 | 未执行 | | |

状态为通过、失败、受阻、未执行、不适用。通过必须完成该编号下全部必需步骤；ADM-06.01/06.02按默认方式通过时，在实际结果中注明`Chrome+HTTP`及接口写入、页面核对的范围。受阻与不适用说明具体原因，不能算通过。重新验证时保留原失败记录，再追加修复版本和重测结果；流程调整不追溯改写旧报告。不要在长期用例正文中勾选历史结果。

最终核对全部编号都有结果，并逐项收尾：

- 撤销本轮临时角色，保留原管理员；普通用户登出，禁用一次性账号，若产品无删除入口则记录账号残留。
- 撤销并删除本轮Token及轮换记录，删除consumer、访问密钥和PortalDB/ConfigDB各cluster测试参数。
- 放弃测试灰度，撤销导入部分写入和草稿，停止SDK。
- 先清理消费者，再清理主应用、临时应用、集群、公共/私有Namespace及边界测试创建的资源；清理失败记录精确目标和原因。
- 截图和记录不含密码、Token/密钥明文或非测试配置。结果、文件和证据留在本机，无需上传、提交或创建GitHub检查。
- 删除本轮接口会话、临时密码文件；资料恢复经接口完成后，仍在Chrome确认最终用户状态。

最终对话给出各状态数量、通过项中Chrome+HTTP的ID及验证范围、失败/未完成的ID、缺陷复现与清理结果，以及本地 `result.md` 路径。只要仍有失败或未完成项，就如实报告；不要将功能组通过扩展成该组所有细项通过。

## 用例维护与自动化说明

直接修改本文的操作和预期，保留组ID及检查项ID；增加新页面或分支时增加编号，同步更新索引数量、数据和清理说明。发布前对照实际页面入口检查是否新增功能；有新增则补入本文后执行。

各组“现有自动化”注明已有哪些Playwright/API/Java/JavaScript测试以及覆盖边界，引用基于当前源码。它们不是本手册的执行前置步骤，也不意味着本次候选包已运行这些测试。需要另外运行时按[已有E2E说明](../../README.md)操作。用例调整不要求新增CI、生成文件、汇总工具或上传结果。

## 完整测试用例

下表可直接跳转每组。全部160项默认未执行；每次在本地结果中记录真实检查结论。

| 组编号 | 场景 | 检查项 |
| --- | --- | ---: |
| [NAV-01](#nav-01) | 首页与列表导航 | 2 |
| [NAV-02](#nav-02) | 应用与配置 Key 搜索 | 2 |
| [NAV-03](#nav-03) | 中英文切换 | 2 |
| [APP-01](#app-01) | 创建应用及必填校验 | 2 |
| [APP-02](#app-02) | 重复AppId拒绝 | 1 |
| [APP-03](#app-03) | 应用信息编辑与展示 | 2 |
| [APP-04](#app-04) | 收藏、置顶与取消 | 2 |
| [CLU-01](#clu-01) | 创建集群及名称校验 | 2 |
| [CLU-02](#clu-02) | 集群切换与管理 | 2 |
| [NS-01](#ns-01) | 私有properties Namespace创建及重复拒绝 | 2 |
| [NS-02](#ns-02) | 五种文件格式Namespace创建 | 5 |
| [NS-03](#ns-03) | 完整名称长度边界与非法名称 | 20 |
| [NS-04](#ns-04) | 公共Namespace创建、搜索和关联 | 2 |
| [NS-05](#ns-05) | 公共配置继承、覆盖和取消覆盖 | 3 |
| [CFG-01](#cfg-01) | 配置类型、必填与重复Key | 2 |
| [CFG-02](#cfg-02) | 值、备注与特殊内容 | 2 |
| [CFG-03](#cfg-03) | 新增、修改、删除的撤销 | 2 |
| [CFG-04](#cfg-04) | 特殊Key精确编辑、删除和撤销 | 3 |
| [CFG-05](#cfg-05) | 配置过滤、排序、修改时间和删除行身份 | 3 |
| [CFG-06](#cfg-06) | 文本编辑保持类型与行号变化 | 2 |
| [CFG-07](#cfg-07) | 跨集群同步与比较 | 2 |
| [CFG-08](#cfg-08) | 视图与工具栏 | 4 |
| [CFG-09](#cfg-09) | 展开、全屏、Esc与刷新 | 2 |
| [FMT-01](#fmt-01) | YAML/YML编辑发布与撤销 | 2 |
| [FMT-02](#fmt-02) | JSON文件与值格式化 | 2 |
| [FMT-03](#fmt-03) | 非法JSON/YAML保护已存内容 | 2 |
| [FMT-04](#fmt-04) | XML/TXT编辑发布与撤销 | 2 |
| [REL-01](#rel-01) | 首次发布与差异预览 | 2 |
| [REL-02](#rel-02) | 二次发布及历史比较 | 2 |
| [REL-03](#rel-03) | 两种回滚入口 | 2 |
| [REL-04](#rel-04) | 更改历史与筛选 | 2 |
| [GRAY-01](#gray-01) | 创建灰度与配置隔离 | 2 |
| [GRAY-02](#gray-02) | 私有单条/公共多App灰度规则维护 | 2 |
| [GRAY-03](#gray-03) | 灰度发布历史完整展示 | 2 |
| [GRAY-04](#gray-04) | 全量发布的阻止、保留和删除分支 | 3 |
| [GRAY-05](#gray-05) | 放弃私有和公共灰度 | 2 |
| [INS-01](#ins-01) | 无实例空态与视图 | 2 |
| [INS-02](#ins-02) | 真实SDK、版本迁移与灰度实例 | 4 |
| [AUTH-01](#auth-01) | 权限页、用户搜索和最后管理员保护 | 2 |
| [AUTH-02](#auth-02) | 角色授予和撤销 | 3 |
| [AUTH-03](#auth-03) | 第二身份只读、编辑和发布隔离 | 4 |
| [KEY-01](#key-01) | 访问密钥生命周期与审计 | 2 |
| [TOK-01](#tok-01) | 个人Token校验、范围选择和详情 | 2 |
| [TOK-02](#tok-02) | Token轮换、撤销和删除 | 3 |
| [TOK-03](#tok-03) | 管理员Token查询、详情和撤销 | 2 |
| [OPEN-01](#open-01) | 开放平台列表、查询和必填 | 2 |
| [OPEN-02](#open-02) | 受限consumer创建、授权和删除 | 3 |
| [ADM-01](#adm-01) | 用户搜索与空表单校验 | 2 |
| [ADM-02](#adm-02) | 系统权限管理查询 | 1 |
| [ADM-03](#adm-03) | PortalDB/ConfigDB参数查询 | 1 |
| [ADM-04](#adm-04) | 测试参数生命周期与cluster隔离 | 3 |
| [ADM-06](#adm-06) | 测试用户创建、编辑和启停（凭据经接口录入） | 3 |
| [ADM-05](#adm-05) | 系统服务信息与健康检查 | 1 |
| [AUD-01](#aud-01) | 审计查询、详情、分页和清空日期 | 3 |
| [EXP-01](#exp-01) | 应用及单Namespace导出 | 2 |
| [EXP-03](#exp-03) | 按环境导出 | 2 |
| [EXP-02](#exp-02) | 正常导入、冲突处理及部分失败 | 7 |
| [SEARCH-01](#search-01) | 全局Value搜索与分页 | 2 |
| [DEL-01](#del-01) | 测试Namespace、集群和应用清理 | 3 |
| [SESSION-01](#session-01) | 登录、退出和独立会话 | 3 |

<a id="nav-01"></a>

## NAV-01 首页与列表导航

入口：`/`

前置条件：管理员已登录；APP-01及NS-04已建立测试数据

- [ ] **NAV-01.01** 依次打开我的应用、最近浏览、收藏列表、公共 Namespace 列表。
  - 预期：四类列表正常加载，无错误提示；已有测试数据在对应列表可见。
- [ ] **NAV-01.02** 从各列表点击本次测试应用或公共 Namespace，再返回首页。
  - 预期：目标 AppId、Namespace 和来源应用准确，返回后列表仍可用。

清理：恢复首页；收藏由APP-04清理。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="nav-02"></a>

## NAV-02 应用与配置 Key 搜索

入口：`/`

前置条件：测试主应用、消费者应用及两个集群已有配置

- [ ] **NAV-02.01** 分别搜索 {{APP_ID}}、中文应用名、qa3ui.number。
  - 预期：AppId和中文名命中正确应用；Key结果包含有该配置的default及{{CLUSTER}}。
- [ ] **NAV-02.02** 点击一个Key搜索结果，并搜索不存在的 qa3ui-missing-{{RUN_ID}}。
  - 预期：点击到正确应用/集群/Namespace；无结果有明确空态，无旧结果残留。

清理：清除搜索条件。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="nav-03"></a>

## NAV-03 中英文切换

入口：`/`

前置条件：管理员已登录

- [ ] **NAV-03.01** Language→English，检查首页、导航菜单和配置表格列名。
  - 预期：主要标题和操作文案切换为英文，原页面数据可用。
- [ ] **NAV-03.02** 切回简体中文，再打开一个管理页面。
  - 预期：中文文案恢复，导航和操作正常；无原始翻译key。

清理：恢复简体中文。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="app-01"></a>

## APP-01 创建应用及必填校验

入口：`/app.html`

前置条件：{{APP_ID}}和{{CLIENT_APP_ID}}尚不存在；管理员有创建权限

- [ ] **APP-01.01** 新建应用，保持必填项为空并尝试提交；再只填写部分必填项。
  - 预期：页面阻止不完整表单提交，指出缺少字段。
- [ ] **APP-01.02** 创建{{APP_ID}}，部门{{ORG_ID}}，选择管理员为负责人，填写本次回归名称；同样创建{{CLIENT_APP_ID}}。
  - 预期：两应用均创建成功；刷新可查，default集群和application存在；负责人信息正确。

清理：保留供后续用例，最后执行DEL-01清理。

现有自动化：部分：core验证应用创建成功；未穷举必填校验和第二应用。

参考：[portal-core.spec.js](../tests/portal-core.spec.js)

<a id="app-02"></a>

## APP-02 重复AppId拒绝

入口：`/app.html`

前置条件：APP-01完成

- [ ] **APP-02.01** 用已存在的{{APP_ID}}再次填写并提交创建表单。
  - 预期：明确提示应用已存在，不产生重复应用或额外默认资源。

清理：退出未提交表单。

现有自动化：部分：regression验证重复创建HTTP错误并停留创建页；仍需检查页面提示和无重复资源。

参考：[portal-regression.spec.js](../tests/portal-regression.spec.js)

<a id="app-03"></a>

## APP-03 应用信息编辑与展示

入口：`/app/setting.html?#/appid={{APP_ID}}`

前置条件：APP-01完成

- [ ] **APP-03.01** 修改应用名称为“UI回归 {{RUN_ID}} 已更新”，保存后刷新。
  - 预期：修改持久化，AppId不变，部门/负责人正确。
- [ ] **APP-03.02** 核对管理页的AppId、应用名、部门和负责人，再返回配置页核对侧栏应用信息。
  - 预期：两处应用信息一致，部门和负责人对应所选对象，展示正常。应用管理页不要求提供邮箱编辑或创建人、最后修改人、时间字段；用户邮箱编辑由ADM-06覆盖。

清理：保留新名称供搜索验证，执行记录记下该名称。

现有自动化：无对应完整自动化：按本手册通过Chrome执行应用信息修改、刷新及两页展示检查。

<a id="app-04"></a>

## APP-04 收藏、置顶与取消

入口：`/`

前置条件：APP-01完成

- [ ] **APP-04.01** 收藏主应用和消费者应用，打开收藏列表并刷新。
  - 预期：两项可见，收藏状态持久化。
- [ ] **APP-04.02** 置顶消费者，刷新后观察顺序，再取消两项收藏。
  - 预期：置顶顺序正确；取消后两项消失，无其他收藏受影响。

清理：取消本次创建的全部收藏及置顶。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="clu-01"></a>

## CLU-01 创建集群及名称校验

入口：`/cluster.html?#/appid={{APP_ID}}`

前置条件：APP-01完成

- [ ] **CLU-01.01** 创建{{CLUSTER}}，再用相同名称创建一次。
  - 预期：第一次成功，重复名称明确拒绝，列表没有重复项。
- [ ] **CLU-01.02** 输入含空格或非法字符的集群名并尝试创建。
  - 预期：页面或服务明确拒绝，不产生非法集群。

清理：保留{{CLUSTER}}供配置和导入验证。

现有自动化：部分：regression覆盖创建及重复；非法名称提示需补齐。

参考：[portal-regression.spec.js](../tests/portal-regression.spec.js)

<a id="clu-02"></a>

## CLU-02 集群切换与管理

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：CLU-01完成；两个集群使用不同qa3ui.number值

- [ ] **CLU-02.01** 在default与{{CLUSTER}}间切换，分别查看application。
  - 预期：集群标识、配置值与各自数据一致，不串数据。
- [ ] **CLU-02.02** 进入管理集群和集群权限页面。
  - 预期：列出两集群，选中目标、应用信息和权限范围正确。

清理：回到default，记录当前选择。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="ns-01"></a>

## NS-01 私有properties Namespace创建及重复拒绝

入口：`/namespace.html?#/appid={{APP_ID}}`

前置条件：APP-01完成

- [ ] **NS-01.01** 创建私有properties Namespace qa3ui，确认关联目标集群。
  - 预期：名称无格式后缀，私有属性正确，所选集群出现空配置面板。
- [ ] **NS-01.02** 再次创建同名qa3ui。
  - 预期：明确拒绝重复，已有Namespace和配置不变。

清理：保留qa3ui，最后删除测试资源。

现有自动化：部分：regression覆盖创建及重复；删除和页面属性需补齐。

参考：[portal-regression.spec.js](../tests/portal-regression.spec.js)

<a id="ns-02"></a>

## NS-02 五种文件格式Namespace创建

入口：`/namespace.html?#/appid={{APP_ID}}`

前置条件：APP-01完成

- [ ] **NS-02.01** 创建私有yaml格式Namespace qa3ui-yaml，刷新配置页。
  - 预期：成功创建qa3ui-yaml.yaml，格式为yaml，名称仅追加一次后缀，面板可编辑。
- [ ] **NS-02.02** 创建私有yml格式Namespace qa3ui-yml，刷新配置页。
  - 预期：成功创建qa3ui-yml.yml，格式为yml，名称仅追加一次后缀，面板可编辑。
- [ ] **NS-02.03** 创建私有json格式Namespace qa3ui-json，刷新配置页。
  - 预期：成功创建qa3ui-json.json，格式为json，名称仅追加一次后缀，面板可编辑。
- [ ] **NS-02.04** 创建私有xml格式Namespace qa3ui-xml，刷新配置页。
  - 预期：成功创建qa3ui-xml.xml，格式为xml，名称仅追加一次后缀，面板可编辑。
- [ ] **NS-02.05** 创建私有txt格式Namespace qa3ui-txt，刷新配置页。
  - 预期：成功创建qa3ui-txt.txt，格式为txt，名称仅追加一次后缀，面板可编辑。

清理：保留供FMT用例。

现有自动化：部分：configservice覆盖yaml/json及实际读取；yml/xml/txt创建需补齐。

参考：[portal-configservice.spec.js](../tests/portal-configservice.spec.js)

<a id="ns-03"></a>

## NS-03 完整名称长度边界与非法名称

入口：`/namespace.html?#/appid={{APP_ID}}`

前置条件：APP-01完成；部门前缀足够短，P+S小于32

- [ ] **NS-03.01** 输入非法字符名称并提交。
  - 预期：明确拒绝，不创建Namespace。
- [ ] **NS-03.02** properties格式、私有：计算完整前缀长度P和后缀长度S=0，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.03** properties格式、公共带部门前缀：计算完整前缀长度P和后缀长度S=0，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.04** properties格式、公共不带部门前缀：计算完整前缀长度P和后缀长度S=0，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.05** json格式、私有：计算完整前缀长度P和后缀长度S=5，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.06** json格式、公共带部门前缀：计算完整前缀长度P和后缀长度S=5，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.07** json格式、公共不带部门前缀：计算完整前缀长度P和后缀长度S=5，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.08** yaml格式、私有：计算完整前缀长度P和后缀长度S=5，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.09** yaml格式、公共带部门前缀：计算完整前缀长度P和后缀长度S=5，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.10** yaml格式、公共不带部门前缀：计算完整前缀长度P和后缀长度S=5，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.11** yml格式、私有：计算完整前缀长度P和后缀长度S=4，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.12** yml格式、公共带部门前缀：计算完整前缀长度P和后缀长度S=4，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.13** yml格式、公共不带部门前缀：计算完整前缀长度P和后缀长度S=4，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.14** xml格式、私有：计算完整前缀长度P和后缀长度S=4，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.15** xml格式、公共带部门前缀：计算完整前缀长度P和后缀长度S=4，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.16** xml格式、公共不带部门前缀：计算完整前缀长度P和后缀长度S=4，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.17** txt格式、私有：计算完整前缀长度P和后缀长度S=4，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.18** txt格式、公共带部门前缀：计算完整前缀长度P和后缀长度S=4，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.19** txt格式、公共不带部门前缀：计算完整前缀长度P和后缀长度S=4，使用唯一名称分别测试输入长度32-P-S及33-P-S。
  - 预期：完整名称32字符可通过长度检查并创建；33字符在前端阻止，不发送创建请求。校验完整名称而非仅输入框长度；记录两个边界结果。
- [ ] **NS-03.20** 用32字符输入名+json触发超长，分别切换中文和英文查看提示。
  - 预期：两个语言的提示均包含部门前缀、名称、格式后缀长度；私有为0/32/5，不显示旧文案或未替换变量。

清理：删除各边界测试创建的空Namespace；恢复中文。

现有自动化：支撑：test_namespaceNameLength.js覆盖6格式×3模式的32/33边界；它是控制器单元测试，不验证Chrome显示文案。

参考：[test_namespaceNameLength.js](../../../apollo-portal/src/test/resources/static/scripts/test_namespaceNameLength.js)

<a id="ns-04"></a>

## NS-04 公共Namespace创建、搜索和关联

入口：`/namespace.html?#/appid={{APP_ID}}`

前置条件：两个测试应用已存在

- [ ] **NS-04.01** 在主应用创建公共properties Namespace，名称{{PUBLIC_LOCAL_NAME}}，保留部门前缀；添加qa3ui.shared=public-base并发布。
  - 预期：完整名称为{{PUBLIC_NS}}，公共属性、来源应用、已发布值正确。
- [ ] **NS-04.02** 首页公共Namespace搜索{{PUBLIC_NS}}，消费者应用选择关联该Namespace。
  - 预期：搜索命中；关联后来源指向{{APP_ID}}/default，消费者可看到public-base。

清理：保留两应用的关联关系供NS-05、CFG-05.03及灰度用例。

现有自动化：部分：priority覆盖公共关联和覆盖；首页搜索及创建校验需补齐。

参考：[portal-priority.spec.js](../tests/portal-priority.spec.js)

<a id="ns-05"></a>

## NS-05 公共配置继承、覆盖和取消覆盖

入口：`/config.html?#/appid={{CLIENT_APP_ID}}`

前置条件：NS-04完成

- [ ] **NS-05.01** 消费者查看qa3ui.shared，再覆盖为client-override并发布。
  - 预期：覆盖前继承public-base；发布后消费者为client-override，公共源仍public-base。
- [ ] **NS-05.02** 删除已发布覆盖，查看待发布删除行，再发布。
  - 预期：删除行显示完整操作者和时间；发布后恢复public-base继承值。
- [ ] **NS-05.03** 重新添加client-override并发布，为后续普通用户删除测试准备数据。
  - 预期：消费者重新覆盖，来源值不变。

清理：保留已发布client-override，后续删除操作一律撤销恢复。

现有自动化：部分：priority覆盖继承和覆盖值；取消覆盖、删除行显示与恢复需补齐。

参考：[portal-priority.spec.js](../tests/portal-priority.spec.js)

<a id="cfg-01"></a>

## CFG-01 配置类型、必填与重复Key

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：default/application为空或有明确基线

- [ ] **CFG-01.01** 在application逐项新增qa3ui.message String、qa3ui.number Number、qa3ui.flag Boolean、qa3ui.object JSON；数据见夹具说明。
  - 预期：四项成功，刷新后值和类型正确。
- [ ] **CFG-01.02** 空Key尝试提交；再用qa3ui.number重复新增。
  - 预期：空Key被拦截，重复Key明确拒绝，不覆盖已有值。

清理：完成后按REL-01发布基线。

现有自动化：部分：core覆盖String新增；其余三类型及非法输入需补齐。

参考：[portal-core.spec.js](../tests/portal-core.spec.js)

<a id="cfg-02"></a>

## CFG-02 值、备注与特殊内容

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：CFG-01已发布基线

- [ ] **CFG-02.01** 修改String值和备注，包含中文、&、/、反斜杠、%、引号和多行。
  - 预期：保存和刷新后逐字一致，类型未变，不显示转义损坏。
- [ ] **CFG-02.02** 修改Number/Boolean/JSON的合法值并保存；刷新后再打开编辑。
  - 预期：类型和内容保留，编辑器显示与表格一致。

清理：撤销未发布修改回基线。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="cfg-03"></a>

## CFG-03 新增、修改、删除的撤销

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：application存在已发布基线

- [ ] **CFG-03.01** 新增临时Key、修改qa3ui.number、删除qa3ui.message，检查三种待发布状态。
  - 预期：新增/修改/删除状态与预览差异正确；未操作Key不变。
- [ ] **CFG-03.02** 点击撤销，先取消，再再次撤销并确认。
  - 预期：取消保留草稿；确认移除新增、恢复修改和删除，未修改项时间不变，无未发布项。

清理：确认基线恢复。

现有自动化：部分：core覆盖删除/撤销及请求；混合草稿、取消和未修改时间需补齐。

参考：[portal-core.spec.js](../tests/portal-core.spec.js)

<a id="cfg-04"></a>

## CFG-04 特殊Key精确编辑、删除和撤销

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：已发布qa3ui%2Fpath=percent、qa3ui/path=slash、qa3ui\path=backslash

- [ ] **CFG-04.01** 仅编辑qa3ui%2Fpath为临时值，保存并检查另两项；仅删除该Key并确认，再撤销。
  - 预期：每一步只命中目标，无400；另两项值和状态不变；撤销后恢复原发布值。
- [ ] **CFG-04.02** 仅编辑qa3ui/path为临时值，保存并检查另两项；仅删除该Key并确认，再撤销。
  - 预期：每一步只命中目标，无400；另两项值和状态不变；撤销后恢复原发布值。
- [ ] **CFG-04.03** 仅编辑qa3ui\path为临时值，保存并检查另两项；仅删除该Key并确认，再撤销。
  - 预期：每一步只命中目标，无400；另两项值和状态不变；撤销后恢复原发布值。

清理：三项恢复percent/slash/backslash，无草稿。

现有自动化：支撑：portal-item-delete是API测试，覆盖plain/slash/backslash删除隔离，不含字面量%2F，也不验证UI编辑/撤销。

参考：[portal-item-delete.spec.js](../tests/portal-item-delete.spec.js)

<a id="cfg-05"></a>

## CFG-05 配置过滤、排序、修改时间和删除行身份

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：基线已发布；普通用户{{TEST_USER}}已创建；临时修改授权由AUTH-02提供

- [ ] **CFG-05.01** 过滤qa3ui.number，清除过滤，再分别切换Key和最后修改时间升降序。
  - 预期：过滤及恢复准确，排序按实际值和时间；日期为可读有效时间，未修改项时间不漂移。
- [ ] **CFG-05.02** 普通用户修改并删除已发布qa3ui.audit-user，管理员与普通用户分别查看删除待发布行。
  - 预期：显示普通用户真实显示名和用户名及有效时间，不是只有括号/用户名；之后撤销。
- [ ] **CFG-05.03** 普通用户在消费者删除qa3ui.shared=client-override覆盖，查看对应删除行，再撤销。
  - 预期：公共覆盖删除行也显示完整身份和日期，撤销恢复覆盖。

清理：撤销草稿；撤销临时角色。

现有自动化：部分：openapi-fields覆盖主/灰度日期渲染、排序和预览；普通用户删除行及公共覆盖身份需补齐。

参考：[portal-openapi-fields.spec.js](../tests/portal-openapi-fields.spec.js)

<a id="cfg-06"></a>

## CFG-06 文本编辑保持类型与行号变化

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：application已发布Number99、Boolean true、JSON对象和String

- [ ] **CFG-06.01** 切换文本模式，将number改为100，在Boolean/JSON之前增加注释和空行后保存。
  - 预期：Number仍为Number，Boolean/JSON类型不变，注释与空行正确解析。
- [ ] **CFG-06.02** 再增加一个普通Key并修改String，切回表格、刷新后检查，再撤销。
  - 预期：新增/修改对应准确，不误删其他Key；撤销恢复Number99和其余发布值。

清理：清理文本草稿。

现有自动化：部分：priority验证文本改值/发布；PropertyResolverTypeTest补充类型保留，Chrome注释/移行分支需补齐。

参考：[portal-priority.spec.js](../tests/portal-priority.spec.js)、[PropertyResolverTypeTest.java](../../../apollo-portal/src/test/java/com/ctrip/framework/apollo/portal/component/txtresolver/PropertyResolverTypeTest.java)

<a id="cfg-07"></a>

## CFG-07 跨集群同步与比较

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：default与{{CLUSTER}}都存在；源有至少两项含Number类型的配置

- [ ] **CFG-07.01** 选择两项同步到{{CLUSTER}}，查看预览并确认，刷新目标。
  - 预期：只同步所选项，Key/值/备注/类型准确；源集群不变。
- [ ] **CFG-07.02** 打开集群配置比较，切换表格/文本、筛选Key、显示/隐藏相同项。
  - 预期：差异和相同项符合两边实际配置；筛选和视图切换不修改配置。

清理：目标发布或撤销至计划基线，记录所选方式。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="cfg-08"></a>

## CFG-08 视图与工具栏

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：配置面板有基线数据

- [ ] **CFG-08.01** 切换到表格视图，检查工具栏并返回。
  - 预期：对应内容正常加载；实例/历史视图不出现误用的配置写操作，配置表格操作可恢复。
- [ ] **CFG-08.02** 切换到文本视图，检查工具栏并返回。
  - 预期：对应内容正常加载；实例/历史视图不出现误用的配置写操作，配置表格操作可恢复。
- [ ] **CFG-08.03** 切换到更改历史视图，检查工具栏并返回。
  - 预期：对应内容正常加载；实例/历史视图不出现误用的配置写操作，配置表格操作可恢复。
- [ ] **CFG-08.04** 切换到实例列表视图，检查工具栏并返回。
  - 预期：对应内容正常加载；实例/历史视图不出现误用的配置写操作，配置表格操作可恢复。

清理：恢复表格视图。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="cfg-09"></a>

## CFG-09 展开、全屏、Esc与刷新

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：properties与json面板均有内容

- [ ] **CFG-09.01** 收缩再展开两个Namespace；进入properties全屏后退出。
  - 预期：布局和内容正确，其他面板状态不受影响。
- [ ] **CFG-09.02** 进入JSON全屏，按真实Esc退出，再刷新。
  - 预期：浏览器退出全屏，面板/工具栏恢复，内容不变，无卡住或空白。

清理：退出全屏，恢复表格。

现有自动化：支撑：test_namespaceFullscreen.js覆盖全屏状态控制；实际Esc和布局需Chrome验证。

参考：[test_namespaceFullscreen.js](../../../apollo-portal/src/test/resources/static/scripts/test_namespaceFullscreen.js)

<a id="fmt-01"></a>

## FMT-01 YAML/YML编辑发布与撤销

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：NS-02对应格式已创建

- [ ] **FMT-01.01** 打开qa3ui-yaml.yaml，粘贴夹具yaml内容，保存并发布；修改后保存，再撤销。
  - 预期：初次发布成功，内容/缩进/中文及特殊字符不损坏；撤销恢复发布内容。
- [ ] **FMT-01.02** 打开qa3ui-yml.yml，粘贴夹具yml内容，保存并发布；修改后保存，再撤销。
  - 预期：初次发布成功，内容/缩进/中文及特殊字符不损坏；撤销恢复发布内容。

清理：无文件草稿，保留已发布内容。

现有自动化：部分：configservice覆盖yaml保存及读取；yml和文件撤销需补齐。

参考：[portal-configservice.spec.js](../tests/portal-configservice.spec.js)

<a id="fmt-02"></a>

## FMT-02 JSON文件与值格式化

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：qa3ui-json.json及JSON类型配置项存在

- [ ] **FMT-02.01** 保存、发布有效JSON文件，修改后保存再撤销。
  - 预期：内容和格式可读，撤销恢复已发布JSON。
- [ ] **FMT-02.02** 打开JSON类型值详情，切换格式化/原始视图；编辑保存后再查看。
  - 预期：嵌套结构、布尔值、中文和特殊字符一致；两视图切换不改写存储值。

清理：撤销临时修改。

现有自动化：部分：configservice覆盖json文件读值；格式化/原始值切换和撤销需补齐。

参考：[portal-configservice.spec.js](../tests/portal-configservice.spec.js)

<a id="fmt-03"></a>

## FMT-03 非法JSON/YAML保护已存内容

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：有效JSON和YAML内容已发布

- [ ] **FMT-03.01** JSON文件输入缺少括号或逗号的内容并保存。
  - 预期：保存拒绝且提示原因；取消/刷新后原有效内容保留。
- [ ] **FMT-03.02** YAML文件输入明确语法错误（例如未闭合的序列）并保存。
  - 预期：保存拒绝且错误可见；取消/刷新后原内容不受影响。

清理：丢弃编辑器中非法输入。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="fmt-04"></a>

## FMT-04 XML/TXT编辑发布与撤销

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：NS-02对应格式已创建

- [ ] **FMT-04.01** 打开qa3ui-xml.xml，粘贴夹具xml内容，保存并发布；修改后保存，再撤销。
  - 预期：初次发布成功，内容/缩进/中文及特殊字符不损坏；撤销恢复发布内容。
- [ ] **FMT-04.02** 打开qa3ui-txt.txt，粘贴夹具txt内容，保存并发布；修改后保存，再撤销。
  - 预期：初次发布成功，内容/缩进/中文及特殊字符不损坏；撤销恢复发布内容。

清理：无文件草稿，保留已发布内容。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="rel-01"></a>

## REL-01 首次发布与差异预览

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：application有待发布配置

- [ ] **REL-01.01** 点击发布，核对新增项差异；先取消预览。
  - 预期：预览显示准确Key、已发布值/待发布值、操作者和有效时间；取消不发布。发布配置保存Key/值，预览不要求展示编辑时的类型；类型校验由CFG-01等编辑用例覆盖。
- [ ] **REL-01.02** 填写唯一版本名ui-{{RUN_ID}}-v1和备注后确认，再刷新。
  - 预期：发布成功，全部目标项已发布，历史有版本名/备注/操作者/时间。

清理：保留v1供后续历史比较。

现有自动化：部分：core覆盖创建和首次发布；openapi-fields检查预览日期；取消预览及全字段展示需补齐。

参考：[portal-core.spec.js](../tests/portal-core.spec.js)、[portal-openapi-fields.spec.js](../tests/portal-openapi-fields.spec.js)

<a id="rel-02"></a>

## REL-02 二次发布及历史比较

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：REL-01完成

- [ ] **REL-02.01** 修改qa3ui.number为不同值并发布ui-{{RUN_ID}}-v2。
  - 预期：预览仅列预期差异，发布后新值生效。
- [ ] **REL-02.02** 发布历史切换v1/v2，查看变更配置和全部配置；加载更多历史。
  - 预期：旧值/新值、备注、操作者和日期正确；列表追加不重复、不丢失。

清理：记录v1/v2值和版本名。

现有自动化：部分：core覆盖第二次发布、版本和历史记录；视图切换与加载更多需补齐。

参考：[portal-core.spec.js](../tests/portal-core.spec.js)

<a id="rel-03"></a>

## REL-03 两种回滚入口

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：至少两个不同的发布版本，必要时重新发布第三版

- [ ] **REL-03.01** 历史页选择指定旧版本，检查差异并确认回滚。
  - 预期：当前生效值回退到所选版本，回滚历史记录正确；草稿行为与页面提示一致。
- [ ] **REL-03.02** 发布新版本，再用配置页顶部快捷回滚；先取消一次，再确认。
  - 预期：取消不影响版本；确认回退到上一版本，当前值与历史一致。

清理：发布/撤销到明确基线，清除无关草稿。

现有自动化：部分：core覆盖指定版本/最新回滚；configservice检查生效值；取消和草稿呈现需补齐。

参考：[portal-core.spec.js](../tests/portal-core.spec.js)、[portal-configservice.spec.js](../tests/portal-configservice.spec.js)

<a id="rel-04"></a>

## REL-04 更改历史与筛选

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：已完成新增/修改/删除及发布操作；至少有一次同时修改qa3ui.number与qa3ui.message的提交，以及不含qa3ui.number的独立提交

- [ ] **REL-04.01** 打开更改历史，分别查看新增、修改和删除记录。
  - 预期：变更类型（新增/修改/删除）、旧值、新值、备注、操作者和时间准确。
- [ ] **REL-04.02** 按qa3ui.number筛选，再清除筛选。
  - 预期：只列出包含目标Key的变更批次，同时保留命中批次中其他Key的完整变更，便于查看同次提交的上下文；不含目标Key的批次被排除。清除后完整列表恢复，可继续翻页。

清理：清除筛选条件。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="gray-01"></a>

## GRAY-01 创建灰度与配置隔离

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：application有已发布主版本，number=42；当前无灰度

- [ ] **GRAY-01.01** 创建灰度，覆盖number=99并新增qa3ui.gray=canary。
  - 预期：主/灰度面板可切换，主版本仍42，灰度显示99和新Key。
- [ ] **GRAY-01.02** 查看灰度配置元数据并切换排序。
  - 预期：身份和时间非空、格式正确，排序正确，无主版本串数据。

清理：保留灰度供后续用例。

现有自动化：部分：priority覆盖灰度链路，openapi-fields覆盖灰度时间/排序；完整隔离页面检查仍需执行。

参考：[portal-priority.spec.js](../tests/portal-priority.spec.js)、[portal-openapi-fields.spec.js](../tests/portal-openapi-fields.spec.js)

<a id="gray-02"></a>

## GRAY-02 私有单条/公共多App灰度规则维护

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：GRAY-01完成；{{PUBLIC_NS}}也准备灰度；主/消费者两个App用于公共Namespace规则检查

- [ ] **GRAY-02.01** 私有灰度为当前App保存一条规则，包含两个测试IP和canary、blue标签；刷新后修改一个IP、移除一个标签，核对其余值；删除整条规则并刷新，再重建。
  - 预期：当前App仅保存一条规则，已有规则时不提供第二条新增入口；修改后的IP/标签准确，未操作值保留；删除后为空，重建成功并持久化。
- [ ] **GRAY-02.02** 公共Namespace灰度为主/消费者两个App各建规则，依次增改删并刷新。
  - 预期：各App规则独立，未操作App的所有IP/标签保留。

清理：保留至少一条含canary、blue标签的规则供历史验证。

现有自动化：部分：openapi-fields实际增改删规则并经AdminService核对保留项；私有/公共两个页面路径均需执行本手册。

参考：[portal-openapi-fields.spec.js](../tests/portal-openapi-fields.spec.js)、[portal-priority.spec.js](../tests/portal-priority.spec.js)

<a id="gray-03"></a>

## GRAY-03 灰度发布历史完整展示

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：灰度有配置变更及IP/标签规则

- [ ] **GRAY-03.01** 发布灰度，进入该次发布历史查看规则。
  - 预期：配置差异与灰度值正确；历史同时展示AppId、全部IP、canary/blue等全部标签。
- [ ] **GRAY-03.02** 创建或查找一次不含标签的本次测试灰度发布记录，再打开历史。
  - 预期：空标签正常展示，不报错，IP仍正确。

清理：保留灰度供合并测试。

现有自动化：部分：priority覆盖灰度发布；历史标签列和无标签兼容需Chrome检查。

参考：[portal-priority.spec.js](../tests/portal-priority.spec.js)

<a id="gray-04"></a>

## GRAY-04 全量发布的阻止、保留和删除分支

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：有已发布灰度

- [ ] **GRAY-04.01** 主分支制造未发布修改，尝试全量发布灰度。
  - 预期：明确阻止冲突操作，主和灰度数据未丢失；撤销主草稿。
- [ ] **GRAY-04.02** 合并灰度并选择保留灰度，核对主配置与分支。
  - 预期：主版本取得灰度值，灰度仍存在且状态正确。
- [ ] **GRAY-04.03** 再次修改/发布灰度，合并时选择删除灰度。
  - 预期：主版本合并正确，灰度面板/分支消失，历史可追踪。

清理：主版本无草稿，无灰度残留。

现有自动化：部分：priority覆盖合并；configservice核对生效值；主草稿拦截及保留/删除两分支需补齐。

参考：[portal-priority.spec.js](../tests/portal-priority.spec.js)、[portal-configservice.spec.js](../tests/portal-configservice.spec.js)

<a id="gray-05"></a>

## GRAY-05 放弃私有和公共灰度

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：主版本已发布

- [ ] **GRAY-05.01** 为私有applicationNamespace新建测试灰度；放弃时先取消，再确认。
  - 预期：取消保留灰度；确认后灰度消失，主版本配置和值不受影响。
- [ ] **GRAY-05.02** 为公共{{PUBLIC_NS}}Namespace新建测试灰度；放弃时先取消，再确认。
  - 预期：取消保留灰度；确认后灰度消失，主版本配置和值不受影响。

清理：私有/公共均不保留灰度。

现有自动化：部分：priority覆盖放弃分支；私有/公共与取消确认均需Chrome补齐。

参考：[portal-priority.spec.js](../tests/portal-priority.spec.js)

<a id="ins-01"></a>

## INS-01 无实例空态与视图

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：使用无客户端访问的独立测试Namespace

- [ ] **INS-01.01** 实例页分别打开最新、非最新、全部并刷新。
  - 预期：都正常加载明确空态，不出现接口错误或遗留计数。
- [ ] **INS-01.02** 打开无客户端命中的灰度实例页，核对总数和空态，刷新后再返回配置视图。
  - 预期：灰度实例总数为0，空态明确，无接口错误或遗留计数，返回配置视图正常；灰度页不要求提供最新/非最新/全部切换入口。

清理：空态验证完成后再启动真实SDK。

现有自动化：部分：regression验证实例路径可达；各视图空态/灰度空态需补齐。

参考：[portal-regression.spec.js](../tests/portal-regression.spec.js)

<a id="ins-02"></a>

## INS-02 真实SDK、版本迁移与灰度实例

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：SDK夹具可运行；主number=99已发布；记录客户端实际IP

- [ ] **INS-02.01** 启动真实SDK读取99，刷新最新/全部实例，检查IP、获取时间和版本。
  - 预期：最新1、旧0、全部1（若有多实例按本次IP筛选）；值与已发布版本一致，时间有效。
- [ ] **INS-02.02** 停止客户端，再发布number=100，查看非最新实例并点击其版本链接。
  - 预期：本次IP归旧版本，旧版本详情仍是99；不是只检查API返回200。
- [ ] **INS-02.03** 建立number=101灰度，规则选择本次实例IP，发布；重启同一SDK后查看灰度实例。
  - 预期：SDK实际读101，灰度实例归属、时间和版本正确。
- [ ] **INS-02.04** 放弃灰度，等待客户端读取主版本并刷新实例页。
  - 预期：SDK读取100，实例归主版本最新；旧/最新/全部计数一致。

清理：停止SDK；放弃灰度；记录缓存目录；实例历史按系统保留期自然保留。

现有自动化：部分：openapi-fields用真实ConfigService HTTP请求创建实例并检查版本/时间；不等价于本手册真实SDK进程和灰度完整链路。

参考：[portal-openapi-fields.spec.js](../tests/portal-openapi-fields.spec.js)、[portal-configservice.spec.js](../tests/portal-configservice.spec.js)

<a id="auth-01"></a>

## AUTH-01 权限页、用户搜索和最后管理员保护

入口：`/app/setting.html?#/appid={{APP_ID}}`

前置条件：管理员及普通用户{{TEST_USER}}已存在

- [ ] **AUTH-01.01** 分别打开应用、Namespace、集群权限页，通过真实搜索查找普通用户。
  - 预期：目标应用/环境/集群正确；搜索结果含用户ID、显示名和邮箱。
- [ ] **AUTH-01.02** 仅剩原管理员时尝试移除该最后应用管理员。
  - 预期：明确拒绝，管理员保留；不修改系统管理员身份。

清理：恢复无临时授权基线。

现有自动化：部分：priority覆盖角色页面；auth-matrix包含真实用户搜索；最后管理员保护需补齐。

参考：[portal-priority.spec.js](../tests/portal-priority.spec.js)、[portal-auth-matrix.spec.js](../tests/portal-auth-matrix.spec.js)

<a id="auth-02"></a>

## AUTH-02 角色授予和撤销

入口：`/namespace/role.html?#/appid={{APP_ID}}&namespaceName=application`

前置条件：普通用户无本次资源角色；管理员普通窗口，普通用户独立隐身窗口

- [ ] **AUTH-02.01** 逐一授予{{ENV}}修改、发布角色，刷新角色页；每项验证后撤销再刷新。
  - 预期：添加与移除持久化，角色与环境准确；原管理员不受影响。
- [ ] **AUTH-02.02** 授予普通用户应用管理员；普通用户刷新检查添加集群/Namespace入口；撤销后再刷新。
  - 预期：管理员入口随授予出现，撤销后失效。
- [ ] **AUTH-02.03** 在测试集群和公共Namespace权限页进行授权/撤销。
  - 预期：权限范围正确，其他环境/Namespace角色不被修改。

清理：撤销本次所有角色，保留原管理员；不得遗留普通用户为应用管理员。

现有自动化：部分：priority覆盖Namespace角色增删，但部分Select2选择通过JS设置；auth-matrix才含真实搜索。应用/集群及独立身份需补齐。

参考：[portal-priority.spec.js](../tests/portal-priority.spec.js)、[portal-auth-matrix.spec.js](../tests/portal-auth-matrix.spec.js)

<a id="auth-03"></a>

## AUTH-03 第二身份只读、编辑和发布隔离

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：普通用户独立会话；可由管理员临时授予单一角色

- [ ] **AUTH-03.01** 无角色登录，查看配置并检查新增、编辑、发布入口及实际可达操作。
  - 预期：配置只读，显示申请权限；不能完成修改或发布。
- [ ] **AUTH-03.02** 只授予修改角色：修改一个值并保存，尝试发布。
  - 预期：修改成功；即使发布按钮可见，实际发布也明确拒绝无权限。
- [ ] **AUTH-03.03** 撤销修改、只授予发布：管理员预置待发布变更，普通用户尝试编辑，再完成发布。
  - 预期：不能编辑；可以查看预览并发布，版本操作者为普通用户。
- [ ] **AUTH-03.04** 撤销所有角色后刷新同一会话，再尝试此前动作。
  - 预期：权限收回生效，不能继续写入或发布；管理员会话仍有效。

清理：撤销授权和草稿；记录真实发布版本，普通用户退出。

现有自动化：支撑：priority覆盖超级管理员无显式角色仍可操作；auth-matrix覆盖登录；均不能替代普通用户仅修改/仅发布的权限矩阵。

参考：[portal-priority.spec.js](../tests/portal-priority.spec.js)、[portal-auth-matrix.spec.js](../tests/portal-auth-matrix.spec.js)

<a id="key-01"></a>

## KEY-01 访问密钥生命周期与审计

入口：`/app/access_key.html?#/appid={{APP_ID}}`

前置条件：本次应用无其他在用客户端密钥

- [ ] **KEY-01.01** 查看默认状态，创建测试密钥，检查创建者/时间；观察明文后刷新。
  - 预期：创建状态正确，观察状态持久化；日期有效；明文不写入报告或截图。
- [ ] **KEY-01.02** 依次启用、刷新、禁用，再删除本次密钥。
  - 预期：状态持久化，修改人和时间正确；删除后列表无本次记录。

清理：清理本次密钥，确认没有影响非测试应用。

现有自动化：部分：priority覆盖密钥页面生命周期，openapi-fields检查审计时间；观察后刷新及明文不持久显示需完整核对。

参考：[portal-priority.spec.js](../tests/portal-priority.spec.js)、[portal-openapi-fields.spec.js](../tests/portal-openapi-fields.spec.js)

<a id="tok-01"></a>

## TOK-01 个人Token校验、范围选择和详情

入口：`/user-token.html`

前置条件：管理员会话；仅使用本次应用和{{ENV}}/application范围

- [ ] **TOK-01.01** 空名称提交；范围列表搜索本次AppId，依次移入单项/全部、移出/重置。
  - 预期：必填校验明确，选择列表无重复，重置恢复初始选择。
- [ ] **TOK-01.02** 创建只含config:read、QPS1、短有效期的受限Token，打开详情后刷新页面。
  - 预期：详情操作/环境/AppId/Namespace/QPS/到期时间准确；明文仅创建或轮换当次可见，刷新后不再显示。

清理：只记Token记录ID/名称，不存明文；交TOK-02清理。

现有自动化：支撑：test_userTokenStatus.js覆盖状态大小写兼容，不验证页面范围选择、校验和详情。

参考：[test_userTokenStatus.js](../../../apollo-portal/src/test/resources/static/scripts/test_userTokenStatus.js)

<a id="tok-02"></a>

## TOK-02 Token轮换、撤销和删除

入口：`/user-token.html`

前置条件：TOK-01已创建本次有效Token

- [ ] **TOK-02.01** 检查有效Token按钮并轮换，核对新旧记录及新Token范围。
  - 预期：有效状态允许轮换/撤销；旧Token变撤销，新Token保留限制，详情一致。
- [ ] **TOK-02.02** 对新Token撤销先取消，再确认，刷新。
  - 预期：取消无变化；确认后状态变撤销，轮换/撤销按钮不可再操作。
- [ ] **TOK-02.03** 删除已撤销的新旧测试Token并刷新。
  - 预期：记录全部消失；没有本次Token残留。

清理：清理所有轮换衍生记录和临时明文。

现有自动化：支撑：test_userTokenStatus.js覆盖状态归一，生命周期仍需Chrome执行。

参考：[test_userTokenStatus.js](../../../apollo-portal/src/test/resources/static/scripts/test_userTokenStatus.js)

<a id="tok-03"></a>

## TOK-03 管理员Token查询、详情和撤销

入口：`/user-token-admin.html`

前置条件：个人页另建本次短期受限Token

- [ ] **TOK-03.01** 按用户+有效状态查询，打开目标详情；使用不存在用户查空态，再重置。
  - 预期：筛选准确，详情完整，空态及重置正常。
- [ ] **TOK-03.02** 管理员撤销，切已撤销筛选；个人页刷新对照，再删除记录。
  - 预期：两页状态一致，撤销人/时间正确；撤销后动作禁用，删除后消失。

清理：删除本次管理员验证Token。

现有自动化：支撑：test_userTokenStatus.js覆盖状态归一，管理员查询/撤销与跨页一致性仍需Chrome执行。

参考：[test_userTokenStatus.js](../../../apollo-portal/src/test/resources/static/scripts/test_userTokenStatus.js)

<a id="open-01"></a>

## OPEN-01 开放平台列表、查询和必填

入口：`/open/manage.html`

前置条件：管理员会话；测试consumer尚未创建

- [ ] **OPEN-01.01** 打开列表检查已有consumer元信息，查询不存在的qa3ui-{{RUN_ID}}。
  - 预期：列表加载正常；显示名称、权限标志、QPS；不存在时明确提示。
- [ ] **OPEN-01.02** 打开创建和授权表单，缺少必填字段时尝试提交。
  - 预期：校验阻止不完整请求，不生成consumer或意外授权。

清理：取消空表单。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="open-02"></a>

## OPEN-02 受限consumer创建、授权和删除

入口：`/open/manage.html`

前置条件：仅对本次应用授权；本次consumer名称唯一

- [ ] **OPEN-02.01** 创建qa3ui-{{RUN_ID}} consumer，QPS1，关闭创建应用/管理用户能力。
  - 预期：创建成功，列表及刷新后的元信息符合输入。
- [ ] **OPEN-02.02** 仅授权{{APP_ID}}/{{ENV}}/application，观察结果。
  - 预期：页面明确提示成功，范围限定于本次资源；页面无既有授权详情入口时不得把详情/单独撤销记为已验证。
- [ ] **OPEN-02.03** 删除consumer先取消，再确认并刷新查询。
  - 预期：取消保留；确认后记录消失。

清理：删除本次consumer；不更改既有consumer。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="adm-01"></a>

## ADM-01 用户搜索与空表单校验

入口：`/user-manage.html`

前置条件：内置用户管理模式；管理员登录

- [ ] **ADM-01.01** 搜索{{TEST_USER}}或其他已知测试用户，搜索不存在用户，再清除。
  - 预期：结果准确，空态正常，清除恢复列表。
- [ ] **ADM-01.02** 打开创建表单，保持用户名和密码为空并尝试提交；再只填写本轮唯一的派生用户名，密码继续留空并提交；返回列表查询该用户名。
  - 预期：两次都被页面或服务明确拒绝，无残缺账号。此项不输入新凭据。

清理：退出空表单，不记录任何密码。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="adm-02"></a>

## ADM-02 系统权限管理查询

入口：`/system-role-manage.html`

前置条件：测试应用已存在

- [ ] **ADM-02.01** 按{{APP_ID}}查询应用权限信息，再查不存在应用。
  - 预期：现有角色信息准确；不存在时明确空态或错误；不修改全局管理权限。

清理：退出查询页。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="adm-03"></a>

## ADM-03 PortalDB/ConfigDB参数查询

入口：`/server_config_manage.html`

前置条件：管理员有参数查看权限

- [ ] **ADM-03.01** 分别选择PortalDB与ConfigDB，查询已知参数和不存在参数；ConfigDB切换{{ENV}}及cluster。
  - 预期：来源和环境/cluster明确，返回内容与筛选一致，空态正确。

清理：不修改既有系统参数。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="adm-04"></a>

## ADM-04 测试参数生命周期与cluster隔离

入口：`/server_config_manage.html`

前置条件：只使用qa3ui.{{RUN_ID}}前缀的新参数Key

- [ ] **ADM-04.01** PortalDB创建测试参数、编辑值/备注、刷新确认，再删除。
  - 预期：值和备注持久化，删除后查不到。
- [ ] **ADM-04.02** ConfigDB在两个不同cluster创建同Key不同值，分别编辑其中一项并查询。
  - 预期：按Key+cluster独立，另一项不变；环境选择正确。
- [ ] **ADM-04.03** 删除上述两项并重新查询各cluster。
  - 预期：本次参数全部清理，原有参数不受影响。

清理：记录并确认PortalDB及各ConfigDB cluster测试参数均不存在。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="adm-06"></a>

## ADM-06 测试用户创建、编辑和启停（凭据经接口录入）

入口：`/user-manage.html`

前置条件：内置用户管理模式；管理员页面和HTTP会话可用；创建使用尚不存在的本轮账号。若{{TEST_USER}}已经用于其他检查，按“环境、身份与变量”使用唯一派生账号；新凭据按“新凭据的接口录入与页面核对”处理

- [ ] **ADM-06.01** 在Chrome打开创建表单，核对字段并填写本轮唯一用户名、显示名“QA UI 普通用户”和测试邮箱；退出未提交表单。通过`POST /openapi/v1/users?isCreate=true`录入相同资料、本轮测试密码及启用状态；返回Chrome刷新查询、打开资料，并用该账号在独立会话登录。
  - 预期：创建成功，仅出现一条目标用户，用户名/显示名/邮箱准确且启用；资料持久化，新会话身份正确。执行方式记为Chrome+HTTP，不将接口写入记为创建表单提交通过。
- [ ] **ADM-06.02** 在Chrome打开本轮用户的编辑表单，核对当前资料，填写新的显示名和邮箱后退出未提交表单；通过`POST /openapi/v1/users?isCreate=false`写入相同变更，使用本轮已建立的测试密码并保留启用状态。刷新列表、重开资料，并在独立会话重新登录核对新名称。
  - 预期：新资料持久化，用户名和启用状态不变，登录展示新名称；编辑表单回显一致。执行方式记为Chrome+HTTP，不将接口写入记为编辑表单提交通过。
- [ ] **ADM-06.03** 禁用用户，独立窗口用正确密码新登录；随后启用再登录。
  - 预期：禁用后新登录失败，启用后恢复，管理页状态持久化。

清理：通过同一更新接口恢复本轮用户资料并传入本轮测试密码，Chrome刷新核对；验证结束撤销角色、退出会话并在页面禁用本轮一次性用户及派生账号，确认状态持久化；删除临时密码和HTTP会话文件。页面无删除能力时记录残留禁用账号。

现有自动化：支撑：UserControllerTest覆盖部分用户管理接口权限及创建调用；本组仍须按Chrome+HTTP步骤核对真实页面、登录和启停，不以单元测试代替。

参考：[UserService.js](../../../apollo-portal/src/main/resources/static/scripts/services/UserService.js)、[UserControllerTest.java](../../../apollo-portal/src/test/java/com/ctrip/framework/apollo/openapi/v1/controller/UserControllerTest.java)

<a id="adm-05"></a>

## ADM-05 系统服务信息与健康检查

入口：`/system_info.html`

前置条件：Portal/Admin/Config均已启动

- [ ] **ADM-05.01** 查看{{ENV}}的Active、Meta/Config/Admin地址，执行页面提供的健康检查。
  - 预期：环境Active=true，地址对应本次部署；各项检查显示健康，失败需记录实际返回。

清理：只读，无清理。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="aud-01"></a>

## AUD-01 审计查询、详情、分页和清空日期

入口：`/audit_log_menu.html`

前置条件：本次有多次同一操作名的记录，至少一次在待测日期区间外

- [ ] **AUD-01.01** 按操作名查询，打开一条详情，加载更多；再查询不存在操作。
  - 预期：列表/详情目标和时间正确，分页无重复，空态正常。
- [ ] **AUD-01.02** 选择包含部分记录的开始/结束时间，记录数量；清空两端再查询。
  - 预期：范围内结果正确；清空后恢复区间外记录，不残留隐藏筛选。
- [ ] **AUD-01.03** 分别仅清空开始或结束时间；再清空并继续加载更多。
  - 预期：仅保留开始时间时返回不早于该时刻的同名操作，仅保留结束时间时返回不晚于该时刻的同名操作，包含边界且保留已知符合条件的记录；重新查询后分页重置，后续页继续使用当前日期条件。两端清空后恢复无时间限制的同名操作结果。

清理：清空日期和操作筛选。

现有自动化：支撑：test_auditLogDates.js覆盖清空单端/双端和分页参数；ApolloAuditLogServiceTest通过真实H2/JPA查询覆盖四种日期组合、包含边界、操作名隔离及分页。页面日期控件及结果渲染仍需Chrome执行。

参考：[test_auditLogDates.js](../../../apollo-portal/src/test/resources/static/scripts/test_auditLogDates.js)、[ApolloAuditLogServiceTest.java](../../../apollo-audit/apollo-audit-impl/src/test/java/com/ctrip/framework/apollo/audit/service/ApolloAuditLogServiceTest.java)

<a id="exp-01"></a>

## EXP-01 应用及单Namespace导出

入口：`/config_export.html`

前置条件：本次应用有properties类型/特殊Key及文件格式数据

- [ ] **EXP-01.01** 按应用集群查询{{APP_ID}}/{{ENV}}/{{CLUSTER}}，导出ZIP并检查下载文件。
  - 预期：下载成功，命名和ZIP路径正确；内容包含目标集群/Namespace，类型、值、备注及%2F Key完整。
- [ ] **EXP-01.02** 配置页导出单application Namespace，检查文件。
  - 预期：文件为Apollo item数组格式，虽扩展名.properties但内容是JSON；与目标数据逐项一致。

清理：将本次下载保存在执行目录，避免包含非测试配置。

现有自动化：部分：regression下载应用ZIP并检查文件名；没有验证ZIP内容或单Namespace导出。

参考：[portal-regression.spec.js](../tests/portal-regression.spec.js)

<a id="exp-03"></a>

## EXP-03 按环境导出

入口：`/config_export.html`

前置条件：本次测试环境已准备应用、集群和Namespace元数据

- [ ] **EXP-03.01** 按环境操作中只选择{{ENV}}导出，检查ZIP结构及本次应用条目。
  - 预期：包含预期应用/集群/AppNamespace元数据和配置；路径与应用导出层次区分正确。
- [ ] **EXP-03.02** 检查文件格式配置、公共Namespace和关联配置是否按实际数据导出。
  - 预期：格式/内容/类型完整，不遗漏本次数据；若环境含其他应用，证据只保留测试条目。

清理：保护完整环境导出内容；报告只附脱敏的本次测试条目。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="exp-02"></a>

## EXP-02 正常导入、冲突处理及部分失败

入口：`/config_export.html`

前置条件：{{CLUSTER}}/application已发布7项基线，Number99；生成本次fixtures

- [ ] **EXP-02.01** 单Namespace菜单导入partial-failure.properties。
  - 预期：显示导入失败、qa3ui.invalid-type和部分写入提示；有效Boolean可能已写入，无效项不存在，撤销后恢复。
- [ ] **EXP-02.02** 导入corrected.properties，检查两项及类型。
  - 预期：成功，Boolean和String准确；撤销临时项。
- [ ] **EXP-02.03** 应用集群页上传partial-failure-app.zip并选覆盖；再对环境页刷新后直接选覆盖上传partial-failure-all.zip。
  - 预期：两个入口均报失败且保留Namespace/Key及部分写入提示；不得只看到绿色成功。分别撤销部分写入。
- [ ] **EXP-02.04** 目标number改777；刷新环境导入页，勾选{{ENV}}，直接选覆盖上传normal-all.zip。
  - 预期：刷新目标为Number99，其余Boolean/JSON/%2F不损坏；覆盖确实执行。
- [ ] **EXP-02.05** 目标再次改777；环境页选跳过导入同ZIP。
  - 预期：目标仍Number777，证明跳过生效。
- [ ] **EXP-02.06** 应用页查询{{APP_ID}}/{{ENV}}/{{CLUSTER}}，用normal-app.zip分别验证跳过与覆盖。
  - 预期：跳过保留777；覆盖恢复99，其他类型/值完整。
- [ ] **EXP-02.07** 应用页选覆盖→环境页选跳过，切回应用页；再环境页选覆盖→应用页；每次观察选中态并执行相应导入。
  - 预期：两页选项一致；实际请求/最终配置符合当前可见策略，不遗留父/子作用域旧值。

清理：撤销所有部分写入与777等草稿；恢复7项已发布基线。

现有自动化：支撑：ConfigsImportServiceTest覆盖后端错误传播；test_configImportConflictAction.js覆盖模板绑定/请求策略。两者均不能替代文件选择、提示和真实数据变化。

参考：[test_configImportConflictAction.js](../../../apollo-portal/src/test/resources/static/scripts/test_configImportConflictAction.js)、[ConfigsImportServiceTest.java](../../../apollo-portal/src/test/java/com/ctrip/framework/apollo/portal/service/ConfigsImportServiceTest.java)

<a id="search-01"></a>

## SEARCH-01 全局Value搜索与分页

入口：`/config.html?#/appid={{APP_ID}}`

前置条件：测试配置中存在可区分Key/Value，足够多结果触发分页

- [ ] **SEARCH-01.01** 打开全局配置搜索，用Key+Value组合搜索并点击结果。
  - 预期：命中正确值，链接到正确应用/环境/集群/Namespace。
- [ ] **SEARCH-01.02** 切换分页/页大小及过滤条件，再查不存在值。
  - 预期：结果条数和分页一致；空态正确，无上一搜索残留。

清理：删除为分页临时新增的配置并恢复基线。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="del-01"></a>

## DEL-01 测试Namespace、集群和应用清理

入口：`/delete_app_cluster_namespace.html`

前置条件：其他测试和证据已完成；仅可操作本次资源清单中的目标

- [ ] **DEL-01.01** 配置页删除本次空Namespace，先取消再确认。
  - 预期：取消保留；确认后面板和查询中消失。
- [ ] **DEL-01.02** 创建独立临时集群、AppNamespace用于删除测试；管理员工具分别查询、取消删除、确认删除。
  - 预期：目标标识准确；取消仍存在；确认后查询不存在；其他测试数据不受影响。
- [ ] **DEL-01.03** 先清理消费者，再删除本次主应用及其他临时应用，刷新首页/集群列表。
  - 预期：删除对象仅属于本次run；列表与查询均无残留；若受引用限制，明确提示并先解除本次依赖。

清理：完成执行记录的所有清理核对；失败时保留准确资源ID供后续处理，不批量删除其他数据。

现有自动化：无对应完整自动化：按本手册通过Chrome执行。

<a id="session-01"></a>

## SESSION-01 登录、退出和独立会话

入口：`/signin`

前置条件：管理员普通窗口、普通用户隐身窗口；有效测试凭据由执行者提供

- [ ] **SESSION-01.01** 普通用户用正确密码登录，确认身份；检查普通窗口管理员身份。
  - 预期：两窗口身份独立，显示名/用户名正确。
- [ ] **SESSION-01.02** 退出普通用户，直接访问此前配置URL；再重新登录。
  - 预期：退出成功，受保护页回登录页，重新登录恢复访问。
- [ ] **SESSION-01.03** 用户启用时用错误密码和不存在用户名登录；另核对禁用用户正确密码登录。
  - 预期：均明确拒绝，不产生已登录会话；不能把禁用导致失败当作错误密码用例。

清理：普通用户退出；管理员恢复起始页面，不在记录保存密码。

现有自动化：部分：core覆盖正常登录；auth-matrix在LDAP/OIDC下覆盖有效/无效登录，不能替代内置认证的禁用用户和退出后保护页检查。

参考：[portal-core.spec.js](../tests/portal-core.spec.js)、[portal-auth-matrix.spec.js](../tests/portal-auth-matrix.spec.js)
