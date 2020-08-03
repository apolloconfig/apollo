package com.ctrip.framework.apollo.configservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.awaitility.Awaitility.*;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.AccessKey;
import com.ctrip.framework.apollo.biz.repository.AccessKeyRepository;
import com.google.common.collect.Lists;
import java.util.Date;
import java.util.concurrent.TimeUnit;
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
    AccessKey firstAccessKey = assembleAccessKey(1L, appId, "secret-1", false,
        false, 1577808000000L);
    AccessKey secondAccessKey = assembleAccessKey(2L, appId, "secret-2", false,
        false, 1577808001000L);
    AccessKey thirdAccessKey = assembleAccessKey(3L, appId, "secret-3", true,
        false, 1577808005000L);

    // Initialize
    accessKeyServiceWithCache.afterPropertiesSet();

    assertThat(accessKeyServiceWithCache.getAvailableSecrets(appId)).isEmpty();

    // Add access key, disable by default
    when(accessKeyRepository.findFirst500ByDataChangeLastModifiedTimeGreaterThanOrderByDataChangeLastModifiedTimeAsc(new Date(0L)))
        .thenReturn(Lists.newArrayList(firstAccessKey, secondAccessKey));
    when(accessKeyRepository.findAllById(anyList()))
        .thenReturn(Lists.newArrayList(firstAccessKey, secondAccessKey));

    await().untilAsserted(() -> assertThat(accessKeyServiceWithCache.getAvailableSecrets(appId)).isEmpty());

    // Update access key, enable both of them
    firstAccessKey = assembleAccessKey(1L, appId, "secret-1", true, false, 1577808002000L);
    secondAccessKey = assembleAccessKey(2L, appId, "secret-2", true, false, 1577808003000L);
    when(accessKeyRepository.findFirst500ByDataChangeLastModifiedTimeGreaterThanOrderByDataChangeLastModifiedTimeAsc(new Date(1577808001000L)))
        .thenReturn(Lists.newArrayList(firstAccessKey, secondAccessKey));
    when(accessKeyRepository.findAllById(anyList()))
        .thenReturn(Lists.newArrayList(firstAccessKey, secondAccessKey));

    await().untilAsserted(() -> assertThat(accessKeyServiceWithCache.getAvailableSecrets(appId))
        .containsExactly("secret-1", "secret-2"));

    // Update access key, disable the first one
    firstAccessKey = assembleAccessKey(1L, appId, "secret-1", false, false, 1577808004000L);
    when(accessKeyRepository.findFirst500ByDataChangeLastModifiedTimeGreaterThanOrderByDataChangeLastModifiedTimeAsc(new Date(1577808003000L)))
        .thenReturn(Lists.newArrayList(firstAccessKey));
    when(accessKeyRepository.findAllById(anyList()))
        .thenReturn(Lists.newArrayList(firstAccessKey, secondAccessKey));

    await().untilAsserted(() -> assertThat(accessKeyServiceWithCache.getAvailableSecrets(appId))
        .containsExactly("secret-2"));

    // Delete access key, delete the second one
    when(accessKeyRepository.findAllById(anyList()))
        .thenReturn(Lists.newArrayList(firstAccessKey));

    await().untilAsserted(
        () -> assertThat(accessKeyServiceWithCache.getAvailableSecrets(appId)).isEmpty());

    // Add new access key in runtime, enable by default
    when(accessKeyRepository.findFirst500ByDataChangeLastModifiedTimeGreaterThanOrderByDataChangeLastModifiedTimeAsc(new Date(1577808004000L)))
        .thenReturn(Lists.newArrayList(thirdAccessKey));
    when(accessKeyRepository.findAllById(anyList()))
        .thenReturn(Lists.newArrayList(firstAccessKey, thirdAccessKey));

    await().untilAsserted(() -> assertThat(accessKeyServiceWithCache.getAvailableSecrets(appId))
        .containsExactly("secret-3"));
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
}