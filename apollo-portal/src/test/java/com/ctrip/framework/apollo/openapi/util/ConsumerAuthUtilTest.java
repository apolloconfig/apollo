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
package com.ctrip.framework.apollo.openapi.util;

import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ConsumerAuthUtilTest {
  private ConsumerAuthUtil consumerAuthUtil;
  @Mock
  private ConsumerService consumerService;
  @Mock
  private HttpServletRequest request;

  @Before
  public void setUp() throws Exception {
    consumerAuthUtil = new ConsumerAuthUtil(consumerService);
  }

  @Test
  public void testGetConsumerId() throws Exception {
    String someToken = "someToken";
    Long someConsumerId = 1L;

    when(consumerService.getConsumerIdByToken(someToken)).thenReturn(someConsumerId);

    assertEquals(someConsumerId, consumerAuthUtil.getConsumerId(someToken));
    verify(consumerService, times(1)).getConsumerIdByToken(someToken);
  }

  @Test
  public void testStoreConsumerId() throws Exception {
    long someConsumerId = 1L;

    consumerAuthUtil.storeConsumerId(request, someConsumerId);

    verify(request, times(1)).setAttribute(ConsumerAuthUtil.CONSUMER_ID, someConsumerId);
  }

  @Test
  public void testRetrieveConsumerId() throws Exception {
    long someConsumerId = 1;

    when(request.getAttribute(ConsumerAuthUtil.CONSUMER_ID)).thenReturn(someConsumerId);

    assertEquals(someConsumerId, consumerAuthUtil.retrieveConsumerId(request));
    verify(request, times(1)).getAttribute(ConsumerAuthUtil.CONSUMER_ID);
  }

  @Test(expected = IllegalStateException.class)
  public void testRetrieveConsumerIdWithConsumerIdNotSet() throws Exception {
    consumerAuthUtil.retrieveConsumerId(request);
  }

  @Test(expected = IllegalStateException.class)
  public void testRetrieveConsumerIdWithConsumerIdInvalid() throws Exception {
    String someInvalidConsumerId = "abc";

    when(request.getAttribute(ConsumerAuthUtil.CONSUMER_ID)).thenReturn(someInvalidConsumerId);
    consumerAuthUtil.retrieveConsumerId(request);
  }

}
