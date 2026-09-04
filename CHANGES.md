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
* [Change: migrate Apollo Portal config item UI operations to OpenAPI](https://github.com/apolloconfig/apollo/pull/5610)
* [Change: migrate Apollo Portal namespace core UI operations to OpenAPI](https://github.com/apolloconfig/apollo/pull/5612)
* [Change: migrate Apollo Portal release, branch, and instance UI operations to OpenAPI](https://github.com/apolloconfig/apollo/pull/5616)
* [Change: migrate Apollo Portal permission and AccessKey UI operations to OpenAPI](https://github.com/apolloconfig/apollo/pull/5617)
* [Change: complete Apollo Portal UI management OpenAPI migration](https://github.com/apolloconfig/apollo/pull/5618)
* [Feature: allow explicitly authorized consumer tokens to manage portal users](https://github.com/apolloconfig/apollo/pull/5623)
* [Feature: add user access tokens for AI agents and automation](https://github.com/apolloconfig/apollo/pull/5632)
* [Fix: return 429 instead of redirecting to sign-in for OpenAPI user token rate limits](https://github.com/apolloconfig/apollo/pull/5637)
* [Fix: physically delete retained release history and unreferenced release rows](https://github.com/apolloconfig/apollo/pull/5641)
* [Fix: preserve item type when revoking unpublished changes](https://github.com/apolloconfig/apollo/pull/5646)
* [Feature: support revoking unpublished changes for non-properties namespaces](https://github.com/apolloconfig/apollo/pull/5656)
* [Feature: add formatted and raw JSON views in Portal](https://github.com/apolloconfig/apollo/pull/5657)
* [Feature: support configurable OIDC username claim (`spring.security.oidc.user-id-claim-name`) for the Apollo user identity](https://github.com/apolloconfig/apollo/pull/5655)
* [Fix: strict JSON/YAML well-formedness validation at the authoritative save path](https://github.com/apolloconfig/apollo/pull/5660)
* [Feature: add batch create/update/delete OpenAPI operations for namespace items](https://github.com/apolloconfig/apollo/pull/5665)
* [Change: upgrade Apollo server baseline to Spring Boot 4.1.1 and Spring Cloud 2025.1.3](https://github.com/apolloconfig/apollo/pull/5671)
* [Fix: preserve last-modified metadata of unchanged items when revoking changes](https://github.com/apolloconfig/apollo/pull/5672)

------------------
All issues and pull requests are [here](https://github.com/apolloconfig/apollo/milestone/18?closed=1)
