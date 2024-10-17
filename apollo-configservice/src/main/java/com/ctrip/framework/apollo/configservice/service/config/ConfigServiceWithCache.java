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
package com.ctrip.framework.apollo.configservice.service.config;

import com.ctrip.framework.apollo.biz.grayReleaseRule.GrayReleaseRulesHolder;
import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.message.Topics;
import com.ctrip.framework.apollo.biz.service.ReleaseMessageService;
import com.ctrip.framework.apollo.biz.service.ReleaseService;
import com.ctrip.framework.apollo.biz.utils.ReleaseMessageKeyGenerator;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.ApolloNotificationMessages;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import org.springframework.util.CollectionUtils;

/**
 * config service with guava cache
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigServiceWithCache extends AbstractConfigService {
  private static final Logger logger = LoggerFactory.getLogger(ConfigServiceWithCache.class);
  private static final long DEFAULT_EXPIRED_AFTER_ACCESS_IN_MINUTES = 60;//1 hour
  private static final String TRACER_EVENT_CACHE_INVALIDATE = "ConfigCache.Invalidate";
  private static final String TRACER_EVENT_CACHE_LOAD = "ConfigCache.LoadFromDB";
  private static final String TRACER_EVENT_CACHE_LOAD_ID = "ConfigCache.LoadFromDBById";
  private static final String TRACER_EVENT_CACHE_GET = "ConfigCache.Get";
  private static final String TRACER_EVENT_CACHE_GET_ID = "ConfigCache.GetById";

  private final ReleaseService releaseService;
  private final ReleaseMessageService releaseMessageService;
  private final BizConfig bizConfig;
  private final MeterRegistry meterRegistry;

  private LoadingCache<String, ConfigCacheEntry> configCache;

  private LoadingCache<Long, Optional<Release>> configIdCache;

  private ConfigCacheEntry nullConfigCacheEntry;

  public ConfigServiceWithCache(final ReleaseService releaseService,
      final ReleaseMessageService releaseMessageService,
      final GrayReleaseRulesHolder grayReleaseRulesHolder,
      final BizConfig bizConfig,
      final MeterRegistry meterRegistry) {
    super(grayReleaseRulesHolder);
    this.releaseService = releaseService;
    this.releaseMessageService = releaseMessageService;
    this.bizConfig = bizConfig;
    this.meterRegistry = meterRegistry;
    nullConfigCacheEntry = new ConfigCacheEntry(ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, null);
  }

  @PostConstruct
  void initialize() {
    buildConfigCache();
    buildConfigIdCache();
  }

  @Override
  protected Release findActiveOne(long id, ApolloNotificationMessages clientMessages) {
    Tracer.logEvent(TRACER_EVENT_CACHE_GET_ID, String.valueOf(id));
    return configIdCache.getUnchecked(id).orElse(null);
  }

  @Override
  protected Release findLatestActiveRelease(String appId, String clusterName, String namespaceName,
                                            ApolloNotificationMessages clientMessages) {
    String messageKey = ReleaseMessageKeyGenerator.generate(appId, clusterName, namespaceName);
    String cacheKey = messageKey;

    if (bizConfig.isConfigServiceCacheKeyIgnoreCase()) {
      cacheKey = cacheKey.toLowerCase();
    }

    Tracer.logEvent(TRACER_EVENT_CACHE_GET, cacheKey);

    ConfigCacheEntry cacheEntry = configCache.getUnchecked(cacheKey);

    //cache is out-dated
    if (clientMessages != null && clientMessages.has(messageKey) &&
        clientMessages.get(messageKey) > cacheEntry.getNotificationId()) {
      //invalidate the cache and try to load from db again
      invalidate(cacheKey);
      cacheEntry = configCache.getUnchecked(cacheKey);
    }

    return cacheEntry.getRelease();
  }

  private void invalidate(String key) {
    configCache.invalidate(key);
    Tracer.logEvent(TRACER_EVENT_CACHE_INVALIDATE, key);
  }

  @Override
  public void handleMessage(ReleaseMessage message, String channel) {
    logger.info("message received - channel: {}, message: {}", channel, message);
    if (!Topics.APOLLO_RELEASE_TOPIC.equals(channel) || Strings.isNullOrEmpty(message.getMessage())) {
      return;
    }

    try {
      String messageKey = message.getMessage();
      if (bizConfig.isConfigServiceCacheKeyIgnoreCase()) {
        messageKey = messageKey.toLowerCase();
      }
      invalidate(messageKey);

      //warm up the cache
      configCache.getUnchecked(messageKey);
    } catch (Throwable ex) {
      //ignore
    }
  }

  private void buildConfigCache() {
    CacheBuilder configCacheBuilder = CacheBuilder.newBuilder()
        .expireAfterAccess(DEFAULT_EXPIRED_AFTER_ACCESS_IN_MINUTES, TimeUnit.MINUTES);
    if (bizConfig.isConfigServiceCacheStatsEnabled()) {
      configCacheBuilder.recordStats();
    }

    configCache = configCacheBuilder.build(new CacheLoader<String, ConfigCacheEntry>() {
      @Override
      public ConfigCacheEntry load(String key) throws Exception {
        List<String> namespaceInfo = ReleaseMessageKeyGenerator.messageToList(key);
        if (CollectionUtils.isEmpty(namespaceInfo)) {
          Tracer.logError(
              new IllegalArgumentException(String.format("Invalid cache load key %s", key)));
          return nullConfigCacheEntry;
        }

        Transaction transaction = Tracer.newTransaction(TRACER_EVENT_CACHE_LOAD, key);
        try {
          ReleaseMessage latestReleaseMessage = releaseMessageService.findLatestReleaseMessageForMessages(Lists
                                                                                                              .newArrayList(key));
          Release latestRelease = releaseService.findLatestActiveRelease(namespaceInfo.get(0), namespaceInfo.get(1),
                                                                         namespaceInfo.get(2));

          transaction.setStatus(Transaction.SUCCESS);

          long notificationId = latestReleaseMessage == null ? ConfigConsts.NOTIFICATION_ID_PLACEHOLDER : latestReleaseMessage
              .getId();

          if (notificationId == ConfigConsts.NOTIFICATION_ID_PLACEHOLDER && latestRelease == null) {
            return nullConfigCacheEntry;
          }

          return new ConfigCacheEntry(notificationId, latestRelease);
        } catch (Throwable ex) {
          transaction.setStatus(ex);
          throw ex;
        } finally {
          transaction.complete();
        }
      }
    });

    if (bizConfig.isConfigServiceCacheStatsEnabled()) {
      GuavaCacheMetrics.monitor(meterRegistry, configCache, "config_cache");
    }

  }

  private void buildConfigIdCache() {
    CacheBuilder configIdCacheBuilder = CacheBuilder.newBuilder()
        .expireAfterAccess(DEFAULT_EXPIRED_AFTER_ACCESS_IN_MINUTES, TimeUnit.MINUTES);
    if (bizConfig.isConfigServiceCacheStatsEnabled()) {
      configIdCacheBuilder.recordStats();
    }
    configIdCache = configIdCacheBuilder.build(new CacheLoader<Long, Optional<Release>>() {
      @Override
      public Optional<Release> load(Long key) throws Exception {
        Transaction transaction = Tracer.newTransaction(TRACER_EVENT_CACHE_LOAD_ID, String.valueOf(key));
        try {
          Release release = releaseService.findActiveOne(key);

          transaction.setStatus(Transaction.SUCCESS);

          return Optional.ofNullable(release);
        } catch (Throwable ex) {
          transaction.setStatus(ex);
          throw ex;
        } finally {
          transaction.complete();
        }
      }
    });

    if (bizConfig.isConfigServiceCacheStatsEnabled()) {
      GuavaCacheMetrics.monitor(meterRegistry, configIdCache, "config_id_cache");
    }

  }

  private static class ConfigCacheEntry {
    private final long notificationId;
    private final Release release;

    public ConfigCacheEntry(long notificationId, Release release) {
      this.notificationId = notificationId;
      this.release = release;
    }

    public long getNotificationId() {
      return notificationId;
    }

    public Release getRelease() {
      return release;
    }
  }
}
