为了让大家更快地上手了解Apollo配置中心，我们这里准备了一个Quick Start，能够在几分钟内在本地环境部署、启动Apollo配置中心。

考虑到Docker的便捷性，我们还提供了Quick Start的Docker版本，如果你对Docker比较熟悉的话，可以参考[Apollo Quick Start Docker部署](zh/deployment/quick-start-docker)通过Docker快速部署Apollo。

不过这里需要注意的是，Quick Start只针对本地测试使用，如果要部署到生产环境，还请另行参考[分布式部署指南](zh/deployment/distributed-deployment-guide)。

> 注：Quick Start需要有bash环境，Windows用户请安装[Git Bash](https://git-for-windows.github.io/)，建议使用最新版本，老版本可能会遇到未知问题。也可以直接通过IDE环境启动，详见[Apollo开发指南](zh/contribution/apollo-development-guide)。

# &nbsp;
# 一、准备工作
## 1.1 Java

* Apollo服务端：1.8+
* Apollo客户端：1.8+
   * 如需运行在 Java 1.7 运行时环境，请使用 1.x 版本的 apollo 客户端，如 1.9.1

在配置好后，可以通过如下命令检查：
```sh
java -version
```

样例输出：
```sh
java version "1.8.0_74"
Java(TM) SE Runtime Environment (build 1.8.0_74-b02)
Java HotSpot(TM) 64-Bit Server VM (build 25.74-b02, mixed mode)
```

Windows用户请确保JAVA_HOME环境变量已经设置。

## 1.2 MySQL
* 如果使用 H2 内存数据库/H2 文件数据库，则无需 MySQL，可以跳过此步骤
* 版本要求：5.6.5+

Apollo的表结构对`timestamp`使用了多个default声明，所以需要5.6.5以上版本。

连接上MySQL后，可以通过如下命令检查：
```sql
SHOW VARIABLES WHERE Variable_name = 'version';
```

| Variable_name | Value  |
|---------------|--------|
| version       | 5.7.11 |

## 1.3 下载Quick Start安装包
我们准备好了一个Quick Start安装包，大家只需要下载到本地，就可以直接使用，免去了编译、打包过程。

安装包共50M，如果访问github网速不给力的话，可以从百度网盘下载。

1. 从GitHub下载
    * checkout或下载[apollo-quick-start项目](https://github.com/apolloconfig/apollo-quick-start)
    * **由于Quick Start项目比较大，所以放在了另外的repository，请注意项目地址**
        * https://github.com/apolloconfig/apollo-quick-start
2. 从百度网盘下载
    * 通过[网盘链接](https://pan.baidu.com/s/1Ieelw6y3adECgktO0ea0Gg)下载，提取码: 9wwe
    * 下载到本地后，在本地解压apollo-quick-start.zip
3. 为啥安装包要58M这么大？
    * 因为这是一个可以自启动的jar包，里面包含了所有依赖jar包以及一个内置的tomcat容器

### 1.3.1 手动打包Quick Start安装包

Quick Start只针对本地测试使用，所以一般用户不需要自己下载源码打包，只需要下载已经打好的包即可。不过也有部分用户希望在修改代码后重新打包，那么可以参考如下步骤：

1. 修改apollo-configservice, apollo-adminservice和apollo-portal的pom.xml，注释掉spring-boot-maven-plugin和maven-assembly-plugin
2. 在根目录下执行`mvn clean package -pl apollo-assembly -am -DskipTests=true`
3. 复制apollo-assembly/target下的jar包，rename为apollo-all-in-one.jar

# 二、数据库初始化及启动
#### 注意事项
1. apollo 服务端进程需要分别使用8070, 8080, 8090端口，请确保这3个端口当前没有被使用。
2. 脚本中的 SPRING_PROFILES_ACTIVE 环境变量中的 `github` 是必须的 profile，`database-discovery` 指定使用数据库服务发现， `auth` 是 portal 提供简单认证的 profile，不需要认证或者使用其它认证方式时可以去掉

## 2.1 使用 H2 内存数据库，自动初始化
无需任何配置，直接使用如下命令启动即可
> 注：使用内存数据库时，任何操作都会在 apollo 进程重启后丢失
```bash
export SPRING_PROFILES_ACTIVE="github,database-discovery,auth"
unset SPRING_SQL_CONFIG_INIT_MODE
unset SPRING_SQL_PORTAL_INIT_MODE
java -jar apollo-all-in-one.jar

```

## 2.2 使用 H2 文件数据库，自动初始化
#### 注意事项
1. 脚本中环境变量中的路径 `~/apollo/apollo-config-db` 和 `~/apollo/apollo-portal-db` 可以替换为其它自定义路径，需要保证该路径有读写权限

### 2.2.1 首次启动
首次启动使用 SPRING_SQL_CONFIG_INIT_MODE="always" 和 SPRING_SQL_PORTAL_INIT_MODE="always" 环境变量来进行初始化
```bash
export SPRING_PROFILES_ACTIVE="github,database-discovery,auth"
# config db
export SPRING_SQL_CONFIG_INIT_MODE="always"
export SPRING_CONFIG_DATASOURCE_URL="jdbc:h2:file:~/apollo/apollo-config-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE"
# portal db
export SPRING_SQL_PORTAL_INIT_MODE="always"
export SPRING_PORTAL_DATASOURCE_URL="jdbc:h2:file:~/apollo/apollo-portal-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE"
java -jar apollo-all-in-one.jar

```

### 2.2.2 后续启动
后续启动去掉 SPRING_SQL_CONFIG_INIT_MODE 和 SPRING_SQL_PORTAL_INIT_MODE 环境变量来避免重复初始化
```bash
export SPRING_PROFILES_ACTIVE="github,database-discovery,auth"
# config db
unset SPRING_SQL_CONFIG_INIT_MODE
export SPRING_CONFIG_DATASOURCE_URL="jdbc:h2:file:~/apollo/apollo-config-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE"
# portal db
unset SPRING_SQL_PORTAL_INIT_MODE
export SPRING_PORTAL_DATASOURCE_URL="jdbc:h2:file:~/apollo/apollo-portal-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE"
java -jar apollo-all-in-one.jar

```

## 2.3 使用 mysql 数据库，自动初始化
#### 注意事项
1. 脚本环境变量中的 your-mysql-server:3306 需要替换为实际的 mysql 服务器地址和端口，ApolloConfigDB 和 ApolloPortalDB 需要替换为实际的数据库名称
2. 脚本环境变量中的 "apollo-username" 和 "apollo-password" 需要填写实际的用户名和密码

### 2.3.1 首次启动
首次启动使用 SPRING_SQL_INIT_MODE="always" 环境变量来进行初始化
```bash
export SPRING_PROFILES_ACTIVE="github,database-discovery,auth"
# config db
export SPRING_SQL_CONFIG_INIT_MODE="always"
export SPRING_CONFIG_DATASOURCE_URL="jdbc:mysql://your-mysql-server:3306/ApolloConfigDB?useUnicode=true&characterEncoding=UTF8"
export SPRING_CONFIG_DATASOURCE_USERNAME="apollo-username"
export SPRING_CONFIG_DATASOURCE_PASSWORD="apollo-password"
# portal db
export SPRING_SQL_PORTAL_INIT_MODE="always"
export SPRING_PORTAL_DATASOURCE_URL="jdbc:mysql://your-mysql-server:3306/ApolloPortalDB?useUnicode=true&characterEncoding=UTF8"
export SPRING_PORTAL_DATASOURCE_USERNAME="apollo-username"
export SPRING_PORTAL_DATASOURCE_PASSWORD="apollo-password"
java -jar apollo-all-in-one.jar

```

### 2.3.2 后续启动
后续启动去掉 SPRING_SQL_CONFIG_INIT_MODE 和 SPRING_SQL_PORTAL_INIT_MODE 环境变量来避免重复初始化
```bash
export SPRING_PROFILES_ACTIVE="github,database-discovery,auth"
# config db
unset SPRING_SQL_CONFIG_INIT_MODE
export SPRING_CONFIG_DATASOURCE_URL="jdbc:mysql://your-mysql-server:3306/ApolloConfigDB?useUnicode=true&characterEncoding=UTF8"
export SPRING_CONFIG_DATASOURCE_USERNAME="apollo-username"
export SPRING_CONFIG_DATASOURCE_PASSWORD="apollo-password"
# portal db
unset SPRING_SQL_PORTAL_INIT_MODE
export SPRING_PORTAL_DATASOURCE_URL="jdbc:mysql://your-mysql-server:3306/ApolloPortalDB?useUnicode=true&characterEncoding=UTF8"
export SPRING_PORTAL_DATASOURCE_USERNAME="apollo-username"
export SPRING_PORTAL_DATASOURCE_PASSWORD="apollo-password"
java -jar apollo-all-in-one.jar

```

## 2.4 使用 mysql 数据库，手动初始化

### 2.4.1 手动初始化  ApolloConfigDB 和 ApolloPortalDB
ApolloConfigDB 通过各种MySQL客户端导入[apolloconfigdb.sql](https://github.com/apolloconfig/apollo/blob/master/scripts/sql/profiles/mysql-default/apolloconfigdb.sql)即可。
ApolloPortalDB 通过各种MySQL客户端导入[apolloportaldb.sql](https://github.com/apolloconfig/apollo/blob/master/scripts/sql/profiles/mysql-default/apolloportaldb.sql)即可。

### 2.4.2 运行
#### 注意事项
1. 脚本环境变量中的 your-mysql-server:3306 需要替换为实际的 mysql 服务器地址和端口，ApolloConfigDB 和 ApolloPortalDB 需要替换为实际的数据库名称
2. 脚本环境变量中的 "apollo-username" 和 "apollo-password" 需要填写实际的用户名和密码

```bash
export SPRING_PROFILES_ACTIVE="github,database-discovery,auth"
# config db
unset SPRING_SQL_CONFIG_INIT_MODE
export SPRING_CONFIG_DATASOURCE_URL="jdbc:mysql://your-mysql-server:3306/ApolloConfigDB?useUnicode=true&characterEncoding=UTF8"
export SPRING_CONFIG_DATASOURCE_USERNAME="apollo-username"
export SPRING_CONFIG_DATASOURCE_PASSWORD="apollo-password"
# portal db
unset SPRING_SQL_PORTAL_INIT_MODE
export SPRING_PORTAL_DATASOURCE_URL="jdbc:mysql://your-mysql-server:3306/ApolloPortalDB?useUnicode=true&characterEncoding=UTF8"
export SPRING_PORTAL_DATASOURCE_USERNAME="apollo-username"
export SPRING_PORTAL_DATASOURCE_PASSWORD="apollo-password"
java -jar apollo-all-in-one.jar

```

# 三、注意

Quick Start只是用来帮助大家快速体验Apollo项目，具体实际使用时请参考：[分布式部署指南](zh/deployment/distributed-deployment-guide)。

另外需要注意的是Quick Start不支持增加环境，只有通过分布式部署才可以新增环境，同样请参考：[分布式部署指南](zh/deployment/distributed-deployment-guide)

# 四、使用Apollo配置中心
## 4.1 使用样例项目

### 4.1.1 初始化样例配置
1. 打开http://localhost:8070

> Quick Start集成了[Spring Security简单认证](zh/extension/portal-how-to-implement-user-login-function#实现方式一：使用apollo提供的spring-security简单认证)，更多信息可以参考[Portal 实现用户登录功能](zh/extension/portal-how-to-implement-user-login-function)

<img src="https://cdn.jsdelivr.net/gh/apolloconfig/apollo-quick-start@master/images/apollo-login.jpg" alt="登录" width="640px">

2. 输入用户名apollo，密码admin后登录

![首页](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-quick-start@master/images/apollo-sample-home.jpg)

3. 点击创建应用，输入`SampleApp`信息并提交

![创建应用](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-quick-start@master/images/apollo-create-sample-app.jpg)

4. 进入SampleApp配置界面，点击新增配置，输入`timeout`信息并提交

![创建配置](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-quick-start@master/images/apollo-create-sample-config.jpg)

5. 点击发布按钮，并填写发布信息

![配置界面](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-quick-start@master/images/sample-app-config.jpg)

![发布界面](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-quick-start@master/images/sample-app-release-detail.jpg)

### 4.1.2 运行客户端程序
我们准备了一个简单的[Demo客户端](https://github.com/apolloconfig/apollo-demo-java/blob/main/api-demo/src/main/java/com/apolloconfig/apollo/demo/api/SimpleApolloConfigDemo.java)来演示从Apollo配置中心获取配置。

程序很简单，就是用户输入一个key的名字，程序会输出这个key对应的值。

如果没找到这个key，则输出undefined。

同时，客户端还会监听配置变化事件，一旦有变化就会输出变化的配置信息。

运行`./demo.sh client`启动Demo客户端，忽略前面的调试信息，可以看到如下提示：
```sh
Apollo Config Demo. Please input key to get the value. Input quit to exit.
>
```
输入`timeout`，会看到如下信息：
```sh
> timeout
Loading key : timeout with value: 1000
```

> 如果运行客户端遇到问题，可以通过修改`client/log4j2.xml`中的level为DEBUG来查看更详细日志信息
> ```xml
> <logger name="com.ctrip.framework.apollo" additivity="false" level="trace">
>     <AppenderRef ref="Async" level="DEBUG"/>
> </logger>
> ```

### 4.1.3 修改配置并发布

回到配置界面，修改`timeout`配置项的值为2000，并发布配置。

![修改配置](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-quick-start@master/images/sample-app-modify-config.jpg)

### 4.1.4 客户端查看修改后的值
如果客户端一直在运行的话，在配置发布后就会监听到配置变化，并输出修改的配置信息：
```sh
Changes for namespace application
Change - key: timeout, oldValue: 1000, newValue: 2000, changeType: MODIFIED
```

再次输入`timeout`查看对应的值，会看到如下信息：
```sh
> timeout
Loading key : timeout with value: 2000
```

## 4.2 使用新的项目
### 4.2.1 应用接入Apollo
这部分可以参考[Java应用接入指南](zh/client/java-sdk-user-guide)

### 4.2.2 运行客户端程序
由于使用了新的项目，所以客户端需要修改appId信息。

编辑`client/META-INF/app.properties`，修改app.id为你新创建的app id。
```properties
app.id=你的appId
```
运行`./demo.sh client`启动Demo客户端即可。
