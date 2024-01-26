从 1.8.0 版本开始，apollo 增加了 webhook 支持，从而可以在配置发布时触发 webhook 并告知配置发布的信息。

## 启用方式

> 配置项统一存储在ApolloPortalDB.ServerConfig表中，也可以通过`管理员工具 - 系统参数`页面进行配置，修改完一分钟实时生效。

1. webhook.supported.envs

开启 webhook 的环境列表，多个环境以英文逗号分隔，如

```
DEV,FAT,UAT,PRO
```

2. config.release.webhook.service.url

webhook 通知的 url 地址，需要接收 HTTP POST 请求。如有多个地址，以英文逗号分隔，如

```
http://www.xxx.com/webhook1,http://www.xxx.com/webhook2
```

## Webhook 接入方式

1. URL 参数

参数名 | 参数说明
--- | ---
env | 该次配置发布所在的环境

2. Request body sample

```json
{
    "appId": "",  // appId
    "clusterName": "",  // 集群
    "namespaceName": "", // namespace
    "operator": "",  // 发布人
    "releaseId": 2,  // releaseId
    "releaseTitle": "",  // releaseTitle 
    "releaseComment": "",  // releaseComment
    "releaseTime": "",  // 发布时间  eg：2020-01-01T00:00:00.000+0800
    "configuration": [ { // 发布后的全部配置，如果为灰度发布，则为灰度发布后的全部配置
        "firstEntity": "",  // 配置的key
        "secondEntity": ""  // 配置的value
    } ],
    "isReleaseAbandoned": false,
    "previousReleaseId": 1,  // 上一次正式发布的releaseId
    "operation":  // 0-正常发布 1-配置回滚 2-灰度发布 4-全量发布
    "operationContext": {  // 操作设置的属性配置
        "isEmergencyPublish": true/false,  // 是否紧急发布
        "rules": [ {  // 灰度规则
            "clientAppId": "",   // appId
            "clientIpList": [ "10.0.0.2", "10.0.0.3" ]  // IP列表
        } ],
        "branchReleaseKeys": [ "", "" ]  // 灰度发布的key
    }
}
```