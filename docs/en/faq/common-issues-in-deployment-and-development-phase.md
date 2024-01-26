### 1. How does windows execute build.sh?
Install [Git Bash](https://git-for-windows.github.io/), then run `./build.sh` Note the first `./`

### 2. When running locally Portal keeps reporting Env is down. 

By default config service starts on port 8080 and admin service starts on port 8090. Please check if these two ports are occupied by other applications.

If there is also an exception message: org.springframework.web.client.HttpClientErrorException: 405 Method Not Allowed, it is usually due to the local startup of `ShadowSocks`, because `ShadowSocks` will occupy port 8090 by default. port 8090 by default.

Version 1.1.0 added **System Information** page, you can view the current Meta Server and admin service information of each environment through `Administrator Tools` -> `System Information` to help troubleshoot the problem.

### 3. Admin server or config server is registered with intranet IP, resulting in portal or client cannot access admin server or config server

Please refer to [network policy](en/deployment/distributed-deployment-guide?id=_14-network-policy).

### 4. How to add environment by Portal Console

#### 4.1 1.6.0 and above

Version 1.6.0 adds the ability to customize the environment, which allows you to add an environment without modifying the code

1. add environment for protaldb, refer to [3.1 Adjusting ApolloPortalDB configuration](en/deployment/distributed-deployment-guide?id=_31-adjusting-apolloportaldb-configuration)
2. add the meta server address corresponding to the new environment for apollo-portal, refer to: [2.2.1.1.2.4 Configuring meta service information for apollo-portal](en/deployment/distributed-deployment-guide?id=_221124-configuring-apollo-portal39s-meta-service-information). apollo-client also needs to be configured properly when used in a new environment, refer to: [1.2.2 Apollo Meta Server](en/client/java-sdk-user-guide?id=_122-apollo-meta-server).

>Note 1: A set of Portal can manage multiple environments, but each environment needs to deploy a set of Config Service, Admin Service and ApolloConfigDB independently, please refer to: [2.1.2 Creating ApolloConfigDB](en/deployment/distributed-deployment-guide?id=_212-creating-apolloconfigdb), [3.2 Adjusting ApolloConfigDB configuration](en/deployment/distributed-deployment-guide?id=_32-adjusting-apolloconfigdb-configuration), [2.2.1.1.2 Configuring database connection information](en/deployment/distributed-deployment-guide?id=_22112-configuring-database-connection-information)

> Note 2: If you are adding an environment to Apollo Configuration Center that has been running for a while, don't forget to refer to [2.1.2.4 Importing ApolloConfigDB project data from another environment](en/deployment/distributed-deployment-guide?id=_2124-importing-apolloconfigdb-project-data-from-another-environment) to initialize the new environment

#### 4.2 1.5.1 and earlier
##### 4.2.1 Adding an Apollo pre-defined environment

If the environment to be added is an Apollo pre-defined environment (DEV, FAT, UAT, PRO), two steps are required.
1. protaldb add environment, refer to [3.1 Adjusting ApolloPortalDB configuration](en/deployment/distributed-deployment-guide?id=_31-adjusting-apolloportaldb-configuration)
2. add the meta server address corresponding to the new environment for apollo-portal, refer to: [2.2.1.1.2.4 Configuring meta service information for apollo-portal](en/deployment/distributed-deployment-guide?id=_221124-configuring-apollo-portal39s-meta-service-information). apollo-client also needs to be configured properly when used in a new environment, refer to: [1.2.2 Apollo Meta Server](en/client/java-sdk-user-guide?id=_122-apollo-meta-server).

>Note 1: A set of Portal can manage multiple environments, but each environment needs to deploy a set of Config Service, Admin Service and ApolloConfigDB independently, please refer to: [2.1.2 Creating ApolloConfigDB](en/deployment/distributed-deployment-guide?id=_212-creating-apolloconfigdb), [3.2 Adjusting ApolloConfigDB configuration](en/deployment/distributed-deployment-guide?id=_32-adjusting-apolloconfigdb-configuration), [2.2.1.1.2 Configuring database connection information](en/deployment/distributed-deployment-guide?id=_22112-configuring-database-connection-information)

> Note 2: If you are adding an environment to Apollo Configuration Center that has been running for a while, don't forget to refer to [2.1.2.4 Importing ApolloConfigDB project data from another environment](en/deployment/distributed-deployment-guide?id=_2124-importing-apolloconfigdb-project-data-from-another-environment) to initialize the new environment

> Note 3: if your custom environment name is "PROD", it will be forcibly converted to "PRO". Similarly, if the environment name is "FWS", it will be forcibly converted to "FAT".

##### 4.2.2 Adding a custom environment

If the environment to be added is not one of Apollo's pre-defined environments, please refer to the following steps.

1. Assume the name of the environment to be added is beta
2. Modify [com.ctrip.framework.apollo.core.enums.Env](https://github.com/apolloconfig/apollo/blob/master/apollo-core/src/main/java/com/ctrip/framework/apollo/core/enums/Env.java) class by adding the ``BETA`` enumeration to it.
```java
public enum Env{
  LOCAL, DEV, BETA, FWS, FAT, UAT, LPT, PRO, TOOLS, UNKNOWN;
  ...
}
```
3. Modify the [com.ctrip.framework.apollo.core.enums.EnvUtils](https://github.com/apolloconfig/apollo/blob/master/apollo-core/src/main/java/com/ctrip/framework/apollo/core/enums/EnvUtils.java) class to include conversion logic for the `BETA` enumeration.
```java
public final class EnvUtils {
  
  public static Env transformEnv(String envName) {
    if (StringUtils.isBlank(envName)) {
      return Env.UNKNOWN;
    }
    switch (envName.trim().toUpperCase()) {
      ...
      case "BETA":
        return Env.BETA;
      ...
      default:
        return Env.UNKNOWN;
    }
  }
}
```
4. Modify [apollo-env.properties](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/resources/apollo-env.properties) to add the ``beta.meta`` placeholder.
```properties
local.meta=http://localhost:8080
dev.meta=${dev_meta}
fat.meta=${fat_meta}
beta.meta=${beta_meta}
uat.meta=${uat_meta}
lpt.meta=${lpt_meta}
pro.meta=${pro_meta}
```
5. Modify the [com.ctrip.framework.apollo.core.internals.LegacyMetaServerProvider](https://github.com/apolloconfig/apollo/blob/master/apollo-core/src/main/java/com/ctrip/framework/apollo/core/internals/LegacyMetaServerProvider.java) class to add logic to read the meta server address of the ``BETA`` environment.
```java
public class LegacyMetaServerProvider {
    ...
    domains.put(Env.BETA, getMetaServerAddress(prop, "beta_meta", "beta.meta"));
    ...
}
```

6. Add `BETA` environment for protaldb, refer to [3.1 Adjusting ApolloPortalDB configuration](en/deployment/distributed-deployment-guide?id=_31-adjusting-apolloportaldb-configuration)
7. Add the meta server address corresponding to the new environment for apollo-portal, refer to: [2.2.1.1.2.4 Configuring meta service information for apollo-portal](en/deployment/distributed-deployment-guide?id=_221124-configuring-apollo-portal39s-meta-service-information). apollo-client also needs to be configured properly when used in a new environment, refer to: [1.2.2 Apollo Meta Server](en/client/java-sdk-user-guide?id=_122-apollo-meta-server).

>Note 1: A set of Portal can manage multiple environments, but each environment needs to deploy a set of Config Service, Admin Service and ApolloConfigDB independently, please refer to: [2.1.2 Creating ApolloConfigDB](en/deployment/distributed-deployment-guide?id=_212-creating-apolloconfigdb), [3.2 Adjusting ApolloConfigDB configuration](en/deployment/distributed-deployment-guide?id=_32-adjusting-apolloconfigdb-configuration), [2.2.1.1.2 Configuring database connection information](en/deployment/distributed-deployment-guide?id=_22112-configuring-database-connection-information)

> Note 2: If you are adding an environment to Apollo Configuration Center that has been running for a while, don't forget to refer to [2.1.2.4 Importing ApolloConfigDB project data from another environment](en/deployment/distributed-deployment-guide?id=_2124-importing-apolloconfigdb-project-data-from-another-environment) to initialize the new environment

### 5. How do I delete applications, clusters, Namespace?

From version 0.11.0 Apollo Administrator has added a page to delete applications, clusters and AppNamespace, we recommend to use this page to delete them.

Page entry :

![delete-app-cluster-namespace-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/delete-app-cluster-namespace-entry.png)

Page details :

![delete-app-cluster-namespace-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/delete-app-cluster-namespace-detail.png)

### 6. How to solve the problem of getting inaccurate IP due to multiple NICs on the client side?

The logic of getting client NIC has been adjusted in version 1.4.0, so you need to differentiate according to client version

#### 6.1 `apollo-client` for 1.3.0 and earlier versions
If there are multiple NICs and they are all normal NICs, you need to add a mapping relationship in /etc/hosts to raise the weight.

Format: `ip ${hostname}`

Here ${hostname} is the result of your execution of hostname on the machine.

For example, if the correct IP is: 192.168.1.50, the result of the hostname execution is: jim-ubuntu-pc
Then the final record mapped in the hosts file is

```
192.168.1.50 jim-ubuntu-pc
```

#### 6.2 apollo-client for version 1.4.0 and later

If you have multiple NICs and they are all normal NICs, you can change the priority by adjusting their order in the system, with the first in the order having higher priority.

### 7. Dynamically adjusting the Logging level of Spring Boot via Apollo

You can refer to [spring-cloud-logger](https://github.com/ctripcorp/apollo) in the [apollo-use-cases](https://github.com/ctripcorp/apollo-use-cases/tree/master/spring-cloud-logger) project and [spring-boot-logger](https://github.com/ctripcorp/apollo-use-cases/tree/master/spring-boot-logger) code examples.

### 8. Register Config Service and Admin Service to a separate Eureka Server

Apollo comes with Eureka as an internal registry implementation by default, and there is generally no need to consider deploying a separate registry for Apollo.

However, some companies already have a set of Eureka, if you want to register Apollo's Config Service and Admin Service to achieve unified management, you can follow the steps below.

#### 1. Configure Config Service to not start the built-in Eureka Server

##### 1.1 1.5.0 and above

Configure apollo-configService with `apollo.eureka.server.enabled=false`, either through bootstrap.yml or the -D parameter.

##### 1.2 versions before 1.5.0

Modify [com.ctrip.framework.apollo.configservice.ConfigServiceApplication](https://github.com/apolloconfig/apollo/blob/master/apollo-configservice/src/main/java/com/ctrip/framework/apollo/configservice/ConfigServiceApplication.java) , change `@EnableEurekaServer` to `@ EnableEurekaClient`

```java
@EnableEurekaClient
@EnableAspectJAutoProxy
@EnableAutoConfiguration // (exclude = EurekaClientConfigBean.class)
@Configuration
@EnableTransactionManagement
@PropertySource(value = {"classpath:configservice.properties"})
@ComponentScan(basePackageClasses = {ApolloCommonConfig.class,
    ApolloBizConfig.class,
    ConfigServiceApplication.class,
    ApolloMetaServiceConfig.class})
public class ConfigServiceApplication {
  ...
}
```

#### 2. Modify `eureka.service.url` in ApolloConfigDB.ServerConfig table to point to your own Eureka address

For example, if your own Eureka service address is 1.1.1.1:8761 and 2.2.2.2:8761, then set `eureka.service.url` in `ApolloConfigDB.ServerConfig` table with:

```
http://1.1.1.1:8761/eureka/,http://2.2.2.2:8761/eureka/
```

Note that changing the Eureka address only requires changing the `eureka.service.url` in the `ApolloConfigDB.ServerConfig` table, not the meta server address.

> By default, the meta service and config service are deployed in the same JVM process, so the address of the meta service is the address of the config service, so you don't need to change the meta server address when you modify the Eureka address.

### 9. Using `ConditionalOnProperty` in Spring Boot does not read the configuration

The `@ConditionalOnProperty` feature is supported since version 0.10.0, see [Spring Boot integration method](en/client/java-sdk-user-guide?id=_3213-spring-boot-integration-methods-recommended)

### 10. How to achieve the client in room A reads the config service of room A nearby and the client in room B reads the config service of room B nearby in multiple rooms?

Please refer to [Issue 1294](https://github.com/apolloconfig/apollo/issues/1294), in this case, because the Chinese and American server rooms are far away from each other, so the config db needs to be deployed in two places. If it is a multi-city machine room, the config service of both machine rooms can be connected to one config db.

### 11. Does apollo have support for `HEAD` request pages? AliCloud slb configuration health check only supports `HEAD` requests

Each service of apollo has `/health` page, which is used by apollo to do health check and supports various request methods such as GET, POST, HEAD, etc.

### 12. How can apollo be configured to view permissions?

Starting from version 1.1.0, apollo-portal adds support for view permissions, which allows you to configure an environment to allow only project members to view the private Namespace.

The project members here are
1. The project's administrator
2. Have the permission to modify or publish the private Namespace in that environment

The configuration is very simple, after logging in with the super administrator account, go to `Administrator Tools - System Parameters` page to add or modify `configView.memberOnly.envs` configuration items.

![configView.memberOnly.envs](https://user-images.githubusercontent.com/837658/46456519-c155e100-c7e1-11e8-969b-8f332379fa29.png)

### 13. How to put apollo in a separate tomcat to run?

Some companies' O&M policies may require that they must use a standalone tomcat to run their applications and do not allow apollo to run in the default startup.sh way. The following is a brief example of how to make the apollo server run in a standalone tomcat.

1. Get the apollo code (the release version is recommended for production deployments)
2. Modify the apollo-configservice pom.xml and add `<packaging>war</packaging>`.
3. Configure build.sh according to the distributed deployment documentation, and then package it
4. Put the apollo-configservice war package under tomcat
    * cp apollo-configservice/target/apollo-configservice-xxx.war ${tomcat-dir}/webapps/ROOT.war
    Run tomcat's startup.sh
5. Run tomcat's startup.sh

In addition, apollo has some tuning parameters that are recommended to be configured in tomcat's server.xml, which can be found in [application.properties](https://github.com/apolloconfig/apollo/blob/master/apollo-common/src/main/resources/application.properties#L12)

### 14. How to replace registry Eureka with zookeeper?

Many company microservice projects are already using zookeeper, if you want to replace Eureka with zookeeper for the purpose of easy service management, you can refer to the transformation steps contributed by [@hanyidreamer](https://github.com/hanyidreamer): [registry Eureka replacement for zookeeper](https://blog.csdn.net/u014732209/article/details/89555535)

### 15. How to achieve different configurations and not affect each other when multiple people are developing locally at the same time?

Refer to [#1560](https://github.com/apolloconfig/apollo/issues/1560)

### 16. How to set the relative path after Portal is mounted to nginx/slb?

In general it is recommended to use the root directory directly to mount the portal, however if there are cases where you want to share nginx/slb with other applications and need to add a relative path (e.g. /apollo), then you can configure it as follows.

#### 16.1 Portal for version 1.7.0 and above

First add the `-D` parameter `server.servlet.context-path=/apollo` or the system environment variable `SERVER_SERVLET_CONTEXT_PATH=/apollo` for apollo-portal.

Then just configure forwarding on nginx/slb, using nginx as an example.
```
location /apollo/ {
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Server $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_pass http://127.0.0.1:8070/apollo/;
}
```

#### 16.2 Portal for version 1.6.0 and above

First add the `prefix.path=/apollo` configuration parameter to the portal, the configuration is very simple, after logging in with the super administrator account, go to the `Administrator Tools - System Parameters` page and add or modify the `prefix.path` configuration item.

Then you can configure forwarding on nginx/slb, using nginx as an example.
```
location /apollo/ {
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Server $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_pass http://127.0.0.1:8070/;
}
```

### 17. How to configure https after Portal is mounted to nginx/slb?

1. configure https access configuration on nginx/slb, taking nginx as an example:

```
    server {
        listen 80 default_server;

        location / {
            # redirect all requests on port 80 to https
            return 301 https://$http_host$request_uri;
        }
    }

     server {
         # If the nginx version is lower and does not support http2, configure listen 443 ssl;
         listen 443 ssl http2;
         server_name your-domain-name;
         # ssl certificate, nginx needs to use a certificate with a complete certificate chain
         ssl_certificate /etc/nginx/ssl/xxx.crt;
         ssl_certificate_key /etc/nginx/ssl/xxx.key;

         location / {
             proxy_pass http://apollo-portal-address:8070;
             proxy_set_header x-real-ip $remote_addr;
             proxy_set_header x-forwarded-for $proxy_add_x_forwarded_for;
             #! ! ! This must be $http_host, if it is configured as $host, the port will be wrong when redirecting
             proxy_set_header host $http_host;
             proxy_set_header x-forwarded-proto $scheme;
             proxy_http_version 1.1;
         }
     }
```

2. Configure apollo-portal to parse the header information from the reverse proxy

Modify application-github.properties under the config directory in the apollo-portal installation package and add the following configuration:

```properties
server.forward-headers-strategy=framework
```

It can also be configured through environment variables:

```
SERVER_FORWARD_HEADERS_STRATEGY=framework
```
