/*
 * Copyright 2024 Apollo Authors
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
package com.ctrip.framework.apollo.biz.config;

import com.ctrip.framework.apollo.biz.service.BizDBPropertySource;
import com.ctrip.framework.apollo.common.config.RefreshableConfig;
import com.ctrip.framework.apollo.common.config.RefreshablePropertySource;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class BizConfig extends RefreshableConfig {

  private final static Logger logger = LoggerFactory.getLogger(BizConfig.class);

  private static final int DEFAULT_ITEM_KEY_LENGTH = 128;
  private static final int DEFAULT_ITEM_VALUE_LENGTH = 20000;

  private static final int DEFAULT_MAX_NAMESPACE_NUM = 200;

  private static final int DEFAULT_MAX_ITEM_NUM = 1000;

  private static final int DEFAULT_APPNAMESPACE_CACHE_REBUILD_INTERVAL = 60; //60s
  private static final int DEFAULT_GRAY_RELEASE_RULE_SCAN_INTERVAL = 60; //60s
  private static final int DEFAULT_APPNAMESPACE_CACHE_SCAN_INTERVAL = 1; //1s
  private static final int DEFAULT_ACCESS_KEY_CACHE_SCAN_INTERVAL = 1; //1s
  private static final int DEFAULT_ACCESS_KEY_CACHE_REBUILD_INTERVAL = 60; //60s
  private static final int DEFAULT_ACCESS_KEY_AUTH_TIME_DIFF_TOLERANCE = 60; //60s
  private static final int DEFAULT_RELEASE_MESSAGE_CACHE_SCAN_INTERVAL = 1; //1s
  private static final int DEFAULT_RELEASE_MESSAGE_SCAN_INTERVAL_IN_MS = 1000; //1000ms
  private static final int DEFAULT_RELEASE_MESSAGE_NOTIFICATION_BATCH = 100;
  private static final int DEFAULT_RELEASE_MESSAGE_NOTIFICATION_BATCH_INTERVAL_IN_MILLI = 100;//100ms
  private static final int DEFAULT_LONG_POLLING_TIMEOUT = 60; //60s
  public static final int DEFAULT_RELEASE_HISTORY_RETENTION_SIZE = -1;

  private static final Gson GSON = new Gson();

  private static final Type appIdValueLengthOverrideTypeReference =
      new TypeToken<Map<String, Integer>>() {
      }.getType();
  private static final Type namespaceValueLengthOverrideTypeReference =
      new TypeToken<Map<Long, Integer>>() {
      }.getType();
  private static final Type releaseHistoryRetentionSizeOverrideTypeReference =
      new TypeToken<Map<String, Integer>>() {
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

  public Map<String, Integer> appIdValueLengthLimitOverride() {
    String appIdValueLengthOverrideString = getValue("appid.value.length.limit.override");
    return parseOverrideConfig(appIdValueLengthOverrideString, appIdValueLengthOverrideTypeReference, value -> value > 0);
  }

  public Map<Long, Integer> namespaceValueLengthLimitOverride() {
    String namespaceValueLengthOverrideString = getValue("namespace.value.length.limit.override");
    return parseOverrideConfig(namespaceValueLengthOverrideString, namespaceValueLengthOverrideTypeReference, value -> value > 0);
  }

  public boolean isNamespaceNumLimitEnabled() {
    return getBooleanProperty("namespace.num.limit.enabled", false);
  }

  public int namespaceNumLimit() {
    int limit = getIntProperty("namespace.num.limit", DEFAULT_MAX_NAMESPACE_NUM);
    return checkInt(limit, 0, Integer.MAX_VALUE, DEFAULT_MAX_NAMESPACE_NUM);
  }

  public Set<String> namespaceNumLimitWhite() {
    return Sets.newHashSet(getArrayProperty("namespace.num.limit.white", new String[0]));
  }

  public boolean isItemNumLimitEnabled() {
    return getBooleanProperty("item.num.limit.enabled", false);
  }

  public int itemNumLimit() {
    int limit = getIntProperty("item.num.limit", DEFAULT_MAX_ITEM_NUM);
    return checkInt(limit, 5, Integer.MAX_VALUE, DEFAULT_MAX_ITEM_NUM);
  }

  public boolean isNamespaceLockSwitchOff() {
    return !getBooleanProperty("namespace.lock.switch", false);
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
    int interval = getIntProperty("apollo.access-key-cache-scan.interval",
        DEFAULT_ACCESS_KEY_CACHE_SCAN_INTERVAL);
    return checkInt(interval, 1, Integer.MAX_VALUE, DEFAULT_ACCESS_KEY_CACHE_SCAN_INTERVAL);
  }

  public TimeUnit accessKeyCacheScanIntervalTimeUnit() {
    return TimeUnit.SECONDS;
  }

  public int accessKeyCacheRebuildInterval() {
    int interval = getIntProperty("apollo.access-key-cache-rebuild.interval",
        DEFAULT_ACCESS_KEY_CACHE_REBUILD_INTERVAL);
    return checkInt(interval, 1, Integer.MAX_VALUE, DEFAULT_ACCESS_KEY_CACHE_REBUILD_INTERVAL);
  }

  public TimeUnit accessKeyCacheRebuildIntervalTimeUnit() {
    return TimeUnit.SECONDS;
  }

  public int accessKeyAuthTimeDiffTolerance() {
    int authTimeDiffTolerance = getIntProperty("apollo.access-key.auth-time-diff-tolerance",
        DEFAULT_ACCESS_KEY_AUTH_TIME_DIFF_TOLERANCE);
    return checkInt(authTimeDiffTolerance, 1, Integer.MAX_VALUE,
        DEFAULT_ACCESS_KEY_AUTH_TIME_DIFF_TOLERANCE);
  }

  public int releaseHistoryRetentionSize() {
    int count = getIntProperty("apollo.release-history.retention.size", DEFAULT_RELEASE_HISTORY_RETENTION_SIZE);
    return checkInt(count, 1, Integer.MAX_VALUE, DEFAULT_RELEASE_HISTORY_RETENTION_SIZE);
  }

  public Map<String, Integer> releaseHistoryRetentionSizeOverride() {
    String overrideString = getValue("apollo.release-history.retention.size.override");
    return parseOverrideConfig(overrideString, releaseHistoryRetentionSizeOverrideTypeReference, value -> value > 0);
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

  public boolean isConfigServiceCacheStatsEnabled() {
    return getBooleanProperty("config-service.cache.stats.enabled", false);
  }

  public boolean isConfigServiceCacheKeyIgnoreCase() {
    return getBooleanProperty("config-service.cache.key.ignore-case", false);
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

  private <K, V> Map<K, V> parseOverrideConfig(String configValue, Type typeReference, Predicate<V> valueFilter) {
    Map<K, V> result = Maps.newHashMap();
    if (!Strings.isNullOrEmpty(configValue)) {
      try {
        Map<K, V> parsed = GSON.fromJson(configValue, typeReference);
        for (Map.Entry<K, V> entry : parsed.entrySet()) {
          if (entry.getValue() != null && valueFilter.test(entry.getValue())) {
            result.put(entry.getKey(), entry.getValue());
          }
        }
      } catch (Exception e) {
        logger.error("Invalid override config value: {}", configValue, e);
      }
    }
    return Collections.unmodifiableMap(result);
  }

}
