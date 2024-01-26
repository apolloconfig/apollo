## 1. What is Apollo?
Apollo (Apollo) is a reliable distributed configuration management center, born in Ctrip framework R&D department, which can centralize and manage the configuration of different environments and clusters of applications, and can push to the application side in real time after configuration modification, and has standardized permissions, process governance and other features, suitable for microservice configuration management scenarios.

For more information, please refer to [Apollo Configuration Center Introduction](en/design/apollo-introduction)

## 2. What is Cluster?
It is a grouping of different instances of an application, for example, typically you can divide the application instances in server room A into one cluster and the application instances in server room B into another cluster according to the data center.

## 3. What is Namespace?
A grouping of different configurations under one application.
Please refer to [Apollo core concept "Namespace"](en/design/apollo-core-concept-namespace)

## 4. I want to access Apollo, how do I do it?
Please refer to [Apollo user guide](en/portal/apollo-user-guide)

## 5. My application needs different configurations for different server rooms, does Apollo support it?
Apollo is supported. Please refer to `III. Cluster independent configuration instructions` in [Apollo User Guide](en/portal/apollo-user-guide?id=iii-cluster-independent-configuration-instructions)

## 6. I have multiple applications that need to use the same configuration, can Apollo support it?
Apollo is supported. Please refer to `IV. Using the same configuration for multiple AppId` in [Apollo User Guide](en/portal/apollo-user-guide?id=iv-using-the-same-configuration-for-multiple-appid).

## 7. Does Apollo support view permission control or configuration encryption?
Starting from version 1.1.0, apollo-portal adds support for view permissions, which can support configuring an environment to allow only project members to view the configuration of a private Namespace.

The project members here are :
1. The project's administrator
2. Have the permission to modify or publish the private Namespace in that environment

The configuration is very simple, after logging in with the super administrator account, go to `Administrator Tools - System Parameters` page to add or modify `configView.memberOnly.envs` configuration items.

![configView.memberOnly.envs](https://user-images.githubusercontent.com/837658/46456519-c155e100-c7e1-11e8-969b-8f332379fa29.png)

The configuration encryption can be found in the [spring-boot-encrypt demo project](https://github.com/ctripcorp/apollo-use-cases/tree/master/spring-boot-encrypt)

## 8. If there are multiple config servers, how to configure the meta server address when packaging?
There are multiple meta servers can be achieved through nginx reverse proxy, by proxy multiple meta servers with one domain name ha.

## 9. What are the advantages of Apollo over Spring Cloud Config?
The beauty of Spring Cloud Config is that its configuration is stored in Git, which naturally isolates configuration changes, permissions, versioning, etc. This design makes Spring Cloud Config simple to use overall, but it also brings some inconveniences.

Here's a quick summary.

| Function Point                                               | Apollo                                                       | Spring Cloud Config                                          | Notes |
| ------------------------------------------------------------ | ------------------------------------------------------------ | ------------------------------------------------------------ | ----- |
| Configuration interface                                      | One interface to manage different environments, different clusters configuration | None, need to operate through git                            |       |
| Spring Cloud Config requires a Git webhook and an additional message queue to support realtime effect. |                                                              |                                                              |       |
| Versioning                                                   | Release history and rollback buttons directly on the interface | None, requires git action                                    |       |
| Grayscale releases                                           | Support                                                      | Not support                                                  |       |
| Authorization, audit, auditing                               | Support directly on the interface, and support the separation of modify and release permissions | Need to set up through the git repository, and do not support the separation of modify and release permissions |       |
| Instance configuration monitoring                            | You can easily see which clients are currently using which configurations | Not supported                                                |       |
| Configuration fetching performance                           | Fast, with database access and cache support                 | Slow, requiring clone repository from git, then read from filesystem |       |
| Net applications, provides API support for other language applications, and also supports Spring annotation to get configuration | Support for Spring applications, provides annotation to get configuration | Apollo is a little more widely available                     |       |

## 10. What are the advantages of Apollo compared to Disconf?

Since we are not experienced users of `Disconf`, we can't give a subjective evaluation.
However, an enthusiastic user in the Apollo support group [@Krast](https://github.com/krast) made an [Open Source Configuration Center Comparison Matrix](https://github.com/apolloconfig/apollo/files/983064/default.pdf), which can be consulted.

