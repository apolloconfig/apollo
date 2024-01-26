>注意：本文档适用对象是Apollo系统的使用者，如果你是公司内Apollo系统的开发者/维护人员，建议先参考[Apollo开发指南](zh/contribution/apollo-development-guide)。

# &nbsp;
# 〇、重要提示！

> 以下文档是旧文档，已不支持，最新.Net接入文档请参考[Apollo.net框架集成](https://github.com/apolloconfig/apollo.net#一框架集成)

# 一、准备工作

## 1.1 环境要求

* .Net: 4.0+

## 1.2 必选设置
Apollo客户端依赖于`AppId`，`Environment`等环境信息来工作，所以请确保阅读下面的说明并且做正确的配置：

### 1.2.1 AppId

AppId是应用的身份信息，是从服务端获取配置的一个重要信息。

请确保在app.config或web.config有AppID的配置，其中内容形如：

```xml
<?xml version="1.0"?>
<configuration>
    <appSettings>
        <!-- Change to the actual app id -->
        <add key="Apollo.AppId" value="100004458"/>
    </appSettings>
</configuration>
```

> 注：app.id是用来标识应用身份的唯一id，格式为string。

### 1.2.2 Environment

Apollo支持应用在不同的环境有不同的配置，所以Environment是另一个从服务器获取配置的重要信息。

Environment通过配置文件来指定，文件位置为`C:\opt\settings\server.properties`，文件内容形如：

```properties
env=DEV
```

目前，`env`支持以下几个值（大小写不敏感）：
* DEV
  * Development environment
* FAT
  * Feature Acceptance Test environment
* UAT
  * User Acceptance Test environment
* PRO
  * Production environment

### 1.2.3 服务地址
Apollo客户端针对不同的环境会从不同的服务器获取配置，所以请确保在app.config或web.config正确配置了服务器地址(Apollo.{ENV}.Meta)，其中内容形如：

```xml
<?xml version="1.0"?>
<configuration>
    <appSettings>
        <!-- Change to the actual app id -->
        <add key="Apollo.AppId" value="100004458"/>
        <!-- Should change the apollo config service url for each environment -->
        <add key="Apollo.DEV.Meta" value="http://dev-configservice:8080"/>
        <add key="Apollo.FAT.Meta" value="http://fat-configservice:8080"/>
        <add key="Apollo.UAT.Meta" value="http://uat-configservice:8080"/>
        <add key="Apollo.PRO.Meta" value="http://pro-configservice:8080"/>
    </appSettings>
</configuration>
```

### 1.2.4 本地缓存路径
Apollo客户端会把从服务端获取到的配置在本地文件系统缓存一份，用于在遇到服务不可用，或网络不通的时候，依然能从本地恢复配置，不影响应用正常运行。

本地缓存路径位于`C:\opt\data\{appId}\config-cache`，所以请确保`C:\opt\data\`目录存在，且应用有读写权限。

### 1.2.5 可选设置

**Cluster**（集群）

Apollo支持配置按照集群划分，也就是说对于一个appId和一个环境，对不同的集群可以有不同的配置。

如果需要使用这个功能，你可以通过以下方式来指定运行时的集群：

1. 通过App Config
    * 我们可以在App.config文件中设置Apollo.Cluster来指定运行时集群（注意大小写）
    * 例如，下面的截图配置指定了运行时的集群为SomeCluster
    * ![apollo-net-apollo-cluster](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-net-apollo-cluster.png)

2. 通过配置文件
    * 首先确保`C:\opt\settings\server.properties`在目标机器上存在
    * 在这个文件中，可以设置数据中心集群，如`idc=xxx`
    * 注意key为全小写

**Cluster Precedence**（集群顺序）

1. 如果`Apollo.Cluster`和`idc`同时指定：
    * 我们会首先尝试从`Apollo.Cluster`指定的集群加载配置
    * 如果没找到任何配置，会尝试从`idc`指定的集群加载配置
    * 如果还是没找到，会从默认的集群（`default`）加载

2. 如果只指定了`Apollo.Cluster`：
    * 我们会首先尝试从`Apollo.Cluster`指定的集群加载配置
    * 如果没找到，会从默认的集群（`default`）加载

3. 如果只指定了`idc`：
    * 我们会首先尝试从`idc`指定的集群加载配置
    * 如果没找到，会从默认的集群（`default`）加载

4. 如果`Apollo.Cluster`和`idc`都没有指定：
    * 我们会从默认的集群（`default`）加载配置

# 二、DLL引用
.Net客户端项目地址位于：[https://github.com/ctripcorp/apollo.net](https://github.com/ctripcorp/apollo.net)。

将项目下载到本地，切换到`Release`配置，编译Solution后会在`apollo.net\Apollo\bin\Release`中生成`Framework.Apollo.Client.dll`。

在应用中引用`Framework.Apollo.Client.dll`即可。

如果需要支持.Net Core的Apollo版本，可以参考[dotnet-core](https://github.com/ctripcorp/apollo.net/tree/dotnet-core)以及[nuget仓库](https://www.nuget.org/packages?q=Com.Ctrip.Framework.Apollo)

# 三、客户端用法

## 3.1 获取默认namespace的配置（application）
```c#
Config config = ConfigService.GetAppConfig(); //config instance is singleton for each namespace and is never null
string someKey = "someKeyFromDefaultNamespace";
string someDefaultValue = "someDefaultValueForTheKey";
string value = config.GetProperty(someKey, someDefaultValue);
```
通过上述的**config.GetProperty**可以获取到someKey对应的实时最新的配置值。

另外，配置值从内存中获取，所以不需要应用自己做缓存。

## 3.2 监听配置变化事件

监听配置变化事件只在应用真的关心配置变化，需要在配置变化时得到通知时使用，比如：数据库连接串变化后需要重建连接等。

如果只是希望每次都取到最新的配置的话，只需要按照上面的例子，调用**config.GetProperty**即可。
```c#
Config config = ConfigService.GetAppConfig(); //config instance is singleton for each namespace and is never null
config.ConfigChanged += new ConfigChangeEvent(OnChanged);
private void OnChanged(object sender, ConfigChangeEventArgs changeEvent)
{
	Console.WriteLine("Changes for namespace {0}", changeEvent.Namespace);
	foreach (string key in changeEvent.ChangedKeys)
	{
		ConfigChange change = changeEvent.GetChange(key);
		Console.WriteLine("Change - key: {0}, oldValue: {1}, newValue: {2}, changeType: {3}", change.PropertyName, change.OldValue, change.NewValue, change.ChangeType);
	}
}
```

## 3.3 获取公共Namespace的配置
```c#
string somePublicNamespace = "CAT";
Config config = ConfigService.GetConfig(somePublicNamespace); //config instance is singleton for each namespace and is never null
string someKey = "someKeyFromPublicNamespace";
string someDefaultValue = "someDefaultValueForTheKey";
string value = config.GetProperty(someKey, someDefaultValue);
```

## 3.4 Demo
apollo.net项目中有一个样例客户端的项目：`ApolloDemo`，具体信息可以参考[Apollo开发指南](zh/contribution/apollo-development-guide)中的[2.4 .Net样例客户端启动](zh/contribution/apollo-development-guide?id=_24-net样例客户端启动)部分。

>注：Apollo .Net客户端开源版目前默认会把日志直接输出到Console，大家可以自己实现Logging相关功能。
>
> 详见[https://github.com/ctripcorp/apollo.net/tree/master/Apollo/Logging/Spi](https://github.com/ctripcorp/apollo.net/tree/master/Apollo/Logging/Spi)

# 四、客户端设计
![client-architecture](https://github.com/apolloconfig/apollo/raw/master/doc/images/client-architecture.png)

上图简要描述了Apollo客户端的实现原理：

1. 客户端和服务端保持了一个长连接，从而能第一时间获得配置更新的推送。（通过Http Long Polling实现）
2. 客户端还会定时从Apollo配置中心服务端拉取应用的最新配置。
    * 这是一个fallback机制，为了防止推送机制失效导致配置不更新
    * 客户端定时拉取会上报本地版本，所以一般情况下，对于定时拉取的操作，服务端都会返回304 - Not Modified
    * 定时频率默认为每5分钟拉取一次，客户端也可以通过App.config设置`Apollo.RefreshInterval`来覆盖，单位为毫秒。
3. 客户端从Apollo配置中心服务端获取到应用的最新配置后，会保存在内存中
4. 客户端会把从服务端获取到的配置在本地文件系统缓存一份
    * 在遇到服务不可用，或网络不通的时候，依然能从本地恢复配置
5. 应用程序可以从Apollo客户端获取最新的配置、订阅配置更新通知

# 五、本地开发模式

Apollo客户端还支持本地开发模式，这个主要用于当开发环境无法连接Apollo服务器的时候，比如在邮轮、飞机上做相关功能开发。

在本地开发模式下，Apollo只会从本地文件读取配置信息，不会从Apollo服务器读取配置。

可以通过下面的步骤开启Apollo本地开发模式。

## 5.1 修改环境
修改C:\opt\settings\server.properties文件，设置env为Local：
```properties
env=Local
```

## 5.2 准备本地配置文件
在本地开发模式下，Apollo客户端会从本地读取文件，所以我们需要事先准备好配置文件。

### 5.2.1 本地配置目录
本地配置目录位于：C:\opt\data\\{_appId_}\config-cache。

appId就是应用的appId，如100004458。

请确保该目录存在，且应用程序对该目录有读权限。

**【小技巧】** 推荐的方式是先在普通模式下使用Apollo，这样Apollo会自动创建该目录并在目录下生成配置文件。

### 5.2.2 本地配置文件
本地配置文件需要按照一定的文件名格式放置于本地配置目录下，文件名格式如下：

**_{appId}+{cluster}+{namespace}.json_**

* appId就是应用自己的appId，如100004458
* cluster就是应用使用的集群，一般在本地模式下没有做过配置的话，就是default
* namespace就是应用使用配置namespace，一般是application
![client-local-cache](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-net-config-cache.png)

文件内容以json格式存储，比如如果有两个key，一个是request.timeout，另一个是batch，那么文件内容就是如下格式：
```json
{
    "request.timeout":"1000",
    "batch":"2000"
}
```

## 5.3 修改配置
在本地开发模式下，Apollo不会实时监测文件内容是否有变化，所以如果修改了配置，需要重启应用生效。
