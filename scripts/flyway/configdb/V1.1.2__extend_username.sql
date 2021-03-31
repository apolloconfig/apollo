# delta schema to upgrade apollo config db from v1.7.0 to v1.8.0

Use ApolloConfigDB;

ALTER TABLE `App`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `AppNamespace`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Audit`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Cluster`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Commit`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `GrayReleaseRule`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Item`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Namespace`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `NamespaceLock`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `Release`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `ReleaseHistory`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `ServerConfig`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';

ALTER TABLE `AccessKey`
    MODIFY COLUMN `DataChange_CreatedBy` VARCHAR(64) NOT NULL DEFAULT 'default' COMMENT '创建人邮箱前缀',
    MODIFY COLUMN `DataChange_LastModifiedBy` VARCHAR(64) DEFAULT '' COMMENT '最后修改人邮箱前缀';
