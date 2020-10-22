package com.ctrip.framework.apollo.biz.message;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import com.ctrip.framework.apollo.biz.AbstractUnitTest;
import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.repository.ReleaseMessageRepository;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
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
    releaseMessageScanner = new ReleaseMessageScanner();
    ReflectionTestUtils
        .setField(releaseMessageScanner, "releaseMessageRepository", releaseMessageRepository);
    ReflectionTestUtils.setField(releaseMessageScanner, "bizConfig", bizConfig);
    databaseScanInterval = 100; //100 ms
    when(bizConfig.releaseMessageScanIntervalInMilli()).thenReturn(databaseScanInterval);
    releaseMessageScanner.afterPropertiesSet();
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

  private ReleaseMessage assembleReleaseMessage(long id, String message) {
    ReleaseMessage releaseMessage = new ReleaseMessage();
    releaseMessage.setId(id);
    releaseMessage.setMessage(message);
    return releaseMessage;
  }
}
