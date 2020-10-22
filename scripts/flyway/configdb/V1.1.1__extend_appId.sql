# delta schema to upgrade apollo config db from v1.7.0 to v1.8.0

Use ApolloConfigDB;
alter table `AppNamespace`  change AppId AppId varchar(64) NOT NULL DEFAULT 'default' COMMENT 'app id';
alter table `Cluster`  change AppId AppId varchar(64) NOT NULL DEFAULT 'default' COMMENT 'app id';
alter table `GrayReleaseRule`  change AppId AppId varchar(64) NOT NULL DEFAULT 'default' COMMENT 'app id';
alter table `Instance`  change AppId AppId varchar(64) NOT NULL DEFAULT 'default' COMMENT 'app id';
alter table `InstanceConfig` change ConfigAppId ConfigAppId varchar(64) NOT NULL DEFAULT 'default' COMMENT 'Config App Id';
alter table `ReleaseHistory`  change AppId AppId varchar(64) NOT NULL DEFAULT 'default' COMMENT 'app id';
