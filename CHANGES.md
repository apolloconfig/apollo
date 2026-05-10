Changes by Version
==================
Release Notes.

Apollo 3.0.0

------------------
* [Fix: include super admin in hasAnyPermission semantics](https://github.com/apolloconfig/apollo/pull/5568)
* [Change: official Config/Admin packages now default to database discovery; upgraded Eureka deployments should explicitly keep the `github` profile to preserve legacy behavior](https://github.com/apolloconfig/apollo/pull/5580)
* [Refactor: extract config constants and methods in BizConfig, PortalConfig, and RefreshableConfig](https://github.com/apolloconfig/apollo/pull/5583)
* [Change: migrate Apollo server baseline to Spring Boot 4.0.x, align Spring Cloud discovery integrations, and add external discovery smoke workflow](https://github.com/apolloconfig/apollo/pull/5585)
* [Feature: auto-provision an enabled AccessKey per environment when creating a new app, controlled by `apollo.access-key.auto-provision.enabled` in ApolloConfigDB.ServerConfig](https://github.com/apolloconfig/apollo/pull/5589)
* [Feature: support ServerConfig create/update/delete by `key + cluster` in ConfigDB management, with UI cluster awareness and multi-cluster safety tests](https://github.com/apolloconfig/apollo/pull/5601)
* [Change: adapt Apollo Portal OpenAPI migration to apollo-openapi v0.2.0](https://github.com/apolloconfig/apollo/pull/5608)

------------------
All issues and pull requests are [here](https://github.com/apolloconfig/apollo/milestone/18?closed=1)
