# &nbsp;
# 一、总体设计

## 1.1 基础模型

如下即是Apollo的基础模型：

1. 用户在配置中心对配置进行修改并发布
2. 配置中心通知Apollo客户端有配置更新
3. Apollo客户端从配置中心拉取最新的配置、更新本地配置并通知到应用

![basic-architecture](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/basic-architecture.png)

## 1.2 架构模块

下图是Apollo架构模块的概览，详细说明可以参考[Apollo配置中心架构剖析](https://mp.weixin.qq.com/s/-hUaQPzfsl9Lm3IqQW3VDQ)。
![overall-architecture](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/overall-architecture.png)

上图简要描述了Apollo的总体设计，我们可以从下往上看：

* Config Service提供配置的读取、推送等功能，服务对象是Apollo客户端

```mermaid
sequenceDiagram
	Client ->> Config Service: request
    Config Service ->> ConfigDB: request
    ConfigDB -->> Config Service: ack
	Config Service -->> Client: ack
```



* Admin Service提供配置的修改、发布等功能，服务对象是Apollo Portal（管理界面）

```mermaid
sequenceDiagram
	Portal ->> Admin Service: r/w, publish appId/cluster/namespace
	Admin Service ->> ConfigDB: r/w, publish appId/cluster/namespace
	ConfigDB -->> Admin Service: ack
	Admin Service -->> Portal: ack
```



* Config Service和Admin Service都是多实例、无状态部署，所以需要将自己注册到Eureka中并保持心跳
* 在Eureka之上我们架了一层Meta Server用于封装Eureka的服务发现接口

```mermaid
sequenceDiagram
    Client or Portal ->> Meta Server: discovery service's instances
	Meta Server ->> Eureka: discovery service's instances
	Eureka -->> Meta Server: service's instances
	Meta Server -->> Client or Portal: service's instances
```



* Client通过域名访问Meta Server获取Config Service服务列表（IP+Port），而后直接通过IP+Port访问服务，同时在Client侧会做load balance、错误重试

```mermaid
sequenceDiagram
	Client ->> Meta Server: discovery Config Service's instances
	Meta Server -->> Client: Config Service's instances(Multiple IP+Port)
	loop until success
		Client ->> Client: load balance choose a Config Service instance
		Client ->> Config Service: request
		Config Service -->> Client: ack
	end
```



* Portal通过域名访问Meta Server获取Admin Service服务列表（IP+Port），而后直接通过IP+Port访问服务，同时在Portal侧会做load balance、错误重试

```mermaid
sequenceDiagram
	Portal ->> Meta Server: discovery Admin Service's instances
	Meta Server -->> Portal: Admin Service's instances(Multiple IP+Port)
	loop until success
		Portal ->> Portal: load balance choose a Admin Service instance
		Portal ->> Admin Service: request
		Admin Service -->> Portal: ack
	end
```



* 为了简化部署，我们实际上会把Config Service、Eureka和Meta Server三个逻辑角色部署在同一个JVM进程中

```mermaid
graph
	subgraph JVM Process
		1[Config Service]
		2[Eureka]
		3[Meta Server]
	end
```
实际部署的架构可以参考[部署架构](zh/deployment/deployment-architecture.md)

### 1.2.1 Why Eureka

为什么我们采用Eureka作为服务注册中心，而不是使用传统的zk、etcd呢？我大致总结了一下，有以下几方面的原因：

* 它提供了完整的Service Registry和Service Discovery实现
	* 首先是提供了完整的实现，并且也经受住了Netflix自己的生产环境考验，相对使用起来会比较省心。
* 和Spring Cloud无缝集成
	* 我们的项目本身就使用了Spring Cloud和Spring Boot，同时Spring Cloud还有一套非常完善的开源代码来整合Eureka，所以使用起来非常方便。
	* 另外，Eureka还支持在我们应用自身的容器中启动，也就是说我们的应用启动完之后，既充当了Eureka的角色，同时也是服务的提供者。这样就极大的提高了服务的可用性。
	* **这一点是我们选择Eureka而不是zk、etcd等的主要原因，为了提高配置中心的可用性和降低部署复杂度，我们需要尽可能地减少外部依赖。**
* Open Source
	* 最后一点是开源，由于代码是开源的，所以非常便于我们了解它的实现原理和排查问题。

## 1.3 各模块概要介绍

### 1.3.1 Config Service

* 提供配置获取接口

```mermaid
sequenceDiagram
	Client ->> Config Service: get content of appId/cluster/namespace
	opt if namespace is not cached
		Config Service ->> ConfigDB: get content of appId/cluster/namespace
		ConfigDB -->> Config Service: content of appId/cluster/namespace
	end
	Config Service -->> Client: content of appId/cluster/namespace
```



* 提供配置更新推送接口（基于Http long polling）
    * 服务端使用[Spring DeferredResult](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/context/request/async/DeferredResult.html)实现异步化，从而大大增加长连接数量
    * 目前使用的tomcat embed默认配置是最多10000个连接（可以调整），使用了4C8G的虚拟机实测可以支撑10000个连接，所以满足需求（一个应用实例只会发起一个长连接）。
* 接口服务对象为Apollo客户端

### 1.3.2 Admin Service

* 提供配置管理接口
* 提供配置修改、发布、检索等接口
* 接口服务对象为Portal

### 1.3.3 Meta Server

* Portal通过域名访问Meta Server获取Admin Service服务列表（IP+Port）
* Client通过域名访问Meta Server获取Config Service服务列表（IP+Port）
* Meta Server从Eureka获取Config Service和Admin Service的服务信息，相当于是一个Eureka Client
* 增设一个Meta Server的角色主要是为了封装服务发现的细节，对Portal和Client而言，永远通过一个Http接口获取Admin Service和Config Service的服务信息，而不需要关心背后实际的服务注册和发现组件
* Meta Server只是一个逻辑角色，在部署时和Config Service是在一个JVM进程中的，所以IP、端口和Config Service一致

### 1.3.4 Eureka

* 基于[Eureka](https://github.com/Netflix/eureka)和[Spring Cloud Netflix](https://cloud.spring.io/spring-cloud-netflix/)提供服务注册和发现
* Config Service和Admin Service会向Eureka注册服务，并保持心跳
* 为了简单起见，目前Eureka在部署时和Config Service是在一个JVM进程中的（通过Spring Cloud Netflix）

### 1.3.5 Portal

* 提供Web界面供用户管理配置
* 通过Meta Server获取Admin Service服务列表（IP+Port），通过IP+Port访问服务
* 在Portal侧做load balance、错误重试

### 1.3.6 Client

* Apollo提供的客户端程序，为应用提供配置获取、实时更新等功能
* 通过Meta Server获取Config Service服务列表（IP+Port），通过IP+Port访问服务
* 在Client侧做load balance、错误重试

## 1.4 E-R Diagram

### 1.4.1 主体E-R Diagram
![apollo-erd](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-erd.png)

* **App**
    * App信息
* **AppNamespace**
    * App下Namespace的元信息
* **Cluster**
    * 集群信息
* **Namespace**
    * 集群下的namespace
* **Item**
    * Namespace的配置，每个Item是一个key, value组合
* **Release**
    * Namespace发布的配置，每个发布包含发布时该Namespace的所有配置
* **Commit**
    * Namespace下的配置更改记录
* **Audit**
    * 审计信息，记录用户在何时使用何种方式操作了哪个实体。

### 1.4.2 权限相关E-R Diagram
![apollo-erd-role-permission](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-erd-role-permission.png)

* **User**
    * Apollo portal用户
* **UserRole**
    * 用户和角色的关系
* **Role**
    * 角色
* **RolePermission**
    * 角色和权限的关系
* **Permission**
    * 权限
    * 对应到具体的实体资源和操作，如修改NamespaceA的配置，发布NamespaceB的配置等。
* **Consumer**
    * 第三方应用
* **ConsumerToken**
    * 发给第三方应用的token
* **ConsumerRole**
    * 第三方应用和角色的关系
* **ConsumerAudit**
    * 第三方应用访问审计

# 二、服务端设计

## 2.1 配置发布后的实时推送设计

在配置中心中，一个重要的功能就是配置发布后实时推送到客户端。下面我们简要看一下这块是怎么设计实现的。

![release-message-notification-design](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/release-message-notification-design.png)

上图简要描述了配置发布的大致过程：

1. 用户在Portal操作配置发布
2. Portal调用Admin Service的接口操作发布
3. Admin Service发布配置后，发送ReleaseMessage给各个Config Service
4. Config Service收到ReleaseMessage后，通知对应的客户端

### 2.1.1 发送ReleaseMessage的实现方式

Admin Service在配置发布后，需要通知所有的Config Service有配置发布，从而Config Service可以通知对应的客户端来拉取最新的配置。

从概念上来看，这是一个典型的消息使用场景，Admin Service作为producer发出消息，各个Config Service作为consumer消费消息。通过一个消息组件（Message Queue）就能很好的实现Admin Service和Config Service的解耦。

在实现上，考虑到Apollo的实际使用场景，以及为了尽可能减少外部依赖，我们没有采用外部的消息中间件，而是通过数据库实现了一个简单的消息队列。

实现方式如下：

1. Admin Service在配置发布后会往ReleaseMessage表插入一条消息记录，消息内容就是配置发布的AppId+Cluster+Namespace，参见[DatabaseMessageSender](https://github.com/apolloconfig/apollo/blob/master/apollo-biz/src/main/java/com/ctrip/framework/apollo/biz/message/DatabaseMessageSender.java)
2. Config Service有一个线程会每秒扫描一次ReleaseMessage表，看看是否有新的消息记录，参见[ReleaseMessageScanner](https://github.com/apolloconfig/apollo/blob/master/apollo-biz/src/main/java/com/ctrip/framework/apollo/biz/message/ReleaseMessageScanner.java)
3. Config Service如果发现有新的消息记录，那么就会通知到所有的消息监听器（[ReleaseMessageListener](https://github.com/apolloconfig/apollo/blob/master/apollo-biz/src/main/java/com/ctrip/framework/apollo/biz/message/ReleaseMessageListener.java)），如[NotificationControllerV2](https://github.com/apolloconfig/apollo/blob/master/apollo-configservice/src/main/java/com/ctrip/framework/apollo/configservice/controller/NotificationControllerV2.java)，消息监听器的注册过程参见[ConfigServiceAutoConfiguration](https://github.com/apolloconfig/apollo/blob/master/apollo-configservice/src/main/java/com/ctrip/framework/apollo/configservice/ConfigServiceAutoConfiguration.java)
4. NotificationControllerV2得到配置发布的AppId+Cluster+Namespace后，会通知对应的客户端

示意图如下：

<img src="https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/release-message-design.png" alt="release-message-design" width="400px">

### 2.1.2 Config Service通知客户端的实现方式

上一节中简要描述了NotificationControllerV2是如何得知有配置发布的，那NotificationControllerV2在得知有配置发布后是如何通知到客户端的呢？

实现方式如下：

1. 客户端会发起一个Http请求到Config Service的`notifications/v2`接口，也就是[NotificationControllerV2](https://github.com/apolloconfig/apollo/blob/master/apollo-configservice/src/main/java/com/ctrip/framework/apollo/configservice/controller/NotificationControllerV2.java)，参见[RemoteConfigLongPollService](https://github.com/apolloconfig/apollo-java/blob/main/apollo-client/src/main/java/com/ctrip/framework/apollo/internals/RemoteConfigLongPollService.java)
2. NotificationControllerV2不会立即返回结果，而是通过[Spring DeferredResult](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/context/request/async/DeferredResult.html)把请求挂起
3. 如果在60秒内没有该客户端关心的配置发布，那么会返回Http状态码304给客户端
4. 如果有该客户端关心的配置发布，NotificationControllerV2会调用DeferredResult的[setResult](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/context/request/async/DeferredResult.html#setResult-T-)方法，传入有配置变化的namespace信息，同时该请求会立即返回。客户端从返回的结果中获取到配置变化的namespace后，会立即请求Config Service获取该namespace的最新配置。

# 三、客户端设计
![client-architecture](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/client-architecture.png)

上图简要描述了Apollo客户端的实现原理：

1. 客户端和服务端保持了一个长连接，从而能第一时间获得配置更新的推送。（通过Http Long Polling实现）
2. 客户端还会定时从Apollo配置中心服务端拉取应用的最新配置。
    * 这是一个fallback机制，为了防止推送机制失效导致配置不更新
    * 客户端定时拉取会上报本地版本，所以一般情况下，对于定时拉取的操作，服务端都会返回304 - Not Modified
    * 定时频率默认为每5分钟拉取一次，客户端也可以通过在运行时指定System Property: `apollo.refreshInterval`来覆盖，单位为分钟。
3. 客户端从Apollo配置中心服务端获取到应用的最新配置后，会保存在内存中
4. 客户端会把从服务端获取到的配置在本地文件系统缓存一份
    * 在遇到服务不可用，或网络不通的时候，依然能从本地恢复配置
5. 应用程序可以从Apollo客户端获取最新的配置、订阅配置更新通知

## 3.1 和Spring集成的原理

Apollo除了支持API方式获取配置，也支持和Spring/Spring Boot集成，集成原理简述如下。

Spring从3.1版本开始增加了`ConfigurableEnvironment`和`PropertySource`：

* ConfigurableEnvironment
    * Spring的ApplicationContext会包含一个Environment（实现ConfigurableEnvironment接口）
    * ConfigurableEnvironment自身包含了很多个PropertySource
* PropertySource
    * 属性源
    * 可以理解为很多个Key - Value的属性配置

在运行时的结构形如：
![Overview](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/environment.png)

需要注意的是，PropertySource之间是有优先级顺序的，如果有一个Key在多个property source中都存在，那么在前面的property source优先。

所以对上图的例子：

* env.getProperty(“key1”) -> value1
* **env.getProperty(“key2”) -> value2**
* env.getProperty(“key3”) -> value4

在理解了上述原理后，Apollo和Spring/Spring Boot集成的手段就呼之欲出了：在应用启动阶段，Apollo从远端获取配置，然后组装成PropertySource并插入到第一个即可，如下图所示：

![Overview](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/environment-remote-source.png)

相关代码可以参考[PropertySourcesProcessor](https://github.com/apolloconfig/apollo-java/blob/main/apollo-client/src/main/java/com/ctrip/framework/apollo/spring/config/PropertySourcesProcessor.java)

# 四、可用性考虑

<table>
<thead>
<tr>
<th width="20%">场景</th>
<th width="20%">影响</th>
<th width="30%">降级</th>
<th width="30%">原因</th>
</tr>
</thead>
<tbody>
<tr>
<td>某台Config Service下线</td>
<td>无影响</td>
<td></td>
<td>Config Service无状态，客户端重连其它Config Service</td>
</tr>
<tr>
<td>所有Config Service下线</td>
<td>客户端无法读取最新配置，Portal无影响</td>
<td>客户端重启时，可以读取本地缓存配置文件。如果是新扩容的机器，可以从其它机器上获取已缓存的配置文件，具体信息可以参考<a href='/#/zh/client/java-sdk-user-guide?id=_123-本地缓存路径'>Java客户端使用指南 - 1.2.3 本地缓存路径</a>
</td>
<td></td>
</tr>
<tr>
<td>某台Admin Service下线</td>
<td>无影响</td>
<td></td>
<td>Admin Service无状态，Portal重连其它Admin Service</td>
</tr>
<tr>
<td>所有Admin Service下线</td>
<td>客户端无影响，Portal无法更新配置</td>
<td></td>
<td></td>
</tr>
<tr>
<td>某台Portal下线</td>
<td>无影响</td>
<td></td>
<td>Portal域名通过SLB绑定多台服务器，重试后指向可用的服务器</td>
</tr>
<tr>
<td>全部Portal下线</td>
<td>客户端无影响，Portal无法更新配置</td>
<td></td>
<td></td>
</tr>
<tr>
<td>某个数据中心下线</td>
<td>无影响</td>
<td></td>
<td>多数据中心部署，数据完全同步，Meta Server/Portal域名通过SLB自动切换到其它存活的数据中心</td>
</tr>
<tr>
<td>数据库宕机</td>
<td>客户端无影响，Portal无法更新配置</td>
<td>Config Service开启<a href="/#/zh/deployment/distributed-deployment-guide?id=_323-config-servicecacheenabled-是否开启配置缓存">配置缓存</a>后，对配置的读取不受数据库宕机影响</td>
<td></td>
</tr>
</tbody>
</table>

# 五、监控相关

## 5.1 Tracing

### 5.1.1 CAT
Apollo客户端和服务端目前支持[CAT](https://github.com/dianping/cat)自动打点，所以如果自己公司内部部署了CAT的话，只要引入cat-client后Apollo就会自动启用CAT打点。

如果不使用CAT的话，也不用担心，只要不引入cat-client，Apollo是不会启用CAT打点的。

Apollo也提供了Tracer相关的SPI，可以方便地对接自己公司的监控系统。

更多信息，可以参考[v0.4.0 Release Note](https://github.com/apolloconfig/apollo/releases/tag/v0.4.0)

### 5.1.2 SkyWalking

可以参考[@hepyu](https://github.com/hepyu)贡献的[apollo-skywalking-pro样例](https://github.com/hepyu/k8s-app-config/tree/master/product/standard/apollo-skywalking-pro)。

## 5.2 Metrics

从1.5.0版本开始，Apollo服务端支持通过`/prometheus`暴露prometheus格式的metrics，如`http://${someIp:somePort}/prometheus`