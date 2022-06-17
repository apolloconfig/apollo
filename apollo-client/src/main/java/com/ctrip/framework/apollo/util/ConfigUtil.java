/*
 * Copyright 2022 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.util;

import static com.ctrip.framework.apollo.util.factory.PropertiesFactory.APOLLO_PROPERTY_ORDER_ENABLE;

import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.MetaDomainConsts;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.enums.EnvUtils;
import com.ctrip.framework.apollo.core.utils.DeprecatedPropertyNotifyUtil;
import com.ctrip.framework.foundation.Foundation;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.RateLimiter;
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigUtil {

  private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
  private int refreshInterval = 5;
  private TimeUnit refreshIntervalTimeUnit = TimeUnit.MINUTES;
  private int connectTimeout = 1000; //1 second
  private int readTimeout = 5000; //5 seconds
  private String cluster;
  private int loadConfigQPS = 2; //2 times per second
  private int longPollQPS = 2; //2 times per second
  //for on error retry
  private long onErrorRetryInterval = 1;//1 second
  private TimeUnit onErrorRetryIntervalTimeUnit = TimeUnit.SECONDS;//1 second
  //for typed config cache of parser result, e.g. integer, double, long, etc.
  private long maxConfigCacheSize = 500;//500 cache key
  private long configCacheExpireTime = 1;//1 minute
  private TimeUnit configCacheExpireTimeUnit = TimeUnit.MINUTES;//1 minute
  private long longPollingInitialDelayInMills = 2000;//2 seconds
  private boolean autoUpdateInjectedSpringProperties = true;
  private final RateLimiter warnLogRateLimiter;
  private boolean propertiesOrdered = false;
  private boolean propertyNamesCacheEnabled = false;
  private boolean propertyFileCacheEnabled = true;
  private boolean overrideSystemProperties = true;

  public ConfigUtil() {
    warnLogRateLimiter = RateLimiter.create(0.017); // 1 warning log output per minute
    initRefreshInterval();
    initConnectTimeout();
    initReadTimeout();
    initCluster();
    initQPS();
    initMaxConfigCacheSize();
    initLongPollingInitialDelayInMills();
    initAutoUpdateInjectedSpringProperties();
    initPropertiesOrdered();
    initPropertyNamesCacheEnabled();
    initPropertyFileCacheEnabled();
    initOverrideSystemProperties();
  }

  /**
   * Get the app id for the current application.
   *
   * @return the app id or ConfigConsts.NO_APPID_PLACEHOLDER if app id is not available
   */
  public String getAppId() {
    String appId = Foundation.app().getAppId();
    if (Strings.isNullOrEmpty(appId)) {
      appId = ConfigConsts.NO_APPID_PLACEHOLDER;
      if (warnLogRateLimiter.tryAcquire()) {
        logger.warn(
            "app.id is not set, please make sure it is set in classpath:/META-INF/app.properties, now apollo will only load public namespace configurations!");
      }
    }
    return appId;
  }

  /**
   * Get the apollo label for the current application.
   *
   * @return apollo Label
   */
  public String getApolloLabel() {
    return Foundation.app().getApolloLabel();
  }

  /**
   * Get the access key secret for the current application.
   *
   * @return the current access key secret, null if there is no such secret.
   */
  public String getAccessKeySecret() {
    return Foundation.app().getAccessKeySecret();
  }

  /**
   * Get the data center info for the current application.
   *
   * @return the current data center, null if there is no such info.
   */
  public String getDataCenter() {
    return Foundation.server().getDataCenter();
  }

  private void initCluster() {
    //Load data center from system property
    cluster = System.getProperty(ConfigConsts.APOLLO_CLUSTER_KEY);

    //Use data center as cluster
    if (Strings.isNullOrEmpty(cluster)) {
      cluster = getDataCenter();
    }

    //Use default cluster
    if (Strings.isNullOrEmpty(cluster)) {
      cluster = ConfigConsts.CLUSTER_NAME_DEFAULT;
    }
  }

  /**
   * Get the cluster name for the current application.
   *
   * @return the cluster name, or "default" if not specified
   */
  public String getCluster() {
    return cluster;
  }

  /**
   * Get the current environment.
   *
   * @return the env, UNKNOWN if env is not set or invalid
   */
  public Env getApolloEnv() {
    return EnvUtils.transformEnv(Foundation.server().getEnvType());
  }

  public String getLocalIp() {
    return Foundation.net().getHostAddress();
  }

  public String getMetaServerDomainName() {
    return MetaDomainConsts.getDomain(getApolloEnv());
  }

  private void initConnectTimeout() {
    String customizedConnectTimeout = System.getProperty("apollo.connectTimeout");
    if (!Strings.isNullOrEmpty(customizedConnectTimeout)) {
      try {
        connectTimeout = Integer.parseInt(customizedConnectTimeout);
      } catch (Throwable ex) {
        logger.error("Config for apollo.connectTimeout is invalid: {}", customizedConnectTimeout);
      }
    }
  }

  public int getConnectTimeout() {
    return connectTimeout;
  }

  private void initReadTimeout() {
    String customizedReadTimeout = System.getProperty("apollo.readTimeout");
    if (!Strings.isNullOrEmpty(customizedReadTimeout)) {
      try {
        readTimeout = Integer.parseInt(customizedReadTimeout);
      } catch (Throwable ex) {
        logger.error("Config for apollo.readTimeout is invalid: {}", customizedReadTimeout);
      }
    }
  }

  public int getReadTimeout() {
    return readTimeout;
  }

  private void initRefreshInterval() {
    String customizedRefreshInterval = System.getProperty("apollo.refreshInterval");
    if (!Strings.isNullOrEmpty(customizedRefreshInterval)) {
      try {
        refreshInterval = Integer.parseInt(customizedRefreshInterval);
      } catch (Throwable ex) {
        logger.error("Config for apollo.refreshInterval is invalid: {}", customizedRefreshInterval);
      }
    }
  }

  public int getRefreshInterval() {
    return refreshInterval;
  }

  public TimeUnit getRefreshIntervalTimeUnit() {
    return refreshIntervalTimeUnit;
  }

  private void initQPS() {
    String customizedLoadConfigQPS = System.getProperty("apollo.loadConfigQPS");
    if (!Strings.isNullOrEmpty(customizedLoadConfigQPS)) {
      try {
        loadConfigQPS = Integer.parseInt(customizedLoadConfigQPS);
      } catch (Throwable ex) {
        logger.error("Config for apollo.loadConfigQPS is invalid: {}", customizedLoadConfigQPS);
      }
    }

    String customizedLongPollQPS = System.getProperty("apollo.longPollQPS");
    if (!Strings.isNullOrEmpty(customizedLongPollQPS)) {
      try {
        longPollQPS = Integer.parseInt(customizedLongPollQPS);
      } catch (Throwable ex) {
        logger.error("Config for apollo.longPollQPS is invalid: {}", customizedLongPollQPS);
      }
    }
  }

  public int getLoadConfigQPS() {
    return loadConfigQPS;
  }

  public int getLongPollQPS() {
    return longPollQPS;
  }

  public long getOnErrorRetryInterval() {
    return onErrorRetryInterval;
  }

  public TimeUnit getOnErrorRetryIntervalTimeUnit() {
    return onErrorRetryIntervalTimeUnit;
  }

  public String getDefaultLocalCacheDir() {
    String cacheRoot = getCustomizedCacheRoot();

    if (!Strings.isNullOrEmpty(cacheRoot)) {
      return cacheRoot + File.separator + getAppId();
    }

    cacheRoot = isOSWindows() ? "C:\\opt\\data\\%s" : "/opt/data/%s";
    return String.format(cacheRoot, getAppId());
  }

  private String getCustomizedCacheRoot() {
    // 1. Get from System Property
    String cacheRoot = System.getProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR);
    if (Strings.isNullOrEmpty(cacheRoot)) {
      // 2. Get from OS environment variable
      cacheRoot = System.getenv(ApolloClientSystemConsts.APOLLO_CACHE_DIR_ENVIRONMENT_VARIABLES);
    }
    if (Strings.isNullOrEmpty(cacheRoot)) {
      // 3. Get from server.properties
      cacheRoot = Foundation.server().getProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR, null);
    }
    if (Strings.isNullOrEmpty(cacheRoot)) {
      // 4. Get from app.properties
      cacheRoot = Foundation.app().getProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR, null);
    }
    if (Strings.isNullOrEmpty(cacheRoot)) {
      // 5. Get from deprecated config
      cacheRoot = getDeprecatedCustomizedCacheRoot();
    }
    return cacheRoot;
  }

  @SuppressWarnings("deprecation")
  private String getDeprecatedCustomizedCacheRoot() {
    // 1. Get from System Property
    String cacheRoot = System.getProperty(ApolloClientSystemConsts.DEPRECATED_APOLLO_CACHE_DIR);
    if (!Strings.isNullOrEmpty(cacheRoot)) {
      DeprecatedPropertyNotifyUtil.warn(ApolloClientSystemConsts.DEPRECATED_APOLLO_CACHE_DIR,
          ApolloClientSystemConsts.APOLLO_CACHE_DIR);
    }
    if (Strings.isNullOrEmpty(cacheRoot)) {
      // 2. Get from OS environment variable
      cacheRoot = System.getenv(ApolloClientSystemConsts.DEPRECATED_APOLLO_CACHE_DIR_ENVIRONMENT_VARIABLES);
      if (!Strings.isNullOrEmpty(cacheRoot)) {
        DeprecatedPropertyNotifyUtil
            .warn(ApolloClientSystemConsts.DEPRECATED_APOLLO_CACHE_DIR_ENVIRONMENT_VARIABLES,
                ApolloClientSystemConsts.APOLLO_CACHE_DIR_ENVIRONMENT_VARIABLES);
      }
    }
    if (Strings.isNullOrEmpty(cacheRoot)) {
      // 3. Get from server.properties
      cacheRoot = Foundation.server().getProperty(ApolloClientSystemConsts.DEPRECATED_APOLLO_CACHE_DIR, null);
      if (!Strings.isNullOrEmpty(cacheRoot)) {
        DeprecatedPropertyNotifyUtil.warn(ApolloClientSystemConsts.DEPRECATED_APOLLO_CACHE_DIR,
            ApolloClientSystemConsts.APOLLO_CACHE_DIR);
      }
    }
    if (Strings.isNullOrEmpty(cacheRoot)) {
      // 4. Get from app.properties
      cacheRoot = Foundation.app().getProperty(ApolloClientSystemConsts.DEPRECATED_APOLLO_CACHE_DIR, null);
      if (!Strings.isNullOrEmpty(cacheRoot)) {
        DeprecatedPropertyNotifyUtil.warn(ApolloClientSystemConsts.DEPRECATED_APOLLO_CACHE_DIR,
            ApolloClientSystemConsts.APOLLO_CACHE_DIR);
      }
    }
    return cacheRoot;
  }

  public boolean isInLocalMode() {
    try {
      return Env.LOCAL == getApolloEnv();
    } catch (Throwable ex) {
      //ignore
    }
    return false;
  }

  public boolean isOSWindows() {
    String osName = System.getProperty("os.name");
    if (Strings.isNullOrEmpty(osName)) {
      return false;
    }
    return osName.startsWith("Windows");
  }

  private void initMaxConfigCacheSize() {
    String customizedConfigCacheSize = System.getProperty("apollo.configCacheSize");
    if (!Strings.isNullOrEmpty(customizedConfigCacheSize)) {
      try {
        maxConfigCacheSize = Long.parseLong(customizedConfigCacheSize);
      } catch (Throwable ex) {
        logger.error("Config for apollo.configCacheSize is invalid: {}", customizedConfigCacheSize);
      }
    }
  }

  public long getMaxConfigCacheSize() {
    return maxConfigCacheSize;
  }

  public long getConfigCacheExpireTime() {
    return configCacheExpireTime;
  }

  public TimeUnit getConfigCacheExpireTimeUnit() {
    return configCacheExpireTimeUnit;
  }

  private void initLongPollingInitialDelayInMills() {
    String customizedLongPollingInitialDelay = System
        .getProperty("apollo.longPollingInitialDelayInMills");
    if (!Strings.isNullOrEmpty(customizedLongPollingInitialDelay)) {
      try {
        longPollingInitialDelayInMills = Long.parseLong(customizedLongPollingInitialDelay);
      } catch (Throwable ex) {
        logger.error("Config for apollo.longPollingInitialDelayInMills is invalid: {}",
            customizedLongPollingInitialDelay);
      }
    }
  }

  public long getLongPollingInitialDelayInMills() {
    return longPollingInitialDelayInMills;
  }

  private void initAutoUpdateInjectedSpringProperties() {
    // 1. Get from System Property
    String enableAutoUpdate = System.getProperty("apollo.autoUpdateInjectedSpringProperties");
    if (Strings.isNullOrEmpty(enableAutoUpdate)) {
      // 2. Get from app.properties
      enableAutoUpdate = Foundation.app()
          .getProperty("apollo.autoUpdateInjectedSpringProperties", null);
    }
    if (!Strings.isNullOrEmpty(enableAutoUpdate)) {
      autoUpdateInjectedSpringProperties = Boolean.parseBoolean(enableAutoUpdate.trim());
    }
  }

  public boolean isAutoUpdateInjectedSpringPropertiesEnabled() {
    return autoUpdateInjectedSpringProperties;
  }

  private void initPropertiesOrdered() {
    String enablePropertiesOrdered = System.getProperty(APOLLO_PROPERTY_ORDER_ENABLE);

    if (Strings.isNullOrEmpty(enablePropertiesOrdered)) {
      enablePropertiesOrdered = Foundation.app().getProperty(APOLLO_PROPERTY_ORDER_ENABLE, "false");
    }

    if (!Strings.isNullOrEmpty(enablePropertiesOrdered)) {
      try {
        propertiesOrdered = Boolean.parseBoolean(enablePropertiesOrdered);
      } catch (Throwable ex) {
        logger.warn("Config for {} is invalid: {}, set default value: false",
            APOLLO_PROPERTY_ORDER_ENABLE, enablePropertiesOrdered);
      }
    }
  }

  public boolean isPropertiesOrderEnabled() {
    return propertiesOrdered;
  }

  public boolean isPropertyNamesCacheEnabled() {
    return propertyNamesCacheEnabled;
  }

  public boolean isPropertyFileCacheEnabled() {
    return propertyFileCacheEnabled;
  }

  public boolean isOverrideSystemProperties() {
    return overrideSystemProperties;
  }

  private void initPropertyNamesCacheEnabled() {
    propertyNamesCacheEnabled = getPropertyBoolean(ApolloClientSystemConsts.APOLLO_PROPERTY_NAMES_CACHE_ENABLE,
            ApolloClientSystemConsts.APOLLO_PROPERTY_NAMES_CACHE_ENABLE_ENVIRONMENT_VARIABLES,
            propertyNamesCacheEnabled);
  }

  private void initPropertyFileCacheEnabled() {
    propertyFileCacheEnabled = getPropertyBoolean(ApolloClientSystemConsts.APOLLO_CACHE_FILE_ENABLE,
            ApolloClientSystemConsts.APOLLO_CACHE_FILE_ENABLE_ENVIRONMENT_VARIABLES,
            propertyFileCacheEnabled);
  }

  private void initOverrideSystemProperties() {
    overrideSystemProperties = getPropertyBoolean(ApolloClientSystemConsts.APOLLO_OVERRIDE_SYSTEM_PROPERTIES,
            ApolloClientSystemConsts.APOLLO_OVERRIDE_SYSTEM_PROPERTIES,
            overrideSystemProperties);
  }

  private boolean getPropertyBoolean(String propertyName, String envName, boolean defaultVal) {
    String enablePropertyNamesCache = System.getProperty(propertyName);
    if (Strings.isNullOrEmpty(enablePropertyNamesCache)) {
      enablePropertyNamesCache = System.getenv(envName);
    }
    if (Strings.isNullOrEmpty(enablePropertyNamesCache)) {
      enablePropertyNamesCache = Foundation.app().getProperty(propertyName, null);
    }
    if (!Strings.isNullOrEmpty(enablePropertyNamesCache)) {
      try {
        return Boolean.parseBoolean(enablePropertyNamesCache);
      } catch (Throwable ex) {
        logger.warn("Config for {} is invalid: {}, set default value: {}",
                propertyName, enablePropertyNamesCache, defaultVal);
      }
    }
    return defaultVal;
  }
}
