# 1. 升级版本
在 Apollo 工程下全文替换版本号，例如 1.9.1-SNAPSHOT 升级到 1.9.1。替换过程中，一定要注意非版本号的地方替换。
# 2. 撰写发布报告
两个版本之间每一个 PR 都会记录在 [https://github.com/ctripcorp/apollo/blob/master/CHANGES.md](https://github.com/ctripcorp/apollo/blob/master/CHANGES.md) 里，所以撰写发布报告只需要从 CHANGES.md 里提取即可。发布报告参考：[https://github.com/ctripcorp/apollo/releases/tag/v1.8.0](https://github.com/ctripcorp/apollo/releases/tag/v1.8.0)
​

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
   1. git tag v1.9.0
3. push tag
   1. git push origin tag v1.9.0
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
在 scripts 目录下执行以下三个命令
​

计算 configservice checksum
> shasum ../apollo-configservice/target/apollo-configservice-1.9.0-github.zip > ../apollo-configservice/target/apollo-configservice-1.9.0-github.zip.sha1

计算 adminservice checksum
> shasum ../apollo-adminservice/target/apollo-adminservice-1.9.0-github.zip > ../apollo-adminservice/target/apollo-adminservice-1.9.0-github.zip.sha1

计算 portal checksum
> shasum ../apollo-portal/target/apollo-portal-1.9.0-github.zip > ../apollo-portal/target/apollo-portal-1.9.0-github.zip.sha1

## 4.3 创建 pre-release
github 创建 pre-release
![image.png](https://cdn.nlark.com/yuque/0/2021/png/94396/1629015924779-f517e179-2711-4ccb-ab46-1fb69ca2f191.png#clientId=u9690858a-d2d3-4&from=paste&height=641&id=u4161f0d9&margin=%5Bobject%20Object%5D&name=image.png&originHeight=1282&originWidth=3564&originalType=binary&ratio=1&size=258348&status=done&style=none&taskId=ue15b5715-83f8-4ff2-9c6a-285d1c11989&width=1782)




填写 Release Note & 上传包
![image.png](https://cdn.nlark.com/yuque/0/2021/png/94396/1629015996198-eb38be3c-23e2-466e-8a46-51a568cc8f30.png#clientId=u9690858a-d2d3-4&from=paste&height=930&id=ub74daa78&margin=%5Bobject%20Object%5D&name=image.png&originHeight=1860&originWidth=1518&originalType=binary&ratio=1&size=313753&status=done&style=none&taskId=u764c1d49-c82a-417f-8b5d-ca1344d8224&width=759)
## 4.4 预发布 Apollo-Client Jar 包
通过 github workflow 来发布。
[https://github.com/ctripcorp/apollo/actions/workflows/release.yml](https://github.com/ctripcorp/apollo/actions/workflows/release.yml)
![image.png](https://cdn.nlark.com/yuque/0/2021/png/94396/1629106996256-ec68519a-1b22-484e-a1bb-90376528c3a4.png#clientId=u1dee6cad-a507-4&from=paste&height=588&id=ue1658a72&margin=%5Bobject%20Object%5D&name=image.png&originHeight=1176&originWidth=2472&originalType=binary&ratio=1&size=239867&status=done&style=none&taskId=u88feb7a7-66a4-471f-ae8f-91f65eecad4&width=1236)
注意：选择 snapshots 可能会失败，原因是账户授权问题。选择 releases 可以正常发布。
## 4.5 版本发布 PMC 投票
投票是为了让各个 PMC 成员协作验证版本的内容，防止发布有问题的版本。
投票具体的形式为在 Discussions 发起一个帖子，可参考：[https://github.com/ctripcorp/apollo/discussions/3899](https://github.com/ctripcorp/apollo/discussions/3899)
## 4.6 正式发布 Apollo-Client Jar 到仓库


## 4.7 发布 Docker 镜像
### 4.7.1 构建镜像
在 4.2 步骤打完包的前提下，在 apollo 根目录下执行
> mvn docker:build -pl apollo-configservice,apollo-adminservice,apollo-portal -D1.9.0

注意：如果出现报错，可能需要重启一下本地 docker
### 4.7.2 Push 镜像到仓库
仓库地址：[https://hub.docker.com/u/apolloconfig](https://hub.docker.com/u/apolloconfig)
依次 Push configservice/adminservice/portal，切记 latest 版本也要 push。
![image.png](https://cdn.nlark.com/yuque/0/2021/png/94396/1629720378811-2a2f8ec7-e8f9-465b-97d3-1ad5ce461a17.png#clientId=uccccf577-5763-4&from=paste&height=720&id=ud81f423f&margin=%5Bobject%20Object%5D&name=image.png&originHeight=720&originWidth=1270&originalType=binary&ratio=1&size=114623&status=done&style=none&taskId=u4255742c-e782-444b-9ad8-9307b448f91&width=1270)
​

## 4.8 更新 helm chart
### 4.8.1 更新 chart 内容

1. cd ${apollo_home}/scripts/helm
1. helm package apollo-portal && helm package apollo-service
1. mv *.tgz ${apollo_home}/apollo/docs/charts/
1. cd ${apollo_home}/apollo/docs/charts/
1. helm repo index .
### 4.8.2 分支合并到 master
创建一个 pull request，把上述产物合并到 master 分支。
# 5. 发布公告
参考：[https://github.com/ctripcorp/apollo/discussions/3740](https://github.com/ctripcorp/apollo/discussions/3740)


