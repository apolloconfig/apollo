# &nbsp;



# 一、介绍

根据不同的场景，apolloconfig部署的架构会有很多种，这里不讨论细节，仅从部署架构的宏观角度，来介绍各种部署的方案

## 1.1 flowchart

用flowchart来表达部署方式，这里先介绍一些基本的概念

### 1.1.1 依赖关系

依赖关系用

```mermaid
graph LR
	1 --> 2
```

表示1依赖2，也就是2必须存在，1才可以正常工作，例如

```mermaid
flowchart LR
	应用 --> MySQL
```

表示应用需要使用MySQL才可以正常工作

依赖关系可能会比较复杂，以及存在多层级的依赖，例如

```mermaid
flowchart LR
	服务A --> 注册中心
	服务A --> 服务B --> MySQL
	服务A --> Redis
```

服务A需要注册中心，服务B，Redis

并且服务B需要MySQL

### 1.1.2 包含关系

包含关系用

```mermaid
graph
	subgraph a
		b
	end
```

表示a包含b，也就是b是a的一部分，包含关系可能会出现嵌套的情况，例如

```mermaid
flowchart LR
	subgraph Linux-Server
		subgraph JVM1
			Thread1.1
			Thread1.2
		end
		subgraph JVM2
			Thread2.1
		end
		MySQL
		Redis
	end
```

表示在一台Linux服务器上，运行着MySQL，Redis，2个JVM，JVM里分别又存在Thread

# 二、单机

单机部署的场景通常是新手学习，或者公司内部对性能要求不高的测试环境，不适用于生产环境

## 2.1 单机，单环境 All In One

这是最简单，部署起来最方便的单机部署方式

需要：

* 1台Linux服务器：有JRE
* 2个database：1个PortalDB和ConfigDB

如下图，所有模块部署在同一台Linux机器上，总共有3个JVM进程

```mermaid
flowchart LR
	m[Meta Server]
	e[Eureka]
	c[Config Service]
	a[Admin Service]
	p[Portal]
	
	configdb[(ConfigDB)]
	portaldb[(PortalDB)]
	
	subgraph Linux Server
		subgraph JVM8080
			m
			e
			c
		end
		subgraph JVM8090
			a
		end
		subgraph JVM8070
			p
		end
	end
	
	c --> configdb
	a --> configdb
	p --> portaldb
```

JVM8080：对外暴露的网络端口是8080，里面有Meta Server，Eureka，Config Service，其中Config Service又使用了ConfigDB

JVM8090：对外暴露的网络端口是8090，里面有Admin Service，并且Admin Service使用了ConfigDB

JVM8070：对外暴露的网络端口是8070，里面有Portal，并且Portal使用了PortalDB

如果加入模块之间的依赖，flowchart会变成

```mermaid
flowchart LR
	m[Meta Server]
	e[Eureka]
	c[Config Service]
	a[Admin Service]
	p[Portal]
	
	configdb[(ConfigDB)]
	portaldb[(PortalDB)]
	
	subgraph Linux Server
		subgraph JVM8080
			m
			e
			c
		end
		subgraph JVM8090
			a
		end
		subgraph JVM8070
			p
		end
	end
	
	c --> configdb
	a --> configdb
	p --> portaldb

    m --> e
    c --> e
    a --> e
    
	p --> m
	p --> a
```

Config Service和Admin Service会把自己注册到Eureka上

Portal通过Meta Server服务发现Admin Service

为了flowchart看起来更加简洁，可以只表示进程之间的依赖关系

```mermaid
flowchart LR
	m[Meta Server]
	e[Eureka]
	c[Config Service]
	a[Admin Service]
	p[Portal]
	
	configdb[(ConfigDB)]
	portaldb[(PortalDB)]
	
	subgraph Linux Server
		subgraph JVM8080
			m
			e
			c
		end
		subgraph JVM8090
			a
		end
		subgraph JVM8070
			p
		end
	end
	
	JVM8080 --> configdb
	JVM8090 --> configdb
	JVM8070 --> portaldb
    
	JVM8090 --> JVM8080
	JVM8070 --> JVM8090
```

进程JVM8070依赖进程JVM8090和PortalDB

进程JVM8090依赖进程JVM8080和ConfigDB

进程JVM8080依赖ConfigDB

## 2.2 单机，单环境 分开部署

### 2.2.1 单机，单环境 分开部署 3台Linux服务器

3个JVM进程也可以分散到3台Linux机器上

需要：

* 3台Linux服务器：分别部署3个进程
* 2个database

```mermaid
flowchart LR
	m[Meta Server]
	e[Eureka]
	c[Config Service]
	a[Admin Service]
	p[Portal]
	
	configdb[(ConfigDB)]
	portaldb[(PortalDB)]

	subgraph Linux Server 1
		subgraph JVM8080
			m
			e
			c
		end
	end
	
	subgraph Linux Server 2
		subgraph JVM8090
			a
		end
	end
	
	subgraph Linux Server 3
		subgraph JVM8070
			p
		end
	end
	
	JVM8080 --> configdb
	JVM8090 --> configdb
	JVM8070 --> portaldb
    
	JVM8090 --> JVM8080
	JVM8070 --> JVM8090
```

### 2.2.2 单机，单环境 分开部署 2台Linux服务器

不过通常我们会把Config Service和Admin Service部署在一台Linux服务器上

需要：

* 2台Linux服务器：1台部署Portal，另一台部署Config Service和Admin Service
* 2个database

```mermaid
flowchart LR
	m[Meta Server]
	e[Eureka]
	c[Config Service]
	a[Admin Service]
	p[Portal]
	
	configdb[(ConfigDB)]
	portaldb[(PortalDB)]

	subgraph Linux Server 1
		subgraph JVM8080
			m
			e
			c
		end
		subgraph JVM8090
			a
		end
	end
	
	subgraph Linux Server 2
		subgraph JVM8070
			p
		end
	end
	
	JVM8080 --> configdb
	JVM8090 --> configdb
	JVM8070 --> portaldb
    
	JVM8090 --> JVM8080
	JVM8070 --> JVM8090
```

后续为了flowchart更简洁，将JVM8080里的内容进行简化，只显示Config Service，里面的Meta Server和Eureka不再显示

```mermaid
flowchart LR
    subgraph JVM8080
        m[Meta Server]
        e[Eureka]
        c[Config Service]
    end

    subgraph new-JVM8080[JVM8080]
    	new-c[Config Service]
    end

    JVM8080 --> |simplify| new-JVM8080
```

所以部署架构可以简化表示成

```mermaid
flowchart LR
	c[Config Service]
	a[Admin Service]
	p[Portal]
	
	configdb[(ConfigDB)]
	portaldb[(PortalDB)]

	subgraph Linux Server 1
		subgraph JVM8080
			c
		end
		subgraph JVM8090
			a
		end
	end
	
	subgraph Linux Server 2
		subgraph JVM8070
			p
		end
	end
	
	JVM8080 --> configdb
	JVM8090 --> configdb
	JVM8070 --> portaldb
    
	JVM8090 --> JVM8080
	JVM8070 --> JVM8090
```



## 2.3 单机，双环境

单个环境基本没法满足实际的应用场景，例如公司里有SIT测试环境和UAT测试环境，此时需要部署2个环境提供配置服务

很容易想到的部署架构如下，把单机，单环境的部署架构重复2次即可

需要：

* 2台Linux服务器
* 4个database

```mermaid
flowchart LR
	subgraph SIT
        c1[SIT Config Service]
        a1[SIT Admin Service]
        p1[SIT Portal]

        configdb1[(SIT ConfigDB)]
        portaldb1[(SIT PortalDB)]

        subgraph SIT Linux Server
            subgraph sit-jvm-8080[SIT JVM8080]
                c1
            end
            subgraph sit-jvm-8090[SIT JVM8090]
                a1
            end
            subgraph sit-jvm-8070[SIT JVM8070]
                p1
            end
        end

        sit-jvm-8080 --> configdb1
        sit-jvm-8090 --> configdb1
        sit-jvm-8070 --> portaldb1
        
        sit-jvm-8090 --> sit-jvm-8080
        sit-jvm-8070 --> sit-jvm-8090
	end

	subgraph UAT
        c2[UAT Config Service]
        a2[UAT Admin Service]
        p2[UAT Portal]

        configdb2[(UAT ConfigDB)]
        portaldb2[(UAT PortalDB)]

        subgraph UAT Linux Server
            subgraph uat-jvm-8080[UAT JVM8080]
                c2
            end
            subgraph uat-jvm-8090[UAT JVM8090]
                a2
            end
            subgraph uat-jvm-8070[UAT JVM8070]
                p2
            end
        end

        uat-jvm-8080 --> configdb2
        uat-jvm-8090 --> configdb2
        uat-jvm-8070 --> portaldb2
        
        uat-jvm-8090 --> uat-jvm-8080
        uat-jvm-8070 --> uat-jvm-8090
	end
```

但是这种方案，会存在2个Portal界面，没法1个界面管理2个环境，使用体验不是很好，Portal实际上可以只部署1套，推荐的部署架构如下

* 3台Linux服务器：
  * Portal Linux Server单独部署Portal
  * SIT Linux Server部署SIT的Config Service和Admin Service
  * UAT Linux Server部署UAT的Config Service和Admin Service
* 3个database：1个PortalDB + 1个SIT的ConfigDB + 1个UAT的ConfigDB

```mermaid
flowchart LR
	p[Portal]
	portaldb[PortalDB]
	p --> portaldb
	
	subgraph Portal Linux Server
		subgraph JVM8070
			p
		end
	end

	subgraph SIT
        c1[SIT Config Service]
        a1[SIT Admin Service]

        configdb1[(SIT ConfigDB)]

        subgraph SIT Linux Server
            subgraph sit-jvm-8080[SIT JVM8080]
                c1
            end
            subgraph sit-jvm-8090[SIT JVM8090]
                a1
            end
        end

        sit-jvm-8080 --> configdb1
        sit-jvm-8090 --> configdb1
        
        sit-jvm-8090 --> sit-jvm-8080
	end

	subgraph UAT
        c2[UAT Config Service]
        a2[UAT Admin Service]

        configdb2[(UAT ConfigDB)]

        subgraph UAT Linux Server
            subgraph uat-jvm-8080[UAT JVM8080]
                c2
            end
            subgraph uat-jvm-8090[UAT JVM8090]
                a2
            end
        end

        uat-jvm-8080 --> configdb2
        uat-jvm-8090 --> configdb2
        
        uat-jvm-8090 --> uat-jvm-8080
	end

	JVM8070 --> sit-jvm-8090
	JVM8070 --> uat-jvm-8090
```

## 2.4 单机，三个环境

假设现在需要满足SIT、UAT、PP这3个环境的使用场景，

在之前双环境的基础之上，再多加1台PP环境的Linux服务和ConfigDB即可，Portal通过修改配置的方式，来管理这3个环境

```mermaid
flowchart LR
	p[Portal]
	portaldb[PortalDB]
	p --> portaldb
	
	subgraph Portal Linux Server
		subgraph JVM8070
			p
		end
	end

	subgraph SIT
        c1[SIT Config Service]
        a1[SIT Admin Service]

        configdb1[(SIT ConfigDB)]

        subgraph SIT Linux Server
            subgraph sit-jvm-8080[SIT JVM8080]
                c1
            end
            subgraph sit-jvm-8090[SIT JVM8090]
                a1
            end
        end

        sit-jvm-8080 --> configdb1
        sit-jvm-8090 --> configdb1
        
        sit-jvm-8090 --> sit-jvm-8080
	end

	subgraph UAT
        c2[UAT Config Service]
        a2[UAT Admin Service]

        configdb2[(UAT ConfigDB)]

        subgraph UAT Linux Server
            subgraph uat-jvm-8080[UAT JVM8080]
                c2
            end
            subgraph uat-jvm-8090[UAT JVM8090]
                a2
            end
        end

        uat-jvm-8080 --> configdb2
        uat-jvm-8090 --> configdb2
        
        uat-jvm-8090 --> uat-jvm-8080
	end
	
	subgraph PP
        c3[PP Config Service]
        a3[PP Admin Service]

        configdb3[(PP ConfigDB)]

        subgraph PP Linux Server
            subgraph pp-jvm-8080[PP JVM8080]
                c3
            end
            subgraph pp-jvm-8090[PP JVM8090]
                a3
            end
        end

        pp-jvm-8080 --> configdb3
        pp-jvm-8090 --> configdb3
        
        pp-jvm-8090 --> pp-jvm-8080
	end

	JVM8070 --> sit-jvm-8090
	JVM8070 --> uat-jvm-8090
	JVM8070 --> pp-jvm-8090
```

## 2.5 单机，多个环境

原理同上，每个环境1台Linux服务器+1个ConfigDB

然后Portal添加新环境的信息即可

# 三、高可用

1个环境只有1个Config Service进程，无法满足高可用，为了避免单点宕机后影响系统的可用性，需要多实例部署，也就是部署多个Java进程在不同的Linux服务器上

## 3.1 最简高可用，单环境

回到常见的非高可用部署方式，

```mermaid
flowchart LR
	c[Config Service]
	a[Admin Service]
	p[Portal]
	
	configdb[(ConfigDB)]
	portaldb[(PortalDB)]

	subgraph Linux Server 1
		subgraph JVM8080
			c
		end
		subgraph JVM8090
			a
		end
	end
	
	subgraph Linux Server 2
		subgraph JVM8070
			p
		end
	end
	
	JVM8080 --> configdb
	JVM8090 --> configdb
	JVM8070 --> portaldb
    
	JVM8090 --> JVM8080
	JVM8070 --> JVM8090
```

当Linux Server 1宕机时，client就只能读取本地磁盘上的config-cache了，如果需要防止单台Linux宕机导致Config Service不可用，可以尝试再新增1台Linux机器

需要

* 3台Linux服务器：1台部署Portal，另外2台分别部署Config Service和Admin Service
* 2个database

```mermaid
flowchart LR
	c-1[Config Service]
	c-2[Config Service]
	a-1[Admin Service]
	a-2[Admin Service]
	p[Portal]
	
	configdb[(ConfigDB)]
	portaldb[(PortalDB)]
	
	JVM8080-1[JVM8080]
	JVM8080-2[JVM8080]
	
	JVM8090-1[JVM8090]
	JVM8090-2[JVM8090]

	subgraph Linux Server 1.1
		subgraph JVM8080-1[JVM8080]
			c-1
		end
		subgraph JVM8090-1[JVM8090]
			a-1
		end
	end
	subgraph Linux Server 1.2
		subgraph JVM8080-2[JVM8080]
			c-2
		end
		subgraph JVM8090-2[JVM8090]
			a-2
		end
	end
	
	subgraph Linux Server 2
		subgraph JVM8070
			p
		end
	end
	
	JVM8080-1 --> configdb
	JVM8090-1 --> configdb
	JVM8080-2 --> configdb
	JVM8090-2 --> configdb
	
	JVM8070 --> portaldb
    
	JVM8090-1 --> JVM8080-1
	JVM8090-2 --> JVM8080-2
	
	JVM8070 --> JVM8090-1
	JVM8070 --> JVM8090-2
```

这种部署方式下，Linux Server 1.1 或者 Linux Server 1.2宕机，系统仍旧可用，

## 3.2 高可用，单环境

在上述的基础上，如果client的数量有很多（例如上万个Java进程），可以横向扩展Config Service，引入Linux Server 1.3, Linux Server 1.4, ...

Admin Service由于只有Portal访问，在数量上可以比Config Service少很多

具体如何评定Config Service的数量，请参考 [Apollo性能测试报告](zh/misc/apollo-benchmark.md)

## 3.3 高可用，双环境

如[2.3 单机，双环境](#_23-单机，双环境)种，如果想让SIT和UAT都变成高可用，只需要分别在环境中再添加机器即可，如下图，每个环境中各有2台Linux Server，如果有性能上需求，可以再在每个环境中，使用更多的机器来部署Config Service即可

```mermaid
flowchart LR
	p[Portal]
	portaldb[(PortalDB)]
	p --> portaldb
	
	subgraph Portal Linux Server
		subgraph JVM8070
			p
		end
	end

	subgraph SIT
        sit-c1[SIT Config Service]
        sit-a1[SIT Admin Service]
        sit-c2[SIT Config Service]
        sit-a2[SIT Admin Service]
        
        sit-configdb[(SIT ConfigDB)]

        subgraph SIT Linux Server 2.1
            subgraph sit-c1-jvm-8080[SIT JVM8080]
                sit-c1
            end
            subgraph sit-c1-jvm-8090[SIT JVM8090]
                sit-a1
            end
        end
        
        subgraph SIT Linux Server 2.2
            subgraph sit-c2-jvm-8080[SIT JVM8080]
                sit-c2
            end
            subgraph sit-c2-jvm-8090[SIT JVM8090]
                sit-a2
            end
        end
        
        sit-c1-jvm-8080 --> sit-configdb
        sit-c1-jvm-8090 --> sit-configdb
        sit-c2-jvm-8080 --> sit-configdb
        sit-c2-jvm-8090 --> sit-configdb
        
        sit-c1-jvm-8090 --> sit-c1-jvm-8080
        sit-c2-jvm-8090 --> sit-c2-jvm-8080
	end

	subgraph UAT
        uat-c1[UAT Config Service]
        uat-a1[UAT Admin Service]
        uat-c2[UAT Config Service]
        uat-a2[UAT Admin Service]
        
        uat-configdb[(UAT ConfigDB)]

        subgraph UAT Linux Server 2.1
            subgraph uat-c1-jvm-8080[UAT JVM8080]
                uat-c1
            end
            subgraph uat-c1-jvm-8090[UAT JVM8090]
                uat-a1
            end
        end
        
        subgraph UAT Linux Server 2.2
            subgraph uat-c2-jvm-8080[UAT JVM8080]
                uat-c2
            end
            subgraph uat-c2-jvm-8090[UAT JVM8090]
                uat-a2
            end
        end
        
        uat-c1-jvm-8080 --> uat-configdb
        uat-c1-jvm-8090 --> uat-configdb
        uat-c2-jvm-8080 --> uat-configdb
        uat-c2-jvm-8090 --> uat-configdb
        
        uat-c1-jvm-8090 --> uat-c1-jvm-8080
        uat-c2-jvm-8090 --> uat-c2-jvm-8080
	end

	JVM8070 --> sit-c1-jvm-8090
	JVM8070 --> sit-c2-jvm-8090
	
	JVM8070 --> uat-c1-jvm-8090
	JVM8070 --> uat-c2-jvm-8090
```

## 3.4 高可用，多个环境

在上述的基础上，如果要添加一个环境，例如BETA环境，需要新增2台及以上的Linux服务器+1个ConfigDB

Portal添加新环境的信息，指向BETA环境的apollo.meta

## 3.5 高可用，单环境，单机房

实际生产环境中，很多公司和测试环境进行了隔离，所以生产环境属于单环境，只有一个PRO环境

在只有1个机房时，参考 [3.2 高可用，单环境](#_32-高可用，单环境)

## 3.6 高可用，单环境，双机房

如果有2个机房，通常机房之间存在网络隔离，如果是同城机房，idc1和idc2，可以采用如下的部署方式

```mermaid
flowchart LR
	idc1-p[idc1 Portal]
	idc2-p[idc2 Portal]
	portaldb[(PortalDB)]
	idc1-p --> portaldb
	idc2-p --> portaldb

	configdb[(ConfigDB)]

	idc1-c1-jvm-8080 --> configdb
	idc1-c1-jvm-8090 --> configdb
	idc1-c2-jvm-8080 --> configdb
	idc1-c2-jvm-8090 --> configdb

	idc2-c1-jvm-8080 --> configdb
	idc2-c1-jvm-8090 --> configdb
	idc2-c2-jvm-8080 --> configdb
	idc2-c2-jvm-8090 --> configdb

	subgraph idc1
		subgraph idc1 Portal Linux Server
			subgraph idc1-JVM8070
				idc1-p
			end
		end
	
		idc1-c1[idc1 Config Service]
		idc1-a1[idc1 Admin Service]
		idc1-c2[idc1 Config Service]
		idc1-a2[idc1 Admin Service]
		
		

		subgraph idc1 Linux Server 2.1
			subgraph idc1-c1-jvm-8080[idc1 JVM8080]
				idc1-c1
			end
			subgraph idc1-c1-jvm-8090[idc1 JVM8090]
				idc1-a1
			end
		end
		
		subgraph idc1 Linux Server 2.2
			subgraph idc1-c2-jvm-8080[idc1 JVM8080]
				idc1-c2
			end
			subgraph idc1-c2-jvm-8090[idc1 JVM8090]
				idc1-a2
			end
		end
		
		idc1-c1-jvm-8090 --> idc1-c1-jvm-8080
		idc1-c2-jvm-8090 --> idc1-c2-jvm-8080
	end

	subgraph idc2
		subgraph idc2 Portal Linux Server
			subgraph idc2-JVM8070
				idc2-p
			end
		end
		idc2-c1[idc2 Config Service]
		idc2-a1[idc2 Admin Service]
		idc2-c2[idc2 Config Service]
		idc2-a2[idc2 Admin Service]

		subgraph idc2 Linux Server 2.1
			subgraph idc2-c1-jvm-8080[idc2 JVM8080]
				idc2-c1
			end
			subgraph idc2-c1-jvm-8090[idc2 JVM8090]
				idc2-a1
			end
		end
		
		subgraph idc2 Linux Server 2.2
			subgraph idc2-c2-jvm-8080[idc2 JVM8080]
				idc2-c2
			end
			subgraph idc2-c2-jvm-8090[idc2 JVM8090]
				idc2-a2
			end
		end
		
		idc2-c1-jvm-8090 --> idc2-c1-jvm-8080
		idc2-c2-jvm-8090 --> idc2-c2-jvm-8080
	end


	idc1-JVM8070 --> idc1-c1-jvm-8090
	idc1-JVM8070 --> idc1-c2-jvm-8090
	
	idc2-JVM8070 --> idc2-c1-jvm-8090
	idc2-JVM8070 --> idc2-c2-jvm-8090
```

每个机房有自己的一套Portal, Config Service, Admin Service

对于ConfigDB，在同城双机房下，连接的ConfigDB是同一个，不存在2个不同的ConfigDB，对于PortalDB也是如此，需要连接同一个

ConfigDB和PortalDB在图中没有放入idc1或者idc2，需要自行选用合适的MySQL架构以及部署方式

# 四、部署图

## 4.1 ctrip

以ctrip为例，我们的部署策略如下：
![Deployment](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/apollo-deployment.png)

* Portal部署在生产环境的机房，通过它来直接管理FAT、UAT、PRO等环境的配置
* Meta Server、Config Service和Admin Service在每个环境都单独部署，使用独立的数据库
* Meta Server、Config Service和Admin Service在生产环境部署在两个机房，实现双活
* Meta Server和Config Service部署在同一个JVM进程内，Admin Service部署在同一台服务器的另一个JVM进程内

## 4.2 样例部署图

[@lyliyongblue](https://github.com/lyliyongblue) 贡献的样例部署图（建议右键新窗口打开看大图）：

![Deployment](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/lyliyongblue-apollo-deployment.png)
