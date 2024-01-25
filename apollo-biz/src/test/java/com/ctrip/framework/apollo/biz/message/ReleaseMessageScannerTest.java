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
package com.ctrip.framework.apollo.biz.message;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.SettableFuture;

import com.ctrip.framework.apollo.biz.AbstractUnitTest;
import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.repository.ReleaseMessageRepository;

import java.util.ArrayList;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ReleaseMessageScannerTest extends AbstractUnitTest {
  private ReleaseMessageScanner releaseMessageScanner;
  @Mock
  private ReleaseMessageRepository releaseMessageRepository;
  @Mock
  private BizConfig bizConfig;
  private int databaseScanInterval;

  @Before
  public void setUp() throws Exception {
    releaseMessageScanner = new ReleaseMessageScanner(bizConfig, releaseMessageRepository);
    databaseScanInterval = 100; //100 ms
    when(bizConfig.releaseMessageScanIntervalInMilli()).thenReturn(databaseScanInterval);
    releaseMessageScanner.afterPropertiesSet();

    Awaitility.reset();
    Awaitility.setDefaultTimeout(databaseScanInterval * 5, TimeUnit.MILLISECONDS);
    Awaitility.setDefaultPollInterval(databaseScanInterval, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testScanMessageAndNotifyMessageListener() throws Exception {
    SettableFuture<ReleaseMessage> someListenerFuture = SettableFuture.create();
    ReleaseMessageListener someListener = (message, channel) -> someListenerFuture.set(message);
    releaseMessageScanner.addMessageListener(someListener);

    String someMessage = "someMessage";
    long someId = 100;
    ReleaseMessage someReleaseMessage = assembleReleaseMessage(someId, someMessage);

    when(releaseMessageRepository.findFirst500ByIdGreaterThanOrderByIdAsc(0L)).thenReturn(
        Lists.newArrayList(someReleaseMessage));

    ReleaseMessage someListenerMessage =
        someListenerFuture.get(5000, TimeUnit.MILLISECONDS);

    assertEquals(someMessage, someListenerMessage.getMessage());
    assertEquals(someId, someListenerMessage.getId());

    SettableFuture<ReleaseMessage> anotherListenerFuture = SettableFuture.create();
    ReleaseMessageListener anotherListener = (message, channel) -> anotherListenerFuture.set(message);
    releaseMessageScanner.addMessageListener(anotherListener);

    String anotherMessage = "anotherMessage";
    long anotherId = someId + 1;
    ReleaseMessage anotherReleaseMessage = assembleReleaseMessage(anotherId, anotherMessage);

    when(releaseMessageRepository.findFirst500ByIdGreaterThanOrderByIdAsc(someId)).thenReturn(
        Lists.newArrayList(anotherReleaseMessage));

    ReleaseMessage anotherListenerMessage =
        anotherListenerFuture.get(5000, TimeUnit.MILLISECONDS);

    assertEquals(anotherMessage, anotherListenerMessage.getMessage());
    assertEquals(anotherId, anotherListenerMessage.getId());
  }

  @Test
  public void testScanMessageWithGapAndNotifyMessageListener() throws Exception {
    String someMessage = "someMessage";
    long someId = 1;
    ReleaseMessage someReleaseMessage = assembleReleaseMessage(someId, someMessage);

    String someMissingMessage = "someMissingMessage";
    long someMissingId = 2;
    ReleaseMessage someMissingReleaseMessage = assembleReleaseMessage(someMissingId, someMissingMessage);

    String anotherMessage = "anotherMessage";
    long anotherId = 3;
    ReleaseMessage anotherReleaseMessage = assembleReleaseMessage(anotherId, anotherMessage);

    String anotherMissingMessage = "anotherMissingMessage";
    long anotherMissingId = 4;
    ReleaseMessage anotherMissingReleaseMessage = assembleReleaseMessage(anotherMissingId, anotherMissingMessage);

    long someRolledBackId = 5;

    String yetAnotherMessage = "yetAnotherMessage";
    long yetAnotherId = 6;
    ReleaseMessage yetAnotherReleaseMessage = assembleReleaseMessage(yetAnotherId, yetAnotherMessage);

    ArrayList<ReleaseMessage> receivedMessage = Lists.newArrayList();
    SettableFuture<ReleaseMessage> someListenerFuture = SettableFuture.create();
    ReleaseMessageListener someListener = (message, channel) -> receivedMessage.add(message);
    releaseMessageScanner.addMessageListener(someListener);

    when(releaseMessageRepository.findFirst500ByIdGreaterThanOrderByIdAsc(0L)).thenReturn(
        Lists.newArrayList(someReleaseMessage));

    await().untilAsserted(() -> {
      assertEquals(1, receivedMessage.size());
      assertSame(someReleaseMessage, receivedMessage.get(0));
    });

    when(releaseMessageRepository.findFirst500ByIdGreaterThanOrderByIdAsc(someId)).thenReturn(
        Lists.newArrayList(anotherReleaseMessage));

    await().untilAsserted(() -> {
      assertEquals(2, receivedMessage.size());
      assertSame(someReleaseMessage, receivedMessage.get(0));
      assertSame(anotherReleaseMessage, receivedMessage.get(1));
    });

    when(releaseMessageRepository.findAllById(Sets.newHashSet(someMissingId)))
        .thenReturn(Lists.newArrayList(someMissingReleaseMessage));

    await().untilAsserted(() -> {
      assertEquals(3, receivedMessage.size());
      assertSame(someReleaseMessage, receivedMessage.get(0));
      assertSame(anotherReleaseMessage, receivedMessage.get(1));
      assertSame(someMissingReleaseMessage, receivedMessage.get(2));
    });

    when(releaseMessageRepository.findFirst500ByIdGreaterThanOrderByIdAsc(anotherId)).thenReturn(
        Lists.newArrayList(yetAnotherReleaseMessage));

    await().untilAsserted(() -> {
      assertEquals(4, receivedMessage.size());
      assertSame(someReleaseMessage, receivedMessage.get(0));
      assertSame(anotherReleaseMessage, receivedMessage.get(1));
      assertSame(someMissingReleaseMessage, receivedMessage.get(2));
      assertSame(yetAnotherReleaseMessage, receivedMessage.get(3));
    });

    when(releaseMessageRepository.findAllById(Sets.newHashSet(anotherMissingId, someRolledBackId)))
        .thenReturn(Lists.newArrayList(anotherMissingReleaseMessage));

    await().untilAsserted(() -> {
      assertEquals(5, receivedMessage.size());
      assertSame(someReleaseMessage, receivedMessage.get(0));
      assertSame(anotherReleaseMessage, receivedMessage.get(1));
      assertSame(someMissingReleaseMessage, receivedMessage.get(2));
      assertSame(yetAnotherReleaseMessage, receivedMessage.get(3));
      assertSame(anotherMissingReleaseMessage, receivedMessage.get(4));
    });
  }

  private ReleaseMessage assembleReleaseMessage(long id, String message) {
    ReleaseMessage releaseMessage = new ReleaseMessage();
    releaseMessage.setId(id);
    releaseMessage.setMessage(message);
    return releaseMessage;
  }
}
