This document describes how to compile and run Apollo locally using the IDE so that it can help you understand the inner workings of Apollo and also prepare you for custom development.

# &nbsp;

# I. Preparation

## 1.1 Local Runtime Environment

Apollo local development requires the following components.

1. Java: 1.8+
2. MySQL: 5.6.5+ 3.
3. IDE: No special requirements

MySQL is required to create Apollo database and import the base data.
Please refer to the following sections in [distributed-deployment-guide](en/deployment/distributed-deployment-guide) for the specific steps.

1. [Preparation](en/deployment/distributed-deployment-guide?id=i-preparation)
2. [Create database](en/deployment/distributed-deployment-guide?id=_21-creating-the-database)

## 1.2 Apollo general design

Please refer to [Apollo Configuration Center Design](en/design/apollo-design) for details.

# II. Local startup

## 2.1 Apollo Config Service and Apollo Admin Service

When we develop locally, we usually start both `apollo-config service` and `apollo-adminservice` in the IDE.

The following is an example of how to start `apollo-configService` and `apollo-adminservice` locally with Intellij Community 2016.2 version.

![ConfigAdminApplication-Overview](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/ConfigAdminApplication-Overview.png)

### 2.1.1 Create a new running configuration

![NewConfiguration-Application](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/NewConfiguration-Application.png)

### 2.1.2 Main class configuration

`com.ctrip.framework.apollo.assembly.ApolloApplication`

> Note: If you want to start `apollo-configservice` and `apollo-adminservice` independently, you can replace Main Class with
> ConfigServiceApplication` and
> `com.ctrip.framework.apollo.adminservice.AdminServiceApplication`

### 2.1.3 VM options configuration

![ConfigAdminApplication-VM-Options](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/ConfigAdminApplication-VM-Options.png)

	-Dapollo_profile=github
	-Dspring.datasource.url=jdbc:mysql://localhost:3306/ApolloConfigDB?characterEncoding=utf8
	-Dspring.datasource.username=root
	-Dspring.datasource.password=

>Note 1: replace spring.datasource related configuration with your own database connection information, note that the database is `ApolloConfigDB`.
>
>Note 2: The default log output of the program is /opt/logs/100003171/apollo-assembly.log, if you need to modify the log file path, you can add the `logging.file.name` parameter, as follows.
>
>-Dlogging.file.name=/your-path/apollo-assembly.log

### 2.1.4 Program arguments configuration

`--configservice --adminservice`

### 2.1.5 Run

Click Run or Debug for the new run configuration.

![ConfigAdminApplication-Run](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/ConfigAdminApplication-Run.png)

After starting, open [http://localhost:8080](http://localhost:8080) to see that both `apollo-configservice` and `apollo-adminservice` have been started and registered to Eureka.

![ConfigAdminApplication-Eureka](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/ConfigAdminApplication-Eureka.png)

> Note: In addition to confirming the service status in Eureka, you can also confirm the service health through the health check interface at.
>
> apollo-adminservice: [http://localhost:8090/health](http://localhost:8090/health)
> apollo-configservice: [http://localhost:8080/health](http://localhost:8080/health)
>
> If the service is healthy, the status.code in the return content should be `UP`.
>
> {
> 	"status": {
> 		"code": "UP",
> 	...
> 	},
> ...
> }

## 2.2 Apollo-Portal

The following is an example of how to start `apollo-portal` locally with Intellij Community 2016.2 version.

![PortalApplication-Overview](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/PortalApplication-Overview.png)

### 2.2.1 New run configuration

![NewConfiguration-Application](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/NewConfiguration-Application.png)

### 2.2.2 Main class configuration

`com.ctrip.framework.apollo.portal.PortalApplication`

### 2.2.3 VM options configuration

![PortalApplication-VM-Options](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/PortalApplication-VM-Options.png)

	-Dapollo_profile=github,auth
	-Ddev_meta=http://localhost:8080/
	-Dserver.port=8070
	-Dspring.datasource.url=jdbc:mysql://localhost:3306/ApolloPortalDB?characterEncoding=utf8
	-Dspring.datasource.username=root
	-Dspring.datasource.password=

>Note 1: apollo_profile is specified here as `github` and `auth`, where `github` is a profile required by Apollo for database configuration, and `auth` is added from 0.9.0 to support simple authentication using Spring Security provided by apollo. For more information you can refer to [Portal-implement-user-login-function](en/extension/portal-how-to-implement-user-login-function)
>
>Note 2: spring.datasource related configuration replaced with your own database connection information, note that the database is `ApolloPortalDB `.
>
>Note 3: The default configuration imported in ApolloPortalDB will only show the configuration of DEV environment, so the dev\_meta property is configured here, if you want to show the configuration of other environment locally, you need to add the meta server address of other environment here, such as fat\_meta.
>
>Note 4: Here server.port=8070 is specified because `apollo-configservice` starts on port 8080, so here `apollo-portal` is configured to start on port 8070.
>
>Note 5: The default log output of the program is /opt/logs/100003173/apollo-portal.log. If you need to modify the log file path, you can add the `logging.file.name` parameter as follows.
>
>-Dlogging.file.name=/your-path/apollo-portal.log

### 2.2.4 Running

Click Run or Debug for the newly created run configuration.

![PortalApplication-Run](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/PortalApplication-Run.png)

After starting, open [http://localhost:8070](http://localhost:8070) to see the Apollo Configuration Center interface.

![PortalApplication-Home](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/PortalApplication-Home.png)

>Note: If `auth` profile is enabled, the default username is apollo and password is admin

### 2.2.5 Demo application access

For better development and debugging, we usually create a demo project for our own use. 

You can refer to [General Application Access Guide](en/portal/apollo-user-guide?id=i-general-application-access-guide) to create your own demo project.

## 2.3 Java sample client startup

There is a sample client project: [apollo-demo-java](https://github.com/apolloconfig/apollo-demo-java), the following is an example of how to start it locally with Intellij.

### 2.3.1 Configure the project AppId

When creating a demo project in `2.2.5 Demo Application Access`, the system will ask to fill in a globally unique AppId, which we need to configure into the app.properties file of the `apollo-demo` project: `apollo-demo-java/api-demo/src/main/resources/ META-INF/app.properties`.

![apollo-demo-app-properties](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/apollo-demo-app-properties.jpg)

If our own demo project uses an AppId of 100004458, then the file content would be

    app.id=100004458

>Note: AppId is the unique identity of the application, which is used by Apollo clients to get the application's own private Namespace configuration.

> For public Namespace configurations, you can get the configuration without the AppId, but then you lose the ability for the application to override the public Namespace configuration.

> More ways to configure AppId can be found in [1.2.1 AppId](en/client/java-sdk-user-guide?id=_121-appid)

### 2.3.2 New run configuration

![NewConfiguration-Application](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/NewConfiguration-Application.png)

### 2.3.3 Main class configuration

`com.apolloconfig.apollo.demo.api.SimpleApolloConfigDemo`

### 2.3.4 VM options configuration

![apollo-demo-vm-options](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/apollo-demo-vm-options.jpg)

    -Dapollo.meta=http://localhost:8080

> Note: Here the current environment's meta server address is `http://localhost:8080`, which is also the address of `apollo-configservice`.

> For more ways to configure Apollo Meta Server, please refer to [1.2.2 Apollo Meta Server](en/client/java-sdk-user-guide?id=_122-apollo-meta-server)

### 2.3.5 Overview

![apollo-demo-overview](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/apollo-demo-overview.jpg)

### 2.3.6 Running

Click Run or Debug on the newly created run configuration.

![apollo-demo-run](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/apollo-demo-run.png)

After starting, ignore the previous debug message and you will see the following message.

    Apollo Config Demo. Please input key to get the value. Input quit to exit.
    >

Enter the value you have configured on the Portal, such as `timeout` in our demo project, and you will see the following message.

    > timeout
    > [SimpleApolloConfigDemo] Loading key : timeout with value: 100

> The default client log level is `DEBUG`, if you need to adjust it, you can modify the level in `apollo-demo/src/main/resources/log4j2.xml`.
>
> ```xml
> <logger name="com.ctrip.framework.apollo" additivity="false" level="trace">
> <AppenderRef ref="Async" level="DEBUG"/>
> </logger>

## 2.4 .Net sample client startup

The [apollo.net](https://github.com/ctripcorp/apollo.net) project has a sample client project: `ApolloDemo`, here's an example of how to start it locally with VS 2010.

### 2.4.1 Configuring the project AppId

When creating a Demo project in `2.2.5 Demo Application Access`, the system will ask to fill in a globally unique AppId, which we need to configure into the App.config file of the `ApolloDemo` project: `apollo.net\ApolloDemo\App.config`.

![apollo-demo-app-config](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-net-app-config.png)

If our own demo project uses an AppId of 100004458, then the contents of the file would be

```xml
<add key="AppID" value="100004458"/>
```

> Note: AppId is a unique identifier for the application, which Apollo clients use to get the application's own private Namespace configuration.

> For public Namespace configurations, the configuration can be obtained without the AppId, but the ability of the application to override the public Namespace configuration is lost.

### 2.4.2 Configuring Service Addresses

Apollo client will get the configuration from different servers for different environments, so we need to configure the server address (Apollo.{ENV}.Meta) in app.config or web.config. Suppose the DEV environment's configuration service (apollo-config service) address is 11.22.33.44, then we will do the following configuration.

![apollo-net-server-url-config](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-net-server-url-config.png)

### 2.4.3 Running

Just run `ApolloConfigDemo.cs`.

After starting, ignore the previous debugging message and you will see the following prompt.

    Apollo Config Demo. Please input key to get the value. Input quit to exit.
    >

Enter the value you configured on the Portal, such as `timeout` in our demo project, and you will see the following message.

    > timeout
    > Loading key: timeout with value: 100

>Net client will output logs directly to the Console by default, so you can implement your own logging-related features.
>You can implement your own logging-related functions.
>See [https://github.com/ctripcorp/apollo.net/tree/master/Apollo/Logging/Spi](https://github.com/ctripcorp/apollo.net/tree/master/) for details Apollo/Logging/Spi)

# III. Development

## Module dependency diagram

![Module Dependency Diagram](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/module-dependency.png)

## 3.1 Portal implementation for user login

Please refer to [Portal implementation of user login function](en/extension/portal-how-to-implement-user-login-function)

## 3.2 Portal access to mail service

Please refer to [Portal Enabling Email Service](en/extension/portal-how-to-enable-email-service)

## 3.3 Shared session for Portal cluster deployment

Please refer to [Portal Shared Session](en/extension/portal-how-to-enable-session-store)
