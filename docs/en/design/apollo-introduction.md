# &nbsp;

# 1. What is Apollo

## 1.1 Background

With the increasing complexity of program functions, the configuration of the program is increasing: switches of various functions, parameter configuration, server address...

The expectations for program configuration are also getting higher and higher: real-time effective after configuration modification, grayscale release, sub-environment, sub-cluster management configuration, perfect authority, audit mechanism...

In such a large environment, traditional methods such as configuration files and databases have become increasingly unable to meet developers' needs for configuration management.

Apollo Configuration Center came into being!

## 1.2 Introduction to Apollo

Apollo (Apollo) is a reliable distributed configuration management center, which was born in the Ctrip framework R&D department. It can centrally manage the configuration of different environments and different clusters of applications. After the configuration is modified, it can be pushed to the application side in real time, and it has standardized Features such as permissions and process governance are suitable for microservice configuration management scenarios.

Apollo supports 4 dimensions to manage the configuration in Key-Value format:


1. application
2. environment
3. cluster (cluster)
4. namespace

At the same time, Apollo is developed based on the open source model, open source address: <a href="https://github.com/ctripcorp/apollo" target="_blank">https://github.com/ctripcorp/apollo</a>

## 1.2 Basic Concepts of Configuration

Since Apollo is positioned in the configuration center, it is necessary to briefly introduce what configuration is here.

According to our understanding, the configuration has the following properties:

* **Configuration is a read-only variable independent of the program**
  * Configuration is independent of the program first, the same program will behave differently under different configurations.
  * Second, the configuration is read-only to the program, the program changes its behavior by reading the configuration, but the program should not change the configuration.
  * Common configurations are: DB Connection Str, Thread Pool Size, Buffer Size, Request Timeout, Feature Switch, Server Urls, etc.

* **Configuration accompanies the entire life cycle of the application**
  * Configuration runs through the entire life cycle of the application. The application is initialized by reading the configuration at startup, and the behavior is adjusted according to the configuration at runtime.

* **Configuration can be loaded in multiple ways**
  * There are also many ways to load the configuration, the common ones are the internal hard code of the program, configuration files, environment variables, startup parameters, database-based, etc.

**Configuration requires governance**
  * Permission control
    * Since the configuration can change the behavior of the program, incorrect configuration can even cause disasters, so the modification of the configuration must have a relatively complete permission control
  * Different environments, cluster configuration management
    * The same program often needs to have different configurations in different environments (development, testing, production) and different clusters (such as different data centers), so it is necessary to have complete environment and cluster configuration management
  * Framework class component configuration management
    * There is also a special kind of configuration - framework class component configuration, such as the configuration of CAT client.
    * Although this type of framework component is developed and maintained by other teams, the runtime is in the actual business application, so it can be considered that the framework component is also a part of the application in essence.
    * The configuration corresponding to such components also needs to have a relatively complete management method.

# 2. Why Apollo

It is precisely based on the particularity of configuration that Apollo has been determined to become a configuration publishing platform with governance capabilities from the beginning of its design. Currently, it provides the following features:

* **Unified management of the configuration of different environments and different clusters**
  * Apollo provides a unified interface to centrally manage the configuration of different environments, clusters, and namespaces.
  * The same code is deployed in different clusters and can have different configurations, such as the address of zookeeper, etc.
  * It is convenient to support multiple different applications to share the same configuration through namespace (namespace), and also allows applications to override the shared configuration

* **Configuration changes take effect in real time (hot release)**
  * After the user modifies the configuration in Apollo and publishes it, the client can receive the latest configuration in real time (1 second) and notify the application

**Version release management**
  * All configuration releases have a version concept, which can easily support configuration rollback

**Greyscale Release**
  * Support configured grayscale publishing. For example, after clicking publish, it will only take effect on some application instances. After observing for a period of time, there is no problem before pushing to all application instances.

* **Permission management, release audit, operation audit**
  * The management of application and configuration has a complete authority management mechanism, and the management of configuration is also divided into two links: editing and publishing, thereby reducing human errors.
  * All operations have audit logs, which can easily track problems

* **Client configuration information monitoring**
  * You can easily see which instances the configuration is being used on the interface

**Global Search Configuration Items**

- A fuzzy search of the key and value of a configuration item finds in which application, environment, cluster, namespace the configuration item with the corresponding value is used
- It is easy for administrators and SRE roles to quickly and easily find and change the configuration values of resources by highlighting, paging and jumping through configurations

**Java and .Net native clients available**

  * Provides native clients of Java and .Net for easy application integration
  * Support Spring Placeholder, Annotation and Spring Boot's ConfigurationProperties for easy application use (requires Spring 3.1.1+)
  * Also provides Http interface, non-Java and .Net applications can also be easily used

**Provide open platform API**
  * Apollo itself provides a relatively complete unified configuration management interface, which supports multi-environment, multi-data center configuration management, permissions, process governance and other features. However, Apollo does not impose too many restrictions on configuration modifications for the sake of generality. As long as it conforms to the basic format, it can be saved. It will not perform targeted verification for different configuration values, such as database username and password, Redis service address, etc.
  * For this type of application configuration, Apollo supports the application side to modify and publish the configuration in Apollo through the open platform API, and has complete authorization and permission control

**Easy to deploy**
  * As a basic service, the configuration center has very high availability requirements, which requires Apollo to have as few external dependencies as possible
  * Currently the only external dependency is MySQL, so deployment is very simple, as long as Java and MySQL are installed, Apollo can run
  * Apollo also provides a packaging script, which can generate all the required installation packages with one click, and supports custom runtime parameters

# 3. Apollo at a glance

## 3.1 Basic model

The following is the basic model of Apollo:

1. The user modifies and publishes the configuration in the configuration center
2. The configuration center notifies the Apollo client that there is a configuration update
3. The Apollo client pulls the latest configuration from the configuration center, updates the local configuration and notifies the application

![basic-architecture](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/basic-architecture.png)

## 3.2 Interface overview

![apollo-home-screenshot](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-home-screenshot.jpg)

The picture above is the configuration home page of a project in the Apollo Configuration Center

* The environment list module at the top left of the page displays all environments and clusters, and users can switch at any time.
* The configuration information of two namespaces (application and FX.apollo) is displayed in the center of the page, which is displayed and edited in table mode by default. Users can also switch to text mode to view and edit as files.
* Operations such as publishing, rollback, grayscale, authorization, viewing change history and publishing history can be easily performed on the page

## 3.3 Add or modify configuration items

Users can easily add/modify configuration items through the configuration center interface. For more usage instructions, please refer to [Application Access Guide](en/portal/apollo-user-guide)

![edit-item-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/edit-item-entry.png)

Enter configuration information:

![edit-item](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/edit-item.png)

## 3.4 Release configuration

Publish the configuration through the configuration center:

![publish-items](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/publish-items-entry.png)

Fill in the release information:

![publish-items](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/publish-items.png)

## 3.5 Client get configuration (Java API sample)

After the configuration is released, it can be obtained on the client side. Taking Java as an example, the sample code for obtaining the configuration is as follows. Apollo client also supports integration with Spring. For more client usage instructions, please refer to [Java Client Usage Guide](en/client/java-sdk-user-guide) and [.Net Client Usage Guide](en/client/dotnet-sdk-user-guide).

```java
Config config = ConfigService.getAppConfig();
Integer defaultRequestTimeout = 200;
Integer requestTimeout = config.getIntProperty("requestTimeout", defaultRequestTimeout);
```

## 3.6 Client monitoring configuration changes

By obtaining the configuration code above, the application can obtain the latest configuration in real time.

However, in some scenarios, the application also needs to be notified when the configuration changes, such as the switching of database connections, so Apollo also provides the function of monitoring configuration changes. The Java example is as follows:

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

## 3.7 Spring integration example

Apollo and Spring can also be easily integrated. You only need to mark `@EnableApolloConfig` to get configuration information through `@Value`:

```java
@Configuration
@EnableApolloConfig
public class AppConfig {}
```

```java
@Component
public class SomeBean {
    //The value of timeout will be updated automatically
    @Value("${request.timeout:200}")
    private int timeout;
}
```

# 4. Apollo in depth

Through the above introduction, I believe you have a preliminary understanding of Apollo, and I believe that most of the usage scenarios have been covered.

The next section will mainly introduce Apollo's cluster management, namespace management and the corresponding configuration acquisition rules.

## 4.1 Core Concepts

Before introducing advanced features, we need to understand a few core concepts in Apollo.

1. **application (application)**
   * Apollo clients need to know who the current application is at runtime, so they can go get the corresponding configuration.
   * Each application needs to have a unique identity -- appId, we believe that the application identity follows the code, so it needs to be configured in the code, see [Java Client Usage Guide](en/client/java-sdk-user-guide) for more information.

2. **environment (environment)**
   * Configure the corresponding environment, Apollo client needs to know which environment the current application is in at runtime, so that it can go get the application configuration
   * We believe that the environment is independent of the code, the same code deployed in different environments should be able to access the configuration of different environments
   * So the environment is specified by default by reading the configuration on the machine (the env property in server.properties), but for development convenience, we also support runtime specification by System Property, etc. For more information, see [Java Client User Guide](en/client/java-sdk-user-guide). 

3. **cluster (cluster)**
   * Grouping of different instances of an application, for example, typically by data center, dividing the application instances in the Shanghai server room into one cluster, and dividing the application instances in the Beijing server room into another cluster.
   * For different clusters, the same configuration can have different values, such as zookeeper address.
   * Clusters are specified by default by reading the configuration on the machine (idc property in server.properties), but they are also supported at runtime by System Property, see [Java Client Usage Guide](en/client/java-sdk-user-guide) for more information.

4. **namespace (namespace)**
   * A grouping of different configurations under an application. Namespace can be simply compared to a file, where different types of configurations are stored in different files, such as database configuration files, RPC configuration files, the application's own configuration files, etc.
   * Applications can directly read the configuration namespace of public components, such as DAL, RPC, etc.
   * The application can also adjust the configuration of the public component by inheriting the configuration namespace of the public component, such as the initial database connection number of DAL

## 4.2 Customizing Cluster

> This section is only required if the application needs to apply different configurations to different clusters, if there is no relevant need, you can skip this section]

For example, if we have applications deployed in both data center A and data center B, then if we want the configuration of the two data centers to be different, we can solve it by creating a new cluster.

### 4.2.1 New Cluster

Only the project administrator has access to the new Cluster. The administrator can see the "Add Cluster" button on the left side of the page.

![create-cluster](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-cluster.png)

When you click it, you will enter the Add Cluster page. Generally, you can divide the clusters by data center, such as SHAJQ, SHAOY, etc. However, custom clusters are also supported.

However, custom clusters are also supported, for example, you can create a cluster for a machine in room A and a machine in room B, using one set of configurations.

![create-cluster-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-cluster-detail.png)

### 4.2.2 Adding configuration in Cluster and publishing

After the cluster is successfully added, you can add configuration to the cluster. First, you need to switch to SHAJQ cluster as shown in the figure below, and then the configuration addition process is the same as [3.3 Adding/Modifying Configuration Items](#_33-add-or-modify-configuration-items), so we won't go over it here.

![cluster-created](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/cluster-created.png)

### 4.2.3 Specifying the Cluster to which the application instance belongs

Apollo will use the data center where the application instance is located as the cluster by default, so no additional configuration is needed if the two are the same.

If the cluster and data center are not the same, then you need to specify the runtime cluster by using System Property.

* -Dapollo.cluster=SomeCluster
* Note here that `apollo.cluster` is all lowercase

## 4.3 Customizing Namespace

> [This section is only required for public component configuration or shared configuration of multiple applications, you can skip this section if you don't have any relevant requirements]

If the application has public components (such as hermes-producer, cat-client, etc.) for other applications to use, you need to implement the configuration of public components through custom namespace.

### 4.3.1 New Namespace

Take hermes-producer as an example, you need to create a new namespace first, only the project administrator has permission to create namespace, the administrator can see the "Add Namespace" button on the left side of the page.

![create-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-namespace.png)

Apollo will prefix the namespace with the department the application belongs to, such as FX.

![create-namespace-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-namespace-detail.png)

### 4.3.2 Associating to environments and clusters

After the namespace is created, you need to select which environments and clusters to use it under

![link-namespace-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/link-namespace-detail.png)

### 4.3.3 Add configuration items to the Namespace

Next, add a configuration item under this new namespace

![add-item-in-new-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/add-item-in-new-namespace.png)

Once added, you can see the configuration in the FX.Hermes.Producer namespace.

![item-created-in-new-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/item-created-in-new-namespace.png)

### 4.3.4 Publish namespace configuration

![publish-items-in-new-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/publish-items-in-new-namespace.png)

### 4.3.5 Client-side fetching of Namespace configuration

Apollo client also supports integration with Spring, see [Java Client User Guide](en/client/java-sdk-user-guide) and [Net Client Net client](en/client/dotnet-sdk-user-guide).

```java
Config config = ConfigService.getConfig("FX.Hermes.Producer");
Integer defaultSenderBatchSize = 200;
Integer senderBatchSize = config.getIntProperty("sender.batchsize", defaultSenderBatchSize);
```

### 4.3.6 Client listens for Namespace configuration changes

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

### 4.3.7 Sample Spring integration

``` java
@Configuration
@EnableApolloConfig("FX.Hermes.Producer")
public class AppConfig {}
```

```java
@Component
public class SomeBean {
    //the value of timeout will be updated automatically
    @Value("${request.timeout:200}")
    private int timeout;
}
```

## 4.4 Configuring fetch rules

> [This section is only needed if the application is customized with a cluster or namespace, if there is no related need, you can skip this section]

After the concept of cluster, the rules of configuration become important.

For example, if the application is deployed in server room A, but no new cluster is created in Apollo, what is the behavior of Apollo at this time?

Or if cluster=SomeCluster is specified at runtime, but no new cluster is created in Apollo, what is the behavior of Apollo at this time?

The next step is to introduce the rules for configuration acquisition.

### 4.4.1 Application's own configuration acquisition rules

When the application uses the following statement to get the configuration, we call it getting the application's own configuration, that is, the configuration of the application namespace of the application itself.

```java
Config config = ConfigService.getAppConfig();
```

The rules for getting the configuration for this case, in short, are as follows.

1. first look for the configuration of the runtime cluster (specified by apollo.cluster)
2. if not found, then look for the configuration of the data center cluster
3. if still not found, then return the configuration of the default cluster

The diagram is as follows.

![application-config-precedence](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/application-config-precedence.png)

So if the application is deployed in datacenter A, but the user has not created a cluster in Apollo, then the configuration obtained is that of the default cluster (default).

If the application is deployed in data center A, and SomeCluster is specified at runtime, but no cluster is created in Apollo, then the configuration obtained is that of data center A cluster, and if data center A cluster is not configured, then the configuration obtained is that of default cluster (default).

### 4.4.2 Public component configuration acquisition rules

Take `FX.Hermes.Producer` as an example, hermes producer is a public component published by hermes. When the following statement is used to get the configuration, we call it getting the configuration of the public component.

```java
Config config = ConfigService.getConfig("FX.Hermes.Producer");
```

The rules for getting the configuration for this case, in short, are as follows.

1. first get the configuration of the `FX.Hermes.Producer` namespace under the current application
Then get the configuration of `FX.Hermes.Producer` namespace under the hermes application
3. The concatenation of the above two configurations is the final used configuration, if there is any part with the same key, the current application takes precedence

The diagram is as follows.

![public-namespace-config-precedence](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-config-precedence.png)

In this way, the configuration management of the framework class components is achieved. The framework component provider provides the default values for the configuration, and the application can override them if it has special needs.

## 4.5 Overall design

![overall-architecture](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/overall-architecture.png)

The above diagram briefly describes the overall design of Apollo, which we can see from bottom to top.

* Config Service provides configuration reading, pushing and other functions, and the service object is Apollo client
* Admin Service provides configuration modification and publishing, and the service object is Apollo Portal (management interface).
* Config Service and Admin Service are both multi-instance, stateless deployments, so they need to register themselves to Eureka and keep the heartbeat
* On top of Eureka, we erected a Meta Server to encapsulate Eureka's service discovery interface
* Client accesses Meta Server via domain name to get Config Service service list (IP+Port), and then accesses the service directly via IP+Port, while doing load balance and error retry on the Client side.
* Portal accesses Meta Server through domain name to get the Admin Service service list (IP+Port), and then accesses the service directly through IP+Port, and at the same time does load balance and error retry on the Portal side.
* To simplify deployment, we will actually deploy the three logical roles Config Service, Eureka and Meta Server in the same JVM process

### 4.5.1 Why Eureka

Why do we use Eureka as a service registry instead of the traditional zk, etcd? I roughly summarized the following reasons.

* It provides a complete Service Registry and Service Discovery implementation
  * First of all, it provides a complete implementation and has also withstood the test of Netflix's own production environment, so it's relatively painless to use.
* Seamless integration with Spring Cloud
  * Our project itself uses Spring Cloud and Spring Boot, and Spring Cloud has a very comprehensive set of open source code to integrate with Eureka, so it's very easy to use.
  * In addition, Eureka supports starting in our application's own container, which means that after our application is started, it acts as both an Eureka and a service provider. This greatly improves the availability of the service.
  * **This is the main reason why we chose Eureka over zk, etc. In order to improve the availability of the configuration center and reduce the complexity of deployment, we need to minimize external dependencies as much as possible.**
* Open Source
  * The last point is open source. Since the code is open source, it is very easy for us to understand how it is implemented and troubleshoot problems.

## 4.6 Client Design

![client-architecture](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/client-architecture.png)

The above diagram briefly describes how the Apollo client is implemented.

1. the client maintains a long connection with the server so that it can be the first to get configuration updates pushed to it.
   The client also regularly pulls the latest configuration of the application from the Apollo Configuration Center server.
   * This is a fallback mechanism to prevent the configuration from not being updated due to the failure of the push mechanism.
   * The client will report the local version when it is pulled, so in general, the server will return 304 - Not Modified for the timed pull operation.
   * Timing frequency defaults to pulling every 5 minutes. Clients can also override this by specifying System Property: `apollo.refreshInterval` at runtime, in minutes.
3. After the client gets the latest configuration of the application from the Apollo Configuration Center server, it will be saved in memory
4. the client will cache a copy of the configuration fetched from the server on the local file system
   * In case of service unavailability or network failure, the configuration can still be restored locally
5. the application gets the latest configuration from the Apollo client and subscribes to configuration update notifications

### 4.6.1 Apollo-Configuration auto-push mechanism

As mentioned earlier, Apollo client and server maintain a long connection to get the first push of configuration updates.

The long connection is actually implemented through Http Long Polling, specifically.

* The client initiates an Http request to the server
* The server will hold the connection for 60 seconds
  * If there is a configuration change that the client cares about within 60 seconds, the held client request will return immediately and inform the client of the configuration change namespace information, and the client will pull the latest configuration of the corresponding namespace accordingly
  * If there is no configuration change that the client cares about within 60 seconds, then the Http status code 304 will be returned to the client
* The client will re-initiate the connection immediately after receiving the server-side request, going back to the first step

Considering that there will be tens of thousands of clients initiating long connections to the server, on the server side we use async servlet (Spring DeferredResult) to serve Http Long Polling requests.

## 4.7 Availability considerations

The following table describes the availability of Apollo under different scenarios.

| Scenario                            | Impact                                                       | Downgrade                                                    | Reason                                                       |
| ----------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| One of config services goes offline | No effect                                                    |                                                              | The Config service is stateless, and the client reconnects to other config services |
| All config services are offline     | The client cannot read the latest configuration, and Portal has no effect | When the client restarts, the local cache configuration file can be read |                                                              |
| One of admin services goes offline  | No effect                                                    |                                                              | The Admin service has no status, and Portal reconnects to other admin services |
| All admin services are offline      | The client is not affected, and the portal cannot update the configuration |                                                              |                                                              |
| One of portals goes offline         | No effect                                                    |                                                              | The portal domain name is bound to multiple servers through slb, and points to an available server after retrying |
| All portals are offline             | The client is not affected, and the portal cannot update the configuration |                                                              |                                                              |
| One of data center goes offline     | No effect                                                    |                                                              | Multi-data center deployment, data is fully synchronized, Meta Server/Portal domain name is automatically switched to other surviving data centers through slb |

# 5、Contribute to Apollo

Apollo is developed in open source mode from the beginning of development, so you are also very welcome to join in the interest and spare capacity of friends.

The server-side development is in Java, based on Spring Cloud and Spring Boot framework. The client side currently provides both Java and .

GitHub address: <a href="https://github.com/ctripcorp/apollo" target="_blank">https://github.com/ctripcorp/apollo</a>

Welcome to submit a Pull Request!
