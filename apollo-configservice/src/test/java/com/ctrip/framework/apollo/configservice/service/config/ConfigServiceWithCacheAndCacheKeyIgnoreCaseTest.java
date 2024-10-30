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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.grayReleaseRule.GrayReleaseRulesHolder;
import com.ctrip.framework.apollo.biz.message.Topics;
import com.ctrip.framework.apollo.biz.service.ReleaseMessageService;
import com.ctrip.framework.apollo.biz.service.ReleaseService;
import com.ctrip.framework.apollo.biz.utils.ReleaseMessageKeyGenerator;
import com.ctrip.framework.apollo.core.dto.ApolloNotificationMessages;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.regex.Pattern;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author kl (http://kailing.pub)
 * @since 2023/3/28
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceWithCacheAndCacheKeyIgnoreCaseTest {

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
  private String lowerCaseSomeKey;
  private String normalSomeKey;
  private long someNotificationId;
  private ApolloNotificationMessages someNotificationMessages;

  @Before
  public void setUp() throws Exception {
    configServiceWithCache = new ConfigServiceWithCache(releaseService, releaseMessageService,
        grayReleaseRulesHolder, bizConfig, meterRegistry);

    when(bizConfig.isConfigServiceCacheKeyIgnoreCase()).thenReturn(true);

    configServiceWithCache.initialize();

    someAppId = "someAppId";
    someClusterName = "someClusterName";
    someNamespaceName = "someNamespaceName";
    someNotificationId = 1;

    normalSomeKey = ReleaseMessageKeyGenerator.generate(someAppId, someClusterName, someNamespaceName);
    lowerCaseSomeKey = normalSomeKey.toLowerCase();
    someNotificationMessages = new ApolloNotificationMessages();
  }

  @Test
  public void testFindActiveOne() {
    long someId = 1;

    when(releaseService.findActiveOne(someId)).thenReturn(someRelease);

    assertEquals(someRelease,
        configServiceWithCache.findActiveOne(someId, someNotificationMessages));
    verify(releaseService, times(1)).findActiveOne(someId);
  }

  @Test
  public void testFindActiveOneWithSameIdMultipleTimes() {
    long someId = 1;

    when(releaseService.findActiveOne(someId)).thenReturn(someRelease);

    assertEquals(someRelease,
        configServiceWithCache.findActiveOne(someId, someNotificationMessages));
    assertEquals(someRelease,
        configServiceWithCache.findActiveOne(someId, someNotificationMessages));
    assertEquals(someRelease,
        configServiceWithCache.findActiveOne(someId, someNotificationMessages));

    verify(releaseService, times(1)).findActiveOne(someId);
  }

  @Test
  public void testFindActiveOneWithMultipleIdMultipleTimes() {
    long someId = 1;
    long anotherId = 2;
    Release anotherRelease = mock(Release.class);

    when(releaseService.findActiveOne(someId)).thenReturn(someRelease);
    when(releaseService.findActiveOne(anotherId)).thenReturn(anotherRelease);

    assertEquals(someRelease,
        configServiceWithCache.findActiveOne(someId, someNotificationMessages));
    assertEquals(someRelease,
        configServiceWithCache.findActiveOne(someId, someNotificationMessages));

    assertEquals(anotherRelease,
        configServiceWithCache.findActiveOne(anotherId, someNotificationMessages));
    assertEquals(anotherRelease,
        configServiceWithCache.findActiveOne(anotherId, someNotificationMessages));

    verify(releaseService, times(1)).findActiveOne(someId);
    verify(releaseService, times(1)).findActiveOne(anotherId);
  }

  @Test
  public void testFindActiveOneWithReleaseNotFoundMultipleTimes() {
    long someId = 1;

    when(releaseService.findActiveOne(someId)).thenReturn(null);

    assertNull(configServiceWithCache.findActiveOne(someId, someNotificationMessages));
    assertNull(configServiceWithCache.findActiveOne(someId, someNotificationMessages));
    assertNull(configServiceWithCache.findActiveOne(someId, someNotificationMessages));

    verify(releaseService, times(1)).findActiveOne(someId);
  }

  @Test
  public void testFindLatestActiveRelease() {
    when(releaseMessageService.findLatestReleaseMessageForMessages(Lists.newArrayList(lowerCaseSomeKey)))
        .thenReturn(someReleaseMessage);
    when(releaseService.findLatestActiveRelease(
        matchesCaseInsensitive(someAppId),
        matchesCaseInsensitive(someClusterName),
        matchesCaseInsensitive(someNamespaceName)
    )).thenReturn(someRelease);
    when(someReleaseMessage.getId()).thenReturn(someNotificationId);

    Release release = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);
    Release anotherRelease = configServiceWithCache.findLatestActiveRelease(someAppId,
        someClusterName, someNamespaceName, someNotificationMessages);

    int retryTimes = 100;

    for (int i = 0; i < retryTimes; i++) {
      configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
          someNamespaceName, someNotificationMessages);
    }

    assertEquals(someRelease, release);
    assertEquals(someRelease, anotherRelease);

    verify(releaseMessageService, times(1)).findLatestReleaseMessageForMessages(
        Lists.newArrayList(lowerCaseSomeKey));
    verify(releaseService, times(1)).findLatestActiveRelease(
        someAppId.toLowerCase(),
        someClusterName.toLowerCase(),
        someNamespaceName.toLowerCase());
  }

  @Test
  public void testFindLatestActiveReleaseWithReleaseNotFound() {
    when(releaseMessageService.findLatestReleaseMessageForMessages(
        Lists.newArrayList(lowerCaseSomeKey))).thenReturn(null);
    when(releaseService.findLatestActiveRelease(
        matchesCaseInsensitive(someAppId),
        matchesCaseInsensitive(someClusterName),
        matchesCaseInsensitive(someNamespaceName)
    )).thenReturn(null);

    Release release = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName,
        someNotificationMessages);
    Release anotherRelease = configServiceWithCache.findLatestActiveRelease(someAppId,
        someClusterName,
        someNamespaceName, someNotificationMessages);

    int retryTimes = 100;

    for (int i = 0; i < retryTimes; i++) {
      configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
          someNamespaceName, someNotificationMessages);
    }

    assertNull(release);
    assertNull(anotherRelease);

    verify(releaseMessageService, times(1)).findLatestReleaseMessageForMessages(
        Lists.newArrayList(lowerCaseSomeKey));
    verify(releaseService, times(1)).findLatestActiveRelease(
        someAppId.toLowerCase(),
        someClusterName.toLowerCase(),
        someNamespaceName.toLowerCase());
  }

  @Test
  public void testFindLatestActiveReleaseWithDirtyRelease() {
    long someNewNotificationId = someNotificationId + 1;
    ReleaseMessage anotherReleaseMessage = mock(ReleaseMessage.class);
    Release anotherRelease = mock(Release.class);

    when(releaseMessageService.findLatestReleaseMessageForMessages(
        Lists.newArrayList(lowerCaseSomeKey))).thenReturn
        (someReleaseMessage);
    when(releaseService.findLatestActiveRelease(
        matchesCaseInsensitive(someAppId),
        matchesCaseInsensitive(someClusterName),
        matchesCaseInsensitive(someNamespaceName)
    )).thenReturn(someRelease);
    when(someReleaseMessage.getId()).thenReturn(someNotificationId);

    Release release = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName,
        someNotificationMessages);

    when(releaseMessageService.findLatestReleaseMessageForMessages(
        Lists.newArrayList(lowerCaseSomeKey))).thenReturn(anotherReleaseMessage);
    when(releaseService.findLatestActiveRelease(
        matchesCaseInsensitive(someAppId),
        matchesCaseInsensitive(someClusterName),
        matchesCaseInsensitive(someNamespaceName)
    )).thenReturn(anotherRelease);
    when(anotherReleaseMessage.getId()).thenReturn(someNewNotificationId);

    Release stillOldRelease = configServiceWithCache.findLatestActiveRelease(someAppId,
        someClusterName,
        someNamespaceName, someNotificationMessages);

    someNotificationMessages.put(
        ReleaseMessageKeyGenerator.generate(someAppId, someClusterName, someNamespaceName),
        someNewNotificationId);

    Release shouldBeNewRelease = configServiceWithCache.findLatestActiveRelease(someAppId,
        someClusterName,
        someNamespaceName, someNotificationMessages);

    assertEquals(someRelease, release);
    assertEquals(someRelease, stillOldRelease);
    assertEquals(anotherRelease, shouldBeNewRelease);

    verify(releaseMessageService, times(2)).findLatestReleaseMessageForMessages(
        Lists.newArrayList(lowerCaseSomeKey));
    verify(releaseService, times(2)).findLatestActiveRelease(
        someAppId.toLowerCase(),
        someClusterName.toLowerCase(),
        someNamespaceName.toLowerCase());
  }

  @Test
  public void testFindLatestActiveReleaseWithReleaseMessageNotification() {
    long someNewNotificationId = someNotificationId + 1;
    ReleaseMessage anotherReleaseMessage = mock(ReleaseMessage.class);
    Release anotherRelease = mock(Release.class);

    when(releaseMessageService.findLatestReleaseMessageForMessages(Lists.newArrayList(lowerCaseSomeKey)))
        .thenReturn(someReleaseMessage);
    when(releaseService.findLatestActiveRelease(
        matchesCaseInsensitive(someAppId),
        matchesCaseInsensitive(someClusterName),
        matchesCaseInsensitive(someNamespaceName)
    )).thenReturn(someRelease);
    when(someReleaseMessage.getId()).thenReturn(someNotificationId);

    Release release = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName,
        someNotificationMessages);

    when(releaseMessageService.findLatestReleaseMessageForMessages(Lists.newArrayList(lowerCaseSomeKey)))
        .thenReturn(anotherReleaseMessage);
    when(releaseService.findLatestActiveRelease(
        matchesCaseInsensitive(someAppId),
        matchesCaseInsensitive(someClusterName),
        matchesCaseInsensitive(someNamespaceName)
    )).thenReturn(anotherRelease);

    when(anotherReleaseMessage.getMessage()).thenReturn(lowerCaseSomeKey);
    when(anotherReleaseMessage.getId()).thenReturn(someNewNotificationId);

    Release stillOldRelease = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);

    configServiceWithCache.handleMessage(anotherReleaseMessage, Topics.APOLLO_RELEASE_TOPIC);

    Release shouldBeNewRelease = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);

    assertEquals(someRelease, release);
    assertEquals(someRelease, stillOldRelease);
    assertEquals(anotherRelease, shouldBeNewRelease);

    verify(releaseMessageService, times(2)).findLatestReleaseMessageForMessages(
        Lists.newArrayList(lowerCaseSomeKey));
    verify(releaseService, times(2)).findLatestActiveRelease(
        someAppId.toLowerCase(),
        someClusterName.toLowerCase(),
        someNamespaceName.toLowerCase());

    when(anotherReleaseMessage.getMessage()).thenReturn(normalSomeKey);
    when(anotherReleaseMessage.getId()).thenReturn(someNewNotificationId);

    configServiceWithCache.handleMessage(anotherReleaseMessage, Topics.APOLLO_RELEASE_TOPIC);

    shouldBeNewRelease = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName, someNotificationMessages);
    assertEquals(anotherRelease, shouldBeNewRelease);
  }

  @Test
  public void testFindLatestActiveReleaseWithIrrelevantMessages() {
    long someNewNotificationId = someNotificationId + 1;
    String someIrrelevantKey = "someIrrelevantKey";

    when(releaseMessageService.findLatestReleaseMessageForMessages(Lists.newArrayList(lowerCaseSomeKey)))
        .thenReturn(someReleaseMessage);
    when(releaseService.findLatestActiveRelease(
        matchesCaseInsensitive(someAppId),
        matchesCaseInsensitive(someClusterName),
        matchesCaseInsensitive(someNamespaceName)
    )).thenReturn(someRelease);
    when(someReleaseMessage.getId()).thenReturn(someNotificationId);

    Release release = configServiceWithCache.findLatestActiveRelease(someAppId, someClusterName,
        someNamespaceName,
        someNotificationMessages);

    Release stillOldRelease = configServiceWithCache.findLatestActiveRelease(someAppId,
        someClusterName,
        someNamespaceName, someNotificationMessages);

    someNotificationMessages.put(someIrrelevantKey, someNewNotificationId);

    Release shouldStillBeOldRelease = configServiceWithCache.findLatestActiveRelease(someAppId,
        someClusterName,
        someNamespaceName, someNotificationMessages);

    assertEquals(someRelease, release);
    assertEquals(someRelease, stillOldRelease);
    assertEquals(someRelease, shouldStillBeOldRelease);

    verify(releaseMessageService, times(1)).findLatestReleaseMessageForMessages(
        Lists.newArrayList(lowerCaseSomeKey));
    verify(releaseService, times(1)).findLatestActiveRelease(
        someAppId.toLowerCase(),
        someClusterName.toLowerCase(),
        someNamespaceName.toLowerCase());
  }

  private String matchesCaseInsensitive(final String regex) {
    return matches(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
  }

}
