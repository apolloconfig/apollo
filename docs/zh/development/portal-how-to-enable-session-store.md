从 1.9.0 版本开始，apollo-portal 增加了 session 共享支持，从而可以在集群部署 apollo-portal 时实现 session 共享。

## 使用方式

### 1. 基于 JDBC 的 session 共享(默认)
默认配置即为 基于 JDBC 的 session 共享  
所以清除 session 共享相关的配置并配置数据库连接即可，需要清理的配置如下  
外置配置文件(properties/yml)里面的 `spring.session.store-type` 配置项  
环境变量里面的 `SPRING_SESSION_STORE_TYPE`  
System Property 里面的 `spring.session.store-type`

数据库连接有以下几种方式设置，按照优先级从高到低分别为：
#### 1.1 System Property
```bash
-Dspring.datasource.url=xxx
-Dspring.datasource.username=xxx
-Dspring.datasource.password=xxx

```

#### 1.2 环境变量
```bash
export SPRING_DATASOURCE_URL="xxx"
export SPRING_DATASOURCE_USERNAME="xxx"
export SPRING_DATASOURCE_PASSWORD="xxx"

```

#### 1.3 外部配置文件
例如 `config/application-github.properties`
```properties
spring.datasource.url=xxx
spring.datasource.username=xxx
spring.datasource.password=xxx

```

#### 1.4 关于非 mysql 数据库初始化 session 的表
apollo 的 sql 当中的建表语句为 mysql 格式, 如果需要使用其它数据库可以参考 [spring-session](https://github.com/spring-projects/spring-session) 提供的其它建表 sql    
请根据所使用的数据库选择对应的 sql 脚本  
[db2.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-db2.sql)  
[derby.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-derby.sql)  
[h2.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-h2.sql)  
[hsqldb.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-hsqldb.sql)  
[mysql.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-mysql.sql)  
[oracle.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-oracle.sql)  
[postgresql.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-postgresql.sql)  
[sqlite.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-sqlite.sql)  
[sqlserver.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-sqlserver.sql)  
[sybase.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-sybase.sql)

### 2. 基于 Redis 的 session 共享
有以下几种方式设置，按照优先级从高到低分别为：
注：redis 也支持集群、哨兵模式，配置方式为标准的 `Spring Data Redis` 模式(以 `spring.redis` 开头的配置项)，具体方式请自行研究 `Spring Data Redis` 相关文档或咨询 `Spring Data` Group
#### 2.1 System Property
```bash
-Dspring.session.store-type=redis
-Dspring.redis.host=xxx
-Dspring.redis.port=xxx
-Dspring.redis.username=xxx
-Dspring.redis.password=xxx

```

#### 2.2 环境变量
```bash
export SPRING_SESSION_STORE_TYPE="redis"
export SPRING_REDIS_HOST="xxx"
export SPRING_REDIS_PORT="xxx"
export SPRING_REDIS_USERNAME="xxx"
export SPRING_REDIS_PASSWORD="xxx"

```

#### 2.3 外部配置文件
例如 `config/application-github.properties`
```properties
spring.session.store-type=redis
spring.redis.host=xxx
spring.redis.port=xxx
spring.redis.username=xxx
spring.redis.password=xxx

```

### 3. 不启用 session 共享
有以下几种方式设置，按照优先级从高到低分别为：
#### 3.1 System Property
```bash
-Dspring.session.store-type=none

```

#### 3.2 环境变量
```bash
export SPRING_SESSION_STORE_TYPE="none"

```

#### 3.3 外部配置文件
例如 `config/application-github.properties`
```properties
spring.session.store-type=none

```
