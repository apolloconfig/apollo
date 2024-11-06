
# 宝塔面板(适用9.2.0及以上的版本)快速部署Apollo

## 一、 准备工作
前往[宝塔面板官网](https://www.bt.cn/new/download.html)，选择正式版的脚本下载安装（如果已安装，请跳过此步）

### 1.1 登陆面板安装docker环境
安装后登录宝塔面板，在左侧导航栏点击Docker，首先进入会提示安装Docker服务，点击立即安装，按提示完成安装
![20241010103723](https://github.com/user-attachments/assets/6642c5b8-892e-4703-84c2-f82fcc841ceb)
![20241010104431](https://github.com/user-attachments/assets/c63bda9b-eb07-4352-be7e-c12dbf457018)

### 1.2 通过docker应用商店部署
在应用商店中找到Apollo，点击安装，配置域名、端口等基本信息即可完成安装
![1111](https://github.com/user-attachments/assets/93486f4f-0ba8-4098-88d7-ea1cf546a0a1)

注意：
域名为非必填，如果填写了域名则通过【网站】--> 【反向代理】来管理，填写域名后不需要勾选【允许外部访问】，否则需要勾选后才可以通过端口访问


## 二、访问Apollo
安装后在浏览器输入上一步设置的域名或者IP+端口即可访问
