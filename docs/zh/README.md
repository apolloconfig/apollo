<img src="https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/logo/logo-simple.png" alt="apollo-logo" width="40%">

# Introduction

Apollo（阿波罗）是一款可靠的分布式配置管理中心，诞生于携程框架研发部，能够集中化管理应用不同环境、不同集群的配置，配置修改后能够实时推送到应用端，并且具备规范的权限、流程治理等特性，适用于微服务配置管理场景。

服务端基于Spring Boot和Spring Cloud开发，打包后可以直接运行，不需要额外安装Tomcat等应用容器。

Java客户端不依赖任何框架，能够运行于所有Java运行时环境，同时对Spring/Spring Boot环境也有较好的支持。

.Net客户端不依赖任何框架，能够运行于所有.Net运行时环境。

更多产品介绍参见[Apollo配置中心介绍](zh/design/apollo-introduction)

本地快速部署请参见[Quick Start](zh/deployment/quick-start)

演示环境（Demo）:

- [http://81.68.181.139](http://81.68.181.139/)
- 账号/密码:apollo/admin

> 如访问GitHub速度缓慢，可以访问[Gitee镜像](https://gitee.com/apolloconfig/apollo)，不定期同步

# Screenshots
![配置界面](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-home-screenshot.jpg)

# Features
* **统一管理不同环境、不同集群的配置**
  * Apollo提供了一个统一界面集中式管理不同环境（environment）、不同集群（cluster）、不同命名空间（namespace）的配置。
  * 同一份代码部署在不同的集群，可以有不同的配置，比如zk的地址等
  * 通过命名空间（namespace）可以很方便的支持多个不同应用共享同一份配置，同时还允许应用对共享的配置进行覆盖
  * 配置界面支持多语言（中文，English）

* **配置修改实时生效（热发布）**
  * 用户在Apollo修改完配置并发布后，客户端能实时（1秒）接收到最新的配置，并通知到应用程序。

* **版本发布管理**
  * 所有的配置发布都有版本概念，从而可以方便的支持配置的回滚。

* **灰度发布**
  * 支持配置的灰度发布，比如点了发布后，只对部分应用实例生效，等观察一段时间没问题后再推给所有应用实例。

- **配置项的全局视角搜索**
  - 通过对配置项的key与value进行的模糊检索，找到拥有对应值的配置项在哪个应用、环境、集群、命名空间中被使用。
  - 通过高亮显示、分页与跳转配置等操作，便于让管理员以及SRE角色快速、便捷地找到与更改资源的配置值。

* **权限管理、发布审核、操作审计**
  * 应用和配置的管理都有完善的权限管理机制，对配置的管理还分为了编辑和发布两个环节，从而减少人为的错误。
  * 所有的操作都有审计日志，可以方便的追踪问题。

* **客户端配置信息监控**
  * 可以方便的看到配置在被哪些实例使用

* **提供Java和.Net原生客户端**
  * 提供了Java和.Net的原生客户端，方便应用集成
  * 支持Spring Placeholder，Annotation和Spring Boot的ConfigurationProperties，方便应用使用（需要Spring 3.1.1+）
  * 同时提供了Http接口，非Java和.Net应用也可以方便的使用

* **提供开放平台API**
  * Apollo自身提供了比较完善的统一配置管理界面，支持多环境、多数据中心配置管理、权限、流程治理等特性。
  * 不过Apollo出于通用性考虑，对配置的修改不会做过多限制，只要符合基本的格式就能够保存。
  * 在我们的调研中发现，对于有些使用方，它们的配置可能会有比较复杂的格式，如xml, json，需要对格式做校验。
  * 还有一些使用方如DAL，不仅有特定的格式，而且对输入的值也需要进行校验后方可保存，如检查数据库、用户名和密码是否匹配。
  * 对于这类应用，Apollo支持应用方通过开放接口在Apollo进行配置的修改和发布，并且具备完善的授权和权限控制

* **部署简单**
  * 配置中心作为基础服务，可用性要求非常高，这就要求Apollo对外部依赖尽可能地少
  * 目前唯一的外部依赖是MySQL，所以部署非常简单，只要安装好Java和MySQL就可以让Apollo跑起来
  * Apollo还提供了打包脚本，一键就可以生成所有需要的安装包，并且支持自定义运行时参数

# Release Notes
  * [版本发布历史](https://github.com/apolloconfig/apollo/releases)

# Presentation
  * [开源配置中心Apollo的设计与实现](http://www.itdks.com/dakalive/detail/3420)
    * [Slides](https://github.com/apolloconfig/apollo-community/blob/master/slides/design-and-implementation-of-apollo.pdf)
  * [配置中心，让微服务更『智能』](https://2018.qconshanghai.com/presentation/799)
    * [Slides](https://github.com/apolloconfig/apollo-community/blob/master/slides/configuration-center-makes-microservices-smart.pdf)

# Publication
  * [开源配置中心Apollo的设计与实现](https://www.infoq.cn/article/open-source-configuration-center-apollo)
  * [配置中心，让微服务更『智能』](https://mp.weixin.qq.com/s/iDmYJre_ULEIxuliu1EbIQ)

# Support
<table>
  <thead>
    <th>Apollo技术支持②群<br />群号：904287263（未满）</th>
    <th>Apollo技术支持⑤群<br />群号：914839843（已满）</th>
    <th>Apollo技术支持④群<br />群号：516773934（已满）</th>
    <th>Apollo技术支持③群<br />群号：742035428（已满）</th>
    <th>Apollo技术支持①群<br />群号：375526581（已满）</th>
  </thead>
  <tbody>
    <tr>
      <td><img src="https://cdn.jsdelivr.net/gh/apolloconfig/apollo-community@master/images/tech-support/tech-support-qq-2.png" alt="tech-support-qq-2"></td>
      <td><img src="https://cdn.jsdelivr.net/gh/apolloconfig/apollo-community@master/images/tech-support/tech-support-qq-5.png" alt="tech-support-qq-5"></td>
      <td><img src="https://cdn.jsdelivr.net/gh/apolloconfig/apollo-community@master/images/tech-support/tech-support-qq-4.png" alt="tech-support-qq-4"></td>
      <td><img src="https://cdn.jsdelivr.net/gh/apolloconfig/apollo-community@master/images/tech-support/tech-support-qq-3.png" alt="tech-support-qq-3"></td>
      <td><img src="https://cdn.jsdelivr.net/gh/apolloconfig/apollo-community@master/images/tech-support/tech-support-qq-1.png" alt="tech-support-qq-1"></td>
    </tr>
  </tbody>
</table>

# License
The project is licensed under the [Apache 2 license](https://github.com/apolloconfig/apollo/blob/master/LICENSE).
