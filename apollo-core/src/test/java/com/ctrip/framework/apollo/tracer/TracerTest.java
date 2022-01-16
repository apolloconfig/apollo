/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.tracer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.tracer.internals.MockMessageProducerManager;
import com.ctrip.framework.apollo.tracer.internals.NullTransaction;
import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class TracerTest {
  private MessageProducer someProducer;

  @Before
  public void setUp() throws Exception {
    someProducer = mock(MessageProducer.class);
    MockMessageProducerManager.setProducer(someProducer);
  }

  @Test
  public void testLogError() throws Exception {
    String someMessage = "someMessage";
    Throwable someCause = mock(Throwable.class);

    Tracer.logError(someMessage, someCause);

    verify(someProducer, times(1)).logError(someMessage, someCause);
  }

  @Test
  public void testLogErrorWithException() throws Exception {
    String someMessage = "someMessage";
    Throwable someCause = mock(Throwable.class);
    doThrow(RuntimeException.class).when(someProducer).logError(someMessage, someCause);

    Tracer.logError(someMessage, someCause);

    verify(someProducer, times(1)).logError(someMessage, someCause);
  }

  @Test
  public void testLogErrorWithOnlyCause() throws Exception {
    Throwable someCause = mock(Throwable.class);

    Tracer.logError(someCause);

    verify(someProducer, times(1)).logError(someCause);
  }

  @Test
  public void testLogErrorWithOnlyCauseWithException() throws Exception {
    Throwable someCause = mock(Throwable.class);
    doThrow(RuntimeException.class).when(someProducer).logError(someCause);

    Tracer.logError(someCause);

    verify(someProducer, times(1)).logError(someCause);
  }

  @Test
  public void testLogEvent() throws Exception {
    String someType = "someType";
    String someName = "someName";

    Tracer.logEvent(someType, someName);

    verify(someProducer, times(1)).logEvent(someType, someName);
  }

  @Test
  public void testLogEventWithException() throws Exception {
    String someType = "someType";
    String someName = "someName";
    doThrow(RuntimeException.class).when(someProducer).logEvent(someType, someName);

    Tracer.logEvent(someType, someName);

    verify(someProducer, times(1)).logEvent(someType, someName);
  }

  @Test
  public void testLogEventWithStatusAndNameValuePairs() throws Exception {
    String someType = "someType";
    String someName = "someName";
    String someStatus = "someStatus";
    String someNameValuePairs = "someNameValuePairs";

    Tracer.logEvent(someType, someName, someStatus, someNameValuePairs);

    verify(someProducer, times(1)).logEvent(someType, someName, someStatus, someNameValuePairs);
  }

  @Test
  public void testLogEventWithStatusAndNameValuePairsWithException() throws Exception {
    String someType = "someType";
    String someName = "someName";
    String someStatus = "someStatus";
    String someNameValuePairs = "someNameValuePairs";
    doThrow(RuntimeException.class)
        .when(someProducer)
        .logEvent(someType, someName, someStatus, someNameValuePairs);

    Tracer.logEvent(someType, someName, someStatus, someNameValuePairs);

    verify(someProducer, times(1)).logEvent(someType, someName, someStatus, someNameValuePairs);
  }

  @Test
  public void testNewTransaction() throws Exception {
    String someType = "someType";
    String someName = "someName";
    Transaction someTransaction = mock(Transaction.class);

    when(someProducer.newTransaction(someType, someName)).thenReturn(someTransaction);

    Transaction result = Tracer.newTransaction(someType, someName);

    verify(someProducer, times(1)).newTransaction(someType, someName);
    assertEquals(someTransaction, result);
  }

  @Test
  public void testNewTransactionWithException() throws Exception {
    String someType = "someType";
    String someName = "someName";

    when(someProducer.newTransaction(someType, someName)).thenThrow(RuntimeException.class);

    Transaction result = Tracer.newTransaction(someType, someName);

    verify(someProducer, times(1)).newTransaction(someType, someName);
    assertTrue(result instanceof NullTransaction);
  }
}
