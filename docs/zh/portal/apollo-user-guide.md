# &nbsp;
# 名词解释
* 普通应用
    * 普通应用指的是独立运行的程序，如
        * Web应用程序
        * 带有main函数的程序
* 公共组件
    * 公共组件指的是发布的类库、客户端程序，不会自己独立运行，如
        * Java的jar包
        * .Net的dll文件

# 一、普通应用接入指南
## 1.1 创建项目
要使用Apollo，第一步需要创建项目。

1. 打开apollo-portal主页
2. 点击“创建项目”

![create-app-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-app-entry.png)

3. 输入项目信息
    * 部门：选择应用所在的部门
    * 应用AppId：用来标识应用身份的唯一id，格式为string，需要和客户端app.properties中配置的app.id对应
    * 应用名称：应用名，仅用于界面展示
    * 应用负责人：选择的人默认会成为该项目的管理员，具备项目权限管理、集群创建、Namespace创建等权限

![create-app](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-app.png)

4. 点击提交

    创建成功后，会自动跳转到项目首页

![app-created](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/app-created.png)

## 1.2 项目权限分配
### 1.2.1 项目管理员权限

项目管理员拥有以下权限：

1. 可以管理项目的权限分配
2. 可以创建集群
3. 可以创建Namespace

创建项目时填写的应用负责人默认会成为项目的管理员之一，如果还需要其他人也成为项目管理员，可以按照下面步骤操作：

1. 点击页面左侧的“管理项目”
    * ![app-permission-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/app-permission-entry.png)

2. 搜索需要添加的成员并点击添加
    * ![app-permission-search-user](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/app-permission-search-user.png)
    * ![app-permission-user-added](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/app-permission-user-added.png)

### 1.2.2 配置编辑、发布权限
配置权限分为编辑和发布：

* 编辑权限允许用户在Apollo界面上创建、修改、删除配置
    * 配置修改后只在Apollo界面上变化，不会影响到应用实际使用的配置
* 发布权限允许用户在Apollo界面上发布、回滚配置
    * 配置只有在发布、回滚动作后才会被应用实际使用到
    * Apollo在用户操作发布、回滚动作后实时通知到应用，并使最新配置生效

项目创建完，默认没有分配配置的编辑和发布权限，需要项目管理员进行授权。

1. 点击application这个namespace的授权按钮
    * ![namespace-permission-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-entry.png)

2. 分配修改权限
    * ![namespace-permission-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-edit.png)

3. 分配发布权限
    * ![namespace-publish-permission](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-publish-permission.png)

### 1.2.3 不同维度的权限管理

对于Apollo的配置权限，在初始设计时把权限绑定在了 namespace 上，因为 apollo 的权限管理本身是比较灵活的，所以可以在这个基础之上进行扩展。

基于 Apollo 的主要实体类的设计 [E-R Diagram](/docs/zh/design/apollo-design.md?id=_14-e-r-diagram)，
我们可以认为 Namespace 是一个权限的最小单元，而 App 是一个权限的最大单元。
中间依次是 Env、Cluster，所以我们可以对不同维度的权限进行管理。

| App | Env | Cluster | Namespace | Model | Impl |
| --- | --- | --- | --- | --- |------|
| ☑️ |  |  |  | App → * | 未实现  |
| ☑️ |  |  | ☑️ | App → Namespace | 已实现  |
| ☑️ | ☑️ |  |  | App + Env → * | 未实现  |
| ☑️ | ☑️ |  | ☑️ | App + Env → Namespace | 已实现  |
| ☑️ | ☑️ | ☑️ |  | App + Env + Cluster → * | 已实现  |
| ☑️ | ☑️ | ☑️ | ☑️ | App + Env + Cluster → Namespace | 未实现  |

对不同权限模型的释义：

| Model | Target | PermissionType (e.g. Modify) | TargetId |
| --- | --- | --- | --- |
| App → * | App的所有namespace |  |  |
| App → Namespace | App下的所有指定名字的namespace | ModifyNamespace | App+Namespace |
| App + Env → * | App的env下所有namespace |  |  |
| App + Env → Namespace | App的env下所有指定名字的namespace | ModifyNamespace | App+Namespace+Env |
| App + Env + Cluster → * | App的env中cluster的所有namespace | ModifyNamespaceInCluster | App+Env+ClusterName |
| App + Env + Cluster → Namespace | App的env中cluster下指定名字的namespace |  |  |

#### 1.2.3.1 App下的所有指定名字的namespace

1. 点击application这个namespace的授权按钮
    * ![ns-permission-app-allns-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-allns-entry.png)
    
2. 选择“所有环境”
    * ![ns-permission-app-allns-select](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-allns-select.png)

3. 分配修改权限
    * ![namespace-permission-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-edit.png)

4. 分配发布权限
    * ![namespace-publish-permission](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-publish-permission.png)

#### 1.2.3.2 App的env下所有指定名字的namespace

1. 点击application这个namespace的授权按钮
    * ![ns-permission-app-allns-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-allns-entry.png)

2. 选择环境
    * ![ns-permission-app-env-ns-select](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-env-ns-select.png)

3. 分配修改权限
    * ![namespace-permission-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-edit.png)

4. 分配发布权限
    * ![namespace-publish-permission](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-publish-permission.png)

#### 1.2.3.3 App的env中cluster的所有namespace

1. 点击“管理集群”进取管理集群页面
    * ![manage-cluster-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/manage-cluster-entry.png)

2. 点击想管理的Cluster的授权按钮
    * ![ns-permission-app-env-cluster-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-env-cluster-entry.png)

3. 编辑权限
    * ![ns-permission-app-env-cluster-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-env-cluster-edit.png)

## 1.3 添加配置项
编辑配置需要拥有这个Namespace的编辑权限，如果发现没有新增配置按钮，可以找项目管理员授权。

### 1.3.1 通过表格模式添加配置

1. 点击新增配置
    * ![create-item-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-item-entry.png)

2. 输入配置项
    * ![create-item-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-item-detail.png)

3. 点击提交
    * ![item-created](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/item-created.png)

### 1.3.2 通过文本模式编辑
Apollo除了支持表格模式，逐个添加、修改配置外，还提供文本模式批量添加、修改。
这个对于从已有的properties文件迁移尤其有用。

1. 切换到文本编辑模式
![text-mode-config-overview](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/text-mode-config-overview.png)

2. 点击右侧的修改配置按钮
![text-mode-config-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/text-mode-config-entry.png)

3. 输入配置项，并点击提交修改
![text-mode-config-submit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/text-mode-config-submit.png)

## 1.4 发布配置
配置只有在发布后才会真的被应用使用到，所以在编辑完配置后，需要发布配置。

发布配置需要拥有这个Namespace的发布权限，如果发现没有发布按钮，可以找项目管理员授权。

1. 点击“发布按钮”
![publish-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/hermes-portal-publish-entry.png)

2. 填写发布相关信息，点击发布
![publish-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/hermes-portal-publish-detail.png)

## 1.5 应用读取配置
配置发布成功后，应用就可以通过Apollo客户端读取到配置了。

Apollo目前提供Java客户端，具体信息请点击[Java客户端使用文档](zh/client/java-sdk-user-guide)：

如果应用使用了其它语言，也可以通过直接访问Http接口获取配置，具体可以参考[其它语言客户端接入指南](zh/client/other-language-client-user-guide)

## 1.6 回滚已发布配置

如果发现已发布的配置有问题，可以通过点击『回滚』按钮来将客户端读取到的配置回滚到上一个发布版本。

这里的回滚机制类似于发布系统，发布系统中的回滚操作是将部署到机器上的安装包回滚到上一个部署的版本，但代码仓库中的代码是不会回滚的，从而开发可以在修复代码后重新发布。

Apollo中的回滚也是类似的机制，点击回滚后是将发布到客户端的配置回滚到上一个已发布版本，也就是说客户端读取到的配置会恢复到上一个版本，但页面上编辑状态的配置是不会回滚的，从而开发可以在修复配置后重新发布。

## 1.7 配置查询（管理员权限）

在配置添加或修改后，管理员用户可以通过进入 `管理员工具 - Value的全局搜索` 页面，来对配置项进行所属查询以及跳转修改。

这里的查询为模糊检索，通过对配置项的key与value至少一项进行检索，找到该配置在哪个应用、环境、集群、命名空间中被使用。

- properties格式配置可以直接通过对key与value进行检索

![Configuration query-properties](../images/Configuration query-properties.png)

- xml、json、yml、yaml、txt等格式配置，由于存储时以content-value进行存储，故可以通过key=content、value=配置项内容，进行检索

![Configuration query-Non properties](../images/Configuration query-Non properties.png)

# 二、公共组件接入指南
## 2.1 公共组件和普通应用的区别

公共组件是指那些发布给其它应用使用的客户端代码，比如CAT客户端、Hermes Producer客户端等。

虽然这类组件是由其他团队开发、维护，但是运行时是在业务实际应用内的，所以本质上可以认为是应用的一部分。

通常情况下，这类组件所用到的配置由原始开发团队维护，不过由于实际应用的运行时、环境各不一样，所以我们也允许应用在实际使用时能够覆盖公共组件的部分配置。

## 2.2 公共组件接入步骤

公共组件的接入步骤，和普通应用几乎一致，唯一的区别是公共组件需要创建自己唯一的Namespace。

所以，首先执行普通应用接入文档中的以下几个步骤，然后再按照本章节后面的步骤操作。

1. [创建项目](#_11-%E5%88%9B%E5%BB%BA%E9%A1%B9%E7%9B%AE)
2. [项目管理员权限](#_121-%E9%A1%B9%E7%9B%AE%E7%AE%A1%E7%90%86%E5%91%98%E6%9D%83%E9%99%90)

### 2.2.1 创建Namespace

创建Namespace需要项目管理员权限，如果发现没有添加Namespace按钮，可以找项目管理员授权。

1. 点击页面左侧的添加Namespace
    * ![create-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-namespace.png)

2. 点击“创建新的Namespace”
    * ![create-namespace-select-type](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-namespace-select-type.png)

3. 输入公共组件的Namespace名称，需要注意的是Namespace名称全局唯一
    * Apollo会默认把部门代号添加在最前面
    * ![create-namespace-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-namespace-detail.png)

4. 点击提交后，页面会自动跳转到关联Namespace页面
    * 首先，选中所有需要有这个Namespace的环境和集群，一般建议全选
    * 其次，选中刚刚创建的namespace
    * 最后，点击提交
    * ![link-namespace-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/link-namespace-detail.png)

5. 关联成功后，页面会自动跳转到Namespace权限管理页面
    1. 分配修改权限
        * ![namespace-permission-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-edit.png)
    2. 分配发布权限
        * ![namespace-publish-permission](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-publish-permission.png)

6. 点击“返回”回到项目页面

### 2.2.2 添加配置项

编辑配置需要拥有这个Namespace的编辑权限，如果发现没有新增配置按钮，可以找项目管理员授权。

#### 2.2.2.1 通过表格模式添加配置

1. 点击新增配置
![public-namespace-edit-item-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-edit-item-entry.png)

2. 输入配置项
![public-namespace-edit-item](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-edit-item.png)

3. 点击提交
![public-namespace-item-created](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-item-created.png)

#### 2.2.2.2 通过文本模式编辑
这部分和普通应用一致，具体步骤请参见[1.3.2 通过文本模式编辑](#_132-%E9%80%9A%E8%BF%87%E6%96%87%E6%9C%AC%E6%A8%A1%E5%BC%8F%E7%BC%96%E8%BE%91)。

### 2.2.3 发布配置

配置只有在发布后才会真的被应用使用到，所以在编辑完配置后，需要发布配置。

发布配置需要拥有这个Namespace的发布权限，如果发现没有发布按钮，可以找项目管理员授权。

1. 点击“发布按钮”
![public-namespace-publish-items-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-publish-items-entry.png)

2. 填写发布相关信息，点击发布
![public-namespace-publish-items](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-publish-items.png)

### 2.2.4 应用读取配置

配置发布成功后，应用就可以通过Apollo客户端读取到配置了。

Apollo目前提供Java客户端，具体信息请点击[Java客户端使用文档](zh/client/java-sdk-user-guide)：

如果应用使用了其它语言，也可以通过直接访问Http接口获取配置，具体可以参考[其它语言客户端接入指南](zh/client/other-language-client-user-guide)

对于公共组件的配置读取，可以参考上述文档中的“获取公共Namespace的配置”部分。

## 2.3 应用覆盖公用组件配置步骤
前面提到，通常情况下，公共组件所用到的配置由原始开发团队维护，不过由于实际应用的运行时、环境各不一样，所以我们也允许应用在实际使用时能够覆盖公共组件的部分配置。

这里就讲一下应用如何覆盖公用组件的配置，简单起见，假设apollo-portal应用使用了hermes producer客户端，并且希望调整hermes的批量发送大小。

### 2.3.1 关联公共组件Namespace

1. 进入使用公共组件的应用项目首页，点击左侧的添加Namespace按钮
    * 所以，在这个例子中，我们需要进入apollo-portal的首页。
    * （添加Namespace需要项目管理员权限，如果发现没有添加Namespace按钮，可以找项目管理员授权）
    * ![link-public-namespace-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/link-public-namespace-entry.png)

2. 找到hermes producer的namespace，并选择需要关联到哪些环境和集群
![link-public-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/link-public-namespace.png)

3. 关联成功后，页面会自动跳转到Namespace权限管理页面
    1. 分配修改权限
    ![namespace-permission-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-edit.png)
    2. 分配发布权限
    ![namespace-publish-permission](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-publish-permission.png)

4. 点击“返回”回到项目页面

### 2.3.2 覆盖公用组件配置

1. 点击新增配置
![override-public-namespace-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/override-public-namespace-entry.png)

2. 输入要覆盖的配置项
![override-public-namespace-item](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/override-public-namespace-item.png)

3. 点击提交
![override-public-namespace-item-done](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/override-public-namespace-item-done.png)

### 2.3.3 发布配置

配置只有在发布后才会真的被应用使用到，所以在编辑完配置后，需要发布配置。

发布配置需要拥有这个Namespace的发布权限，如果发现没有发布按钮，可以找项目管理员授权。

1. 点击“发布按钮”
![override-public-namespace-item-publish-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/override-public-namespace-item-publish-entry.png)

2. 填写发布相关信息，点击发布
![override-public-namespace-item-publish](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/override-public-namespace-item-publish.png)

3. 配置发布成功后，hermes producer客户端在apollo-portal应用里面运行时读取到的sender.batchSize的值就是1000。

# 三、集群独立配置说明

在有些特殊情况下，应用有需求对不同的集群做不同的配置，比如部署在A机房的应用连接的es服务器地址和部署在B机房的应用连接的es服务器地址不一样。

在这种情况下，可以通过在Apollo创建不同的集群来解决。

## 3.1 创建集群

创建集群需要项目管理员权限，如果发现没有添加集群按钮，可以找项目管理员授权。

1. 点击页面左侧的“添加集群”按钮
    * ![create-cluster](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-cluster.png)

2. 输入集群名称，选择环境并提交
    * ![create-cluster-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-cluster-detail.png)

3. 切换到对应的集群，修改配置并发布即可
    * ![config-in-cluster-created](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/cluster-created.png)

4. 通过上述配置，部署在SHAJQ机房的应用就会读到SHAJQ集群下的配置

5. 如果应用还在其它机房部署了应用，那么在上述的配置下，会读到default集群下的配置。

# 四、多个AppId使用同一份配置

在一些情况下，尽管应用本身不是公共组件，但还是需要在多个AppId之间共用同一份配置，比如同一个产品的不同项目：XX-Web, XX-Service, XX-Job等。

这种情况下如果希望实现多个AppId使用同一份配置的话，基本概念和公共组件的配置是一致的。

具体来说，就是在其中一个AppId下创建一个namespace，写入公共的配置信息，然后在各个项目中读取该namespace的配置即可。

如果某个AppId需要覆盖公共的配置信息，那么在该AppId下关联公共的namespace并写入需要覆盖的配置即可。

具体步骤可以参考[公共组件接入指南](#%e4%ba%8c%e3%80%81%e5%85%ac%e5%85%b1%e7%bb%84%e4%bb%b6%e6%8e%a5%e5%85%a5%e6%8c%87%e5%8d%97)。

# 五、灰度发布使用指南
通过灰度发布功能，可以实现：

1. 对于一些对程序有比较大影响的配置，可以先在一个或者多个实例生效，观察一段时间没问题后再全量发布配置。
2. 对于一些需要调优的配置参数，可以通过灰度发布功能来实现A/B测试。可以在不同的机器上应用不同的配置，不断调整、测评一段时间后找出较优的配置再全量发布配置。

下面将结合一个实际例子来描述如何使用灰度发布功能。

## 5.1 场景介绍
100004458(apollo-demo)项目有两个客户端：

1. 10.32.21.19
2. 10.32.21.22

![initial-instance-list](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/initial-instance-list.png)

**灰度目标：**

* 当前有一个配置timeout=2000，我们希望对10.32.21.22灰度发布timeout=3000，对10.32.21.19仍然是timeout=2000。

![initial-config](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/initial-config.png)

## 5.2 创建灰度
首先点击application namespace右上角的`创建灰度`按钮。

![create-gray-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/create-gray-release.png)

点击确定后，灰度版本就创建成功了，页面会自动切换到`灰度版本`Tab。

![initial-gray-release-tab](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/initial-gray-release-tab.png)

## 5.3 灰度配置
点击`主版本的配置`中，timeout配置最右侧的`对此配置灰度`按钮

![initial-gray-release-tab](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/edit-gray-release-config.png)

在弹出框中填入要灰度的值：3000，点击提交。

![submit-gray-release-config](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/submit-gray-release-config.png)

![gray-release-config-submitted](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-config-submitted.png)

灰度配置完成后，确认灰度的配置和主版本配置

点击`灰度发布`按钮

![click-gray-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/click-gray-release.png)

与主版本的值和灰度版本已发布的值对比，确认将要发布的灰度配置

![gray-release-diff-items](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-diff-items.png)

## 5.4 配置灰度规则
切换到`灰度规则`Tab，点击`新增规则`按钮

![new-gray-release-rule](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/new-gray-release-rule.png)

在弹出框中`灰度的IP`下拉框会默认展示当前使用配置的机器列表，选择我们要灰度的IP。

![select-gray-release-ip](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/select-gray-release-ip.png)

![gray-release-ip-selected](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-ip-selected.png)

除了IP维度以外，从2.0.0版本开始还支持通过label来标识灰度的实例列表，适用于IP不固定的场景如`Kubernetes`。

手动输入想要设置的label标签，输入完成后点击点击添加按钮。

![manual-input-gray-release-label](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/manual-input-gray-release-label.png)

![manual-input-gray-release-label-2](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/manual-input-gray-release-label2.png)

![gray-release-rule-saved](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-rule-saved.png)

上述规则配置后，灰度配置会对AppId为`100004458`，IP为`10.32.21.22`或者`Label`标记为`myLabel`或`appLabel`的实例生效。

> 关于`Label`如何标记，可以参考[ApolloLabel](zh/client/java-sdk-user-guide?id=_1247-apollolabel)的配置说明。

如果下拉框中没找到需要的IP，说明机器还没从Apollo取过配置，可以点击手动输入IP来输入，输入完后点击添加按钮

![manual-input-gray-release-ip](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/manual-input-gray-release-ip.png)

![manual-input-gray-release-ip-2](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/manual-input-gray-release-ip-2.png)

>注：对于公共Namespace的灰度规则，需要先指定要灰度的appId，然后再选择IP和Label。

## 5.5 灰度发布
配置规则已经生效，不过灰度配置还没有发布。切换到`配置`Tab。

再次检查灰度的配置部分，如果没有问题，点击`灰度发布`。

![prepare-to-do-gray-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/prepare-to-do-gray-release.png)

在弹出框中可以看到主版本的值是2000，灰度版本即将发布的值是3000。填入其它信息后，点击发布。

![gray-release-confirm-dialog](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-confirm-dialog.png)

发布后，切换到`灰度实例列表`Tab，就能看到10.32.21.22已经使用了灰度发布的值。

![gray-release-instance-list](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-instance-list.png)

切换到`主版本`的`实例列表`，会看到主版本配置只有10.32.21.19在使用了。

![master-branch-instance-list](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/master-branch-instance-list.png)

后面可以继续配置的修改或规则的更改。配置的修改需要点击灰度发布后才会生效，规则的修改在规则点击完成后就会实时生效。

## 5.6 全量发布
如果灰度的配置测试下来比较理想，符合预期，那么就可以操作`全量发布`。

全量发布的效果是：

1. 灰度版本的配置会合并回主版本，在这个例子中，就是主版本的timeout会被更新成3000
2. 主版本的配置会自动进行一次发布
3. 在全量发布页面，可以选择是否保留当前灰度版本，默认为不保留。

![prepare-to-full-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/prepare-to-full-release.png)

![full-release-confirm-dialog](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/full-release-confirm-dialog.png)

![full-release-confirm-dialog-2](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/full-release-confirm-dialog-2.png)

我选择了不保留灰度版本，所以发布完的效果就是主版本的配置更新、灰度版本删除。点击主版本的实例列表，可以看到10.32.21.22和10.32.21.19都使用了主版本最新的配置。

![master-branch-instance-list-after-full-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/master-branch-instance-list-after-full-release.png)

## 5.7 放弃灰度
如果灰度版本不理想或者不需要了，可以点击`放弃灰度`。

![abandon-gray-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/abandon-gray-release.png)

## 5.8 发布历史
点击主版本的`发布历史`按钮，可以看到当前namespace的主版本以及灰度版本的发布历史。

![view-release-history](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/view-release-history.png)

![view-release-history-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/view-release-history-detail.png)

# 六、其它功能配置

## 6.1 配置查看权限

从1.1.0版本开始，apollo-portal增加了查看权限的支持，可以支持配置某个环境只允许项目成员查看私有Namespace的配置。

这里的项目成员是指：
1. 项目的管理员
2. 具备该私有Namespace在该环境下的修改或发布权限

配置方式很简单，用超级管理员账号登录后，进入`管理员工具 - 系统参数`页面新增或修改`configView.memberOnly.envs`配置项即可。

![configView.memberOnly.envs](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/configure-view-permissions.png)

## 6.2 配置访问密钥

Apollo从1.6.0版本开始增加访问密钥机制，从而只有经过身份验证的客户端才能访问敏感配置。如果应用开启了访问密钥，客户端需要配置密钥，否则无法获取配置。

1. 项目管理员打开管理密钥页面
    ![管理密钥入口](https://user-images.githubusercontent.com/837658/94990081-f4d3cd80-05ab-11eb-9470-fed5ec6de92e.png)

2. 为项目的每个环境生成访问密钥，注意默认是禁用的，建议在客户端都配置完成后再开启
    ![密钥配置页面](https://user-images.githubusercontent.com/837658/94990150-788dba00-05ac-11eb-9a12-727fdb872e42.png)

3. 客户端侧[配置访问密钥](zh/client/java-sdk-user-guide#_1244-配置访问密钥)

## 6.3 全局搜索配置项的系统参数设置

从2.4.0版本开始，apollo-portal增加了全局搜索配置项的功能，通过对配置项的key与value进行的模糊检索，找到拥有对应值的配置项在哪个应用、环境、集群、命名空间中被使用。为了防止在进行配置项的全局视角搜索时出现内存溢出（OOM）的问题，我们引入了一个系统参数`apollo.portal.search.perEnvMaxResults`。这个参数用于限制每个环境配置项单次最大搜索结果的数量。默认情况下，这个值被设置为`200`，但管理员可以根据实际需求进行调整。

**设置方法：**

1. 用超级管理员账号登录到Apollo配置中心的界面
2. 进入`管理员工具 - 系统参数`页面新增或修改`apollo.portal.search.perEnvMaxResults`配置项即可

请注意，修改系统参数可能会影响搜索功能的性能，因此在修改之前应该进行充分的测试，并确保理解参数的具体作用。

![System-parameterization-of-global-search-configuration-items](../images/System-parameterization-of-global-search-configuration-items.png)


## 6.4 appld+cluster维度下命名空间数量限制功能参数设置
从2.4.0版本开始，apollo-portal提供了appld+cluster维度下可以创建的命名空间数量上限校验的功能，此功能默认关闭，需要配置系统 `namespace.num.limit.enabled` 开启，同时提供了系统参数`namespace.num.limit`来动态配置appld+cluster维度下的Namespace数量上限值，默认为200个，考虑到一些基础组件如网关、消息队列、Redis、数据库等需要特殊处理，新增了系统参数`namespace.num.limit.white` 来配置校验白名单，不受Namespace数量上限的影响

**设置方法：**
1. 用超级管理员账号登录到Apollo配置中心的界面
2. 进入 `管理员工具 - 系统参数 - ConfigDB 配置管理` 页面新增或修改 `namespace.num.limit.enabled` 配置项为true/false 即可开启/关闭此功能，默认关闭

   ![item-num-limit-enabled](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-num-limit-enabled.png)

3. 进入 `管理员工具 - 系统参数 - ConfigDB 配置管理` 页面新增或修改 `namespace.num.limit` 配置项来配置单个appld+cluster下的namespace数量上限值，默认为200

   ![item-num-limit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-num-limit.png)

4. 进入 `管理员工具 - 系统参数 - ConfigDB 配置管理` 页面新增或修改 `namespace.num.limit.white` 配置项来配置namespace数量上限校验的白名单，多个AppId使用英文逗号分隔

   ![item-num-limit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-num-limit-white.png)



## 6.5 单个命名空间下的配置项数量限制
从2.4.0版本开始，apollo-portal提供了限制单个命名空间下的配置项数量的功能，此功能默认关闭，需要配置系统 `item.num.limit.enabled` 开启，同时提供了系统参数`item.num.limit`来动态配置单个Namespace下的item数量上限值

**设置方法：**
1. 用超级管理员账号登录到Apollo配置中心的界面
2. 进入`管理员工具 - 系统参数 - ConfigDB 配置管理`页面新增或修改`item.num.limit.enabled`配置项为true/false 即可开启/关闭此功能

   ![item-num-limit-enabled](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/item-num-limit-enabled.png)

3. 进入`管理员工具 - 系统参数 - ConfigDB 配置管理`页面新增或修改`item.num.limit`配置项来配置单个Namespace下的item数量上限值

   ![item-num-limit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/item-num-limit.png)




# 七、最佳实践

## 7.1 安全相关

配置中心作为基础服务，存储着公司非常重要的配置信息，所以安全因素需要大家重点关注，下面列举了一些注意事项供大家参考，也欢迎大家分享自己的实践案例。

### 7.1.1 认证

建议接入公司统一的身份认证系统，如 SSO、LDAP 等，接入方式可以参考[Portal 实现用户登录功能](zh/extension/portal-how-to-implement-user-login-function)

> 如果使用Apollo提供的Spring Security简单认证，务必记得要修改超级管理员apollo的密码

### 7.1.2 授权

Apollo 支持细粒度的权限控制，请务必根据实际情况做好权限控制：
1. [项目管理员权限](#_121-项目管理员权限)
    * Apollo 默认允许所有登录用户创建项目，如果只允许部分用户创建项目，可以开启[创建项目权限控制](zh/deployment/distributed-deployment-guide?id=_3110-rolecreate-applicationenabled-是否开启创建项目权限控制)
2. [配置编辑、发布权限](#_122-配置编辑、发布权限)
    * 配置编辑、发布权限支持按环境配置，比如开发环境开发人员可以自行完成配置编辑和发布的过程，但是生产环境发布权限交由测试或运维人员
    * 生产环境建议同时开启[发布审核](zh/deployment/distributed-deployment-guide?id=_322-namespacelockswitch-一次发布只能有一个人修改开关，用于发布审核)，从而控制一次配置发布只能由一个人修改，另一个人发布，确保配置修改得到充分检查
3. [配置查看权限](#_61-配置查看权限)
    * 可以指定某个环境只允许项目成员查看私有Namespace的配置，从而避免敏感配置泄露，如生产环境

### 7.1.3 系统访问

除了用户权限，在系统访问上也需要加以考虑：

1. `apollo-configservice`和`apollo-adminservice`是基于内网可信网络设计的，所以出于安全考虑，禁止`apollo-configservice`和`apollo-adminservice`直接暴露在公网
2. 对敏感配置可以考虑开启[访问秘钥](#_62-%e9%85%8d%e7%bd%ae%e8%ae%bf%e9%97%ae%e5%af%86%e9%92%a5)，从而只有经过身份验证的客户端才能访问敏感配置
3. 1.7.1及以上版本可以考虑为`apollo-adminservice`开启[访问控制](zh/deployment/distributed-deployment-guide?id=_326-admin-serviceaccesscontrolenabled-配置apollo-adminservice是否开启访问控制)，从而只有[受控的](zh/deployment/distributed-deployment-guide?id=_3112-admin-serviceaccesstokens-设置apollo-portal访问各环境apollo-adminservice所需的access-token)`apollo-portal`才能访问对应接口，增强安全性
4. 2.1.0及以上版本可以考虑为`eureka`开启[访问控制](zh/deployment/distributed-deployment-guide?id=_329-apolloeurekaserversecurityenabled-配置是否开启eureka-server的登录认证)，从而只有受控的`apollo-configservice`和`apollo-adminservice`可以注册到`eureka`，增强安全性
