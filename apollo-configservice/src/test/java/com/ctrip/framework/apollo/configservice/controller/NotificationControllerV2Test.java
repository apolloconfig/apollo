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

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.message.Topics;
import com.ctrip.framework.apollo.biz.utils.EntityManagerUtil;
import com.ctrip.framework.apollo.configservice.service.ReleaseMessageServiceWithCache;
import com.ctrip.framework.apollo.configservice.util.NamespaceUtil;
import com.ctrip.framework.apollo.configservice.util.WatchKeysUtil;
import com.ctrip.framework.apollo.configservice.wrapper.DeferredResultWrapper;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;
import com.ctrip.framework.apollo.core.dto.ApolloNotificationMessages;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class NotificationControllerV2Test {
  private NotificationControllerV2 controller;
  private String someAppId;
  private String someCluster;
  private String defaultCluster;
  private String defaultNamespace;
  private String somePublicNamespace;
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
  @Mock
  private BizConfig bizConfig;

  private Gson gson;

  private Multimap<String, DeferredResultWrapper> deferredResults;

  @Before
  public void setUp() throws Exception {
    gson = new Gson();
    controller = new NotificationControllerV2(
        watchKeysUtil, releaseMessageService, entityManagerUtil, namespaceUtil, gson, bizConfig
    );

    when(bizConfig.releaseMessageNotificationBatch()).thenReturn(100);
    when(bizConfig.releaseMessageNotificationBatchIntervalInMilli()).thenReturn(5);

    someAppId = "someAppId";
    someCluster = "someCluster";
    defaultCluster = ConfigConsts.CLUSTER_NAME_DEFAULT;
    defaultNamespace = ConfigConsts.NAMESPACE_APPLICATION;
    somePublicNamespace = "somePublicNamespace";
    someDataCenter = "someDC";
    someNotificationId = 1;
    someClientIp = "someClientIp";

    when(namespaceUtil.filterNamespaceName(defaultNamespace)).thenReturn(defaultNamespace);
    when(namespaceUtil.filterNamespaceName(somePublicNamespace)).thenReturn(somePublicNamespace);
    when(namespaceUtil.normalizeNamespace(someAppId, defaultNamespace)).thenReturn(defaultNamespace);
    when(namespaceUtil.normalizeNamespace(someAppId, somePublicNamespace)).thenReturn(somePublicNamespace);

    deferredResults =
        (Multimap<String, DeferredResultWrapper>) ReflectionTestUtils.getField(controller, "deferredResults");
  }

  @Test
  public void testPollNotificationWithDefaultNamespace() throws Exception {
    String someWatchKey = "someKey";
    String anotherWatchKey = "anotherKey";

    Multimap<String, String> watchKeysMap =
        assembleMultiMap(defaultNamespace, Lists.newArrayList(someWatchKey, anotherWatchKey));

    String notificationAsString =
        transformApolloConfigNotificationsToString(defaultNamespace, someNotificationId);

    when(watchKeysUtil
        .assembleAllWatchKeys(someAppId, someCluster, Sets.newHashSet(defaultNamespace),
            someDataCenter)).thenReturn(
        watchKeysMap);

    DeferredResult<ResponseEntity<List<ApolloConfigNotification>>>
        deferredResult = controller
        .pollNotification(someAppId, someCluster, notificationAsString, someDataCenter,
            someClientIp);

    assertEquals(watchKeysMap.size(), deferredResults.size());

    assertWatchKeys(watchKeysMap, deferredResult);
  }

  @Test
  public void testPollNotificationWithDefaultNamespaceAsFile() throws Exception {
    String namespace = String.format("%s.%s", defaultNamespace, "properties");
    when(namespaceUtil.filterNamespaceName(namespace)).thenReturn(defaultNamespace);

    String someWatchKey = "someKey";
    String anotherWatchKey = "anotherKey";

    Multimap<String, String> watchKeysMap =
        assembleMultiMap(defaultNamespace, Lists.newArrayList(someWatchKey, anotherWatchKey));

    String notificationAsString =
        transformApolloConfigNotificationsToString(namespace, someNotificationId);

    when(watchKeysUtil
        .assembleAllWatchKeys(someAppId, someCluster, Sets.newHashSet(defaultNamespace),
            someDataCenter)).thenReturn(
        watchKeysMap);

    DeferredResult<ResponseEntity<List<ApolloConfigNotification>>>
        deferredResult = controller
        .pollNotification(someAppId, someCluster, notificationAsString, someDataCenter,
            someClientIp);

    assertEquals(watchKeysMap.size(), deferredResults.size());

    assertWatchKeys(watchKeysMap, deferredResult);
  }


  @Test
  public void testPollNotificationWithMultipleNamespaces() throws Exception {
    String defaultNamespaceAsFile = defaultNamespace + ".properties";
    String somePublicNamespaceAsFile = somePublicNamespace + ".xml";

    when(namespaceUtil.filterNamespaceName(defaultNamespaceAsFile)).thenReturn(defaultNamespace);
    when(namespaceUtil.filterNamespaceName(somePublicNamespaceAsFile)).thenReturn(somePublicNamespaceAsFile);
    when(namespaceUtil.normalizeNamespace(someAppId, somePublicNamespaceAsFile)).thenReturn(somePublicNamespaceAsFile);

    String someWatchKey = "someKey";
    String anotherWatchKey = "anotherKey";
    String somePublicWatchKey = "somePublicWatchKey";
    String somePublicFileWatchKey = "somePublicFileWatchKey";

    Multimap<String, String> watchKeysMap =
        assembleMultiMap(defaultNamespace, Lists.newArrayList(someWatchKey, anotherWatchKey));
    watchKeysMap
        .putAll(assembleMultiMap(somePublicNamespace, Lists.newArrayList(somePublicWatchKey)));
    watchKeysMap
        .putAll(assembleMultiMap(somePublicNamespaceAsFile,
            Lists.newArrayList(somePublicFileWatchKey)));

    String notificationAsString =
        transformApolloConfigNotificationsToString(defaultNamespaceAsFile, someNotificationId,
            somePublicNamespace, someNotificationId, somePublicNamespaceAsFile,
            someNotificationId);

    when(watchKeysUtil
        .assembleAllWatchKeys(someAppId, someCluster,
            Sets.newHashSet(defaultNamespace, somePublicNamespace, somePublicNamespaceAsFile),
            someDataCenter)).thenReturn(
        watchKeysMap);

    DeferredResult<ResponseEntity<List<ApolloConfigNotification>>>
        deferredResult = controller
        .pollNotification(someAppId, someCluster, notificationAsString, someDataCenter,
            someClientIp);

    assertEquals(watchKeysMap.size(), deferredResults.size());

    assertWatchKeys(watchKeysMap, deferredResult);

    verify(watchKeysUtil, times(1)).assembleAllWatchKeys(someAppId, someCluster,
        Sets.newHashSet(defaultNamespace, somePublicNamespace, somePublicNamespaceAsFile),
        someDataCenter);
  }

  @Test
  public void testPollNotificationWithMultipleNamespaceWithNotificationIdOutDated()
      throws Exception {
    String someWatchKey = "someKey";
    String anotherWatchKey = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
        .join(someAppId, someCluster, somePublicNamespace);
    String yetAnotherWatchKey = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
        .join(someAppId, defaultCluster, somePublicNamespace);
    long notificationId = someNotificationId + 1;
    long yetAnotherNotificationId = someNotificationId;

    Multimap<String, String> watchKeysMap =
        assembleMultiMap(defaultNamespace, Lists.newArrayList(someWatchKey));
    watchKeysMap
        .putAll(assembleMultiMap(somePublicNamespace, Lists.newArrayList(anotherWatchKey, yetAnotherWatchKey)));

    when(watchKeysUtil
        .assembleAllWatchKeys(someAppId, someCluster,
            Sets.newHashSet(defaultNamespace, somePublicNamespace), someDataCenter)).thenReturn(
        watchKeysMap);

    ReleaseMessage someReleaseMessage = mock(ReleaseMessage.class);
    when(someReleaseMessage.getId()).thenReturn(notificationId);
    when(someReleaseMessage.getMessage()).thenReturn(anotherWatchKey);
    ReleaseMessage yetAnotherReleaseMessage = mock(ReleaseMessage.class);
    when(yetAnotherReleaseMessage.getId()).thenReturn(yetAnotherNotificationId);
    when(yetAnotherReleaseMessage.getMessage()).thenReturn(yetAnotherWatchKey);
    when(releaseMessageService
        .findLatestReleaseMessagesGroupByMessages(Sets.newHashSet(watchKeysMap.values())))
        .thenReturn(Lists.newArrayList(someReleaseMessage, yetAnotherReleaseMessage));

    String notificationAsString =
        transformApolloConfigNotificationsToString(defaultNamespace, someNotificationId,
            somePublicNamespace, someNotificationId);

    DeferredResult<ResponseEntity<List<ApolloConfigNotification>>>
        deferredResult = controller
        .pollNotification(someAppId, someCluster, notificationAsString, someDataCenter,
            someClientIp);

    ResponseEntity<List<ApolloConfigNotification>> result =
        (ResponseEntity<List<ApolloConfigNotification>>) deferredResult.getResult();

    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, result.getBody().size());
    assertEquals(somePublicNamespace, result.getBody().get(0).getNamespaceName());
    assertEquals(notificationId, result.getBody().get(0).getNotificationId());

    ApolloNotificationMessages notificationMessages = result.getBody().get(0).getMessages();
    assertEquals(2, notificationMessages.getDetails().size());
    assertEquals(notificationId, notificationMessages.get(anotherWatchKey).longValue());
    assertEquals(yetAnotherNotificationId, notificationMessages.get(yetAnotherWatchKey).longValue());
  }

  @Test
  public void testPollNotificationWithMultipleNamespacesAndHandleMessage() throws Exception {
    String someWatchKey = "someKey";
    String anotherWatchKey = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
        .join(someAppId, someCluster, somePublicNamespace);

    Multimap<String, String> watchKeysMap =
        assembleMultiMap(defaultNamespace, Lists.newArrayList(someWatchKey));
    watchKeysMap
        .putAll(assembleMultiMap(somePublicNamespace, Lists.newArrayList(anotherWatchKey)));

    when(watchKeysUtil
        .assembleAllWatchKeys(someAppId, someCluster,
            Sets.newHashSet(defaultNamespace, somePublicNamespace), someDataCenter)).thenReturn(
        watchKeysMap);

    String notificationAsString =
        transformApolloConfigNotificationsToString(defaultNamespace, someNotificationId,
            somePublicNamespace, someNotificationId);

    DeferredResult<ResponseEntity<List<ApolloConfigNotification>>>
        deferredResult = controller
        .pollNotification(someAppId, someCluster, notificationAsString, someDataCenter,
            someClientIp);

    assertEquals(watchKeysMap.size(), deferredResults.size());

    long someId = 1;
    ReleaseMessage someReleaseMessage = new ReleaseMessage(anotherWatchKey);
    someReleaseMessage.setId(someId);

    controller.handleMessage(someReleaseMessage, Topics.APOLLO_RELEASE_TOPIC);

    ResponseEntity<List<ApolloConfigNotification>> response =
        (ResponseEntity<List<ApolloConfigNotification>>) deferredResult.getResult();

    assertEquals(1, response.getBody().size());
    ApolloConfigNotification notification = response.getBody().get(0);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(somePublicNamespace, notification.getNamespaceName());
    assertEquals(someId, notification.getNotificationId());

    ApolloNotificationMessages notificationMessages = response.getBody().get(0).getMessages();
    assertEquals(1, notificationMessages.getDetails().size());
    assertEquals(someId, notificationMessages.get(anotherWatchKey).longValue());
  }

  @Test
  public void testPollNotificationWithHandleMessageInBatch() throws Exception {
    String someWatchKey = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
        .join(someAppId, someCluster, defaultNamespace);
    int someBatch = 1;
    int someBatchInterval = 10;

    Multimap<String, String> watchKeysMap =
        assembleMultiMap(defaultNamespace, Lists.newArrayList(someWatchKey));

    String notificationAsString =
        transformApolloConfigNotificationsToString(defaultNamespace, someNotificationId);

    when(watchKeysUtil
        .assembleAllWatchKeys(someAppId, someCluster, Sets.newHashSet(defaultNamespace),
            someDataCenter)).thenReturn(watchKeysMap);

    when(bizConfig.releaseMessageNotificationBatch()).thenReturn(someBatch);
    when(bizConfig.releaseMessageNotificationBatchIntervalInMilli()).thenReturn(someBatchInterval);

    DeferredResult<ResponseEntity<List<ApolloConfigNotification>>>
        deferredResult = controller
        .pollNotification(someAppId, someCluster, notificationAsString, someDataCenter,
            someClientIp);
    DeferredResult<ResponseEntity<List<ApolloConfigNotification>>>
        anotherDeferredResult = controller
        .pollNotification(someAppId, someCluster, notificationAsString, someDataCenter,
            someClientIp);

    long someId = 1;
    ReleaseMessage someReleaseMessage = new ReleaseMessage(someWatchKey);
    someReleaseMessage.setId(someId);

    controller.handleMessage(someReleaseMessage, Topics.APOLLO_RELEASE_TOPIC);

    //in batch mode, at most one of them should have result
    assertFalse(deferredResult.hasResult() && anotherDeferredResult.hasResult());

    //now both of them should have result
    await().atMost(someBatchInterval * 500, TimeUnit.MILLISECONDS).untilAsserted(
        () -> assertTrue(deferredResult.hasResult() && anotherDeferredResult.hasResult()));
  }

  @Test
  public void testPollNotificationWithIncorrectCase() throws Exception {
    String appIdWithIncorrectCase = someAppId.toUpperCase();
    String namespaceWithIncorrectCase = defaultNamespace.toUpperCase();
    String someMessage = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
        .join(someAppId, someCluster, defaultNamespace);
    String someWatchKey = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
        .join(appIdWithIncorrectCase, someCluster, defaultNamespace);

    Multimap<String, String> watchKeysMap =
        assembleMultiMap(defaultNamespace, Lists.newArrayList(someWatchKey));

    String notificationAsString =
        transformApolloConfigNotificationsToString(defaultNamespace.toUpperCase(), someNotificationId);

    when(namespaceUtil.filterNamespaceName(namespaceWithIncorrectCase)).thenReturn(namespaceWithIncorrectCase);
    when(namespaceUtil.normalizeNamespace(appIdWithIncorrectCase, namespaceWithIncorrectCase)).thenReturn(defaultNamespace);
    when(watchKeysUtil
        .assembleAllWatchKeys(appIdWithIncorrectCase, someCluster, Sets.newHashSet(defaultNamespace),
            someDataCenter)).thenReturn(watchKeysMap);

    DeferredResult<ResponseEntity<List<ApolloConfigNotification>>>
        deferredResult = controller
        .pollNotification(appIdWithIncorrectCase, someCluster, notificationAsString, someDataCenter,
            someClientIp);

    long someId = 1;
    ReleaseMessage someReleaseMessage = new ReleaseMessage(someMessage);
    someReleaseMessage.setId(someId);

    controller.handleMessage(someReleaseMessage, Topics.APOLLO_RELEASE_TOPIC);

    assertTrue(deferredResult.hasResult());

    ResponseEntity<List<ApolloConfigNotification>> response =
        (ResponseEntity<List<ApolloConfigNotification>>) deferredResult.getResult();

    assertEquals(1, response.getBody().size());
    ApolloConfigNotification notification = response.getBody().get(0);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(namespaceWithIncorrectCase, notification.getNamespaceName());
    assertEquals(someId, notification.getNotificationId());

    ApolloNotificationMessages notificationMessages = notification.getMessages();
    assertEquals(1, notificationMessages.getDetails().size());
    assertEquals(someId, notificationMessages.get(someMessage).longValue());

  }

  private String transformApolloConfigNotificationsToString(
      String namespace, long notificationId) {
    List<ApolloConfigNotification> notifications =
        Lists.newArrayList(assembleApolloConfigNotification(namespace, notificationId));
    return gson.toJson(notifications);
  }

  private String transformApolloConfigNotificationsToString(String namespace, long notificationId,
                                                            String anotherNamespace,
                                                            long anotherNotificationId) {
    List<ApolloConfigNotification> notifications =
        Lists.newArrayList(assembleApolloConfigNotification(namespace, notificationId),
            assembleApolloConfigNotification(anotherNamespace, anotherNotificationId));
    return gson.toJson(notifications);
  }

  private String transformApolloConfigNotificationsToString(String namespace, long notificationId,
                                                            String anotherNamespace,
                                                            long anotherNotificationId,
                                                            String yetAnotherNamespace,
                                                            long yetAnotherNotificationId) {
    List<ApolloConfigNotification> notifications =
        Lists.newArrayList(assembleApolloConfigNotification(namespace, notificationId),
            assembleApolloConfigNotification(anotherNamespace, anotherNotificationId),
            assembleApolloConfigNotification(yetAnotherNamespace, yetAnotherNotificationId));
    return gson.toJson(notifications);
  }

  private ApolloConfigNotification assembleApolloConfigNotification(String namespace,
                                                                    long notificationId) {
    ApolloConfigNotification notification = new ApolloConfigNotification(namespace, notificationId);
    return notification;
  }

  private Multimap<String, String> assembleMultiMap(String key, Iterable<String> values) {
    Multimap<String, String> multimap = HashMultimap.create();
    multimap.putAll(key, values);
    return multimap;
  }

  private void assertWatchKeys(Multimap<String, String> watchKeysMap, DeferredResult deferredResult) {
    for (String watchKey : watchKeysMap.values()) {
      Collection<DeferredResultWrapper> deferredResultWrappers = deferredResults.get(watchKey);
      boolean found = false;
      for (DeferredResultWrapper wrapper: deferredResultWrappers) {
        if (Objects.equals(wrapper.getResult(), deferredResult)) {
          found = true;
        }
      }
      assertTrue(found);
    }
  }
}
