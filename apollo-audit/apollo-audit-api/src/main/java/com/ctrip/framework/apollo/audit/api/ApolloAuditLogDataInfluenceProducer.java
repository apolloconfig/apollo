/*
 * Copyright 2023 Apollo Authors
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
package com.ctrip.framework.apollo.audit.api;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTable;
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.util.ApolloAuditUtil;
import java.lang.reflect.Field;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApolloAuditLogDataInfluenceProducer {

  private static final Logger logger = LoggerFactory.getLogger(
      ApolloAuditLogDataInfluenceProducer.class);

  private final ApolloAuditLogService logService;
  private final ApolloAuditLogDataInfluenceService dataInfluenceService;
  private final ApolloAuditTracer tracer;

  public ApolloAuditLogDataInfluenceProducer(ApolloAuditLogService logService,
      ApolloAuditLogDataInfluenceService dataInfluenceService, ApolloAuditTracer tracer) {
    this.logService = logService;
    this.dataInfluenceService = dataInfluenceService;
    this.tracer = tracer;
  }

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

  public void appendDeleteDataInfluences(Object o) {
    long id = ApolloAuditUtil.getIdByReflect(o);
    String entityName = o.getClass().getAnnotation(ApolloAuditLogDataInfluenceTable.class)
        .tableName();
    appendDeleteDataInfluence(id, entityName);
  }

  public void appendDeleteDataInfluence(long deleteId, String deleteEntityName) {
    appendSingleDataInfluence(deleteId, deleteEntityName, null, null, null);
  }

  public void appendSingleDataInfluence(long entityId, String entityName, String fieldName,
      String fieldOldValue, String fieldNewValue) {
    String spanId = tracer.scopeManager().activeSpanContext().getSpanId();
    ApolloAuditLogDataInfluence influence = ApolloAuditLogDataInfluence.builder().spanId(spanId)
        .entityName(entityName).entityId(entityId).fieldName(fieldName).oldVal(fieldOldValue)
        .newVal(fieldNewValue).build();

    dataInfluenceService.save(influence);
  }
}
