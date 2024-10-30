Apollo是配置管理系统，会提供权限管理（Authorization），理论上是不负责用户登录认证功能的实现（Authentication）。

所以Apollo定义了一些SPI用来解耦，Apollo接入登录的关键就是实现这些SPI。

## 实现方式一：使用Apollo提供的Spring Security简单认证
可能很多公司并没有统一的登录认证系统，如果自己实现一套会比较麻烦。Apollo针对这种情况，从0.9.0开始提供了利用Spring Security实现的Http Basic简单认证版本。

使用步骤如下：
### 1. 安装0.9.0以上版本

>如果之前是0.8.0版本，需要导入[apolloportaldb-v080-v090.sql](https://github.com/apolloconfig/apollo/blob/master/scripts/sql/profiles/mysql-default/delta/v080-v090/apolloportaldb-v080-v090.sql)

查看ApolloPortalDB，应该已经存在`Users`表，并有一条初始记录。初始用户名是apollo，密码是admin。

|Id|Username|Password|Email|Enabled|
|-|-|-|-|-|
|1|apollo|$2a$10$7r20uS.BQ9uBpf3Baj3uQOZvMVvB1RN3PYoKE94gtz2.WAOuiiwXS|apollo@acme.com|1|

### 2. 重启Portal
如果是IDE启动的话，确保`-Dapollo_profile=github,auth`

### 3. 添加用户
超级管理员登录系统后打开`管理员工具 - 用户管理`即可添加用户。

### 4. 修改用户密码

超级管理员登录系统后打开`管理员工具 - 用户管理`，输入用户名和密码后即可修改用户密码，建议同时修改超级管理员apollo的密码。

## 实现方式二： 接入LDAP

从1.2.0版本开始，Apollo支持了ldap协议的登录，按照如下方式配置即可。

> 如果采用helm chart部署方式，建议通过配置方式实现，不用修改镜像，可以参考[启用 LDAP 支持](zh/deployment/distributed-deployment-guide#_241449-启用-ldap-支持)

### 1. OpenLDAP接入方式

#### 1.1 配置`application-ldap.yml`

解压`apollo-portal-x.x.x-github.zip`后，在`config`目录下创建`application-ldap.yml`，内容参考如下（[样例](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/resources/application-ldap-openldap-sample.yml)），相关的内容需要按照具体情况调整：

```yml
spring:
  ldap:
    base: "dc=example,dc=org"
    username: "cn=admin,dc=example,dc=org" # 配置管理员账号，用于搜索、匹配用户
    password: "password"
    search-filter: "(uid={0})"  # 用户过滤器，登录的时候用这个过滤器来搜索用户
    urls:
    - "ldap://localhost:389"

ldap:
  mapping: # 配置 ldap 属性
    object-class: "inetOrgPerson" # ldap 用户 objectClass 配置
    login-id: "uid" # ldap 用户惟一 id，用来作为登录的 id
    user-display-name: "cn" # ldap 用户名，用来作为显示名
    email: "mail" # ldap 邮箱属性
```

##### 1.1.1 基于memberOf过滤用户

OpenLDAP[开启memberOf特性](https://myanbin.github.io/post/enable-memberof-in-openldap.html)后，可以配置filter从而缩小用户搜索的范围：

```yml
spring:
  ldap:
    base: "dc=example,dc=org"
    username: "cn=admin,dc=example,dc=org" # 配置管理员账号，用于搜索、匹配用户
    password: "password"
    search-filter: "(uid={0})"  # 用户过滤器，登录的时候用这个过滤器来搜索用户
    urls:
    - "ldap://localhost:389"

ldap:
  mapping: # 配置 ldap 属性
    object-class: "inetOrgPerson" # ldap 用户 objectClass 配置
    login-id: "uid" # ldap 用户惟一 id，用来作为登录的 id
    user-display-name: "cn" # ldap 用户名，用来作为显示名
    email: "mail" # ldap 邮箱属性
  filter: # 配置过滤，目前只支持 memberOf
    memberOf: "cn=ServiceDEV,ou=DEV,dc=example,dc=org|cn=WebDEV,ou=DEV,dc=example,dc=org" # 只允许 memberOf 属性为 ServiceDEV 和 WebDEV 的用户访问
```

##### 1.1.2 基于Group过滤用户

从1.3.0版本开始支持基于Group过滤用户，从而可以控制只有特定Group的用户可以登录和使用apollo：

```yml
spring:
  ldap:
    base: "dc=example,dc=org"
    username: "cn=admin,dc=example,dc=org" # 配置管理员账号，用于搜索、匹配用户
    password: "password"
    search-filter: "(uid={0})"  # 用户过滤器，登录的时候用这个过滤器来搜索用户
    urls:
    - "ldap://localhost:389"

ldap:
  mapping: # 配置 ldap 属性
    object-class: "inetOrgPerson" # ldap 用户 objectClass 配置
    login-id: "uid" # ldap 用户惟一 id，用来作为登录的 id
    rdnKey: "uid" # ldap rdn key
    user-display-name: "cn" # ldap 用户名，用来作为显示名
    email: "mail" # ldap 邮箱属性
  group: # 启用group search，启用后只有特定group的用户可以登录apollo
    object-class: "posixGroup"  # 配置groupClassName
    group-base: "ou=group" # group search base
    group-search: "(&(cn=dev))" # group filter
    group-membership: "memberUid" # group memberShip eg. member or memberUid
```

#### 1.2 配置`startup.sh`

修改`scripts/startup.sh`，指定`spring.profiles.active`为`github,ldap`。

```bash
SERVICE_NAME=apollo-portal
## Adjust log dir if necessary
LOG_DIR=/opt/logs
## Adjust server port if necessary
SERVER_PORT=8070

export JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=github,ldap"
```

### 2. Active Directory接入方式

#### 2.1 配置`application-ldap.yml`

解压`apollo-portal-x.x.x-github.zip`后，在`config`目录下创建`application-ldap.yml`，内容参考如下（[样例](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/resources/application-ldap-activedirectory-sample.yml)），相关的内容需要按照具体情况调整：

```yml
spring:
  ldap:
    base: "dc=example,dc=com"
    username: "admin" # 配置管理员账号，用于搜索、匹配用户
    password: "password"
    search-filter: "(sAMAccountName={0})"  # 用户过滤器，登录的时候用这个过滤器来搜索用户
    urls:
    - "ldap://1.1.1.1:389"

ldap:
  mapping: # 配置 ldap 属性
    object-class: "user" # ldap 用户 objectClass 配置
    login-id: "sAMAccountName" # ldap 用户惟一 id，用来作为登录的 id
    user-display-name: "cn" # ldap 用户名，用来作为显示名
    email: "userPrincipalName" # ldap 邮箱属性
  filter: # 可选项，配置过滤，目前只支持 memberOf
    memberOf: "CN=ServiceDEV,OU=test,DC=example,DC=com|CN=WebDEV,OU=test,DC=example,DC=com" # 只允许 memberOf 属性为 ServiceDEV 和 WebDEV 的用户访问
```

#### 2.2 配置`startup.sh`

修改`scripts/startup.sh`，指定`spring.profiles.active`为`github,ldap`。

```bash
SERVICE_NAME=apollo-portal
## Adjust log dir if necessary
LOG_DIR=/opt/logs
## Adjust server port if necessary
SERVER_PORT=8070

export JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=github,ldap"
```

### 3. ApacheDS接入方式

#### 3.1 配置`application-ldap.yml`

解压`apollo-portal-x.x.x-github.zip`后，在`config`目录下创建`application-ldap.yml`，内容参考如下（[样例](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/resources/application-ldap-apacheds-sample.yml)），相关的内容需要按照具体情况调整：

```yml
spring:
  ldap:
    base: "dc=example,dc=com"
    username: "uid=admin,ou=system" # 配置管理员账号，用于搜索、匹配用户
    password: "password"
    search-filter: "(uid={0})"  # 用户过滤器，登录的时候用这个过滤器来搜索用户
    urls:
    - "ldap://localhost:10389"

ldap:
  mapping: # 配置 ldap 属性
    object-class: "inetOrgPerson" # ldap 用户 objectClass 配置
    login-id: "uid" # ldap 用户惟一 id，用来作为登录的 id
    user-display-name: "displayName" # ldap 用户名，用来作为显示名
    email: "mail" # ldap 邮箱属性
```

##### 3.1.1 基于Group过滤用户

从1.3.0版本开始支持基于Group过滤用户，从而可以控制只有特定Group的用户可以登录和使用apollo：

```yml
spring:
  ldap:
    base: "dc=example,dc=com"
    username: "uid=admin,ou=system" # 配置管理员账号，用于搜索、匹配用户
    password: "password"
    search-filter: "(uid={0})"  # 用户过滤器，登录的时候用这个过滤器来搜索用户
    urls:
    - "ldap://localhost:10389"

ldap:
  mapping: # 配置 ldap 属性
    object-class: "inetOrgPerson" # ldap 用户 objectClass 配置
    login-id: "uid" # ldap 用户惟一 id，用来作为登录的 id
    rdnKey: "cn" # ldap rdn key
    user-display-name: "displayName" # ldap 用户名，用来作为显示名
    email: "mail" # ldap 邮箱属性
  group: # 配置ldap group，启用后只有特定group的用户可以登录apollo
    object-class: "groupOfNames"  # 配置groupClassName
    group-base: "ou=group" # group search base
    group-search: "(&(cn=dev))" # group filter
    group-membership: "member" # group memberShip eg. member or memberUid
```

#### 3.2 配置`startup.sh`

修改`scripts/startup.sh`，指定`spring.profiles.active`为`github,ldap`。

```bash
SERVICE_NAME=apollo-portal
## Adjust log dir if necessary
LOG_DIR=/opt/logs
## Adjust server port if necessary
SERVER_PORT=8070

export JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=github,ldap"
```
## 实现方式三： 接入OIDC

从 1.8.0 版本开始支持 OpenID Connect 登录, 这种实现方式的前提是已经部署了 OpenID Connect 登录服务  
配置前需要准备:
* OpenID Connect 的提供者配置端点(符合 RFC 8414 标准的 issuer-uri), 需要是 **https** 的, 例如 https://host:port/auth/realms/apollo/.well-known/openid-configuration
* 在 OpenID Connect 服务里创建一个 client, idToken 的签名算法必须设置为 **RS256**, 获取 client-id 以及对应的 client-secret

### 1. 配置 `application-oidc.yml`

解压`apollo-portal-x.x.x-github.zip`后，在`config`目录下创建`application-oidc.yml`，内容参考如下（[样例](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/resources/application-oidc-sample.yml)），相关的内容需要按照具体情况调整：

#### 1.1 最小配置

```yml
server:
  # 解析反向代理请求头
  forward-headers-strategy: framework
spring:
  security:
    oauth2:
      client:
        provider:
          # provider-name 是 oidc 提供者的名称, 任意字符均可, registration 的配置需要用到这个名称
          <fill-in-the-provider-name-here>:
            # 必须是 https, oidc 的 issuer-uri
            # 例如 你的 issuer-uri 是 https://host:port/auth/realms/apollo/.well-known/openid-configuration, 那么此处只需要配置 https://host:port/auth/realms/apollo 即可, spring boot 处理的时候会加上 /.well-known/openid-configuration 的后缀
            issuer-uri: https://host:port/auth/realms/apollo
        registration:
          # registration-name 是 oidc 客户端的名称, 任意字符均可, oidc 登录必须配置一个 authorization_code 类型的 registration
          <fill-in-the-registration-name-here>:
            # oidc 登录必须配置一个 authorization_code 类型的 registration
            authorization-grant-type: authorization_code
            client-authentication-method: basic
            # client-id 是在 oidc 提供者处配置的客户端ID, 用于登录 provider
            client-id: apollo-portal
            # provider 的名称, 需要和上面配置的 provider 名称保持一致
            provider: <fill-in-the-provider-name-here>
            # openid 为 oidc 登录的必须 scope, 此处可以添加其它自定义的 scope
            scope:
              - openid
            # client-secret 是在 oidc 提供者处配置的客户端密码, 用于登录 provider
            # 从安全角度考虑更推荐使用环境变量来配置, 环境变量的命名规则为: 将配置项的 key 当中的 点(.)、横杠(-)替换为下划线(_), 然后将所有字母改为大写, spring boot 会自动处理符合此规则的环境变量
            # 例如 spring.security.oauth2.client.registration.<fill-in-the-registration-name-here>.client-secret -> SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_<FILL_IN_THE_REGISTRATION_NAME_HERE>_CLIENT_SECRET (<FILL_IN_THE_REGISTRATION_NAME_HERE> 可以替换为自定义的 oidc 客户端的名称)
            client-secret: d43c91c0-xxxx-xxxx-xxxx-xxxxxxxxxxxx

```

#### 1.2 扩展配置
* 如果 OpenID Connect 登录服务支持 client_credentials 模式, 还可以再配置一个 client_credentials 类型的 registration, 用于 apollo-portal 作为客户端请求其它被 oidc 保护的资源
* 如果 OpenID Connect 登录服务支持 jwt, 还可以配置 ${spring.security.oauth2.resourceserver.jwt.issuer-uri}, 以支持通过 jwt 访问 apollo-portal

```yml
server:
  # 解析反向代理请求头
  forward-headers-strategy: framework
spring:
  security:
    oauth2:
      client:
        provider:
          # provider-name 是 oidc 提供者的名称, 任意字符均可, registration 的配置需要用到这个名称
          <fill-in-the-provider-name-here>:
            # 必须是 https, oidc 的 issuer-uri, 和 jwt 的 issuer-uri 一致的话直接引用即可, 也可以单独设置
            issuer-uri: ${spring.security.oauth2.resourceserver.jwt.issuer-uri}
        registration:
          # registration-name 是 oidc 客户端的名称, 任意字符均可, oidc 登录必须配置一个 authorization_code 类型的 registration
          <fill-in-the-registration-name-here>:
            # oidc 登录必须配置一个 authorization_code 类型的 registration
            authorization-grant-type: authorization_code
            client-authentication-method: basic
            # client-id 是在 oidc 提供者处配置的客户端ID, 用于登录 provider
            client-id: apollo-portal
            # provider 的名称, 需要和上面配置的 provider 名称保持一致
            provider: <fill-in-the-provider-name-here>
            # openid 为 oidc 登录的必须 scope, 此处可以添加其它自定义的 scope
            scope:
              - openid
            # client-secret 是在 oidc 提供者处配置的客户端密码, 用于登录 provider
            # 从安全角度考虑更推荐使用环境变量来配置, 环境变量的命名规则为: 将配置项的 key 当中的 点(.)、横杠(-)替换为下划线(_), 然后将所有字母改为大写, spring boot 会自动处理符合此规则的环境变量
            # 例如 spring.security.oauth2.client.registration.<fill-in-the-registration-name-here>.client-secret -> SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_<FILL_IN_THE_REGISTRATION_NAME_HERE>_CLIENT_SECRET (<FILL_IN_THE_REGISTRATION_NAME_HERE> 可以替换为自定义的 oidc 客户端的名称)
            client-secret: d43c91c0-xxxx-xxxx-xxxx-xxxxxxxxxxxx
          # registration-name-client 是 oidc 客户端的名称, 任意字符均可, client_credentials 类型的 registration 为选填项, 可以不配置
          registration-name-client:
            # client_credentials 类型的 registration 为选填项, 用于 apollo-portal 作为客户端请求其它被 oidc 保护的资源, 可以不配置
            authorization-grant-type: client_credentials
            client-authentication-method: basic
            # client-id 是在 oidc 提供者处配置的客户端ID, 用于登录 provider
            client-id: apollo-portal
            # provider 的名称, 需要和上面配置的 provider 名称保持一致
            provider: <fill-in-the-provider-name-here>
            # openid 为 oidc 登录的必须 scope, 此处可以添加其它自定义的 scope
            scope:
              - openid
            # client-secret 是在 oidc 提供者处配置的客户端密码, 用于登录 provider, 多个 registration 的密码如果一致可以直接引用
            client-secret: ${spring.security.oauth2.client.registration.registration-name.client-secret}
      resourceserver:
        jwt:
          # 必须是 https, jwt 的 issuer-uri
          # 例如 你的 issuer-uri 是 https://host:port/auth/realms/apollo/.well-known/openid-configuration, 那么此处只需要配置 https://host:port/auth/realms/apollo 即可, spring boot 处理的时候会自动加上 /.well-known/openid-configuration 的后缀
          issuer-uri: https://host:port/auth/realms/apollo
```

#### 1.3 用户显示名配置

用户的显示名支持自定义配置, 在 `application-oidc.yml` 添加配置项即可

* 可以使用的 oidc 标准 claim name
  详见 https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims , 非标准个性化 claim
  name 请咨询你的 OpenID Connect 登录服务管理员
* oidc 交互式登录用户的显示名配置项为 `spring.security.oidc.user-display-name-claim-name`,
  未配置的情况下默认取 `preferred_username`, 该字段为空则尝试获取 `name`
* oidc jwt 方式登录用户的显示名配置项为 `spring.security.oidc.jwt-user-display-name-claim-name`,
  无默认值

##### 1.3.1 用户显示名配置示例

* 例如在进行 oidc 交互式登录时使用 `name` 作为显示名, 则配置如下

```yml
spring:
  security:
    oidc:
      user-display-name-claim-name: "name"

```

* 例如在进行 oidc 交互式登录时使用 `email` 作为显示名, 则配置如下

```yml
spring:
  security:
    oidc:
      user-display-name-claim-name: "email"

```

* jwt 的标准 claim name (https://tools.ietf.org/html/rfc7519#section-4) 里面没有适合作为用户显示名的字段,
  所以需要 OpenID Connect 登录服务管理员添加非标准的个性化字段
* 例如使用 oidc jwt 登录时, OpenID Connect 登录服务提供了一个名为 `user_display_name` 的个性化字段,
  你想要将这个字段作为显示名, 则配置如下

```yml
spring:
  security:
    oidc:
      jwt-user-display-name-claim-name: "user_display_name"

```

* 支持同时配置 oidc 交互式登录名 和 oidc jwt 登录名
* 例如根据登录方式不同, 进行 oidc 交互式登录时候使用 `name` 作为显示名,
  进行 oidc jwt 登录时使用 `user_display_name` 作为显示名, 则配置如下

```yml
spring:
  security:
    oidc:
      user-display-name-claim-name: "name"
      jwt-user-display-name-claim-name: "user_display_name"

```

### 2. 配置 `startup.sh`

修改`scripts/startup.sh`，指定`spring.profiles.active`为`github,oidc`。

```bash
SERVICE_NAME=apollo-portal
## Adjust log dir if necessary
LOG_DIR=/opt/logs
## Adjust server port if necessary
SERVER_PORT=8070

export JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=github,oidc"
```

### 3. 配置 apollo-portal 启用 https
#### 3.1 添加反向代理 header
这里以 nginx 为例, 将以下配置直接添加或者 include (推荐) 到 nginx 的 http 配置段内
```nginx
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
    server_name xxx;

    # ssl 证书, nginx 需要使用完整证书链的证书
    ssl_certificate /etc/nginx/ssl/xxx.crt;
    ssl_certificate_key /etc/nginx/ssl/xxx.key;
    # ... 其余 ssl 配置

    location / {
        proxy_pass http://apollo-portal-dev:8070;
        proxy_set_header x-real-ip $remote_addr;
        proxy_set_header x-forwarded-for $proxy_add_x_forwarded_for;
        # ！！！这里必须是 $http_host, 如果配置成 $host 会导致跳转的时候端口错误
        proxy_set_header host $http_host;
        proxy_set_header x-forwarded-proto $scheme;
        proxy_http_version 1.1;
    }
}

```

#### 3.2 检查 application-oidc.yml 配置
在 `application-oidc.yml` 里必须存在配置项 `server.forward-headers-strategy=framework`

```yml
server:
  # 解析反向代理请求头
  forward-headers-strategy: framework

```

#### 3.3 添加 OpenID Connect 登录服务的重定向地址白名单
出于安全考虑, 一般来说 OpenID Connect 登录服务对重定向的地址会有白名单限制, 所以需要将 apollo-portal 的 https 地址添加到白名单才能正常重定向

## 实现方式四： 接入公司的统一登录认证系统

这种实现方式的前提是公司已经有统一的登录认证系统，最常见的比如SSO、LDAP等。接入时，实现以下SPI。其中UserService和UserInfoHolder是必须要实现的。

接口说明如下：
* UserService（Required）：用户服务，用来给Portal提供用户搜索相关功能
* UserInfoHolder（Required）：获取当前登录用户信息，SSO一般都是把当前登录用户信息放在线程ThreadLocal上
* LogoutHandler（Optional）：用来实现登出功能
* SsoHeartbeatHandler（Optional）：Portal页面如果长时间不刷新，登录信息会过期。通过此接口来刷新登录信息

可以参考apollo-portal下的[com.ctrip.framework.apollo.portal.spi](https://github.com/apolloconfig/apollo/tree/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi)这个包下面的四个实现：

1. defaultimpl：默认实现，全局只有apollo一个账号
2. ctrip：ctrip实现，接入了SSO并实现用户搜索、查询接口
3. springsecurity: spring security实现，可以新增用户，修改用户密码等
4. ldap: [@pandalin](https://github.com/pandalin)和[codepiano](https://github.com/codepiano)贡献的ldap实现

实现了相关接口后，可以通过[com.ctrip.framework.apollo.portal.configuration.AuthConfiguration](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/configuration/AuthConfiguration.java)在运行时替换默认的实现。

接入SSO的思路如下：

1. SSO会提供一个jar包，需要配置一个filter
2. filter会拦截所有请求，检查是否已经登录
3. 如果没有登录，那么就会跳转到SSO的登录页面
4. 在SSO登录页面登录成功后，会跳转回apollo的页面，带上认证的信息
5. 再次进入SSO的filter，校验认证信息，把用户的信息保存下来，并且把用户凭证写入cookie或分布式session，以免下次还要重新登录
6. 进入Apollo的代码，Apollo的代码会调用UserInfoHolder.getUser获取当前登录用户

注意，以上1-5这几步都是SSO的代码，不是Apollo的代码，Apollo的代码只需要你实现第6步。

>注：运行时使用不同的实现是通过[Profiles](http://docs.spring.io/autorepo/docs/spring-boot/current/reference/html/boot-features-profiles.html)实现的，比如你自己的sso实现是在`custom` profile中的话，在打包脚本中可以指定-Dapollo_profile=github,custom。其中`github`是Apollo必须的一个profile，用于数据库的配置，`custom`是你自己实现的profile。同时需要注意在[AuthConfiguration](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/configuration/AuthConfiguration.java)中修改默认实现的条件
，从`@ConditionalOnMissingProfile({"ctrip", "auth", "ldap"})`改为`@ConditionalOnMissingProfile({"ctrip", "auth", "ldap", "custom"})`。

