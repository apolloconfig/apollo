In order to let you get started with Apollo Configuration Center faster, we have prepared a Quick Start here, which can deploy and start Apollo Configuration Center in your local environment in a few minutes.

If you are familiar with Docker, you can refer to [Apollo Quick Start Docker Deployment](en/deployment/quick-start-docker) to deploy Apollo via Docker. Apollo Quick Start Docker.

However, it should be noted that Quick Start is only for local testing, if you want to deploy to production environment, please refer to [distributed-deployment-guide](en/deployment/distributed-deployment-guide) separately.

> Note: Quick Start requires a bash environment, Windows users please install [Git Bash](https://git-for-windows.github.io/), we recommend using the latest version, older versions may encounter unknown problems. You can also start directly through the IDE environment, see [Apollo Development Guide](en/contribution/apollo-development-guide) for details.

# &nbsp;
# I. Preparation
## 1.1 Java

* Apollo server: 1.8+
* Apollo client: 1.8+
   * For running in Java 1.7 runtime environment, please use apollo client of 1.x version, such as 1.9.1

Once configured, this can be checked with the following command.
```sh
java -version
```

Sample output.
```sh
java version "1.8.0_74"
Java(TM) SE Runtime Environment (build 1.8.0_74-b02)
Java HotSpot(TM) 64-Bit Server VM (build 25.74-b02, mixed mode)
```

Windows users please make sure that JAVA_HOME environment variable is set.

## 1.2 MySQL

* Version requirement: 5.6.5+

Apollo's table structure uses multiple default declarations for `timestamp`, so version 5.6.5+ is required.

After connecting to MySQL, you can check with the following command.
```sql
SHOW VARIABLES WHERE Variable_name = 'version';
```

| Variable_name | Value  |
| ------------- | ------ |
| version       | 5.7.11 |

## 1.3 Downloading the Quick Start installation package
We have prepared a Quick Start installation package, you just need to download it locally and you can use it directly, eliminating the need to compile and package the process.

The installation package is 50M, if you can't access github, you can download it from Baidu.com.

1. Download from GitHub
    * Checkout or download the [apollo-build-scripts project](https://github.com/apolloconfig/apollo-quick-start)
    * **Since the Quick Start project is relatively large, it is placed in a different repository, so please note the project address**
        * https://github.com/apolloconfig/apollo-quick-start
2. Download from Baidu.com
    * Downloaded via [weblink](https://pan.baidu.com/s/1Ieelw6y3adECgktO0ea0Gg), extraction code: 9wwe
    * After downloading to local, unzip apollo-quick-start.zip locally
3. why is the installation package so large as 58M?
    * Because it is a self-starting jar package, which contains all the dependent jar packages and a built-in tomcat container

### 1.3.1 Manually packaged Quick Start installation package

Quick Start is only for local testing, so generally users do not need to download the source code to package it themselves, but just download the already typed package. However, there are some users who want to repackage the package after modifying the code, then you can refer to the following steps.

1. Modify the apollo-configservice, apollo-adminservice and apollo-portal pom.xml, comment out spring-boot-maven-plugin and maven-assembly-plugin
2. Execute `mvn clean package -pl apollo-assembly -am -DskipTests=true` in the root directory.
3. Copy the jar package under apollo-assembly/target and rename it to apollo-all-in-one.jar

# II. Installation steps
## 2.1 Create the database
Apollo server side needs a total of two databases: `ApolloPortalDB` and `ApolloConfigDB`, we have prepared the database, table creation and sample data as sql files respectively, just import the database.

> Note: If you have already created Apollo database locally, please take care to backup the data. The sql file we prepared will clear the Apollo related tables.

### 2.1.1 Creating ApolloPortalDB
Just import [sql/apolloportaldb.sql](https://github.com/apolloconfig/apollo-quick-start/blob/master/sql/apolloportaldb.sql) through various MySQL clients.

The following is an example of a native MySQL client.
```sql
source /your_local_path/sql/apolloportaldb.sql
```

After the successful import, you can verify it by executing the following sql statement.
```sql
select `Id`, `AppId`, `Name` from ApolloPortalDB.App;
```

| Id   | AppId     | Name       |
| ---- | --------- | ---------- |
| 1    | SampleApp | Sample App |

### 2.1.2 Creating ApolloConfigDB
You can import [sql/apolloconfigdb.sql](https://github.com/apolloconfig/apollo-quick-start/blob/master/sql/apolloconfigdb.sql) through various MySQL clients.

The following is an example of a native MySQL client.
```sql
source /your_local_path/sql/apolloconfigdb.sql
```

After the successful import, you can verify it by executing the following sql statement.
```sql
select `NamespaceId`, `Key`, `Value`, `Comment` from ApolloConfigDB.Item;
```
| NamespaceId | Key     | Value | Comment                      |
| ----------- | ------- | ----- | ---------------------------- |
| 1           | timeout | 100   | sample timeout configuration |

## 2.2 Configuring Database Connection Information
The Apollo server needs to know how to connect to the database you created earlier, so you need to edit [demo.sh](https://github.com/apolloconfig/apollo-quick-start/blob/master/demo.sh) and modify ApolloPortalDB and ApolloConfigDB related database connection string information.

> Note: The filled in user needs to have read and write access to ApolloPortalDB and ApolloConfigDB data.

```sh
#apollo config db info
apollo_config_db_url="jdbc:mysql://localhost:3306/ApolloConfigDB?characterEncoding=utf8&serverTimezone=Asia/Shanghai"
apollo_config_db_username=username
apollo_config_db_password=password (if you don't have a password, just leave it blank)

# apollo portal db info
apollo_portal_db_url="jdbc:mysql://localhost:3306/ApolloPortalDB?characterEncoding=utf8&serverTimezone=Asia/Shanghai"
apollo_portal_db_username=username
apollo_portal_db_password=password (if you don't have a password, just leave it blank)
```

> Note: Do not modify other parts of demo.sh

# III. Start Apollo Configuration Center
## 3.1 Make sure the port is not occupied
The Quick Start script will start 3 services locally, using ports 8070, 8080, 8090 respectively, please make sure these 3 ports are not currently used.

For example, under Linux/Mac, you can check with the following command.
```sh
lsof -i:8080
```

## 3.2 Execute the startup script
```sh
./demo.sh start
```

When you see the following output, you've started successfully!
```sh
==== starting service ====
Service logging file is . /service/apollo-service.log
Started [10768]
Waiting for config service startup .......
Config service started. You may visit http://localhost:8080 for service status now!
Waiting for admin service startup....
Admin service started
==== starting portal ====
Portal logging file is . /portal/apollo-portal.log
Started [10846]
Waiting for portal startup ......
Portal started. You can visit http://localhost:8070 now!
You can visit  now!
```



## 3.3 Troubleshooting
If you encounter an exception in the startup, you can check the log files in the service and portal directories respectively to troubleshoot the problem.

> Note: During start-up of apollo-configservice, eureka registration failure message will be output in the log, e.g. `com.sun.jersey.api.client.ClientHandlerException: java.net.ConnectException: Connection refused`. Note that this is expected because apollo-configservice needs to register the service with Meta Server (itself), but since it is not up yet during the startup process itself, this error is reported. A retry action will be performed later, so it will register properly when the service is up by itself.

## 3.4 Note

Quick Start is only used to help you quickly experience Apollo project, please refer to: [distributed-deployment-guide](en/deployment/distributed-deployment-guide) for details.

It should be noted that Quick Start does not support adding environments, but only through distributed deployment, please refer to: [distributed-deployment-guide](en/deployment/distributed-deployment-guide)

# IV. Using Apollo Configuration Center
## 4.1 Using the sample project

### 4.1.1 Viewing the sample configuration
1. Open http://localhost:8070

> Quick Start integrates with [Spring Security simple authentication](en/extension/portal-how-to-implement-user-login-function?id=implementation-1-simple-authentication-using-spring-security-provided-by-apollo), for more information you can refer to [Portal implementing user login function](en/extension/portal-how-to-implement-user-login-function)

<img src="https://github.com/apolloconfig/apollo-quick-start/raw/master/images/apollo-login.png" alt="login" width="640px">

2. Enter username apollo and password admin and log in

![Home](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-build-scripts@master/images/apollo-sample-home.png)

3. Click on SampleApp to enter the configuration screen, you can see that there is currently a configuration timeout=100
![Configuration Console](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-build-scripts@master/images/sample-app-config.png)

> If prompted `system error, please retry or contact system owner`, please retry a few seconds later, because there is a refresh delay for services registered through Eureka.

### 4.1.2 Running the client application
We have prepared a simple [Demo client](https://github.com/apolloconfig/apollo-demo-java/blob/main/api-demo/src/main/java/com/apolloconfig/apollo/demo/api/SimpleApolloConfigDemo.java) to demonstrate getting configuration from Apollo Configuration Center.

The program is simple: the user enters the name of a key, and the program outputs the value corresponding to that key.

If the key is not found, undefined is output.

Also, the client listens for configuration change events and outputs the changed configuration information once there is a change.

Run `./demo.sh client` to start the demo client and ignore the previous debugging information, you can see the following prompt.
```sh
Apollo Config Demo. Please input key to get the value. Input quit to exit.
>
```
Enter ``timeout`` and you will see the following message.
```sh
> timeout
> [SimpleApolloConfigDemo] Loading key : timeout with value: 100
```

> If you encounter problems running the client, you can view more detailed logging information by changing the level in ``client/log4j2.xml`` to DEBUG
> ```xml
> <logger name="com.ctrip.framework.apollo" additivity="false" level="trace">
> <AppenderRef ref="Async" level="DEBUG"/>
> </logger>
> ```

### 4.1.3 Modify the configuration and publish

1. In the configuration screen, click the edit button for the timeout item
    ![Edit Configuration](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-build-scripts@master/images/sample-app-modify-config.png)

2. In the popup box, change the value to 200 and submit
    ![Update Configuration](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-build-scripts@master/images/sample-app-submit-config.png)

  

3. Click the Publish button and fill in the publish information
    ![Publish Configuration](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-build-scripts@master/images/sample-app-release-config.png)

![Release Info](https://cdn.jsdelivr.net/gh/apolloconfig/apollo-build-scripts@master/images/sample-app-release-detail.png)

### 4.1.4 Client view the modified value
If the client has been running, it will listen for configuration changes after the configuration is published and output the modified configuration information as follows.
```sh
[SimpleApolloConfigDemo] Changes for namespace application
[SimpleApolloConfigDemo] Change - key: timeout, oldValue: 100, newValue: 200, changeType: MODIFIED
```

Type ``timeout`` again to see the corresponding value and you will see the following message.
```sh
> timeout
> [SimpleApolloConfigDemo] Loading key : timeout with value: 200
```

## 4.2 Using the new project
### 4.2.1 App access to Apollo
This part can be found in [Java Application Access Guide](en/client/java-sdk-user-guide)

### 4.2.2 Run the client application
Since a new project is used, the client needs to modify the appId information.

Edit ``client/META-INF/app.properties`` and change `app.id` to your newly created app id.
```properties
app.id=your appId
```

```
Run `./demo.sh client` to start the demo client.
```
