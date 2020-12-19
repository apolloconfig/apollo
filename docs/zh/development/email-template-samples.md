在配置发布时候，可以发布信息邮件通知到相关的负责人，具体接入方式请参考[Apollo开发指南-3.2 Portal接入邮件服务](zh/development/apollo-development-guide#_32-portal接入邮件服务)。

以下为发布邮件和回滚邮件的模板内容样式，邮件模板为html格式，发送html格式的邮件时，可能需要做一些额外的处理，取决于每个公司的邮件服务实现。为了减少字符数，模板经过了压缩处理，可自行格式化提高可读性。

## email.template.framework

```html
<html><head><style type="text/css">.table{width:100%;max-width:100%;margin-bottom:20px;border-collapse:collapse;background-color:transparent}td{padding:8px;line-height:1.42857143;vertical-align:top;border:1px solid #ddd;border-top:1px solid #ddd}.table-bordered{border:1px solid #ddd}</style></head><body><h3>发布基本信息</h3><table class="table table-bordered"><tr><td width="10%"><b>AppId</b></td><td width="15%">#{appId}</td><td width="10%"><b>环境</b></td><td width="15%">#{env}</td><td width="10%"><b>集群</b></td><td width="15%">#{clusterName}</td><td width="10%"><b>Namespace</b></td><td width="15%">#{namespaceName}</td></tr><tr><td><b>发布者</b></td><td>#{operator}</td><td><b>发布时间</b></td><td>#{releaseTime}</td><td><b>发布标题</b></td><td>#{releaseTitle}</td><td><b>备注</b></td><td>#{releaseComment}</td></tr></table>#{diffModule}#{rulesModule}<br><a href="#{apollo.portal.address}/config/history.html?#/appid=#{appId}&env=#{env}&clusterName=#{clusterName}&namespaceName=#{namespaceName}&releaseHistoryId=#{releaseHistoryId}">点击查看详细的发布信息</a><br><br>如有Apollo使用问题请先查阅<a href="http://conf.ctripcorp.com/display/FRAM/Apollo">文档</a>，或直接回复本邮件咨询。</body></html>
```

> 注：使用此模板需要在 portal 的系统参数中配置 apollo.portal.address，指向 apollo portal 的地址

## email.template.release.module.diff

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

## email.template.rollback.module.diff
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

## email.template.release.module.rules
```html
<div>
    <br>
    <h3>灰度规则</h3>
    #{rulesContent}
    <br>
</div>
```

## 发布邮件样例
![发布邮件模板](https://raw.githubusercontent.com/ctripcorp/apollo/master/doc/images/email-template-release.png)

## 回滚邮件样例
![回滚邮件模板](https://raw.githubusercontent.com/ctripcorp/apollo/master/doc/images/email-template-rollback.png)