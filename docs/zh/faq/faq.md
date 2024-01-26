## 1. Apollo是什么？
Apollo（阿波罗）是一款可靠的分布式配置管理中心，诞生于携程框架研发部，能够集中化管理应用不同环境、不同集群的配置，配置修改后能够实时推送到应用端，并且具备规范的权限、流程治理等特性，适用于微服务配置管理场景。

更多介绍，可以参考[Apollo配置中心介绍](zh/design/apollo-introduction)

## 2. Cluster是什么？
一个应用下不同实例的分组，比如典型的可以按照数据中心分，把A机房的应用实例分为一个集群，把B机房的应用实例分为另一个集群。

## 3. Namespace是什么？
一个应用下不同配置的分组。
请参考[Apollo核心概念之“Namespace”](zh/design/apollo-core-concept-namespace)

## 4. 我想要接入Apollo，该如何操作？
请参考[Apollo使用指南](zh/portal/apollo-user-guide)

## 5. 我的应用需要不同机房的配置不一样，Apollo是否能支持？
Apollo是支持的。请参考[Apollo使用指南](zh/portal/apollo-user-guide)中的`三、集群独立配置说明`

## 6. 我有多个应用需要使用同一份配置，Apollo是否能支持？
Apollo是支持的。请参考[Apollo使用指南](zh/portal/apollo-user-guide)中的`四、多个AppId使用同一份配置`

## 7. Apollo是否支持查看权限控制或者配置加密？
从1.1.0版本开始，apollo-portal增加了查看权限的支持，可以支持配置某个环境只允许项目成员查看私有Namespace的配置。

这里的项目成员是指：
1. 项目的管理员
2. 具备该私有Namespace在该环境下的修改或发布权限

配置方式很简单，用超级管理员账号登录后，进入`管理员工具 - 系统参数`页面新增或修改`configView.memberOnly.envs`配置项即可。

![configView.memberOnly.envs](https://user-images.githubusercontent.com/837658/46456519-c155e100-c7e1-11e8-969b-8f332379fa29.png)

配置加密可以参考[spring-boot-encrypt demo项目](https://github.com/ctripcorp/apollo-use-cases/tree/master/spring-boot-encrypt)

## 8. 如果有多个config server，打包时如何配置meta server地址？
有多台meta server可以通过nginx反向代理，通过一个域名代理多个meta server实现ha。

## 9. Apollo相比于Spring Cloud Config有什么优势？
Spring Cloud Config的精妙之处在于它的配置存储于Git，这就天然的把配置的修改、权限、版本等问题隔离在外。通过这个设计使得Spring Cloud Config整体很简单，不过也带来了一些不便之处。

下面尝试做一个简单的小结：

| 功能点           | Apollo                                     | Spring Cloud Config                             | 备注                                                                       |
|------------------|--------------------------------------------|-------------------------------------------------|----------------------------------------------------------------------------|
| 配置界面         | 一个界面管理不同环境、不同集群配置         | 无，需要通过git操作                             |                                                                            |
| 配置生效时间     | 实时                                       | 重启生效，或手动refresh生效                                        | Spring Cloud Config需要通过Git webhook，加上额外的消息队列才能支持实时生效 |
| 版本管理         | 界面上直接提供发布历史和回滚按钮           | 无，需要通过git操作                             |                                                                            |
| 灰度发布         | 支持                                  | 不支持                                          |                                                                            |
| 授权、审核、审计 | 界面上直接支持，而且支持修改、发布权限分离 | 需要通过git仓库设置，且不支持修改、发布权限分离 |                                                                            |
| 实例配置监控     | 可以方便的看到当前哪些客户端在使用哪些配置   | 不支持                                          |                                                                            |
| 配置获取性能     | 快，通过数据库访问，还有缓存支持         | 较慢，需要从git clone repository，然后从文件系统读取 |                                                                            |
| 客户端支持       | 原生支持所有Java和.Net应用，提供API支持其它语言应用，同时也支持Spring annotation获取配置  | 支持Spring应用，提供annotation获取配置          | Apollo的适用范围更广一些                      |

## 10. Apollo和Disconf相比有什么优点？

由于我们自己并非Disconf的资深用户，所以无法主观地给出评价。
不过之前Apollo技术支持群中的热心网友[@Krast](https://github.com/krast)做了一个[开源配置中心对比矩阵](https://github.com/apolloconfig/apollo/files/983064/default.pdf)，可以参考一下。