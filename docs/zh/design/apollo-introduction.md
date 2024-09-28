# &nbsp;
# 1、What is Apollo

## 1.1 背景

随着程序功能的日益复杂，程序的配置日益增多：各种功能的开关、参数的配置、服务器的地址……

对程序配置的期望值也越来越高：配置修改后实时生效，灰度发布，分环境、分集群管理配置，完善的权限、审核机制……

在这样的大环境下，传统的通过配置文件、数据库等方式已经越来越无法满足开发人员对配置管理的需求。

Apollo配置中心应运而生！

## 1.2 Apollo简介

Apollo（阿波罗）是一款可靠的分布式配置管理中心，诞生于携程框架研发部，能够集中化管理应用不同环境、不同集群的配置，配置修改后能够实时推送到应用端，并且具备规范的权限、流程治理等特性，适用于微服务配置管理场景。

Apollo支持4个维度管理Key-Value格式的配置：
	
1. application (应用)
2. environment (环境)
3. cluster (集群)
4. namespace (命名空间)

同时，Apollo基于开源模式开发，开源地址：<a href="https://github.com/ctripcorp/apollo" target="_blank">https://github.com/ctripcorp/apollo</a>

## 1.3 配置基本概念

既然Apollo定位于配置中心，那么在这里有必要先简单介绍一下什么是配置。

按照我们的理解，配置有以下几个属性：

* **配置是独立于程序的只读变量**
	* 配置首先是独立于程序的，同一份程序在不同的配置下会有不同的行为。
	* 其次，配置对于程序是只读的，程序通过读取配置来改变自己的行为，但是程序不应该去改变配置。
	* 常见的配置有：DB Connection Str、Thread Pool Size、Buffer Size、Request Timeout、Feature Switch、Server Urls等。

* **配置伴随应用的整个生命周期**
	* 配置贯穿于应用的整个生命周期，应用在启动时通过读取配置来初始化，在运行时根据配置调整行为。

* **配置可以有多种加载方式**
	* 配置也有很多种加载方式，常见的有程序内部hard code，配置文件，环境变量，启动参数，基于数据库等

* **配置需要治理**
	* 权限控制
		* 由于配置能改变程序的行为，不正确的配置甚至能引起灾难，所以对配置的修改必须有比较完善的权限控制
	* 不同环境、集群配置管理
		* 同一份程序在不同的环境（开发，测试，生产）、不同的集群（如不同的数据中心）经常需要有不同的配置，所以需要有完善的环境、集群配置管理
	* 框架类组件配置管理
		* 还有一类比较特殊的配置 - 框架类组件配置，比如CAT客户端的配置。
		* 虽然这类框架类组件是由其他团队开发、维护，但是运行时是在业务实际应用内的，所以本质上可以认为框架类组件也是应用的一部分。
		* 这类组件对应的配置也需要有比较完善的管理方式。

# 2、Why Apollo

正是基于配置的特殊性，所以Apollo从设计之初就立志于成为一个有治理能力的配置发布平台，目前提供了以下的特性：

* **统一管理不同环境、不同集群的配置**
	* Apollo提供了一个统一界面集中式管理不同环境（environment）、不同集群（cluster）、不同命名空间（namespace）的配置。
	* 同一份代码部署在不同的集群，可以有不同的配置，比如zookeeper的地址等
	* 通过命名空间（namespace）可以很方便地支持多个不同应用共享同一份配置，同时还允许应用对共享的配置进行覆盖

* **配置修改实时生效（热发布）**
	* 用户在Apollo修改完配置并发布后，客户端能实时（1秒）接收到最新的配置，并通知到应用程序

* **版本发布管理**
	* 所有的配置发布都有版本概念，从而可以方便地支持配置的回滚

* **灰度发布**
	* 支持配置的灰度发布，比如点了发布后，只对部分应用实例生效，等观察一段时间没问题后再推给所有应用实例

* **配置项的全局视角搜索**
	* 通过对配置项的key与value进行的模糊检索，找到拥有对应值的配置项在哪个应用、环境、集群、命名空间中被使用
	* 通过高亮显示、分页与跳转配置等操作，便于让管理员以及SRE角色快速、便捷地找到与更改资源的配置值

* **权限管理、发布审核、操作审计**
  * 应用和配置的管理都有完善的权限管理机制，对配置的管理还分为了编辑和发布两个环节，从而减少人为的错误。
  * 所有的操作都有审计日志，可以方便地追踪问题

* **客户端配置信息监控**
  * 可以在界面上方便地看到配置在被哪些实例使用

* **提供Java和.Net原生客户端**
  * 提供了Java和.Net的原生客户端，方便应用集成
  * 支持Spring Placeholder, Annotation和Spring Boot的ConfigurationProperties，方便应用使用（需要Spring 3.1.1+）
  * 同时提供了Http接口，非Java和.Net应用也可以方便地使用

* **提供开放平台API**
  * Apollo自身提供了比较完善的统一配置管理界面，支持多环境、多数据中心配置管理、权限、流程治理等特性。不过Apollo出于通用性考虑，不会对配置的修改做过多限制，只要符合基本的格式就能保存，不会针对不同的配置值进行针对性的校验，如数据库用户名、密码，Redis服务地址等
  * 对于这类应用配置，Apollo支持应用方通过开放平台API在Apollo进行配置的修改和发布，并且具备完善的授权和权限控制

* **部署简单**
  * 配置中心作为基础服务，可用性要求非常高，这就要求Apollo对外部依赖尽可能地少
  * 目前唯一的外部依赖是MySQL，所以部署非常简单，只要安装好Java和MySQL就可以让Apollo跑起来
  * Apollo还提供了打包脚本，一键就可以生成所有需要的安装包，并且支持自定义运行时参数

# 3、Apollo at a glance

## 3.1 基础模型

如下即是Apollo的基础模型：

1. 用户在配置中心对配置进行修改并发布
2. 配置中心通知Apollo客户端有配置更新
3. Apollo客户端从配置中心拉取最新的配置、更新本地配置并通知到应用

![basic-architecture](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/basic-architecture.png)

## 3.2 界面概览

![apollo-home-screenshot](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-home-screenshot.jpg)

上图是Apollo配置中心中一个项目的配置首页

* 在页面左上方的环境列表模块展示了所有的环境和集群，用户可以随时切换。
* 页面中央展示了两个namespace(application和FX.apollo)的配置信息，默认按照表格模式展示、编辑。用户也可以切换到文本模式，以文件形式查看、编辑。
* 页面上可以方便地进行发布、回滚、灰度、授权、查看更改历史和发布历史等操作

## 3.3 添加/修改配置项

用户可以通过配置中心界面方便的添加/修改配置项，更多使用说明请参见[应用接入指南](zh/portal/apollo-user-guide)

![edit-item-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/edit-item-entry.png)

输入配置信息：

![edit-item](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/edit-item.png)

## 3.4 发布配置

通过配置中心发布配置：

![publish-items](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/publish-items-entry.png)

填写发布信息：

![publish-items](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/publish-items.png)

## 3.5 客户端获取配置（Java API样例）

配置发布后，就能在客户端获取到了，以Java为例，获取配置的示例代码如下。Apollo客户端还支持和Spring整合，更多客户端使用说明请参见[Java客户端使用指南](zh/client/java-sdk-user-guide)和[.Net客户端使用指南](zh/client/java-sdk-user-guide)。

```java
Config config = ConfigService.getAppConfig();
Integer defaultRequestTimeout = 200;
Integer requestTimeout = config.getIntProperty("requestTimeout", defaultRequestTimeout);
```

## 3.6 客户端监听配置变化

通过上述获取配置代码，应用就能实时获取到最新的配置了。

不过在某些场景下，应用还需要在配置变化时获得通知，比如数据库连接的切换等，所以Apollo还提供了监听配置变化的功能，Java示例如下：

```java
Config config = ConfigService.getAppConfig();
config.addChangeListener(new ConfigChangeListener() {
  @Override
  public void onChange(ConfigChangeEvent changeEvent) {
    for (String key : changeEvent.changedKeys()) {
      ConfigChange change = changeEvent.getChange(key);
      System.out.println(String.format(
        "Found change - key: %s, oldValue: %s, newValue: %s, changeType: %s",
        change.getPropertyName(), change.getOldValue(),
        change.getNewValue(), change.getChangeType()));
     }
  }
});
```

## 3.7 Spring集成样例

Apollo和Spring也可以很方便地集成，只需要标注`@EnableApolloConfig`后就可以通过`@Value`获取配置信息：

```java
@Configuration
@EnableApolloConfig
public class AppConfig {}
```

```java
@Component
public class SomeBean {
    //timeout的值会自动更新
    @Value("${request.timeout:200}")
    private int timeout;
}
```

# 4、Apollo in depth

通过上面的介绍，相信大家已经对Apollo有了一个初步的了解，并且相信已经覆盖到了大部分的使用场景。

接下来会主要介绍Apollo的cluster管理（集群）、namespace管理（命名空间）和对应的配置获取规则。

## 4.1 Core Concepts

在介绍高级特性前，我们有必要先来了解一下Apollo中的几个核心概念：

1. **application (应用)**
	* 这个很好理解，就是实际使用配置的应用，Apollo客户端在运行时需要知道当前应用是谁，从而可以去获取对应的配置
	* 每个应用都需要有唯一的身份标识 -- appId，我们认为应用身份是跟着代码走的，所以需要在代码中配置，具体信息请参见[Java客户端使用指南](zh/client/java-sdk-user-guide)。

2. **environment (环境)**
	* 配置对应的环境，Apollo客户端在运行时需要知道当前应用处于哪个环境，从而可以去获取应用的配置
	* 我们认为环境和代码无关，同一份代码部署在不同的环境就应该能够获取到不同环境的配置
	* 所以环境默认是通过读取机器上的配置（server.properties中的env属性）指定的，不过为了开发方便，我们也支持运行时通过System Property等指定，具体信息请参见[Java客户端使用指南](zh/client/java-sdk-user-guide)。

3. **cluster (集群)**
	* 一个应用下不同实例的分组，比如典型的可以按照数据中心分，把上海机房的应用实例分为一个集群，把北京机房的应用实例分为另一个集群。
	* 对不同的cluster，同一个配置可以有不一样的值，如zookeeper地址。
	* 集群默认是通过读取机器上的配置（server.properties中的idc属性）指定的，不过也支持运行时通过System Property指定，具体信息请参见[Java客户端使用指南](zh/client/java-sdk-user-guide)。

4. **namespace (命名空间)**
	* 一个应用下不同配置的分组，可以简单地把namespace类比为文件，不同类型的配置存放在不同的文件中，如数据库配置文件，RPC配置文件，应用自身的配置文件等
	* 应用可以直接读取到公共组件的配置namespace，如DAL，RPC等
	* 应用也可以通过继承公共组件的配置namespace来对公共组件的配置做调整，如DAL的初始数据库连接数

## 4.2 自定义Cluster
> 【本节内容仅对应用需要对不同集群应用不同配置才需要，如没有相关需求，可以跳过本节】

比如我们有应用在A数据中心和B数据中心都有部署，那么如果希望两个数据中心的配置不一样的话，我们可以通过新建cluster来解决。

### 4.2.1 新建Cluster

新建Cluster只有项目的管理员才有权限，管理员可以在页面左侧看到“添加集群”按钮。

![create-cluster](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-cluster.png)

点击后就进入到集群添加页面，一般情况下可以按照数据中心来划分集群，如SHAJQ、SHAOY等。

不过也支持自定义集群，比如可以为A机房的某一台机器和B机房的某一台机创建一个集群，使用一套配置。

![create-cluster-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-cluster-detail.png)

### 4.2.2 在Cluster中添加配置并发布

集群添加成功后，就可以为该集群添加配置了，首先需要按照下图所示切换到SHAJQ集群，之后配置添加流程和[3.3 添加/修改配置项](#_33-添加修改配置项)一样，这里就不再赘述了。

![cluster-created](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/cluster-created.png)

### 4.2.3 指定应用实例所属的Cluster

Apollo会默认使用应用实例所在的数据中心作为cluster，所以如果两者一致的话，不需要额外配置。

如果cluster和数据中心不一致的话，那么就需要通过System Property方式来指定运行时cluster：

* -Dapollo.cluster=SomeCluster
* 这里注意`apollo.cluster`为全小写

## 4.3 自定义Namespace
> 【本节仅对公共组件配置或需要多个应用共享配置才需要，如没有相关需求，可以跳过本节】

如果应用有公共组件（如hermes-producer，cat-client等）供其它应用使用，就需要通过自定义namespace来实现公共组件的配置。

### 4.3.1 新建Namespace

以hermes-producer为例，需要先新建一个namespace，新建namespace只有项目的管理员才有权限，管理员可以在页面左侧看到“添加Namespace”按钮。

![create-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-namespace.png)

点击后就进入namespace添加页面，Apollo会把应用所属的部门作为namespace的前缀，如FX。

![create-namespace-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-namespace-detail.png)

### 4.3.2 关联到环境和集群

Namespace创建完，需要选择在哪些环境和集群下使用

![link-namespace-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/link-namespace-detail.png)

### 4.3.3 在Namespace中添加配置项

接下来在这个新建的namespace下添加配置项

![add-item-in-new-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/add-item-in-new-namespace.png)

添加完成后就能在FX.Hermes.Producer的namespace中看到配置。

![item-created-in-new-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/item-created-in-new-namespace.png)

### 4.3.4 发布namespace的配置

![publish-items-in-new-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/publish-items-in-new-namespace.png)

### 4.3.5 客户端获取Namespace配置

对自定义namespace的配置获取，稍有不同，需要程序传入namespace的名字。Apollo客户端还支持和Spring整合，更多客户端使用说明请参见[Java客户端使用指南](zh/client/java-sdk-user-guide)和[.Net客户端使用指南](zh/client/java-sdk-user-guide)。

```java
Config config = ConfigService.getConfig("FX.Hermes.Producer");
Integer defaultSenderBatchSize = 200;
Integer senderBatchSize = config.getIntProperty("sender.batchsize", defaultSenderBatchSize);
```

### 4.3.6 客户端监听Namespace配置变化

```java
Config config = ConfigService.getConfig("FX.Hermes.Producer");
config.addChangeListener(new ConfigChangeListener() {
  @Override
  public void onChange(ConfigChangeEvent changeEvent) {
    System.out.println("Changes for namespace " + changeEvent.getNamespace());
    for (String key : changeEvent.changedKeys()) {
      ConfigChange change = changeEvent.getChange(key);
      System.out.println(String.format(
        "Found change - key: %s, oldValue: %s, newValue: %s, changeType: %s",
        change.getPropertyName(), change.getOldValue(),
        change.getNewValue(), change.getChangeType()));
     }
  }
});
```

### 4.3.7 Spring集成样例

```java
@Configuration
@EnableApolloConfig("FX.Hermes.Producer")
public class AppConfig {}
```

```java
@Component
public class SomeBean {
    //timeout的值会自动更新
    @Value("${request.timeout:200}")
    private int timeout;
}
```

## 4.4 配置获取规则
> 【本节仅当应用自定义了集群或namespace才需要，如无相关需求，可以跳过本节】

在有了cluster概念后，配置的规则就显得重要了。

比如应用部署在A机房，但是并没有在Apollo新建cluster，这个时候Apollo的行为是怎样的？

或者在运行时指定了cluster=SomeCluster，但是并没有在Apollo新建cluster，这个时候Apollo的行为是怎样的？

接下来就来介绍一下配置获取的规则。

### 4.4.1 应用自身配置的获取规则

当应用使用下面的语句获取配置时，我们称之为获取应用自身的配置，也就是应用自身的application namespace的配置。

```java
Config config = ConfigService.getAppConfig();
```

对这种情况的配置获取规则，简而言之如下：

1. 首先查找运行时cluster的配置（通过apollo.cluster指定）
2. 如果没有找到，则查找数据中心cluster的配置
3. 如果还是没有找到，则返回默认cluster的配置

图示如下：

![application-config-precedence](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/application-config-precedence.png)

所以如果应用部署在A数据中心，但是用户没有在Apollo创建cluster，那么获取的配置就是默认cluster（default）的。

如果应用部署在A数据中心，同时在运行时指定了SomeCluster，但是没有在Apollo创建cluster，那么获取的配置就是A数据中心cluster的配置，如果A数据中心cluster没有配置的话，那么获取的配置就是默认cluster（default）的。

### 4.4.2 公共组件配置的获取规则

以`FX.Hermes.Producer`为例，hermes producer是hermes发布的公共组件。当使用下面的语句获取配置时，我们称之为获取公共组件的配置。

```java
Config config = ConfigService.getConfig("FX.Hermes.Producer");
```

对这种情况的配置获取规则，简而言之如下：

1. 首先获取当前应用下的`FX.Hermes.Producer` namespace的配置
2. 然后获取hermes应用下`FX.Hermes.Producer` namespace的配置
3. 上面两部分配置的并集就是最终使用的配置，如有key一样的部分，以当前应用优先

图示如下：

![public-namespace-config-precedence](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-config-precedence.png)

通过这种方式，就实现了对框架类组件的配置管理，框架组件提供方提供配置的默认值，应用如果有特殊需求，可以自行覆盖。

## 4.5 总体设计

![overall-architecture](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/overall-architecture.png)

上图简要描述了Apollo的总体设计，我们可以从下往上看：

* Config Service提供配置的读取、推送等功能，服务对象是Apollo客户端
* Admin Service提供配置的修改、发布等功能，服务对象是Apollo Portal（管理界面）
* Config Service和Admin Service都是多实例、无状态部署，所以需要将自己注册到Eureka中并保持心跳
* 在Eureka之上我们架了一层Meta Server用于封装Eureka的服务发现接口
* Client通过域名访问Meta Server获取Config Service服务列表（IP+Port），而后直接通过IP+Port访问服务，同时在Client侧会做load balance、错误重试
* Portal通过域名访问Meta Server获取Admin Service服务列表（IP+Port），而后直接通过IP+Port访问服务，同时在Portal侧会做load balance、错误重试
* 为了简化部署，我们实际上会把Config Service、Eureka和Meta Server三个逻辑角色部署在同一个JVM进程中

### 4.5.1 Why Eureka

为什么我们采用Eureka作为服务注册中心，而不是使用传统的zk、etcd呢？我大致总结了一下，有以下几方面的原因：

* 它提供了完整的Service Registry和Service Discovery实现
	* 首先是提供了完整的实现，并且也经受住了Netflix自己的生产环境考验，相对使用起来会比较省心。
* 和Spring Cloud无缝集成
	* 我们的项目本身就使用了Spring Cloud和Spring Boot，同时Spring Cloud还有一套非常完善的开源代码来整合Eureka，所以使用起来非常方便。
	* 另外，Eureka还支持在我们应用自身的容器中启动，也就是说我们的应用启动完之后，既充当了Eureka的角色，同时也是服务的提供者。这样就极大的提高了服务的可用性。
	* **这一点是我们选择Eureka而不是zk、etcd等的主要原因，为了提高配置中心的可用性和降低部署复杂度，我们需要尽可能地减少外部依赖。**
* Open Source
	* 最后一点是开源，由于代码是开源的，所以非常便于我们了解它的实现原理和排查问题。

## 4.6 客户端设计

![client-architecture](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/client-architecture.png)

上图简要描述了Apollo客户端的实现原理：

1. 客户端和服务端保持了一个长连接，从而能第一时间获得配置更新的推送。
2. 客户端还会定时从Apollo配置中心服务端拉取应用的最新配置。
    * 这是一个fallback机制，为了防止推送机制失效导致配置不更新
    * 客户端定时拉取会上报本地版本，所以一般情况下，对于定时拉取的操作，服务端都会返回304 - Not Modified
    * 定时频率默认为每5分钟拉取一次，客户端也可以通过在运行时指定System Property: `apollo.refreshInterval`来覆盖，单位为分钟。
3. 客户端从Apollo配置中心服务端获取到应用的最新配置后，会保存在内存中
4. 客户端会把从服务端获取到的配置在本地文件系统缓存一份
    * 在遇到服务不可用，或网络不通的时候，依然能从本地恢复配置
5. 应用程序从Apollo客户端获取最新的配置、订阅配置更新通知

### 4.6.1 配置更新推送实现

前面提到了Apollo客户端和服务端保持了一个长连接，从而能第一时间获得配置更新的推送。

长连接实际上我们是通过Http Long Polling实现的，具体而言：

* 客户端发起一个Http请求到服务端
* 服务端会保持住这个连接60秒
    * 如果在60秒内有客户端关心的配置变化，被保持住的客户端请求会立即返回，并告知客户端有配置变化的namespace信息，客户端会据此拉取对应namespace的最新配置
    * 如果在60秒内没有客户端关心的配置变化，那么会返回Http状态码304给客户端
* 客户端在收到服务端请求后会立即重新发起连接，回到第一步

考虑到会有数万客户端向服务端发起长连，在服务端我们使用了async servlet(Spring DeferredResult)来服务Http Long Polling请求。

## 4.7 可用性考虑

配置中心作为基础服务，可用性要求非常高，下面的表格描述了不同场景下Apollo的可用性：

| 场景                   | 影响                                 | 降级                                  | 原因                                                                                    |
|------------------------|--------------------------------------|---------------------------------------|-----------------------------------------------------------------------------------------|
| 某台config service下线 | 无影响                               |                                       | Config service无状态，客户端重连其它config service                                      |
| 所有config service下线 | 客户端无法读取最新配置，Portal无影响 | 客户端重启时,可以读取本地缓存配置文件 |                                                                                         |
| 某台admin service下线  | 无影响                               |                                       | Admin service无状态，Portal重连其它admin service                                        |
| 所有admin service下线  | 客户端无影响，portal无法更新配置     |                                       |                                                                                         |
| 某台portal下线         | 无影响                               |                                       | Portal域名通过slb绑定多台服务器，重试后指向可用的服务器                                 |
| 全部portal下线         | 客户端无影响，portal无法更新配置     |                                       |                                                                                         |
| 某个数据中心下线       | 无影响                               |                                       | 多数据中心部署，数据完全同步，Meta Server/Portal域名通过slb自动切换到其它存活的数据中心 |

# 5、Contribute to Apollo

Apollo从开发之初就是以开源模式开发的，所以也非常欢迎有兴趣、有余力的朋友一起加入进来。

服务端开发使用的是Java，基于Spring Cloud和Spring Boot框架。客户端目前提供了Java和.Net两种实现。

GitHub地址：<a href="https://github.com/ctripcorp/apollo" target="_blank">https://github.com/ctripcorp/apollo</a>

欢迎大家发起Pull Request！
