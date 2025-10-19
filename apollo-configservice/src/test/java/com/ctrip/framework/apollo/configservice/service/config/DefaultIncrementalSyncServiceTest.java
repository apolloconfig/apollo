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
package com.ctrip.framework.apollo.configservice.service.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.ctrip.framework.apollo.biz.utils.ReleaseMessageKeyGenerator;
import com.ctrip.framework.apollo.configservice.service.config.DefaultIncrementalSyncService.ReleaseKeyPair;
import com.ctrip.framework.apollo.core.dto.ConfigurationChange;
import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/** @author jason */
@RunWith(MockitoJUnitRunner.class)
public class DefaultIncrementalSyncServiceTest {

  private DefaultIncrementalSyncService defaultIncrementalSyncService;
  private String someKey;
  private String someReleaseKey;
  private String someAppId;
  private String someClusterName;
  private String someNamespaceName;
  private String newReleaseKey;
  private String someClientSideReleaseKey;
  private String someLatestMergedReleaseKey;
  private Map<String, String> someClientSideConfigurations;
  private Map<String, String> someLatestReleaseConfigurations;
  private Cache<ReleaseKeyPair, List<ConfigurationChange>> configurationChangeCache;

  @Before
  public void setUp() throws Exception {
    defaultIncrementalSyncService = new DefaultIncrementalSyncService();
    configurationChangeCache = getConfigurationChangeCache(defaultIncrementalSyncService);
    someReleaseKey = "someReleaseKey";
    someAppId = "someAppId";
    someClusterName = "someClusterName";
    someNamespaceName = "someNamespaceName";

    newReleaseKey = "someReleaseKey";

    someKey = ReleaseMessageKeyGenerator.generate(someAppId, someClusterName, someNamespaceName);

    someClientSideReleaseKey = "client-release-key-v1";
    someLatestMergedReleaseKey = "latest-release-key-v1";

    someClientSideConfigurations = Maps.newHashMap();
    someClientSideConfigurations.put("k1", "v1");
    someClientSideConfigurations.put("k2", "v2");

    someLatestReleaseConfigurations = Maps.newHashMap();
    someLatestReleaseConfigurations.put("k1", "v1-new");
    someLatestReleaseConfigurations.put("k3", "v3");
  }

  @SuppressWarnings("unchecked")
  private Cache<ReleaseKeyPair, List<ConfigurationChange>> getConfigurationChangeCache(
      DefaultIncrementalSyncService service) {
    try {
      Field cacheField = service.getClass().getDeclaredField("configurationChangeCache");
      cacheField.setAccessible(true);
      return (Cache<ReleaseKeyPair, List<ConfigurationChange>>) cacheField.get(service);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get cache field", e);
    }
  }

  @Test
  public void testConfigurationChangeCacheHit() {

    List<ConfigurationChange> firstResult = defaultIncrementalSyncService.getConfigurationChanges(
        someLatestMergedReleaseKey, someLatestReleaseConfigurations, someClientSideReleaseKey,
        someClientSideConfigurations);

    ReleaseKeyPair key = new ReleaseKeyPair(someClientSideReleaseKey, someLatestMergedReleaseKey);
    List<ConfigurationChange> cachedResult = configurationChangeCache.getIfPresent(key);
    assertNotNull(cachedResult);
    assertSame(firstResult, cachedResult);

    List<ConfigurationChange> secondResult = defaultIncrementalSyncService.getConfigurationChanges(
        someLatestMergedReleaseKey, someLatestReleaseConfigurations, someClientSideReleaseKey,
        someClientSideConfigurations);

    assertSame(firstResult, secondResult);
  }

  @Test
  public void testConfigurationChangeCacheMiss() {
    defaultIncrementalSyncService.getConfigurationChanges(someLatestMergedReleaseKey,
        someLatestReleaseConfigurations, someClientSideReleaseKey, someClientSideConfigurations);

    String differentLatestMergedReleaseKey = "different-latest-key";
    ReleaseKeyPair differentKey =
        new ReleaseKeyPair(someClientSideReleaseKey, differentLatestMergedReleaseKey);

    assertNull(configurationChangeCache.getIfPresent(differentKey));
  }

  @Test
  public void testConfigurationChangeCacheWithNullValues() {

    List<ConfigurationChange> result = defaultIncrementalSyncService
        .getConfigurationChanges(someLatestMergedReleaseKey, null, someClientSideReleaseKey, null);

    ReleaseKeyPair key = new ReleaseKeyPair(someClientSideReleaseKey, someLatestMergedReleaseKey);

    List<ConfigurationChange> cachedResult = configurationChangeCache.getIfPresent(key);
    assertNotNull(cachedResult);
    assertSame(result, cachedResult);
    Assert.assertTrue(result.isEmpty());
  }

  @Test
  public void testChangeConfigurationsWithAdd() {
    String key1 = "key1";
    String value1 = "value1";

    String key2 = "key2";
    String value2 = "value2";

    Map<String, String> latestConfig = ImmutableMap.of(key1, value1, key2, value2);
    Map<String, String> clientSideConfigurations = ImmutableMap.of(key1, value1);

    List<ConfigurationChange> result = defaultIncrementalSyncService.getConfigurationChanges(
        newReleaseKey, latestConfig, someReleaseKey, clientSideConfigurations);

    assertEquals(1, result.size());
    assertEquals(key2, result.get(0).getKey());
    assertEquals(value2, result.get(0).getNewValue());
    assertEquals("ADDED", result.get(0).getConfigurationChangeType());
  }

  @Test
  public void testChangeConfigurationsWithLatestConfigIsNULL() {
    String key1 = "key1";
    String value1 = "value1";

    Map<String, String> clientSideConfigurations = ImmutableMap.of(key1, value1);

    List<ConfigurationChange> result = defaultIncrementalSyncService
        .getConfigurationChanges(newReleaseKey, null, someReleaseKey, clientSideConfigurations);

    assertEquals(1, result.size());
    assertEquals(key1, result.get(0).getKey());
    assertEquals(null, result.get(0).getNewValue());
    assertEquals("DELETED", result.get(0).getConfigurationChangeType());
  }

  @Test
  public void testChangeConfigurationsWithHistoryConfigIsNULL() {
    String key1 = "key1";
    String value1 = "value1";

    Map<String, String> latestConfig = ImmutableMap.of(key1, value1);

    List<ConfigurationChange> result = defaultIncrementalSyncService
        .getConfigurationChanges(newReleaseKey, latestConfig, someReleaseKey, null);

    assertEquals(1, result.size());
    assertEquals(key1, result.get(0).getKey());
    assertEquals(value1, result.get(0).getNewValue());
    assertEquals("ADDED", result.get(0).getConfigurationChangeType());
  }

  @Test
  public void testChangeConfigurationsWithUpdate() {
    String key1 = "key1";
    String value1 = "value1";

    String anotherValue1 = "anotherValue1";

    Map<String, String> latestConfig = ImmutableMap.of(key1, anotherValue1);
    Map<String, String> clientSideConfigurations = ImmutableMap.of(key1, value1);

    List<ConfigurationChange> result = defaultIncrementalSyncService.getConfigurationChanges(
        newReleaseKey, latestConfig, someReleaseKey, clientSideConfigurations);

    assertEquals(1, result.size());
    assertEquals(key1, result.get(0).getKey());
    assertEquals(anotherValue1, result.get(0).getNewValue());
    assertEquals("MODIFIED", result.get(0).getConfigurationChangeType());
  }

  @Test
  public void testChangeConfigurationsWithDelete() {
    String key1 = "key1";
    String value1 = "value1";

    Map<String, String> latestConfig = ImmutableMap.of();
    Map<String, String> clientSideConfigurations = ImmutableMap.of(key1, value1);

    List<ConfigurationChange> result = defaultIncrementalSyncService.getConfigurationChanges(
        newReleaseKey, latestConfig, someReleaseKey, clientSideConfigurations);

    assertEquals(1, result.size());
    assertEquals(key1, result.get(0).getKey());
    assertEquals(null, result.get(0).getNewValue());
    assertEquals("DELETED", result.get(0).getConfigurationChangeType());
  }
}
