### 1. What is Namespace?

Namespace is a collection of configuration items, similar to the concept of a configuration file.

### 2. What is an `application` Namespace?

When Apollo creates a project, it creates an `application` namespace by default. Spring Boot students know that Spring Boot projects have a default configuration file application.yml. here application.yml is equivalent to the `application` Namespace. For 90% of applications, the `application` namespace is sufficient for everyday configuration scenarios.

####  The code for client to get the `application` Namespace is as follows.

``` java
  Config config = ConfigService.getAppConfig();
```

#### The code for the client to get a `non-application` Namespace is as follows.

``` java
  Config config = ConfigService.getConfig(namespaceName);
```

### 3. What are the formats of Namespace?

Configuration files come in various formats such as properties, xml, yml, yaml, json, etc. Similarly Namespace also has these formats. In the Portal UI, you can see that the Namespace for `application` has a `properties` tag, indicating that `application` is in properties format.

>Note1: For the namespace in non-properties format, you need to call `ConfigService.getConfigFile(String namespace, ConfigFileFormat configFileFormat)` to get it when you use the client, if you use the [Http interface direct call](en/client/other-language-client-user-guide), the corresponding namespace parameter needs to be passed in the namespace name plus the suffix name, such as datasources.json.

>Note 2: apollo-client version 1.3.0 has better support for yaml/yml, which is consistent with the properties format: `Config config = ConfigService.getConfig("application.yml");`, Spring's The Spring injection is also consistent with properties.

### 4. Classification of Namespace access rights

There are two types of access permissions for Namespace.

  * private (private)
  * public (public)

The access rights here are relative to the Apollo client.

#### 4.1 Private Permissions

Namespace with private access can only be accessed by the application it belongs to. If an application tries to get the private namespace of another application, Apollo will report a `404 ` exception.

#### 4.2 Public Permissions

Namespace with public privileges can be fetched by any application.

### 5. Types of Namespace

There are three types of Namespace.

  * private type
  * public type  
  * Associated types (inherited types)

#### 5.1 Private Types

Namespace of private type has private permission. For example, the  `application` Namespace mentioned above is a private type.


#### 5.2 Public Types

##### 5.2.1 Meaning

Namespace of a public type has public privileges. A public type Namespace is equivalent to a configuration that is outside the application and identifies the public Namespace by its name, so the name of the public Namespace must be globally unique.

##### 5.2.2 Usage Scenarios

  * Department-level shared configurations
  * Configurations shared at the group level
  * Configurations shared between several projects
  * Configuration for middleware clients


#### 5.3 Association Types

##### 5.3.1 Meaning

An association type may also be called an inherited type, and the association type has private privileges. The Namespace of an association type inherits from the Namespace of a public type and is used to override certain configurations of the public Namespace. For example, the public Namespace has two configuration items 

```
k1 = v1
k2 = v2
```

Then application A has an associated type of Namespace associated with this public Namespace and overrides the configuration item k1 with a new value of v3. Then when application A actually runs, the configuration of the public Namespace is obtained as

```
k1 = v3
k2 = v2
```

##### 5.3.2 Usage Scenarios

Give a real-world usage scenario. Assume that the configuration of the RPC framework (e.g., timeout) has the following requirements.

  * Provide a company-wide default configuration that can be dynamically adjusted
  * RPC client projects can customize certain configuration items and can be dynamically adjusted

1. If you remove  `dynamically adjustable` from the above two requirements, the approach is simple. There is a configuration file in the rpc-client.jar package, and each time you modify the configuration file, you can resend a new version of the jar package. Similarly, the same is true for client-side projects.
2. If only the client project is supported, the configuration can be dynamically adjusted. The client project can configure some configuration items on Apollo  `application` Namespace. When initializing the service, just read the configuration from Apollo. The disadvantage of this is that each project has to customize some keys, which is not uniform. 3.
3. so how to support the above requirements perfectly? The answer is to use a combination of Apollo's public type Namespace and the associated type Namespace. The code in rpc-client.jar reads the configuration of the `rpc-client` Namespace. If you need to adjust the default configuration, just change the configuration of the public type `rpc-client` Namespace. If a client project wants to customize or dynamically modify some configuration items, simply associate `rpc-client` with Apollo's own project to create a Namespace of the associated type  `rpc-client`. Then you can modify the configuration items under the Namespace of the associated type `rpc-client`. One thing we need to point out here is that rpc-client.jar is running in the application container, so the configuration of the `rpc-client` Namespace that rpc-client gets is the namespace of the associated type of the application plus the namespace of the public type Namespace.

#### 5.4 Example

As shown in the following figure, there are three applications: Application A, Application B, and Application C.

 * Application A has two Namespace of private type: application and NS-Private, and one Namespace of associated type: NS-Public.
 * Application B has one Namespace of private type: application, and one Namespace of public type: NS-Public.
 * Application C has only one Namespace of type private: application

![Namespace Example](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-model-example.png)

##### 5.4.1 Application A gets the Apollo configuration

```java
  //application 
  Config appConfig = ConfigService.getAppConfig();
  appConfig.getProperty("k1", null); // k1 = v11
  appConfig.getProperty("k2", null); // k2 = v21
  
  // NS-Private
  Config privateConfig = ConfigService.getConfig("NS-Private");
  privateConfig.getProperty("k1", null); // k1 = v3
  privateConfig.getProperty("k3", null); // k3 = v4
  
  // NS-Public, overrides the case of public type configuration, k4 is overridden
  Config publicConfig = ConfigService.getConfig("NS-Public");
  publicConfig.getProperty("k4", null); // k4 = v6 cover
  publicConfig.getProperty("k6", null); // k6 = v6
  publicConfig.getProperty("k7", null); // k7 = v7
```

##### 5.4.2 Application B to get Apollo configuration

``` java
  //application
  Config appConfig = ConfigService.getAppConfig();
  appConfig.getProperty("k1", null); // k1 = v12
  appConfig.getProperty("k2", null); // k2 = null
  appConfig.getProperty("k3", null); // k3 = v32
  
  // NS-Private, since there is no NS-Private Namespace so we get the default value
  Config privateConfig = ConfigService.getConfig("NS-Private"). privateConfig.getProperty("NS-Private");
  privateConfig.getProperty("k1", "default value"); 
  
  //NS-Public
  Config publicConfig = ConfigService.getConfig("NS-Public");
  publicConfig.getProperty("k4", null); // k4 = v5
  publicConfig.getProperty("k6", null); // k6 = v6
  publicConfig.getProperty("k7", null); // k7 = v7
```

##### 5.4.3 Applying C to get Apollo configuration

``` java
  //application
  Config appConfig = ConfigService.getAppConfig();
  appConfig.getProperty("k1", null); // k1 = v12
  appConfig.getProperty("k2", null); // k2 = null
  appConfig.getProperty("k3", null); // k3 = v33
  
  // NS-Private, since there is no NS-Private Namespace so we get the default value
  Config privateConfig = ConfigService.getConfig("NS-Private"). privateConfig.getProperty("NS-Private");
  privateConfig.getProperty("k1", "default value"); 
  
  //NS-Public, the public type Namespace, which any project can get
  Config publicConfig = ConfigServi

```

##### 5.4.4 ChangeListener

As you can see in the above code example, the Client Namespace is mapped to a Config object. Listeners for namespace configuration changes are registered on the Config object.

So the Namespace code that monitors the application in application A is as follows:

```java
Config appConfig = ConfigService.getAppConfig();
appConfig.addChangeListener(new ConfigChangeListener() {
  public void onChange(ConfigChangeEvent changeEvent) {
    //do something
  }
})
```

The Namespace code for monitoring NS-Private in application A is as follows:

```java
Config privateConfig = ConfigService.getConfig("NS-Private");
privateConfig.addChangeListener(new ConfigChangeListener() {
  public void onChange(ConfigChangeEvent changeEvent) {
    //do something
  }
})
```

The Namespace code for monitoring NS-Public in application A, application B, and application C is as follows:

```java
Config publicConfig = ConfigService.getConfig("NS-Public");
publicConfig.addChangeListener(new ConfigChangeListener() {
  public void onChange(ConfigChangeEvent changeEvent) {
    //do something
  }
})
```
