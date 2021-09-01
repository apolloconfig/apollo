# 使用方法

>  Apollo 1.7.0版本增加了基于Kubernetes原生服务发现的[Helm Chart部署模式](https://www.apolloconfig.com/#/zh/deployment/distributed-deployment-guide?id=_241-%e5%9f%ba%e4%ba%8ekubernetes%e5%8e%9f%e7%94%9f%e6%9c%8d%e5%8a%a1%e5%8f%91%e7%8e%b0)，由于不再使用内置的Eureka，所以在整体部署上有很大简化，如无特殊需求，建议使用该模式部署。

## 1. 创建数据库
具体步骤请参考 [2.1 创建数据库](https://www.apolloconfig.com/#/zh/deployment/distributed-deployment-guide?id=_21-%e5%88%9b%e5%bb%ba%e6%95%b0%e6%8d%ae%e5%ba%93)，需要注意的是 ApolloPortalDB 只需要在生产环境部署一个即可，而 ApolloConfigDB 需要在每个环境部署一套，示例假设你的 apollo 开启了 4 个环境, 即 dev、fat、uat、prod，那么就需要创建 4 个 ApolloConfigDB。

## 2. 调整部署配置

### 2.1 ApolloConfigDB 数据库连接信息

以 dev 环境为例，需要修改：

1. `apollo-env-dev/service-apollo-config-server-dev.yaml`和`apollo-env-dev/service-apollo-admin-server-dev.yaml`中`application-github.properties`的`spring.datasource.url`，`spring.datasource.username`和`spring.datasource.password`配置
2. `apollo-env-dev/service-mysql-for-apollo-dev-env.yaml`中 mysql endpoint 地址信息

### 2.2 eureka.service.url

以 dev 环境为例，默认是以 replica 为 3 做的样例配置，如果 replica 数量改变了，那么也要对应修改`apollo-env-dev/service-apollo-config-server-dev.yaml`和`apollo-env-dev/service-apollo-admin-server-dev.yaml`中`application-github.properties`的`eureka.service.url`配置。

如果该配置希望以数据库中为准，那么在 yaml 中直接删除该配置项即可。

### 2.3 ApolloPortalDB 数据库连接信息

1. 修改`service-apollo-portal-server.yaml`中`application-github.properties`的`spring.datasource.url`，`spring.datasource.username`和`spring.datasource.password`配置
2. 修改`service-apollo-portal-server.yaml`中 mysql endpoint 地址信息

### 2.4 ApolloPortal 的环境信息

1. 修改`service-apollo-portal-server.yaml`中`application-github.properties`的`apollo.portal.envs`配置
   * 如果该配置希望以数据库中为准，那么在 yaml 中直接删除该配置项即可。
2. 修改`service-apollo-portal-server.yaml`中`apollo-env.properties`的各环境 meta server 地址信息

## 3. Deploy apollo on kubernetes

示例假设 apollo 开启了 4 个环境, 即 dev、fat、uat、pro

按照 scripts/apollo-on-kubernetes/kubernetes/kubectl-apply.sh 文件的内容部署 apollo 即可。

```bash
scripts/apollo-on-kubernetes/kubernetes$ cat kubectl-apply.sh
# create namespace
kubectl create namespace sre

# dev-env
kubectl apply -f apollo-env-dev/service-mysql-for-apollo-dev-env.yaml --record && \
kubectl apply -f apollo-env-dev/service-apollo-config-server-dev.yaml --record && \
kubectl apply -f apollo-env-dev/service-apollo-admin-server-dev.yaml --record

# fat-env
kubectl apply -f apollo-env-fat/service-mysql-for-apollo-fat-env.yaml --record && \
kubectl apply -f apollo-env-fat/service-apollo-config-server-fat.yaml --record && \
kubectl apply -f apollo-env-fat/service-apollo-admin-server-fat.yaml --record

# uat-env
kubectl apply -f apollo-env-uat/service-mysql-for-apollo-uat-env.yaml --record && \
kubectl apply -f apollo-env-uat/service-apollo-config-server-uat.yaml --record && \
kubectl apply -f apollo-env-uat/service-apollo-admin-server-uat.yaml --record

# prod-env
kubectl apply -f apollo-env-prod/service-mysql-for-apollo-prod-env.yaml --record && \
kubectl apply -f apollo-env-prod/service-apollo-config-server-prod.yaml --record && \
kubectl apply -f apollo-env-prod/service-apollo-admin-server-prod.yaml --record

# portal
kubectl apply -f service-apollo-portal-server.yaml --record
```

## 4. 验证所有 pod 处于 Running 并且 READY 状态

```bash
kubectl get pod -n sre -o wide

# 示例结果
NAME                                                        READY     STATUS    RESTARTS   AGE       IP            NODE
deployment-apollo-admin-server-dev-b7bbd657-4d5jx           1/1       Running   0          2d        10.247.4.79   k8s-apollo-node-2
deployment-apollo-admin-server-dev-b7bbd657-lwz5x           1/1       Running   0          2d        10.247.8.7    k8s-apollo-node-3
deployment-apollo-admin-server-dev-b7bbd657-xs4wt           1/1       Running   0          2d        10.247.1.23   k8s-apollo-node-1
deployment-apollo-admin-server-prod-699bbd894f-j977p        1/1       Running   0          2d        10.247.4.83   k8s-apollo-node-2
deployment-apollo-admin-server-prod-699bbd894f-n9m54        1/1       Running   0          2d        10.247.8.11   k8s-apollo-node-3
deployment-apollo-admin-server-prod-699bbd894f-vs56w        1/1       Running   0          2d        10.247.1.27   k8s-apollo-node-1
deployment-apollo-admin-server-uat-7c855cd4f5-9br65   1/1       Running   0          2d        10.247.1.25   k8s-apollo-node-1
deployment-apollo-admin-server-uat-7c855cd4f5-cck5g   1/1       Running   0          2d        10.247.8.9    k8s-apollo-node-3
deployment-apollo-admin-server-uat-7c855cd4f5-x6gt4   1/1       Running   0          2d        10.247.4.81   k8s-apollo-node-2
deployment-apollo-portal-server-6d4bbc879c-bv7cn            1/1       Running   0          2d        10.247.8.12   k8s-apollo-node-3
deployment-apollo-portal-server-6d4bbc879c-c4zrb            1/1       Running   0          2d        10.247.1.28   k8s-apollo-node-1
deployment-apollo-portal-server-6d4bbc879c-qm4mn            1/1       Running   0          2d        10.247.4.84   k8s-apollo-node-2
statefulset-apollo-config-server-dev-0                      1/1       Running   0          2d        10.247.8.6    k8s-apollo-node-3
statefulset-apollo-config-server-dev-1                      1/1       Running   0          2d        10.247.4.78   k8s-apollo-node-2
statefulset-apollo-config-server-dev-2                      1/1       Running   0          2d        10.247.1.22   k8s-apollo-node-1
statefulset-apollo-config-server-prod-0                     1/1       Running   0          2d        10.247.8.10   k8s-apollo-node-3
statefulset-apollo-config-server-prod-1                     1/1       Running   0          2d        10.247.4.82   k8s-apollo-node-2
statefulset-apollo-config-server-prod-2                     1/1       Running   0          2d        10.247.1.26   k8s-apollo-node-1
statefulset-apollo-config-server-uat-0                1/1       Running   0          2d        10.247.8.8    k8s-apollo-node-3
statefulset-apollo-config-server-uat-1                1/1       Running   0          2d        10.247.4.80   k8s-apollo-node-2
statefulset-apollo-config-server-uat-2                1/1       Running   0          2d        10.247.1.24   k8s-apollo-node-1
```

### 2.4 访问 apollo service

- server 端(即 portal) <br/>
&nbsp;&nbsp;&nbsp;&nbsp;kubernetes-master-ip:30001

- client 端, 在 client 端无需再实现负载均衡 <br/>
Dev<br/>
&nbsp;&nbsp;&nbsp;&nbsp;kubernetes-master-ip:30002 <br/>
Fat <br/>
&nbsp;&nbsp;&nbsp;&nbsp;kubernetes-master-ip:30003 <br/>
Uat <br/>
&nbsp;&nbsp;&nbsp;&nbsp;kubernetes-master-ip:30004 <br/>
Prod <br/>
&nbsp;&nbsp;&nbsp;&nbsp;kubernetes-master-ip:30005 <br/>

# FAQ

## 关于 kubernetes yaml 文件
具体内容请查看 `scripts/apollo-on-kubernetes/kubernetes/service-apollo-portal-server.yaml` 注释 <br/>
其他类似。

## 关于 eureka.service.url
使用 meta-server(即 config-server) 的 pod name, config-server 务必使用 statefulset。
格式为：`http://<config server pod名>.<meta server 服务名>:<meta server端口号>/eureka/`。

以 apollo-env-dev 为例:
```bash
('eureka.service.url', 'default', 'http://statefulset-apollo-config-server-dev-0.service-apollo-meta-server-dev:8080/eureka/,http://statefulset-apollo-config-server-dev-1.service-apollo-meta-server-dev:8080/eureka/,http://statefulset-apollo-config-server-dev-2.service-apollo-meta-server-dev:8080/eureka/', 'Eureka服务Url，多个service以英文逗号分隔')
```
你可以精简 config-server pod 的 name, 示例的长名字是为了更好的阅读与理解。

### 方式一：通过Spring Boot文件 application-github.properties配置（推荐）
推荐此方式配置 `eureka.service.url`，因为可以通过ConfigMap的方式传入容器，无需再修改数据库的字段。

Admin Server的配置：
```yaml
---
# configmap for apollo-admin-server-dev
kind: ConfigMap
apiVersion: v1
metadata:
  namespace: sre
  name: configmap-apollo-admin-server-dev
data:
  application-github.properties: |
    spring.datasource.url = jdbc:mysql://service-mysql-for-apollo-dev-env-mariadb.sre:3306/DevApolloConfigDB?characterEncoding=utf8
    spring.datasource.username = root
    spring.datasource.password = test
    eureka.service.url = http://statefulset-apollo-config-server-dev-0.service-apollo-meta-server-dev:8080/eureka/,http://statefulset-apollo-config-server-dev-1.service-apollo-meta-server-dev:8080/eureka/,http://statefulset-apollo-config-server-dev-2.service-apollo-meta-server-dev:8080/eureka/

```

Config Server的配置：
```yaml
---
# configmap for apollo-config-server-dev
kind: ConfigMap
apiVersion: v1
metadata:
  namespace: sre
  name: configmap-apollo-config-server-dev
data:
  application-github.properties: |
    spring.datasource.url = jdbc:mysql://service-mysql-for-apollo-dev-env-mariadb.sre:3306/DevApolloConfigDB?characterEncoding=utf8
    spring.datasource.username = root
    spring.datasource.password = m6bCdQXa00
    eureka.service.url = http://statefulset-apollo-config-server-dev-0.service-apollo-meta-server-dev:8080/eureka/,http://statefulset-apollo-config-server-dev-1.service-apollo-meta-server-dev:8080/eureka/,http://statefulset-apollo-config-server-dev-2.service-apollo-meta-server-dev:8080/eureka/

```

### 方式二：修改数据表 ApolloConfigDB.ServerConfig
修改数据库表 ApolloConfigDB.ServerConfig的 eureka.service.url。
