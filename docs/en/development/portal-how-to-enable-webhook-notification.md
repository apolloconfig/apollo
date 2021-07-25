Apollo has supported  **webhook** since 1.8.0 edition, which is triggered to send you a message when your configuration is released.

## how to enable webhook

The configuration items are stored in the table named ApolloPortalDB.ServerConfig. "admin—system parameter" page is an alternative to configuration. All the configurations will be valid in one minutes.

1. webhook.supported.envs

Open the environments lists. Multiple envs are split by comma, like

```
DEV, FAT, UAT, PRO
```

2. config.release.webhook.service.url

Config the url that receives HTTP post request sent by webhook for notifying. Multiple urls should be split by comma, like
```
http://www.xxx.com/webhook1,http://www.xxx.com/webhook2
```

## how to use

1. URL params

param name    | param annotation
--- | ---
 env   | env of the configuration to be released

2. Request body sample

```json
{
    "appId": "",  // appId
    "clusterName": "",  // cluster
    "namespaceName": "", // namespace
    "operator": "",  // modifier
    "releaseId": 2,  // release id
    "releaseTitle": "",  // release title 
    "releaseComment": "",  // release Comment
    "releaseTime": "",  // release time  eg：2020-01-01T00:00:00.000+0800
    "configuration": [ { // all configurations to be released; also applys to gray release
        "firstEntity": "",  // key of configuration
        "secondEntity": ""  // value of configuration
    } ],
    "isReleaseAbandoned": false,
    "previousReleaseId": 1,  // releaseId of latest formal release
    "operation":  // 0-normal release 1-rollabck 2-gray release 4-full release
    "operationContext": {  // property setting for operation
        "isEmergencyPublish": true/false,  // emergercy release or not
        "rules": [ {  // rules for gray release
            "clientAppId": "",   // appId
            "clientIpList": [ "10.0.0.2", "10.0.0.3" ]  // IP lists
        } ],
        "branchReleaseKeys": [ "", "" ]  // key of Gray Release
    }
}
```
