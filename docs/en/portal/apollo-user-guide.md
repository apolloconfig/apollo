# &nbsp;

# Glossary of Terms

* Common application
  * Common applications refer to programs that run independently, such as
    * Web applications
    * Programs with main functions
* Public components
  * Public components refer to distributed libraries, client programs that do not run on their own, such as
    * Java jar packages
    * .Net dll file

# I. General Application Access Guide

## 1.1 Creating a project

To use Apollo, you need to create a project as the first step.

1. Open apollo-portal homepage
2. Click "Create a project".

![create-app-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-app-entry.png)

3. Enter the project information
   * Department: Select the department where the application is located
   * Application AppId: the unique id used to identify the application, the format is string, it needs to correspond to the app.id configured in the client app.properties
   * Application Name: the name of the application, used only for interface display
   * Application Manager: The person who selects it will be the administrator of the project by default, with permissions to manage project permissions, create clusters, create Namespace, etc.

![create-app](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-app.png)

4. Click Submit

   After successful creation, it will automatically jump to the project home page

![app-created](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/app-created.png)

## 1.2 Project permission assignment

### 1.2.1 Project administrator privileges

The project administrator has the following privileges.

1. Can manage the permissions assignment of the project
2. Create clusters
3. Can create Namespace

If you need someone else to be the project administrator, you can follow the steps below.

1. Click "Manage Projects" on the left side of the page
   * ![app-permission-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/app-permission-entry.png)

2. Search for the member you want to add and click Add
   * ![app-permission-search-user](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/app-permission-search-user.png)
   * ![app-permission-user-added](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/app-permission-user-added.png)

### 1.2.2 Configuring editing and publishing permissions

Configuration permissions are divided into edit and publish.

* Edit permission allows users to create, modify, and delete configurations on the Apollo interface
  * Configuration modification only changes on Apollo interface and does not affect the actual configuration used by the application
* Publish permissions allow users to publish and roll back configurations in the Apollo interface
  * Configurations are only actually used by the application after they have been published and rolled back
  * Apollo notifies the application in real time after the user has performed a publish or rollback action and the latest configuration takes effect

After the project is created, there are no editing and publishing permissions assigned to the configuration by default, and the project administrator needs to authorize it.

1. Click the authorization button of the namespace application
   * ![namespace-permission-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-entry.png)

2. Assign the modify permission
   * ![namespace-permission-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-edit.png)

3. Assign publish privileges
   * ![namespace-publish-permission](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-publish-permission.png)

### 1.2.3 Configuring permissions for different dimensions

Regarding Apollo's configuration permissions, the permissions were bound to the namespace during the initial design. Because Apollo's permission management itself is relatively flexible, it can be expanded on this basis.
Design of main entity classes based on Apollo [E-R Diagram](/docs/en/design/apollo-design.md?id=_14-e-r-diagram)，
We can think of Namespace as the smallest unit of permissions, and App as the largest unit of permissions.
In the middle are Env and Cluster, so we can manage permissions in different dimensions.

| App | Env | Cluster | Namespace | Model | Impl |
| --- | --- | --- | --- | --- |------|
| ☑️ |  |  |  | App → * | no   |
| ☑️ |  |  | ☑️ | App → Namespace | yes  |
| ☑️ | ☑️ |  |  | App + Env → * | no   |
| ☑️ | ☑️ |  | ☑️ | App + Env → Namespace | yes  |
| ☑️ | ☑️ | ☑️ |  | App + Env + Cluster → * | yes  |
| ☑️ | ☑️ | ☑️ | ☑️ | App + Env + Cluster → Namespace | no    |

Explanation of different permission models：

| Model | Target | PermissionType (e.g. Modify) | TargetId |
| --- | --- | --- | --- |
| App → * | All namespaces of App |  |  |
| App → Namespace | All namespaces with specified names under App | ModifyNamespace | App+Namespace |
| App + Env → * | All namespaces under App's env |  |  |
| App + Env → Namespace | All namespaces with specified names under App's env | ModifyNamespace | App+Namespace+Env |
| App + Env + Cluster → * | All namespaces of the cluster in App's env | ModifyNamespaceInCluster | App+Env+ClusterName |
| App + Env + Cluster → Namespace | The namespace with the specified name under the cluster in App's env |  |  |

#### 1.2.3.1 All namespaces of App

1. Click the authorization button of the application
   * ![ns-permission-app-allns-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-allns-entry.png)

2. Select "All environments"
   * ![ns-permission-app-allns-select](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-allns-select.png)

3. Assign the modify permission
   * ![namespace-permission-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-edit.png)

4. Assign publish privileges
   * ![namespace-publish-permission](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-publish-permission.png)

#### 1.2.3.2 All namespaces of App's env

1. Click the authorization button of the application
   * ![ns-permission-app-allns-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-allns-entry.png)

2. Select the env
   * ![ns-permission-app-env-ns-select](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-env-ns-select.png)

3. Assign the modify permission
   * ![namespace-permission-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-edit.png)

4. Assign publish privileges
   * ![namespace-publish-permission](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-publish-permission.png)

#### 1.2.3.3 All namespaces of the cluster in App's env

1. Click "Manage Cluster" to enter the management cluster page
   * ![manage-cluster-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/manage-cluster-entry.png)

2. Click the authorization button of the Cluster you want to manage
   * ![ns-permission-app-env-cluster-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-env-cluster-entry.png)

3. Edit permissions
   * ![ns-permission-app-env-cluster-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/ns-permission-app-env-cluster-edit.png)

## 1.3 Adding configuration items

To edit the configuration, you need to have the edit permission of this Namespace. If you find that there is no Add Configuration button, you can find the project administrator to authorize it.

### 1.3.1 Adding configuration via form mode

1. Click Add Configuration
   * ![create-item-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-item-entry.png)

2. Enter a configuration item
   * ![create-item-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-item-detail.png)

3. Click Submit
   * ![item-created](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/item-created.png)

### 1.3.2 Editing via text mode

Apollo not only supports table mode to add and modify configurations one by one, but also provides text mode to add and modify them in bulk.
This is especially useful for migrating from an existing properties file.

1. Switch to text editing mode
   ![text-mode-config-overview](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/text-mode-config-overview.png)

2. Click the Modify Configuration button on the right
   ![text-mode-config-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/text-mode-config-entry.png)

3. Enter the configuration entries and click on Submit Changes
   ![text-mode-config-submit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/text-mode-config-submit.png)

## 1.4 Publishing the configuration

The configuration will only really be used by the application after it has been published, so after editing the configuration, you need to publish it.

If you find that there is no publish button, you can ask the project administrator to authorize it.

1. Click the "Publish button"
   ![publish-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/hermes-portal-publish-entry.png)

2. Fill in the information about the publish and click Publish
   ![publish-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/hermes-portal-publish-detail.png)

## 1.5 Application reads the configuration

After the configuration is successfully published, the application can read the configuration through the Apollo client.

Apollo currently provides a Java client. For more information, please click [Java Client Usage Documentation](en/client/java-sdk-user-guide).

If the application uses other languages, you can also get the configuration by directly accessing the Http interface, for details, please refer to [other-language-client-access-guide](en/client/other-language-client-user-guide)

## 1.6 Rollback published configuration

If you find any problem with the released configuration, you can roll back the configuration read by the client to the previous release by clicking the "Rollback" button.

The rollback mechanism here is similar to the release system, where the rollback operation in the release system rolls back the installed packages deployed to the machine to the previous deployed version, but the code in the code repository is not rolled back, so that development can re-release the code after fixing it.

The rollback in Apollo is a similar mechanism. Clicking rollback rolls back the configuration published to the client to the previous published version, which means that the configuration read by the client will be restored to the previous version, but the configuration in the edited state on the page will not be rolled back, so that the developer can re-publish after fixing the configuration.

## 1.7 Configuration queries (administrator privileges)

After a configuration has been added or modified, the administrator user can make a query for the configuration item it belongs to as well as jump to modifications by going to the `Administrator Tools - Global Search for Value` page.

The query here is a fuzzy search, where at least one of the key and value of the configuration item is searched to find out in which application, environment, cluster, namespace the configuration is used.

- Properties format configuration can be retrieved directly from the key and value

![Configuration query-properties](../images/Configuration query-properties.png)

- xml, json, yml, yaml, txt and other formats configuration, because the storage of content-value storage, so you can key = content, value = configuration item content, retrieval

![Configuration query-Non properties](../images/Configuration query-Non properties.png)

# II. Public component access guide

## 2.1 Difference between public components and common applications

Public components are those client code that are published for use by other applications, such as the CAT client, Hermes Producer client, etc.

Although such components are developed and maintained by other teams, the runtime is within the actual business application, so they can essentially be considered part of the application.

Usually, the configuration used by these components is maintained by the original development team, but since the runtime and environment of the actual application vary, we also allow the application to override some of the configuration of the public components when they are actually used.

## 2.2 Steps to access public components

The access steps for public components are almost identical to those for normal applications, the only difference being that public components need to create their own unique Namespace.

So, first perform the following steps in the common application access document, and then follow the steps later in this section.

1. [Create Project](en/portal/apollo-user-guide?id=_11-creating-a-project)
2. [Project administrator privileges](en/portal/apollo-user-guide?id=_121-project-administrator-privileges)

### 2.2.1 Creating Namespace

If you find no Add Namespace button, you can find the project administrator to authorize it.

1. Click Add Namespace on the left side of the page
   * ![create-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-namespace.png)

2. Click "Create New Namespace".
   * ![create-namespace-select-type](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-namespace-select-type.png)

3. Enter the namespace name for the public component. Note that the namespace name is globally unique.
   * Apollo will add the department name at the top by default
   * ![create-namespace-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-namespace-detail.png)

4. After clicking Submit, the page will automatically jump to the Associated Namespace page
   * First, select all the environments and clusters that need to have this Namespace, it is generally recommended to select all of them.
   * Second, select the namespace you just created.
   * Finally, click Submit
   Finally, click Submit * ![link-namespace-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/link-namespace-detail.png)

5. After successful linking, the page will automatically jump to the Namespace permission management page

   1. Assign the permission to modify the namespace 
      - ​	![namespace-permission-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-edit.png)

   2. Assign permission to publish
      - ​	![namespace-publish-permission](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-publish-permission.png)

6. Click "Back" to return to the project page

### 2.2.2 Adding configuration items

To edit the configuration, you need to have the edit permission of this Namespace. If you find that there is no Add Configuration button, you can find the project administrator to authorize it.

#### 2.2.2.1 Adding configuration via form mode

1. Click Add Configuration
   ![public-namespace-edit-item-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-edit-item-entry.png)

2. Enter the configuration items
   ![public-namespace-edit-item](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-edit-item.png)

3. Click Submit
   ![public-namespace-item-created](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-item-created.png)

#### 2.2.2.2 Edit by text mode

This part is the same as normal application, please refer to [1.3.2 Editing via text mode](en/portal/apollo-user-guide?id=_132-editing-via-text-mode) for the detailed steps.

### 2.2.3 Publish configuration

The configuration will only really be used by the application after it has been published, so after editing the configuration, it needs to be published.

If you find that there is no publish button, you can ask the project administrator to authorize it.

1. Click the "Publish button"
   ![public-namespace-publish-items-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-publish-items-entry.png)

2. Fill in the information about publishing and click Publish
   ![public-namespace-publish-items](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/public-namespace-publish-items.png)

### 2.2.4 Application reading configuration

Once the configuration is successfully published, the application can read the configuration through the Apollo client.

Apollo currently provides a Java client. For more information, click [Java Client Usage Documentation](en/client/java-sdk-user-guide).

If the application uses other languages, you can also get the configuration by directly accessing the Http interface, for details, please refer to [other-language-client-access-guide](en/client/other-language-client-user-guide)

For reading the configuration of public components, you can refer to the "Getting the Configuration of Public Namespace" section in the above document.

## 2.3 Application Override Public Component Configuration Steps

As mentioned earlier, the configuration of public components is usually maintained by the original development team, but since the actual application runtime and environment vary, we also allow applications to override some of the configuration of public components when they are actually used.

Here is how the application can override the configuration of the public component. For simplicity, assume that the apollo-portal application uses the hermes producer client and wants to adjust the batch send size of hermes.

### 2.3.1 Associating the public component Namespace

1. Go to the home page of the application project that uses the public component and click the Add Namespace button on the left
   * So, in this example, we need to go to the home page of apollo-portal.
   * (Adding Namespace requires project administrator privileges, if you find no Add Namespace button, you can find the project administrator to authorize it)
   * ![link-public-namespace-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/link-public-namespace-entry.png)

2. Find the namespace of the hermes producer and select which environments and clusters to associate with it
   ![link-public-namespace](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/link-public-namespace.png)

3. After successful linking, the page will automatically jump to the Namespace permission management page
   1. Assign modify permission
      ![namespace-permission-edit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-permission-edit.png)
   2. Assign permission to publish
      ![namespace-publish-permission](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-publish-permission.png)

4. Click "Back" to return to the project page

### 2.3.2 Overriding common component configuration

1. Click Add Configuration
   ![override-public-namespace-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/override-public-namespace-entry.png)

2. Enter the configuration items to be overridden
   ![override-public-namespace-item](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/override-public-namespace-item.png)

3. Click Submit
   ![override-public-namespace-item-done](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/override-public-namespace-item-done.png)

### 2.3.3 Publish configuration

The configuration will only really be used by the application after it is published, so after editing the configuration, it needs to be published.

To publish the configuration, you need to have the publish permission of this Namespace, if you find that there is no publish button, you can ask the project administrator to authorize it.

1. Click the "Publish button"
   ![override-public-namespace-item-publish-entry](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/override-public-namespace-item-publish-entry.png)

2. Fill in the information about the publication and click Publish
   ![override-public-namespace-item-publish](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/override-public-namespace-item-publish.png)

3. After the configuration is successfully published, the value of sender.batchSize read by the hermes producer client when running inside the apollo-portal application is 1000.

# III. Cluster independent configuration instructions

In some special cases, the application has the need to do different configurations for different clusters, such as the application deployed in server room A connects to a different es server address than the application deployed in server room B connects to a different es server address.

In this case, it can be solved by creating different clusters in Apollo.

## 3.1 Creating a cluster

If you find that there is no Add Cluster button, you can ask the project administrator for authorization.

1. Click the "Add Cluster" button on the left side of the page
   * ![create-cluster](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-cluster.png)

2. Enter the cluster name, select the environment and submit
   * ![create-cluster-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/create-cluster-detail.png)

3. Switch to the corresponding cluster, modify the configuration and release it
   * ![config-in-cluster-created](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/cluster-created.png)

4. With the above configuration, the application deployed in the SHAJQ server room will read the configuration under the SHAJQ cluster

5. If the application is also deployed in other server rooms, then with the above configuration, the configuration under the default cluster will be read.

# IV. Using the same configuration for multiple AppId

In some cases, although the application itself is not a public component, it still needs to share the same configuration among multiple AppId, such as different projects of the same product: XX-Web, XX-Service, XX-Job and so on.

In this case, if you want to implement multiple AppId to use the same configuration, the basic concept is the same as the configuration of public components.

Specifically, we create a namespace under one of the AppId, write the public configuration information, and then read the configuration of the namespace in each project.

If an AppId needs to override the public configuration information, then associate a public namespace under that AppId and write the configuration that needs to be overridden.

The specific steps can be referred to [Public Component Access Guide](en/portal/apollo-user-guide?id=ii-public-component-access-guide).

# V. Grayscale publishing usage guide

With the grayscale release function, you can achieve.

1. For some configurations that have a relatively large impact on the program, you can first take effect in one or more instances, and observe no problem for a period of time before releasing the configuration in full.
2. For some configuration parameters that need to be tuned, A/B testing can be achieved through the grayscale release function. You can apply different configurations on different machines and keep tuning and evaluating for a period of time to find out the better configuration before releasing the configuration in full.

The following is a practical example to describe how to use the grayscale release function.

## 5.1 Introduction to the scenario

The 100004458 (apollo-demo) project has two clients.

1. 10.32.21.19
2. 10.32.21.22

![initial-instance-list](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/initial-instance-list.png)

**Grayscale targets:**

* Currently there is a configuration timeout=2000, we want to grayscale release timeout=3000 for 10.32.21.22 and still timeout=2000 for 10.32.21.19.

![initial-config](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/initial-config.png)

## 5.2 Creating greyscale

First click the `create grayscale` button in the top right corner of the application namespace.

![create-gray-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/create-gray-release.png)

After clicking OK, the grayscale version is created successfully and the page will automatically switch to the `grayscale version` tab.

![initial-gray-release-tab](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/initial-gray-release-tab.png)

## 5.3 Grayscale configuration

Click on the `Configure main version`, the `Gray this configuration` button on the far right of the timeout configuration

![initial-gray-release-tab](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/edit-gray-release-config.png)

Fill in the popup box with the value to be grayed out: 3000 and click Submit.

![submit-gray-release-config](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/submit-gray-release-config.png)

![gray-release-config-submitted](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-config-submitted.png)

After the grayscale configuration, confirm the grayscale configuration and the major version configuration.

Click on the `Grayscale Publish` button.

![click-gray-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/click-gray-release.png)

Confirm the grayscale configuration to be released by comparing the value of the major version and the published value of the grayscale version.

![gray-release-diff-items](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-diff-items.png)

## 5.4 Configuring grayscale rules

Switch to the `Gray Rule` Tab and click the `Add Rule` button

![new-gray-release-rule](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/new-gray-release-rule.png)

In the popup box `Grayed IP` dropdown box will show the list of machines currently using the configuration by default, select the IP we want to gray.

![select-gray-release-ip](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/select-gray-release-ip.png )

![gray-release-ip-selected](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-ip-selected.png)

In addition to the IP dimension, from version 2.0.0 onwards there is also support for identifying the list of instances in grayscale by label, which is suitable for scenarios where the IP is not fixed such as `Kubernetes`.

Manually enter the label you want to set, and click the Add button when you're done.

![manual-input-gray-release-label](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/manual-input-gray-release-label.png)

![manual-input-gray-release-label-2](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/manual-input-gray-release-label2.png)

![gray-release-rule-saved](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-rule-saved.png)

After the above rules are configured, the grayed configuration will take effect for instances with AppId `100004458`, IP `10.32.21.22` or `Label` marked as `myLabel` or `appLabel`.

> For more information on how to label `Label`, you can refer to the configuration instructions in [ApolloLabel](en/client/java-sdk-user-guide?id=_1247-apollo-label).

If the required IP is not found in the drop-down box, it means that the machine has not yet taken the configuration from Apollo, you can enter it by clicking Enter IP manually, and click the Add button after entering the IP.

![manual-input-gray-release-ip](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/manual-input-gray-release-ip.png)

![manual-input-gray-release-ip-2](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/manual-input-gray-release-ip-2.png)

>Note: For the grayscale rule of public Namespace, you need to specify the appId to be grayscale first, then select the IP and Label.

## 5.5 Grayscale Release

The configuration rules are in effect, but the grayscale configuration has not been published yet. Switch to the `Configuration` Tab.

Check the grayscale configuration section again, and if there are no problems, click `Grayscale Publish`.

![prepare-to-do-gray-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/prepare-to-do-gray-release.png)

In the popup box, you can see that the value of the master version is 2000 and the value of the gray version to be released is 3000. fill in the other information and click on release.

![gray-release-confirm-dialog](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-confirm-dialog.png)

After the release, switch to the `gray instance list` Tab and you can see that 10.32.21.22 has used the values of the gray release.

![gray-release-instance-list](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/gray-release-instance-list.png)

Switch to the `instance list` of the `master release` and you will see that only 10.32.21.19 of the master configuration is in use.

![master-branch-instance-list](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/master-branch-instance-list.png)

You can continue with configuration changes or rule changes later. Configuration changes need to click Gray Release before they take effect, and rule changes take effect in real time after the rule is clicked to complete.

## 5.6 Full Release

If the grayscale configuration is tested down ideally and meets expectations, then you can operate `full release`.

The effect of a full release is that

1. The grayscale configuration will be merged back to the main version, in this case, the timeout of the main version will be updated to 3000
2. The configuration of the main version will be automatically published once
3. In the full release page, you can choose whether to keep the current grayscale version, the default is not to keep.

![prepare-to-full-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/prepare-to-full-release.png)

![full-release-confirm-dialog](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/full-release-confirm-dialog.png)

![full-release-confirm-dialog-2](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/full-release-confirm-dialog-2.png) 

I chose not to keep the grayscale version, so the effect after the release is that the configuration of the master version is updated and the grayscale version is deleted. Clicking on the instance list of the main version, you can see that 10.32.21.22 and 10.32.21.19 both use the latest configuration of the main version.

![master-branch-instance-list-after-full-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/master-branch-instance-list-after-full-release.png)

## 5.7 Giving up grayscale

If the grayscale version is not ideal or not needed anymore, you can click `Drop Grayscale`.

![abandon-gray-release](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/abandon-gray-release.png)

## 5.8 Release History

Click the `release history` button of the main release to see the release history of the current namespace's main release as well as the grayscale version.

![view-release-history](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/view-release-history.png)

![view-release-history-detail](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/gray-release/view-release-history-detail.png)



# VI、Other Configurations

## 6.1 Configure view permissions

Starting from version 1.1.0, apollo-portal has added support for view permissions, which allows you to configure an environment to allow only project members to view the private Namespace configuration.

The project members here are :

1. The project's administrator
2. Have the permission to modify or publish the private Namespace in that environment

The configuration is very simple. After logging in with your super administrator account, go to the `Administrator Tools - System Parameters` page and add or modify the `configView.memberOnly.envs` configuration item.

![configView.memberOnly.envs](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/configure-view-permissions.png)

## 6.2 Configuring access keys

Apollo has added an access key mechanism since version 1.6.0, so that only authenticated clients can access sensitive configurations. If the application has access keys enabled, the client needs to configure the keys, otherwise the configuration cannot be accessed.

1. The project administrator opens the Manage Keys page
   ![Admin Key Portal](https://user-images.githubusercontent.com/837658/94990081-f4d3cd80-05ab-11eb-9470-fed5ec6de92e.png)

2. Generate an access key for each environment of the project, note that it is disabled by default, and it is recommended to turn it on after the clients are all configured
   ![Key Configuration Page](https://user-images.githubusercontent.com/837658/94990150-788dba00-05ac-11eb-9a12-727fdb872e42.png)

3. Client-side [configure access key](en/client/java-sdk-user-guide?id=_1244-configuring-access-keys) .

## 6.3 System parameterization of global search configuration items

Starting from version 2.4.0, apollo-portal adds the ability to globally search for configuration items by fuzzy retrieval of the key and value of a configuration item to find out which application, environment, cluster, or namespace the configuration item with the corresponding value is used in. In order to prevent memory overflow (OOM) problems when performing global view searches of configuration items, we introduce a system parameter `apollo.portal.search.perEnvMaxResults`, which is used to limit the number of maximum search results per environment configuration item in a single search. By default, this value is set to `200`, but administrators can adjust it to suit their actual needs.

**Setting method：**

1. Log in to the Apollo Configuration Center interface with a super administrator account.
2. Just go to the `Administrator Tools - System Parameters` page and add or modify the `apollo.portal.search.perEnvMaxResults` configuration item.

Please note that modifications to system parameters may affect the performance of the search function, so you should perform adequate testing and ensure that you understand exactly what the parameters do before making changes.

![System-parameterization-of-global-search-configuration-items](../images/System-parameterization-of-global-search-configuration-items.png)



## 6.4 Parameter settings for limiting the number of namespaces in the appld+cluster dimension

Starting from version 2.4.0, apollo-portal provides the function of checking the upper limit of the number of namespaces that can be created under the appld+cluster dimension. This function is disabled by default and needs to be enabled by configuring the system `namespace.num.limit.enabled`. At the same time, the system parameter `namespace.num.limit` is provided to dynamically configure the upper limit of the number of Namespaces under the appld+cluster dimension. The default value is 200. Considering that some basic components such as gateways, message queues, Redis, and databases require special processing, a new system parameter `namespace.num.limit.white` is added to configure the verification whitelist, which is not affected by the upper limit of the number of Namespaces.

**Setting method：**

1. Log in to the Apollo Configuration Center interface with a super administrator account.
2. Go to the `Administrator Tools - System Parameters - ConfigDB Configuration Management` page and add or modify the `namespace.num.limit.enabled` configuration item to true/false to enable/disable this function. It is disabled by default.

   ![item-num-limit-enabled](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-num-limit-enabled.png)

3. Go to the `Administrator Tools - System Parameters - ConfigDB Configuration Management` page to add or modify the `namespace.num.limit` configuration item to configure the upper limit of the number of namespaces under a single appld+cluster. The default value is 200

   ![item-num-limit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-num-limit.png)

4. Go to `Administrator Tools - System Parameters - ConfigDB Configuration Management` page to add or modify the `namespace.num.limit.white` configuration item to configure the whitelist for namespace quantity limit verification. Multiple AppIds are separated by English commas.

   ![item-num-limit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/namespace-num-limit-white.png)


## 6.5 Limitation on the number of configuration items in a single namespace
Starting from version 2.4.0, apollo-portal provides the function of limiting the number of configuration items in a single namespace. This function is disabled by default and needs to be enabled by configuring the system `item.num.limit.enabled`. At the same time, the system parameter `item.num.limit` is provided to dynamically configure the upper limit of the number of items in a single Namespace.

**Setting method：**
1. Log in to the Apollo Configuration Center interface with a super administrator account
2. Go to the `Administrator Tools - System Parameters - ConfigDB Configuration Management` page and add or modify the `item.num.limit.enabled` configuration item to true/false to enable/disable this function.

   ![item-num-limit-enabled](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/item-num-limit-enabled.png)
3. Go to the `Admin Tools - System Parameters - ConfigDB Configuration Management` page and add or modify the `item.num.limit` configuration item to configure the upper limit of the number of items under a single Namespace.

   ![item-num-limit](https://cdn.jsdelivr.net/gh/apolloconfig/apollo@master/doc/images/item-num-limit.png)


# VII. Best practices

## 7.1 Security Related

As a basic service, the configuration center stores very important configuration information of the company, so security factors need to be focused on, some considerations are listed below for your reference, and you are also welcome to share your own practice cases.

### 7.1.1 Authentication

It is recommended to access the company's unified authentication system, such as SSO, LDAP, etc. The access method can be found in [Portal to implement user login function](en/extension/portal-how-to-implement-user-login-function)

> If you use Spring Security simple authentication provided by Apollo, you must remember to change the password of super administrator apollo

### 7.1.2 Authorization

Apollo supports fine-grained permissions control, so please make sure to control the permissions according to the actual situation: 1.

1. [Project administrator privileges](en/portal/apollo-user-guide?id=_121-project-administrator-privileges)
   * Apollo allows all logged-in users to create projects by default. If you only allow some users to create projects, you can turn on [Create project permission control](en/deployment/distributed-deployment-guide?id=_3110-rolecreate-applicationenabled-whether-to-enable-control-over-creating-project-permissions)
2. [Configure editing and publishing privileges](en/portal/apollo-user-guide?id=_122-configuring-editing-and-publishing-permissions)
   * Configuration editing and publishing privileges support configuration by environment, for example, the development environment developers can complete the process of configuration editing and publishing by themselves, but the production environment publishing privileges to the test or operation and maintenance personnel
   * It is recommended to turn on [release audit](en/deployment/distributed-deployment-guide?id=_322-namespacelockswitch-only-one-person-can-modify-the-switch-at-a-time-for-release-review) at the same time for production environment, so as to control that only one person can modify a configuration release and another person can release it. This ensures that configuration changes are adequately checked.
3. [Configuration view permissions](en/portal/apollo-user-guide?id=_61-configure-view-permissions)
   * You can specify that only project members of an environment are allowed to view the configuration of a private Namespace, thus avoiding sensitive configuration leaks, such as production environments

### 7.1.3 System access

In addition to user permissions, system access also needs to be considered in terms of.

1. `apollo-configservice` and `apollo-adminservice` are designed based on the intranet trusted network, so for security reasons, `apollo-configservice` and `apollo-adminservice` are prohibited from being exposed directly to the public network
2. For sensitive configurations, consider enabling [access secret key](en/portal/apollo-user-guide?id=_62-configuring-access-keys) so that only authenticated clients can access sensitive configurations
3. version 1.7.1 and above can consider enabling [access control](en/deployment/distributed-deployment-guide?id=_326-admin-servicesaccesscontrolenabled-configure-whether-apollo-adminservice-has-access-control-enabled) for `apollo-adminservice`, so that only [controlled](en/deployment/distributed-deployment-guide?id=_3112-admin-servicesaccesstokens-set-the-access-token-required-by-apollo-portal-to-access-the-apollo-adminservice-for-each-environment) `apollo-portal` can access the corresponding interface to enhance security
4. version 2.1.0 and above can consider enabling [access control](en/deployment/distributed-deployment-guide?id=_329-apolloeurekaserversecurityenabled-configure-whether-to-enable-eureka-login-authentication) for `eureka`, so that only controlled `apollo-configservice` and `apollo-adminservice` can be registered to `eureka` to enhance security
