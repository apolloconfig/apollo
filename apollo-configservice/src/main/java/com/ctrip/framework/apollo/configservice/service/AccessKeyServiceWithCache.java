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
package com.ctrip.framework.apollo.configservice.service;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.AccessKey;
import com.ctrip.framework.apollo.biz.repository.AccessKeyRepository;
import com.ctrip.framework.apollo.common.constants.AccessKeyMode;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder.ListMultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @author nisiyong
 */
@Service
public class AccessKeyServiceWithCache implements InitializingBean {

  private static Logger logger = LoggerFactory.getLogger(AccessKeyServiceWithCache.class);

  private final AccessKeyRepository accessKeyRepository;
  private final BizConfig bizConfig;

  private int scanInterval;
  private TimeUnit scanIntervalTimeUnit;
  private int rebuildInterval;
  private TimeUnit rebuildIntervalTimeUnit;
  private ScheduledExecutorService scheduledExecutorService;
  private Date lastTimeScanned;

  private ListMultimap<String, AccessKey> accessKeyCache;
  private ConcurrentMap<Long, AccessKey> accessKeyIdCache;

  public AccessKeyServiceWithCache(final AccessKeyRepository accessKeyRepository,
      final BizConfig bizConfig) {
    this.accessKeyRepository = accessKeyRepository;
    this.bizConfig = bizConfig;

    initialize();
  }

  private void initialize() {
    scheduledExecutorService = new ScheduledThreadPoolExecutor(1,
        ApolloThreadFactory.create("AccessKeyServiceWithCache", true));
    lastTimeScanned = new Date(0L);

    ListMultimap<String, AccessKey> multimap = ListMultimapBuilder.treeKeys(String.CASE_INSENSITIVE_ORDER)
        .arrayListValues().build();
    accessKeyCache = Multimaps.synchronizedListMultimap(multimap);
    accessKeyIdCache = Maps.newConcurrentMap();
  }

  public List<String> getAvailableSecrets(String appId) {
    return getSecrets(appId, key -> key.isEnabled() && key.getMode() == AccessKeyMode.FILTER);
  }

  public List<String> getObservableSecrets(String appId) {
    return getSecrets(appId, key -> key.isEnabled() && key.getMode() == AccessKeyMode.OBSERVER);
  }

  public List<String> getSecrets(String appId, Predicate<AccessKey> filter) {
    List<AccessKey> accessKeys = accessKeyCache.get(appId);
    if (CollectionUtils.isEmpty(accessKeys)) {
      return Collections.emptyList();
    }

    return accessKeys.stream()
        .filter(filter)
        .map(AccessKey::getSecret)
        .collect(Collectors.toList());
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    populateDataBaseInterval();
    scanNewAndUpdatedAccessKeys(); //block the startup process until load finished

    scheduledExecutorService.scheduleWithFixedDelay(this::scanNewAndUpdatedAccessKeys,
        scanInterval, scanInterval, scanIntervalTimeUnit);

    scheduledExecutorService.scheduleAtFixedRate(this::rebuildAccessKeyCache,
        rebuildInterval, rebuildInterval, rebuildIntervalTimeUnit);
  }

  private void scanNewAndUpdatedAccessKeys() {
    Transaction transaction = Tracer.newTransaction("Apollo.AccessKeyServiceWithCache",
        "scanNewAndUpdatedAccessKeys");
    try {
      loadNewAndUpdatedAccessKeys();
      transaction.setStatus(Transaction.SUCCESS);
    } catch (Throwable ex) {
      transaction.setStatus(ex);
      logger.error("Load new/updated app access keys failed", ex);
    } finally {
      transaction.complete();
    }
  }

  private void rebuildAccessKeyCache() {
    Transaction transaction = Tracer.newTransaction("Apollo.AccessKeyServiceWithCache",
        "rebuildCache");
    try {
      deleteAccessKeyCache();
      transaction.setStatus(Transaction.SUCCESS);
    } catch (Throwable ex) {
      transaction.setStatus(ex);
      logger.error("Rebuild cache failed", ex);
    } finally {
      transaction.complete();
    }
  }

  private void loadNewAndUpdatedAccessKeys() {
    boolean hasMore = true;
    while (hasMore && !Thread.currentThread().isInterrupted()) {
      //current batch is 500
      List<AccessKey> accessKeys = accessKeyRepository
          .findFirst500ByDataChangeLastModifiedTimeGreaterThanOrderByDataChangeLastModifiedTimeAsc(lastTimeScanned);
      if (CollectionUtils.isEmpty(accessKeys)) {
        break;
      }

      int scanned = accessKeys.size();
      mergeAccessKeys(accessKeys);
      logger.info("Loaded {} new/updated Accesskey from startTime {}", scanned, lastTimeScanned);

      hasMore = scanned == 500;
      lastTimeScanned = accessKeys.get(scanned - 1).getDataChangeLastModifiedTime();

      // In order to avoid missing some records at the last time, we need to scan records at this time individually
      if (hasMore) {
        List<AccessKey> lastModifiedTimeAccessKeys = accessKeyRepository.findByDataChangeLastModifiedTime(lastTimeScanned);
        mergeAccessKeys(lastModifiedTimeAccessKeys);
        logger.info("Loaded {} new/updated Accesskey at lastModifiedTime {}", scanned, lastTimeScanned);
      }
    }
  }

  private void mergeAccessKeys(List<AccessKey> accessKeys) {
    for (AccessKey accessKey : accessKeys) {
      AccessKey thatInCache = accessKeyIdCache.get(accessKey.getId());

      accessKeyIdCache.put(accessKey.getId(), accessKey);
      accessKeyCache.put(accessKey.getAppId(), accessKey);

      if (thatInCache != null && accessKey.getDataChangeLastModifiedTime()
          .after(thatInCache.getDataChangeLastModifiedTime())) {
        accessKeyCache.remove(accessKey.getAppId(), thatInCache);
        logger.info("Found Accesskey changes, old: {}, new: {}", thatInCache, accessKey);
      }
    }
  }

  private void deleteAccessKeyCache() {
    List<Long> ids = Lists.newArrayList(accessKeyIdCache.keySet());
    if (CollectionUtils.isEmpty(ids)) {
      return;
    }

    List<List<Long>> partitionIds = Lists.partition(ids, 500);
    for (List<Long> toRebuildIds : partitionIds) {
      Iterable<AccessKey> accessKeys = accessKeyRepository.findAllById(toRebuildIds);

      Set<Long> foundIds = Sets.newHashSet();
      for (AccessKey accessKey : accessKeys) {
        foundIds.add(accessKey.getId());
      }

      //handle deleted
      SetView<Long> deletedIds = Sets.difference(Sets.newHashSet(toRebuildIds), foundIds);
      handleDeletedAccessKeys(deletedIds);
    }
  }

  private void handleDeletedAccessKeys(Set<Long> deletedIds) {
    if (CollectionUtils.isEmpty(deletedIds)) {
      return;
    }
    for (Long deletedId : deletedIds) {
      AccessKey deleted = accessKeyIdCache.remove(deletedId);
      if (deleted == null) {
        continue;
      }

      accessKeyCache.remove(deleted.getAppId(), deleted);
      logger.info("Found AccessKey deleted, {}", deleted);
    }
  }

  private void populateDataBaseInterval() {
    scanInterval = bizConfig.accessKeyCacheScanInterval();
    scanIntervalTimeUnit = bizConfig.accessKeyCacheScanIntervalTimeUnit();
    rebuildInterval = bizConfig.accessKeyCacheRebuildInterval();
    rebuildIntervalTimeUnit = bizConfig.accessKeyCacheRebuildIntervalTimeUnit();
  }
}
