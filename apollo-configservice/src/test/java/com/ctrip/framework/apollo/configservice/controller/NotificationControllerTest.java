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
package com.ctrip.framework.apollo.configservice.controller;

import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.message.Topics;
import com.ctrip.framework.apollo.biz.utils.EntityManagerUtil;
import com.ctrip.framework.apollo.configservice.service.ReleaseMessageServiceWithCache;
import com.ctrip.framework.apollo.configservice.util.NamespaceUtil;
import com.ctrip.framework.apollo.configservice.util.WatchKeysUtil;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;
import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationControllerTest {
  private NotificationController controller;
  private String someAppId;
  private String someCluster;
  private String defaultNamespace;
  private String someDataCenter;
  private long someNotificationId;
  private String someClientIp;
  @Mock
  private ReleaseMessageServiceWithCache releaseMessageService;
  @Mock
  private EntityManagerUtil entityManagerUtil;
  @Mock
  private NamespaceUtil namespaceUtil;
  @Mock
  private WatchKeysUtil watchKeysUtil;

  private Multimap<String, DeferredResult<ResponseEntity<ApolloConfigNotification>>>
      deferredResults;

  @Before
  public void setUp() throws Exception {
    controller = new NotificationController(watchKeysUtil, releaseMessageService, entityManagerUtil, namespaceUtil);

    someAppId = "someAppId";
    someCluster = "someCluster";
    defaultNamespace = ConfigConsts.NAMESPACE_APPLICATION;
    someDataCenter = "someDC";
    someNotificationId = 1;
    someClientIp = "someClientIp";

    when(namespaceUtil.filterNamespaceName(defaultNamespace)).thenReturn(defaultNamespace);

    deferredResults =
        (Multimap<String, DeferredResult<ResponseEntity<ApolloConfigNotification>>>) ReflectionTestUtils
            .getField(controller, "deferredResults");
  }

  @Test
  public void testPollNotificationWithDefaultNamespace() throws Exception {
    String someWatchKey = "someKey";
    String anotherWatchKey = "anotherKey";

    Set<String> watchKeys = Sets.newHashSet(someWatchKey, anotherWatchKey);

    when(watchKeysUtil
        .assembleAllWatchKeys(someAppId, someCluster, defaultNamespace,
            someDataCenter)).thenReturn(
        watchKeys);

    DeferredResult<ResponseEntity<ApolloConfigNotification>>
        deferredResult = controller
        .pollNotification(someAppId, someCluster, defaultNamespace, someDataCenter,
            someNotificationId, someClientIp);

    assertEquals(watchKeys.size(), deferredResults.size());

    for (String watchKey : watchKeys) {
      assertTrue(deferredResults.get(watchKey).contains(deferredResult));
    }
  }

  @Test
  public void testPollNotificationWithDefaultNamespaceAsFile() throws Exception {
    String namespace = String.format("%s.%s", defaultNamespace, "properties");
    when(namespaceUtil.filterNamespaceName(namespace)).thenReturn(defaultNamespace);

    String someWatchKey = "someKey";
    String anotherWatchKey = "anotherKey";

    Set<String> watchKeys = Sets.newHashSet(someWatchKey, anotherWatchKey);

    when(watchKeysUtil
        .assembleAllWatchKeys(someAppId, someCluster, defaultNamespace,
            someDataCenter)).thenReturn(
        watchKeys);

    DeferredResult<ResponseEntity<ApolloConfigNotification>>
        deferredResult = controller
        .pollNotification(someAppId, someCluster, namespace, someDataCenter,
            someNotificationId, someClientIp);

    assertEquals(watchKeys.size(), deferredResults.size());

    for (String watchKey : watchKeys) {
      assertTrue(deferredResults.get(watchKey).contains(deferredResult));
    }
  }

  @Test
  public void testPollNotificationWithSomeNamespaceAsFile() throws Exception {
    String namespace = "someNamespace.xml";

    when(namespaceUtil.filterNamespaceName(namespace)).thenReturn(namespace);

    String someWatchKey = "someKey";

    Set<String> watchKeys = Sets.newHashSet(someWatchKey);
    when(watchKeysUtil
        .assembleAllWatchKeys(someAppId, someCluster, namespace, someDataCenter))
        .thenReturn(
            watchKeys);

    DeferredResult<ResponseEntity<ApolloConfigNotification>>
        deferredResult = controller
        .pollNotification(someAppId, someCluster, namespace, someDataCenter,
            someNotificationId, someClientIp);

    assertEquals(watchKeys.size(), deferredResults.size());

    for (String watchKey : watchKeys) {
      assertTrue(deferredResults.get(watchKey).contains(deferredResult));
    }
  }

  @Test
  public void testPollNotificationWithDefaultNamespaceWithNotificationIdOutDated()
      throws Exception {
    long notificationId = someNotificationId + 1;
    ReleaseMessage someReleaseMessage = mock(ReleaseMessage.class);

    String someWatchKey = "someKey";

    Set<String> watchKeys = Sets.newHashSet(someWatchKey);
    when(watchKeysUtil
        .assembleAllWatchKeys(someAppId, someCluster, defaultNamespace,
            someDataCenter))
        .thenReturn(
            watchKeys);

    when(someReleaseMessage.getId()).thenReturn(notificationId);
    when(releaseMessageService.findLatestReleaseMessageForMessages(watchKeys))
        .thenReturn(someReleaseMessage);

    DeferredResult<ResponseEntity<ApolloConfigNotification>>
        deferredResult = controller
        .pollNotification(someAppId, someCluster, defaultNamespace, someDataCenter,
            someNotificationId, someClientIp);

    ResponseEntity<ApolloConfigNotification> result =
        (ResponseEntity<ApolloConfigNotification>) deferredResult.getResult();

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(defaultNamespace, result.getBody().getNamespaceName());
    assertEquals(notificationId, result.getBody().getNotificationId());
  }

  @Test
  public void testPollNotificationWithDefaultNamespaceAndHandleMessage() throws Exception {
    String someWatchKey = "someKey";
    String anotherWatchKey = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
        .join(someAppId, someCluster, defaultNamespace);

    Set<String> watchKeys = Sets.newHashSet(someWatchKey, anotherWatchKey);

    when(watchKeysUtil
        .assembleAllWatchKeys(someAppId, someCluster, defaultNamespace,
            someDataCenter)).thenReturn(
        watchKeys);

    DeferredResult<ResponseEntity<ApolloConfigNotification>>
        deferredResult = controller
        .pollNotification(someAppId, someCluster, defaultNamespace, someDataCenter,
            someNotificationId, someClientIp);

    long someId = 1;
    ReleaseMessage someReleaseMessage = new ReleaseMessage(anotherWatchKey);
    someReleaseMessage.setId(someId);

    controller.handleMessage(someReleaseMessage, Topics.APOLLO_RELEASE_TOPIC);

    ResponseEntity<ApolloConfigNotification> response =
        (ResponseEntity<ApolloConfigNotification>) deferredResult.getResult();
    ApolloConfigNotification notification = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(defaultNamespace, notification.getNamespaceName());
    assertEquals(someId, notification.getNotificationId());
  }
}
