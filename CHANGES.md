Changes by Version
==================
Release Notes.

Apollo 2.1.0

------------------
* [fix:occur a 400 error request when openapi key's parameter contain "a[0]"](https://github.com/apolloconfig/apollo/pull/4424)
* [Upgrade mysql-connector-java version to fix possible transaction rollback failure issue](https://github.com/apolloconfig/apollo/pull/4425)
* [Remove database migration tool Flyway](https://github.com/apolloconfig/apollo/pull/4361)
* [Optimize Spring-Security Firewall Deny Request Response 400](https://github.com/apolloconfig/apollo/pull/4428)
* [Optimize the UI experience of open platform authorization management](https://github.com/apolloconfig/apollo/pull/4436)
* [Allow users to associate multiple public namespaces at a time](https://github.com/apolloconfig/apollo/pull/4437)
* [Move apollo-demo, scripts/docker-quick-start and scripts/apollo-on-kubernetes out of main repository](https://github.com/apolloconfig/apollo/pull/4440)
* [Add search key when comparing Configuration items](https://github.com/apolloconfig/apollo/pull/4459)
* [A user-friendly user management page for apollo portal](https://github.com/apolloconfig/apollo/pull/4464)
* [Optimize performance of '/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces' interface queries](https://github.com/apolloconfig/apollo/pull/4473)
* [Add a new API to load items with pagination](https://github.com/apolloconfig/apollo/pull/4468)
* [fix(#4474):'openjdk:8-jre-alpine' potentially causing wrong number of cpu cores](https://github.com/apolloconfig/apollo/pull/4475)
* [Switching spring-session serialization mode to json for compatibility with spring-security version updates](https://github.com/apolloconfig/apollo/pull/4484)
* [fix(#4483):Fixed overwrite JSON type configuration being empty](https://github.com/apolloconfig/apollo/pull/4486)
* [Allow users to delete AppNamespace](https://github.com/apolloconfig/apollo/pull/4499)
* [fix the deleted at timestamp issue](https://github.com/apolloconfig/apollo/pull/4493)
* [add configuration processor for portal developers](https://github.com/apolloconfig/apollo/pull/4521)
* [Add a potential json value check feature](https://github.com/apolloconfig/apollo/pull/4519)
* [Add index for table ReleaseHistory](https://github.com/apolloconfig/apollo/pull/4550)
* [Add basic type check for Item value](https://github.com/apolloconfig/apollo/pull/4542)
* [add an option to custom oidc userDisplayName](https://github.com/apolloconfig/apollo/pull/4507)
* [fix openapi item with url illegalKey 400 error](https://github.com/apolloconfig/apollo/pull/4549)
* [fix the exception occurred when publish/rollback namespaces with grayrelease](https://github.com/apolloconfig/apollo/pull/4564)
* [fix create namespace with single dot 500 error](https://github.com/apolloconfig/apollo/pull/4568)
* [Add nodejs client sdk and fix doc](https://github.com/apolloconfig/apollo/pull/4590)
* [Move apollo-core, apollo-client, apollo-mockserver, apollo-openapi and apollo-client-config-data to apollo-java repo](https://github.com/apolloconfig/apollo/pull/4594)
* [fix get the openapi interface that contains namespace information for deleted items](https://github.com/apolloconfig/apollo/pull/4596)
* [A user-friendly config management page for apollo portal](https://github.com/apolloconfig/apollo/pull/4592)
* [feat: support use database as a registry](https://github.com/apolloconfig/apollo/pull/4595)
* [fix doc bug](https://github.com/apolloconfig/apollo/pull/4579)
* [fix Grayscale release Item Value length limit can not be synchronized with its main version](https://github.com/apolloconfig/apollo/pull/4622)
* [feat: use can change spring.profiles.active's value without rebuild project](https://github.com/apolloconfig/apollo/pull/4616)
* [refactor: remove app.properties and move some config file's location](https://github.com/apolloconfig/apollo/pull/4637)
* [Fix the problem of deleting blank items appear at the end](https://github.com/apolloconfig/apollo/pull/4662)
* [Portal-UI adds serverConfig configuration management of ApolloConfigDB](https://github.com/apolloconfig/apollo/pull/4680)
* [Enable login authentication for eureka](https://github.com/apolloconfig/apollo/pull/4663)
* [Add github action to publish docker image](https://github.com/apolloconfig/apollo/pull/4685)

------------------
All issues and pull requests are [here](https://github.com/apolloconfig/apollo/milestone/11?closed=1)
