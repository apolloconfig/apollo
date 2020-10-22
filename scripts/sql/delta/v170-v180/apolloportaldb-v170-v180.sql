# delta schema to upgrade apollo portal db from v1.7.0 to v1.8.0

Use ApolloPortalDB;

alter table `AppNamespace`  change AppId AppId varchar(64) NOT NULL DEFAULT 'default' COMMENT 'app id';
