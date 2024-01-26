# 1. 升级版本

在 Apollo 工程下全文替换版本号，例如 1.9.1-SNAPSHOT 升级到 1.9.1。替换过程中，一定要注意非版本号的地方替换。

# 2. 撰写发布报告

两个版本之间每一个 PR 都会记录在 [https://github.com/apolloconfig/apollo/blob/master/CHANGES.md](https://github.com/apolloconfig/apollo/blob/master/CHANGES.md) 里，所以撰写发布报告只需要从 CHANGES.md 里提取即可。发布报告参考：[https://github.com/apolloconfig/apollo/releases/tag/v1.8.0](https://github.com/apolloconfig/apollo/releases/tag/v1.8.0)

# 3. 版本验证

版本验证主要包含三方面的验证：

1. 新引入的代码变更验证，例如新功能、bugfix
1. Apollo 核心主流程验证，包括：配置发布，动态推送，灰度推送等
1. 升级过程验证，包括：经典部署模式、docker 模式、k8s 模式

# 4. 版本发布

## 4.1 打 tag

1. 拉取 master 最新代码
   1. git pull origin master  
2. 打 tag
   1. git tag ${new-version}
3. push tag
   1. git push origin tag ${new-version}

## 4.2 打包

### 4.2.1 前置检查

在打包之前检查本地环境, mvn -v 确保 java 版本是 1.8, 例如以下输出：

> mvn -v
> Apache Maven 3.8.1 (05c21c65bdfed0f71a2f2ada8b84da59348c4c5d)Maven home: /usr/local/Cellar/maven/3.8.1/libexec
> Java version: 1.8.0_301, vendor: Oracle Corporation, runtime: /Library/Java/JavaVirtualMachines/jdk1.8.0_301.jdk/Contents/Home/jre
> Default locale: zh_CN, platform encoding: UTF-8
> OS name: "mac os x", version: "10.16", arch: "x86_64", family: "mac"

### 4.2.2 打包

在 ${apollo_home}/scripts/ 目录下执行：

> ./build.sh

如果报以下错误：

> zsh: ./build.sh: bad interpreter: /bin/sh^M: no such file or directory

则需要执行以下命令转换成 unix

> brew install dos2unix
> dos2unix build.sh

### 4.2.3 计算构建包的 checksum

计算 configservice checksum

>在 ${apollo_home}/apollo-configservice/target/ 目录下执行：
> 
>shasum apollo-configservice-${new-version}-github.zip > apollo-configservice-${new-version}-github.zip.sha1

计算 adminservice checksum

>在 ${apollo_home}/apollo-adminservice/target/ 目录下执行：
>
>shasum apollo-adminservice-${new-version}-github.zip > apollo-adminservice-${new-version}-github.zip.sha1

计算 portal checksum

>在 ${apollo_home}/apollo-portal/target/ 目录下执行：  
>
> shasum apollo-portal-${new-version}-github.zip > apollo-portal-${new-version}-github.zip.sha1

## 4.3 创建 pre-release

github 创建 pre-release

![image.png](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/create-release.png)

填写 Release Note & 上传包

![image.png](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/fill-release-form.png)

## 4.4 预发布 Apollo-Client Jar 包

通过 github workflow 来发布。

[https://github.com/apolloconfig/apollo-java/actions/workflows/release.yml](https://github.com/apolloconfig/apollo-java/actions/workflows/release.yml)

![image.png](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/publish-sdk.png)

> 注意：如果发布的是 SNAPSHOT 版本，使用默认值 snapshots 即可，如果发布的是正式版本（不带 SNAPSHOT），则需要修改为 releases 才可以正常发布。

## 4.5 版本发布 PMC 投票

投票是为了让各个 PMC 成员协作验证版本的内容，防止发布有问题的版本。
投票具体的形式为在 Discussions 发起一个帖子，可参考：[https://github.com/apolloconfig/apollo/discussions/3899](https://github.com/apolloconfig/apollo/discussions/3899)

## 4.6 正式发布 Apollo-Client Jar 到仓库

## 4.7 发布 Docker 镜像

### 4.7.1 本地构建镜像并测试

在 4.2 步骤打完包的前提下，在 apollo 根目录下执行

> mvn docker:build -pl apollo-configservice,apollo-adminservice,apollo-portal

注意：如果出现报错，可能需要重启一下本地 docker

### 4.7.2 Push 镜像到仓库

通过 github workflow 来发布。

[https://github.com/apolloconfig/apollo/actions/workflows/docker-publish.yml](https://github.com/apolloconfig/apollo/actions/workflows/docker-publish.yml)

在文本框中填写发布的版本号即可

![image.png](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/local-development/publish-docker.jpg)

## 4.8 更新 helm chart

### 4.8.1 更新 chart 内容

1. cd [apollo-helm-chart](https://github.com/apolloconfig/apollo-helm-chart)
2. helm package apollo-portal && helm package apollo-service
3. mv *.tgz docs
4. cd docs
5. helm repo index .

### 4.8.2 分支合并到 main

创建一个 pull request，把上述产物合并到 [main](https://github.com/apolloconfig/apollo-helm-chart) 分支。

# 5. 发布公告

参考：[https://github.com/apolloconfig/apollo/discussions/3740](https://github.com/apolloconfig/apollo/discussions/3740)
