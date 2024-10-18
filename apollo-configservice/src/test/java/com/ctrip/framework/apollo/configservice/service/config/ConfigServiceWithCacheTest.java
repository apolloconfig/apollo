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
import com.ctrip.framework.apollo.core.dto.ApolloNotificationMessages;
import com.google.common.collect.Lists;

import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.message.Topics;
import com.ctrip.framework.apollo.biz.service.ReleaseMessageService;
import com.ctrip.framework.apollo.biz.service.ReleaseService;
import com.ctrip.framework.apollo.biz.utils.ReleaseMessageKeyGenerator;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceWithCacheTest {
  private ConfigServiceWithCache configServiceWithCache;

  @Mock
  private ReleaseService releaseService;
  @Mock
  private ReleaseMessageService releaseMessageService;
  @Mock
  private Release someRelease;
  @Mock
  private ReleaseMessage someReleaseMessage;
  @Mock
  private BizConfig bizConfig;
  @Mock
  private MeterRegistry meterRegistry;
  @Mock
  private GrayReleaseRulesHolder grayReleaseRulesHolder;

  private String someAppId;
  private String someClusterName;
  private String someNamespaceName;
  private String someKey;
  private long someNotificationId;
  private ApolloNotificationMessages someNotificationMessages;

  @Before
  public void setUp() throws Exception {
    configServiceWithCache = new ConfigServiceWithCache(releaseService, releaseMessageService,
        grayReleaseRulesHolder, bizConfig, meterRegistry);

    configServiceWithCache.initialize();

    someAppId = "someAppId";
    someClusterName = "someClusterName";
    someNamespaceName = "someNamespaceName";
    someNotificationId = 1;

    someKey = ReleaseMessageKeyGenerator.generate(someAppId, someClusterName, someNamespaceName);

    someNotificationMessages = new ApolloNotificationMessages();
  }

  @Test
  public void testFindActiveOne() throws Exception {
    long someId = 1;

    when(releaseService.findActiveOne(someId)).thenReturn(someRelease);

    assertEquals(someRelease, configServiceWithCache.findActiveOne(someId, someNotificationMessages));

    verify(releaseService, times(1)).findActiveOne(someId);
  }

  @Test
  public void testFindActiveOneWithSameIdMultipleTimes() throws Exception {
    long someId = 1;

    when(releaseService.findActiveOne(someId)).thenReturn(someRelease);

    assertEquals(someRelease, configServiceWithCache.findActiveOne(someId, someNotificationMessages));
    assertEquals(someRelease, configServiceWithCache.findActiveOne(someId, someNotificationMessages));
    assertEquals(someRelease, configServiceWithCache.findActiveOne(someId, someNotificationMessages));

    verify(releaseService, times(1)).findActiveOne(someId);
  }

  @Test
  public void testFindActiveOneWithMultipleIdMultipleTimes() throws Exception {
    long someId = 1;
    long anotherId = 2;
    Release anotherRelease = mock(Release.class);

    when(releaseService.findActiveOne(someId)).thenReturn(someRelease);
    when(releaseService.findActiveOne(anotherId)).thenReturn(anotherRelease);

    assertEquals(someRelease, configServiceWithCache.findActiveOne(someId, someNotificationMessages));
    assertEquals(someRelease, configServiceWithCache.findActiveOne(someId, someNotificationMessages));

    assertEquals(anotherRelease, configServiceWithCache.findActiveOne(anotherId, someNotificationMessages));
    assertEquals(anotherRelease, configServiceWithCache.findActiveOne(anotherId, someNotificationMessages));

    verify(releaseService, times(1)).findActiveOne(someId);
    verify(releaseService, times(1)).findActiveOne(anotherId);
  }

  @Test
  public void testFindActiveOneWithReleaseNotFoundMultipleTimes() throws Exception {
    long someId = 1;

    when(releaseService.findActiveOne(someId)).thenReturn(null);

    assertNull(configServiceWithCache.findActiveOne(someId, someNotificationMessages));
    assertNull(configServiceWithCache.findActiveOne(someId, someNotificationMessages));
    assertNull(configServiceWithCache.findActiveOne(someId, someNotificationMessages));

    verify(releaseService, times(1)).findActiveOne(someId);
  }

  @Test
  public void testFindLatestActiveRelease() throws Exception {
    when(releaseMessageService.findLatestReleaseMessageForMessages(Lists.newArrayList(someKey))).thenReturn
        (someReleaseMessage);
    when(releaseService.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName)).thenReturn
        (someRelease);
    when(someReleaseMessage.getId()).thenReturn(someNotificationId);

    Release release = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName,
        someNotificationMessages);
    Release anotherRelease = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);

    int retryTimes = 100;

    for (int i = 0; i < retryTimes; i++) {
      configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
          someNamespaceName, someNotificationMessages);
    }

    assertEquals(someRelease, release);
    assertEquals(someRelease, anotherRelease);

    verify(releaseMessageService, times(1)).findLatestReleaseMessageForMessages(Lists.newArrayList(someKey));
    verify(releaseService, times(1)).findLatestActiveRelease(someAppId, someClusterName, someNamespaceName);
  }

  @Test
  public void testFindLatestActiveReleaseWithReleaseNotFound() throws Exception {
    when(releaseMessageService.findLatestReleaseMessageForMessages(Lists.newArrayList(someKey))).thenReturn(null);
    when(releaseService.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName)).thenReturn(null);

    Release release = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName,
        someNotificationMessages);
    Release anotherRelease = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);

    int retryTimes = 100;

    for (int i = 0; i < retryTimes; i++) {
      configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
          someNamespaceName, someNotificationMessages);
    }

    assertNull(release);
    assertNull(anotherRelease);

    verify(releaseMessageService, times(1)).findLatestReleaseMessageForMessages(Lists.newArrayList(someKey));
    verify(releaseService, times(1)).findLatestActiveRelease(someAppId, someClusterName, someNamespaceName);
  }

  @Test
  public void testFindLatestActiveReleaseWithDirtyRelease() throws Exception {
    long someNewNotificationId = someNotificationId + 1;
    ReleaseMessage anotherReleaseMessage = mock(ReleaseMessage.class);
    Release anotherRelease = mock(Release.class);

    when(releaseMessageService.findLatestReleaseMessageForMessages(Lists.newArrayList(someKey))).thenReturn
        (someReleaseMessage);
    when(releaseService.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName)).thenReturn
        (someRelease);
    when(someReleaseMessage.getId()).thenReturn(someNotificationId);

    Release release = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName,
        someNotificationMessages);

    when(releaseMessageService.findLatestReleaseMessageForMessages(Lists.newArrayList(someKey))).thenReturn
        (anotherReleaseMessage);
    when(releaseService.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName)).thenReturn
        (anotherRelease);
    when(anotherReleaseMessage.getId()).thenReturn(someNewNotificationId);

    Release stillOldRelease = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);

    someNotificationMessages.put(someKey, someNewNotificationId);

    Release shouldBeNewRelease = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);

    assertEquals(someRelease, release);
    assertEquals(someRelease, stillOldRelease);
    assertEquals(anotherRelease, shouldBeNewRelease);

    verify(releaseMessageService, times(2)).findLatestReleaseMessageForMessages(Lists.newArrayList(someKey));
    verify(releaseService, times(2)).findLatestActiveRelease(someAppId, someClusterName, someNamespaceName);
  }

  @Test
  public void testFindLatestActiveReleaseWithReleaseMessageNotification() throws Exception {
    long someNewNotificationId = someNotificationId + 1;
    ReleaseMessage anotherReleaseMessage = mock(ReleaseMessage.class);
    Release anotherRelease = mock(Release.class);

    when(releaseMessageService.findLatestReleaseMessageForMessages(Lists.newArrayList(someKey))).thenReturn
        (someReleaseMessage);
    when(releaseService.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName)).thenReturn
        (someRelease);
    when(someReleaseMessage.getId()).thenReturn(someNotificationId);

    Release release = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName,
        someNotificationMessages);

    when(releaseMessageService.findLatestReleaseMessageForMessages(Lists.newArrayList(someKey))).thenReturn
        (anotherReleaseMessage);
    when(releaseService.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName)).thenReturn
        (anotherRelease);
    when(anotherReleaseMessage.getMessage()).thenReturn(someKey);
    when(anotherReleaseMessage.getId()).thenReturn(someNewNotificationId);

    Release stillOldRelease = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);

    configServiceWithCache.handleMessage(anotherReleaseMessage, Topics.APOLLO_RELEASE_TOPIC);

    Release shouldBeNewRelease = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);

    assertEquals(someRelease, release);
    assertEquals(someRelease, stillOldRelease);
    assertEquals(anotherRelease, shouldBeNewRelease);

    verify(releaseMessageService, times(2)).findLatestReleaseMessageForMessages(Lists.newArrayList(someKey));
    verify(releaseService, times(2)).findLatestActiveRelease(someAppId, someClusterName, someNamespaceName);
  }

  @Test
  public void testFindLatestActiveReleaseWithIrrelevantMessages() throws Exception {
    long someNewNotificationId = someNotificationId + 1;
    String someIrrelevantKey = "someIrrelevantKey";

    when(releaseMessageService.findLatestReleaseMessageForMessages(Lists.newArrayList(someKey))).thenReturn
        (someReleaseMessage);
    when(releaseService.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName)).thenReturn
        (someRelease);
    when(someReleaseMessage.getId()).thenReturn(someNotificationId);

    Release release = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName, someNamespaceName,
        someNotificationMessages);

    Release stillOldRelease = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);

    someNotificationMessages.put(someIrrelevantKey, someNewNotificationId);

    Release shouldStillBeOldRelease = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);

    assertEquals(someRelease, release);
    assertEquals(someRelease, stillOldRelease);
    assertEquals(someRelease, shouldStillBeOldRelease);

    verify(releaseMessageService, times(1)).findLatestReleaseMessageForMessages(Lists.newArrayList(someKey));
    verify(releaseService, times(1)).findLatestActiveRelease(someAppId, someClusterName, someNamespaceName);
  }
}
