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
package com.ctrip.framework.apollo.audit.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ctrip.framework.apollo.audit.MockBeanFactory;
import com.ctrip.framework.apollo.audit.MockDataInfluenceEntity;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScope;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScopeManager;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTraceContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDataInfluenceDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDetailsDTO;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = ApolloAuditLogApiJpaImpl.class)
public class ApolloAuditLogApiJpaImplTest {

  // record api

  final OpType create = OpType.CREATE;
  final OpType delete = OpType.DELETE;
  final String opName = "test.create";
  final String traceId = "test-trace-id";
  final String spanId = "test-span-id";

  final String entityId = "1";
  final String entityName = "App";
  final String fieldName = "name";
  final String fieldCurrentValue = "xxx";

  final int entityNum = 3;

  // query api

  final int page = 0;
  final int size = 10;

  @SpyBean
  ApolloAuditLogApiJpaImpl api;

  @MockBean
  ApolloAuditLogService logService;
  @MockBean
  ApolloAuditLogDataInfluenceService dataInfluenceService;
  @MockBean
  ApolloAuditTraceContext traceContext;
  @MockBean
  ApolloAuditTracer tracer;

  @Captor
  private ArgumentCaptor<ApolloAuditLogDataInfluence> influenceCaptor;

  @BeforeEach
  void beforeEach() {
    Mockito.reset(traceContext, tracer);
    Mockito.when(traceContext.tracer()).thenReturn(tracer);
  }

  @Test
  public void testAppendAuditLog() {
    final String description = "no description";
    {
      ApolloAuditSpan activeSpan = new ApolloAuditSpan();
      activeSpan.setOpType(create);
      activeSpan.setOpName(opName);
      activeSpan.setContext(new ApolloAuditSpanContext(traceId, spanId));
      ApolloAuditScopeManager manager = new ApolloAuditScopeManager();
      ApolloAuditScope scope = new ApolloAuditScope(activeSpan, manager);

      Mockito.when(tracer.startActiveSpan(Mockito.eq(create), Mockito.eq(opName), Mockito.eq(description)))
          .thenReturn(scope);
    }
    ApolloAuditScope scope = (ApolloAuditScope) api.appendAuditLog(create, opName);

    Mockito.verify(traceContext, Mockito.times(1)).tracer();
    Mockito.verify(tracer, Mockito.times(1))
        .startActiveSpan(Mockito.eq(create), Mockito.eq(opName), Mockito.eq(description));

    assertEquals(create, scope.activeSpan().getOpType());
    assertEquals(opName, scope.activeSpan().getOpName());
    assertEquals(traceId, scope.activeSpan().traceId());
    assertEquals(spanId, scope.activeSpan().spanId());
  }

  @Test
  public void testAppendDataInfluenceCaseCreateOrUpdate() {
    {
      ApolloAuditSpan span = Mockito.mock(ApolloAuditSpan.class);
      Mockito.when(tracer.getActiveSpan()).thenReturn(span);
      Mockito.when(span.spanId()).thenReturn(spanId);
      Mockito.when(span.getOpType()).thenReturn(create);
    }

    api.appendDataInfluence(entityName, entityId, fieldName, fieldCurrentValue);

    Mockito.verify(dataInfluenceService, Mockito.times(1)).save(influenceCaptor.capture());

    ApolloAuditLogDataInfluence capturedInfluence = influenceCaptor.getValue();
    assertEquals(entityId, capturedInfluence.getInfluenceEntityId());
    assertEquals(entityName, capturedInfluence.getInfluenceEntityName());
    assertEquals(fieldName, capturedInfluence.getFieldName());
    assertNull(capturedInfluence.getFieldOldValue());
    assertEquals(fieldCurrentValue, capturedInfluence.getFieldNewValue());
    assertEquals(spanId, capturedInfluence.getSpanId());
  }

  @Test
  public void testAppendDataInfluenceCaseDelete() {
    {
      ApolloAuditSpan span = Mockito.mock(ApolloAuditSpan.class);
      Mockito.when(tracer.getActiveSpan()).thenReturn(span);
      Mockito.when(span.spanId()).thenReturn(spanId);
      Mockito.when(span.getOpType()).thenReturn(delete);
    }

    api.appendDataInfluence(entityName, entityId, fieldName, fieldCurrentValue);

    Mockito.verify(dataInfluenceService, Mockito.times(1)).save(influenceCaptor.capture());

    ApolloAuditLogDataInfluence capturedInfluence = influenceCaptor.getValue();
    assertEquals(entityId, capturedInfluence.getInfluenceEntityId());
    assertEquals(entityName, capturedInfluence.getInfluenceEntityName());
    assertEquals(fieldName, capturedInfluence.getFieldName());
    assertEquals(fieldCurrentValue, capturedInfluence.getFieldOldValue());
    assertNull(capturedInfluence.getFieldNewValue());
    assertEquals(spanId, capturedInfluence.getSpanId());
  }

  @Test
  public void testAppendDataInfluenceCaseTracerIsNull() {
    Mockito.when(traceContext.tracer()).thenReturn(null);
    api.appendDataInfluence(entityName, entityId, fieldName, fieldCurrentValue);
    Mockito.verify(traceContext, Mockito.times(1)).tracer();
  }

  @Test
  public void testAppendDataInfluenceCaseActiveSpanIsNull() {
    Mockito.when(tracer.getActiveSpan()).thenReturn(null);
    api.appendDataInfluence(entityName, entityId, fieldName, fieldCurrentValue);
    Mockito.verify(traceContext, Mockito.times(2)).tracer();
    Mockito.verify(tracer, Mockito.times(1)).getActiveSpan();
  }

  @Test
  public void testAppendDataInfluences() {
    List<Object> entities = MockBeanFactory.mockDataInfluenceEntityListByLength(entityNum);
    api.appendDataInfluences(entities, MockDataInfluenceEntity.class);

    Mockito.verify(api, Mockito.times(entityNum))
        .appendDataInfluence(Mockito.eq("MockTableName"), Mockito.any(), Mockito.eq("MarkedAttribute"),
            Mockito.any());
  }

  @Test
  public void testAppendDataInfluencesCaseWrongBeanDefinition() {
    List<Object> entities = new ArrayList<>();
    entities.add(new Object());
    assertThrows(IllegalArgumentException.class, () -> {
      api.appendDataInfluences(entities, MockDataInfluenceEntity.class);
    });

  }

  @Test
  public void testAppendDataInfluencesCaseIncompleteConditions() {
    List<Object> entities = new ArrayList<>(entityNum);

    api.appendDataInfluences(entities, Object.class);

    Mockito.verify(api, Mockito.times(0))
        .appendDataInfluence(Mockito.any(), Mockito.any(),
            Mockito.any(), Mockito.any());
  }



  @Test
  public void testQueryLogs() {
    {
      List<ApolloAuditLog> logList = MockBeanFactory.mockAuditLogListByLength(size);
      Mockito.when(logService.findAll(Mockito.eq(page), Mockito.eq(size)))
          .thenReturn(logList);
    }

    List<ApolloAuditLogDTO> dtoList = api.queryLogs(page, size);
    Mockito.verify(logService, Mockito.times(1))
        .findAll(Mockito.eq(page), Mockito.eq(size));
    assertEquals(size, dtoList.size());
  }

  @Test
  public void testQueryLogsByOpNameCaseDateIsNull() {
    final String opName = "query-op-name";
    final Date startDate = null;
    final Date endDate = null;
    {
      List<ApolloAuditLog> logList = MockBeanFactory.mockAuditLogListByLength(size);
      Mockito.when(logService.findByOpName(Mockito.eq(opName), Mockito.eq(page), Mockito.eq(size)))
          .thenReturn(logList);
    }

    List<ApolloAuditLogDTO> dtoList = api.queryLogsByOpName(opName, startDate, endDate, page, size);
    Mockito.verify(logService, Mockito.times(1))
        .findByOpName(Mockito.eq(opName), Mockito.eq(page), Mockito.eq(size));
    assertEquals(size, dtoList.size());
  }

  @Test
  public void testQueryLogsByOpName() {
    final String opName = "query-op-name";
    final Date startDate = new Date();
    final Date endDate = new Date();
    {
      List<ApolloAuditLog> logList = MockBeanFactory.mockAuditLogListByLength(size);
      Mockito.when(logService.findByOpNameAndTime(Mockito.eq(opName),
              Mockito.eq(startDate), Mockito.eq(endDate), Mockito.eq(page), Mockito.eq(size)))
          .thenReturn(logList);
    }

    List<ApolloAuditLogDTO> dtoList = api.queryLogsByOpName(opName, startDate, endDate, page, size);
    Mockito.verify(logService, Mockito.times(1))
        .findByOpNameAndTime(Mockito.eq(opName),
            Mockito.eq(startDate), Mockito.eq(endDate), Mockito.eq(page), Mockito.eq(size));
    assertEquals(size, dtoList.size());
  }

  @Test
  public void testQueryTraceDetails() {
    final String traceId = "query-trace-id";
    final int traceDetailsLength = 3;
    final int dataInfluenceOfEachLog = 3;
    {
      List<ApolloAuditLog> logList = MockBeanFactory.mockAuditLogListByLength(traceDetailsLength);
      Mockito.when(logService.findByTraceId(Mockito.eq(traceId)))
          .thenReturn(logList);
      List<ApolloAuditLogDataInfluence> dataInfluenceList =
          MockBeanFactory.mockDataInfluenceListByLength(dataInfluenceOfEachLog);
      Mockito.when(dataInfluenceService.findBySpanId(Mockito.any()))
          .thenReturn(dataInfluenceList);
    }

    List<ApolloAuditLogDetailsDTO> detailsDTOList = api.queryTraceDetails(traceId);

    Mockito.verify(logService, Mockito.times(1))
        .findByTraceId(Mockito.eq(traceId));
    Mockito.verify(dataInfluenceService, Mockito.times(3))
        .findBySpanId(Mockito.any());

    assertEquals(traceDetailsLength, detailsDTOList.size());
    assertEquals(dataInfluenceOfEachLog, detailsDTOList.get(0).getDataInfluenceDTOList().size());
  }

  @Test
  public void testQueryDataInfluencesByField() {
    final String entityName = "App";
    final String entityId = "1";
    final String fieldName = "xxx";
    {
      List<ApolloAuditLogDataInfluence> dataInfluenceList = MockBeanFactory.mockDataInfluenceListByLength(size);
      Mockito.when(dataInfluenceService.findByEntityNameAndEntityIdAndFieldName(Mockito.eq(entityName),
              Mockito.eq(entityId), Mockito.eq(fieldName), Mockito.eq(page), Mockito.eq(size)))
          .thenReturn(dataInfluenceList);
    }

    List<ApolloAuditLogDataInfluenceDTO> dtoList = api.queryDataInfluencesByField(entityName, entityId, fieldName, page, size);
    Mockito.verify(dataInfluenceService, Mockito.times(1))
        .findByEntityNameAndEntityIdAndFieldName(Mockito.eq(entityName),
            Mockito.eq(entityId), Mockito.eq(fieldName), Mockito.eq(page), Mockito.eq(size));
    assertEquals(size, dtoList.size());
  }
}
