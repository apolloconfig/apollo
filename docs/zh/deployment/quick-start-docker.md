如果您对Docker非常熟悉，可以使用Docker的方式快速部署Apollo，从而快速的了解Apollo。如果您对Docker并不是很了解，请参考[常规方式部署Quick Start](zh/deployment/quick-start)。

另外需要说明的是，不管是Docker方式部署Quick Start还是常规方式部署的，Quick Start只是用来快速入门、了解Apollo。如果部署Apollo在公司中使用，请参考[分布式部署指南](zh/deployment/distributed-deployment-guide)。

> 由于Docker对windows的支持并不是很好，所以不建议您在windows环境下使用Docker方式部署，除非您对windows docker非常了解

## 一、 准备工作

### 1.1 安装Docker
具体步骤可以参考[Docker安装指南](https://yeasy.gitbooks.io/docker_practice/content/install/)，通过以下命令测试是否成功安装
```
docker -v
```

为了加速Docker镜像下载，建议[配置镜像加速器](https://yeasy.gitbooks.io/docker_practice/content/install/mirror.html)。

### 1.2 下载Docker Quick Start配置文件

下载[docker-compose.yml](https://github.com/apolloconfig/apollo-quick-start/blob/master/docker-compose.yml)和[sql 文件夹](https://github.com/apolloconfig/apollo-quick-start/tree/master/sql)到本地目录，如 docker-quick-start。

> 如果使用的是 arm 架构的机器，例如 mac m1，需要下载[docker-compose-arm64.yml](https://github.com/apolloconfig/apollo-quick-start/blob/master/docker-compose-arm64.yml)

```bash
- docker-quick-start
  - docker-compose.yml
  - sql
    - apolloconfigdb.sql
    - apolloportaldb.sql
```

## 二、启动Apollo配置中心

在docker-quick-start目录下执行`docker-compose up`，第一次执行会触发下载镜像等操作，需要耐心等待一些时间。

> 如果使用的是 arm 架构的机器，例如 mac m1，执行 `docker-compose -f docker-compose-arm64.yml up`

Apollo 3.0.0 及以后的 Quick Start 镜像在前台运行一个包含 Config Service、Admin Service 和 Portal 的 Java 进程，应用日志默认同时输出到控制台和 `/apollo-quick-start/apollo-service.log`。

在 `apollo-quick-start` 的日志中，看到下面三个应用上下文的 `isActive: true`，说明三个服务都已完成启动；最后的 `portalContext` 日志出现后，可以访问 http://localhost:8070。

```log
apollo-quick-start | ... [starting:config] : configContext [application-1] isActive: true
apollo-quick-start | ... [starting:admin] : adminContext [application-2] isActive: true
apollo-quick-start | ... [starting:portal] : portalContext [application-3] isActive: true
```

> 注1：数据库的端口映射为13306，所以如果希望在宿主机上访问数据库，可以通过localhost:13306，用户名是root，密码留空。

> 注2：Config Service、Admin Service 和 Portal 的文件日志统一位于 `/apollo-quick-start/apollo-service.log`，可通过 `docker exec -it apollo-quick-start bash` 登录容器查看。

## 三、使用Apollo配置中心

使用相关步骤可以参考[Quick Start - 四、使用Apollo配置中心](zh/deployment/quick-start#四、使用apollo配置中心)

需要注意的是，在Docker环境下需要通过下面的命令运行Demo客户端：
```bash
docker exec -i apollo-quick-start /apollo-quick-start/demo.sh client
```

默认情况下 apollo-configservice 只会注册内网 IP，只有通过上述命令启动的客户端能连通，如果希望外部的客户端也能访问，请参考[网络策略](zh/deployment/distributed-deployment-guide?id=_14-网络策略)。