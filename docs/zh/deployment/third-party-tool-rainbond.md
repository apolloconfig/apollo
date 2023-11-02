# &nbsp;



# 一、背景信息

当前文档描述如何通过云原生应用管理平台 [Rainbond](https://www.rainbond.com/?channel=apollo) 一键安装高可用 Apollo 集群。这种方式适合给不太了解 Kubernetes、容器化等复杂技术的用户使用，降低了在 Kubernetes 中部署 Apollo 的门槛。

## 1.1 Rainbond 与 Apollo 的结合

[Rainbond](https://www.rainbond.com/?channel=apollo) 是一款易于使用的开源云原生应用管理平台。

借助于它，用户可以在图形化界面中完成微服务的部署与运维。

借助 Kubernetes 和容器化技术的能力，将故障自愈、弹性伸缩等自动化运维能力赋能给用户的业务。

Rainbond 内置原生 Service Mesh 微服务框架，同时与 Spring Cloud、Dubbo 等其他微服务框架也有很好的整合体验。

故而大量的 Rainbond 用户也可能是 Apollo 分布式配置管理中心的用户。

这类用户不必再关心如何部署 Apollo 集群，Rainbond 团队将 Apollo 制作成为可以一键部署的应用模版，供开源用户免费下载安装。

这种安装方式极大的降低了用户使用 Apollo 集群的部署负担，目前支持 1.9.2，2.0.1版本。

当前的安装方式，默认集成了一套 `PRO` 环境，追加其他环境，参见后文中的高级特性章节。

## 1.2 关于应用模版

应用模版是面向 Rainbond 云原生应用管理平台的安装包，用户可以基于它一键安装业务系统到自己的 Rainbond 中去。无论这个业务系统多么复杂，应用模版都会将其抽象成为一个应用，裹挟着应用内所有组件的镜像、配置信息以及所有组件之间的关联关系一并安装起来。

# 二、前提条件

- 部署好的 Rainbond 云原生应用管理平台：例如 [快速体验版本](https://www.rainbond.com/docs/quick-start/quick-install/?channel=apollo)，可以在个人 PC 环境中以启动一个容器的代价运行。

- 可以连接到互联网。

# 三、快速开始

## 3.1 访问内置的开源应用商店

选择左侧的 **应用市场** 标签页，在页面中切换到 **开源应用商店** 标签页，搜索关键词 **apollo** 即可找到 Apollo 应用。

![apollo-1](https://grstatic.oss-cn-shanghai.aliyuncs.com/wechat/apollo/apollo2.0.1/apollo-1.jpg)

## 3.2 一键安装

点击 Apollo 右侧的 **安装** 可以进入安装页面，填写简单的信息之后，点击 **确定** 即可开始安装，页面自动跳转到拓扑视图。

![apollo-2](https://grstatic.oss-cn-shanghai.aliyuncs.com/wechat/apollo/apollo2.0.1/apollo-2.jpg)



参数说明：

| 选择项   | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 团队名称 | 用户自建的工作空间，以命名空间隔离                           |
| 集群名称 | 选择 Apollo 被部署到哪一个 K8s 集群                          |
| 选择应用 | 选择 Apollo 被部署到哪一个应用，应用中包含有若干有关联的组件 |
| 应用版本 | 选择 Apollo 的版本，目前可选版本为 1.9.2，2.0.1              |

等待几分钟后，Apollo 集群就会安装完成，并运行起来。

![apollo-3](https://grstatic.oss-cn-shanghai.aliyuncs.com/wechat/apollo/apollo2.0.1/apollo-3.jpg)

## 3.3 测试

访问组件 `Apollo-portal-2.0.1` 所提供的默认域名，即可登录 Apollo 控制台，在系统信息中，验证 `PRO` 环境已经就绪。

![apollo-4](https://grstatic.oss-cn-shanghai.aliyuncs.com/wechat/apollo/apollo2.0.1/apollo-4.jpg)

## 3.4 配置

在 Rainbond 中，可以基于图形化界面对 Apollo 集群进行配置。主要包括环境变量、配置文件挂载、插件配置三个方面。

- 环境变量：通过在不同的组件页面中的环境配置中，可以自定义环境变量。比如为 `Apollo-portal-2.0.1` 默认添加了 `APOLLO_PORTAL_ENVS=pro` 用于定义当前 portal 纳管的环境。

- 配置文件：通过在不同的组件页面中的环境配置中，可以为组件设置配置文件。
  
  - `Apollo-portal-2.0.1` 挂载 `/apollo-portal/config/apollo-env.properties` 用于定义不同环境的 meta 地址。
  
  - `Apollo-config-2.0.1` 挂载 `/apollo-configservice/config/application-github.properties` 用于声明当前环境 config 和 admin 的服务地址。

- 插件配置：在 Rainbond 中通过为 `Apollo-portal-2.0.1` `Apollo-config-2.0.1` 安装出口网络治理插件来定义下游调用地址，这是一种 Service Mesh 微服务治理的实现方式。通过定义下游服务的域名，来访问下游服务的指定端口。如在 `Apollo-portal-2.0.1` 的插件中，访问 `Apollo-config-2.0.1`  8080 端口的域名为 `apollo-config-pro` ，这也是配置中只定义域名，而不需要定义端口的原因。

# 四、高级特性

## 4.1 实例数量伸缩

Apollo 配置中心所包含的 `Apollo-portal-2.0.1` `Apollo-config-2.0.1` `Apollo-admin-2.0.1` 组件均使用 Deployment 控制器部署，通过 Rainbond 内置的 Service Mesh 微服务框架实现服务发现与通信。故而这三个组件均可以一键扩展多个实例，实现集群化部署。

以 `Apollo-portal-2.0.1` 为例，点击 **伸缩** ，修改 **实例数量** 后，点击 **设置** 即可。

![apollo-5](https://grstatic.oss-cn-shanghai.aliyuncs.com/wechat/apollo/apollo2.0.1/apollo-5.jpg)

## 4.2 追加环境

Apollo 配置中心支持对接多套环境，并使用统一的 Portal 页面进行管理。基于 Rainbond 一键安装而来的 Apollo 集群默认附带了 `PRO` 环境。接下来讲解在 Rainbond 场景中，如何追加一套 `DEV` 环境，假设在 `DEV` 环境中，通过 `apollo-config-dev`、`apollo-admin-dev`来分别访问 `Apollo-config-Dev` `Apollo-admin-Dev` 组件。

1. 再部署一套 Apollo 集群，并去除新集群中 `Apollo-portal-2.0.1` `ApolloPortalDB`组件。为了便于管理，修改 `Apollo-config-2.0.1` `Apollo-admin-2.0.1` 组件的名称。添加 `Apollo-portal-2.0.1` 到 `Apollo-config-Dev` `Apollo-admin-Dev` 的依赖。拓扑展示如下：

> 注意，这个步骤会触发连接信息环境变量冲突的情况，记得为 `Apollo-config-Dev` `Apollo-admin-Dev` 组件的对内端口，重新定义你喜欢的名字。

![apollo-6](https://grstatic.oss-cn-shanghai.aliyuncs.com/wechat/apollo/apollo2.0.1/apollo-6.jpg)

2. 在 **环境配置** 页面，修改 `Apollo-config-Dev` 的配置文件 `/apollo-configservice/config/application-github.properties` ，将 config 和 admin 的服务地址修改成为预期的值。

![apollo-10](https://static.goodrain.com/wechat/apollo/apollo-10.png)

3. 分别进入`Apollo-config-Dev` `Apollo-portal-2.0.1` 的插件页面，为其出口网络治理插件修改配置，Rainbond 内置的微服务框架，通过设定的域名（Domains）来定义下游服务的访问地址。以 `Apollo-portal-2.0.1` 为例，需要配置到 `Apollo-config-Dev` `Apollo-admin-Dev` 的访问域名。

![apollo-7](https://static.goodrain.com/wechat/apollo/apollo-7.png)

配置完成后点击 **更新配置**, `Apollo-portal-2.0.1` 就可以通过 apollo-config-dev 这个域名访问到 `Apollo-config-Dev`。

同理，`Apollo-config-Dev` 需要配置到 `Apollo-admin-Dev` 的访问域名。配置完成后更新配置。

4. 修改 `Apollo-portal-2.0.1` 的配置，来加入新的 `DEV` 环境。

修改环境变量 `APOLLO_PORTAL_ENVS` 的值，加入 `dev` 环境。

![apollo-8](https://grstatic.oss-cn-shanghai.aliyuncs.com/wechat/apollo/apollo2.0.1/apollo-7.jpg)

修改配置文件 `/apollo-portal/config/apollo-env.properties` ，写入 `dev` 环境的 meta 地址。

![apollo-9](https://static.goodrain.com/wechat/apollo/apollo-9.png)

更新 `Apollo-portal-2.0.1` 组件，使所有配置生效。查看系统信息，验证环境加入完成。

![apollo-11](https://static.goodrain.com/wechat/apollo/apollo-11.png)
