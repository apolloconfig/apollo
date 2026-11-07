Changes by Version
==================
Release Notes.

Apollo 3.1.0

------------------
* [Change: migrate the deprecated stale bot to GitHub Actions](https://github.com/apolloconfig/apollo/pull/5690)
* [Change: standardize Maven POM formatting with Spotless SortPom](https://github.com/apolloconfig/apollo/pull/5702)
* [Docs: clarify consumer token salt configuration and existing-token rotation](https://github.com/apolloconfig/apollo/pull/5703)
* [Fix: change GrayReleaseRule.Rules to TEXT to avoid utf8mb4 row-size 1118](https://github.com/apolloconfig/apollo/pull/5699)
* [Fix createOrUpdateItem failing when the item key does not exist](https://github.com/apolloconfig/apollo/pull/5707)
* [Fix: reject Portal startup without configured authentication](https://github.com/apolloconfig/apollo/pull/5704)
* [Fix: restore Portal shared sessions after the Spring Boot 4 migration](https://github.com/apolloconfig/apollo/pull/5706)
* [Fix: isolate the release history test database in apollo-biz](https://github.com/apolloconfig/apollo/pull/5712)
* [Fix: resolve bearer token conflict between resource server jwt and user tokens](https://github.com/apolloconfig/apollo/pull/5711)

------------------
All issues and pull requests are [here](https://github.com/apolloconfig/apollo/milestone/22?closed=1)
