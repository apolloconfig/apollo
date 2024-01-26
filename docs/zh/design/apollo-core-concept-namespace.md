### 1. 什么是Namespace?
Namespace是配置项的集合，类似于一个配置文件的概念。

### 2. 什么是“application”的Namespace？
Apollo在创建项目的时候，都会默认创建一个“application”的Namespace。顾名思义，“application”是给应用自身使用的，熟悉Spring Boot的同学都知道，Spring Boot项目都有一个默认配置文件application.yml。在这里application.yml就等同于“application”的Namespace。对于90%的应用来说，“application”的Namespace已经满足日常配置使用场景了。

#### 客户端获取“application” Namespace的代码如下：

``` java
  Config config = ConfigService.getAppConfig();
```

#### 客户端获取非“application” Namespace的代码如下：

``` java
  Config config = ConfigService.getConfig(namespaceName);
```

### 3. Namespace的格式有哪些？
配置文件有多种格式，例如：properties、xml、yml、yaml、json等。同样Namespace也具有这些格式。在Portal UI中可以看到“application”的Namespace上有一个“properties”标签，表明“application”是properties格式的。

>注1：非properties格式的namespace，在客户端使用时需要调用`ConfigService.getConfigFile(String namespace, ConfigFileFormat configFileFormat)`来获取，如果使用[Http接口直接调用](zh/client/other-language-client-user-guide#_12-通过带缓存的http接口从apollo读取配置)时，对应的namespace参数需要传入namespace的名字加上后缀名，如datasources.json。

>注2：apollo-client 1.3.0版本开始对yaml/yml做了更好的支持，使用起来和properties格式一致：`Config config = ConfigService.getConfig("application.yml");`，Spring的注入方式也和properties一致。

### 4. Namespace的获取权限分类
Namespace的获取权限分为两种：
  
  * private （私有的）
  * public （公共的）
  
这里的获取权限是相对于Apollo客户端来说的。

#### 4.1 private权限
private权限的Namespace，只能被所属的应用获取到。一个应用尝试获取其它应用private的Namespace，Apollo会报“404”异常。

#### 4.2 public权限
public权限的Namespace，能被任何应用获取。

### 5. Namespace的类型

Namespace类型有三种：

  * 私有类型
  * 公共类型  
  * 关联类型（继承类型）


#### 5.1 私有类型
私有类型的Namespace具有private权限。例如上文提到的“application” Namespace就是私有类型。


#### 5.2 公共类型

##### 5.2.1 含义
公共类型的Namespace具有public权限。公共类型的Namespace相当于游离于应用之外的配置，且通过Namespace的名称去标识公共Namespace，所以公共的Namespace的名称必须全局唯一。

##### 5.2.2 使用场景

  * 部门级别共享的配置
  * 小组级别共享的配置
  * 几个项目之间共享的配置
  * 中间件客户端的配置


#### 5.3 关联类型

##### 5.3.1 含义

关联类型又可称为继承类型，关联类型具有private权限。关联类型的Namespace继承于公共类型的Namespace，用于覆盖公共Namespace的某些配置。例如公共的Namespace有两个配置项 
```
k1 = v1
k2 = v2
```
然后应用A有一个关联类型的Namespace关联了此公共Namespace，且覆盖了配置项k1，新值为v3。那么在应用A实际运行时，获取到的公共Namespace的配置为：
```
k1 = v3
k2 = v2
```

##### 5.3.2 使用场景

举一个实际使用的场景。假设RPC框架的配置（如：timeout）有以下要求：
  * 提供一份全公司默认的配置且可动态调整
  * RPC客户端项目可以自定义某些配置项且可动态调整

1. 如果把以上两点要求去掉“动态调整”，那么做法很简单。在rpc-client.jar包里有一份配置文件，每次修改配置文件然后重新发一个版本的jar包即可。同理，客户端项目修改配置也是如此。
2. 如果只支持客户端项目可动态调整配置。客户端项目可以在Apollo “application” Namespace上配置一些配置项。在初始化service的时候，从Apollo上读取配置即可。这样做的坏处就是，每个项目都要自定义一些key，不统一。
3. 那么如何完美支持以上需求呢？答案就是结合使用Apollo的公共类型的Namespace和关联类型的Namespace。RPC团队在Apollo上维护一个叫“rpc-client”的公共Namespace，在“rpc-client” Namespace上配置默认的参数值。rpc-client.jar里的代码读取“rpc-client”Namespace的配置即可。如果需要调整默认的配置，只需要修改公共类型“rpc-client” Namespace的配置。如果客户端项目想要自定义或动态修改某些配置项，只需要在Apollo 自己项目下关联“rpc-client”，就能创建关联类型“rpc-client”的Namespace。然后在关联类型“rpc-client”的Namespace下修改配置项即可。这里有一点需要指出的，那就是rpc-client.jar是在应用容器里运行的，所以rpc-client获取到的“rpc-client” Namespace的配置是应用的关联类型的Namespace加上公共类型的Namespace。

#### 5.4 例子
如下图所示，有三个应用：应用A、应用B、应用C。

 * 应用A有两个私有类型的Namespace：application和NS-Private，以及一个关联类型的Namespace：NS-Public。
 * 应用B有一个私有类型的Namespace：application，以及一个公共类型的Namespace：NS-Public。
 * 应用C只有一个私有类型的Namespace：application
 
![Namespace例子](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-model-example.png)

##### 5.4.1 应用A获取Apollo配置
```java
  //application 
  Config appConfig = ConfigService.getAppConfig();
  appConfig.getProperty("k1", null); // k1 = v11
  appConfig.getProperty("k2", null); // k2 = v21
  
  //NS-Private
  Config privateConfig = ConfigService.getConfig("NS-Private");
  privateConfig.getProperty("k1", null); // k1 = v3
  privateConfig.getProperty("k3", null); // k3 = v4
  
  //NS-Public，覆盖公共类型配置的情况，k4被覆盖
  Config publicConfig = ConfigService.getConfig("NS-Public");
  publicConfig.getProperty("k4", null); // k4 = v6 cover
  publicConfig.getProperty("k6", null); // k6 = v6
  publicConfig.getProperty("k7", null); // k7 = v7
```
##### 5.4.2 应用B获取Apollo配置
```java
  //application
  Config appConfig = ConfigService.getAppConfig();
  appConfig.getProperty("k1", null); // k1 = v12
  appConfig.getProperty("k2", null); // k2 = null
  appConfig.getProperty("k3", null); // k3 = v32
  
  //NS-Private，由于没有NS-Private Namespace 所以获取到default value
  Config privateConfig = ConfigService.getConfig("NS-Private");
  privateConfig.getProperty("k1", "default value"); 
  
  //NS-Public
  Config publicConfig = ConfigService.getConfig("NS-Public");
  publicConfig.getProperty("k4", null); // k4 = v5
  publicConfig.getProperty("k6", null); // k6 = v6
  publicConfig.getProperty("k7", null); // k7 = v7
```

##### 5.4.3 应用C获取Apollo配置
```java
  //application
  Config appConfig = ConfigService.getAppConfig();
  appConfig.getProperty("k1", null); // k1 = v12
  appConfig.getProperty("k2", null); // k2 = null
  appConfig.getProperty("k3", null); // k3 = v33
  
  //NS-Private，由于没有NS-Private Namespace 所以获取到default value
  Config privateConfig = ConfigService.getConfig("NS-Private");
  privateConfig.getProperty("k1", "default value"); 
  
  //NS-Public，公共类型的Namespace，任何项目都可以获取到
  Config publicConfig = ConfigService.getConfig("NS-Public");
  publicConfig.getProperty("k4", null); // k4 = v5
  publicConfig.getProperty("k6", null); // k6 = v6
  publicConfig.getProperty("k7", null); // k7 = v7
```
##### 5.4.4 ChangeListener

以上代码例子中可以看到，在客户端Namespace映射成一个Config对象。Namespace配置变更的监听器是注册在Config对象上。

所以在应用A中监听application的Namespace代码如下：
```java
Config appConfig = ConfigService.getAppConfig();
appConfig.addChangeListener(new ConfigChangeListener() {
  public void onChange(ConfigChangeEvent changeEvent) {
    //do something
  }
})
```
在应用A中监听NS-Private的Namespace代码如下：
```java
Config privateConfig = ConfigService.getConfig("NS-Private");
privateConfig.addChangeListener(new ConfigChangeListener() {
  public void onChange(ConfigChangeEvent changeEvent) {
    //do something
  }
})
```
在应用A、应用B、应用C中监听NS-Public的Namespace代码如下：
```java
Config publicConfig = ConfigService.getConfig("NS-Public");
publicConfig.addChangeListener(new ConfigChangeListener() {
  public void onChange(ConfigChangeEvent changeEvent) {
    //do something
  }
})
```
 


