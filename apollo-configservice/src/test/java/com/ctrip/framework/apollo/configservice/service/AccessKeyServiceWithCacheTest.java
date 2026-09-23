/*
 * Copyright 2025 Apollo Authors
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.awaitility.Awaitility.*;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.AccessKey;
import com.ctrip.framework.apollo.biz.repository.AccessKeyRepository;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author nisiyong
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AccessKeyServiceWithCacheTest {

  private AccessKeyServiceWithCache accessKeyServiceWithCache;
  @Mock
  private AccessKeyRepository accessKeyRepository;
  @Mock
  private BizConfig bizConfig;
  private int scanInterval;
  private TimeUnit scanIntervalTimeUnit;

  @Before
  public void setUp() {
    accessKeyServiceWithCache = new AccessKeyServiceWithCache(accessKeyRepository, bizConfig);

    scanInterval = 50;
    scanIntervalTimeUnit = TimeUnit.MILLISECONDS;
    when(bizConfig.accessKeyCacheScanInterval()).thenReturn(scanInterval);
    when(bizConfig.accessKeyCacheScanIntervalTimeUnit()).thenReturn(scanIntervalTimeUnit);
    when(bizConfig.accessKeyCacheRebuildInterval()).thenReturn(scanInterval);
    when(bizConfig.accessKeyCacheRebuildIntervalTimeUnit()).thenReturn(scanIntervalTimeUnit);

    Awaitility.reset();
    Awaitility.setDefaultTimeout(scanInterval * 100, scanIntervalTimeUnit);
    Awaitility.setDefaultPollInterval(scanInterval, scanIntervalTimeUnit);
  }

  @Test
  public void testGetAvailableSecrets() throws Exception {
    String appId = "someAppId";
    AccessKey firstAccessKey =
        assembleAccessKey(1L, appId, "secret-1", false, false, 1577808000000L);
    AccessKey secondAccessKey =
        assembleAccessKey(2L, appId, "secret-2", false, false, 1577808001000L);
    AccessKey thirdAccessKey =
        assembleAccessKey(3L, appId, "secret-3", true, false, 1577808005000L);

    // Initialize
    accessKeyServiceWithCache.afterPropertiesSet();

    assertThat(accessKeyServiceWithCache.getAvailableSecrets(appId)).isEmpty();

    // Add access key, disable by default
    when(accessKeyRepository
        .findFirst500ByDataChangeLastModifiedTimeGreaterThanEqualAndDataChangeLastModifiedTimeLessThanOrderByDataChangeLastModifiedTimeAsc(
            new Date(0L), new Date()))
        .thenReturn(Lists.newArrayList(firstAccessKey, secondAccessKey));
    when(accessKeyRepository.findAllById(anyList()))
        .thenReturn(Lists.newArrayList(firstAccessKey, secondAccessKey));

    await().untilAsserted(
        () -> assertThat(accessKeyServiceWithCache.getAvailableSecrets(appId)).isEmpty());
  }

  @Test
  public void testGetSecretsDoesNotFailWhenCacheMutatedConcurrently() throws Exception {
    String appId = "someAppId";
    AccessKey enabledKey = assembleAccessKey(1L, appId, "secret-1", true, false, 1577808000000L);

    when(accessKeyRepository
        .findFirst500ByDataChangeLastModifiedTimeGreaterThanEqualAndDataChangeLastModifiedTimeLessThanOrderByDataChangeLastModifiedTimeAsc(
            any(), any()))
        .thenReturn(Lists.newArrayList(enabledKey));
    when(accessKeyRepository.findAllById(anyList())).thenReturn(Lists.newArrayList(enabledKey));

    accessKeyServiceWithCache.afterPropertiesSet();
    try {
      assertThat(accessKeyServiceWithCache.getAvailableSecrets(appId)).containsExactly("secret-1");

      Field cacheField = AccessKeyServiceWithCache.class.getDeclaredField("accessKeyCache");
      cacheField.setAccessible(true);
      @SuppressWarnings("unchecked")
      ListMultimap<String, AccessKey> cache =
          (ListMultimap<String, AccessKey>) cacheField.get(accessKeyServiceWithCache);

      AtomicInteger failures = new AtomicInteger();
      AtomicBoolean stop = new AtomicBoolean(false);
      int readerCount = 4;
      ExecutorService pool = Executors.newFixedThreadPool(readerCount + 1);
      CountDownLatch started = new CountDownLatch(readerCount + 1);
      try {
        for (int i = 0; i < readerCount; i++) {
          pool.submit(() -> {
            started.countDown();
            while (!stop.get()) {
              try {
                accessKeyServiceWithCache.getAvailableSecrets(appId);
              } catch (RuntimeException ex) {
                failures.incrementAndGet();
              }
            }
          });
        }
        pool.submit(() -> {
          started.countDown();
          long seq = 0;
          while (!stop.get()) {
            AccessKey extra =
                assembleAccessKey(1000L + seq, appId, "secret-x-" + seq, true, false, 1L);
            cache.put(appId, extra);
            cache.remove(appId, extra);
            seq++;
          }
        });

        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        TimeUnit.MILLISECONDS.sleep(500);
        assertThat(failures.get()).isZero();
      } finally {
        stop.set(true);
        pool.shutdownNow();
        assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
      }
    } finally {
      accessKeyServiceWithCache.destroy();
    }
  }

  @Test
  public void testGetSecretsDoesNotSeeOldAndNewKeyTogether() throws Exception {
    String appId = "someAppId";
    Method mergeAccessKeys =
        AccessKeyServiceWithCache.class.getDeclaredMethod("mergeAccessKeys", List.class);
    mergeAccessKeys.setAccessible(true);

    AccessKey initial = assembleAccessKey(1L, appId, "secret-0", true, false, 1L);
    mergeAccessKeys.invoke(accessKeyServiceWithCache, Lists.newArrayList(initial));
    assertThat(accessKeyServiceWithCache.getAvailableSecrets(appId)).containsExactly("secret-0");

    AtomicInteger overlaps = new AtomicInteger();
    AtomicInteger failures = new AtomicInteger();
    AtomicBoolean stop = new AtomicBoolean(false);
    int readerCount = 4;
    ExecutorService pool = Executors.newFixedThreadPool(readerCount + 1);
    CountDownLatch started = new CountDownLatch(readerCount + 1);
    try {
      for (int i = 0; i < readerCount; i++) {
        pool.submit(() -> {
          started.countDown();
          while (!stop.get()) {
            try {
              List<String> secrets = accessKeyServiceWithCache.getAvailableSecrets(appId);
              if (secrets.size() != 1) {
                overlaps.incrementAndGet();
              }
            } catch (RuntimeException ex) {
              failures.incrementAndGet();
            }
          }
        });
      }
      pool.submit(() -> {
        started.countDown();
        long seq = 1;
        while (!stop.get()) {
          try {
            AccessKey updated = assembleAccessKey(1L, appId, "secret-" + seq, true, false, seq);
            mergeAccessKeys.invoke(accessKeyServiceWithCache, Lists.newArrayList(updated));
            seq++;
          } catch (Exception ex) {
            failures.incrementAndGet();
          }
        }
      });

      assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
      TimeUnit.MILLISECONDS.sleep(500);
      assertThat(failures.get()).isZero();
      assertThat(overlaps.get()).isZero();
    } finally {
      stop.set(true);
      pool.shutdownNow();
      assertThat(pool.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }
  }

  public AccessKey assembleAccessKey(Long id, String appId, String secret, boolean enabled,
      boolean deleted, long dataChangeLastModifiedTime) {
    AccessKey accessKey = new AccessKey();
    accessKey.setId(id);
    accessKey.setAppId(appId);
    accessKey.setSecret(secret);
    accessKey.setEnabled(enabled);
    accessKey.setDeleted(deleted);
    accessKey.setDataChangeLastModifiedTime(new Date(dataChangeLastModifiedTime));
    return accessKey;
  }

  /**
   * the referenced object is not reclaimable by garbage collection at least until after the
   * invocation of this method. see the java 9 method {@link java.lang.ref.Reference#reachabilityFence}
   * see the netty consistency method for JDK 6-8 {@link io.netty.util.ResourceLeakDetector.DefaultResourceLeak#reachabilityFence0}
   *
   * @param ref the reference
   */
  private static void reachabilityFence(Object ref) {
    if (ref != null) {
      synchronized (ref) {
      }
    }
  }
}
