> Note: This document is intended for users of Apollo systems. If you are a developer/maintainer of Apollo systems in your company, it is recommended to refer to [Apollo Development Guide](en/contribution/apollo-development-guide) first.

# &nbsp;

# ! Important !

> Net access documentation, please refer to [Apollo.net Framework Integration](https://github.com/apolloconfig/apollo.net#一框架集成)

# I. Preparation

## 1.1 Environment requirements

* .Net: 4.0+

## 1.2 Mandatory settings

Apollo client relies on `AppId`, `Environment` and other environment information to work, so make sure to read the following instructions and do the correct configuration.

### 1.2.1 AppId

AppId is the identity of the application and is an important piece of information to get the configuration from the server.

Make sure you have the AppID configuration in `app.config` or `web.config`, where the content looks like

```xml
<?xml version="1.0"?>
<configuration>
    <appSettings>
        <! -- Change to the actual app id -->
        <add key="Apollo.AppId" value="100004458"/>
    </appSettings>
</configuration>
```

> Note: app.id is a unique id used to identify the application identity in string format.

### 1.2.2 Environment

Apollo supports applications that have different configurations for different environments, so Environment is another important piece of information to get the configuration from the server.

Environment is specified through a configuration file in the location `C:\opt\settings\server.properties`, with the contents of the file shaped like this

```properties
env=DEV
```

Currently, `env` supports the following values (case-insensitive).

* DEV
  * Development environment
* FAT
  * Feature Acceptance Test environment
* UAT
  * User Acceptance Test environment
* PRO
  * Production environment

### 1.2.3 Service address

Apollo clients get their configuration from different servers for different environments, so make sure that the server address (Apollo.{ENV}.Meta) is correctly configured in app.config or web.config, where the content looks like

```xml
<?xml version="1.0"?>
<configuration>
    <appSettings>
        <! -- Change to the actual app id -->
        <add key="Apollo.AppId" value="100004458"/>
        <! -- Should change the apollo config service url for each environment -->
        <add key="Apollo.DEV.Meta" value="http://dev-configservice:8080"/>
        <add key="Apollo.FAT.Meta" value="http://fat-configservice:8080"/>
        <add key="Apollo.UAT.Meta" value="http://uat-configservice:8080"/>
        <Meta" value="http://pro-configservice:8080"/>
    </appSettings>
</configuration>
```

### 1.2.4 Local cache path

The Apollo client will cache a copy of the configuration obtained from the server in the local file system, so that if the service is unavailable or the network is down, the configuration can still be restored locally without affecting the normal operation of the application.

The local cache path is located in `C:\opt\data\{appId}\config-cache`, so please make sure that the `C:\opt\data\` directory exists and the application has read/write access.

### 1.2.5 Optional settings

**Cluster**

Apollo supports configuration by cluster, meaning that for an appId and an environment, there can be different configurations for different clusters.

If you need to use this feature, you can specify the runtime clusters by.

1. via App Config
   * We can specify the runtime cluster by setting Apollo.Cluster in the App.config file (note the case)
   * For example, the following screenshot configuration specifies the runtime cluster as SomeCluster
   * ![apollo-net-apollo-cluster](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-net-apollo-cluster.png)

2. via configuration file
   * First make sure that `C:\opt\settings\server.properties` exists on the target machine
   * In this file, you can set the data center cluster, such as `idc=xxx`
   * Note that the key is all lowercase

**Cluster Precedence** (cluster order)

1. If `Apollo.Cluster` and `idc` are both specified.
   * We will first try to load the configuration from the cluster specified by `Apollo.Cluster`
   * If no configuration is found, we will try to load the configuration from the cluster specified by `idc`.
   * If nothing is found, we will load from the default cluster (`default`)

2. If only `Apollo.Cluster` is specified.
   * We will first try to load the configuration from the cluster specified by `Apollo.Cluster`
   * If not found, it will be loaded from the default cluster (`default`)

3. If only `idc` is specified.
   * We will first try to load the configuration from the cluster specified by `idc`
   * If not found, it will be loaded from the default cluster (`default`)

4. If neither `Apollo.Cluster` nor `idc` is specified.
   * It will load the configuration from the default cluster (`default`)

# II. DLL references

Net client project address is located at: [https://github.com/ctripcorp/apollo.net](https://github.com/ctripcorp/apollo.net).

Download the project locally, switch to the `Release` configuration, compile the Solution and it will generate `Framework.Apollo.Client.dll` in `apollo.net\Apollo\bin\Release`.

Just reference `Framework.Apollo.Client.dll` in your application.

.Net Core, you can refer to [dotnet-core](https://github.com/ctripcorp/apollo.net/tree/dotnet-core) and [nuget repository](https://www.nuget.org/packages?q=Com.Ctrip.Framework.Apollo)

# III. Client-side usage

## 3.1 Get the configuration of the default namespace (application)

```c#
Config config = ConfigService.GetAppConfig(); //config instance is singleton for each namespace and is never null
string someKey = "someKeyFromDefaultNamespace";
string someDefaultValue = "someDefaultValueForTheKey";
string value = config.GetProperty(someKey, someDefaultValue);
```

With the above **config.getProperty** you can get the real-time latest configuration value corresponding to someKey.

In addition, the configuration values are fetched from memory, so there is no need for the application to do its own caching.

## 3.2 Listening for configuration change events

Listening for configuration change events is only used when the application really cares about configuration changes and needs to be notified when the configuration changes, e.g. when the database connection string changes and the connection needs to be rebuilt, etc.

If you just want to fetch the latest configuration every time, just call **config.GetProperty** as in the example above.

```c#
Config config = ConfigService.GetAppConfig(); //config instance is singleton for each namespace and is never null
config.ConfigChanged += new ConfigChangeEvent(OnChanged);
private void OnChanged(object sender, ConfigChangeEventArgs changeEvent)
{
	Console.WriteLine("Changes for namespace {0}", changeEvent.Namespace);
	foreach (string key in changeEvent.ChangedKeys)
	{
		ConfigChange change = changeEvent.GetChange(key);
		Console.WriteLine("Change - key: {0}, oldValue: {1}, newValue: {2}, changeType: {3}", change.PropertyName, change, NewValue, change.ChangeType);
	}
}
```

## 3.3 Get the configuration of the public Namespace

```c#
string somePublicNamespace = "CAT";
Config config = ConfigService.GetConfig(somePublicNamespace); //config instance is singleton for each namespace and is never null
string someKey = "someKeyFromPublicNamespace";
string someDefaultValue = "someDefaultValueForTheKey";
string value = config.GetProperty(someKey, someDefaultValue);
```

## 3.4 Demo

There is a sample client project in apollo.net project: `ApolloDemo`, you can refer to [2.4 .Net sample client startup](en/contribution/apollo-development-guide?id=_24-net-sample-client-startup) for more information. 

>Net client open source version will output logs directly to the Console by default, you can implement your own logging-related features.
>
>See [https://github.com/ctripcorp/apollo.net/tree/master/Apollo/Logging/Spi](https://github.com/apolloconfig/apollo.net/tree/dotnet-old/Apollo/Spi) for more details .

# IV. Client design

![client-architecture](https://github.com/apolloconfig/apollo/raw/master/doc/images/client-architecture.png)

The above diagram briefly describes the principle of Apollo client implementation.

1. The client and the server maintain a long connection, so that the first time to get the configuration updates pushed. (achieved through Http Long Polling) .
2. The client also regularly pulls the latest configuration of the application from the Apollo Configuration Center server.
   * This is a fallback mechanism to prevent the configuration from being updated due to the failure of the push mechanism.
   * The client will report the local version of the timed pull, so in general, for the timed pull operation, the server will return 304 - Not Modified.
   * Timing frequency is pulled every 5 minutes by default, client can also set `Apollo.RefreshInterval` through App.config to override it in milliseconds.
3. The client will save the latest configuration of the application in memory after getting it from the Apollo Configuration Center server
4. The client will cache a copy of the configuration obtained from the server in the local file system. In case of service unavailability or network failure, the configuration can still be restored locally.
5. The application can get the latest configuration from the Apollo client, subscribe to configuration update notifications.

# V. Local Development Mode

Apollo client also supports local development mode, which is mainly used when the development environment cannot connect to Apollo server, such as doing related function development on cruise ships or airplanes.

In local development mode, Apollo will only read configuration information from local files, not from Apollo server.

You can enable Apollo local development mode by following the steps below.

## 5.1 Modify the environment

Modify the `C:\opt\settings\server.properties` file to set the env to Local: 

```properties
env=Local
```

## 5.2 Preparing local configuration files

In local development mode, Apollo client will read files from local, so we need to prepare the configuration file beforehand.

### 5.2.1 Local configuration directory

The local configuration directory is located at: C:\opt\data\{_appId_}\config-cache.

The appId is the appId of the application, e.g. 100004458.

Please make sure that the directory exists and the application has read access to the directory.

**[Tip]** The recommended way is to use Apollo in normal mode first, so that Apollo will automatically create the directory and generate the configuration file under it.

### 5.2.2 Local configuration files

Local configuration files need to be placed in the local configuration directory according to a certain file name format, which is as follows.

**_{appId}+{cluster}+{namespace}.json_**

* AppId is the application's own appId, such as 100004458
* Cluster is the cluster used by the application, generally in local mode without configuration, it is default
* Namespace is the configuration `namespace` used by the application, usually `application`
  ![client-local-cache](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-net-config-cache.png)

The content of the file is stored in json format, for example, if there are two keys, one is request.timeout, the other is batch, then the content of the file is the following format.

```json
{
    "request.timeout": "1000",
    "batch": "2000"
}
```

## 5.3 Modifying the configuration

In local development mode, Apollo does not monitor the file content for changes in real time, so if you modify the configuration, you need to restart the application to take effect.
