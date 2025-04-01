At present, Apollo team only provides Java and .Net clients due to manpower constraints. For applications in other languages, you can directly obtain the configuration through the Http interface through the introduction of this article.

Also, if any team/individual is interested, they are welcome to help us to implement the client in other languages, please contact @nobodyiam and @lepdou for details.

>Note: There are already clients for Go, Python, NodeJS, PHP, C++ contributed by enthusiastic users, for more information you can refer to "SDK Guide"

## 1.1 Application access to Apollo

First you need to access your application in Apollo, you can refer to [application access document](en/portal/apollo-user-guide) for the specific steps.

## 1.2 Reading configuration from Apollo via Http interface with cache

This interface will fetch the configuration from the cache and is suitable for more frequent configuration pull requests, such as a simple polling of the configuration every 30 seconds.

Since the cache has a delay of at most one second, if you need to work with configuration push notifications to achieve real-time configuration updates, please refer to [1.3 Reading configuration from Apollo via Http interface without cache](en/client/other-language-client-user-guide?id=_13-reading-configuration-from-apollo-via-http-interface-without-cache).

### 1.2.1 Http interface description

**URL**: `{config_server_url}/configfiles/json/{appId}/{clusterName}/{namespaceName}?ip={clientIp}`

**Method**: GET

**Parameter Description**.

| Parameter Name    | Required                     | Parameter Value                                              | Remarks                                                      |
| ----------------- | ---------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| config_server_url | Yes                          | The address of the Apollo configuration service              |                                                              |
| appId             | Yes                          | The appId of the application                                 |                                                              |
| clusterName       | Yes                          | clusterName                                                  | Normally, just pass in default. If you want to configure by cluster, you can refer to [cluster-independent configuration instructions](en/portal/apollo-user-guide?id=iii-cluster-independent-configuration-instructions) to do the relevant configuration, and then fill in the corresponding cluster name here. |
| namespaceName     | is the name of the namespace | If no new namespace has been created, just pass in application. If you have created a Namespace and need to use the configuration of that Namespace, pass the corresponding Namespace name. **For other types of namespace, you need to pass the namespace name with a suffix, such as datasources.json** | ip                                                           |
| ip                | no                           | ip of the machine where the application will be deployed     | This parameter is optional and is used to implement grayscale publishing. If you don't want to pass this parameter, please note that the URL from ? Please note that the entire query parameters starting with the ? sign should not appear in the URL. |

### 1.2.2 Http interface return format

The Http interface returns JSON format, UTF-8 encoding, which contains all the configuration items in the corresponding namespace.

If the namespace is of type `properties`, the return content sample is as follows:

```json
{
    "portal.elastic.document.type": "biz",
    "portal.elastic.cluster.name": "hermes-es-fws"
}
```

If the namespace is not of type `properties`, the return content sample is as follows:
```json
{
    "content": "{\"portal.elastic.document.type\":\"biz\",\"portal.elastic.cluster.name\":\"hermes-es-fws\"}"
}
```

> You can get the raw configuration content without escaping via `{config_server_url}/configfiles/raw/{appId}/{clusterName}/{namespaceName}?ip={clientIp}`.

> The configuration in the form of properties can be obtained via `{config_server_url}/configfiles/{appId}/{clusterName}/{namespaceName}?ip={clientIp}`

### 1.2.3 Testing

Since it is an Http interface, after the URL is assembled OK, it can be accessed directly through a browser, or a relevant http interface testing tool.

## 1.3 Reading configuration from Apollo via Http interface without cache

This interface will get the configuration directly from the database and can be used with configuration push notifications to achieve real-time configuration updates.

### 1.3.1 Http interface description

**URL**: `{config_server_url}/configs/{appId}/{clusterName}/{namespaceName}?releaseKey={releaseKey}&messages={messages}&label={label}&ip={clientIp}`

**Method**: GET

**Parameter Description**.

| Parameter Name    | Required | Parameter Value                                         | Remarks                                                                                                                                                                                                                                                                                                          |
| ----------------- |----------|---------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| config_server_url | Yes      | The address of the Apollo configuration service         |                                                                                                                                                                                                                                                                                                                  |
| appId             | Yes      | The appId of the application                            |                                                                                                                                                                                                                                                                                                                  |
| clusterName       | Yes      | clusterName                                             | Normally, just pass in default. If you want to configure by cluster, you can refer to [cluster-independent configuration instructions](en/portal/apollo-user-guide?id=iii-cluster-independent-configuration-instructions) to do the relevant configuration, and then fill in the corresponding cluster name here. |
| namespaceName     | Yes      | is the name of the namespace                            | If no new namespace has been created, just pass in application. If you have created a Namespace and need to use the configuration of that Namespace, pass the corresponding Namespace name. **If the namespace is created and needs to use the configuration of the namespace, pass the namespace name.          |                                                              
| releaseKey        | No       | Previous releaseKey                                     | The releaseKey of the last release object can be passed to the server to compare the version, if there is no change in the version, the server will return 304 to save traffic and computing.                                                                                                                    |                                                              
| messages          | No       | Latest notificationId                                   | To update the memory cache on the server in real-time, if the `releaseKey` parameter is passed without the `messages` parameter, there is a chance that the latest configuration will not be obtained in a multi-instance server environment with memory caching enabled. The `messages` parameter is a JSON string structure {"details":{"key":notificationId}}. The `appId`, `clusterName`, and `namespaceName` need to be concatenated using the + sign to form the key. Assuming `appId=app`, `clusterName=default`, `namespaceName=test`, and `notificationId=11`, the `messages` parameter would be {"details":{"app+default+test":11}}. When using the messages parameter, URL encoding is required.|
| label             | No       | Labels for grayscale configuration                      | This parameter is optional and is used for label rule matching in grayscale publishing.                                                                                                                                                                                                                          |
| ip                | No       | The ip of the machine where the application is deployed | This parameter is optional and is used for ip rule matching in grayscale publishing.                                                                                                                                                                                                                             |

### 1.3.2 Http interface return format

This Http interface returns JSON format, UTF-8 encoding.

If the configuration has not changed (the incoming releaseKey and the server-side equal), HttpStatus 304 is returned, and the response body is empty.

If the configuration has changed, HttpStatus 200 is returned, and the response body is the meta information of the corresponding namespace and all configuration items in it.

The return content Sample is as follows.

```json
{
  "appId": "100004458",
  "cluster": "default",
  "namespaceName": "application",
  "configurations": {
    "portal.elastic.document.type": "biz",
    "portal.elastic.cluster.name": "hermes-es-fws"
  },
  "releaseKey": "20170430092936-dee2d58e74515ff3"
}
```

### 1.3.3 Testing

Since it is an Http interface, after the URL is assembled OK, it can be accessed directly through a browser, or a relevant http interface testing tool.

## 1.4 Application-aware configuration updates

Apollo provides push notifications for configuration updates based on Http long polling, and third-party clients can decide whether they need to use this feature depending on their actual needs.

If you are not so sensitive to the configuration update time, you can sense the configuration update by refreshing it at regular intervals, the refresh frequency can be determined by the application itself, and it is recommended to be above 30 seconds.

If you need to be aware of configuration updates in real time (1 second), you can refer to the following document to implement the configuration update push feature.

### 1.4.1 Configuration update push idea

Here we suggest you to refer to Apollo's Java implementation: [RemoteConfigLongPollService.java](https://github.com/apolloconfig/apollo-java/blob/main/apollo-client/src/main/java/com/ctrip/framework/apollo/internals/RemoteConfigLongPollService.java), which has more than 200 lines of code and is relatively simple in general.

#### 1.4.1.1 Initialization

The first thing we need to do is to determine which namespaces need to be configured for update pushing. Apollo's implementation is to register a namespace when the program gets its configuration for the first time, so we know which namespaces need to be configured for update pushing.

The result of the initialization is a Map of notifications, the content is namespaceName -> notificationId (initial value of -1).

If you find any new namespace that needs to be configured for update pushing during the run, you can directly stuff it into the notification Map.

#### 1.4.1.2 Request Service

Once you have the notifications Map, you can request services. Here we describe the logic of requesting services, please refer to the interface description later for specific URL parameters and descriptions.

1. request the remote service, bring your application information and notifications information
2. The server checks whether the notificationId is up-to-date for each namespace and corresponding notificationId passed to it.
3. if they are the latest, hold the request for 60 seconds, if there is no configuration change within 60 seconds, then return HttpStatus 304. if there is a configuration change within 60 seconds, then return the latest notificationId of the corresponding namespace, HttpStatus 200.
If the notificationId is found to be older than the server, the latest notificationId of the corresponding namespace, HttpStatus 200, will be returned directly. 
5. After the client gets the server side return, determine the return HttpStatus .
6. If the returned HttpStatus is 304, that the configuration has not changed, re-execute step 1.
7. If the returned HttpStatus is 200, the configuration has changed, for the change of namespace to pull configuration from the server again, see [1.3 Read configuration from Apollo through the Http interface without cache](en/client/other-language-client-user-guide?id=_13-reading-configuration-from-apollo-via-http-interface-without-cache). Also update the notificationId in the notifications map. re-run step 1.


### 1.4.2 Http interface description

**URL**: `{config_server_url}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}`

**Method**: GET

**Parameter Description**.

| Parameter Name    | Required | Parameter Value                                 | Remarks                                                      |
| ----------------- | -------- | ----------------------------------------------- | ------------------------------------------------------------ |
| config_server_url | Yes      | The address of the Apollo configuration service |                                                              |
| appId             | Yes      | The appId of the application                    |                                                              |
| clusterName       | Yes      | clusterName                                     | Normally, just pass in default. If you want to configure by cluster, you can refer to [cluster-independent configuration instructions](en/portal/apollo-user-guide?id=iii-cluster-independent-configuration-instructions) to do the relevant configuration, and then fill in the corresponding cluster name here. |
| notifications     | yes      | notifications information                       | pass in the local notifications information, note that here need to be in the form of array to json pass in, such as: [{"namespaceName": "application", "notificationId": 100}, {"namespaceName": "FX.apollo", "notificationId": 200}]. **Note that for namespace of properties type, you only need to pass in the namespace name, such as application. for other types of namespace, you need to pass in the namespace name plus the suffix, such as datasources.json** |

> Note 1: Since the server side will hold the request for 60 seconds, please make sure that the timeout for the client to access the server side is greater than 60 seconds.

> Note 2: Don't forget to [url encode](https://en.wikipedia.org/wiki/Percent-encoding) for the parameters

### 1.4.3 Http interface return format

The Http interface returns a JSON format, UTF-8 encoding, containing the namespace with changes and the latest notificationId.

The return content Sample is as follows.

```json
[
  {
    "namespaceName": "application",
    "notificationId": 101
  }
]
```

### 1.4.4 Testing

Since it is an Http interface, after the URL is assembled OK, it can be accessed directly through a browser, or a relevant http interface test tool.

## 1.5 Configuring access keys

Apollo has added an access key mechanism since version 1.6.0, so that only authenticated clients can access sensitive configurations. If the application has access keys enabled, the client needs to add a signature when sending a request, otherwise the configuration cannot be accessed.

Header information to be set.

| Header        | Value                                                        | Remarks                                                      |
| ------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| Authorization | Apollo ${appId}:${signature}                                 | appId: The appId of the application, signature: Generate a signature by combining the access key with the current timestamp in milliseconds, as well as the path and query components of the visited URL. The specific implementation can be found in [Signature.signature](https://github.com/apolloconfig/apollo/blob/aa184a2e11d6e7e3f519d860d69f3cf30ccfcf9c/apollo-core/src/main/java/com/ctrip/framework/apollo/core/signature/Signature.java#L22) |
| Timestamp     | Number of milliseconds elapsed from `1970-1-1 00:00:00 UTC+0` to now | See [System.currentTimeMillis](https://docs.oracle.com/javase/7/docs/api/java/lang/System.html#currentTimeMillis()) |

## 1.6 Error Code Description

Under normal circumstances, the Http status code returned by the interface is 200, the following lists the non-200 error code descriptions that Apollo will return.

### 1.6.1 400 - Bad Request

The client needs to check whether the corresponding parameter is correct according to the prompt.

### 1.6.2 401 - Unauthorized

The client is not authorized, e.g. the server has configured the access key, but the client is not configured or configured incorrectly.

### 1.6.3 404 - Not Found

The resource to be accessed by the interface does not exist, usually the URL or the parameters of the URL are wrong, or the corresponding namespace has not yet been published for configuration.

### 1.6.4 405 - Method Not Allowed

The interface access method is incorrect, for example, the interface should use GET to use POST access, etc. The client needs to check whether the interface access method is correct.

### 1.6.5 500 - Internal Server Error

Other types of errors will return 500 by default. For this type of error, if the application cannot find the cause based on the prompt, you can try to check the server logs to troubleshoot the problem.
