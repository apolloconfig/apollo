package com.ctrip.framework.apollo.biz.config;

import com.ctrip.framework.apollo.biz.service.BizDBPropertySource;
import com.ctrip.framework.apollo.common.config.RefreshableConfig;
import com.ctrip.framework.apollo.common.config.RefreshablePropertySource;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class BizConfig extends RefreshableConfig {

  private static final int DEFAULT_ITEM_KEY_LENGTH = 128;
  private static final int DEFAULT_ITEM_VALUE_LENGTH = 20000;
  private static final int DEFAULT_APPNAMESPACE_CACHE_REBUILD_INTERVAL = 60; //60s
  private static final int DEFAULT_GRAY_RELEASE_RULE_SCAN_INTERVAL = 60; //60s
  private static final int DEFAULT_APPNAMESPACE_CACHE_SCAN_INTERVAL = 1; //1s
  private static final int DEFAULT_ACCESSKEY_CACHE_SCAN_INTERVAL = 1; //1s
  private static final int DEFAULT_ACCESSKEY_CACHE_REBUILD_INTERVAL = 60; //60s
  private static final int DEFAULT_RELEASE_MESSAGE_CACHE_SCAN_INTERVAL = 1; //1s
  private static final int DEFAULT_RELEASE_MESSAGE_SCAN_INTERVAL_IN_MS = 1000; //1000ms
  private static final int DEFAULT_RELEASE_MESSAGE_NOTIFICATION_BATCH = 100;
  private static final int DEFAULT_RELEASE_MESSAGE_NOTIFICATION_BATCH_INTERVAL_IN_MILLI = 100;//100ms
  private static final int DEFAULT_LONG_POLLING_TIMEOUT = 60; //60s

  private static final Gson GSON = new Gson();

  private static final Type namespaceValueLengthOverrideTypeReference =
      new TypeToken<Map<Long, Integer>>() {
      }.getType();

  private final BizDBPropertySource propertySource;

  public BizConfig(final BizDBPropertySource propertySource) {
    this.propertySource = propertySource;
  }

  @Override
  protected List<RefreshablePropertySource> getRefreshablePropertySources() {
    return Collections.singletonList(propertySource);
  }

  public List<String> eurekaServiceUrls() {
    String configuration = getValue("eureka.service.url", "");
    if (Strings.isNullOrEmpty(configuration)) {
      return Collections.emptyList();
    }

    return splitter.splitToList(configuration);
  }

  public int grayReleaseRuleScanInterval() {
    int interval = getIntProperty("apollo.gray-release-rule-scan.interval", DEFAULT_GRAY_RELEASE_RULE_SCAN_INTERVAL);
    return checkInt(interval, 1, Integer.MAX_VALUE, DEFAULT_GRAY_RELEASE_RULE_SCAN_INTERVAL);
  }

  public long longPollingTimeoutInMilli() {
    int timeout = getIntProperty("long.polling.timeout", DEFAULT_LONG_POLLING_TIMEOUT);
    // java client's long polling timeout is 90 seconds, so server side long polling timeout must be less than 90
    return 1000 * checkInt(timeout, 1, 90, DEFAULT_LONG_POLLING_TIMEOUT);
  }

  public int itemKeyLengthLimit() {
    int limit = getIntProperty("item.key.length.limit", DEFAULT_ITEM_KEY_LENGTH);
    return checkInt(limit, 5, Integer.MAX_VALUE, DEFAULT_ITEM_KEY_LENGTH);
  }

  public int itemValueLengthLimit() {
    int limit = getIntProperty("item.value.length.limit", DEFAULT_ITEM_VALUE_LENGTH);
    return checkInt(limit, 5, Integer.MAX_VALUE, DEFAULT_ITEM_VALUE_LENGTH);
  }

  public Map<Long, Integer> namespaceValueLengthLimitOverride() {
    String namespaceValueLengthOverrideString = getValue("namespace.value.length.limit.override");
    Map<Long, Integer> namespaceValueLengthOverride = Maps.newHashMap();
    if (!Strings.isNullOrEmpty(namespaceValueLengthOverrideString)) {
      namespaceValueLengthOverride =
          GSON.fromJson(namespaceValueLengthOverrideString, namespaceValueLengthOverrideTypeReference);
    }

    return namespaceValueLengthOverride;
  }

  public boolean isNamespaceLockSwitchOff() {
    return !getBooleanProperty("namespace.lock.switch", false);
  }

  /**
   * ctrip config
   **/
  public String cloggingUrl() {
    return getValue("clogging.server.url");
  }

  public String cloggingPort() {
    return getValue("clogging.server.port");
  }

  public int appNamespaceCacheScanInterval() {
    int interval = getIntProperty("apollo.app-namespace-cache-scan.interval", DEFAULT_APPNAMESPACE_CACHE_SCAN_INTERVAL);
    return checkInt(interval, 1, Integer.MAX_VALUE, DEFAULT_APPNAMESPACE_CACHE_SCAN_INTERVAL);
  }

  public TimeUnit appNamespaceCacheScanIntervalTimeUnit() {
    return TimeUnit.SECONDS;
  }

  public int appNamespaceCacheRebuildInterval() {
    int interval = getIntProperty("apollo.app-namespace-cache-rebuild.interval", DEFAULT_APPNAMESPACE_CACHE_REBUILD_INTERVAL);
    return checkInt(interval, 1, Integer.MAX_VALUE, DEFAULT_APPNAMESPACE_CACHE_REBUILD_INTERVAL);
  }

  public TimeUnit appNamespaceCacheRebuildIntervalTimeUnit() {
    return TimeUnit.SECONDS;
  }

  public int accessKeyCacheScanInterval() {
    int interval = getIntProperty("apollo.access-key-cache-scan.interval", DEFAULT_ACCESSKEY_CACHE_SCAN_INTERVAL);
    return checkInt(interval, 1, Integer.MAX_VALUE, DEFAULT_ACCESSKEY_CACHE_SCAN_INTERVAL);
  }

  public TimeUnit accessKeyCacheScanIntervalTimeUnit() {
    return TimeUnit.SECONDS;
  }

  public int accessKeyCacheRebuildInterval() {
    int interval = getIntProperty("apollo.access-key-cache-rebuild.interval", DEFAULT_ACCESSKEY_CACHE_REBUILD_INTERVAL);
    return checkInt(interval, 1, Integer.MAX_VALUE, DEFAULT_ACCESSKEY_CACHE_REBUILD_INTERVAL);
  }

  public TimeUnit accessKeyCacheRebuildIntervalTimeUnit() {
    return TimeUnit.SECONDS;
  }

  public int releaseMessageCacheScanInterval() {
    int interval = getIntProperty("apollo.release-message-cache-scan.interval", DEFAULT_RELEASE_MESSAGE_CACHE_SCAN_INTERVAL);
    return checkInt(interval, 1, Integer.MAX_VALUE, DEFAULT_RELEASE_MESSAGE_CACHE_SCAN_INTERVAL);
  }

  public TimeUnit releaseMessageCacheScanIntervalTimeUnit() {
    return TimeUnit.SECONDS;
  }

  public int releaseMessageScanIntervalInMilli() {
    int interval = getIntProperty("apollo.message-scan.interval", DEFAULT_RELEASE_MESSAGE_SCAN_INTERVAL_IN_MS);
    return checkInt(interval, 100, Integer.MAX_VALUE, DEFAULT_RELEASE_MESSAGE_SCAN_INTERVAL_IN_MS);
  }

  public int releaseMessageNotificationBatch() {
    int batch = getIntProperty("apollo.release-message.notification.batch", DEFAULT_RELEASE_MESSAGE_NOTIFICATION_BATCH);
    return checkInt(batch, 1, Integer.MAX_VALUE, DEFAULT_RELEASE_MESSAGE_NOTIFICATION_BATCH);
  }

  public int releaseMessageNotificationBatchIntervalInMilli() {
    int interval = getIntProperty("apollo.release-message.notification.batch.interval", DEFAULT_RELEASE_MESSAGE_NOTIFICATION_BATCH_INTERVAL_IN_MILLI);
    return checkInt(interval, 10, Integer.MAX_VALUE, DEFAULT_RELEASE_MESSAGE_NOTIFICATION_BATCH_INTERVAL_IN_MILLI);
  }

  public boolean isConfigServiceCacheEnabled() {
    return getBooleanProperty("config-service.cache.enabled", false);
  }

  int checkInt(int value, int min, int max, int defaultValue) {
    if (value >= min && value <= max) {
      return value;
    }
    return defaultValue;
  }

  public boolean isAdminServiceAccessControlEnabled() {
    return getBooleanProperty("admin-service.access.control.enabled", false);
  }

  public String getAdminServiceAccessTokens() {
    return getValue("admin-service.access.tokens");
  }
}
