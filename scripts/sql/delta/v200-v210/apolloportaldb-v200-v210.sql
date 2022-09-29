--
-- Copyright 2022 Apollo Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
-- http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--
# delta schema to upgrade apollo config db from v2.0.0 to v2.1.0

Use ApolloPortalDB;

-- add other Config
INSERT INTO `ServerConfig` (`Key`, `Value`, `Comment`)
VALUES
	('email.supported.envs', '', ''),
	('webhook.supported.envs', '', ''),
	('api.connectTimeout', '3000', ''),
	('apollo.portal.address', '', ''),
	('emergencyPublish.supported.envs', '', ''),
	('namespace.publish.tips.supported.envs', '', ''),
	('email.enabled', 'false', ''),
	('email.config.host', '', ''),
	('email.config.user', '', ''),
	('email.config.password', '', ''),
	('email.sender', '', ''),
	('email.template.framework', '', ''),
	('email.template.release.module.diff', '', ''),
	('email.template.rollback.module.diff', '', ''),
	('email.template.release.module.rules', '', ''),
	('wiki.address', 'https://www.apolloconfig.com', ''),
	('role.create-application.enabled', 'false', ''),
	('role.manage-app-master.enabled', 'false', ''),
	('admin-service.access.tokens', '', ''),
	('config.release.webhook.service.url', '', ''),
	('searchByItem.switch', 'true', ''),
	('apollo.portal.auth.user-password-not-allow-list', '', '');