# &nbsp;



# Background

Current documentation describes how to install a high availability Apollo cluster with one-click through [Rainbond](https://www.rainbond.com/?channel=apollo), a cloud native application management platform. This approach is suitable for users who are less familiar with Kubernetes, containerization and other complex technologies, and lowers the barrier to deploy Apollo in Kubernetes.

## Combination of Rainbond and Apollo

Rainbond is an easy to use open source cloud native application management platform. 

With the help of it, users can complete the deployment, operation and maintenance of microservices in a graphical interface. 

With the help of Kubernetes and containerization technology, automatic operation and maintenance capabilities such as fault self-healing and elastic expansion can be endowed to users' applications.

Rainbond has a built-in native Service Mesh microservice framework, and also has a good integration experience with other microservice frameworks such as Spring Cloud and Dubbo. 

Therefore, a large number of Rainbond users may also be users of the Apollo configuration center. 

Instead of worrying about how to deploy a Apollo cluster, the Rainbond team made Apollo a one-click application template for free download and installation by open source users. 

This installation method greatly reduces the deployment burden of users using Apollo clusters. Currently, versions 1.9.2 is supported.

The current installation mode integrates a set of `PRO` environments by default. Instructions to add additional environments is described in the advanced features section later.

## About application template

Application template is a package manager for Rainbond cloud native application management platform. Users can install applications into Rainbond with one-click. No matter how complex the application is, the application template abstracts it into an package, which is installed with docker images of all the components, configuration information, and relationships between all the components.


# Prerequisite

- Deployed Rainbond cloud native application management platform. For example: The [Quick Start](https://www.rainbond.com/docs/quick-start/quick-install/?channel=apollo) can be used to run in a PC within a container.

- Internet connection.

# Quick Start

## Access the built-in open source app store

Select the **App Store** on the left, switch to the **Open Source App Store**, and search **apollo** to find the Apollo application.

![apollo-1](https://static.goodrain.com/wechat/apollo/apollo-1.png)

## One-click install

Click **Install** on the right of Apollo to enter the installation page. After filling in simple information, click **OK** to start the installation, and the page automatically jumps to the topology view.

![apollo-2](https://static.goodrain.com/wechat/apollo/apollo-2.png)

Parameters：

| Options  | Instructions                                 |
| ---- | ---------------------------------- |
| team name | User-defined workspace isolated by namespace                 |
| cluster name |  Select which K8s cluster Apollo will be deployed to           |
| application | Select the application to which Apollo will be deployed, which contains several associated components |
| version | Select the version of Apollo, the useable version is 1.9.2       |

After a few minutes, the Apollo is installed and up and running.


![apollo-3](https://static.goodrain.com/wechat/apollo/apollo-3.png)

## Testing

Access the default domain provided by component `Apollo-portal-1.9.2` to log into the Apollo console and verify in system information that the `PRO` environment is ready.

![apollo-4](https://static.goodrain.com/wechat/apollo/apollo-4.png)

## Configuration

In Rainbond, Apollo clusters can be configured based on a graphical interface. It mainly includes three parts: environment variables, configuration file mounting and plugin configuration.

- Environment variables: You can customize environment variables through environment configuration in different component pages. For example, `Apollo-portal-1.9.2` adds `APOLLO_PORTAL_ENVS=pro` by default to define the environment managed by the current portal.

- Configuration files: You can set configuration files for components in environment configuration on different component pages.
  
  - `Apollo-portal-1.9.2` mount `/apollo-portal/config/apollo-env.properties` is used to define meta addresses for different environments.
  
  - `Apollo-config-1.9.2` mount `/apollo-configservice/config/application-github.properties` used to declare the current config and admin service address.

- Plugin configuration: The downstream call address is defined in Rainbond by installing the `Egress Network Governance Plugin` for `Apollo-portal-1.9.2` `Apollo-config-1.9.2`, which is an implementation of Service Mesh microservice governance. By defining the domain name of the downstream service, you can access the specified port of the downstream service. For example, in the `Apollo-portal-1.9.2` plugin, the domain name to access port `Apollo-config-1.9.2` 's 8080 is `Apollo-config-pro`, which is why the configuration only defines the domain name and does not need to define the port.

# Advanced

## Instance number scaling

The `Apollo-portal-1.9.2` `Apollo-config-1.9.2` `Apollo-admin-1.9.2` components included in the Apollo configuration center are deployed using a Deployment controller, Service discovery and communication are realized through Rainbond's built-in Service Mesh microservice framework. Therefore, all three components can scale multiple instances with one click for clustered deployment.

Take `Apollo-portal-1.9.2` as an example, click **scale**, modify the number of **instances**, and click **Set**.

![apollo-5](https://static.goodrain.com/wechat/apollo/apollo-5.png)

## Add envs

The Apollo configuration center supports multiple environments and manages them using a unified Portal page. Apollo clusters based on Rainbond's one-click installation ship with the 'PRO' environment by default. In the Rainbond scenario, I'll show you how to append a set of 'DEV' environments. Access the `Apollo-config-dev` and `Apollo-admin-dev` components, respectively.

1. Deploy another Set of Apollo clusters and remove `Apollo-portal-1.9.2` `ApolloPortalDB` components from the new cluster. Change the name of `Apollo-config-1.9.2` `Apollo-admin-1.9.2` component for ease of administration. Add a dependency from   `Apollo-portal-1.9.2` to `Apollo-config-dev` `Apollo-admin-dev`. The topology is shown as follows：

> Note that this step will trigger a connection information environment variable conflict, remember to redefine your preferred name for the `Apollo-config-dev` `Apollo-admin-dev` component's internal port.

![apollo-6](https://static.goodrain.com/wechat/apollo/apollo-6.png)

2. In **environment configuration** page, modify `Apollo-config-Dev` configuration file `/apollo-configservice/config/application-github.properties`, Change the service addresses of config and admin to the expected values.

![apollo-10](https://static.goodrain.com/wechat/apollo/apollo-10.png)

3. Go to the `Apollo-config-dev` `Apollo-portal-1.9.2` plugin page, modify the configuration for its `Egress Network Governance Plugin` , Rainbond built-in microservice framework, define the downstream service address by setting Domains. In the case of `Apollo-portal-1.9.2`, an access domain name to `apollo-config-dev` `apollo-admin-dev` needs to be configured.

![apollo-7](https://static.goodrain.com/wechat/apollo/apollo-7.png)

After the configuration is complete, click **update configuration**, `Apollo-config-Dev` can be accessed through the domain name `apollo-config-dev`.

Similarly, `Apollo-config-dev` needs to be configured with an access domain name to `apollo-admin-dev`. Update the configuration after the configuration is complete.

4. Modify the configuration of `Apollo-portal-1.9.2` to join the new `DEV` environment.

Modify the value of environment variable `APOLLO_PORTAL_ENVS` to add to the `dev` environment.

![apollo-8](https://static.goodrain.com/wechat/apollo/apollo-8.png)

Modify the configuration file `/apollo-portal/config/apollo-env.properties` and write the meta address of the `dev` environment.

![apollo-9](https://static.goodrain.com/wechat/apollo/apollo-9.png)

Update the `Apollo-portal-1.9.2` component to make all configurations take effect. View system information to verify that the environment is added.

![apollo-11](https://static.goodrain.com/wechat/apollo/apollo-11.png)
