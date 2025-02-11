目前Apollo团队由于人力所限，只提供了Java和.Net的客户端，对于其它语言的应用，可以通过本文的介绍来直接通过Http接口获取配置。

另外，如果有团队/个人有兴趣的话，也欢迎帮助我们来实现其它语言的客户端，具体细节可以联系@nobodyiam和@lepdou。

>注：目前已有热心用户贡献了Go、Python、NodeJS、PHP、C++的客户端，更多信息可以参考"客户端指南"

## 1.1 应用接入Apollo
首先需要在Apollo中接入你的应用，具体步骤可以参考[应用接入文档](zh/portal/apollo-user-guide?id=一、普通应用接入指南)。

## 1.2 通过带缓存的Http接口从Apollo读取配置

该接口会从缓存中获取配置，适合频率较高的配置拉取请求，如简单的每30秒轮询一次配置。

由于缓存最多会有一秒的延时，所以如果需要配合配置推送通知实现实时更新配置的话，请参考[1.3 通过不带缓存的Http接口从Apollo读取配置](#_13-%E9%80%9A%E8%BF%87%E4%B8%8D%E5%B8%A6%E7%BC%93%E5%AD%98%E7%9A%84http%E6%8E%A5%E5%8F%A3%E4%BB%8Eapollo%E8%AF%BB%E5%8F%96%E9%85%8D%E7%BD%AE)。

### 1.2.1 Http接口说明
**URL**: {config_server_url}/configfiles/json/{appId}/{clusterName}/{namespaceName}?ip={clientIp}

**Method**: GET

**参数说明**：

| 参数名            | 是否必须 | 参数值               | 备注                                                                                                                                                                                                                                                                                                        |
|-------------------|----------|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| config_server_url | 是       | Apollo配置服务的地址 |                                                                                                                                                                                                                                                                                                             |
| appId             | 是       | 应用的appId          |                                                                                                                                                                                                                                                                                                             |
| clusterName       | 是       | 集群名               | 一般情况下传入 default 即可。 如果希望配置按集群划分，可以参考[集群独立配置说明](zh/portal/apollo-user-guide?id=三、集群独立配置说明)做相关配置，然后在这里填入对应的集群名。 |
| namespaceName     | 是       | Namespace的名字      | 如果没有新建过Namespace的话，传入application即可。 如果创建了Namespace，并且需要使用该Namespace的配置，则传入对应的Namespace名字。**需要注意的是对于properties类型的namespace，只需要传入namespace的名字即可，如application。对于其它类型的namespace，需要传入namespace的名字加上后缀名，如datasources.json**                                                                                                                                                                        |
| ip                | 否       | 应用部署的机器ip     | 这个参数是可选的，用来实现灰度发布。 如果不想传这个参数，请注意URL中从?号开始的query parameters整个都不要出现。                                                                                                                                                                                             |

### 1.2.2 Http接口返回格式
该Http接口返回的是JSON格式、UTF-8编码，包含了对应namespace中所有的配置项。

若是properties类型的namespace，返回内容Sample如下：
```json
{
    "portal.elastic.document.type":"biz",
    "portal.elastic.cluster.name":"hermes-es-fws"
}
```

若不是properties类型的namespace，返回内容Sample如下（content是namespace的内容）：
```json
{
    "content": "{\"portal.elastic.document.type\":\"biz\",\"portal.elastic.cluster.name\":\"hermes-es-fws\"}"
}
```

> 通过`{config_server_url}/configfiles/raw/{appId}/{clusterName}/{namespaceName}?ip={clientIp}`可以获取到原始的配置内容，不会进行转义。

> 通过`{config_server_url}/configfiles/{appId}/{clusterName}/{namespaceName}?ip={clientIp}`可以获取到properties形式的配置

### 1.2.3 测试
由于是Http接口，所以在URL组装OK之后，直接通过浏览器、或者相关的http接口测试工具访问即可。

## 1.3 通过不带缓存的Http接口从Apollo读取配置

该接口会直接从数据库中获取配置，可以配合配置推送通知实现实时更新配置。

### 1.3.1 Http接口说明
**URL**: {config_server_url}/configs/{appId}/{clusterName}/{namespaceName}?releaseKey={releaseKey}&messages={messages}&label={label}&ip={clientIp}

**Method**: GET

**参数说明**：

| 参数名               | 是否必须 | 参数值                 | 备注                                                                                                                                                                                                                                                                                                                                                        |
|-------------------|---------|---------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| config_server_url | 是       | Apollo配置服务的地址       |                                                                                                                                                                                                                                                                                                                                                           |
| appId             | 是       | 应用的appId            |                                                                                                                                                                                                                                                                                                                                                           |
| clusterName       | 是       | 集群名                 | 一般情况下传入 default 即可。 如果希望配置按集群划分，可以参考[集群独立配置说明](zh/portal/apollo-user-guide?id=三、集群独立配置说明)做相关配置，然后在这里填入对应的集群名。                                                                                                                                                                                                                                              |
| namespaceName     | 是       | Namespace的名字        | 如果没有新建过Namespace的话，传入application即可。 如果创建了Namespace，并且需要使用该Namespace的配置，则传入对应的Namespace名字。**需要注意的是对于properties类型的namespace，只需要传入namespace的名字即可，如application。对于其它类型的namespace，需要传入namespace的名字加上后缀名，如datasources.json**                                                                                                                                     |
| releaseKey        | 否       | 上一次的releaseKey      | 将上一次返回对象中的releaseKey传入即可，用来给服务端比较版本，如果版本比下来没有变化，则服务端直接返回304以节省流量和运算                                                                                                                                                                                                                                                                                       |
| messages          | 否       | 最新的 notificationId  | 用于给服务端即时更新内存缓存，如果传递了 releaseKey，而不传递 messages参数，在服务端多实例、且开启内存缓存时、有概率会获取不到最新的配置。这个参数是json结构的字符串 {"details":{"key":notificationId}}，需要将 `appId`、`clusterName`、`namespaceName`使用 `+` 号拼接为 key,假设现在 `appId=app`、`clusterName=default`、`namespaceName=test`、`notificationId=11`，则 messages 参数为 {"details":{"app+default+test":11}}，使用 messages 参数时，需要进行 URL编码。 |
| label             | 否       | 灰度配置的标签             | 这个参数是可选的，用于灰度发布的标签规则匹配。                                                                                                                                                                                                                                                                                                                                   |
| ip                | 否       | 应用部署的机器ip           | 这个参数是可选的，用于灰度发布的 ip 规则匹配。                                                                                                                                                                                                                                                                                                                                 |
   
### 1.3.2 Http接口返回格式
该Http接口返回的是JSON格式、UTF-8编码。

如果配置没有变化（传入的releaseKey和服务端的相等），则返回HttpStatus 304，response body为空。

如果配置有变化，则会返回HttpStatus 200，response body为对应namespace的meta信息以及其中所有的配置项。

返回内容Sample如下：
```json
{
  "appId": "100004458",
  "cluster": "default",
  "namespaceName": "application",
  "configurations": {
    "portal.elastic.document.type":"biz",
    "portal.elastic.cluster.name":"hermes-es-fws"
  },
  "releaseKey": "20170430092936-dee2d58e74515ff3"
}
```

### 1.3.3 测试
由于是Http接口，所以在URL组装OK之后，直接通过浏览器、或者相关的http接口测试工具访问即可。

## 1.4 应用感知配置更新
Apollo提供了基于Http long polling的配置更新推送通知，第三方客户端可以看自己实际的需求决定是否需要使用这个功能。

如果对配置更新时间不是那么敏感的话，可以通过定时刷新来感知配置更新，刷新频率可以视应用自身情况来定，建议在30秒以上。

如果需要做到实时感知配置更新（1秒）的话，可以参考下面的文档实现配置更新推送的功能。

### 1.4.1 配置更新推送实现思路

这里建议大家可以参考Apollo的Java实现：[RemoteConfigLongPollService.java](https://github.com/apolloconfig/apollo-java/blob/main/apollo-client/src/main/java/com/ctrip/framework/apollo/internals/RemoteConfigLongPollService.java)，代码量200多行，总体上还是比较简单的。

#### 1.4.1.1 初始化
首先需要确定哪些namespace需要配置更新推送，Apollo的实现方式是程序第一次获取某个namespace的配置时就会来注册一下，我们就知道有哪些namespace需要配置更新推送了。

初始化后的结果就是得到一个notifications的Map，内容是namespaceName -> notificationId（初始值为-1）。

运行过程中如果发现有新的namespace需要配置更新推送，直接塞到notifications这个Map里面即可。

#### 1.4.1.2 请求服务
有了notifications这个Map之后，就可以请求服务了。这里先描述一下请求服务的逻辑，具体的URL参数和说明请参见后面的接口说明。

1. 请求远端服务，带上自己的应用信息以及notifications信息
2. 服务端针对传过来的每一个namespace和对应的notificationId，检查notificationId是否是最新的
3. 如果都是最新的，则保持住请求60秒，如果60秒内没有配置变化，则返回HttpStatus 304。如果60秒内有配置变化，则返回对应namespace的最新notificationId, HttpStatus 200。
4. 如果传过来的notifications信息中发现有notificationId比服务端老，则直接返回对应namespace的最新notificationId, HttpStatus 200。
5. 客户端拿到服务端返回后，判断返回的HttpStatus
6. 如果返回的HttpStatus是304，说明配置没有变化，重新执行第1步
7. 如果返回的HttpStatus是200，说明配置有变化，针对变化的namespace重新去服务端拉取配置，参见[1.3 通过不带缓存的Http接口从Apollo读取配置](#_13-%E9%80%9A%E8%BF%87%E4%B8%8D%E5%B8%A6%E7%BC%93%E5%AD%98%E7%9A%84http%E6%8E%A5%E5%8F%A3%E4%BB%8Eapollo%E8%AF%BB%E5%8F%96%E9%85%8D%E7%BD%AE)。同时更新notifications map中的notificationId。重新执行第1步。


### 1.4.2 Http接口说明
**URL**: {config_server_url}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}

**Method**: GET

**参数说明**：

| 参数名            | 是否必须 | 参数值               | 备注                                                                                                                                                                                                                                                                                                        |
|-------------------|----------|----------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| config_server_url | 是       | Apollo配置服务的地址 |                                                                                                                                                                                                                                                                                                             |
| appId             | 是       | 应用的appId          |                                                                                                                                                                                                                                                                                                             |
| clusterName       | 是       | 集群名               | 一般情况下传入 default 即可。 如果希望配置按集群划分，可以参考[集群独立配置说明](zh/portal/apollo-user-guide?id=三、集群独立配置说明)做相关配置，然后在这里填入对应的集群名。 |
| notifications     | 是       | notifications信息    | 传入本地的notifications信息，注意这里需要以array形式转为json传入，如：[{"namespaceName": "application", "notificationId": 100}, {"namespaceName": "FX.apollo", "notificationId": 200}]。**需要注意的是对于properties类型的namespace，只需要传入namespace的名字即可，如application。对于其它类型的namespace，需要传入namespace的名字加上后缀名，如datasources.json**                                                                                                                                                                        |

> 注1：由于服务端会hold住请求60秒，所以请确保客户端访问服务端的超时时间要大于60秒。

> 注2：别忘了对参数进行[url encode](https://en.wikipedia.org/wiki/Percent-encoding)

### 1.4.3 Http接口返回格式
该Http接口返回的是JSON格式、UTF-8编码，包含了有变化的namespace和最新的notificationId。

返回内容Sample如下：
```json
[
  {
    "namespaceName": "application",
    "notificationId": 101
  }
]
```

### 1.4.4 测试
由于是Http接口，所以在URL组装OK之后，直接通过浏览器、或者相关的http接口测试工具访问即可。

## 1.5 配置访问密钥

Apollo从1.6.0版本开始增加访问密钥机制，从而只有经过身份验证的客户端才能访问敏感配置。如果应用开启了访问密钥，客户端发出请求时需要增加签名，否则无法获取配置。

需要设置的Header信息：

| Header        | Value                                          | 备注                                                                                                                                                          |
|---------------|------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Authorization | Apollo ${appId}:${signature}                   | appId: 应用的appId，signature：使用访问密钥对当前时间毫秒值以及所访问的URL里的的path和query部分加签后的值，具体实现可以参考[Signature.signature](https://github.com/apolloconfig/apollo/blob/aa184a2e11d6e7e3f519d860d69f3cf30ccfcf9c/apollo-core/src/main/java/com/ctrip/framework/apollo/core/signature/Signature.java#L22)  |
| Timestamp     | 从`1970-1-1 00:00:00 UTC+0`到现在所经过的毫秒数 | 可以参考[System.currentTimeMillis](https://docs.oracle.com/javase/7/docs/api/java/lang/System.html#currentTimeMillis()) |

## 1.6 错误码说明
正常情况下，接口返回的Http状态码是200，下面列举了Apollo会返回的非200错误码说明。

### 1.6.1 400 - Bad Request
客户端传入参数的错误，如必选参数没有传入等，客户端需要根据提示信息检查对应的参数是否正确。

### 1.6.2 401 - Unauthorized
客户端未授权，如服务端配置了访问密钥，客户端未配置或配置错误。

### 1.6.3 404 - Not Found
接口要访问的资源不存在，一般是URL或URL的参数错误，或者是对应的namespace还没有发布过配置。

### 1.6.4 405 - Method Not Allowed
接口访问的Method不正确，比如应该使用GET的接口使用了POST访问等，客户端需要检查接口访问方式是否正确。

### 1.6.5 500 - Internal Server Error
其它类型的错误默认都会返回500，对这类错误如果应用无法根据提示信息找到原因的话，可以尝试查看服务端日志来排查问题。
