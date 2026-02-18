Changes by Version
==================
Release Notes.

Apollo 2.5.0

------------------
* [Refactor: align permission validator api between openapi and portal](https://github.com/apolloconfig/apollo/pull/5337)
* [Feature: Provide a new configfiles API to return the raw content of configuration files directly](https://github.com/apolloconfig/apollo/pull/5336)
* [Feature: Enhanced instance configuration auditing and caching](https://github.com/apolloconfig/apollo/pull/5361)
* [Feature: Provide a new open API to return the organization list](https://github.com/apolloconfig/apollo/pull/5365)
* [Refactor: Exception handler adds root cause information](https://github.com/apolloconfig/apollo/pull/5367)
* [Feature: Enhanced parameter verification for edit item](https://github.com/apolloconfig/apollo/pull/5376)
* [Feature: Added a new feature to get instance count by namespace.](https://github.com/apolloconfig/apollo/pull/5381)
* [Bugfix: Remove cluster-related roles and permissions upon deletion](https://github.com/apolloconfig/apollo/pull/5395)
* [Security: Prevent unauthorized access to other users' apps in /apps/by-owner endpoint](https://github.com/apolloconfig/apollo/pull/5396)
* [Fix: Bump h2database and snakeyaml version](https://github.com/apolloconfig/apollo/pull/5406)
* [Bugfix: Correct permission target format to appId+env+namespace/cluster](https://github.com/apolloconfig/apollo/pull/5407)
* [Security: Hide password when registering or modifying users](https://github.com/apolloconfig/apollo/pull/5414)
* [Fix: the logical judgment for configuration addition, deletion, and modification.](https://github.com/apolloconfig/apollo/pull/5432)
* [Feature support incremental configuration synchronization client](https://github.com/apolloconfig/apollo/pull/5288)
* [optimize: Implement unified permission verification logic and Optimize the implementation of permission verification](https://github.com/apolloconfig/apollo/pull/5456)
* [CI: Add code and header formatter by spotless plugin](https://github.com/apolloconfig/apollo/pull/5485)
* [Fix: Operate the AccessKey multiple times within one second](https://github.com/apolloconfig/apollo/pull/5490)
* [Bugfix: Prevent accidental cache deletion when recreating AppNamespace with the same name and appid](https://github.com/apolloconfig/apollo/issues/5502)
* [Feature: Support ordinary users to modify personal information](https://github.com/apolloconfig/apollo/pull/5511)
* [Feature: Support exporting and importing configurations for specified applications and clusters](https://github.com/apolloconfig/apollo/pull/5517)
* [doc: Add rust apollo client link](https://github.com/apolloconfig/apollo/pull/5514)
* [Perf: optimize namespace-related interface](https://github.com/apolloconfig/apollo/pull/5518)
* [Perf: Replace synchronized multimap with concurrent hashmap in NotificationControllerV2 for better performance](https://github.com/apolloconfig/apollo/pull/5532)
* [Feature: Enable graceful shutdown for apollo-adminservice and apollo-configservice](https://github.com/apolloconfig/apollo/pull/5536)
* [Feature: Support search box and fullscreen in namespace text editor](https://github.com/apolloconfig/apollo/pull/5545)
* [CI: Add portal UI Playwright e2e gate on PRs with JDK 17](https://github.com/apolloconfig/apollo/pull/5551)
* [CI: Add portal auth matrix Playwright E2E gate for LDAP and OIDC login flows](https://github.com/apolloconfig/apollo/pull/5557)
* [CI: Add standalone Docker validation workflow with Java 17 runtime image checks](https://github.com/apolloconfig/apollo/pull/5558)
------------------
All issues and pull requests are [here](https://github.com/apolloconfig/apollo/milestone/16?closed=1)
