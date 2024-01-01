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
package com.ctrip.framework.apollo.audit.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ctrip.framework.apollo.audit.constants.ApolloAuditConstants;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditOperatorSupplier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@SpringBootTest
@ContextConfiguration(classes = ApolloAuditTraceContext.class)
public class ApolloAuditTraceContextTest {

  @SpyBean
  ApolloAuditTraceContext traceContext;

  @MockBean
  ApolloAuditOperatorSupplier supplier;

  @BeforeEach
  public void beforeEach() {
    // will be null of each unit-test begin
    RequestContextHolder.resetRequestAttributes();
    Mockito.reset(
        traceContext
    );
  }

  @Test
  public void testGetTracerNotInRequestThread() {
    ApolloAuditTracer get = traceContext.tracer();
    assertNull(get);
  }

  @Test
  public void testGetTracerCaseNoTracerExistsInRequestThreads() {
    RequestAttributes mockRequestAttributes = Mockito.mock(RequestAttributes.class);
    RequestContextHolder.setRequestAttributes(mockRequestAttributes);

    ApolloAuditTracer get = traceContext.tracer();
    assertNotNull(get);
    Mockito.verify(traceContext, Mockito.times(1))
        .setTracer(Mockito.any(ApolloAuditTracer.class));
  }

  @Test
  public void testGetTracerInRequestThreads() {
    ApolloAuditTracer mockTracer = new ApolloAuditTracer(Mockito.mock(ApolloAuditScopeManager.class), supplier);
    RequestAttributes mockRequestAttributes = Mockito.mock(RequestAttributes.class);
    RequestContextHolder.setRequestAttributes(mockRequestAttributes);
    Mockito.when(mockRequestAttributes.getAttribute(Mockito.eq(ApolloAuditConstants.TRACER), Mockito.eq(RequestAttributes.SCOPE_REQUEST)))
            .thenReturn(mockTracer);
    ApolloAuditTracer get = traceContext.tracer();
    assertNotNull(get);
    Mockito.verify(traceContext, Mockito.times(0))
        .setTracer(Mockito.any(ApolloAuditTracer.class));
  }

  @Test
  public void testGetTracerInAnotherThreadButSameRequest() {
    ApolloAuditTracer mockTracer = Mockito.mock(ApolloAuditTracer.class);
    {
      Mockito.when(traceContext.tracer()).thenReturn(mockTracer);
    }
    CountDownLatch latch = new CountDownLatch(1);
    Executors.newSingleThreadExecutor().submit(() -> {
      ApolloAuditTracer tracer = traceContext.tracer();

      assertEquals(mockTracer, tracer);

      latch.countDown();
    });
  }

  @Test
  public void testGetTracerInAnotherRequest() {
    ApolloAuditTracer mockTracer = Mockito.mock(ApolloAuditTracer.class);
    {
      Mockito.when(traceContext.tracer()).thenReturn(mockTracer);
    }
    CountDownLatch latch = new CountDownLatch(1);
    Executors.newSingleThreadExecutor().submit(() -> {
      RequestContextHolder.resetRequestAttributes();
      ApolloAuditTracer tracer = traceContext.tracer();

      assertNotEquals(mockTracer, tracer);

      latch.countDown();
    });
  }

}
