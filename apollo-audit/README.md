# Features: Apollo-Audit-Log

This module provides audit log functions for other Apollo modules.

Only apolloconfig's developer need to read it, 

apolloconfig's user doesn't need.

## How to enable/disable

We can switch this module freely by properties:

by adding properties to application.properties:

```
# true: enabled the new feature of audit log
# false/missing: disable it
apollo.audit.log.enabled = true
```

## How to generate audit log

### Append an AuditLog

Through an AuditLog, we have the ability to record **Who, When, Why, Where, What and How** operates something.

We can do this by using annotations:

```java
@ApolloAuditLog(type=OpType.CREATE,name="App.create")
public App create() {
  // ...
}
```

Through this, an AuditLog will be created and its AuditScope will be activated during the execution of this method.

Equally, we can use ApolloAuditLogApi to do this manually:

```java
public App create() {
  try(AutoCloseable auditScope = api.appendAuditLog(type, name)) {
    // ...
  }
}
/**************OR**************/
public App create() {
  Autocloseable auditScope = api.appendAuditLog(type, name);
  // ...
  auditScope.close();
}
```

The only thing you need to pay attention to is that you need to close this scope manually～

### Append DataInfluence

This function can also be implemented automatically and manually.

There is a corresponding relationship between DataInfluences and a certain AuditLog, and they are caused by this AuditLog. But not all AuditLogs will generate DataInfluences!

#### Mark which data change

First, we need to add audit-bean-definition to class of the entity you want to audit:

```java
@ApolloAuditLogDataInfluenceTable(tableName = "App")
public class App extends BaseEntity {
  @ApolloAuditLogDataInfluenceTableField(fieldName = "Name")
  private String name;
  private String orgId;
}
```

In class App, we define that its data-influence table' name is "App", the field "name" needs to be audited and its audit field name in the table "App" is "Name". The field "orgId" is no need to be audited.

Second, use API's method to append it:

Actually we don't need to manually call it. We can depend on the DomainEvents that pre-set in BaseEntity:

```java
@DomainEvents
public Collection<ApolloAuditLogDataInfluenceEvent> domainEvents() {
  return Collections.singletonList(new ApolloAuditLogDataInfluenceEvent(this.getClass(), this));
}
```

And this will call appendDataInfluences automatically by the listener.

#### Manually

```java
/**
 * Append DataInfluences by a list of entities needs to be audited, and their
 * audit-bean-definition.
 */
ApolloAuditLogApi.appendDataInfluences(List<Object> entities, Class<?> beanDefinition);
```

Just call the api method in an active scope, the data influences will combine with the log automatically.

```java
public App create() {
  try(AutoCloseable auditScope = api.appendAuditLog(type, name)) {
    // ...
    api.appendDataInfluences(appList, App.class);
  	// or.
  	api.appendDataInfluence("App","10001","Name","xxx");
  }
}
```

#### some tricky situations

Yet, sometimes we can't catch the domain events like some operations that directly change database fields. We can use annotations to catch the input parameters:

```java
@ApolloAuditLog(type=OpType.DELETE,name="AppNamespace.batchDeleteByAppId")
public AppNamespace batchDeleteByAppId(
  @ApolloAuditLogDataInfluence
  @ApolloAuditLogDataInfluenceTable(tableName="AppNamespace")
  @ApolloAuditLogDataInfluenceTableField(fieldName="AppId") String appId) {
  // ...
}
```

This will generate a special data influence. It means that all entities matching the input parameter value have been affected.

## How to verify the audit-log work

### check-by-UI

The entrance of audit log UI is in Admin Tools.

Then, we can check if the AuditLogs are created properly by searching or just find in table below.

Then, check in the trace detail page.

We can check if the relationship between AuditLogs are correct and the DataInfluences caused by certain AuditLog is logically established.

In the rightmost column, we can view the historical operation records of the specified field's value. Null means being deleted~

### check-by-database

The databases are in ApolloPortalDB, the table `AuditLog` and `AuditLogDataInfluence`.

We can verify if the parent/followsfrom relationships are in line with our expectations.