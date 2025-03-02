### I. What is the open-platform?

Apollo provides a set of Http REST interfaces to enable third-party applications to manage their own configurations. Although Apollo system itself provides a Portal to manage the configuration, but in some scenarios, the application needs to manage the configuration through the program.

### II. Third-party application access to Apollo open-platform

#### 2.1 Registering third-party applications

The person in charge of the third-party application needs to provide some basic information of the third-party application to Apollo administrator.

The basic information is as follows.

* AppId, app name, department of the third-party application
* The person in charge of the third-party app

Apollo administrator creates the third-party application at `http://{portal_address}/open/add-consumer.html`. It is better to check whether this AppId has been created before creating it. After successful creation, a token will be generated as follows.

![Open Platform Management](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-open-manage.png)

#### 2.2 View third-party apps
Apollo administrator in `http://{portal_address}/open/manage.html` The HTML page allows you to view the list of third-party applications. It also provides management operations such as [View token and empower] and [delete], as shown in the following figure:
![第三方应用列表](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-open-manage-list.png)

The modal box page of [View token and empower] is shown in the following figure:
![查看Token并赋权](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-open-manage-token.png)


#### 2.3 Authorization for registered third-party apps

Third-party applications should not be able to manipulate any Namespace configuration, so you need to bind the token to a Namespace that can be manipulated. Apollo administrators assign rights to the token in the `http://{portal_address}/open/add-consumer.html` page. After the assignment, the third-party application can manage the configuration of the authorized Namespace through the Http REST interface provided by Apollo.

#### 2.4 Third-party application calls Apollo Open API

##### 2.4.1 Calling the Http REST Interface

Third-party applications in any language can call Apollo's Open API. When calling the interface, you need to set attention to the following two points.

 * Add an Authorization field to the Http Header, with the field value of the applied token
 * Http Header Content-Type field needs to be set to application/json;charset=UTF-8

##### 2.4.2 Java application calls Apollo Open API via apollo-openapi

Starting from version 1.1.0, Apollo provides the [apollo-openapi](https://github.com/apolloconfig/apollo/tree/master/apollo-openapi) client, so third-party applications in the Java language can more easily invoke the Apollo Open API.

First introduce the `apollo-openapi` dependency.

```xml
<dependency>
    <groupId>com.ctrip.framework.apollo</groupId>
    <artifactId>apollo-openapi</artifactId>
    <version>1.7.0</version>
</dependency>
```

Construct ``ApolloOpenApiClient`` in the program.

``` java
String portalUrl = "http://localhost:8070"; // portal url
String token = "e16e5cd903fd0c97a116c873b448544b9d086de9"; // token of the application
ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder()
                                                .withPortalUrl(portalUrl)
                                                .withToken(token)
                                                .build();
```

You can then operate the Apollo Open API directly through the `ApolloOpenApiClient` interface, see the Rest interface documentation below for a description of the interface.

##### 2.4.3 .Net core application calls Apollo Open API

Net core also provides a client for the open api, see https://github.com/ctripcorp/apollo.net/pull/77 for details

##### 2.4.4 Shell Scripts calls Apollo Open API

Encapsulated bash functions, the underlying use of curl to send HTTP requests

* Bash function: [openapi.sh](https://github.com/apolloconfig/apollo/blob/master/scripts/openapi/bash/openapi.sh)

* Usage example: [openapi-usage-example.sh](https://github.com/apolloconfig/apollo/blob/master/scripts/openapi/bash/openapi-usage-example.sh)
* All the shell scripts related to openapi are in the folder https://github.com/apolloconfig/apollo/tree/master/scripts/openapi/bash

### III. Interface documentation

#### 3.1 URL path parameter description

| parameter name | parameter description                                        |
| -------------- | ------------------------------------------------------------ |
| env            | Managed configuration environment                            |
| appId          | The managed configuration AppId                              |
| clusterName    | The name of the managed configuration cluster, normally passed in as default. If it is a special cluster, just pass the name of the corresponding cluster. |
| namespaceName  | the name of the managed namespace, if it is not in properties format, you need to add the suffix name, such as `sample.yml` |

#### 3.2 API interface list

##### 3.2.1 Get App's environment, cluster information

* **URL** : `http://{portal_address}/openapi/v1/apps/{appId}/envclusters`
* **Method** : GET
* **Request Params** : None
* **Response Sample**：

``` json
[
    {
        "env": "FAT",
        "clusters":[ //cluster list
            "default",
            "FAT381"
        ]
    },
    {
        "env": "UAT",
        "clusters":[
            "default"
        ]
    },
    {
        "env": "PRO",
        "clusters":[
            "default",
            "SHAOY",
            "SHAJQ"
        ]
    }
]
```

##### 3.2.2 Get App information

* **URL** : `http://{portal_address}/openapi/v1/apps`
* **Method** : GET
* **Request Params** : 

| Parameter Name | Required | Type   | Description                                                  |
| -------------- | -------- | ------ | ------------------------------------------------------------ |
| appIds         | false    | String | List of appId, separated by commas, or return all app information if empty |

* **Response Sample**:

``` json
[
    {
        "name": "first_app",
        "appId": "100003171",
        "orgId": "development",
        "orgName": "development",
        "ownerName": "apollo",
        "ownerEmail": "test@test.com",
        "dataChangeCreatedBy": "apollo",
        "dataChangeLastModifiedBy": "apollo",
        "dataChangeCreatedTime": "2019-05-08T09:13:31.000+0800",
        "dataChangeLastModifiedTime": "2019-05-08T09:13:31.000+0800"
    },
    {
        "name": "apollo-demo",
        "appId": "100004458",
        "orgId": "development",
        "orgName": "product-development",
        "ownerName": "apollo",
        "ownerEmail": "apollo@cmcm.com",
        "dataChangeCreatedBy": "apollo",
        "dataChangeLastModifiedBy": "apollo",
        "dataChangeCreatedTime": "2018-12-23T12:35:16.000+0800",
        "dataChangeLastModifiedTime": "2019-04-08T13:58:36.000+0800"
    }
]
```

##### 3.2.3 Getting the cluster interface 

* **URL** : `http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}`
* **Method** : GET
* **Request Params** : None
* **Response Sample**:

``` json
{
    "name": "default",
    "appId": "100004458",
    "dataChangeCreatedBy": "apollo",
    "dataChangeLastModifiedBy": "apollo",
    "dataChangeCreatedTime": "2018-12-23T12:35:16.000+0800",
    "dataChangeLastModifiedTime": "2018-12-23T12:35:16.000+0800"
}
```

##### 3.2.4 Creating a Cluster Interface

Clusters can be created through this interface, and calling this interface requires granting the third-party APP administrative privileges to the target APP.

* **URL** : `http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters`
* **Method** : POST
* **Request Params** : None
* **Request Content** (Request Body, JSON format) 

| Parameter Name      | Required | Type   | Description                                                  |
| ------------------- | -------- | ------ | ------------------------------------------------------------ |
| name                | true     | String | The name of the Cluster                                      |
| appId               | true     | String | The AppId to which the Cluster belongs                       |
| dataChangeCreatedBy | true     | String | The creator of the namespace, in the format of the domain account, which is the User ID of the sso system |

* **Response Sample**: 

``` json
{
    "name": "someClusterName",
    "appId": "100004458",
    "dataChangeCreatedBy": "apollo",
    "dataChangeLastModifiedBy": "apollo",
    "dataChangeCreatedTime": "2018-12-23T12:35:16.000+0800",
    "dataChangeLastModifiedTime": "2018-12-23T12:35:16.000+0800"
}
```

##### 3.2.5 Interface to get information about all Namespaces under a cluster

* **URL** : `http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces`
* **Method**: GET
* **Request Params**: None
* **Response Sample**:

``` json
[
  {
    "appId": "100003171",
    "clusterName": "default",
    "namespaceName": "application",
    "comment": "default app namespace",
    "format": "properties", //Namespace format may take values of: properties, xml, json, yml, yaml
    "isPublic": false, //Whether the Namespace is public or not
    "items": [ // collection of all configurations under Namespace
      {
        "key": "batch",
        "value": "100",
        "dataChangeCreatedBy": "song_s",
        "dataChangeLastModifiedBy": "song_s",
        "dataChangeCreatedTime": "2016-07-21T16:03:43.000+0800",
        "dataChangeLastModifiedTime": "2016-07-21T16:03:43.000+0800"
      }
    ],
    "dataChangeCreatedBy": "song_s",
    "dataChangeLastModifiedBy": "song_s",
    "dataChangeCreatedTime": "2016-07-20T14:05:58.000+0800",
    "dataChangeLastModifiedTime": "2016-07-20T14:05:58.000+0800"
  },
  {
    "appId": "100003171",
    "clusterName": "default",
    "namespaceName": "FX.apollo",
    "comment": "apollo public namespace",
    "format": "properties",
    "isPublic": true,
    "items": [
      {
        "key": "request.timeout",
        "value": "3000",
        "comment": "",
        "dataChangeCreatedBy": "song_s",
        "dataChangeLastModifiedBy": "song_s",
        "dataChangeCreatedTime": "2016-07-20T14:08:30.000+0800",
        "dataChangeLastModifiedTime": "2016-08-01T13:56:25.000+0800"
      },
      {
        "id": 1116,
        "key": "batch",
        "value": "3000",
        "comment": "",
        "dataChangeCreatedBy": "song_s",
        "dataChangeLastModifiedBy": "song_s",
        "dataChangeCreatedTime": "2016-07-28T15:13:42.000+0800",
        "dataChangeLastModifiedTime": "2016-08-01T13:51:00.000+0800"
      }
    ],
    "dataChangeCreatedBy": "song_s",
    "dataChangeLastModifiedBy": "song_s",
    "dataChangeCreatedTime": "2016-07-20T14:08:13.000+0800",
    "dataChangeLastModifiedTime": "2016-07-20T14:08:13.000+0800"
  }
]
```

##### 3.2.6 Interface to get information about a Namespace

* **URL** : http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}
* **Method** : GET
* **Request Params** : None
* **Response Sample** : 

``` json
{
    "appId": "100003171",
    "clusterName": "default",
    "namespaceName": "application",
    "comment": "default app namespace",
    "format": "properties", //Namespace format may take values of: properties, xml, json, yml, yaml
    "isPublic": false, //Whether the Namespace is public or not
    "items": [ // collection of all configurations under Namespace
      {
        "key": "batch",
        "value": "100",
        "dataChangeCreatedBy": "song_s",
        "dataChangeLastModifiedBy": "song_s",
        "dataChangeCreatedTime": "2016-07-21T16:03:43.000+0800",
        "dataChangeLastModifiedTime": "2016-07-21T16:03:43.000+0800"
      }
    ],
    "dataChangeCreatedBy": "song_s",
    "dataChangeLastModifiedBy": "song_s",
    "dataChangeCreatedTime": "2016-07-20T14:05:58.000+0800",
    "dataChangeLastModifiedTime": "2016-07-20T14:05:58.000+0800"
  }
```

##### 3.2.7 Creating Namespace

Namespace can be created through this interface, and calling this interface requires granting the third-party app administrative privileges to the target app.

* **URL** : `http://{portal_address}/openapi/v1/apps/{appId}/appnamespaces`
* **Method** : POST
* **Request Params** : None
* **Request Content** (Request Body, JSON format) 

| Parameter Name      | Required | Type    | Description                                                  |
| ------------------- | -------- | ------- | ------------------------------------------------------------ |
| name                | true     | String  | The name of the Namespace                                    |
| appId               | true     | String  | The AppId to which the Namespace belongs                     |
| format              | true     | String  | The format of the Namespace, ** which can only be of the following types: properties, xml, json, yml, yaml** |
| isPublic            | true     | boolean | Whether the file is public                                   |
| comment             | false    | String  | Namespace description                                        |
| dataChangeCreatedBy | true     | String  | The creator of the namespace, in the format of the domain account, which is the User ID of the sso system |

* **Response Sample**: 

``` json
{
  "name": "FX.public-0420-11",
  "appId": "100003173",
  "format": "properties",
  "isPublic": true,
  "comment": "test",
  "dataChangeCreatedBy": "zhanglea",
  "dataChangeLastModifiedBy": "zhanglea",
  "dataChangeCreatedTime": "2017-04-20T18:25:49.033+0800",
  "dataChangeLastModifiedTime": "2017-04-20T18:25:49.033+0800"
}
```

* **Explanation of returned values** :. 

> If properties file, name = ${appId belongs to department}. ${name passed in}, e.g. call the interface passed in name=xy-z, format=properties, the application's department is framework (FX), then name=FX.xy-z


> if not the properties file name = ${appId belongs to the department}. ${name value passed in}. ${format}, for example, call the interface passed name=xy-z, format=json, the application's department is the framework (FX), then name=FX.xy-z.json

##### 3.2.8 Get the current editor of a Namespace interface 

Apollo has a restriction rule in the production environment (PRO): only one person can edit the configuration for each release, and the person who edited the release cannot be the editor of the release.
In other words, if a user A modifies the configuration of a namespace, it can only be modified by A before the namespace is released, and no other user can modify it. At the same time, the user A cannot publish the configuration he modified, but must find another person who has the permission to do so.
This interface is the interface used to get whether the current namespace is locked or not. In non-production environments (FAT, UAT), this interface always returns that no one is locked.

* **URL** : http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/lock
* **Method** : GET
* **Request Params** : None
* **Response Sample (unlocked)** : 

``` json
{
  "namespaceName": "application",
  "isLocked": false
}
```

* **Response Sample (isLocked)** :

``` json
{
  "namespaceName": "application",
  "isLocked": true,
  "lockedBy": "song_s" //lockedowner
}
```

##### 3.2.9 Reading the configuration interface 

* **URL** : `http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}`
* **Method** : GET
* **Request Params** : None
* **Response Sample** : 

``` json
{
    "key": "timeout",
    "value": "3000",
    "comment": "timeout",
    "dataChangeCreatedBy": "zhanglea",
    "dataChangeLastModifiedBy": "zhanglea",
    "dataChangeCreatedTime": "2016-08-11T12:06:41.818+0800",
    "dataChangeLastModifiedTime": "2016-08-11T12:06:41.818+0800"
}
```

##### 3.2.10 New configuration interface 

* **URL** : `http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items`
* **Method** : POST
* **Request Params** : None
* **Request Content** (Request Body, JSON format)

| Parameter Name      | Required | Type   | Description                                                  |
| ------------------- | -------- | ------ | ------------------------------------------------------------ |
| key                 | true     | String | The key of the configuration, the length cannot exceed 128 characters. Non-properties format, key is fixed to `content` |
| value               | true     | String | The configured value, which cannot exceed 20000 characters in length, is in non-properties format. |
| comment             | false    | String | The configured comment, the length can not exceed 256 characters |
| dataChangeCreatedBy | true     | String | The creator of the item, in the format of the domain account, which is the User ID of the sso system |

* **Request body sample** :

``` json
{
    "key": "timeout",
    "value": "3000",
    "comment": "timeout",
    "dataChangeCreatedBy": "zhanglea"
}

```

* **Response Sample**:

``` json
{
    "key": "timeout",
    "value": "3000",
    "comment": "timeout",
    "dataChangeCreatedBy": "zhanglea",
    "dataChangeLastModifiedBy": "zhanglea",
    "dataChangeCreatedTime": "2016-08-11T12:06:41.818+0800",
    "dataChangeLastModifiedTime": "2016-08-11T12:06:41.818+0800"
}
```

##### 3.2.11 Modifying the configuration interface

* **URL** : `http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}`
* **Method** : PUT
* **Request Params** :

| Parameter Name    | Required | Type    | Description                                                  |
| ----------------- | -------- | ------- | ------------------------------------------------------------ |
| createIfNotExists | false    | Boolean | Whether to create automatically when the configuration does not exist |

* **Request Body, JSON format**:

| parameter name           | required | type   | description                                                  |
| ------------------------ | -------- | ------ | ------------------------------------------------------------ |
| key                      | true     | String | The key of the configuration, must be the same as the key in the url. For non-properties format, the key is fixed to `content` |
| value                    | true     | String | The configured value can not exceed 20000 characters, non-properties format, the value is the entire content of the file |
| comment                  | false    | String | The configured comment, the length can not exceed 256 characters |
| dataChangeLastModifiedBy | true     | String | The modifier of the item, in the format of the domain account, which is the User ID of the sso system |
| dataChangeCreatedBy      | false    | String | Required when createIfNotExists is true. item's creator, in the format of the domain account, i.e. sso system's User ID |

* **Request Body Sample** :

```json
{
    "key": "timeout",
    "value": "3000",
    "comment": "timeout",
    "dataChangeLastModifiedBy": "zhanglea"
}
```

* **Response Value** : none


##### 3.2.12 Deleting configuration interfaces

* **URL** : `http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key}? operator={operator}`
* **Method** : DELETE
* **Request Params** :

| Parameter Name | Required | Type   | Description                                                  |
| -------------- | -------- | ------ | ------------------------------------------------------------ |
| key            | true     | String | The configured key. non-properties format, the key is fixed to `content` |
| operator       | true     | String | Delete the operator of the configuration, domain account     |

* **Response Value** : None

##### 3.2.13 Publish Configuration Interface

* **URL** : `http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases`
* **Method** : POST
* **Request Params** : None
* **Request Body** : None

| Parameter Name | Required | Type   | Description                                                  |
| -------------- | -------- | ------ | ------------------------------------------------------------ |
| releaseTitle   | true     | String | The title of the release, which cannot exceed 64 characters in length |
| releaseComment | false    | String | The comment of the release, which cannot exceed 256 characters in length |
| releasedBy     | true     | String | The publisher, domain account, and note: If `namespace.lock.switch` in `ApolloConfigDB.ServerConfig` is set to true (default is false), then the environment does not allow the publisher and editor to be the same person. . So if the editor is zhanglea, the publisher can no longer be zhanglea. |

* **Request Body Example** :

```json
{
    "releaseTitle": "2016-08-11",
    "releaseComment": "Modify timeout value",
    "releasedBy": "zhanglea"
}
```

* **Response Sample** :

``` json
{
    "appId": "test-0620-01",
    "clusterName": "test",
    "namespaceName": "application",
    "name": "2016-08-11",
    "configurations": {
        "timeout": "3000",
    },
    "comment": "Modify timeout value",
    "dataChangeCreatedBy": "zhanglea",
    "dataChangeLastModifiedBy": "zhanglea",
    "dataChangeCreatedTime": "2016-08-11T14:03:46.232+0800",
    "dataChangeLastModifiedTime": "2016-08-11T14:03:46.235+0800"
}
```

##### 3.2.14 Interface to get the published configuration currently in effect for a Namespace 

* **URL** : `http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases/ latest`
* **Method** : GET
* **Request Params** : None
* **Response Sample** :

``` json
{
    "appId": "test-0620-01",
    "clusterName": "test",
    "namespaceName": "application",
    "name": "2016-08-11",
    "configurations": {
        "timeout": "3000",
    },
    "comment": "Modify timeout value",
    "dataChangeCreatedBy": "zhanglea",
    "dataChangeLastModifiedBy": "zhanglea",
    "dataChangeCreatedTime": "2016-08-11T14:03:46.232+0800",
    "dataChangeLastModifiedTime": "2016-08-11T14:03:46.235+0800"
}
```

##### 3.2.15 Rollback of published configuration interface 

* **URL** : `http://{portal_address}/openapi/v1/envs/{env}/releases/{releaseId}/rollback`
* **Method** : PUT
* **Request Params** :

| Parameter Name | Required | Type   | Description                                     |
| -------------- | -------- | ------ | ----------------------------------------------- |
| operator       | true     | String | Deletes the configured operator, domain account |

* **Response Value** : None

##### 3.2.16 Get configuration items with pagination

* **URL** ：  `http://{portal_address}/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items`
* **Method** ： GET
* **Version** ： >= 2.1.0
* **Request Params** ：

| Parameter Name | Required | Type | Description                                |
|----------------|----------|------|--------------------------------------------|
| page           | false    | int  | page number, starting from 0, default is 0 |
| size           | false    | int  | records in each page, default is 50        |

* **Response Sample** ：

``` json
{
    "content": [
        {
            "key": "timeout",
            "value": "3000",
            "comment": "timeout",
            "dataChangeCreatedBy": "mghio",
            "dataChangeLastModifiedBy": "mghio",
            "dataChangeCreatedTime": "2022-07-17T21:37:41.818+0800",
            "dataChangeLastModifiedTime": "2022-07-17T21:37:41.818+0800"
        },
        {
            "key": "page.size",
            "value": "200",
            "comment": "page size",
            "dataChangeCreatedBy": "mghio",
            "dataChangeLastModifiedBy": "mghio",
            "dataChangeCreatedTime": "2022-07-17T21:37:41.818+0800",
            "dataChangeLastModifiedTime": "2022-07-17T21:37:41.818+0800"
        }
    ],
    "page": 0,
    "size": 50,
    "total": 2
}
```

##### 3.2.17 Create App And grant administrative privileges

App can be created through this interface, 

>  note: When creating and allowing third-party application, **check the box to Allow app creation, otherwise an exception will be throw, HTTP status code 401**

* **URL** ：  http://{portal_address}/openapi/v1/apps/
* **Method** ： POST
* **Request Params** ：无
* **Request Body, JSON format**:

| Parameter Name      | Required | Type     | Description                                              |
| ------------------- | -------- | -------- | -------------------------------------------------------- |
| assignAppRoleToSelf | true     | Boolean  | true：granting app's administrative privileges to self   |
| admins              | false    | String[] | granting app's administrative privileges to those users  |
| app                 | true     | Object   | APP's information，see Request Sample followed for field |

* **Request Sample** ： 

```json
{
  "assignAppRoleToSelf": true,
  "admins": [
    "user1",
    "user2"
  ],
  "app": {
    "name": "appName1234",
    "appId": "xxx-web",
    "orgId": "development",
    "orgName": "产品研发部",
    "ownerName": "user3",
    "ownerEmail": "user3@test.com"
  }
}
```

* **Response Sample** ： None

### IV. Error code description

Under normal circumstances, the Http status code returned by the interface is 200, the following lists the non-200 error code descriptions that Apollo will return.

#### 4.1 400 - Bad Request

The client needs to check whether the corresponding parameters are correct according to the prompt.

#### 4.2 401 - Unauthorized

The token passed to the interface is illegal or expired, the client needs to check if the token was passed correctly.

#### 4.3 403 - Forbidden

The interface is trying to access a resource that is not authorized, for example, it is only authorized to manage Namespace under application A, but it is trying to manage the configuration under application B.

#### 4.4 404 - Not Found

The resource to be accessed by the interface does not exist, typically the URL or the parameters of the URL are incorrect.

#### 4.5 405 - Method Not Allowed

The Method of the interface access is incorrect, for example, the interface that should use POST uses GET access, etc. The client needs to check if the interface access method is correct.

#### 4.6 500 - Internal Server Error

Other types of errors will return 500 by default. For this type of error, if the application cannot find the cause according to the prompt, you can ask Apollo's R&D team to troubleshoot the problem together.
