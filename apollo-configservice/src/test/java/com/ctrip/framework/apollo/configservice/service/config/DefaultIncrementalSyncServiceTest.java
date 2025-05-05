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
public class DefaultIncrementalSyncServiceTest {

  private DefaultIncrementalSyncService defaultIncrementalSyncService;

  @Mock
  private ReleaseService releaseService;

  @Mock
  private ReleaseMessageService releaseMessageService;
  @Mock
  private Release someRelease;

  private String someKey;

  private String someReleaseKey;

  private String someAppId;
  private String someClusterName;
  private String someNamespaceName;

  private String newReleaseKey;



  @Before
  public void setUp() throws Exception {
    defaultIncrementalSyncService = new DefaultIncrementalSyncService();

    someReleaseKey = "someReleaseKey";
    someAppId = "someAppId";
    someClusterName = "someClusterName";
    someNamespaceName = "someNamespaceName";

    newReleaseKey = "someReleaseKey";

    someKey = ReleaseMessageKeyGenerator.generate(someAppId, someClusterName, someNamespaceName);

  }

  @Test
  public void testChangeConfigurationsWithAdd() {
    String key1 = "key1";
    String value1 = "value1";

    String key2 = "key2";
    String value2 = "value2";

    Map<String, String> latestConfig = ImmutableMap.of(key1, value1, key2, value2);
    Map<String, String> clientSideConfigurations = ImmutableMap.of(key1, value1);

    List<ConfigurationChange> result =
        defaultIncrementalSyncService.getConfigurationChanges(newReleaseKey, latestConfig,
            someReleaseKey, clientSideConfigurations);

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

    List<ConfigurationChange> result =
        defaultIncrementalSyncService.getConfigurationChanges(newReleaseKey, null, someReleaseKey,
            clientSideConfigurations);

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

    List<ConfigurationChange> result = defaultIncrementalSyncService.getConfigurationChanges(
        newReleaseKey, latestConfig, someReleaseKey, null);


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

    List<ConfigurationChange> result =
        defaultIncrementalSyncService.getConfigurationChanges(newReleaseKey, latestConfig,
            someReleaseKey, clientSideConfigurations);

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

    List<ConfigurationChange> result =
        defaultIncrementalSyncService.getConfigurationChanges(newReleaseKey, latestConfig,
            someReleaseKey, clientSideConfigurations);

    assertEquals(1, result.size());
    assertEquals(key1, result.get(0).getKey());
    assertEquals(null, result.get(0).getNewValue());
    assertEquals("DELETED", result.get(0).getConfigurationChangeType());
  }

}
