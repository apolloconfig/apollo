在配置发布时候，我们希望发布信息邮件通知到相关的负责人。现支持发送邮件的动作有：普通发布、灰度发布、全量发布、回滚，通知对象包括：具有namespace编辑和发布权限的人员以及App负责人。

由于各公司的邮件服务往往有不同的实现，所以Apollo定义了一些SPI用来解耦，Apollo接入邮件服务的关键就是实现这些SPI。

## 一、实现方式一：使用Apollo提供的smtp邮件服务

### 1.1 接入步骤
在ApolloPortalDB.ServerConfig表配置以下参数，也可以通过管理员工具 - 系统参数页面进行配置，修改完一分钟实时生效。如下：
* **email.enabled** 设置为true即可启用默认的smtp邮件服务 
* **email.config.host** smtp的服务地址，如`smtp.163.com`
* **email.config.user** smtp帐号用户名
* **email.config.password** smtp帐号密码
* **email.supported.envs** 支持发送邮件的环境列表，英文逗号隔开。我们不希望发布邮件变成用户的垃圾邮件，只有某些环境下的发布动作才会发送邮件。
* **email.sender** 邮件的发送人，可以不配置，默认为`email.config.user`。
* **apollo.portal.address** Apollo Portal的地址。方便用户从邮件点击跳转到Apollo Portal查看详细的发布信息。
* **email.template.framework** 邮件内容模板框架。将邮件内容模板化、可配置化，方便管理和变更邮件内容。
* **email.template.release.module.diff** 发布邮件的diff模块。
* **email.template.rollback.module.diff** 回滚邮件的diff模块。
* **email.template.release.module.rules** 灰度发布的灰度规则模块。

我们提供了[邮件模板样例](#三、邮件模板样例)，方便大家使用。

## 二、实现方式二：接入公司的统一邮件服务

和SSO类似，每个公司也有自己的邮件服务实现，所以我们相应的定义了[EmailService](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/EmailService.java)接口，现有两个实现类：
1. CtripEmailService：携程实现的EmailService
2. DefaultEmailService：smtp实现

### 2.1 接入步骤
1. 提供自己公司的[EmailService](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/EmailService.java)实现，并在[EmailConfiguration](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/configuration/EmailConfiguration.java)中注册。
2. 在ApolloPortalDB.ServerConfig表配置以下参数，也可以通过管理员工具 - 系统参数页面进行配置，修改完一分钟实时生效。如下：
* **email.supported.envs** 支持发送邮件的环境列表，英文逗号隔开。我们不希望发布邮件变成用户的垃圾邮件，只有某些环境下的发布动作才会发送邮件。
* **email.sender** 邮件的发送人。
* **apollo.portal.address** Apollo Portal的地址。方便用户从邮件点击跳转到Apollo Portal查看详细的发布信息。
* **email.template.framework** 邮件内容模板框架。将邮件内容模板化、可配置化，方便管理和变更邮件内容。
* **email.template.release.module.diff** 发布邮件的diff模块。
* **email.template.rollback.module.diff** 回滚邮件的diff模块。
* **email.template.release.module.rules** 灰度发布的灰度规则模块。
  
我们提供了[邮件模板样例](#三、邮件模板样例)，方便大家使用。

>注：运行时使用不同的实现是通过[Profiles](http://docs.spring.io/autorepo/docs/spring-boot/current/reference/html/boot-features-profiles.html)实现的，比如你自己的Email实现是在`custom` profile中的话，在打包脚本中可以指定-Dapollo_profile=github,custom。其中`github`是Apollo必须的一个profile，用于数据库的配置，`custom`是你自己实现的profile。同时需要注意在[EmailConfiguration](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/configuration/EmailConfiguration.java)中修改默认实现的条件`@Profile({"!custom"})`。

### 2.2 相关代码
1. [ConfigPublishListener](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/listener/ConfigPublishListener.java)监听发布事件，调用emailbuilder构建邮件内容，然后调用EmailService发送邮件
2. [emailbuilder](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/components/emailbuilder)包是构建邮件内容的实现
3. [EmailService](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/EmailService.java) 邮件发送服务
4. [EmailConfiguration](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/configuration/EmailConfiguration.java) 邮件服务注册类

## 三、邮件模板样例
以下为发布邮件和回滚邮件的模板内容样式，邮件模板为html格式，发送html格式的邮件时，可能需要做一些额外的处理，取决于每个公司的邮件服务实现。为了减少字符数，模板经过了压缩处理，可自行格式化提高可读性。

### 3.1 email.template.framework

```html
<html><head><style type="text/css">.table{width:100%;max-width:100%;margin-bottom:20px;border-collapse:collapse;background-color:transparent}td{padding:8px;line-height:1.42857143;vertical-align:top;border:1px solid #ddd;border-top:1px solid #ddd}.table-bordered{border:1px solid #ddd}</style></head><body><h3>发布基本信息</h3><table class="table table-bordered"><tr><td width="10%"><b>AppId</b></td><td width="15%">#{appId}</td><td width="10%"><b>环境</b></td><td width="15%">#{env}</td><td width="10%"><b>集群</b></td><td width="15%">#{clusterName}</td><td width="10%"><b>Namespace</b></td><td width="15%">#{namespaceName}</td></tr><tr><td><b>发布者</b></td><td>#{operator}</td><td><b>发布时间</b></td><td>#{releaseTime}</td><td><b>发布标题</b></td><td>#{releaseTitle}</td><td><b>备注</b></td><td>#{releaseComment}</td></tr></table>#{diffModule}#{rulesModule}<br><a href="#{apollo.portal.address}/config/history.html?#/appid=#{appId}&env=#{env}&clusterName=#{clusterName}&namespaceName=#{namespaceName}&releaseHistoryId=#{releaseHistoryId}">点击查看详细的发布信息</a><br><br>如有Apollo使用问题请先查阅<a href="http://conf.ctripcorp.com/display/FRAM/Apollo">文档</a>，或直接回复本邮件咨询。</body></html>
```

> 注：使用此模板需要在 portal 的系统参数中配置 apollo.portal.address，指向 apollo portal 的地址

### 3.2 email.template.release.module.diff

```html
<h3>变更的配置</h3>
<table class="table table-bordered">
    <tr>
        <td width="10%"><b>Type</b></td>
        <td width="20%"><b>Key</b></td>
        <td width="35%"><b>Old Value</b></td>
        <td width="35%"><b>New Value</b></td>
    </tr>
    #{diffContent}
</table>
```

### 3.3 email.template.rollback.module.diff
```html
<div>
    <br><br>
    <h3>变更的配置</h3>
    <table class="table table-bordered">
        <tr>
            <td width="10%"><b>Type</b></td>
            <td width="20%"><b>Key</b></td>
            <td width="35%"><b>回滚前</b></td>
            <td width="35%"><b>回滚后</b></td>
        </tr>
        #{diffContent}
    </table>
    <br>
</div>
```

### 3.4 email.template.release.module.rules
```html
<div>
    <br>
    <h3>灰度规则</h3>
    #{rulesContent}
    <br>
</div>
```

### 3.5 发布邮件样例
![发布邮件模板](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/email-template-release.png)

### 3.6 回滚邮件样例
![回滚邮件模板](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/email-template-rollback.png)