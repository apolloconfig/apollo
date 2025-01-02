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

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.grayReleaseRule.GrayReleaseRulesHolder;
import com.ctrip.framework.apollo.biz.message.Topics;
import com.ctrip.framework.apollo.biz.service.ReleaseMessageService;
import com.ctrip.framework.apollo.biz.service.ReleaseService;
import com.ctrip.framework.apollo.biz.utils.ReleaseMessageKeyGenerator;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * config service with change cache
 *
 * @author jason
 */
public class ConfigServiceWithChangeCache extends ConfigServiceWithCache {

  private static final Logger logger = LoggerFactory.getLogger(ConfigServiceWithChangeCache.class);


  private static final long DEFAULT_EXPIRED_AFTER_ACCESS_IN_SENCONDS = 10;

  private static final String TRACER_EVENT_CHANGE_CACHE_LOAD_KEY = "ConfigChangeCache.LoadFromDBbyKey";

  private static final String TRACER_EVENT_CHANGE_CACHE_LOAD = "ConfigChangeCache.LoadFromDB";


  public LoadingCache<String, Optional<Release>> releasesCache;


  public ConfigServiceWithChangeCache(final ReleaseService releaseService,
      final ReleaseMessageService releaseMessageService,
      final GrayReleaseRulesHolder grayReleaseRulesHolder,
      final BizConfig bizConfig,
      final MeterRegistry meterRegistry) {
		
    super(releaseService, releaseMessageService, grayReleaseRulesHolder, bizConfig, meterRegistry);

  }

  @PostConstruct
  public void initialize() {
    super.initialize();
    buildReleaseCache();
  }

  private void buildReleaseCache() {

    CacheBuilder releasesCacheBuilder = CacheBuilder.newBuilder()
        .expireAfterAccess(DEFAULT_EXPIRED_AFTER_ACCESS_IN_SENCONDS, TimeUnit.SECONDS);

    releasesCache = releasesCacheBuilder.build(new CacheLoader<String, Optional<Release>>() {
      @Override
      public Optional<Release> load(String key) {
        Transaction transaction = Tracer.newTransaction(TRACER_EVENT_CHANGE_CACHE_LOAD_KEY, key);
        try {
          Release release = releaseService.findByReleaseKey(key);

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
  }

  @Override
  public void handleMessage(ReleaseMessage message, String channel) {
    logger.info("message received - channel: {}, message: {}", channel, message);
    if (!Topics.APOLLO_RELEASE_TOPIC.equals(channel) || Strings.isNullOrEmpty(
        message.getMessage())) {
      return;
    }
    String messageKey = message.getMessage();
    if (bizConfig.isConfigServiceCacheKeyIgnoreCase()) {
      messageKey = messageKey.toLowerCase();
    }
    Transaction transaction = Tracer.newTransaction(TRACER_EVENT_CHANGE_CACHE_LOAD, messageKey);
    try {
      List<String> namespaceInfo = ReleaseMessageKeyGenerator.messageToList(messageKey);
      Release latestRelease = releaseService.findLatestActiveRelease(namespaceInfo.get(0),
          namespaceInfo.get(1), namespaceInfo.get(2));

      releasesCache.put(latestRelease.getReleaseKey(), Optional.ofNullable(latestRelease));

      transaction.setStatus(Transaction.SUCCESS);
    } catch (Throwable ex) {
      transaction.setStatus(ex);
      //ignore
    } finally {
      transaction.complete();
    }
  }

  @Override
  public Map<String, Release> findReleasesByReleaseKeys(Set<String> releaseKeys) {
    try {

      ImmutableMap<String, Optional<Release>> releases = releasesCache.getAll(releaseKeys);

      Map<String, Release> filterReleases = releases.entrySet().stream()
          .filter(entry -> entry.getValue().isPresent())
          .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
      //find all keys
      if (releaseKeys.size() == filterReleases.size()) {
        return filterReleases;
      }
    } catch (ExecutionException e) {
      //ignore
    }
    return null;
  }
}
