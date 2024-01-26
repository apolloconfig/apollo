When configuring the release, we hope to release the information email notification to the relevant person in charge. The actions currently supported for sending emails include: normal publishing, grayscale publishing, full publishing, and rollback. The notification objects include: personnel with namespace editing and publishing permissions, and the person in charge of the app.

Since each company's mail service often has different implementations, Apollo defines some SPIs for decoupling. The key to Apollo's access to mail services is to implement these SPIs.

## 1. Implementation-1: Use the smtp mail service provided by Apollo

### 1.1 Access steps

Configure the following parameters in the ApolloPortalDB.ServerConfig table or through the Administrator Tools - System Parameters page, and the modification will take effect in real time within one minute. as follows:

* **email.enabled** Set to true to enable the default smtp mail service
* **email.config.host** smtp service address, such as `smtp.163.com`
* **email.config.user** smtp account username
* **email.config.password** smtp account password
* **email.supported.envs** A comma-separated list of environments that support sending emails. We don't want posting emails to become spam for users, only posting actions under certain circumstances will send emails.
* **email.sender** The sender of the email, can not be configured, the default is `email.config.user`.
* **apollo.portal.address** The address of the Apollo Portal. It is convenient for users to click to jump from the email to the Apollo Portal to view the detailed release information.
* **email.template.framework** Email content template framework. The email content is templated and configurable to facilitate management and change of email content.
* **email.template.release.module.diff** The diff module for publishing emails.
* **email.template.rollback.module.diff** The diff module for rolling back emails.
* **email.template.release.module.rules** Grayscale rules module for grayscale release.

We provide [Sample Email Template](en/extension/portal-how-to-enable-email-service?id=_3-email-template-sample) for your convenience.

## 2. Implementation-2: Access the company's unified mail service

Similar to SSO, each company also has its own mail service implementation, so we define [EmailService](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/EmailService.java) interface, there are two implementation classes:

1. CtripEmailService: EmailService implemented by Ctrip
2. DefaultEmailService: smtp implementation

### 2.1 Access steps

1. Provide your company's [EmailService](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/EmailService.java) implementation and in [EmailConfiguration](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/configuration/EmailConfiguration.java) registered.
2. Configure the following parameters in the ApolloPortalDB.ServerConfig table, or through the Administrator Tools - System Parameters page, and the modification will take effect in one minute. as follows:

* **email.supported.envs** A comma-separated list of environments that support sending emails. We don't want posting emails to become spam for users, only posting actions under certain circumstances will send emails.
* **email.sender** The sender of the email.
* **apollo.portal.address** The address of the Apollo Portal. It is convenient for users to click to jump from the email to the Apollo Portal to view the detailed release information.
* **email.template.framework** Email content template framework. The email content is templated and configurable to facilitate management and change of email content.
* **email.template.release.module.diff** The diff module for publishing emails.
* **email.template.rollback.module.diff** The diff module for rolling back emails.
* **email.template.release.module.rules** Grayscale rules module for grayscale release.

We provide  [Sample Email Template](en/extension/portal-how-to-enable-email-service?id=_3-email-template-sample)  for your convenience.

Note: using different implementations at runtime is achieved through [Profiles](http://docs.spring.io/autorepo/docs/spring-boot/current/reference/html/boot-features-profiles.html), For example, if your own Email implementation is in the `custom` profile, you can specify -Dapollo_profile=github,custom in the packaging script. Among them, `github` is a required profile of Apollo, which is used for database configuration, and `custom` is a profile that you implement yourself. Also note that in [EmailConfiguration](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/configuration/EmailConfiguration.java) to modify the default implementation condition `@Profile({"!custom"})`.

### 2.2 Related code

1. [ConfigPublishListener](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/listener/ConfigPublishListener.java) to monitor the release event, call emailbuilder to build email content, and then call EmailService to send emails
2. The [emailbuilder](https://github.com/apolloconfig/apollo/tree/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/component/emailbuilder) package is the build email realization of content
3. [EmailService](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/EmailService.java) Email sending Serve
4. [EmailConfiguration](https://github.com/apolloconfig/apollo/blob/master/apollo-portal/src/main/java/com/ctrip/framework/apollo/portal/spi/configuration/EmailConfiguration.java) Mail service registration class

## 3. Email template sample

The following are the template content styles for publishing emails and rolling back emails. The email templates are in html format. When sending emails in html format, some additional processing may be required, depending on the implementation of each company's email service. To reduce character count, templates are compressed and self-formatted to improve readability.

### 3.1 email.template.framework

```html
<html><head><style type="text/css">.table{width:100%;max-width:100%;margin-bottom:20px;border-collapse:collapse;background-color:transparent}td {padding:8px;line-height:1.42857143;vertical-align:top;border:1px solid #ddd;border-top:1px solid #ddd}.table-bordered{border:1px solid #ddd}</style>< /head><body><h3>Post basic information</h3><table class="table table-bordered"><tr><td width="10%"><b>AppId</b></td ><td width="15%">#{appId}</td><td width="10%"><b>Environment</b></td><td width="15%">#{ env}</td><td width="10%"><b>cluster</b></td><td width="15%">#{clusterName}</td><td width="10 %"><b>Namespace</b></td><td width="15%">#{namespaceName}</td></tr><tr><td><b>Publisher</b ></td><td>#{operator}</td><td><b>release time</b></td><td>#{releaseTime}</td><td><b>release Title</b></td><td>#{releaseTitle}</td><td><b>Comment</b></td><td>#{releaseComment}</td></tr> </table>#{diffModule}#{rulesModule}<br><a href="#{apollo.portal.address}/config/history.html?#/appid=#{appId}&env=#{env}&clusterName =#{clusterName}&namespaceName=#{namespaceName}&releaseHistoryId=#{ releaseHistoryId}">Click to view detailed release information</a><br><br>If you have any questions about using Apollo, please check <a href="http://conf.ctripcorp.com/display/FRAM/Apollo"> document</a>, or reply directly to this email inquiry. </body></html>
```

> Note: To use this template, you need to configure apollo.portal.address in the system parameters of the portal to point to the address of the apollo portal

### 3.2 email.template.release.module.diff

```html
<h3>Changed Configuration Items</h3>
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
    <h3>Changed Configuration Items</h3>
    <table class="table table-bordered">
        <tr>
            <td width="10%"><b>Type</b></td>
            <td width="20%"><b>Key</b></td>
            <td width="35%"><b>Before Rollback</b></td>
            <td width="35%"><b>After Rollback</b></td>
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
    <h3>Grayscale Rules</h3>
    #{rulesContent}
    <br>
</div>
```

### 3.5 Sample Release Email

![发布邮件模板](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/email-template-release.png)

### 3.6 Sample Rollback Email

![回滚邮件模板](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/email-template-rollback.png)
