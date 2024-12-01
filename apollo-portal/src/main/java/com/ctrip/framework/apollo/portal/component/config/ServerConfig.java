package com.ctrip.framework.apollo.portal.component.config;

import com.ctrip.framework.apollo.common.config.RefreshableConfig;
import com.ctrip.framework.apollo.portal.entity.vo.Organization;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.PortalDBPropertySource;
import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class handled server related configs for portalConfig class
 */
@Component
public class ServerConfig extends RefreshableConfig {

    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);

    private static final int DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_NORMAL_INTERVAL_IN_SECOND = 5 * 60; //5min
    private static final int DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_OFFLINE_INTERVAL_IN_SECOND = 10; //10s

    private static final Gson GSON = new Gson();

    private static final Type ORGANIZATION = new TypeToken<List<Organization>>() {}.getType();

    /**
     * meta servers config in "PortalDB.ServerConfig"
     */
    private static final Type META_SERVERS = new TypeToken<Map<String, String>>() {}.getType();

    public ServerConfig(final PortalDBPropertySource portalDBPropertySource) {
        super(portalDBPropertySource);
    }

    public String portalAddress() {
        return getValue(PortalConfigConstants.DEFAULT_PORTAL_ADDRESS_KEY);
    }

    public Map<String, String> getMetaServers() {
        final String key = PortalConfigConstants.APOLLO_META_SERVERS;
        String jsonContent = getValue(key);
        if (jsonContent == null) {
            return Collections.emptyMap();
        }
        try {
            return GSON.fromJson(jsonContent, META_SERVERS);
        } catch (Exception e) {
            logger.error("Wrong format for: {}", key, e);
            return Collections.emptyMap();
        }
    }

    public int refreshAdminServerAddressTaskNormalIntervalSecond() {
        int interval = getIntProperty(PortalConfigConstants.REFRESH_ADMIN_SERVER_NORMAL_INTERVAL_KEY, DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_NORMAL_INTERVAL_IN_SECOND);
        return checkInt(interval, 5, Integer.MAX_VALUE, DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_NORMAL_INTERVAL_IN_SECOND);
    }

    public int refreshAdminServerAddressTaskOfflineIntervalSecond() {
        int interval = getIntProperty(PortalConfigConstants.REFRESH_ADMIN_SERVER_OFFLINE_INTERVAL_KEY, DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_OFFLINE_INTERVAL_IN_SECOND);
        return checkInt(interval, 5, Integer.MAX_VALUE, DEFAULT_REFRESH_ADMIN_SERVER_ADDRESS_TASK_OFFLINE_INTERVAL_IN_SECOND);
    }

    private int checkInt(int value, int min, int max, int defaultValue) {
        if (value >= min && value <= max) {
            return value;
        }
        logger.warn("Configuration value '{}' is out of bounds [{} - {}]. Using default value '{}'.", value, min, max, defaultValue);
        return defaultValue;
    }

    public List<String> superAdmins() {
        String superAdminConfig = getValue(PortalConfigConstants.SUPER_ADMIN, "");
        if (Strings.isNullOrEmpty(superAdminConfig)) {
            return Collections.emptyList();
        }
        return splitter.splitToList(superAdminConfig);
    }

    public String getAdminServiceAccessTokens() {
        return getValue(PortalConfigConstants.ADMIN_SERVICE_ACCESS_TOKENS_KEY);
    }

    public String[] webHookUrls() {
        return getArrayProperty(PortalConfigConstants.CONFIG_RELEASE_WEBHOOK_SERVICE_URL_KEY, null);
    }

    public boolean supportSearchByItem() {
        return getBooleanProperty(PortalConfigConstants.SEARCH_BY_ITEM_SWITCH_KEY, true);
    }

    public List<Organization> organizations() {

        String organizations = getValue(PortalConfigConstants.DEFAULT_ORGANIZATION_KEY);
        return organizations == null ? Collections.emptyList() : GSON.fromJson(organizations, ORGANIZATION);
    }

    public int connectTimeout() {
        return getIntProperty(PortalConfigConstants.API_CONNECTION_TIMEOUT, 3000);
    }

    public int readTimeout() {
        return getIntProperty(PortalConfigConstants.API_READ_TIMEOUT, 10000);
    }

    public int connectionTimeToLive() {
        return getIntProperty(PortalConfigConstants.API_TIME_TO_LIVE, -1);
    }

    public int connectPoolMaxTotal() {
        return getIntProperty(PortalConfigConstants.API_POOL_MAX_TOTAL, 20);
    }

    public String consumerTokenSalt() {
        return getValue(PortalConfigConstants.CONSUMER_TOKEN_SALT_KEY, PortalConfigConstants.CONSUMER_TOKEN_DEFAULT_VALUE);
    }

    public String wikiAddress() {
        return getValue(PortalConfigConstants.WIKI_ADDRESS_KEY, PortalConfigConstants.WIKI_ADDRESS_DEFAULT_VALUE);
    }

    public int connectPoolMaxPerRoute() {
        return getIntProperty(PortalConfigConstants.API_POOL_MAX_PER_ROUTE, 2);
    }

    public boolean isEmergencyPublishAllowed(Env env) {
        String targetEnv = env.getName();
        String[] emergencyPublishSupportedEnvs = getArrayProperty(PortalConfigConstants.EMERGENCY_PUBLISH_SUPPORTED_ENVS_KEY, new String[0]);

        for (String supportedEnv : emergencyPublishSupportedEnvs) {
            if (targetEnv.equalsIgnoreCase(supportedEnv.trim())) {
                return true;
            }
        }
        return false;
    }
}
