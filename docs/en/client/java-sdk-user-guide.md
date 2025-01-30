> Note: This document is intended for users of Apollo systems. If you are a developer/maintainer of Apollo systems in your company, it is recommended to refer to [Apollo Development Guide](en/contribution/apollo-development-guide) first.

# &nbsp;

# I. Preparation

## 1.1 Environment requirements

* Java: 1.8+
  * To run in Java 1.7 runtime environment, please use 1.x version of apollo client, such as 1.9.1
* Guava: 22.0+
  * The Apollo client will reference Guava 32 by default. If your project references other versions, make sure the version number is greater than or equal to 22.0

>Note: For Apollo client, you can make a few code changes to downgrade to Java 1.6 if needed, see [Issue 483](https://github.com/apolloconfig/apollo/issues/483) for details

## 1.2 Mandatory settings

Apollo client depends on `AppId`, `Apollo Meta Server` and other environment information to work, so please make sure to read the following instructions and do the correct configuration.

### 1.2.1 AppId

AppId is the identity of the application and is an important piece of information to get the configuration from the server.

There are several ways to set it, from highest to lowest priority, as follows.

1. System Property

Apollo 0.7.0+ supports passing in app.id information via System Property, such as

```bash
-Dapp.id=YOUR-APP-ID
```

2. System Environment of the operating system

Apollo 1.4.0+ supports passing app.id information via the operating system's System Environment `APP_ID`, as in

```bash
APP_ID=YOUR-APP-ID
```

3. Spring Boot application.properties

Apollo 1.0.0+ supports configuration via Spring Boot's application.properties file, such as

```properties
app.id=YOUR-APP-ID
```

> This configuration does not work with multiple war packages deployed in the same tomcat

4. app.properties

Make sure that the classpath:/META-INF/app.properties file exists and that its contents look like.

>app.id=YOUR-APP-ID

The file location reference is as follows.

![app-id-location](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/app-id-location.png)

> Note: app.id is a unique id used to identify the application identity in the format string.

### 1.2.2 Apollo Meta Server

Apollo supports applications with different configurations in different environments, so you need to provide the [Apollo Meta Server](en/design/apollo-design?id=_133-meta-server) information of the current environment to Apollo clients at runtime. By default, the meta server and config service are deployed in the same JVM process, so the address of the meta server is the address of the config service.

To achieve high availability of meta server, it is recommended to do dynamic load balancing by SLB (Software Load Balancer). meta server address can also be filled with IP, such as `http://1.1.1.1:8080,http://2.2.2.2:8080`, but it is still recommended for production environment Use the domain name (go SLB), because machine expansion, shrinkage, etc. may lead to changes in the IP list.

The following ways to configure apollo meta server information are supported since version 1.0.0, in descending order of priority

1. Via the Java System Property `apollo.meta`
   * Can be specified via the Java System Property `apollo.meta`
   * Can be specified in the Java program startup script with `-Dapollo.meta=http://config-service-url`
     * If you are running a jar file, you need to note that the format is `java -Dapollo.meta=http://config-service-url -jar xxx.jar`
   * You can also specify it programmatically, e.g. `System.setProperty("apollo.meta", "http://config-service-url");`
2. Through the Spring Boot configuration file
   * You can specify `apollo.meta=http://config-service-url` in `application.properties` or `bootstrap.properties` of Spring Boot

> This configuration does not work with multiple war packages deployed in the same tomcat

3. Through the operating system's System Environment `APOLLO_META`
   * Can be specified by System Environment `APOLLO_META` of the operating system
   * Note that the key is all-caps and separated by `_`.
4. Via `server.properties` configuration file
   * You can specify `apollo.meta=http://config-service-url` in the `server.properties` configuration file
   * For Mac/Linux, the default file location is `/sopt/settings/server.properties`.
   * For Windows, the default file location is `C:\opt\settings\server.properties`
5. Via the `app.properties` configuration file
   * You can specify `apollo.meta=http://config-service-url` in `classpath:/META-INF/app.properties`
6. Via Java system property `${env}_meta`
   * If the current [env](#_1241-environment) is `dev`, then the user can configure `-Ddev_meta=http://config-service-url`
   * Using this configuration method, then the Environment must be configured correctly, see [1.2.4.1 Environment](en/client/java-sdk-user-guide?id=_1241-environment) for details
7. Via the OS System Environment `${ENV}_META` (supported since version 1.2.0)
   * If the current [env](#_1241-environment) is `dev`, then the user can configure the OS System Environment `DEV_META=http://config-service-url`
   * Note that the key is all-caps
   * Using this configuration method, then the Environment must be configured correctly, see [1.2.4.1 Environment](en/client/java-sdk-user-guide?id=_1241-environment) for details
8. Via the `apollo-env.properties` file
   * The user can also create an `apollo-env.properties` and put it under the classpath of the application or under the config directory of the spring boot application
   * If you use this configuration, then you must configure the Environment correctly, see [1.2.4.1 Environment](en/client/java-sdk-user-guide?id=_1241-environment)
   * The contents of the file look like this.

```properties
dev.meta=http://1.1.1.1:8080
fat.meta=http://apollo.fat.xxx.com
uat.meta=http://apollo.uat.xxx.com
pro.meta=http://apollo.xxx.com
```

> If the Meta Server address cannot be obtained by any of the above means, Apollo will eventually fallback to `http://apollo.meta` as the Meta Server address

#### 1.2.2.1 Customizing Apollo Meta Server Address Location Logic

In version 1.0.0, Apollo provides the [MetaServerProvider SPI](https://github.com/apolloconfig/apollo/blob/master/apollo-core/src/main/java/com/ctrip/framework/apollo/core/spi/MetaServerProvider.java), which allows users to inject their own MetaServerProvider to customize the Meta Server address location logic.

Since we use the typical [Java Service Loader pattern](https://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html), it is still relatively simple to implement.

One thing to note is that apollo will iterate through all MetaServerProviders in order at runtime until a MetaServerProvider provides a non-empty Meta Server address, so users need to pay extra attention to the Order of the custom MetaServerProvider. The rule is that smaller Order has higher priority, so MetaServerProvider with Order=0 will be ranked ahead of MetaServerProvider with Order=1.

**If your company has many applications that need to access Apollo, it is recommended to package a jar package and then provide a custom Apollo Meta Server positioning logic so that the applications that access Apollo can be used with zero configuration. For example, write your own `xx-company-apollo-client`, the jar package depends on `apollo-client`, define a custom MetaServerProvider implementation in the jar package by spi, and then the application directly depends on `xx-company-apollo-client`.**

The implementation of MetaServerProvider can be found in [LegacyMetaServerProvider](https://github.com/apolloconfig/apollo/blob/master/apollo-core/src/main/java/com/ctrip/framework/apollo/core/internals/LegacyMetaServerProvider.java) and [DefaultMetaServerProvider](https://github.com/apolloconfig/apollo-java/blob/main/apollo-client/src/main/java/com/ctrip/framework/apollo/internals/DefaultMetaServerProvider.java).

#### 1.2.2.2 Skip Apollo Meta Server service discovery

> For apollo-client version 0.11.0 and above

In general, it is recommended to use Apollo's Meta Server mechanism to implement service discovery for Config Service, so that high availability of Config Service can be achieved. However, apollo-client also supports skipping Meta Server service discovery, which is mainly used in the following scenarios: 1.

1. Config Service is deployed on the public cloud, and the address registered to Meta Server is an intranet address, so the local development environment cannot connect directly
   * If the Config Service is exposed through the public SLB, remember to set the IP whitelist to avoid data leakage.
2. Config Service is deployed in docker environment, and the address registered to Meta Server is docker intranet address, so local development environment cannot connect to it directly.
3. Config Service is deployed in kubernetes, and you want to use kubernetes' own service discovery capability (Service).

For the above scenarios, you can skip Meta Server service discovery by directly specifying the Config Service address, in descending order of priority, as follows

1. Via Java System Property `apollo.config-service` (1.9.0+) or `apollo.configService` (before 1.9.0)
   * Can be specified via Java's System Property `apollo.config-service` (1.9.0+) or `apollo.configService` (before 1.9.0)
   * In the Java program startup script, you can specify `-Dapollo.config-service=http://config-service-url:port`
     * If you are running a jar file, you need to note that the format is `java -Dapollo.configService=http://config-service-url:port -jar xxx.jar`
   * It can also be specified programmatically, such as `System.setProperty("apollo.config-service", "http://config-service-url:port");`
2. Through the operating system's System Environment `APOLLO_CONFIG_SERVICE` (1.9.0+) or `APOLLO_CONFIGSERVICE` (before 1.9.0)
   * Can be specified by System Environment `APOLLO_CONFIG_SERVICE`(1.9.0+) or `APOLLO_CONFIGSERVICE`(before 1.9.0) of the operating system
   * Note that the key is all-caps and separated by `_`.
3. Via `server.properties` configuration file
   * You can specify `apollo.config-service=http://config-service-url:port`(1.9.0+) or `apollo.configService=http://config-service-url:port` in the `server.properties` configuration file (before 1.9.0)
    * For Mac/Linux, the default file location is `/opt/settings/server.properties`
   * For Windows, the default file location is `C:\opt\settings\server.properties`

### 1.2.3 Local cache path

The Apollo client will cache a copy of the configuration obtained from the server in the local file system, so that if the service is unavailable or the network is down, the configuration can still be restored locally without affecting the normal operation of the application.

The local cache path is located in the following path by default, so please make sure `/opt/data` or `C:\opt\data\` directory exists and the application has read/write permission.

* **Mac/Linux**: /opt/data/{_appId_}/config-cache
* **Windows**: C:\opt\data\\\{_appId_}\config-cache

The local configuration file will be placed under the local cache path in the following filename format.

**_{appId}+{cluster}+{namespace}.properties_**

* AppId is the application's own appId, e.g. 100004458
* Cluster is the cluster used by the application, generally in local mode without configuration, it is default
* Namespace is the configuration `namespace` used by the application, usually `application`
  ![client-local-cache](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/client-local-cache.png)

The content of the file is stored in properties format, for example, if there are two keys, one is request.timeout, and the other is batch, then the content of the file is in the following format.

```properties
request.timeout=2000
batch=2000
```
> Note: If deployed in a Kubernetes environment, you can also enable the configMap cache to further improve availability

#### 1.2.3.1 Customizing the cache path

The following custom cache paths are supported since version 1.0.0, in descending order of priority

1. Via the Java System Property `apollo.cache-dir` (1.9.0+) or `apollo.cacheDir` (before 1.9.0)
   * Can be specified by Java's System Property `apollo.cache-dir`(1.9.0+) or `apollo.cacheDir`(before 1.9.0)
   * In the Java program startup script, you can specify `-Dapollo.cache-dir=/opt/data/some-cache-dir`(1.9.0+) or `apollo.cacheDir=/opt/data/some-cache-dir`(before 1.9.0)
     * If you are running a jar file, note that the format is `java -Dapollo.cache-dir=/opt/data/some-cache-dir -jar xxx.jar` (1.9.0+) or `java -Dapollo.cacheDir=/opt/data/some-cache-dir -jar xxx.jar`(before 1.9.0)
   * Can also be specified programmatically, e.g. `System.setProperty("apollo.cache-dir", "/opt/data/some-cache-dir");`(1.9.0+) or `System.setProperty("apollo.cacheDir", "/opt/data/some-cache-dir");`(before 1.9.0)
2. Via the Spring Boot configuration file
   * You can specify `apollo.cache-dir=/opt/data/some-cache-dir` (1.9.0+) or `apollo. cacheDir=/opt/data/some-cache-dir`(before 1.9.0)
3. Via OS System Environment `APOLLO_CACHE_DIR` (1.9.0+) or `APOLLO_CACHEDIR` (before 1.9.0)
   * Can be specified by the OS System Environment `APOLLO_CACHE_DIR`(1.9.0+) or `APOLLO_CACHEDIR`(before 1.9.0)
   * Note that the key is all-caps and separated by `_`.
4. Via `server.properties` configuration file
   * You can specify `apollo.cache-dir=/opt/data/some-cache-dir`(1.9.0+) or `apollo.cacheDir=/opt/data/some-cache-dir`(before 1.9. 0) in the `server.properties` configuration file. 0 or earlier)
   * For Mac/Linux, the default file location is `/opt/settings/server.properties`
   * For Windows, the default file location is `C:\opt\settings\server.properties`

> Note: The local cache path can also be used for the disaster recovery directory. If the application needs to be expanded when all the config services are down, then the configuration can also be copied from the cache path on the existing machine to the same cache path on the new machine first

### 1.2.4 Optional settings

#### 1.2.4.1 Environment

The Environment can be configured in any of the following 3 ways.

1. Via Java System Property
   * Environment can be specified through Java's System Property `env`.
   * In the Java program startup script, you can specify `-Denv=YOUR-ENVIRONMENT`
     * If you are running a jar file, note that the format is `java -Denv=YOUR-ENVIRONMENT -jar xxx.jar`
   * Note that the key is all lowercase

2. Through the operating system's System Environment
   * You can also specify the System Environment `ENV` of the operating system
   * Note that the key is all uppercase

3. Through the configuration file
   * The last recommended way is to specify `env=YOUR-ENVIRONMENT` through the configuration file
   * For Mac/Linux, the default file location is `/opt/settings/server.properties`.
   * For Windows, the default file location is `C:\opt\settings\server.properties`.

The contents of the file look like :

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

For more environment definitions, you can refer to [Env.java](https://github.com/apolloconfig/apollo/blob/master/apollo-core/src/main/java/com/ctrip/framework/apollo/core/enums/Env.java)

#### 1.2.4.2 Cluster

Apollo supports configuration by cluster, which means that for an appId and an environment, there can be different configurations for different clusters.

The following ways of clustering are supported since version 1.0.0, in descending order of priority.

1. Via Java System Property `apollo.cluster`
   * can be specified via Java's System Property `apollo.cluster`
   * Can be specified in the Java program startup script with `-Dapollo.cluster=SomeCluster`
     * If you are running a jar file, you need to note that the format is `java -Dapollo.cluster=SomeCluster -jar xxx.jar`
   * You can also specify it programmatically, e.g. `System.setProperty("apollo.cluster", "SomeCluster");`
2. Through the Spring Boot configuration file
   * You can specify `apollo.cluster=SomeCluster` in `application.properties` or `bootstrap.properties` of Spring Boot
3. Via Java System Property
   * You can specify the environment via Java's System Property `idc`.
   * In the Java program startup script, you can specify `-Didc=xxx`
     * If you are running a jar file, you should note that the format is `java -Didc=xxx -jar xxx.jar`
   * Note that the key is all lowercase
4. Through the operating system's System Environment
   * Can also be specified by the operating system's System Environment `IDC
   * Note that the key is all uppercase
5. Through the `server.properties` configuration file
   * You can specify `idc=xxx` in the `server.properties` configuration file
   * For Mac/Linux, the default file location is `/opt/settings/server.properties`.
   * For Windows, the default file location is `C:\opt\settings\server.properties`

**Cluster Precedence** (cluster order)

1. If `apollo.cluster` and `idc` are both specified.
   * It will first try to load the configuration from the cluster specified by `apollo.cluster`
   * If nothing is found, we try to load the configuration from the cluster specified by `idc`.
   * If nothing is found, we will load from the default cluster (`default`)

2. If only `apollo.cluster` is specified.
   * We will first try to load the configuration from the cluster specified by `apollo.cluster`
   * If not found, it will be loaded from the default cluster (`default`)

3. If only `idc` is specified.
   * We will first try to load the configuration from the cluster specified by `idc`
   * If not found, it will be loaded from the default cluster (`default`)

4. If neither `apollo.cluster` nor `idc` is specified.
   * It will load the configuration from the default cluster (`default`)

#### 1.2.4.3 Set whether the in-memory configuration items remain in the same order as they are on the page

> For versions 1.6.0 and above

By default, the in-memory configuration of the apollo client is stored in Properties (Hashtable underneath) and is not intentionally kept in the same order as seen on the page, which has no effect on most scenarios. However, some scenarios will strongly rely on the order of configuration items (such as the routing rules of spring cloud zuul), for this case, you can turn on the OrderedProperties feature to make the in-memory configuration order consistent with what you see on the page.

The configuration methods, in descending order of priority, are

1. Via the Java System Property `apollo.properties.order.enable
   * Can be specified via Java's System Property `apollo.property.order.enable`
   * You can specify `-Dapollo.property.order.enable=true` in the Java program startup script
     * If you are running a jar file, you need to note that the format is `java -Dapollo.property.order.enable=true -jar xxx.jar`
   * You can also specify it programmatically, such as `System.setProperty("apollo.property.order.enable", "true");`
2. Via the Spring Boot configuration file
   * You can specify `apollo.properties.order.enable=true` in Spring Boot's `application.properties` or `bootstrap.properties`
3. Via the `app.properties` configuration file
   * You can specify `apollo.properties.order.enable=true` in `classpath:/META-INF/app.properties`

#### 1.2.4.4 Configuring access keys

> For versions 1.6.0 and above

Apollo has added an access key mechanism since version 1.6.0 so that only authenticated clients can access sensitive configurations. If the application has access keys enabled, the client needs to configure the keys, otherwise the configuration cannot be accessed.

The configuration methods are as follows, in descending order of priority

1. Via Java System Property `apollo.access-key.secret` (1.9.0+) or `apollo.accessskey.secret` (before 1.9.0)
   * Can be specified via Java's System Property `apollo.access-key.secret` (1.9.0+) or `apollo.accessskey.secret` (before 1.9.0)
   * In the Java application startup script, you can specify `-Dapollo.access-key.secret=1cf998c4e2ad4704b45a98a509d15719` (1.9.0+) or `-Dapollo.accesskey.secret=1cf998c4e2ad4704b45a98a509d15719`(before 1.9.0)
     * If running a jar file, note that the format is `java -Dapollo.access-key.secret=1cf998c4e2ad4704b45a98a509d15719 -jar xxx.jar` (1.9.0+) or `java -Dapollo.accesskey.secret=1cf998c4e2ad4704b45a98a509d15719 -jar xxx.jar`(before 1.9.0)
   * Can also be specified programmatically, such as `System.setProperty("apollo.access-key.secret", "1cf998c4e2ad4704b45a98a509d15719");`(1.9.0+) or `System.setProperty("apollo.accesskey.secret", "1cf998c4e2ad4704b45a98a509d15719");` (before 1.9.0)
2. Via the Spring Boot configuration file
   * You can specify `apollo.access-key.secret=1cf998c4e2ad4704b45a98a509d15719` in `application.properties` or `bootstrap.properties` of Spring Boot (1.9.0 +) or `apollo.accesskey.secret=1cf998c4e2ad4704b45a98a509d15719` (before 1.9.0)
3. Through the operating system's System Environment
   * Can also be specified through the OS System Environment `APOLLO_ACCESS_KEY_SECRET`(1.9.0+) or `APOLLO_ACCESSKEY_SECRET`(before 1.9.0)
   * Note that the key is all-caps
4. Via `app.properties` configuration file
   * You can specify `apollo.access-key.secret=1cf998c4e2ad4704b45a98a509d15719`(1.9.0+) or `apollo.accessskey.secret=1cf998c4e2ad4704b45a98a509d15719`(before 1.9.0)

#### 1.2.4.5 Custom server.properties path

> for version 1.8.0 and above

The following ways to customize the server.properties path are supported since version 1.8.0, in descending order of priority

1. Via the Java System Property `apollo.path.server.properties`
   * Can be specified via the Java System Property `apollo.path.server.properties`
   * You can specify `-Dapollo.path.server.properties=/some-dir/some-file.properties` in the Java program startup script
     * If you are running a jar file, you need to note that the format is `java -Dapollo.path.server.properties=/some-dir/some-file.properties -jar xxx.jar`
   * Can also be specified programmatically, e.g. `System.setProperty("apollo.path.server.properties", "/some-dir/some-file.properties");`
2. Via the operating system's System Environment `APOLLO_PATH_SERVER_PROPERTIES`
   * can be specified by System Environment `APOLLO_PATH_SERVER_PROPERTIES` of the operating system
   * Note that the key is all-caps and separated by `_`.

#### 1.2.4.6 Enables `propertyNames` caching, which can significantly improve startup speed in a large number of configuration scenarios

> For version 1.9.0 and above

In scenarios where `@ConfigurationProperties` is used and there are a large number of configuration items, the Spring container can be slow to start. This configuration can be turned on to significantly improve startup speed, and the cache will be automatically cleared when the configuration changes, default is `false`. See: [issue 3800](https://github.com/apolloconfig/apollo/issues/3800)

The configuration methods, in descending order of priority, are

1. via Java System Property `apollo.property.names.cache.enable`
   * can be specified via the Java System Property `apollo.property.names.cache.enable`
   * You can specify `-Dapollo.property.names.cache.enable=true` in the Java program startup script
     * If you are running a jar file, note that the format is `java -Dapollo.property.names.cache.enable=true -jar xxx.jar`
   * You can also specify it programmatically, such as `System.setProperty("apollo.property.names.cache.enable", "true");`
2. via system environment variables
   * Configure the environment variable `APOLLO_PROPERTY_NAMES_CACHE_ENABLE=true` before starting the program to specify
   * Note that the key is all-caps and separated by `_`.
3. via the Spring Boot configuration file
   * You can specify `apollo.properties.names.cache.enable=true` in Spring Boot's `application.properties` or `bootstrap.properties`.
4. via the `app.properties` configuration file
   * You can specify `apollo.property.names.cache.enable=true` in `classpath:/META-INF/app.properties`

#### 1.2.4.7 Apollo-Label

ApolloLabel is the label information of the application, an important piece of information to get the configuration from the server side for the grayscale rules.

There are several ways to set it, from highest to lowest priority, as follows.

1. System Property

Apollo 2.0.0+ supports passing in apollo.label information via System Property, such as

```bash
-Dapollo.label=YOUR-APOLLO-LABEL
```

2. System Environment of the operating system

Apollo 2.0.0+ supports passing apollo.label information through the operating system's System Environment ``APP_LABEL``, as in

```bash
APOLLO_LABEL=YOUR-APOLLO-LABEL
```

3. Spring boot `application.properties`

Apollo 2.0.0+ supports configuration via Spring Boot's application.properties file, such as

```properties
apollo.label=YOUR-APOLLO-LABEL
```

> This configuration is not suitable for multiple war packages deployed in the same tomcat scenario

4. `app.properties`

Make sure that the `classpath:/META-INF/app.properties` file exists and that its contents are shaped like.

>apollo.label=YOUR-APOLLO-LABEL

The file location reference is as follows.

![app-id-location](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/app-id-location.png)

> Note: apollo.label is a label used to identify the application identity in the format string.

#### 1.2.4.8 Enable Apollo Override System Properties

> For version 2.1.0 and above

Flag to indicate that Apollo's remote properties should override system properties. Default true.

The configuration methods, in descending order of priority, are

1. Via the Java System Property `apollo.override-system-properties`
    * Can be specified via Java's System Property `apollo.override-system-properties`
    * You can specify `-Dapollo.override-system-properties=true` in the Java program startup script
        * If you are running a jar file, you need to note that the format is `java -Dapollo.override-system-properties=true -jar xxx.jar`
    * You can also specify it programmatically, such as `System.setProperty("apollo.override-system-properties", "true");`
2. Via the Spring Boot configuration file
    * You can specify `apollo.override-system-properties=true` in Spring Boot's `application.properties` or `bootstrap.properties`
3. Via the `app.properties` configuration file
    * You can specify `apollo.override-system-properties=true` in `classpath:/META-INF/app.properties`

#### 1.2.4.9 Enable Client Monitoring

> For version 2.4.0 and above

After enabling the following configurations, you can use `ConfigService.getConfigMonitor()` to retrieve client monitoring information and enable automatic reporting.

```properties
# 1. Whether to enable the Monitor mechanism, i.e., whether ConfigMonitor is enabled. Default is false.
apollo.client.monitor.enabled = true

# 2. Whether to expose Monitor data by JMX. When enabled, you can view related information through tools like J-console. Default is false.
apollo.client.monitor.jmx.enabled = true

# 3. The maximum number of exception logs that Monitor can store. The default is 25, following the FIFO principle.
apollo.client.monitor.exception-queue-size = 30

# 4. Specify the Exporter type for exporting metric data to the corresponding monitoring system. 
# If you introduce apollo-plugin-client-prometheus, set this to "prometheus" to enable it.
# This depends on the SPI implementation of MetricsExporter.
apollo.client.monitor.external.type = prometheus

# 5. Specify the frequency at which the Exporter exports status information from Monitor as metric data. 
# The default is once every 10 seconds.
apollo.client.monitor.external.export-period = 20
```

#### 1.2.4.10 ConfigMap cache

> For version 2.4.0 and above

Starting from version 2.4.0, the availability of the client in the Kubernetes environment has been enhanced. After enabling the ConfigMap cache, the client will cache a copy of the configuration information fetched from the server in the ConfigMap. In the case of service unavailability, network issues, and loss of local cache files, the configuration can still be restored from the ConfigMap. Here are the relevant configurations:

`apollo.cache.kubernetes.enable`：Whether to enable the ConfigMap cache mechanism, the default is false.

`apollo.cache.kubernetes.namespace`：The namespace of the ConfigMap to be used (the namespace in Kubernetes), the default value is "default".

The configuration information will be placed in the specified ConfigMap according to the following correspondence:

namespace: Use the specified value, if not specified, the default is "default"

configMapName: apollo-configcache-{appId}

key:{cluster}___{namespace}

value: The content is the JSON format string of the corresponding configuration information.

> appId is the application's own appId, such as 100004458.
> 
> cluster is the cluster used by the application, which is usually default if not configured locally 
> 
> namespace Indicates the configuration namespace used by the application. If '_' appears in the namespace, it will be escaped to '__' when the key is concatenated.
> Since this feature is extended, so the client-java dependency is set to optional. You need to import the matching version
> Since read and write operations on the ConfigMap are required, the pod where the client is located must have the corresponding permissions. The specific configuration method can be referred to below.

How to authorize a Pod's Service Account to have read and write permissions for ConfigMap:

1. Create a Service Account: If there is no Service Account, you need to create one.
   ```yaml
   apiVersion: v1
   kind: ServiceAccount
   metadata:
     name: my-service-account
     namespace: default
   ```
2. Create a Role or ClusterRole: Define a Role or ClusterRole to grant read and write permissions for a specific ConfigMap. If the ConfigMap is used across multiple Namespaces, a ClusterRole should be used.
   ```yaml
   apiVersion: rbac.authorization.k8s.io/v1
   kind: Role
   metadata:
     namespace: default
     name: configmap-role
   rules:
   - apiGroups: [""]
     resources: ["configmaps"]
     verbs: ["get", "list", "watch", "create", "update", "delete"]
   ```
3. Bind the Service Account to the Role or ClusterRole: Use RoleBinding or ClusterRoleBinding to bind the Service Account to the Role or ClusterRole created above.
   ```yaml
   apiVersion: rbac.authorization.k8s.io/v1
   kind: RoleBinding
   metadata:
     name: configmap-reader-binding
     namespace: default
   subjects:
   - kind: ServiceAccount
     name: my-service-account
     namespace: default
   roleRef:
     kind: Role
     name: configmap-role
     apiGroup: rbac.authorization.k8s.io
   ```
4. Specify the Service Account in the Pod configuration: Ensure that the Pod's configuration uses the Service Account created above.
   ```yaml
   apiVersion: v1
   kind: Pod
   metadata:
     name: my-pod
     namespace: default
   spec:
     serviceAccountName: my-service-account
     containers:
       - name: my-container
         image: my-image
   ```
5. Apply the configuration: Use the kubectl command-line tool to apply these configurations.
   ```yaml
   kubectl apply -f service-account.yaml
   kubectl apply -f role.yaml
   kubectl apply -f role-binding.yaml
   kubectl apply -f pod.yaml
   ```
   
   These steps give the Service Account in the Pod read and write permissions for the specified ConfigMap.
   If the ConfigMap is cross-namespace, use ClusterRole and ClusterRoleBinding instead of Role and RoleBinding, and ensure that these configurations are applied in all Namespaces that need to access the ConfigMap.

# II. Maven Dependency

Apollo's client jar package has been uploaded to the central repository, the application only needs to be introduced in the following way when it is actually used.

```xml
    <dependency>
        <groupId>com.ctrip.framework.apollo</groupId>
        <artifactId>apollo-client</artifactId>
        <version>1.7.0</version>
    </dependency>
```

# III. Client Usage

Apollo supports API approach and Spring integration approach, how to choose which one to use?

* The API approach is flexible, fully functional, configuration values are updated in real time (hot release), and supports all Java environments.

* Spring approach is easy to access and has N cool ways to play with Spring, such as

  * Placeholder way.
    * Direct use in the code, such as: `@Value("${someKeyFromApollo:someDefaultValue}")`
    * Replace the placeholder in the configuration file, e.g.: `spring.datasource.url: ${someKeyFromApollo:someDefaultValue}`
    * Directly hosting spring's configuration, such as directly configuring `spring.datasource.url=jdbc:mysql://localhost:3306/somedb?characterEncoding=utf8` in apollo
  * Spring boot's [@ConfigurationProperties](http://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/context/properties/ConfigurationProperties.html) method
  * Versions from v0.10.0 onwards support automatic update of placeholder at runtime, see [PR #972](https://github.com/apolloconfig/apollo/pull/972) for details. (Versions prior to v0.10.0 do not re-inject after configuration changes and require a restart to update. If you need real-time updates of configuration values, you can refer to the subsequent description in [3.2.2 Use of Spring Placeholder](en/client/java-sdk-user-guide?id=_322-use-of-spring-placeholder)

* The Spring approach can also be used in combination with the API approach, such as injecting Apollo's Config object, you can get the configuration as usual through the API approach: 

  ```java
  @ApolloConfig
  private Config config; //inject config for namespace application
  ```

* For more interesting practical usage scenarios and sample code, please refer to [apollo-use-cases](https://github.com/ctripcorp/apollo-use-cases)

## 3.1 API Usage

The API approach is the easiest and most efficient way to use Apollo configuration without relying on the Spring Framework to use it.

### 3.1.1 Get the configuration of the default namespace (application)

```java
Config config = ConfigService.getAppConfig(); //config instance is singleton for each namespace and is never null
String someKey = "someKeyFromDefaultNamespace";
String someDefaultValue = "someDefaultValueForTheKey";
String value = config.getProperty(someKey, someDefaultValue);
```

With the above **config.getProperty** you can get the real-time latest configuration value corresponding to someKey.

In addition, the configuration values are fetched from memory, so there is no need for the application to do its own caching.

### 3.1.2 Listening for configuration change events

Listening for configuration change events is only used when the application really cares about configuration changes and needs to be notified when the configuration changes, e.g. when the database connection string changes and the connection needs to be rebuilt, etc.

If you just want to fetch the latest configuration every time, just follow the example above and call **config.getProperty**.

```java
Config config = ConfigService.getAppConfig(); //config instance is singleton for each namespace and is never null
config.addChangeListener(new ConfigChangeListener() {
    @Override
    public void onChange(ConfigChangeEvent changeEvent) {
        System.out.println("Changes for namespace " + changeEvent.getNamespace());
        for (String key : changeEvent.changedKeys()) {
            ConfigChange change = changeEvent.getChange(key);
            System.out.println(String.format("Found change - key: %s, oldValue: %s, newValue: %s, changeType: %s", change.getPropertyName(), change. getOldValue(), change.getNewValue(), change.getChangeType()));
        }
    }
});
```

### 3.1.3 Get the configuration of the public Namespace

``` java
String somePublicNamespace = "CAT";
Config config = ConfigService.getConfig(somePublicNamespace); //config instance is singleton for each namespace and is never null
String someKey = "someKeyFromPublicNamespace";
String someDefaultValue = "someDefaultValueForTheKey";
String value = config.getProperty(someKey, someDefaultValue);
```

### 3.1.4 Get the configuration of a non-properties format namespace

#### 3.1.4.1 Namespace in yaml/yml format

Apollo-Client version 1.3.0 starts to make better support for `yaml/yml`, which is used in the same way as properties format.

```java
Config config = ConfigService.getConfig("application.yml");
String someKey = "someKeyFromYmlNamespace";
String someDefaultValue = "someDefaultValueForTheKey";
String value = config.getProperty(someKey, someDefaultValue);
```

#### 3.1.4.2 Namespace in non-yaml/yml format

To get it, you need to use the `ConfigService.getConfigFile` interface and specify the Format, such as `ConfigFileFormat.XML`.

```java
String someNamespace = "test";
ConfigFile configFile = ConfigService.getConfigFile("test", ConfigFileFormat.XML);
String content = configFile.getContent();
```

### 3.1.5 Read the configuration corresponding to multiple appid and their namespaces.(added in version 2.4.0)
Specify the corresponding appid and namespace to retrieve the config, and then obtain the properties.
```java
String someAppId = "Animal";
String somePublicNamespace = "CAT";
Config config = ConfigService.getConfig(someAppId, somePublicNamespace);
String someKey = "someKeyFromPublicNamespace";
String someDefaultValue = "someDefaultValueForTheKey";
String value = config.getProperty(someKey, someDefaultValue);
```

### 3.1.6 Retrieve Client Monitoring Metrics
> For version 2.4.0 and above

Apollo Client significantly enhanced observability since version 2.4.0, providing the ConfigMonitor API as well as metric export options via JMX and Prometheus. For configuration details, see [1.2.4.9 Enable Client Monitoring](#_1249-enable-client-monitoring).

#### 3.1.6.1 Retrieve Monitoring Data via ConfigMonitor

```java
ConfigMonitor configMonitor = ConfigService.getConfigMonitor();
// Error-related monitoring API  
ApolloClientExceptionMonitorApi exceptionMonitorApi = configMonitor.getExceptionMonitorApi();
List<Exception> apolloConfigExceptionList = exceptionMonitorApi.getApolloConfigExceptionList();
// Namespace-related monitoring API  
ApolloClientNamespaceMonitorApi namespaceMonitorApi = configMonitor.getNamespaceMonitorApi();
List<String> namespace404 = namespaceMonitorApi.getNotFoundNamespaces();
// Bootstrap parameter-related monitoring API  
ApolloClientBootstrapArgsMonitorApi runningParamsMonitorApi = configMonitor.getBootstrapArgsMonitorApi();
String bootstrapNamespaces = runningParamsMonitorApi.getBootstrapNamespaces();
// Thread pool-related monitoring API  
ApolloClientThreadPoolMonitorApi threadPoolMonitorApi = configMonitor.getThreadPoolMonitorApi();
ApolloThreadPoolInfo remoteConfigRepositoryThreadPoolInfo = threadPoolMonitorApi.getRemoteConfigRepositoryThreadPoolInfo();
```


#### 3.1.6.2 Expose Status Information via JMX

Enable the relevant configuration:

```properties
apollo.client.monitor.enabled = true
apollo.client.monitor.jmx.enabled = true
```

After starting the application, use J-console or similar tools to view the metrics. Below is an example using J-console:

![showing Apollo client monitoring metrics in JMX](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-client-monitor-jmx.jpg)
#### 3.1.6.3 Client Export Metrics to External Monitoring Systems

Users can customize the integration with monitoring systems such as Prometheus as needed. The client provides an SPI, see [7.3 Exporting Metrics to Custom Monitoring Systems](#_73-exporting-metrics-to-custom-monitoring-systems). for details.

*Related Metrics Data Tables*

**Namespace Metrics**

Metric corresponding API: ApolloClientNamespaceMonitorApi

| Metric Name                                          | Tag        | Corresponding Monitor-API                           |
| ---------------------------------------------------- | ---------- | --------------------------------------------------- |
| apollo_client_namespace_usage_total                  | namespace | namespaceMetrics.getUsageCount()                    |
| apollo_client_namespace_item_num                     | namespace | namespaceMetrics.getFirstLoadTimeSpendInMs()        |
| apollo_client_namespace_not_found                    |            | namespaceMonitorApi.getNotFoundNamespaces()         |
| apollo_client_namespace_timeout                      |            | namespaceMonitorApi.getTimeoutNamespaces()          |
| apollo_client_namespace_first_load_time_spend_in_ms  | namespace | namespaceMetrics.getLatestUpdateTime                |

**Thread Pool Metrics**

Metric corresponding API: ApolloClientThreadPoolMonitorApi

| Metric Name                                          | Tag             | Corresponding Monitor-API                           |
| ---------------------------------------------------- | --------------- | --------------------------------------------------- |
| apollo_client_thread_pool_pool_size                  | thread_pool_name | threadPoolInfo.getPoolSize()                        |
| apollo_client_thread_pool_maximum_pool_size          | thread_pool_name | threadPoolInfo.getMaximumPoolSize()                 |
| apollo_client_thread_pool_largest_pool_size          | thread_pool_name | threadPoolInfo.getLargestPoolSize()                 |
| apollo_client_thread_pool_completed_task_count       | thread_pool_name | threadPoolInfo.getCompletedTaskCount()              |
| apollo_client_thread_pool_queue_remaining_capacity   | thread_pool_name | threadPoolInfo.getQueueRemainingCapacity()          |
| apollo_client_thread_pool_total_task_count           | thread_pool_name | threadPoolInfo.getTotalTaskCount()                  |
| apollo_client_thread_pool_active_task_count          | thread_pool_name | threadPoolInfo.getActiveTaskCount()                 |
| apollo_client_thread_pool_core_pool_size             | thread_pool_name | threadPoolInfo.getCorePoolSize()                    |
| apollo_client_thread_pool_queue_size                 | thread_pool_name | threadPoolInfo.getQueueSize()                       |

**Exception Metrics**

Metric corresponding API: ApolloClientExceptionMonitorApi

| Metric Name                         | Tag                                                   |
| ----------------------------------- | ----------------------------------------------------- |
| apollo_client_exception_num_total   | exceptionMonitorApi.getExceptionCountFromStartup()    |


## 3.2 Spring integration approach

### 3.2.1 Configuration

Apollo also supports integration with Spring (Spring 3.1.1+), and only requires some simple configuration.

Apollo currently supports both the more traditional `XML-based` configuration and the currently more popular `Java-based (recommended)` configuration.

In case of Spring Boot environments, it is recommended to refer to [3.2.1.3 Spring Boot integration methods (recommended)](en/client/java-sdk-user-guide?id=_3213-spring-boot-integration-methods-recommended) for configuration.

Note that if you have previously used `org.springframework.beans.factory.config.PropertyPlaceholderConfigurer`, please replace it with `org.springframework.context.support.PropertySourcesPlaceholderConfigurer`. It is not recommended to use PropertyPlaceholderConfigurer after Spring 3.1, use PropertySourcesPlaceholderConfigurer instead.

If you have used `<context:property-placeholder>` before, please note that the `spring-context.xsd` version introduced in the xml needs to be 3.1 or higher (usually it will be upgraded automatically as long as no version is specified), and it is recommended to introduce it without the version number, e.g.: `http://www.springframework.org/schema/context/spring-context.xsd`

> Note 1: namespace in yaml/yml format supports integration with Spring since version 1.3.0, when injecting you need to fill in the full name with a suffix, such as application.yml

> Note 2: Non-properties, non-yaml/yml formatted namespace (e.g. xml, json, etc.) do not support integration with Spring at this time.

#### 3.2.1.1 XML-based configuration

>Note: You need to add apollo related xml namespace to the configuration file header, otherwise it will report xml syntax errors.

1. Inject the default namespace configuration into Spring

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:apollo="http://www.ctrip.com/schema/apollo"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.ctrip.com/schema/apollo http://www.ctrip.com/schema/apollo.xsd">
    <! -- This is the simplest form of configuration, which is generally sufficient for applications, and is used to instruct Apollo to inject the configuration of the application namespace into the Spring environment -->
    <apollo:config/>
    <bean class="com.ctrip.framework.apollo.spring.TestXmlBean">
        <property name="timeout" value="${timeout:100}"/>
        <property name="batch" value="${batch:200}"/>
    </bean>
</beans>
```

2. Inject multiple namespace configuration into Spring

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:apollo="http://www.ctrip.com/schema/apollo"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.ctrip.com/schema/apollo http://www.ctrip.com/schema/apollo.xsd">
    <! -- This is the simplest form of configuration, which is generally sufficient for applications, and is used to instruct Apollo to inject the configuration of the application namespace into the Spring environment -->
    <apollo:config/>
    <! -- This is a slightly more complex form of configuration that instructs Apollo to inject the configuration of FX.apollo and application.yml namespace into the Spring environment -->
    <apollo:config namespaces="FX.apollo,application.yml"/>
    <bean class="com.ctrip.framework.apollo.spring.TestXmlBean">
        <property name="timeout" value="${timeout:100}"/>
        <property name="batch" value="${batch:200}"/>
    </bean>
</beans>
```

3. Inject multiple namespaces and specify the order

If multiple property sources have the same key, then the configuration with the highest order will take effect.

If <apollo:config> does not specify an order, then the default is the lowest priority.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:apollo="http://www.ctrip.com/schema/apollo"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.ctrip.com/schema/apollo http://www.ctrip.com/schema/apollo.xsd">
    <apollo:config order="2"/>
    <! -- This is the most complex form of configuration, instructing Apollo to inject the configuration of FX.apollo and application.yml namespace into the Spring environment, and in order before application -->
    <apollo:config namespaces="FX.apollo,application.yml" order="1"/>
    <bean class="com.ctrip.framework.apollo.spring.TestXmlBean">
        <property name="timeout" value="${timeout:100}"/>
        <property name="batch" value="${batch:200}"/>
    </bean>
</beans>
```

#### 3.2.1.2 Java-based configuration (recommended)

Java-based configuration is currently the more popular approach as opposed to XML-based configuration.

Note that `@EnableApolloConfig` should be used together with `@Configuration`, otherwise it will not take effect.

1. Inject the default namespace configuration into Spring

```java
// This is the simplest form of configuration and is generally used by applications to instruct Apollo to inject the configuration of application namespace into the Spring environment
@Configuration
@EnableApolloConfig
public class AppConfig {
  @Bean
  public TestJavaConfigBean javaConfigBean() {
    return new TestJavaConfigBean();
  }
}
```

2. Inject multiple namespace configuration into Spring

```java
@Configuration
@EnableApolloConfig
public class SomeAppConfig {
  @Bean
  public TestJavaConfigBean javaConfigBean() {
    return new TestJavaConfigBean();
  }
}
   
// This is a slightly more complex form of configuration, instructing Apollo to inject the configuration of FX.apollo and application.yml namespace into the Spring environment
@Configuration
@EnableApolloConfig({"FX.apollo", "application.yml"})
public class AnotherAppConfig {}
```

3. Inject multiple namespaces and specify the order

```java
// This is the most complex form of configuration, instructing Apollo to inject the configuration of FX.apollo and application.yml namespace into the Spring environment, and in the order before application
@Configuration
@EnableApolloConfig(order = 2)
public class SomeAppConfig {
  @Bean
  public TestJavaConfigBean javaConfigBean() {
    return new TestJavaConfigBean();
  }
}
@Configuration
@EnableApolloConfig(value = {"FX.apollo", "application.yml"}, order = 1)
public class AnotherAppConfig {}
```

4.Support for multiple appid (added in version 2.4.0)
```java
// Added support for loading multiple appid their corresponding namespaces. 
// Note that when using multiple appid, if there are keys that are the same, 
// only the key from the prioritized loaded appid will be retrieved
@Configuration
@EnableApolloConfig(value = {"FX.apollo", "application.yml"},
        multipleConfigs = {@MultipleConfig(appid = "ORDER_SERVICE", namespaces = {"ORDER.apollo"})}
)
public class SomeAppConfig {}
```

#### 3.2.1.3 Spring Boot integration methods (recommended)

Spring Boot supports the above two integration methods in addition to configuration via application.properties/bootstrap.properties, which enables configuration to be injected at an earlier stage, such as scenarios that use `@ConditionalOnProperty` or have some spring-boot-starter needs to read the configuration to do something in the startup phase (e.g. [dubbo-spring-boot-project](https://github.com/apache/incubator-dubbo-spring-boot-project)). So for Spring Boot environment it is recommended to access Apollo (requires version 0.10.0 and above) by the following way.

It is very simple to use, you just need to configure it in application.properties/bootstrap.properties according to the following sample.

1. Example configuration with default ``application`` namespace injected

```properties
     # will inject 'application' namespace in bootstrap phase
     apollo.bootstrap.enabled = true
```

2. Example configuration for injecting non-default ``application`` namespace or multiple namespaces

```properties
     apollo.bootstrap.enabled = true
     # will inject 'application', 'FX.apollo' and 'application.yml' namespaces in bootstrap phase
     apollo.bootstrap.namespaces = application,FX.apollo,application.yml
```

3. Load Apollo configuration before initializing the logging system (1.2.0+)

Starting with version 1.2.0, if you wish to put logging-related configuration (such as `logging.level.root=info` or parameters in `logback-spring.xml`) in Apollo management as well, then you can additionally configure `apollo.bootstrap.eagerLoad. enabled=true` to put Apollo loading order before logging system loading, for more information you can refer to [PR 1614](https://github.com/apolloconfig/apollo/pull/1614). The reference configuration example is as follows.

```properties
     # will inject 'application' namespace in bootstrap phase
     apollo.bootstrap.enabled = true
     # put apollo initialization before logging system initialization
     apollo.bootstrap.eagerLoad.enabled = true
```

#### 3.2.1.4 Spring Boot Config Data Loader (Spring Boot 2.4+, Apollo Client 1.9.0+ recommended)

For Spring Boot 2.4+ versions there is also support for loading configurations via Config Data Loader mode

##### 3.2.1.4.1 Adding maven dependencies

`apollo-client-config-data` already depends on `apollo-client`, so you only need to add this one dependency, not the apollo-client dependency

```xml
<dependencies>
    <dependency>
        <groupId>com.ctrip.framework.apollo</groupId>
        <artifactId>apollo-client-config-data</artifactId>
        <version>1.9.0</version>
    </dependency
</dependencies>
```

##### 3.2.1.4.2 Configure `app.id`, `env`, `apollo.meta` (or `apollo.config-service`), `apollo.cluster` as described above

##### 3.2.1.4.3 Configure `application.properties` or `application.yml`

Use the default namespace `application`

```properties
# old way
# apollo.bootstrap.enabled=true
# do not configure apollo.bootstrap.namespaces
# new way
spring.config.import=apollo://

```

or

```properties
# old way
# apollo.bootstrap.enabled=true
# apollo.bootstrap.namespaces=application
# new way
spring.config.import=apollo://application

```

Use custom namespace

```properties
# old way
# apollo.bootstrap.enabled=true
# apollo.bootstrap.namespaces=your-namespace
# new way
spring.config.import=apollo://your-namespace

```

Using multiple namespaces  
Note: `spring.config.import` loads the configuration from back to front, while `apollo.bootstrap.namespaces` loads it from back to front, just the opposite. To ensure consistency with the original logic, reverse the order of namespaces

```properties
# old way
# apollo.bootstrap.enabled=true
# apollo.bootstrap.namespaces=namespace1,namespace2,namespace3
# new way
spring.config.import=apollo://namespace3, apollo://namespace2, apollo://namespace1

```

#### 3.2.1.5 Spring Boot Config Data Loader (Spring Boot 2.4+, Apollo Client 1.9.0+ recommended) + webClient extension

For Spring Boot version 2.4 and above, it also supports loading configuration through Config Data Loader mode
Apollo's Config Data Loader also provides a webClient-based http client to replace the original http client, so as to easily extend the http client

##### 3.2.1.5.1 Add maven dependency

WebClient can be based on multiple implementations (reactor netty httpclient, jetty reactive httpclient, apache httpclient5), the dependencies to be added are as follows

###### Reactor netty httpclient

```xml
<dependencies>
   <dependency>
      <groupId>com.ctrip.framework.apollo</groupId>
      <artifactId>apollo-client-config-data</artifactId>
      <version>1.9.0</version>
   </dependency>
   <!-- webclient -->
   <dependency>
      <groupId>org.springframework</groupId>
      <artifactId>spring-webflux</artifactId>
   </dependency>
   <!-- reactor netty httpclient -->
   <dependency>
      <groupId>io.projectreactor.netty</groupId>
      <artifactId>reactor-netty-http</artifactId>
   </dependency>
</dependencies>
```

###### Jetty reactive httpclient

```xml
<dependencies>
    <dependency>
        <groupId>com.ctrip.framework.apollo</groupId>
        <artifactId>apollo-client-config-data</artifactId>
        <version>1.9.0</version>
    </dependency>
    <!-- webclient -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webflux</artifactId>
    </dependency>
    <!-- jetty reactive httpclient -->
    <dependency>
       <groupId>org.eclipse.jetty</groupId>
       <artifactId>jetty-reactive-httpclient</artifactId>
    </dependency>
</dependencies>
```

###### Apache httpclient5

Spring boot does not specify the version of apache httpclient5, so you need to manually specify the version here

```xml
<dependencies>
    <dependency>
        <groupId>com.ctrip.framework.apollo</groupId>
        <artifactId>apollo-client-config-data</artifactId>
        <version>1.9.0</version>
    </dependency>
    <!-- webclient -->
    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-webflux</artifactId>
    </dependency>
    <!-- apache httpclient5 -->
    <dependency>
       <groupId>org.apache.httpcomponents.client5</groupId>
       <artifactId>httpclient5</artifactId>
       <version>5.1</version>
    </dependency>
    <dependency>
       <groupId>org.apache.httpcomponents.core5</groupId>
       <artifactId>httpcore5-reactive</artifactId>
       <version>5.1</version>
    </dependency>
</dependencies>
```

##### 3.2.1.5.2 Configure `app.id`, `env`, `apollo.meta` (or `apollo.config-service`), `apollo.cluster` as described above

##### 3.2.1.5.3 Configuring `application.properties` or `application.yml`

The default namespace is used here as an example. Please refer to 3.2.1.4.3 for the configuration of namespace.

```properties
spring.config.import=apollo://application
apollo.client.extension.enabled=true
```

##### 3.2.1.5.4 Provide implementation of spi

Provides a spi implementation of interface `com.ctrip.framework.apollo.config.data.extension.webclient.customizer.spi.ApolloClientWebClientCustomizerFactory`
After configuring `apollo.client.extension.enabled=true`, Apollo's Config Data Loader will try to load the spi's implementation class to customize the webClient

### 3.2.2 Use of Spring Placeholder

Spring applications usually use Placeholder to inject configuration in the form of ${someKey:someDefaultValue}, such as ${timeout:100}. The key before the colon is the key, and the default value after the colon.

It is recommended to give the default value as much as possible in actual use, so as to avoid runtime errors due to the undefined key.

Versions starting from v0.10.0 support automatic update of placeholders at runtime, see [PR #972](https://github.com/apolloconfig/apollo/pull/972).

If you need to turn off the automatic update function of placeholder at runtime, you can turn it off in the following two ways:

1. By setting the System Property `apollo.autoUpdateInjectedSpringProperties`, such as passing in `-Dapollo.autoUpdateInjectedSpringProperties=false` at startup

2. By setting the `apollo.autoUpdateInjectedSpringProperties` property in META-INF/app.properties, such as

```properties
app.id=SampleApp
apollo.autoUpdateInjectedSpringProperties=false
```

#### 3.2.2.1 XML usage

Suppose I have a TestXmlBean with two configuration items that need to be injected:

```java
public class TestXmlBean {
  private int timeout;
  private int batch;
 
  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  public void setBatch(int batch) {
    this.batch = batch;
  }
 
  public int getTimeout() {
    return timeout;
  }
 
  public int getBatch() {
    return batch;
  }
}
```

Then, I will use the following way to define in XML (assuming that the default application namespace of the application has timeout and batch configuration items):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xmlns:apollo="http://www.ctrip.com/schema/apollo"
       xsi:schemaLocation="http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
       http://www.ctrip.com/schema/apollo http://www.ctrip.com/schema/apollo.xsd">
    <apollo:config/>
    <bean class="com.ctrip.framework.apollo.spring.TestXmlBean">
        <property name="timeout" value="${timeout:100}"/>
        <property name="batch" value="${batch:200}"/>
    </bean>
</beans>
```

#### 3.2.2.2 How to use Java Config

Suppose I have a TestJavaConfigBean, which can also be injected using @Value through Java Config:

```java
public class TestJavaConfigBean {
  @Value("${timeout:100}")
  private int timeout;
  private int batch;
 
  @Value("${batch:200}")
  public void setBatch(int batch) {
    this.batch = batch;
  }
 
  public int getTimeout() {
    return timeout;
  }
 
  public int getBatch() {
    return batch;
  }
}
```

In the Configuration class, use it in the following way (assuming the default application namespace of the application has `timeout` and `batch` configuration items):

```java
@Configuration
@EnableApolloConfig
public class AppConfig {
  @Bean
  public TestJavaConfigBean javaConfigBean() {
    return new TestJavaConfigBean();
  }
}
```

#### 3.2.2.3 How to use ConfigurationProperties

Spring Boot provides [@ConfigurationProperties](http://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/context/properties/ConfigurationProperties.html) to inject configuration into bean objects .

Apollo also supports this method. The following example will inject `redis.cache.expireSeconds` and `redis.cache.commandTimeout` into the `expireSeconds` and `commandTimeout` fields of SampleRedisConfig respectively.

```java
@ConfigurationProperties(prefix = "redis.cache")
public class SampleRedisConfig {
  private int expireSeconds;
  private int commandTimeout;

  public void setExpireSeconds(int expireSeconds) {
    this.expireSeconds = expireSeconds;
  }

  public void setCommandTimeout(int commandTimeout) {
    this.commandTimeout = commandTimeout;
  }
}
```

In the Configuration class, use it in the following way (assuming the default application namespace of the application has `redis.cache.expireSeconds` and `redis.cache.commandTimeout` configuration items):

```java
@Configuration
@EnableApolloConfig
public class AppConfig {
  @Bean
  public SampleRedisConfig sampleRedisConfig() {
    return new SampleRedisConfig();
  }
}
```

It should be noted that if `@ConfigurationProperties` needs to automatically update the injected value when the Apollo configuration changes, you need to use [EnvironmentChangeEvent](https://cloud.spring.io/spring-cloud-static/spring-cloud.html#_environment_changes) or [RefreshScope](https://cloud.spring.io/spring-cloud-static/spring-cloud.html#_refresh_scope). For related code implementation, please refer to [ZuulPropertiesRefresher.java](https://github.com/ctripcorp/apollo-use-cases/blob/master/spring-cloud-zuul/src/main/java/com/ctrip/framework/apollo/use/cases/spring/cloud/zuul/ZuulPropertiesRefresher.java#L48) and [SampleRedisConfig.java](https://github.com/apolloconfig/apollo-demo-java/blob/main/spring-boot-demo/src/main/java/com/apolloconfig/apollo/demo/springboot/config/SampleRedisConfig.java) and [SpringBootApolloRefreshConfig.java](https://github.com/apolloconfig/apollo-demo-java/blob/main/spring-boot-demo/src/main/java/com/apolloconfig/apollo/demo/springboot/refresh/SpringBootApolloRefreshConfig.java)

### 3.2.3 Spring Annotation Support

Apollo also adds several new Annotations to simplify usage in the Spring environment.

1. @ApolloConfig
   * Used to automatically inject the Config object
2. @ApolloConfigChangeListener
   * Used to automatically register ConfigChangeListener
3. @ApolloJsonValue
   * Used to automatically inject the configured json string as an object

Example of usage is as follows:

```java
public class TestApolloAnnotationBean {
  @ApolloConfig
  private Config config; //inject config for namespace application
  @ApolloConfig("application")
  private Config anotherConfig; //inject config for namespace application
  @ApolloConfig("FX.apollo")
  private Config yetAnotherConfig; //inject config for namespace FX.apollo
  @ApolloConfig("application.yml")
  private Config ymlConfig; //inject config for namespace application.yml
 
  /**
   * ApolloJsonValue annotated on fields example, the default value is specified as empty list - []
   * <br />
   * jsonBeanProperty=[{"someString":"hello","someInt":100},{"someString":"world!","someInt":200}]
   */
  @ApolloJsonValue("${jsonBeanProperty:[]}")
  private List<JsonBean> anotherJsonBeans;

  @Value("${batch:100}")
  private int batch;
  
  //config change listener for namespace application
  @ApolloConfigChangeListener
  private void someOnChange(ConfigChangeEvent changeEvent) {
    //update injected value of batch if it is changed in Apollo
    if (changeEvent.isChanged("batch")) {
      batch = config.getIntProperty("batch", 100);
    }
  }
 
  //config change listener for namespace application
  @ApolloConfigChangeListener("application")
  private void anotherOnChange(ConfigChangeEvent changeEvent) {
    //do something
  }
 
  //config change listener for namespaces application, FX.apollo and application.yml
  @ApolloConfigChangeListener({"application", "FX.apollo", "application.yml"})
  private void yetAnotherOnChange(ConfigChangeEvent changeEvent) {
    //do something
  }

  //example of getting config from Apollo directly
  //this will always return the latest value of timeout
  public int getTimeout() {
    return config.getIntProperty("timeout", 200);
  }

  //example of getting config from injected value
  //the program needs to update the injected value when batch is changed in Apollo using @ApolloConfigChangeListener shown above
  public int getBatch() {
    return this.batch;
  }

  private static class JsonBean{
    private String someString;
    private int someInt;
  }
}
```

Use it in the Configuration class as follows:

```java
@Configuration
@EnableApolloConfig
public class AppConfig {
  @Bean
  public TestApolloAnnotationBean testApolloAnnotationBean() {
    return new TestApolloAnnotationBean();
  }
}
```

### 3.2.4 Existing configuration migration

In many cases, the application may already have a lot of configuration, such as Spring Boot application, there will be configurations such as bootstrap.properties/yml, application.properties/yml, etc.

After the application is connected to Apollo, these configurations can be easily migrated to Apollo. The specific steps are as follows:

1. Create a new project for the application in Apollo
2. Configure `META-INF/app.properties` in the application
3. It is recommended to convert the original configuration to the properties format, and then paste it into the application namespace of the application through the text editing mode provided by Apollo, and publish the configuration
   * If the original format is yml, you can use [YamlPropertiesFactoryBean.getObject](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/beans/factory/config/YamlPropertiesFactoryBean.html#getObject--) into properties format
4. If the original is yml and you want to continue to use yml to edit the configuration, you can create a private application.yml namespace, paste all the original configuration into it, and publish the configuration
   * Requires `apollo-client` to be version `1.3.0` and above
5. Delete the original configuration files such as bootstrap.properties/yml, application.properties/yml from the project
   * If you need to keep the local configuration file, it should be noted that some configurations such as `server.port` must ensure that the configuration item has been deleted from the local file

Such as:

```properties
spring.application.name=reservation-service
server.port = 8080

logging.level = ERROR

eureka.client.service-url.defaultZone = http://127.0.0.1:8761/eureka/
eureka.client.healthcheck.enabled=true
eureka.client.register-with-eureka = true
eureka.client.fetch-registry = true
eureka.client.eureka-service-url-poll-interval-seconds = 60

eureka.instance.prefer-ip-address = true
```

![text-mode-spring-boot-config-sample](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/text-mode-spring-boot-config-sample.png )

## 3.3 Demo

There is a sample client project in the project: `apollo-demo`, for details, please refer to [2.3 Java Sample Client Start](en/contribution/apollo-development-guide?id=_23-java-sample-client-startup) in [Apollo Development Guide](en/contribution/apollo-development-guide) section.

For more use case demos, please refer to [Apollo usage scenarios and sample code](https://github.com/ctripcorp/apollo-use-cases).

# IV. Client design

![client-architecture](https://github.com/apolloconfig/apollo/raw/master/doc/images/client-architecture.png)

The above diagram briefly describes the principle of Apollo client implementation.

1. The client and the server maintain a long connection so that it can get the first push of configuration updates. (achieved through Http Long Polling) .
2. The client also regularly pulls the latest configuration of the application from the Apollo Configuration Center server.
   * This is a fallback mechanism to prevent the configuration from being updated due to the failure of the push mechanism.
   * The client will report the local version of the timed pull, so in general, for the timed pull operation, the server will return 304 - Not Modified.
   * Timing frequency defaults to pulling every 5 minutes. Clients can also override this by specifying System Property: `apollo.refreshInterval` at runtime, in minutes.
3. After the client gets the latest configuration of the application from the Apollo Configuration Center server, it will be saved in memory.
4. The client will cache a copy of the configuration obtained from the server in the local file system. In case of service unavailability or network failure, the configuration can still be restored locally.
5. The application can get the latest configuration from the Apollo client, subscribe to configuration update notifications.

# V. Local Development Mode

Apollo client also supports local development mode, which is mainly used when the development environment cannot connect to Apollo server, such as doing related function development on cruise ships or airplanes.

In local development mode, Apollo will only read configuration information from local files, not from Apollo server.

You can enable Apollo local development mode by following the steps below.

## 5.1 Modifying the environment

Modify the `/opt/settings/server.properties` (Mac/Linux) or `C:\opt\settings\server.properties` (Windows) file to set the env to Local:

```properties
env=Local
```

For more ways to configure the environment, please refer to [1.2.4.1 Environment](en/client/java-sdk-user-guide?id=_1241-environment)

## 5.2 Preparing local configuration files

In local development mode, Apollo client will read the files from local, so we need to prepare the configuration file beforehand.

### 5.2.1 Local configuration directory

The local configuration directory is located at.

* **Mac/Linux**: /opt/data/{_appId_}/config-cache
* **Windows**: C:\opt\data\\\{_appId_}\config-cache

The appId is the appId of the application, e.g. 100004458.

Please make sure the directory exists and the application has read access to it.

**[Tip]** The recommended way is to use Apollo in normal mode first, so that Apollo will automatically create the directory and generate the configuration file under it.

### 5.2.2 Local configuration files

Local configuration files need to be placed in the local configuration directory according to a certain file name format, which is as follows.

**_{appId}+{cluster}+{namespace}.properties_**

* AppId is the application's own appId, such as 100004458
* Cluster is the cluster used by the application, generally in local mode without configuration, it is default
* Namespace is the configuration `namespace` used by the `application`, usually `application`
  ![client-local-cache](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/client-local-cache.png)

The content of the file is stored in properties format, for example, if there are two keys, one is request.timeout and the other is batch, then the content of the file is in the following format.

```properties
request.timeout=2000
batch=2000
```

## 5.3 Modifying the configuration

In local development mode, Apollo does not monitor the file content for changes in real time, so if you modify the configuration, you need to restart the application to take effect.

# VI. Test mode

The `apollo-mockserver` has been added since version 1.1.0, so that it can well support scenarios where mock configuration is required for unit testing, using the following methods.

## 6.1 Introducing pom dependencies

```xml
<dependency>
    <groupId>com.ctrip.framework.apollo</groupId>
    <artifactId>apollo-mockserver</artifactId>
    <version>1.7.0</version>
</dependency>

```

## 6.2 Placing mock data under test's resources

The file name convention is `mockdata-{namespace}.properties`

![image](https://user-images.githubusercontent.com/17842829/44515526-5e0e6480-a6f5-11e8-9c3c-4ff2ec737c8d.png)

## 6.3 Writing test classes

For more usage demos, see [ApolloMockServerApiTest.java](https://github.com/apolloconfig/apollo/blob/master/apollo-mockserver/src/test/java/com/ctrip/framework/apollo/mockserver/ApolloMockServerApiTest.java) and [ApolloMockServerSpringIntegrationTest.java](https://github.com/apolloconfig/apollo/blob/master/apollo-mockserver/src/test/java/com/ctrip/framework/apollo/mockserver/ApolloMockServerSpringIntegrationTest.java).

```java
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestConfiguration.class)
public class SpringIntegrationTest {
  // startup apollo's mockserver
  @ClassRule
  public static EmbeddedApollo embeddedApollo = new EmbeddedApollo();

  @Test
  @DirtiesContext  // This annotation is necessary because configuration injection can mess up the application context
  public void testPropertyInject(){
    assertEquals("value1", testBean.key1);
    assertEquals("value2", testBean.key2);
  }

  @Test
  @DirtiesContext
  public void testListenerTriggeredByAdd() throws InterruptedException, ExecutionException, TimeoutException {
    String otherNamespace = "othernamespace";
    embeddedApollo.addOrModifyPropery(otherNamespace,"someKey","someValue");
    ConfigChangeEvent changeEvent = testBean.futureData.get(5000, TimeUnit.MILLISECONDS);
    assertEquals(otherNamespace, changeEvent.getNamespace());
    assertEquals("someValue", changeEvent.getChange("someKey").getNewValue());
  }

  @EnableApolloConfig("application")
  @Configuration
  static class TestConfiguration{
    @Bean
    public TestBean testBean(){
      return new TestBean();
    }
  }

  static class TestBean{
    @Value("${key1:default}")
    String key1;
    @Value("${key2:default}")
    String key2;

    SettableFuture<ConfigChangeEvent> futureData = SettableFuture.create();

    @ApolloConfigChangeListener("othernamespace")
    private void onChange(ConfigChangeEvent changeEvent) {
      futureData.set(changeEvent);
    }
  }
}
```

# Ⅶ. apollo-client customization

## 7.1 ConfigService load balancing algorithm

> from version 2.1.0

To satisfy users' different demands on ConfigService load balancing algorithm when using apollo-client, we provide **spi** since version 2.1.0

The interface is `com.ctrip.framework.apollo.spi.ConfigServiceLoadBalancerClient`.

The Input is multiple ConfigServices returned by meta server, and the output is a ConfigService selected.

The default service provider is `com.ctrip.framework.apollo.spi.RandomConfigServiceLoadBalancerClient`, which chooses one ConfigService from multiple ConfigServices using random strategy .


## 7.2 Exporting Metrics to Prometheus
> For 2.4.0 and above

Metrics can be exported to Prometheus, or different implementations can be written based on SPI to integrate with various monitoring systems.

Import the client plugin
```xml
      <dependency>
        <groupId>com.ctrip.framework.apollo</groupId>
        <artifactId>apollo-plugin-client-prometheus</artifactId>
        <version>2.4.0</version>
      </dependency>
```
Adjust the configuration
```properties
apollo.client.monitor.enabled=true
apollo.client.monitor.external.type=prometheus
```
You can obtain the ExporterData via ConfigMonitor (the format depends on the monitoring system you configure, here it supports Prometheus format).

Since Prometheus retrieves metrics via pulling, users need to expose the endpoint by themselves and implement a controller like the one below.

Example code

```java
@RestController
@ResponseBody
public class TestController {

  @GetMapping("/metrics")
  public String metrics() {
    ConfigMonitor configMonitor = ConfigService.getConfigMonitor();
    return configMonitor.getExporterData();
  }
}   
```

After starting the application, let Prometheus listen to the endpoint, and by printing the request logs, you will see information in a format similar to the following.

```
# TYPE apollo_client_thread_pool_active_task_count gauge
# HELP apollo_client_thread_pool_active_task_count apollo gauge metrics
apollo_client_thread_pool_active_task_count{thread_pool_name="RemoteConfigRepository"} 0.0
apollo_client_thread_pool_active_task_count{thread_pool_name="AbstractApolloClientMetricsExporter"} 1.0
apollo_client_thread_pool_active_task_count{thread_pool_name="AbstractConfigFile"} 0.0
apollo_client_thread_pool_active_task_count{thread_pool_name="AbstractConfig"} 0.0
# TYPE apollo_client_namespace_timeout gauge
# HELP apollo_client_namespace_timeout apollo gauge metrics
apollo_client_namespace_timeout 0.0
# TYPE apollo_client_thread_pool_pool_size gauge
# HELP apollo_client_thread_pool_pool_size apollo gauge metrics
apollo_client_thread_pool_pool_size{thread_pool_name="RemoteConfigRepository"} 1.0
apollo_client_thread_pool_pool_size{thread_pool_name="AbstractApolloClientMetricsExporter"} 1.0
apollo_client_thread_pool_pool_size{thread_pool_name="AbstractConfigFile"} 0.0
apollo_client_thread_pool_pool_size{thread_pool_name="AbstractConfig"} 0.0
# TYPE apollo_client_thread_pool_queue_remaining_capacity gauge
# HELP apollo_client_thread_pool_queue_remaining_capacity apollo gauge metrics
apollo_client_thread_pool_queue_remaining_capacity{thread_pool_name="RemoteConfigRepository"} 2.147483647E9
apollo_client_thread_pool_queue_remaining_capacity{thread_pool_name="AbstractApolloClientMetricsExporter"} 2.147483647E9
apollo_client_thread_pool_queue_remaining_capacity{thread_pool_name="AbstractConfigFile"} 0.0
apollo_client_thread_pool_queue_remaining_capacity{thread_pool_name="AbstractConfig"} 0.0
# TYPE apollo_client_exception_num counter
# HELP apollo_client_exception_num apollo counter metrics
apollo_client_exception_num_total 1404.0
apollo_client_exception_num_created 1.729435502796E9
# TYPE apollo_client_thread_pool_largest_pool_size gauge
# HELP apollo_client_thread_pool_largest_pool_size apollo gauge metrics
apollo_client_thread_pool_largest_pool_size{thread_pool_name="RemoteConfigRepository"} 1.0
apollo_client_thread_pool_largest_pool_size{thread_pool_name="AbstractApolloClientMetricsExporter"} 1.0
apollo_client_thread_pool_largest_pool_size{thread_pool_name="AbstractConfigFile"} 0.0
apollo_client_thread_pool_largest_pool_size{thread_pool_name="AbstractConfig"} 0.0
# TYPE apollo_client_thread_pool_queue_size gauge
# HELP apollo_client_thread_pool_queue_size apollo gauge metrics
apollo_client_thread_pool_queue_size{thread_pool_name="RemoteConfigRepository"} 352.0
apollo_client_thread_pool_queue_size{thread_pool_name="AbstractApolloClientMetricsExporter"} 0.0
apollo_client_thread_pool_queue_size{thread_pool_name="AbstractConfigFile"} 0.0
apollo_client_thread_pool_queue_size{thread_pool_name="AbstractConfig"} 0.0
# TYPE apollo_client_namespace_usage counter
# HELP apollo_client_namespace_usage apollo counter metrics
apollo_client_namespace_usage_total{namespace="application"} 11.0
apollo_client_namespace_usage_created{namespace="application"} 1.729435502791E9
# TYPE apollo_client_thread_pool_core_pool_size gauge
# HELP apollo_client_thread_pool_core_pool_size apollo gauge metrics
apollo_client_thread_pool_core_pool_size{thread_pool_name="RemoteConfigRepository"} 1.0
apollo_client_thread_pool_core_pool_size{thread_pool_name="AbstractApolloClientMetricsExporter"} 1.0
apollo_client_thread_pool_core_pool_size{thread_pool_name="AbstractConfigFile"} 0.0
apollo_client_thread_pool_core_pool_size{thread_pool_name="AbstractConfig"} 0.0
# TYPE apollo_client_namespace_not_found gauge
# HELP apollo_client_namespace_not_found apollo gauge metrics
apollo_client_namespace_not_found 351.0
# TYPE apollo_client_thread_pool_total_task_count gauge
# HELP apollo_client_thread_pool_total_task_count apollo gauge metrics
apollo_client_thread_pool_total_task_count{thread_pool_name="RemoteConfigRepository"} 353.0
apollo_client_thread_pool_total_task_count{thread_pool_name="AbstractApolloClientMetricsExporter"} 4.0
apollo_client_thread_pool_total_task_count{thread_pool_name="AbstractConfigFile"} 0.0
apollo_client_thread_pool_total_task_count{thread_pool_name="AbstractConfig"} 0.0
# TYPE apollo_client_namespace_first_load_time_spend_in_ms gauge
# HELP apollo_client_namespace_first_load_time_spend_in_ms apollo gauge metrics
apollo_client_namespace_first_load_time_spend_in_ms{namespace="application"} 108.0
# TYPE apollo_client_thread_pool_maximum_pool_size gauge
# HELP apollo_client_thread_pool_maximum_pool_size apollo gauge metrics
apollo_client_thread_pool_maximum_pool_size{thread_pool_name="RemoteConfigRepository"} 2.147483647E9
apollo_client_thread_pool_maximum_pool_size{thread_pool_name="AbstractApolloClientMetricsExporter"} 2.147483647E9
apollo_client_thread_pool_maximum_pool_size{thread_pool_name="AbstractConfigFile"} 2.147483647E9
apollo_client_thread_pool_maximum_pool_size{thread_pool_name="AbstractConfig"} 2.147483647E9
# TYPE apollo_client_namespace_item_num gauge
# HELP apollo_client_namespace_item_num apollo gauge metrics
apollo_client_namespace_item_num{namespace="application"} 9.0
# TYPE apollo_client_thread_pool_completed_task_count gauge
# HELP apollo_client_thread_pool_completed_task_count apollo gauge metrics
apollo_client_thread_pool_completed_task_count{thread_pool_name="RemoteConfigRepository"} 1.0
apollo_client_thread_pool_completed_task_count{thread_pool_name="AbstractApolloClientMetricsExporter"} 3.0
apollo_client_thread_pool_completed_task_count{thread_pool_name="AbstractConfigFile"} 0.0
apollo_client_thread_pool_completed_task_count{thread_pool_name="AbstractConfig"} 0.0
# EOF
```

At the same time, you can also view the following information on the Prometheus console:

![Prometheus console showing Apollo client metrics](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-client-monitor-prometheus.png)

## 7.3 Exporting Metrics to Custom Monitoring Systems
> Applicable to version 2.4.0 and above

Users need to implement a MetricsExporter by extending `AbstractApolloClientMetricsExporter` and implement the following methods:
- doInit (Initialization method)
- isSupport (Method to check if the external-type configuration is supported)
- registerOrUpdateCounterSample (Method to register or update Counter metrics)
- registerOrUpdateGaugeSample (Method to register or update Gauge metrics)
- response (Method to export the required metric data)

Additionally, you need to configure the corresponding SPI files.

MetricsExporter Loading Flowchart:
```mermaid
sequenceDiagram
    participant Factory as DefaultMetricsExporterFactory
    participant Exporter as MetricsExporter
    participant SPI as SPI Loader

    %% Step 1: Factory loads all Exporters using SPI
    Factory->>SPI: loadAllOrdered(MetricsExporter)
    SPI-->>Factory: List<MetricsExporter>

    %% Step 2: Factory checks for supported Exporter
    Factory->>Exporter: isSupport(externalSystemType)
    Exporter-->>Factory: true / false

    alt Exporter Found
        %% Step 3: Factory initializes the Exporter
        Factory->>Exporter: init(listeners, exportPeriod)
        Factory-->>Client: Exporter Instance
    else No Exporter Found
        %% Step 4: Factory returns null
        Factory-->>Client: null
    end
```

### 7.3.1 SkyWalking Example
By configuring:
```properties
apollo.client.monitor.enabled=true
# Defined within the exporter
apollo.client.monitor.external.type=skywalking
```

Create a SkyWalkingMetricsExporter class, which extends AbstractApolloClientMetricsExporter.

The basic code after inheritance is as follows: 
Note: This is just an example, do not use it directly in production. Implement it according to your company's specific situation.

```java
public class SkyWalkingMetricsExporter extends AbstractApolloClientMetricsExporter {

  private static final String SKYWALKING = "skywalking";
  protected SkywalkingMeterRegistry registry;
  // When designing, users should consider if the data structure for storing metrics consumes too much memory.
  protected Map<String, Counter> counterMap;
  private Map<String, Gauge> gaugeMap;
  private Map<String, AtomicReference<Double>> gaugeValues;

  @Override
  public void doInit() {
    registry = new SkywalkingMeterRegistry();
    counterMap = new ConcurrentHashMap<>();
    gaugeValues = new ConcurrentHashMap<>();
    gaugeMap = new ConcurrentHashMap<>();
  }

  @Override
  public boolean isSupport(String form) {
    return SKYWALKING.equals(form);
  }

  @Override
  public void registerOrUpdateCounterSample(String name, Map<String, String> tags, double incrValue) {
    String key = name + tags.toString();
    Counter counter = counterMap.get(key);

    if (counter == null) {
      counter = createCounter(name, tags);
      counterMap.put(key, counter);
    }

    counter.increment(incrValue);
  }

  private Counter createCounter(String name, Map<String, String> tags) {
    return Counter.builder(name)
      .tags(tags.entrySet().stream()
        .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList()))
      .register(registry);
  }

  @Override
  public void registerOrUpdateGaugeSample(String name, Map<String, String> tags, double value) {
    String key = name + tags.toString();
    Gauge gauge = gaugeMap.get(key);
    if (gauge == null) {
      createGauge(name, tags, value);
    } else {
      gaugeValues.get(key).set(value);
    }
  }

  public void createGauge(String name, Map<String, String> tags, double value) {
    String key = name + tags.toString();
    AtomicReference<Double> valueHolder = gaugeValues.computeIfAbsent(key, k -> new AtomicReference<>(value));
    gaugeMap.computeIfAbsent(key, k -> Gauge.builder(name, valueHolder::get)
      .tags(tags.entrySet().stream()
        .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList()))
      .register(registry));
  }

  @Override
  public String response() {
    // No need to implement in SkyWalking push mode
    return "This method does not need to be implemented in SkyWalking's push mode";
  }
}

```

The doInit method is for users to extend during initialization. It will be called within the init method in AbstractApolloClientMetricsExporter.

```java
@Override
public void init(List<ApolloClientMonitorEventListener> collectors, long collectPeriod) {
  // code
  doInit(); 
  // code
}
```

Import Micrometer:
```xml
<dependency>
  <groupId>org.apache.skywalking</groupId>
  <artifactId>apm-toolkit-micrometer-1.10</artifactId>
</dependency>
```
Initialize the SkywalkingMeterRegistry according to Micrometer's mechanism, 
and use some maps to store metric data.

```java
private static final String SKYWALKING = "skywalking";
private SkywalkingMeterRegistry registry;
// When designing, users should consider if the data structure for storing metrics consumes too much memory.
private Map<String, Counter> counterMap;
private Map<String, Gauge> gaugeMap;
private Map<String, AtomicReference<Double>> gaugeValues;

@Override
public void doInit() {
  registry = new SkywalkingMeterRegistry();
  counterMap = new ConcurrentHashMap<>();
  gaugeValues = new ConcurrentHashMap<>();
  gaugeMap = new ConcurrentHashMap<>();
}

```

The isSupport method will be called when DefaultApolloClientMetricsExporterFactory reads the MetricsExporter via SPI, to check if the correct exporter is enabled when there are multiple SPI implementations.

For example, when configuring and enabling SkyWalking, and you set the apollo.client.monitor.external.type configuration value as skyWalking, the method will be implemented like this:
```java
@Override
public boolean isSupport(String form) {
  return SKYWALKING.equals(form);
}
```

The registerOrUpdateCounterSample and registerOrUpdateGaugeSample methods are used to register Counter and Gauge type metrics. You just need to register and update the data as per the parameters passed in.
```java
@Override
public void registerOrUpdateCounterSample(String name, Map<String, String> tags, double incrValue) {
  String key = name + tags.toString();
  Counter counter = counterMap.get(key);

  if (counter == null) {
    counter = createCounter(name, tags);
    counterMap.put(key, counter);
  }

  counter.increment(incrValue);
}

private Counter createCounter(String name, Map<String, String> tags) {
  return Counter.builder(name)
    .tags(tags.entrySet().stream()
      .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
      .collect(Collectors.toList()))
    .register(registry);
}

@Override
public void registerOrUpdateGaugeSample(String name, Map<String, String> tags, double value) {
  String key = name + tags.toString();
  Gauge gauge = gaugeMap.get(key);

  if (gauge == null) {
    createGauge(name, tags, value);
  } else {
    gaugeValues.get(key).set(value);
  }
}

public void createGauge(String name, Map<String, String> tags, double value) {
  String key = name + tags.toString();
  AtomicReference<Double> valueHolder = gaugeValues.computeIfAbsent(key, k -> new AtomicReference<>(value));

  gaugeMap.computeIfAbsent(key, k -> Gauge.builder(name, valueHolder::get)
    .tags(tags.entrySet().stream()
      .map(entry -> Tag.of(entry.getKey(), entry.getValue()))
      .collect(Collectors.toList()))
    .register(registry));
}
```

The response method is for monitoring systems with pull-based metrics retrieval, like Prometheus. However, since SkyWalking uses a push mode, this method does not need to be implemented. Just configure SkyWalking for your use case.

```java
@Override
public String response() {
    // No need to implement in SkyWalking's push mode
    return "This method does not need to be implemented in SkyWalking's push mode";
}

```

Finally, in the project directory under resources/META-INF/services, 
create the corresponding SPI file to tell the framework to load this class. The file name should be com.ctrip.framework.apollo.monitor.internal.exporter.ApolloClientMetricsExporter.
```text
your.package.SkyWalkingMetricsExporter
```
At this point, the client metrics data has been integrated into SkyWalking.

### 7.3.2 Prometheus Example

[PrometheusApolloClientMetricsExporter.java](https://github.com/apolloconfig/apollo-java/blob/main/apollo-plugin/apollo-plugin-client-prometheus/src/main/java/com/ctrip/framework/apollo/monitor/internal/exporter/impl/PrometheusApolloClientMetricsExporter.java)
