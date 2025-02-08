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
import com.ctrip.framework.apollo.core.dto.ConfigurationChange;
import com.ctrip.framework.apollo.core.enums.ConfigurationChangeType;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

/**
 * @author jason
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceWithChangeCacheTest {

  private ConfigServiceWithChangeCache configServiceWithChangeCache;

  @Mock
  private ReleaseService releaseService;

  @Mock
  private ReleaseMessageService releaseMessageService;
  @Mock
  private Release someRelease;
  @Mock
  private BizConfig bizConfig;
  @Mock
  private MeterRegistry meterRegistry;
  @Mock
  private GrayReleaseRulesHolder grayReleaseRulesHolder;

  private String someKey;

  private String someReleaseKey;

  private String someAppId;
  private String someClusterName;
  private String someNamespaceName;


  @Before
  public void setUp() throws Exception {
    configServiceWithChangeCache = new ConfigServiceWithChangeCache(releaseService,
        releaseMessageService,
        grayReleaseRulesHolder, bizConfig, meterRegistry);

    configServiceWithChangeCache.initialize();

    someReleaseKey = "someReleaseKey";
    someAppId = "someAppId";
    someClusterName = "someClusterName";
    someNamespaceName = "someNamespaceName";

    someKey = ReleaseMessageKeyGenerator.generate(someAppId, someClusterName, someNamespaceName);

  }

  @Test
  public void testChangeConfigurationsWithAdd() {
    String key1 = "key1";
    String value1 = "value1";

    String key2 = "key2";
    String value2 = "value2";

    Map<String, String> latestConfig = ImmutableMap.of(key1, value1, key2, value2);
    Map<String, String> historyConfig = ImmutableMap.of(key1, value1);

    List<ConfigurationChange> result =
        configServiceWithChangeCache.calcConfigurationChanges(latestConfig, historyConfig);

    assertEquals(1, result.size());
    assertEquals(key2, result.get(0).getKey());
    assertEquals(value2, result.get(0).getNewValue());
    assertEquals("ADDED", result.get(0).getConfigurationChangeType());
  }

  @Test
  public void testChangeConfigurationsWithLatestConfigIsNULL() {
    String key1 = "key1";
    String value1 = "value1";

    Map<String, String> historyConfig = ImmutableMap.of(key1, value1);

    List<ConfigurationChange> result =
        configServiceWithChangeCache.calcConfigurationChanges(null, historyConfig);

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

    List<ConfigurationChange> result =
        configServiceWithChangeCache.calcConfigurationChanges(latestConfig, null);

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
    Map<String, String> historyConfig = ImmutableMap.of(key1, value1);

    List<ConfigurationChange> result =
        configServiceWithChangeCache.calcConfigurationChanges(latestConfig, historyConfig);

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
    Map<String, String> historyConfig = ImmutableMap.of(key1, value1);

    List<ConfigurationChange> result =
        configServiceWithChangeCache.calcConfigurationChanges(latestConfig, historyConfig);

    assertEquals(1, result.size());
    assertEquals(key1, result.get(0).getKey());
    assertEquals(null, result.get(0).getNewValue());
    assertEquals("DELETED", result.get(0).getConfigurationChangeType());
  }

  @Test
  public void testFindReleasesByReleaseKeys() {
    when(releaseService.findByReleaseKey(someReleaseKey)).thenReturn
        (someRelease);

    Map<String, Release> someReleaseMap = configServiceWithChangeCache.findReleasesByReleaseKeys(
        Sets.newHashSet(someReleaseKey));
    Map<String, Release> anotherReleaseMap = configServiceWithChangeCache.findReleasesByReleaseKeys(
        Sets.newHashSet(someReleaseKey));

    int retryTimes = 100;

    for (int i = 0; i < retryTimes; i++) {
      configServiceWithChangeCache.findReleasesByReleaseKeys(Sets.newHashSet(someReleaseKey));
    }

    assertEquals(someRelease, someReleaseMap.get(someReleaseKey));
    assertEquals(someRelease, anotherReleaseMap.get(someReleaseKey));

    verify(releaseService, times(1)).findByReleaseKey(someReleaseKey);
  }

  @Test
  public void testFindReleasesByReleaseKeysWithReleaseNotFound() {
    when(releaseService.findByReleaseKey(someReleaseKey)).thenReturn
        (null);

    Map<String, Release> someReleaseMap = configServiceWithChangeCache.findReleasesByReleaseKeys(
        Sets.newHashSet(someReleaseKey));
    Map<String, Release> anotherReleaseMap = configServiceWithChangeCache.findReleasesByReleaseKeys(
        Sets.newHashSet(someReleaseKey));

    int retryTimes = 100;

    for (int i = 0; i < retryTimes; i++) {
      configServiceWithChangeCache.findReleasesByReleaseKeys(Sets.newHashSet(someReleaseKey));
    }

    assertNull(someReleaseMap);
    assertNull(anotherReleaseMap);

    verify(releaseService, times(1)).findByReleaseKey(someReleaseKey);
  }

  @Test
  public void testFindReleasesByReleaseKeysWithReleaseMessageNotification() {
    ReleaseMessage someReleaseMessage = mock(ReleaseMessage.class);

    when(releaseService.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName)).thenReturn(someRelease);
    when(someReleaseMessage.getMessage()).thenReturn(someKey);
    when(someRelease.getReleaseKey()).thenReturn(someReleaseKey);

    configServiceWithChangeCache.handleMessage(someReleaseMessage, Topics.APOLLO_RELEASE_TOPIC);
    Map<String, Release> someReleaseMap = configServiceWithChangeCache.findReleasesByReleaseKeys(
        Sets.newHashSet(someReleaseKey));
    Map<String, Release> anotherReleaseMap = configServiceWithChangeCache.findReleasesByReleaseKeys(
        Sets.newHashSet(someReleaseKey));

    assertEquals(someRelease, someReleaseMap.get(someReleaseKey));
    assertEquals(someRelease, anotherReleaseMap.get(someReleaseKey));

    verify(releaseService, times(0)).findByReleaseKey(someKey);
  }

}
