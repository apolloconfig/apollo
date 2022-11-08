Many people are concerned about the performance and reliability of Apollo. The following data is collected from a single machine in Ctrip's internal production environment. The monitoring tool is [Cat](https://github.com/dianping/cat).

### I. Test machine configuration

#### 1.1 Machine configuration
4C12G

#### 1.2 JVM parameters
```
-Xms6144m -Xmx6144m -Xss256k -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:NewSize=4096m -XX:MaxNewSize=4096m -XX:SurvivorRatio=8
-XX:+UseParNewGC -XX:ParallelGCThreads=4 -XX:MaxTenuringThreshold=9 -XX:+UseConcMarkSweepGC -XX:+DisableExplicitGC -XX:+ UseCMSInitiatingOccupancyOnly -XX:+ScavengeBeforeFullGC -XX:+UseCMSCompactAtFullCollection -XX:+CMSParallelRemarkEnabled -XX: CMSFullGCsBeforeCompaction=9 -XX:CMSInitiatingOccupancyFraction=60 -XX:+CMSClassUnloadingEnabled -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+ CMSPermGenSweepingEnabled -XX:CMSInitiatingPermOccupancyFraction=70 -XX:+ExplicitGCInvokesConcurrent -XX:+PrintGCDetails -XX:+ PrintGCDateStamps -XX:+PrintGCApplicationConcurrentTime -XX:+PrintHeapAtGC -XX:+HeapDumpOnOutOfMemoryError -XX:- OmitStackTraceInFastThrow -Duser.timezone=Asia/Shanghai -Dclient.encoding.override=UTF-8 -Dfile.encoding=UTF-8 -Djava.security.egd= file:/dev/./urandom
```
#### 1.3 JVM Versions
1.8.0_60

#### 1.4 Apollo version
0.9.0

#### 1.5 Number of client connections per machine (number of clients)
5600

#### 1.6 Total number of client connections (number of clients) in the cluster
10W+

### II. Performance Metrics

#### 2.1 Get configured Http interface response time

`QPS`: 160

`Average response time`: 0.1ms

`95 line response time`: 0.3ms

`999 line response time`: 2.5ms

>Note: config service has config cache enabled, for more information you can refer to [Cache Configuration in Distributed Deployment Guide](en/deployment/distributed-deployment-guide?id=_323-config-servicecacheenabled-whether-to-enable-configuration-caching)

#### 2.2 Config Server GC Status

`YGC`: average 2Min once, one time 300ms

`OGC`: average 1H once, one time consuming 380ms

#### 2.3 CPU metrics

LoadAverage: 0.5

System CPU utilization: 6%

Process CPU utilization: 8%
