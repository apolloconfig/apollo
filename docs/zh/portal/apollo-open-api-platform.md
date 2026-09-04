### 一、 什么是开放平台？

Apollo提供了一套的Http REST接口，使第三方应用能够自己管理配置。虽然Apollo系统本身提供了Portal来管理配置，但是在有些情景下，应用需要通过程序去管理配置。

### 二、 第三方应用接入Apollo开放平台

#### 2.1 注册第三方应用

第三方应用负责人需要向Apollo管理员提供一些第三方应用基本信息。

基本信息如下：

* 第三方应用的AppId、应用名、部门
* 第三方应用负责人

Apollo管理员在 `http://{portal_address}/open/add-consumer.html` 创建第三方应用，创建之前最好先查询此AppId是否已经创建。创建成功之后会生成一个token，如下图所示：

![开放平台管理](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-open-manage.png)

#### 2.2 查看第三方应用
Apollo管理员在 `http://{portal_address}/open/manage.html` 页面可以查看第三方应用列表。并提供了【查看Token并赋权】、【删除】等管理操作，如下图所示：

![第三方应用列表](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-open-manage-list.png)

【查看Token并赋权】的模态框页面如下图所示：

![查看Token并赋权](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-open-manage-token.png)

#### 2.3 给已注册的第三方应用授权
第三方应用不应该能操作任何Namespace的配置，所以需要给token绑定可以操作的Namespace。Apollo管理员在 `http://{portal_address}/open/add-consumer.html` 页面给token赋权。赋权之后，第三方应用就可以通过Apollo提供的Http REST接口来管理已授权的Namespace的配置了。

从 Apollo 3.0.0 开始，如果管理员为该第三方应用勾选了**允许管理用户？**（`ManageUsers`），Consumer Token 也可以调用用户管理 Open API。未授予该权限时，用户查询/创建/更新/启用/禁用请求会返回 HTTP 403。使用 Consumer Token 调用用户管理或权限管理的写接口时，需要传入有效的 `operator` 查询参数（已存在的 Portal 用户）。

#### 2.4 第三方应用调用Apollo Open API

##### 2.4.1 调用Http REST接口
任何语言的第三方应用都可以调用Apollo的Open API，在调用接口时，需要设置注意以下两点：
 * Http Header中增加一个Authorization字段，字段值为申请的token
 * Http Header的Content-Type字段需要设置成application/json;charset=UTF-8

##### 2.4.2 Java应用通过apollo-openapi调用Apollo Open API
从1.1.0版本开始，Apollo提供了[apollo-openapi](https://github.com/apolloconfig/apollo/tree/master/apollo-openapi)客户端，所以Java语言的第三方应用可以更方便地调用Apollo Open API。

首先引入`apollo-openapi`依赖：
```xml
<dependency>
    <groupId>com.ctrip.framework.apollo</groupId>
    <artifactId>apollo-openapi</artifactId>
    <version>1.7.0</version>
</dependency>
```

在程序中构造`ApolloOpenApiClient`：
```java
String portalUrl = "http://localhost:8070"; // portal url
String token = "e16e5cd903fd0c97a116c873b448544b9d086de9"; // 申请的token
ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder()
                                                .withPortalUrl(portalUrl)
                                                .withToken(token)
                                                .build();
```

后续就可以通过`ApolloOpenApiClient`的接口直接操作Apollo Open API了，接口说明参见下面的Rest接口文档。

##### 2.4.3 .Net core应用调用Apollo Open API

.Net core也提供了open api的客户端，详见https://github.com/ctripcorp/apollo.net/pull/77

##### 2.4.4 Shell Scripts调用Apollo Open API

封装了bash的function，底层使用curl来发送HTTP请求

* bash函数：[openapi.sh](https://github.com/apolloconfig/apollo/blob/master/scripts/openapi/bash/openapi.sh)

* 使用示例：[openapi-usage-example.sh](https://github.com/apolloconfig/apollo/blob/master/scripts/openapi/bash/openapi-usage-example.sh)
* 全部和openapi有关的shell脚本在文件夹 https://github.com/apolloconfig/apollo/tree/master/scripts/openapi/bash 下

#### 2.5 用户访问 Token

除第三方应用 Token 外，Apollo 还支持用户在 Portal 的 `访问 Token` 页面创建代表自己身份的用户访问 Token。用户访问 Token 适合 AI Agent、自动化脚本等以“当前用户”身份调用 Apollo API 的场景。

用户访问 Token 的权限不会复制用户角色，而是在每次请求时实时计算：

* 当前用户拥有的权限
* Token 自身配置的操作范围、AppId、环境和 Namespace 范围
* Token 未过期且未被撤销

实际可执行权限为以上条件的交集。因此，当用户权限被收回、用户被禁用、Token 过期或 Token 被撤销后，请求会立即失效或降权。

调用时使用标准 Bearer 认证：

```bash
curl -H "Authorization: Bearer apollo_pat_xxx_xxx" \
     -H "Content-Type: application/json;charset=UTF-8" \
     "http://{portal_address}/openapi/v1/user"
```

用户访问 Token 只用于 Open API 调用，不会作为 Portal 页面或 legacy WebAPI 的登录凭证使用。

固定前缀 `apollo_pat_` 用于识别 Portal 用户访问 Token。Open API 请求如果携带
`Authorization: Bearer apollo_pat_...`，会优先进入用户 Token 鉴权；历史第三方应用
Consumer Token 不使用这个前缀，仍走原有 Consumer Token 鉴权流程。

AI Agent 或自动化脚本可以先调用当前 Token 能力查询接口，确认当前身份和可用范围：

```bash
curl -H "Authorization: Bearer apollo_pat_xxx_xxx" \
     "http://{portal_address}/openapi/v1/user-tokens/current"
```

等价路径包括 `/openapi/v1/user-tokens/whoami` 和
`/openapi/v1/user-tokens/current/capabilities`。响应中包含当前用户、Token 前缀、过期时间、限流值，以及
`operations`、`appIds`、`envs`、`namespaces` 等范围。`allOperations`、`allApps`、`allEnvs`、
`allNamespaces` 为 `true` 时表示对应范围未被 Token 进一步限制。

响应还包含 `actions` 接口能力列表，供 AI Agent 或自动化客户端直接发现当前 Token 可调用的
Open API。每个 action 包含：

字段名 | 说明
--- | ---
id | 稳定的能力标识，如 `item.list`、`release.create`
method | HTTP 方法
path | Open API 路径模板
requiredOperations | 调用该接口需要的 Token 操作范围；多个值表示满足其中任意一个即可
grantedOperations | 当前 Token 实际命中的操作范围；用于区分可替代权限和当前已授予权限
operationMatch | `ANY` 表示 `requiredOperations` 任意匹配，`NONE` 表示只要求有效用户 Token
resourceScope | 该接口主要受哪些资源范围约束，如 `app`、`namespace`、`item`、`release`
description | 面向客户端展示或规划用的简短说明

Portal 创建 Token 时展示的“操作范围”和 `actions.requiredOperations` 使用同一组 operation 字符串，
例如 `config:read`、`config:modify`、`config:release`、`namespace:create`、`namespace:delete`、
`cluster:create`、`app:manage-role`、`user:manage`、`app:create`。`actions` 已经按当前 Token 的 `operations`
过滤；客户端仍需结合返回的 `appIds`、`envs`、`namespaces` 判断具体资源是否在授权范围内。

Apollo 3.0.0 起用户管理与权限管理 Open API 的授权边界如下：

* 使用 Consumer Token 调用用户管理接口时，需要该 Consumer 具备明确的 `ManageUsers` 权限。
* 使用用户访问 Token 调用用户管理接口时，需要 Token 所属用户当前具备 `ManageUsers` 权限，并且 Token 操作范围包含 `user:manage`。对用户访问 Token，创建/更新其他用户以及启用/禁用需要所属用户为 Portal 超级管理员，并且 Token 操作范围包含 `system:admin`（同时仍需 `user:manage`）。若没有 `system:admin`，即使所属用户是超级管理员、Token 仅有 `user:manage`，也会被当作非超级管理员，只能创建/更新 Token 所属用户本人且必须保持用户为启用状态。具备 `ManageUsers` 的 Consumer Token 不受这些额外用户 Token 限制。
* 使用 Consumer Token 调用权限管理时，Apollo 3.0.0 行为按接口区分：
  * 角色查询（`GET .../role-users`）：任意已认证的 Consumer Token 可查询任意 `appId` 的角色用户；没有应用级 Consumer 权限校验，也不要求 `ASSIGN_ROLE`。这是当前运行时行为，并不表示读接口按应用授权隔离。
  * Namespace / 环境 Namespace / 集群 Namespace 的授予与撤销：需要该 Consumer 对目标应用具备应用级分配角色权限（`ASSIGN_ROLE`）。鉴权在解析 `operator` 之前完成；`operator` 只用于标识写操作的审计操作人。
  * 应用级授予与撤销（`POST`/`DELETE /openapi/v1/apps/{appId}/roles/{roleType}`，含 `Master`）：Consumer Token 不支持（鉴权会抛出 `UnsupportedOperationException`，通常表现为 HTTP 500）；请改用通过 manage-app-master 鉴权的用户访问 Token（或 Portal 用户流程）。
* 使用用户访问 Token 调用权限管理时，需要所属用户对目标应用有相应权限，并且 Token 操作范围包含 `app:manage-role`，同时满足对应的 AppId / 环境 / Namespace 资源范围。角色查询额外要求对应资源上的分配角色权限；应用级授予/撤销额外要求 manage-app-master 权限。
* Consumer Token 的写操作需要传入有效的 `operator` 查询参数；用户访问 Token 会忽略 `operator`，并以 Token 所属用户作为操作人。

其中 `config:release` 只表示发布相关能力，例如 `release.create`、`release.gray-create`、
`release.gray-delete`、`release.rollback`。读取 release 内容、发布快照或 diff 可能暴露配置值，
因此 `release.latest`、`release.active-list`、`release.get`、`release.compare` 等读取类 action
使用 `config:read` 控制。

当一个 action 支持多个可替代权限时，`requiredOperations` 会保留全部可替代项，`grantedOperations`
只返回当前 Token 实际满足的项。例如某些应用管理 action 可由 `app:manage-role` 或 `system:admin`
满足，普通应用管理员 Token 会在 `grantedOperations` 中只看到 `app:manage-role`。

用户访问 Token 明文只会在创建或轮换时展示一次，Apollo 只保存 Token 哈希和前缀。用户可以在 Portal 页面查看前缀、过期时间、最后使用时间，并可撤销、轮换或删除 Token 记录。

### 三、 接口文档

#### 3.1 URL路径参数说明

参数名 | 参数说明
--- | ---
env | 所管理的配置环境
appId | 所管理的配置AppId
clusterName | 所管理的配置集群名， 一般情况下传入 default 即可。如果是特殊集群，传入相应集群的名称即可
namespaceName | 所管理的Namespace的名称，如果是非properties格式，需要加上后缀名，如`sample.yml`

#### 3.2 API接口列表

- [3.2.1 获取 App 的环境，集群信息](#_321-获取app的环境，集群信息)
- [3.2.2 获取 App 信息](#_322-获取app信息)
- [3.2.3 获取集群详细信息](#_323-获取集群接口)
- [3.2.4 创建集群](#_324-创建集群接口)
- [3.2.5 获取集群下所有 Namespace 信息](#_325-获取集群下所有namespace信息接口)
- [3.2.6 获取 Namespace 信息](#_326-获取某个namespace信息接口)
- [3.2.7 创建 Namespace](#_327-创建namespace)
- [3.2.8 获取 Namespace 当前编辑人](#_328-获取某个namespace当前编辑人接口)
- [3.2.9 获取具体配置项](#_329-读取配置接口)
- [3.2.10 新增配置项](#_3210-新增配置接口)
- [3.2.11 修改配置项](#_3211-修改配置接口)
- [3.2.12 删除配置项](#_3212-删除配置接口)
- [3.2.13 发布 Namespace](#_3213-发布配置接口)
- [3.2.14 获取 Namespace 最后一次发布的内容](#_3214-获取某个namespace当前生效的已发布配置接口)
- [3.2.15 回滚 Namespace](#_3215-回滚已发布配置接口)
- [3.2.16 分页获取配置项](#_3216-分页获取配置项接口) 
- [3.2.17 创建App并获取管理员权限](#_3217-创建App并获取管理员权限)
- [3.2.18 用户管理](#_3218-用户管理)
- [3.2.19 权限管理](#_3219-权限管理)
- [3.2.20 批量新增/修改/删除配置项](#3220-批量新增修改删除配置项)

##### 3.2.1 获取App的环境，集群信息

* **URL** : http://{portal_address}/openapi/v1/apps/{appId}/envclusters
* **Method** : GET
* **Request Params** : 无
* **返回值Sample**：

``` json
[
    {
        "env":"FAT",
        "clusters":[ //集群列表
            "default",
            "FAT381"
        ]
    },
    {
        "env":"UAT",
        "clusters":[
            "default"
        ]
    },
    {
        "env":"PRO",
        "clusters":[
            "default",
            "SHAOY",
            "SHAJQ"
        ]
    }
]
```

##### 3.2.2 获取App信息

* **URL** : http://{portal_address}/openapi/v1/apps
* **Method** : GET
* **Request Params** : 

参数名 | 必选 | 类型 | 说明
--- | --- | --- | ---
appIds | false | String | appId列表，以逗号分隔，如果为空则返回所有App信息
* **返回值Sample**：

``` json
[
    {
        "name":"first_app",
        "appId":"100003171",
        "orgId":"development",
        "orgName":"研发部",
        "ownerName":"apollo",
        "ownerEmail":"test@test.com",
        "dataChangeCreatedBy":"apollo",
        "dataChangeLastModifiedBy":"apollo",
        "dataChangeCreatedTime":"2019-05-08T09:13:31.000+0800",
        "dataChangeLastModifiedTime":"2019-05-08T09:13:31.000+0800"
    },
    {
        "name":"apollo-demo",
        "appId":"100004458",
        "orgId":"development",
        "orgName":"产品研发部",
        "ownerName":"apollo",
        "ownerEmail":"apollo@cmcm.com",
        "dataChangeCreatedBy":"apollo",
        "dataChangeLastModifiedBy":"apollo",
        "dataChangeCreatedTime":"2018-12-23T12:35:16.000+0800",
        "dataChangeLastModifiedTime":"2019-04-08T13:58:36.000+0800"
    }
]
```

##### 3.2.3 获取集群接口 

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}
* **Method** ： GET
* **Request Params** ：无
* **返回值Sample**：

``` json
{
    "name":"default",
    "appId":"100004458",
    "dataChangeCreatedBy":"apollo",
    "dataChangeLastModifiedBy":"apollo",
    "dataChangeCreatedTime":"2018-12-23T12:35:16.000+0800",
    "dataChangeLastModifiedTime":"2018-12-23T12:35:16.000+0800"
}
```

##### 3.2.4 创建集群接口
可以通过此接口创建集群，调用此接口需要授予第三方APP对目标APP的管理权限。

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters
* **Method** ： POST
* **Request Params** ：无
* **请求内容(Request Body, JSON格式)** ： 

参数名 | 必选 | 类型 | 说明
---- | --- | --- | ---
name | true | String | Cluster的名字
appId |	true | String | Cluster所属的AppId
dataChangeCreatedBy | true | String | namespace的创建人，格式为域账号，也就是sso系统的User ID

* **返回值 Sample** ： 

``` json
 {
    "name":"someClusterName",
    "appId":"100004458",
    "dataChangeCreatedBy":"apollo",
    "dataChangeLastModifiedBy":"apollo",
    "dataChangeCreatedTime":"2018-12-23T12:35:16.000+0800",
    "dataChangeLastModifiedTime":"2018-12-23T12:35:16.000+0800"
}
```

##### 3.2.5 获取集群下所有Namespace信息接口

* **URL** :  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces
* **Method**: GET
* **Request Params**: 无
* **返回值Sample**:

``` json
[
  {
    "appId": "100003171",
    "clusterName": "default",
    "namespaceName": "application",
    "comment": "default app namespace",
    "format": "properties", //Namespace格式可能取值为：properties、xml、json、yml、yaml
    "isPublic": false, //是否为公共的Namespace
    "items": [ // Namespace下所有的配置集合
      {
        "key": "batch",
        "value": "100",
        "dataChangeCreatedBy": "song_s",
        "dataChangeLastModifiedBy": "song_s",
        "dataChangeCreatedTime": "2016-07-21T16:03:43.000+0800",
        "dataChangeLastModifiedTime": "2016-07-21T16:03:43.000+0800"
      }
    ],
    "dataChangeCreatedBy": "song_s",
    "dataChangeLastModifiedBy": "song_s",
    "dataChangeCreatedTime": "2016-07-20T14:05:58.000+0800",
    "dataChangeLastModifiedTime": "2016-07-20T14:05:58.000+0800"
  },
  {
    "appId": "100003171",
    "clusterName": "default",
    "namespaceName": "FX.apollo",
    "comment": "apollo public namespace",
    "format": "properties",
    "isPublic": true,
    "items": [
      {
        "key": "request.timeout",
        "value": "3000",
        "comment": "",
        "dataChangeCreatedBy": "song_s",
        "dataChangeLastModifiedBy": "song_s",
        "dataChangeCreatedTime": "2016-07-20T14:08:30.000+0800",
        "dataChangeLastModifiedTime": "2016-08-01T13:56:25.000+0800"
      },
      {
        "id": 1116,
        "key": "batch",
        "value": "3000",
        "comment": "",
        "dataChangeCreatedBy": "song_s",
        "dataChangeLastModifiedBy": "song_s",
        "dataChangeCreatedTime": "2016-07-28T15:13:42.000+0800",
        "dataChangeLastModifiedTime": "2016-08-01T13:51:00.000+0800"
      }
    ],
    "dataChangeCreatedBy": "song_s",
    "dataChangeLastModifiedBy": "song_s",
    "dataChangeCreatedTime": "2016-07-20T14:08:13.000+0800",
    "dataChangeLastModifiedTime": "2016-07-20T14:08:13.000+0800"
  }
]
```

##### 3.2.6 获取某个Namespace信息接口

* **URL** ： http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}
* **Method** ： GET
* **Request Params** ：无
* **返回值Sample** ：

``` json
{
    "appId": "100003171",
    "clusterName": "default",
    "namespaceName": "application",
    "comment": "default app namespace",
    "format": "properties", //Namespace格式可能取值为：properties、xml、json、yml、yaml
    "isPublic": false, //是否为公共的Namespace
    "items": [ // Namespace下所有的配置集合
      {
        "key": "batch",
        "value": "100",
        "dataChangeCreatedBy": "song_s",
        "dataChangeLastModifiedBy": "song_s",
        "dataChangeCreatedTime": "2016-07-21T16:03:43.000+0800",
        "dataChangeLastModifiedTime": "2016-07-21T16:03:43.000+0800"
      }
    ],
    "dataChangeCreatedBy": "song_s",
    "dataChangeLastModifiedBy": "song_s",
    "dataChangeCreatedTime": "2016-07-20T14:05:58.000+0800",
    "dataChangeLastModifiedTime": "2016-07-20T14:05:58.000+0800"
  }
```
##### 3.2.7 创建Namespace
可以通过此接口创建Namespace，调用此接口需要授予第三方APP对目标APP的管理权限。

* **URL** ：  http://{portal_address}/openapi/v1/apps/{appId}/appnamespaces
* **Method** ： POST
* **Request Params** ：无
* **请求内容(Request Body, JSON格式)** ： 

参数名 | 必选 | 类型 | 说明
---- | --- | --- | ---
name | true | String | Namespace的名字
appId |	true | String | Namespace所属的AppId
format	|true | String | Namespace的格式，**只能是以下类型： properties、xml、json、yml、yaml**
isPublic  |true | boolean | 是否是公共文件
comment  |false | String | Namespace说明
dataChangeCreatedBy | true | String | namespace的创建人，格式为域账号，也就是sso系统的User ID

* **返回值 Sample** ： 

``` json
 {
  "name": "FX.public-0420-11",
  "appId": "100003173",
  "format": "properties",
  "isPublic": true,
  "comment": "test",
  "dataChangeCreatedBy": "zhanglea",
  "dataChangeLastModifiedBy": "zhanglea",
  "dataChangeCreatedTime": "2017-04-20T18:25:49.033+0800",
  "dataChangeLastModifiedTime": "2017-04-20T18:25:49.033+0800"
}
```
* **返回值说明** ： 
> 如果是properties文件，name = ${appId所属的部门}.${传入的name值}  ，例如调用接口传入的name=xy-z, format=properties，应用的部门为框架（FX）,那么name=FX.xy-z


> 如果不是properties文件 name = ${appId所属的部门}.${传入的name值}.${format}，例如调用接口传入的name=xy-z, format=json，应用的部门为框架（FX）,那么name=FX.xy-z.json

##### 3.2.8 获取某个Namespace当前编辑人接口 
Apollo在生产环境（PRO）有限制规则：每次发布只能有一个人编辑配置，且该次发布的人不能是该次发布的编辑人。
也就是说如果一个用户A修改了某个namespace的配置，那么在这个namespace发布前，只能由A修改，其它用户无法修改。同时，该用户A无法发布自己修改的配置，必须找另一个有发布权限的人操作。
这个接口就是用来获取当前namespace是否有人锁定的接口。在非生产环境（FAT、UAT），该接口始终返回没有人锁定。

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/lock
* **Method** ： GET
* **Request Params** ：无
* **返回值 Sample（未锁定）** ： 

``` json
  {
  "namespaceName": "application",
  "isLocked": false
}
```

* **返回值Sample(被锁定)** ：

``` json
  {
  "namespaceName": "application",
  "isLocked": true,
  "lockedBy": "song_s" //锁owner
}
```

##### 3.2.9 读取配置接口 

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}
* **Method** ： GET
* **Request Params** ：无
* **返回值Sample** ：

``` json
  {
    "key": "timeout",
    "value": "3000",
    "comment": "超时时间",
    "dataChangeCreatedBy": "zhanglea",
    "dataChangeLastModifiedBy": "zhanglea",
    "dataChangeCreatedTime": "2016-08-11T12:06:41.818+0800",
    "dataChangeLastModifiedTime": "2016-08-11T12:06:41.818+0800"
}
```

##### 3.2.10 新增配置接口 

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items
* **Method** ： POST
* **Request Params** ：无
* **请求内容(Request Body, JSON格式)** ： 

参数名 | 必选 | 类型 | 说明
---- | --- | --- | ---
key | true | String | 配置的key，长度不能超过128个字符。非properties格式，key固定为`content`
value |	true | String | 配置的value，长度不能超过20000个字符，非properties格式，value为文件全部内容
comment	| false | String | 配置的备注,长度不能超过256个字符
dataChangeCreatedBy | true | String | item的创建人，格式为域账号，也就是sso系统的User ID

* **Request body sample** :

``` json
{
    "key":"timeout",
    "value":"3000",
    "comment":"超时时间",
    "dataChangeCreatedBy":"zhanglea"
}

```

* **返回值Sample** ：

``` json
  {
    "key": "timeout",
    "value": "3000",
    "comment": "超时时间",
    "dataChangeCreatedBy": "zhanglea",
    "dataChangeLastModifiedBy": "zhanglea",
    "dataChangeCreatedTime": "2016-08-11T12:06:41.818+0800",
    "dataChangeLastModifiedTime": "2016-08-11T12:06:41.818+0800"
}
```

##### 3.2.11 修改配置接口

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}
* **Method** ： PUT
* **Request Params** ：

参数名 | 必选 | 类型 | 说明
--- | --- | --- | ---
createIfNotExists | false | Boolean | 当配置不存在时是否自动创建

* **请求内容(Request Body, JSON格式)** ： 

参数名 | 必选 | 类型 | 说明
---- | --- | --- | ---
key | true | String | 配置的key，需和url中的key值一致。非properties格式，key固定为`content`
value |	true | String | 配置的value，长度不能超过20000个字符，非properties格式，value为文件全部内容
comment	| false | String | 配置的备注,长度不能超过256个字符
dataChangeLastModifiedBy | true | String | item的修改人，格式为域账号，也就是sso系统的User ID
dataChangeCreatedBy | false | String | 当createIfNotExists为true时必选。item的创建人，格式为域账号，也就是sso系统的User ID

* **Request body sample** :
```json
{
    "key":"timeout",
    "value":"3000",
    "comment":"超时时间",
    "dataChangeLastModifiedBy":"zhanglea"
}
```

* **返回值** ：无


##### 3.2.12 删除配置接口

* **URL** ： http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}?operator={operator}
* **Method** ： DELETE
* **Request Params** ：

参数名 | 必选 | 类型 | 说明
--- | --- | --- | ---
key | true | String | 配置的key。非properties格式，key固定为`content`
operator | true | String | 删除配置的操作者，域账号

* **返回值** ： 无

##### 3.2.13 发布配置接口

* **URL** ： http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases
* **Method** ： POST
* **Request Params** ：无
* **Request Body** ：

参数名 | 必选 | 类型 | 说明
--- | --- | --- | ---
releaseTitle | true | String | 此次发布的标题，长度不能超过64个字符
releaseComment | false | String | 发布的备注，长度不能超过256个字符
releasedBy | true | String | 发布人，域账号，注意：如果`ApolloConfigDB.ServerConfig`中的`namespace.lock.switch`设置为true的话（默认是false），那么该环境不允许发布人和编辑人为同一人。所以如果编辑人是zhanglea，发布人就不能再是zhanglea。

* **Request Body example** ：

```json
{
    "releaseTitle":"2016-08-11",
    "releaseComment":"修改timeout值",
    "releasedBy":"zhanglea"
}
```

* **返回值Sample** ：

``` json
{
    "appId": "test-0620-01",
    "clusterName": "test",
    "namespaceName": "application",
    "name": "2016-08-11",
    "configurations": {
        "timeout": "3000",
    },
    "comment": "修改timeout值",
    "dataChangeCreatedBy": "zhanglea",
    "dataChangeLastModifiedBy": "zhanglea",
    "dataChangeCreatedTime": "2016-08-11T14:03:46.232+0800",
    "dataChangeLastModifiedTime": "2016-08-11T14:03:46.235+0800"
}
```

##### 3.2.14 获取某个Namespace当前生效的已发布配置接口 

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases/latest
* **Method** ： GET
* **Request Params** ：无
* **返回值Sample** ：

``` json
{
    "appId": "test-0620-01",
    "clusterName": "test",
    "namespaceName": "application",
    "name": "2016-08-11",
    "configurations": {
        "timeout": "3000",
    },
    "comment": "修改timeout值",
    "dataChangeCreatedBy": "zhanglea",
    "dataChangeLastModifiedBy": "zhanglea",
    "dataChangeCreatedTime": "2016-08-11T14:03:46.232+0800",
    "dataChangeLastModifiedTime": "2016-08-11T14:03:46.235+0800"
}
```

##### 3.2.15 回滚已发布配置接口 

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/releases/{releaseId}/rollback
* **Method** ： PUT
* **Request Params** ：

参数名 | 必选 | 类型 | 说明
--- | --- | --- | ---
operator | true | String | 删除配置的操作者，域账号

* **返回值** ： 无

##### 3.2.16 分页获取配置项接口

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items
* **Method** ： GET
* **Version** ： >= 2.1.0
* **Request Params** ：

参数名 | 必选    | 类型  | 说明
--- |-------|-----| ---
page | false | int | 页码，从 0 开始，默认为 0
size | false | int | 页大小，默认为 50

* **返回值Sample** ：

``` json
{
    "content": [
        {
            "key": "timeout",
            "value": "3000",
            "comment": "超时时间",
            "dataChangeCreatedBy": "mghio",
            "dataChangeLastModifiedBy": "mghio",
            "dataChangeCreatedTime": "2022-07-17T21:37:41.818+0800",
            "dataChangeLastModifiedTime": "2022-07-17T21:37:41.818+0800"
        },
        {
            "key": "page.size",
            "value": "200",
            "comment": "页大小",
            "dataChangeCreatedBy": "mghio",
            "dataChangeLastModifiedBy": "mghio",
            "dataChangeCreatedTime": "2022-07-17T21:37:41.818+0800",
            "dataChangeLastModifiedTime": "2022-07-17T21:37:41.818+0800"
        }
    ],
    "page": 0,
    "size": 50,
    "total": 2
}
```

##### 3.2.17 创建App并获取管理员权限

可以通过此接口创建App，

>  注意：需要在创建第三方应用时，**勾选允许创建app，否则会产生异常，HTTP状态码401**

* **URL** ：  http://{portal_address}/openapi/v1/apps/
* **Method** ： POST
* **Request Params** ：无
* **请求内容(Request Body, JSON格式)** ： 

| 参数名              | 必选  | 类型     | 说明                                  |
| ------------------- | ----- | -------- | ------------------------------------- |
| assignAppRoleToSelf | true  | Boolean  | true：授予自己APP的管理权限           |
| admins              | false | String[] | 授予这些用户APP的管理权限             |
| app                 | true  | Object   | APP的信息，字段参考下方的请求值Sample |

* **请求值 Sample** ： 

```json
{
  "assignAppRoleToSelf": true,
  "admins": [
    "user1",
    "user2"
  ],
  "app": {
    "name": "appName1234",
    "appId": "xxx-web",
    "orgId": "development",
    "orgName": "产品研发部",
    "ownerName": "user3",
    "ownerEmail": "user3@test.com"
  }
}
```

* **返回值 Sample** ： 无返回值

##### 3.2.18 用户管理

> 自 Apollo 3.0.0 起可用。本节为接入指南；完整路径、参数与 schema 请以规范合同 [`apollo-openapi` v0.3.10](https://github.com/apolloconfig/apollo-openapi/blob/v0.3.10/apollo-openapi.yaml) 为准。

用户管理 Open API 覆盖 Portal 用户的搜索、查询、创建/更新以及启用/禁用，路径前缀为 `/openapi/v1/users`。授权规则：

* Consumer Token：需要明确的 `ManageUsers` 权限（Portal **允许管理用户？**）。
* 用户访问 Token：需要所属用户当前具备 `ManageUsers` 权限，并且 Token 操作范围包含 `user:manage`。对用户访问 Token，创建/更新其他用户以及启用/禁用需要所属用户为 Portal 超级管理员，并且 Token 操作范围包含 `system:admin`（同时仍需 `user:manage`）。若没有 `system:admin`，即使所属用户是超级管理员、Token 仅有 `user:manage`，也会被当作非超级管理员，只能创建/更新 Token 所属用户本人且必须保持用户为启用状态。具备 `ManageUsers` 的 Consumer Token 不受这些额外用户 Token 限制。
* Consumer Token 的写操作（`POST /openapi/v1/users`、`PUT /openapi/v1/users/enabled`）需要传入有效的 `operator` 查询参数；用户访问 Token 以 Token 所属用户作为操作人。

`GET` 搜索/查询走当前配置的 `UserService`。创建 / 更新 / 启用 / 禁用仅在 Portal 使用内置 `SpringSecurityUserService` 时受支持。其他 `UserService` 实现（例如仅 LDAP）可支持搜索/查询，但会拒绝写操作。

相关路径（完整说明见 OpenAPI 合同）：`GET /openapi/v1/users/{userId}`、`POST /openapi/v1/users`、`PUT /openapi/v1/users/enabled`。

###### 搜索用户

* **URL** : `http://{portal_address}/openapi/v1/users`
* **Method** : GET
* **Request Params** :

参数名 | 必选 | 类型 | 说明
--- | --- | --- | ---
keyword | true | String | 由当前配置的 `UserService` 匹配用户名 / 显示名；不保证匹配邮箱
includeInactiveUsers | false | Boolean | 接口接受该参数（默认 `false`）。内置 Spring Security / OIDC local 会生效；LDAP 会忽略
offset | false | Integer | 接口接受该参数（默认 `0`）；Apollo 3.0.0 内置 `UserService` 实现不会按 offset 分页
limit | false | Integer | 接口接受该参数（默认 `10`）；Apollo 3.0.0 内置 `UserService` 实现不会按 limit 截断

* **请求值 Sample** ：

```text
http://{portal_address}/openapi/v1/users?keyword=apollo&includeInactiveUsers=false
```

* **返回值 Sample** ：

```json
[
  {
    "userId": "apollo",
    "name": "apollo",
    "email": "apollo@acme.com",
    "enabled": 1
  }
]
```

##### 3.2.19 权限管理

> 自 Apollo 3.0.0 起可用。本节为接入指南；完整路径、参数与 schema（含 Namespace、环境 Namespace、集群 Namespace 等变体）请以规范合同 [`apollo-openapi` v0.3.10](https://github.com/apolloconfig/apollo-openapi/blob/v0.3.10/apollo-openapi.yaml) 为准。

权限管理 Open API 用于在应用创建后查询与变更应用 / Namespace 角色授权。示例接口：

* `GET /openapi/v1/apps/{appId}/role-users`
* `POST /openapi/v1/apps/{appId}/namespaces/{namespaceName}/roles/{roleType}`
* `DELETE /openapi/v1/apps/{appId}/namespaces/{namespaceName}/roles/{roleType}`
* `POST /openapi/v1/apps/{appId}/roles/{roleType}` 与 `DELETE /openapi/v1/apps/{appId}/roles/{roleType}`（应用级，含 `Master`）——Apollo 3.0.0 仅支持用户访问 Token；见下方授权说明

Apollo 3.0.0 的授权行为：

* Consumer Token：
  * 角色查询（`GET .../role-users`）：任意已认证的 Consumer Token 可查询任意 `appId` 的角色用户；没有应用级 Consumer 权限校验，也不要求 `ASSIGN_ROLE`。这是当前运行时行为，并不表示读接口按应用授权隔离。
  * Namespace / 环境 Namespace / 集群 Namespace 的授予与撤销：需要对目标应用具备应用级分配角色权限（`ASSIGN_ROLE`）。鉴权在解析 `operator` 之前完成；`operator` 只用于标识写操作的审计操作人。
  * 应用级授予与撤销（`POST`/`DELETE /openapi/v1/apps/{appId}/roles/{roleType}`）：Consumer Token 不支持（鉴权会抛出 `UnsupportedOperationException`，通常表现为 HTTP 500）。
* 用户访问 Token：需要所属用户对目标应用有相应权限，并包含 `app:manage-role` 操作范围及适用的资源范围。角色查询额外要求对应资源上的分配角色权限；应用级授予/撤销额外要求 manage-app-master 权限。

`roleType` 取值与参数 schema 以 OpenAPI 合同 / `RoleType` 为准；常见值包括 `Master`、`ModifyNamespace`、`ReleaseNamespace`、`ModifyNamespacesInCluster`、`ReleaseNamespacesInCluster`。

> **应用负责人 vs 角色：** 应用负责人属于应用元数据，通过 `PUT /openapi/v1/apps/{appId}` 更新。管理员（`Master`）、修改、发布等权限请使用上述权限管理 API —— 修改 owner 字段不会授予或撤销这些角色。

###### 查询应用角色用户

* **URL** : `http://{portal_address}/openapi/v1/apps/{appId}/role-users`
* **Method** : GET
* **Request Params** : 无
* **返回值 Sample** ：

```json
{
  "appId": "xxx-web",
  "masterUsers": [
    {
      "userId": "user1",
      "name": "user1",
      "email": "user1@acme.com",
      "enabled": 1
    }
  ]
}
```

###### 授予 Namespace 角色

Consumer Token 在具备应用级 `ASSIGN_ROLE` 时可使用 Namespace 级授予/撤销。应用级授予/撤销（含通过 `POST /openapi/v1/apps/{appId}/roles/{roleType}` 操作 `Master`）在 Apollo 3.0.0 仅支持用户访问 Token；Consumer 调用会因 `UnsupportedOperationException` 失败（通常为 HTTP 500）。

* **URL** : `http://{portal_address}/openapi/v1/apps/{appId}/namespaces/{namespaceName}/roles/{roleType}`
* **Method** : POST
* **Request Params** :

参数名 | 必选 | 类型 | 说明
--- | --- | --- | ---
userId | true | String | 被授予角色的用户
operator | false | String | Consumer Token 必填；用户访问 Token 忽略该参数（使用所属用户）

* **请求值 Sample** ：

```text
http://{portal_address}/openapi/v1/apps/xxx-web/namespaces/application/roles/ModifyNamespace?userId=user2&operator=apollo
```

* **返回值 Sample** ： 无返回值

###### 撤销 Namespace 角色

* **URL** : `http://{portal_address}/openapi/v1/apps/{appId}/namespaces/{namespaceName}/roles/{roleType}`
* **Method** : DELETE
* **Request Params** :

参数名 | 必选 | 类型 | 说明
--- | --- | --- | ---
userId | true | String | 被移除角色的用户
operator | false | String | Consumer Token 必填；用户访问 Token 忽略该参数（使用所属用户）

* **请求值 Sample** ：

```text
http://{portal_address}/openapi/v1/apps/xxx-web/namespaces/application/roles/ModifyNamespace?userId=user2&operator=apollo
```

* **返回值 Sample** ： 无返回值

##### 3.2.20 批量新增/修改/删除配置项

> 自 Apollo 3.0.0 起可用。规范合同见 [`apollo-openapi` v0.3.11](https://github.com/apolloconfig/apollo-openapi/blob/v0.3.11/apollo-openapi.yaml)。

用于一次性提交一批配置项进行创建、修改或删除，避免逐条调用单条配置项接口。三个接口各自独立，不提供跨接口的原子性（例如"新增一批同时删除另一批"需要调用两次，对应两条 Commit 记录）。鉴权与单条配置项接口一致：需要对目标 Namespace 具备修改权限（`@unifiedPermissionValidator.hasModifyNamespacePermission`）。

###### 批量新增配置项

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/batch-create
* **Method** ： POST
* **Request Params** ：

参数名 | 必选 | 类型 | 说明
--- | --- | --- | ---
operator | false | String | 新增配置的操作者，域账号

* **请求内容(Request Body, JSON格式)** ：`OpenItemDTO` 数组，每一项字段同 [3.2.10 新增配置接口](#3210-新增配置接口)

* **Request body sample** :

``` json
[
    {
        "key":"timeout",
        "value":"3000",
        "comment":"超时时间"
    },
    {
        "key":"retries",
        "value":"3",
        "comment":"重试次数"
    }
]
```

* **返回值** ：无

###### 批量修改配置项

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/batch-update
* **Method** ： PUT
* **Request Params** ：

参数名 | 必选 | 类型 | 说明
--- | --- | --- | ---
operator | false | String | 修改配置的操作者，域账号

* **请求内容(Request Body, JSON格式)** ：`OpenItemDTO` 数组，只有 `key`、`value`、`type`、`comment` 会被使用；`key` 对应的配置项不存在时返回 404

* **Request body sample** :

``` json
[
    {
        "key":"timeout",
        "value":"5000"
    }
]
```

* **返回值** ：无

###### 批量删除配置项

* **URL** ：  http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/batch-delete
* **Method** ： POST
* **Request Params** ：

参数名 | 必选 | 类型 | 说明
--- | --- | --- | ---
operator | false | String | 删除配置的操作者，域账号

* **请求内容(Request Body, JSON格式)** ：待删除的配置项 key 数组；任意 key 不存在时返回 404

* **Request body sample** :

``` json
[
    "timeout",
    "retries"
]
```

* **返回值** ：无

### 四、错误码说明

正常情况下，接口返回的Http状态码是200，下面列举了Apollo会返回的非200错误码说明。

####  4.1 400 - Bad Request
客户端传入参数的错误，如操作人不存在，namespace不存在等等，客户端需要根据提示信息检查对应的参数是否正确。
####  4.2 401 - Unauthorized
接口传入的token非法或者已过期，客户端需要检查token是否传入正确。
####  4.3 403 - Forbidden
接口要访问的资源未得到授权，比如只授权了对A应用下Namespace的管理权限，但是却尝试管理B应用下的配置。
####  4.4 404 - Not Found
接口要访问的资源不存在，一般是URL或URL的参数错误。
####  4.5 405 - Method Not Allowed
接口访问的Method不正确，比如应该使用POST的接口使用了GET访问等，客户端需要检查接口访问方式是否正确。
####  4.6 500 - Internal Server Error
其它类型的错误默认都会返回500，对这类错误如果应用无法根据提示信息找到原因的话，可以找Apollo研发团队一起排查问题。
