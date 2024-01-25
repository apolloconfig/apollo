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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.constants.ApolloAuditConstants;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditOperatorSupplier;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@SpringBootTest
@ContextConfiguration(classes = ApolloAuditTracer.class)
public class ApolloAuditTracerTest {

  final OpType opType = OpType.CREATE;
  final String opName = "create";
  final String description = "description";

  final String activeTraceId = "10001";
  final String activeSpanId = "100010001";
  final String operator = "luke";

  @SpyBean
  ApolloAuditTracer tracer;

  @MockBean
  ApolloAuditScopeManager manager;
  @MockBean
  ApolloAuditOperatorSupplier supplier;

  @BeforeEach
  public void beforeEach() {
    RequestContextHolder.resetRequestAttributes();
    Mockito.reset(
        tracer,
        manager,
        supplier
    );
  }

  @Test
  public void testInject() {

    HttpRequest mockRequest = Mockito.mock(HttpRequest.class);
    {
      ApolloAuditSpan activeSpan = Mockito.mock(ApolloAuditSpan.class);
      Mockito.when(manager.activeSpan()).thenReturn(activeSpan);
      Mockito.when(mockRequest.getHeaders()).thenReturn(new HttpHeaders());
    }
    HttpRequest injected = tracer.inject(mockRequest);

    HttpHeaders headers = injected.getHeaders();
    assertNotNull(headers.get(ApolloAuditConstants.TRACE_ID));
    assertNotNull(headers.get(ApolloAuditConstants.SPAN_ID));
    assertNotNull(headers.get(ApolloAuditConstants.OPERATOR));
    assertNotNull(headers.get(ApolloAuditConstants.PARENT_ID));
    assertNotNull(headers.get(ApolloAuditConstants.FOLLOWS_FROM_ID));
  }

  @Test
  public void testInjectCaseActiveSpanIsNull() {
    HttpRequest mockRequest = Mockito.mock(HttpRequest.class);
    {
      Mockito.when(manager.activeSpan()).thenReturn(null);
      Mockito.when(mockRequest.getHeaders()).thenReturn(new HttpHeaders());
    }
    HttpRequest injected = tracer.inject(mockRequest);

    HttpHeaders headers = injected.getHeaders();
    assertNull(headers.get(ApolloAuditConstants.TRACE_ID));
    assertNull(headers.get(ApolloAuditConstants.SPAN_ID));
    assertNull(headers.get(ApolloAuditConstants.OPERATOR));
    assertNull(headers.get(ApolloAuditConstants.PARENT_ID));
    assertNull(headers.get(ApolloAuditConstants.FOLLOWS_FROM_ID));
  }

  @Test
  public void testStartSpanCaseActiveSpanExistsAndNoFollowsFrom() {
    {
      // has parent span
      ApolloAuditSpan activeSpan = Mockito.mock(ApolloAuditSpan.class);
      Mockito.when(tracer.getActiveSpan()).thenReturn(activeSpan);
      Mockito.when(activeSpan.traceId()).thenReturn(activeTraceId);
      Mockito.when(activeSpan.spanId()).thenReturn(activeSpanId);
      Mockito.when(activeSpan.operator()).thenReturn(operator);
      // not follows from any span
      ApolloAuditScope mockScope = Mockito.mock(ApolloAuditScope.class);
      Mockito.when(manager.getScope()).thenReturn(mockScope);
      Mockito.when(mockScope.getLastSpanId()).thenReturn(null);
    }

    ApolloAuditSpan build = tracer.startSpan(opType, opName, description);

    assertEquals(opType, build.getOpType());
    assertEquals(opName, build.getOpName());
    assertEquals(description, build.getDescription());
    assertEquals(activeTraceId, build.traceId());
    assertEquals(activeSpanId, build.parentId());
    assertEquals(operator, build.operator());
    assertNotNull(build.spanId());
    assertNull(build.followsFromId());
  }

  @Test
  public void testStartSpanCaseActiveSpanExistsAndHasFollowsFrom() {
    final String followsFromSpanId = "100010000";
    {
      // has parent span
      ApolloAuditSpan activeSpan = Mockito.mock(ApolloAuditSpan.class);
      Mockito.when(tracer.getActiveSpan()).thenReturn(activeSpan);
      Mockito.when(activeSpan.traceId()).thenReturn(activeTraceId);
      Mockito.when(activeSpan.spanId()).thenReturn(activeSpanId);
      Mockito.when(activeSpan.operator()).thenReturn(operator);
      // has follows from span
      ApolloAuditScope mockScope = Mockito.mock(ApolloAuditScope.class);
      Mockito.when(manager.getScope()).thenReturn(mockScope);
      Mockito.when(mockScope.getLastSpanId()).thenReturn(followsFromSpanId);
    }

    ApolloAuditSpan build = tracer.startSpan(opType, opName, description);

    assertEquals(opType, build.getOpType());
    assertEquals(opName, build.getOpName());
    assertEquals(description, build.getDescription());
    assertEquals(activeTraceId, build.traceId());
    assertEquals(activeSpanId, build.parentId());
    assertEquals(operator, build.operator());
    assertNotNull(build.spanId());
    assertEquals(followsFromSpanId, build.followsFromId());
  }

  @Test
  public void testStartSpanCaseNoActiveSpanExists() {
    {
      // no parent span
      Mockito.when(tracer.getActiveSpan()).thenReturn(null);
      // is the origin of a trace, need to get operator
      Mockito.when(supplier.getOperator()).thenReturn(operator);
      // of course no scope at this time
      Mockito.when(manager.getScope()).thenReturn(null);
    }

    ApolloAuditSpan build = tracer.startSpan(opType, opName, description);

    assertEquals(opType, build.getOpType());
    assertEquals(opName, build.getOpName());
    assertEquals(description, build.getDescription());
    assertNotNull(build.traceId());
    assertNotNull(build.spanId());
    assertNull(build.parentId());
    assertEquals(operator, build.operator());
    assertNull(build.followsFromId());
  }

  @Test
  public void testStartActiveSpan() {
    ApolloAuditSpan activeSpan = Mockito.mock(ApolloAuditSpan.class);
    {
      doReturn(activeSpan).when(tracer).startSpan(Mockito.eq(opType), Mockito.eq(opName), Mockito.eq(description));
    }
    tracer.startActiveSpan(opType, opName, description);
    Mockito.verify(tracer, Mockito.times(1))
        .startSpan(Mockito.eq(opType), Mockito.eq(opName), Mockito.eq(description));
    Mockito.verify(manager, times(1))
        .activate(Mockito.eq(activeSpan));
  }

  @Test
  public void testGetActiveSpanFromContext() {
    ApolloAuditSpan activeSpan = Mockito.mock(ApolloAuditSpan.class);
    {
      Mockito.when(manager.activeSpan()).thenReturn(activeSpan);
    }
    ApolloAuditSpan get = tracer.getActiveSpan();
    assertEquals(activeSpan, get);
  }

  @Test
  public void testGetActiveSpanFromHttpRequestCaseNotInRequestThread() {
    {
      // no span would be in context
      Mockito.when(manager.activeSpan()).thenReturn(null);
      // not in request thread
    }
    ApolloAuditSpan get = tracer.getActiveSpan();
    assertNull(get);
  }

  @Test
  public void testGetActiveSpanFromHttpRequestCaseInRequestThread() {
    final String httpParentId = "100010002";
    final String httpFollowsFromId = "100010003";
    {
      // no span would be in context
      Mockito.when(manager.activeSpan()).thenReturn(null);
      // in request thread
      HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
      RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
      Mockito.when(request.getHeader(Mockito.eq(ApolloAuditConstants.TRACE_ID)))
          .thenReturn(activeTraceId);
      Mockito.when(request.getHeader(Mockito.eq(ApolloAuditConstants.SPAN_ID)))
          .thenReturn(activeSpanId);
      Mockito.when(request.getHeader(Mockito.eq(ApolloAuditConstants.OPERATOR)))
          .thenReturn(operator);
      Mockito.when(request.getHeader(Mockito.eq(ApolloAuditConstants.PARENT_ID)))
          .thenReturn(httpParentId);
      Mockito.when(request.getHeader(Mockito.eq(ApolloAuditConstants.FOLLOWS_FROM_ID)))
          .thenReturn(httpFollowsFromId);
    }
    ApolloAuditSpan get = tracer.getActiveSpan();
    assertEquals(activeTraceId, get.traceId());
    assertEquals(activeSpanId, get.spanId());
    assertEquals(operator, get.operator());
    assertEquals(httpParentId, get.parentId());
    assertEquals(httpFollowsFromId, get.followsFromId());

    assertNull(get.getOpType());
    assertNull(get.getOpName());
  }

}
