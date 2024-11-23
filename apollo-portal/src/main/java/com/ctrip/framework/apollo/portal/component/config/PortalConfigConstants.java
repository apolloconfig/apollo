package com.ctrip.framework.apollo.portal.component.config;

/**
 * This class holds all the constants used in the config classes
 */
class PortalConfigConstants {

     private PortalConfigConstants() {}

     // Configuration keys
     static final String APOLLO_PORTAL_ENVS = "apollo.portal.envs";
     static final String PER_ENV_MAX_RESULTS = "apollo.portal.search.perEnvMaxResults";
     static final String APOLLO_META_SERVERS = "apollo.portal.meta.servers";
     static final String SUPER_ADMIN = "superAdmin";
     static final String EMAIL_SUPPORTED_ENVS = "email.supported.envs";
     static final String WEBHOOK_SUPPORTED_ENVS = "webhook.supported.envs";

     // API properties
     static final String API_CONNECTION_TIMEOUT = "api.connectTimeout";
     static final String API_READ_TIMEOUT = "api.readTimeout";
     static final String API_TIME_TO_LIVE = "api.connectionTimeToLive";
     static final String API_POOL_MAX_TOTAL = "api.pool.max.total";
     static final String API_POOL_MAX_PER_ROUTE = "api.pool.max.per.route";

     // Email template properties
     static final String EMAIL_TEMPLATE_FRAMEWORK = "email.template.framework";
     static final String EMAIL_RELEASE_MODULE_DIFF = "email.template.release.module.diff";
     static final String EMAIL_ROLLBACK_MODULE_DIFF = "email.template.rollback.module.diff";
     static final String EMAIL_RELEASE_MODULE_RULES = "email.template.release.module.rules";

     // Email properties
     static final String EMAIL_ENABLED = "email.enabled";
     static final String EMAIL_CONFIG_HOST = "email.config.host";
     static final String EMAIL_CONFIG_USER = "email.config.user";
     static final String EMAIL_CONFIG_PASSWORD= "email.config.password";
     static final String EMAIL_SENDER = "email.sender";

     static final String DEFAULT_ORGANIZATION_KEY = "organizations";
     static final String DEFAULT_PORTAL_ADDRESS_KEY = "apollo.portal.address";

     // Admin server keys
     static final String REFRESH_ADMIN_SERVER_NORMAL_INTERVAL_KEY = "refresh.admin.server.address.task.normal.interval.second";
     static final String REFRESH_ADMIN_SERVER_OFFLINE_INTERVAL_KEY = "refresh.admin.server.address.task.offline.interval.second";
     static final String ADMIN_SERVICE_ACCESS_TOKENS_KEY = "admin-service.access.tokens";

     // Other keys
     static final String CONFIG_VIEW_MEMBER_ONLY_ENVS_KEY = "configView.memberOnly.envs";
     static final String EMERGENCY_PUBLISH_SUPPORTED_ENVS_KEY = "emergencyPublish.supported.envs";
     static final String NAMESPACE_PUBLISH_TIPS_SUPPORTED_ENVS_KEY = "namespace.publish.tips.supported.envs";
     static final String CONFIG_RELEASE_WEBHOOK_SERVICE_URL_KEY = "config.release.webhook.service.url";
     static final String SEARCH_BY_ITEM_SWITCH_KEY = "searchByItem.switch";

     // Consumer keys
     static final String CONSUMER_TOKEN_SALT_KEY = "consumer.token.salt";
     static final String CONSUMER_TOKEN_DEFAULT_VALUE = "apollo-portal";

     // Wiki address keys
     static final String WIKI_ADDRESS_KEY = "wiki.address";
     static final String WIKI_ADDRESS_DEFAULT_VALUE = "https://www.apolloconfig.com";
     static final String ADMIN_PRIVATE_NAMESPACE_SWITCH = "admin.createPrivateNamespace.switch";

     // User password not allowed list key
     static final String AUTH_USER_PASSWORD_NOT_ALLOW_LIST_KEY = "apollo.portal.auth.user-password-not-allow-list";
}
