### 1. windows怎么执行build.sh?
安装[Git Bash](https://git-for-windows.github.io/)，然后运行 “./build.sh” 注意前面 “./”

### 2. 本地运行时Portal一直报Env is down. 

默认config service启动在8080端口，admin service启动在8090端口。请确认这两个端口是否被其它应用程序占用。

如果还伴有异常信息：org.springframework.web.client.HttpClientErrorException: 405 Method Not Allowed，一般是由于本地启动了`ShadowSocks`，因为`ShadowSocks`默认会占用8090端口。

1.1.0版本增加了**系统信息**页面，可以通过`管理员工具` -> `系统信息`查看当前各个环境的Meta Server以及admin service信息，有助于排查问题。

### 3. admin server 或者 config server 注册了内网IP，导致portal或者client访问不了admin server或config server

请参考[网络策略](zh/deployment/distributed-deployment-guide?id=_14-网络策略)。

### 4. Portal如何增加环境？

#### 4.1 1.6.0及以上的版本

1.6.0版本增加了自定义环境的功能，可以在不修改代码的情况增加环境

1. protaldb增加环境，参考[3.1 调整ApolloPortalDB配置](zh/deployment/distributed-deployment-guide?id=_31-调整apolloportaldb配置)
2. 为apollo-portal添加新增环境对应的meta server地址，具体参考：[2.2.1.1.2.4 配置apollo-portal的meta service信息](zh/deployment/distributed-deployment-guide?id=_221124-配置apollo-portal的meta-service信息)。apollo-client在新的环境下使用时也需要做好相应的配置，具体参考：[1.2.2 Apollo Meta Server](zh/client/java-sdk-user-guide?id=_122-apollo-meta-server)。

>注1：一套Portal可以管理多个环境，但是每个环境都需要独立部署一套Config Service、Admin Service和ApolloConfigDB，具体请参考：[2.1.2 创建ApolloConfigDB](zh/deployment/distributed-deployment-guide?id=_212-创建apolloconfigdb)，[3.2 调整ApolloConfigDB配置](zh/deployment/distributed-deployment-guide?id=_32-调整apolloconfigdb配置)，[2.2.1.1.2 配置数据库连接信息](zh/deployment/distributed-deployment-guide?id=_22112-配置数据库连接信息)

> 注2：如果是为已经运行了一段时间的Apollo配置中心增加环境，别忘了参考[2.1.2.4 从别的环境导入ApolloConfigDB的项目数据](zh/deployment/distributed-deployment-guide?id=_2124-从别的环境导入apolloconfigdb的项目数据)对新的环境做初始化

> 注3：如果自定义的环境名称为 PROD，会被强制转换为 PRO。FWS 会被强制转换为 FAT。

#### 4.2 1.5.1及之前的版本
##### 4.2.1 添加Apollo预先定义好的环境

如果需要添加的环境是Apollo预先定义的环境（DEV, FAT, UAT, PRO），需要两步操作：
1. protaldb增加环境，参考[3.1 调整ApolloPortalDB配置](zh/deployment/distributed-deployment-guide?id=_31-调整apolloportaldb配置)
2. 为apollo-portal添加新增环境对应的meta server地址，具体参考：[2.2.1.1.2.4 配置apollo-portal的meta service信息](zh/deployment/distributed-deployment-guide?id=_221124-配置apollo-portal的meta-service信息)。apollo-client在新的环境下使用时也需要做好相应的配置，具体参考：[1.2.2 Apollo Meta Server](zh/client/java-sdk-user-guide?id=_122-apollo-meta-server)。

>注1：一套Portal可以管理多个环境，但是每个环境都需要独立部署一套Config Service、Admin Service和ApolloConfigDB，具体请参考：[2.1.2 创建ApolloConfigDB](zh/deployment/distributed-deployment-guide?id=_212-创建apolloconfigdb)，[3.2 调整ApolloConfigDB配置](zh/deployment/distributed-deployment-guide?id=_32-调整apolloconfigdb配置)，[2.2.1.1.2 配置数据库连接信息](zh/deployment/distributed-deployment-guide?id=_22112-配置数据库连接信息)

> 注2：如果是为已经运行了一段时间的Apollo配置中心增加环境，别忘了参考[2.1.2.4 从别的环境导入ApolloConfigDB的项目数据](zh/deployment/distributed-deployment-guide?id=_2124-从别的环境导入apolloconfigdb的项目数据)对新的环境做初始化

##### 4.2.2 添加自定义的环境

如果需要添加的环境不是Apollo预先定义的环境，请参照如下步骤操作：

1. 假设需要添加的环境名称叫beta
2. 修改[com.ctrip.framework.apollo.core.enums.Env](https://github.com/apolloconfig/apollo/blob/master/apollo-core/src/main/java/com/ctrip/framework/apollo/core/enums/Env.java)类，在其中加入`BETA`枚举：
```java
public enum Env{
  LOCAL, DEV, BETA, FWS, FAT, UAT, LPT, PRO, TOOLS, UNKNOWN;
  ...
}
```
3. 修改[com.ctrip.framework.apollo.core.enums.EnvUtils](https://github.com/apolloconfig/apollo/blob/master/apollo-core/src/main/java/com/ctrip/framework/apollo/core/enums/EnvUtils.java)类，在其中加入`BETA`枚举的转换逻辑：
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
4. 修改[apollo-env.properties](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/resources/apollo-env.properties)，增加`beta.meta`占位符：
```properties
local.meta=http://localhost:8080
dev.meta=${dev_meta}
fat.meta=${fat_meta}
beta.meta=${beta_meta}
uat.meta=${uat_meta}
lpt.meta=${lpt_meta}
pro.meta=${pro_meta}
```
5. 修改[com.ctrip.framework.apollo.core.internals.LegacyMetaServerProvider](https://github.com/apolloconfig/apollo/blob/master/apollo-core/src/main/java/com/ctrip/framework/apollo/core/internals/LegacyMetaServerProvider.java)类，增加读取`BETA`环境的meta server地址逻辑：
```java
public class LegacyMetaServerProvider {
    ...
    domains.put(Env.BETA, getMetaServerAddress(prop, "beta_meta", "beta.meta"));
    ...
}
```
6. protaldb增加`BETA`环境，参考[3.1 调整ApolloPortalDB配置](zh/deployment/distributed-deployment-guide?id=_31-调整apolloportaldb配置)
7. 为apollo-portal添加新增环境对应的meta server地址，具体参考：[2.2.1.1.2.4 配置apollo-portal的meta service信息](zh/deployment/distributed-deployment-guide?id=_221124-配置apollo-portal的meta-service信息)。apollo-client在新的环境下使用时也需要做好相应的配置，具体参考：[1.2.2 Apollo Meta Server](zh/client/java-sdk-user-guide?id=_122-apollo-meta-server)。

>注1：一套Portal可以管理多个环境，但是每个环境都需要独立部署一套Config Service、Admin Service和ApolloConfigDB，具体请参考：[2.1.2 创建ApolloConfigDB](zh/deployment/distributed-deployment-guide?id=_212-创建apolloconfigdb)，[3.2 调整ApolloConfigDB配置](zh/deployment/distributed-deployment-guide?id=_32-调整apolloconfigdb配置)，[2.2.1.1.2 配置数据库连接信息](zh/deployment/distributed-deployment-guide?id=_22112-配置数据库连接信息)

> 注2：如果是为已经运行了一段时间的Apollo配置中心增加环境，别忘了参考[2.1.2.4 从别的环境导入ApolloConfigDB的项目数据](zh/deployment/distributed-deployment-guide?id=_2124-从别的环境导入apolloconfigdb的项目数据)对新的环境做初始化

### 5. 如何删除应用、集群、Namespace？

0.11.0版本开始Apollo管理员增加了删除应用、集群和AppNamespace的页面，建议使用该页面进行删除。

页面入口：

![delete-app-cluster-namespace-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/delete-app-cluster-namespace-entry.png)

页面详情：

![delete-app-cluster-namespace-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/delete-app-cluster-namespace-detail.png)

### 6. 客户端多块网卡造成获取IP不准，如何解决？

获取客户端网卡逻辑在1.4.0版本有所调整，所以需要根据客户端版本区分

#### 6.1 apollo-client为1.3.0及之前的版本
如果有多网卡，且都是普通网卡的话，需要在/etc/hosts里面加一条映射关系来提升权重。

格式：`ip ${hostname}`

这里的${hostname}就是你在机器上执行hostname的结果。

比如正确IP为：192.168.1.50，hostname执行结果为：jim-ubuntu-pc
那么最终在hosts文件映射的记录为：

```
192.168.1.50 jim-ubuntu-pc
```

#### 6.2 apollo-client为1.4.0及之后的版本

如果有多网卡，且都是普通网卡的话，可以通过调整它们在系统中的顺序来改变优先级，顺序在前的优先级更高。

### 7. 通过Apollo动态调整Spring Boot的Logging level

可以参考[apollo-use-cases](https://github.com/ctripcorp/apollo-use-cases)项目中的[spring-cloud-logger](https://github.com/ctripcorp/apollo-use-cases/tree/master/spring-cloud-logger)和[spring-boot-logger](https://github.com/ctripcorp/apollo-use-cases/tree/master/spring-boot-logger)代码示例。

### 8. 将Config Service和Admin Service注册到单独的Eureka Server上

Apollo默认自带了Eureka作为内部的注册中心实现，一般情况下不需要考虑为Apollo单独部署注册中心。

不过有些公司自己已经有了一套Eureka，如果希望把Apollo的Config Service和Admin Service也注册过去实现统一管理的话，可以按照如下步骤操作：

#### 1. 配置Config Service不启动内置Eureka Server

##### 1.1 1.5.0及以上版本

为apollo-configservice配置`apollo.eureka.server.enabled=false`即可，通过bootstrap.yml或-D参数等方式皆可。

##### 1.2 1.5.0之前的版本

修改[com.ctrip.framework.apollo.configservice.ConfigServiceApplication](https://github.com/apolloconfig/apollo/blob/master/apollo-configservice/src/main/java/com/ctrip/framework/apollo/configservice/ConfigServiceApplication.java)，把`@EnableEurekaServer`改为`@EnableEurekaClient`

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

#### 2. 修改ApolloConfigDB.ServerConfig表中的`eureka.service.url`，指向自己的Eureka地址

比如自己的Eureka服务地址是1.1.1.1:8761和2.2.2.2:8761，那么就将ApolloConfigDB.ServerConfig表中设置eureka.service.url为：

```
http://1.1.1.1:8761/eureka/,http://2.2.2.2:8761/eureka/
```

需要注意的是更改Eureka地址只需要改ApolloConfigDB.ServerConfig表中的`eureka.service.url`即可，不需要修改meta server地址。

> 默认情况下，meta service和config service是部署在同一个JVM进程，所以meta service的地址就是config service的地址，修改Eureka地址时不需要修改meta server地址。

### 9. Spring Boot中使用`ConditionalOnProperty`读取不到配置

`@ConditionalOnProperty`功能从0.10.0版本开始支持，具体可以参考 [Spring Boot集成方式](zh/client/java-sdk-user-guide?id=_3213-spring-boot集成方式（推荐）)

### 10. 多机房如何实现A机房的客户端就近读取A机房的config service，B机房的客户端就近读取B机房的config service？

请参考[Issue 1294](https://github.com/apolloconfig/apollo/issues/1294)，该案例中由于中美机房相距甚远，所以需要config db两地部署，如果是同城多机房的话，两个机房的config service可以连同一个config db。

### 11. apollo是否有支持HEAD请求的页面？阿里云slb配置健康检查只支持HEAD请求

apollo的每个服务都有`/health`页面的，该页面是apollo用来做健康检测的，支持各种请求方法，如GET, POST, HEAD等。

### 12. apollo如何配置查看权限?

从1.1.0版本开始，apollo-portal增加了查看权限的支持，可以支持配置某个环境只允许项目成员查看私有Namespace的配置。

这里的项目成员是指：
1. 项目的管理员
2. 具备该私有Namespace在该环境下的修改或发布权限

配置方式很简单，用超级管理员账号登录后，进入`管理员工具 - 系统参数`页面新增或修改`configView.memberOnly.envs`配置项即可。

![configView.memberOnly.envs](https://user-images.githubusercontent.com/837658/46456519-c155e100-c7e1-11e8-969b-8f332379fa29.png)

### 13. apollo如何放在独立的tomcat中跑？

有些公司的运维策略可能会要求必须使用独立的tomcat跑应用，不允许apollo默认的startup.sh方式运行，下面以apollo-configservice为例简述一下如何使apollo服务端运行在独立的tomcat中：

1. 获取apollo代码（生产部署建议用release的版本）
2. 修改apollo-configservice的pom.xml，增加`<packaging>war</packaging>`
3. 按照分布式部署文档配置build.sh，然后打包
4. 把apollo-configservice的war包放到tomcat下
    * cp apollo-configservice/target/apollo-configservice-xxx.war ${tomcat-dir}/webapps/ROOT.war
运行tomcat的startup.sh
5. 运行tomcat的startup.sh

另外，apollo还有一些调优参数建议在tomcat的server.xml中配置一下，可以参考[application.properties](https://github.com/apolloconfig/apollo/blob/master/apollo-common/src/main/resources/application.properties#L12)

### 14. 注册中心Eureka如何替换为zookeeper？

许多公司微服务项目已经在使用zookeeper，如果出于方便服务管理的目的，希望Eureka替换为zookeeper的情况，可以参考[@hanyidreamer](https://github.com/hanyidreamer)贡献的改造步骤：[注册中心Eureka替换为zookeeper](https://blog.csdn.net/u014732209/article/details/89555535)

### 15. 本地多人同时开发，如何实现配置不一样且互不影响？

参考[#1560](https://github.com/apolloconfig/apollo/issues/1560)

### 16. Portal挂载到nginx/slb后如何设置相对路径？

一般情况下建议直接使用根目录来挂载portal，不过如果有些情况希望和其它应用共用nginx/slb，需要加上相对路径（如/apollo），那么可以按照下面的方式配置。

#### 16.1 Portal为1.7.0及以上版本

首先为apollo-portal增加-D参数`server.servlet.context-path=/apollo`或系统环境变量`SERVER_SERVLET_CONTEXT_PATH=/apollo`。

然后在nginx/slb上配置转发即可，以nginx为例：
```
location /apollo/ {
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Server $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_pass http://127.0.0.1:8070/apollo/;
}
```

#### 16.2 Portal为1.6.0及以上版本

首先为portal加上`prefix.path=/apollo`配置参数，配置方式很简单，用超级管理员账号登录后，进入`管理员工具 - 系统参数`页面新增或修改`prefix.path`配置项即可。

然后在nginx/slb上配置转发即可，以nginx为例：
```
location /apollo/ {
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header X-Forwarded-Server $host;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_pass http://127.0.0.1:8070/;
}
```

### 17. Portal挂载到nginx/slb后如何配置https？

1. 在nginx/slb上配置https访问配置，以nginx为例：

```
    server {
        listen 80 default_server;

        location / {
            # 把 80 端口的请求全部都重定向到 https
            return 301 https://$http_host$request_uri;
        }
    }

    server {
        # nginx 版本较低不支持 http2 的, 则配置 listen 443 ssl;
        listen 443 ssl http2;
        server_name  your-domain-name;
        # ssl 证书, nginx 需要使用完整证书链的证书
        ssl_certificate /etc/nginx/ssl/xxx.crt;
        ssl_certificate_key /etc/nginx/ssl/xxx.key;

        location / {
            proxy_pass http://apollo-portal-address:8070;
            proxy_set_header x-real-ip $remote_addr;
            proxy_set_header x-forwarded-for $proxy_add_x_forwarded_for;
            # ！！！这里必须是 $http_host, 如果配置成 $host 会导致跳转的时候端口错误
            proxy_set_header host $http_host;
            proxy_set_header x-forwarded-proto $scheme;
            proxy_http_version 1.1;
        }
    }
```

2. 配置apollo-portal解析反向代理的header信息

修改apollo-portal安装包中config目录下的application-github.properties，增加以下配置：

```properties
server.forward-headers-strategy=framework
```

也可以通过环境变量配置：

```
SERVER_FORWARD_HEADERS_STRATEGY=framework
```
