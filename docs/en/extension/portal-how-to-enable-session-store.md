Starting with version 1.9.0, apollo-portal adds session sharing support so that session sharing can be implemented in cluster deployments of apollo-portal.

## Usage

### 1. JDBC-based session sharing (default)

The default configuration is JDBC-based session sharing.  
So clear the session sharing related configuration and configure the database connection, the configuration to be cleared is as follows  
`spring.session.store-type` configuration in the external configuration file (properties/yml)  
Environment variables inside the `SPRING_SESSION_STORE_TYPE`  
System Property inside `spring.session.store-type`

There are several ways to set the database connection, from highest to lowest priority, as follows

#### 1.1 System Property

```bash
-Dspring.datasource.url=xxx
-Dspring.datasource.username=xxx
-Dspring.datasource.password=xxx
```

#### 1.2 Environment variables

```bash
export SPRING_DATASOURCE_URL="xxx"
export SPRING_DATASOURCE_USERNAME="xxx"
export SPRING_DATASOURCE_PASSWORD="xxx"
```

#### 1.3 External configuration files

For example `config/application-github.properties`

```properties
spring.datasource.url=xxx
spring.datasource.username=xxx
spring.datasource.password=xxx
```

#### 1.4 Table on initializing session for non-mysql databases

If you need to use other databases, you can refer to the other table creation sql provided by [spring-session](https://github.com/spring-projects/spring-session)    
Please select the corresponding sql script according to the database you are using  

- [db2.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-db2.sql)  
- [derby.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-derby.sql)  
- [h2.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-h2.sql)  
- [hsqldb.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-hsqldb.sql)  
- [mysql.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-mysql.sql)  
- [oracle.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-oracle.sql)  
- [postgresql.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-postgresql.sql)  
- [sqlite.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-sqlite.sql)  
- [sqlserver.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-sqlserver.sql)  
- [sybase.sql](https://github.com/spring-projects/spring-session/blob/faee8f1bdb8822a5653a81eba838dddf224d92d6/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-sybase.sql)

### 2. Redis-based session sharing

There are several ways to set this up, from highest to lowest priority, as follows
Note: redis also supports cluster and sentry mode, configured in the standard `Spring Data Redis` mode (configuration items starting with `spring.redis`), please study the `Spring Data Redis` related documentation or consult the `Spring Data` Group for details

#### 2.1 System Property

```bash
-Dspring.session.store-type=redis
-Dspring.redis.host=xxx
-Dspring.redis.port=xxx
-Dspring.redis.username=xxx
-Dspring.redis.password=xxx
```

#### 2.2 Environment variables

```bash
export SPRING_SESSION_STORE_TYPE="redis"
export SPRING_REDIS_HOST="xxx"
export SPRING_REDIS_PORT="xxx"
export SPRING_REDIS_USERNAME="xxx"
export SPRING_REDIS_PASSWORD="xxx"
```

#### 2.3 External configuration files

For example ``config/application-github.properties`''

```properties
spring.session.store-type=redis
spring.redis.host=xxx
spring.redis.port=xxx
spring.redis.username=xxx
spring.redis.password=xxx
```

### 3. Do not enable session sharing

There are several ways to set this, in descending order of priority, as follows

#### 3.1 System Property

```bash
-Dspring.session.store-type=none
```

#### 3.2 Environment Variables

```bash
export SPRING_SESSION_STORE_TYPE="none"
```

#### 3.3 External configuration files

For example `config/application-github.properties`

```properties
spring.session.store-type=none
```
