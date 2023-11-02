很多同学关心Apollo的性能和可靠性，以下数据是采集携程内部生产环境单台机器的数据。监控工具是[Cat](https://github.com/dianping/cat)。

### 一、测试机器配置

#### 1.1 机器配置
4C12G

#### 1.2 JVM参数
```
-Xms6144m -Xmx6144m -Xss256k -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:NewSize=4096m -XX:MaxNewSize=4096m -XX:SurvivorRatio=8
-XX:+UseParNewGC -XX:ParallelGCThreads=4 -XX:MaxTenuringThreshold=9 -XX:+UseConcMarkSweepGC -XX:+DisableExplicitGC -XX:+UseCMSInitiatingOccupancyOnly -XX:+ScavengeBeforeFullGC -XX:+UseCMSCompactAtFullCollection -XX:+CMSParallelRemarkEnabled -XX:CMSFullGCsBeforeCompaction=9 -XX:CMSInitiatingOccupancyFraction=60 -XX:+CMSClassUnloadingEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+CMSPermGenSweepingEnabled -XX:CMSInitiatingPermOccupancyFraction=70 -XX:+ExplicitGCInvokesConcurrent -XX:+PrintGCDetails -XX:+PrintGCDateStamps -XX:+PrintGCApplicationConcurrentTime -XX:+PrintHeapAtGC -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -Duser.timezone=Asia/Shanghai -Dclient.encoding.override=UTF-8 -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom
```
#### 1.3 JVM版本
1.8.0_60

#### 1.4 Apollo版本
0.9.0

#### 1.5 单台机器客户端连接数（客户端数）
5600

#### 1.6 集群总客户端连接数（客户端数）
10W+

### 二、性能指标

#### 2.1 获取配置Http接口响应时间

QPS: 160

平均响应时间: 0.1ms

95线响应时间: 0.3ms

999线响应时间: 2.5ms

>注：config service开启了配置缓存，更多信息可以参考[分布式部署指南中的缓存配置](zh/deployment/distributed-deployment-guide#_323-config-servicecacheenabled-是否开启配置缓存)

#### 2.2 Config Server GC情况

YGC: 平均2Min一次，一次耗时300ms

OGC: 平均1H一次，一次耗时380ms

#### 2.3 CPU指标

LoadAverage：0.5

System CPU利用率：6%

Process CPU利用率：8%


