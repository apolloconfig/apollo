package com.ctrip.framework.apollo.audit.component;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTable;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogDataInfluenceProducer;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.util.ApolloAuditUtil;
import java.lang.reflect.Field;
import java.util.List;

public class ApolloAuditLogDataInfluenceProducerImpl implements
    ApolloAuditLogDataInfluenceProducer {

  private final ApolloAuditLogService logService;
  private final ApolloAuditLogDataInfluenceService dataInfluenceService;
  private final ApolloAuditTracer tracer;

  public ApolloAuditLogDataInfluenceProducerImpl(ApolloAuditLogService logService,
      ApolloAuditLogDataInfluenceService dataInfluenceService, ApolloAuditTracer tracer) {
    this.logService = logService;
    this.dataInfluenceService = dataInfluenceService;
    this.tracer = tracer;
  }

  @Override
  public void appendUpdateDataInfluences(Object o, Object n) {
    long id = ApolloAuditUtil.getIdByReflect(o);
    String entityName = o.getClass().getAnnotation(ApolloAuditLogDataInfluenceTable.class)
        .tableName();
    List<Field> oFields = ApolloAuditUtil.getAnnotatedField(
        ApolloAuditLogDataInfluenceTableField.class, o);
    List<Field> nFields = ApolloAuditUtil.getAnnotatedField(
        ApolloAuditLogDataInfluenceTableField.class, n);
    for (Field oField : oFields) {
      for (Field nField : nFields) {
        if (nField.equals(oField)) {
          oField.setAccessible(true);
          nField.setAccessible(true);
          try {
            appendSingleDataInfluence(id, entityName, nField.getName(), oField.get(o).toString(),
                nField.get(n).toString());
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  @Override
  public void appendCreateDataInfluences(Object n) {
    long id = ApolloAuditUtil.getIdByReflect(n);
    String entityName = n.getClass().getAnnotation(ApolloAuditLogDataInfluenceTable.class)
        .tableName();
    List<Field> nFields = ApolloAuditUtil.getAnnotatedField(
        ApolloAuditLogDataInfluenceTableField.class, n);
    for (Field nField : nFields) {
      nField.setAccessible(true);
      try {
        appendSingleDataInfluence(id, entityName, nField.getName(), null, nField.get(n).toString());
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public void appendDeleteDataInfluences(Object o) {
    long id = ApolloAuditUtil.getIdByReflect(o);
    String entityName = o.getClass().getAnnotation(ApolloAuditLogDataInfluenceTable.class)
        .tableName();
    appendDeleteDataInfluence(id, entityName);
  }

  @Override
  public void appendDeleteDataInfluence(long deleteId, String deleteEntityName) {
    appendSingleDataInfluence(deleteId, deleteEntityName, null, null, null);
  }

  @Override
  public void appendSingleDataInfluence(long entityId, String entityName, String fieldName,
      String fieldOldValue, String fieldNewValue) {
    if(tracer.scopeManager().activeSpanContext() == null) {
      return;
    }
    String spanId = tracer.scopeManager().activeSpanContext().getSpanId();
    ApolloAuditLogDataInfluence influence = ApolloAuditLogDataInfluence.builder().spanId(spanId)
        .entityName(entityName).entityId(entityId).fieldName(fieldName).oldVal(fieldOldValue)
        .newVal(fieldNewValue).build();

    dataInfluenceService.save(influence);
  }
}
