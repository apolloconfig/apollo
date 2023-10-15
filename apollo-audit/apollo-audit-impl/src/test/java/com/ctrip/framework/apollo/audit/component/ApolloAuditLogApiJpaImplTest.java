package com.ctrip.framework.apollo.audit.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ctrip.framework.apollo.audit.MockBeanFactory;
import com.ctrip.framework.apollo.audit.MockDataInfluenceEntity;
import com.ctrip.framework.apollo.audit.MockNotDataInfluenceEntity;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScope;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScopeManager;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTraceContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.exception.ApolloAuditEntityBeanDefinitionException;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.util.ApolloAuditUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = ApolloAuditLogApiJpaImpl.class)
public class ApolloAuditLogApiJpaImplTest {

  final OpType opType = OpType.CREATE;
  final String opName = "test.create";
  final String traceId = "test-trace-id";
  final String spanId = "test-span-id";

  final String entityId = "1";
  final String entityName = "App";
  final String fieldName = "name";
  final String fieldOldValue = null;
  final String fieldNewValue = "xxx";

  final String tableName = "MockTableName";
  final Class<?> beanDefinition = MockDataInfluenceEntity.class;
  final int entityNum = 3;

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
    Mockito.reset(
        traceContext,
        tracer
    );
    Mockito.when(traceContext.tracer())
        .thenReturn(tracer);
  }

  @Test
  public void testAppendAuditLog() {
    {
      ApolloAuditSpan activeSpan = new ApolloAuditSpan();
      activeSpan.setOpType(opType);
      activeSpan.setOpName(opName);
      activeSpan.setContext(new ApolloAuditSpanContext(traceId, spanId));
      ApolloAuditScopeManager manager = new ApolloAuditScopeManager();
      ApolloAuditScope scope = new ApolloAuditScope(activeSpan, manager);

      Mockito.when(tracer.startActiveSpan(Mockito.eq(opType), Mockito.eq(opName), Mockito.eq(null)))
          .thenReturn(scope);
    }
    ApolloAuditScope scope = (ApolloAuditScope) api.appendAuditLog(opType, opName);

    Mockito.verify(traceContext, Mockito.times(1))
        .tracer();
    Mockito.verify(tracer, Mockito.times(1))
        .startActiveSpan(Mockito.eq(opType), Mockito.eq(opName), Mockito.eq(null));

    assertEquals(opType, scope.activeSpan().getOpType());
    assertEquals(opName, scope.activeSpan().getOpName());
    assertEquals(traceId, scope.activeSpan().traceId());
    assertEquals(spanId, scope.activeSpan().spanId());
  }

  @Test
  public void testAppendSingleDataInfluence() {
    {
      ApolloAuditSpan span = new ApolloAuditSpan();
      ApolloAuditSpanContext context = new ApolloAuditSpanContext(traceId, spanId);
      span.setContext(context);
      Mockito.when(tracer.getActiveSpan())
          .thenReturn(span);
    }

    api.appendSingleDataInfluence(entityId, entityName, fieldName, fieldOldValue, fieldNewValue);

    Mockito.verify(traceContext, Mockito.times(3))
        .tracer();
    Mockito.verify(dataInfluenceService, Mockito.times(1))
        .save(influenceCaptor.capture());

    ApolloAuditLogDataInfluence capturedInfluence = influenceCaptor.getValue();
    assertEquals(entityId, capturedInfluence.getInfluenceEntityId());
    assertEquals(entityName, capturedInfluence.getInfluenceEntityName());
    assertEquals(fieldName, capturedInfluence.getFieldName());
    assertEquals(fieldOldValue, capturedInfluence.getFieldOldValue());
    assertEquals(fieldNewValue, capturedInfluence.getFieldNewValue());
    assertEquals(spanId, capturedInfluence.getSpanId());
  }

  @Test
  public void testAppendSingleDataInfluenceCaseTracerIsNull() {
    Mockito.when(traceContext.tracer()).thenReturn(null);
    api.appendSingleDataInfluence(entityId, entityName, fieldName, fieldOldValue, fieldNewValue);
    Mockito.verify(traceContext, Mockito.times(1))
        .tracer();
  }

  @Test
  public void testAppendSingleDataInfluenceCaseActiveSpanIsNull() {
    api.appendSingleDataInfluence(entityId, entityName, fieldName, fieldOldValue, fieldNewValue);
    Mockito.verify(traceContext, Mockito.times(2))
        .tracer();
    Mockito.verify(tracer, Mockito.times(1))
        .getActiveSpan();
  }

  @Test
  public void testAppendDataInfluencesCaseIncompleteConditions() {
    List<Object> entities = new ArrayList<>();

    assertThrows(ApolloAuditEntityBeanDefinitionException.class,
        () -> api.appendDataInfluences(entities, MockNotDataInfluenceEntity.class)
    );
  }

  @Test
  public void testAppendCreateOrUpdateDataInfluences() {
    final String attr = "create-or-update";

    List<Object> entities = MockBeanFactory.mockDataInfluenceEntityListByLength(3);
    entities.forEach(e -> ((MockDataInfluenceEntity) e).setMarkedAttribute(attr));

    api.appendDataInfluences(entities, beanDefinition);

    Mockito.verify(api, Mockito.times(entityNum))
        .appendCreateOrUpdateDataInfluences(
            Mockito.argThat(new EntityNotDeletedMatcher()),
            Mockito.eq(tableName),
            Mockito.argThat(new DataInfluenceFieldsMatcher()),
            Mockito.argThat(new IdFieldMatcher())
        );
    Mockito.verify(api, Mockito.times(entityNum))
        .appendSingleDataInfluence(
            Mockito.anyString(),
            Mockito.eq(tableName),
            Mockito.eq("markedAttribute"),
            Mockito.eq(null),
            Mockito.eq(attr)
        );
  }

  @Test
  public void testAppendDeleteDataInfluences() {
    List<Object> entities = MockBeanFactory.mockDataInfluenceEntityListByLength(3);
    entities.forEach(e -> ((MockDataInfluenceEntity) e).setDeleted(true));

    api.appendDataInfluences(entities, beanDefinition);

    Mockito.verify(api, Mockito.times(entityNum))
        .appendDeleteDataInfluences(
            Mockito.argThat(new EntityDeletedMatcher()),
            Mockito.eq(tableName),
            Mockito.argThat(new DataInfluenceFieldsMatcher()),
            Mockito.argThat(new IdFieldMatcher())
        );
    Mockito.verify(api, Mockito.times(entityNum))
        .appendSingleDataInfluence(
            Mockito.anyString(),
            Mockito.eq(tableName),
            Mockito.eq("markedAttribute"),
            Mockito.eq(null),
            Mockito.eq(null)
        );
  }

  private class EntityNotDeletedMatcher implements ArgumentMatcher<Object> {
    @Override
    public boolean matches(Object e) {
      return !((MockDataInfluenceEntity) e).isDeleted();
    }
  }

  private class EntityDeletedMatcher implements ArgumentMatcher<Object> {
    @Override
    public boolean matches(Object e) {
      return ((MockDataInfluenceEntity) e).isDeleted();
    }
  }

  private class DataInfluenceFieldsMatcher implements ArgumentMatcher<List<Field>> {
    @Override
    public boolean matches(List<Field> fields) {

      return fields.size() == 1 && fields.get(0).getName().equals("markedAttribute");
    }
  }
  private class IdFieldMatcher implements ArgumentMatcher<Field> {
    @Override
    public boolean matches(Field field) {
      return field.getName().equals("id");
    }
  }

}
