Apollo is a configuration management system and will provide authority management (Authorization), theoretically it is not responsible for the implementation of user login authentication function (Authentication).

So Apollo defines some SPI's for decoupling and the key to Apollo access login is to implement these SPI's.

## Implementation 1: Simple authentication using Spring Security provided by Apollo

Apollo provides a simple authentication version of Http Basic using Spring Security since 0.9.0 for this situation.

Use the following steps.

### 1. Install 0.9.0 or above

> If the previous version is 0.8.0, you need to import [apolloportaldb-v080-v090.sql](https://github.com/apolloconfig/apollo/blob/master/scripts/sql/profiles/mysql-default/delta/v080-v090/apolloportaldb-v080-v090.sql)

Checking ApolloPortalDB, the `Users` table should already exist and have an initial record. The initial user name is apollo and the password is admin.

| Id   | Username | Password                                                     | Email           | Enabled |
| ---- | -------- | ------------------------------------------------------------ | --------------- | ------- |
| 1    | apollo   | $2a$10$7r20uS.BQ9uBpf3Baj3uQOZvMVvB1RN3PYoKE94gtz2.WAOuiiwXS | apollo@acme.com | 1       |

### 2. Reboot the Portal

Make sure `-Dapollo_profile=github,auth` if it is started by IDE

### 3. Add users

Super Administrator can add users by opening `Administrator Tools - User Management` after logging into the system.

### 4. Change user password

After logging in, open `Administrator Tools - User Management` and enter the user name and password to change the user's password, we recommend changing the password of super administrator apollo at the same time.

## Implementation 2: Access to LDAP

Starting from version 1.2.0, Apollo supports ldap protocol login, which can be configured as follows.

> If you use helm chart deployment method, it is recommended to implement it by configuration method without modifying the image, you can refer to [Enable LDAP support](en/deployment/distributed-deployment-guide?id=_241449-enable-ldap-support)

### 1. OpenLDAP access method

#### 1.1 Configure `application-ldap.yml`

After unpacking `apollo-portal-x.x.x-github.zip`, create `application-ldap.yml` in the `config` directory with the following reference ([sample](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/resources/application-ldap-openldap-sample.yml)) , the relevant content needs to be adjusted according to the specific situation: 

```yml
spring:
  ldap:
    base: "dc=example,dc=org"
    username: "cn=admin,dc=example,dc=org" # Configure administrator account for searching and matching users
    password: "password"
    search-filter: "(uid={0})" # user filter, use this filter to search for users when logging in
    urls:
    - "ldap://localhost:389"

ldap:
  mapping: # Configure the ldap attribute
    object-class: "inetOrgPerson" # ldap user objectClass configuration
    login-id: "uid" # ldap user unique id, used as the login id
    user-display-name: "cn" # ldap user name, used as display name
    email: "mail" # ldap mailbox attribute
```

##### 1.1.1 Filtering users based on memberOf

With OpenLDAP [memberOf feature enabled](https://myanbin.github.io/post/enable-memberof-in-openldap.html), the filter can be configured to narrow down the users to search for.

```yml
spring:
  ldap:
    base: "dc=example,dc=org"
    username: "cn=admin,dc=example,dc=org" # Configure admin account for searching and matching users
    password: "password"
    search-filter: "(uid={0})" # user filter, use this filter to search for users when logging in
    urls:
    - "ldap://localhost:389"

ldap:
  mapping: # Configure the ldap attribute
    object-class: "inetOrgPerson" # ldap user objectClass configuration
    login-id: "uid" # ldap user unique id, used as the login id
    user-display-name: "cn" # ldap user name, used as display name
    email: "mail" # ldap mailbox attribute
  filter: # Configuration filter, currently only memberOf is supported
    memberOf: "cn=ServiceDEV,ou=DEV,dc=example,dc=org|cn=WebDEV,ou=DEV,dc=example,dc=org" # Only allow users with memberOf attribute of ServiceDEV and WebDEV to access
```

##### 1.1.2 Filtering users based on Group

Starting with version 1.3.0, we support filtering users based on Group, which allows you to control that only users of a specific Group can log in and use apollo.

```yml
spring:
  ldap:
    base: "dc=example,dc=org"
    username: "cn=admin,dc=example,dc=org" # Configure admin account for searching and matching users
    password: "password"
    search-filter: "(uid={0})" # user filter, use this filter to search for users when logging in
    urls:
    - "ldap://localhost:389"

ldap:
  mapping: # Configure the ldap attribute
    object-class: "inetOrgPerson" # ldap user objectClass configuration
    login-id: "uid" # ldap user unique id, used as login id
    rdnKey: "uid" # ldap rdn key
    user-display-name: "cn" # ldap user name, used as display name
    email: "mail" # ldap mailbox attribute
  group: # enable group search, only users of a specific group can login to apollo after enabling
    object-class: "posixGroup" # Configure groupClassName
    group-base: "ou=group" # group search base
    group-search: "(&(cn=dev))" # group filter
    group-membership: "memberUid" # group memberShip eg. member or memberUid
```

#### 1.2 Configure `startup.sh`

Modify `scripts/startup.sh` to specify `spring.profiles.active` as `github,ldap`.

```bash
SERVICE_NAME=apollo-portal
## Adjust log dir if necessary
LOG_DIR=/opt/logs
## Adjust server port if necessary
SERVER_PORT=8070

export JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=github,ldap"
```

### 2. Active Directory access method

#### 2.1 Configure `application-ldap.yml`

After unpacking `apollo-portal-x.x.x-github.zip`, create `application-ldap.yml` in the `config` directory with the following reference ([sample](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/resources/application-ldap-activedirectory-sample.yml)) , the relevant content needs to be adapted to the specific case: 

```yml
spring:
  ldap:
    base: "dc=example,dc=com"
    username: "admin" # Configure administrator account for searching and matching users
    password: "password"
    search-filter: "(sAMAccountName={0})" # user filter, use this filter to search for users when logging in
    urls:
    - "ldap://1.1.1.1:389"

ldap:
  mapping: # Configure the ldap attribute
    object-class: "user" # ldap user objectClass configuration
    login-id: "sAMAccountName" # the unique id of the ldap user, used as the login id
    user-display-name: "cn" # ldap user name, used as display name
    email: "userPrincipalName" # ldap mailbox attribute
  filter: # optional, configure filter, currently only support memberOf
    memberOf: "CN=ServiceDEV,OU=test,DC=example,DC=com|CN=WebDEV,OU=test,DC=example,DC=com" # Only allow users with memberOf attribute of ServiceDEV and WebDEV to access
```

#### 2.2 Configure `startup.sh`

Modify ``scripts/startup.sh`` to specify ``spring.profiles.active`` as ``github,ldap``.

```bash
SERVICE_NAME=apollo-portal
## Adjust log dir if necessary
LOG_DIR=/opt/logs
## Adjust server port if necessary
SERVER_PORT=8070

export JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=github,ldap"
```

### 3. ApacheDS access method

#### 3.1 Configure `application-ldap.yml`

After unpacking `apollo-portal-x.x.x-github.zip`, create `application-ldap.yml` in the `config` directory with the following reference ([sample](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/resources/application-ldap-apacheds-sample.yml)) , the relevant content needs to be adjusted according to the specific situation: 

```yml
spring:
  ldap:
    base: "dc=example,dc=com"
    username: "uid=admin,ou=system" # Configure administrator account for searching and matching users
    password: "password"
    search-filter: "(uid={0})" # user filter, use this filter to search for users when logging in
    urls:
    - "ldap://localhost:10389"

ldap:
  mapping: # Configure the ldap attribute
    object-class: "inetOrgPerson" # ldap user objectClass configuration
    login-id: "uid" # ldap user unique id, used as the login id
    user-display-name: "displayName" # ldap user name, used as display name
    email: "mail" # ldap mailbox attribute
```

##### 3.1.1 Filtering users based on Groups

Starting with version 1.3.0, we support filtering users based on Group, which allows you to control that only users of a specific Group can log in and use apollo.

```yml
spring:
  ldap:
    base: "dc=example,dc=com"
    username: "uid=admin,ou=system" # Configure admin account for searching and matching users
    password: "password"
    search-filter: "(uid={0})" # user filter, use this filter to search for users when logging in
    urls:
    - "ldap://localhost:10389"

ldap:
  mapping: # Configure the ldap attribute
    object-class: "inetOrgPerson" # ldap user objectClass configuration
    login-id: "uid" # ldap user unique id, used as the login id
    rdnKey: "cn" # ldap rdn key
    user-display-name: "displayName" # ldap user name, used as display name
    email: "mail" # ldap mailbox attribute
  group: # Configure ldap group, only users of specific group can login apollo after enabled
    object-class: "groupOfNames" # Configure groupClassName
    group-base: "ou=group" # group search base
    group-search: "(&(cn=dev))" # group filter
    group-membership: "member" # group memberShip eg. member or memberUid
```

#### 3.2 Configuring `startup.sh`

Modify ``scripts/startup.sh`` to specify ``spring.profiles.active`` as ``github,ldap``.

```bash
SERVICE_NAME=apollo-portal
## Adjust log dir if necessary
LOG_DIR=/opt/logs
## Adjust server port if necessary
SERVER_PORT=8070

export JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=github,ldap"
```

## Implementation 3: Access to OIDC

Since version 1.8.0 OpenID Connect login is supported, this implementation requires that the OpenID Connect login service has been deployed  
Before configuration, you need to prepare:

* OpenID Connect provider configuration endpoint (RFC 8414-compliant issuer-uri), which needs to be **https**, e.g. https://host:port/auth/realms/apollo/.well-known/openid-configuration
* Create a client in the OpenID Connect service, the signature algorithm of the idToken must be set to **RS256**, get the client-id and the corresponding client-secret

### 1. Configure `application-oidc.yml`

After unpacking `apollo-portal-x.x.x-github.zip`, create `application-oidc.yml` in the `config` directory with the following contents ([sample](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/resources/application-oidc-sample.yml)) , the relevant content needs to be adapted to the specific case:

#### 1.1 Minimum configuration

```yml
server:
  # Parse reverse proxy request headers
  forward-headers-strategy: framework
spring:
  security:
    oauth2:
      client:
        provider:
          # provider-name is the name of the oidc provider, any character is fine, it is required for registration configuration
          <fill-in-the-provider-name-here>:
            # must be https, issuer-uri of oidc
            # For example, if your issuer-uri is https://host:port/auth/realms/apollo/.well-known/openid-configuration, then here you only need to configure https://host:port/auth/realms/ apollo, spring boot will process it with the /.well known/openid-configuration suffix
            issuer-uri: https://host:port/auth/realms/apollo
        registration:
          # registration-name is the name of the oidc client, any character is fine, oidc login must be configured with a registration of type authorization_code
          <fill-in-the-registration-name-here>:
            # oidc login must be configured with a registration of authorization_code type
            authorization-grant-type: authorization_code
            client-authentication-method: basic
            # client-id is the client ID configured at the oidc provider, used to log in to the provider
            client-id: apollo-portal
            # The name of the provider, which should be the same as the provider name configured above
            provider: <fill-in-the-provider-name-here>
            # openid is the required scope for oidc login, you can add other custom scopes here
            scope:
              - openid
            # client-secret is the client password configured at the oidc provider, used to log in to the provider
            # From the security point of view, it is recommended to use environment variables, which should be named as follows: dot(.), crossbar(-) The naming rule for environment variables is: replace the dot (.) and the crossbar (-) in the key of the configuration item with an underscore (_), then change all letters to uppercase, spring boot will automatically process environment variables that match this rule
            # For example, spring.security.oauth2.client.registration.<fill-in-the-registration-name-here>.client-secret -> SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_<FILL_IN_THE_REGISTRATION_NAME_HERE>_CLIENT_SECRET (<FILL_IN_THE_REGISTRATION_NAME_HERE> can be replaced with the name of a custom oidc client)
            client-secret: d43c91c0-xxxx-xxxx-xxxx-xxxxxxxxxxxx

```

#### 1.2 Extended Configuration

* If the OpenID Connect login service supports client_credentials mode, you can also configure a registration of type client_credentials, which can be used by apollo-portal as a client to request other resources protected by oidc
* If the OpenID Connect login service supports jwt, you can also configure ${spring.security.oauth2.resourceserver.jwt.issuer-uri} to support accessing apollo-portal via jwt

```yml
server:
  # Parse reverse proxy request headers
  forward-headers-strategy: framework
spring:
  security:
    oauth2:
      client:
        provider:
          # provider-name is the name of the oidc provider, any character is fine, it is required for registration configuration
          <fill-in-the-provider-name-here>:
            # must be the issuer-uri of https, oidc, and jwt if it is the same as the issuer-uri of jwt, or you can set it separately
            issuer-uri: ${spring.security.oauth2.resourceserver.jwt.issuer-uri}
        registration:
          # registration-name is the name of the oidc client, any character is fine, oidc login must be configured with a registration of type authorization_code
          <fill-in-the-registration-name-here>:
            # oidc login must be configured with a registration of authorization_code type
            authorization-grant-type: authorization_code
            client-authentication-method: basic
            # client-id is the client ID configured at the oidc provider, used to log in to the provider
            client-id: apollo-portal
            # The name of the provider, which should be the same as the provider name configured above
            provider: <fill-in-the-provider-name-here>
            # openid is the required scope for oidc login, you can add other custom scopes here
            scope:
              - openid
            # client-secret is the client password configured at the oidc provider, used to log in to the provider
            # From the security point of view, it is recommended to use environment variables, which should be named as follows: dot(.), crossbar(-) The naming rule for environment variables is: replace the dot (.) and the crossbar (-) in the key of the configuration item with an underscore (_), then change all letters to uppercase, spring boot will automatically process environment variables that match this rule
            # For example, spring.security.oauth2.client.registration.<fill-in-the-registration-name-here>.client-secret -> SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_<FILL_IN_THE_REGISTRATION_NAME_HERE>_CLIENT_SECRET (<FILL_IN_THE_REGISTRATION_NAME_HERE> can be replaced with the name of a custom oidc client)
            client-secret: d43c91c0-xxxx-xxxx-xxxx-xxxxxxxxxxxx
          # registration-name-client is the name of the oidc client, any character is allowed, registration of client_credentials type is optional, can not be configured
          registration-name-client:
            # registration of client_credentials type is optional, used by apollo-portal as a client to request other oidc-protected resources, may not be configured
            authorization-grant-type: client_credentials
            client-authentication-method: basic
            # client-id is the client ID configured at the oidc provider, used to log in to the provider
            client-id: apollo-portal
            # The name of the provider, which must be the same as the provider name configured above
            provider: <fill-in-the-provider-name-here>
            # openid is the required scope for oidc login, you can add other custom scopes here
            scope:
              - openid
            # client-secret is the client password configured at the oidc provider, used to log in to the provider, multiple registrations can be referenced if the password is the same
            client-secret: ${spring.security.oauth2.client.registration.registration-name.client-secret}
      resourceserver:
        jwt:
          # must be issuer-uri for https, jwt
          # For example, if your issuer-uri is https://host:port/auth/realms/apollo/.well-known/openid-configuration, then here you only need to configure https://host:port/auth/realms/ apollo, spring boot will automatically add the /.well known/openid-configuration suffix when processing
          issuer-uri: https://host:port/auth/realms/apollo
```

#### 1.3 Configure user display name

you can also configure a custom user display name in the `application-oidc.yml`

* see https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims for standard oidc claim name,
  and see your OpenID Connect service manager or ISP for nonstandard claim name.
* the configuration property name for oidc (interactive) user display name is `spring.security.oidc.user-display-name-claim-name`,
  default `preferred_username`, and fallback to `name` if the claim value of `preferred_username` is blank
* the configuration property name for oidc jwt user display name is `spring.security.oidc.jwt-user-display-name-claim-name`,
  has no default.

##### 1.3.1 Example of user display name configure

* for example, using `name` as the claim of oidc (interactive) user display name.

```yml
spring:
  security:
    oidc:
      user-display-name-claim-name: "name"

```

* for example, using `email` as the claim of oidc (interactive) user display name.

```yml
spring:
  security:
    oidc:
      user-display-name-claim-name: "email"

```

* There is no claim name suitable for user display name in jwt standard claim name (https://tools.ietf.org/html/rfc7519#section-4),
  see your OpenID Connect service manager or ISP for a nonstandard claim name for display.
* for example, using `user_display_name` as the claim of oidc jwt user display name.

```yml
spring:
  security:
    oidc:
      jwt-user-display-name-claim-name: "user_display_name"

```

* it's ok to configure oidc (interactive) user display name and oidc jwt user display name at the same time.
* for example, using `name` as the claim of oidc (interactive) user display name
  and using `user_display_name` as the claim of oidc jwt user display name.

```yml
spring:
  security:
    oidc:
      user-display-name-claim-name: "name"
      jwt-user-display-name-claim-name: "user_display_name"

```

### 2. Configure `startup.sh`

Modify ``scripts/startup.sh`` to specify ``spring.profiles.active`` as ``github,oidc``.

```bash
SERVICE_NAME=apollo-portal
## Adjust log dir if necessary
LOG_DIR=/opt/logs
## Adjust server port if necessary
SERVER_PORT=8070

export JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=github,oidc"
```

### 3. Configure apollo-portal to enable https

#### 3.1 Adding a reverse proxy header

Using nginx as an example, add or include (recommended) the following configuration directly to the http configuration section of nginx

```nginx
server {
    listen 80 default_server;

    location / {
        # redirect all requests on port 80 to https
        return 301 https://$http_host$request_uri;
    }
}
server {
    # For lower versions of nginx that do not support http2, configure listen 443 ssl;
    listen 443 ssl http2;
    server_name xxx;

    # ssl certificate, nginx needs to use a full certificate chain certificate
    ssl_certificate /etc/nginx/ssl/xxx.crt;
    ssl_certificate_key /etc/nginx/ssl/xxx.key;
    # ... The rest of the ssl configuration

    location / {
        proxy_pass http://apollo-portal-dev:8070;
        proxy_set_header x-real-ip $remote_addr;
        proxy_set_header x-forwarded-for $proxy_add_x_forwarded_for;
        # !!! Here must be $http_host, if configured as $host, it will cause the port error when jumping
        proxy_set_header host $http_host;
        proxy_set_header x-forwarded-proto $scheme;
        proxy_http_version 1.1;
    }
}

```

#### 3.2 Checking the application-oidc.yml configuration

The configuration item ``server.forward-headers-strategy=framework`` must exist in ``application-oidc.yml``.

```yml
server:
  # Parse reverse proxy request headers
  forward-headers-strategy: framework

```

#### 3.3 Adding a whitelist of redirects for the OpenID Connect login service

For security reasons, the OpenID Connect login service generally has a whitelist of redirected addresses, so you need to add the apollo-portal https address to the whitelist in order to redirect properly.

## Implementation 4: access to the company's unified login authentication system

This approach assumes that the company already has a unified login authentication system, such as SSO and LDAP. The following SPI must be implemented to access the system, including UserService and UserInfoHolder.

The interfaces are described as follows.

* UserService (Required): User service, used to provide user search-related functions to the Portal
* UserInfoHolder (Required): Get the current login user information, SSO is generally the current login user information on ThreadLocal
* LogoutHandler (Optional): used to implement the logout function
* SsoHeartbeatHandler (Optional): If the Portal page is not refreshed for a long time, the login information will expire. Refresh the login information through this interface

You can refer to `apollo-portal` module's package of [com.ctrip.framework.apollo.portal.spi](https://github.com/apolloconfig/apollo/tree/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi) , under this package for four implementations.

1. defaultimpl: default implementation, with only one global apollo account
2. ctrip: ctrip implementation, access to the SSO and the implementation of the user search, query interface
3. springsecurity: spring security implementation, you can add new users, modify the user password, etc.
4. ldap: ldap implementation contributed by [@pandalin](https://github.com/pandalin) and [codepiano](https://github.com/codepiano)

After implementing the relevant interfaces, the AuthConfiguration can be accessed via [com.ctrip.framework.apollo.portal.configuration](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/configuration/AuthConfiguration.java) to replace the default implementation at runtime.

The idea of accessing SSO is as follows.

1. SSO will provide a jar package, you need to configure a filter
2. filter will intercept all requests and check if you are already logged in
3. if there is no login, then it will jump to the SSO login page
4. After successful login in the SSO login page, it will jump back to the apollo page with the authentication information. 5.
5. enter the SSO filter again, verify the authentication information, save the user's information, and write the user's credentials to a cookie or distributed session to avoid having to log in again next time
6. enter Apollo's code, Apollo's code will call UserInfoHolder.getUser to get the current logged-in user

Note that the above steps 1-5 are all SSO code, not Apollo code, Apollo code only requires you to implement step 6.

>Note: The runtime use of different implementations is achieved by [Profiles](http://docs.spring.io/autorepo/docs/spring-boot/current/reference/html/boot-features-profiles.html). For example, if your own sso implementation is in the `custom` profile, you can specify -Dapollo_profile=github,custom in the packaging script. profile. Also note that in [AuthConfiguration](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/configuration/AuthConfiguration.java) to modify the default implementation of the condition
>, from `@ConditionalOnMissingProfile({"ctrip", "auth", "ldap"})` to `@ConditionalOnMissingProfile({"ctrip", "auth", "ldap", "custom"})`.
