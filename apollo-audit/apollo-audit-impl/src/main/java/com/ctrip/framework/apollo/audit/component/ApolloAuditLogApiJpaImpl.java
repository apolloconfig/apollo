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
package com.ctrip.framework.apollo.audit.component;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableField;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScope;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTraceContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDataInfluenceDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDetailsDTO;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.exception.ApolloAuditEntityBeanDefinitionException;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.util.ApolloAuditUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApolloAuditLogApiJpaImpl implements ApolloAuditLogApi {

  private final ApolloAuditLogService logService;
  private final ApolloAuditLogDataInfluenceService dataInfluenceService;
  private final ApolloAuditTraceContext traceContext;

  public ApolloAuditLogApiJpaImpl(ApolloAuditLogService logService,
      ApolloAuditLogDataInfluenceService dataInfluenceService, ApolloAuditTraceContext traceContext) {
    this.logService = logService;
    this.dataInfluenceService = dataInfluenceService;
    this.traceContext = traceContext;
  }

  @Override
  public AutoCloseable appendAuditLog(OpType type, String name) {
    return appendAuditLog(type, name, null);
  }

  @Override
  public AutoCloseable appendAuditLog(OpType type, String name, String description) {
    ApolloAuditTracer tracer = traceContext.tracer();
    ApolloAuditScope scope = tracer.startActiveSpan(type, name, description);
    logService.logSpan(scope.activeSpan());
    return scope;
  }

  @Override
  public void appendSingleDataInfluence(String entityId, String entityName, String fieldName,
      String fieldOldValue, String fieldNewValue) {
    // might be
    if (traceContext.tracer() == null) {
      return;
    }
    if (traceContext.tracer().getActiveSpan() == null) {
      return;
    }
    String spanId = traceContext.tracer().getActiveSpan().spanId();
    ApolloAuditLogDataInfluence influence = ApolloAuditLogDataInfluence.builder().spanId(spanId)
        .entityName(entityName).entityId(entityId).fieldName(fieldName).oldVal(fieldOldValue)
        .newVal(fieldNewValue).build();
    dataInfluenceService.save(influence);
  }

  @Override
  public void appendDataInfluences(List<Object> entities, Class<?> beanDefinition) throws ApolloAuditEntityBeanDefinitionException {
    String tableName = ApolloAuditUtil.getApolloAuditLogTableName(beanDefinition);
    if (Objects.isNull(tableName) || tableName.equals("")) {
      throw new ApolloAuditEntityBeanDefinitionException("entity not being managed by audit annotations",
          beanDefinition.getName());
    }
    List<Field> dataInfluenceFields = ApolloAuditUtil.getAnnotatedFields(
        ApolloAuditLogDataInfluenceTableField.class, beanDefinition);
    Field idField = ApolloAuditUtil.getPersistenceIdFieldByAnnotation(beanDefinition);
    entities.forEach(e -> {
      if (ApolloAuditUtil.isLogicDeleted(e)) {
        appendDeleteDataInfluences(e, tableName, dataInfluenceFields, idField);
      } else {
        appendCreateOrUpdateDataInfluences(e, tableName, dataInfluenceFields, idField);
      }
    });

  }

  public void appendCreateOrUpdateDataInfluences(Object e, String tableName,
      List<Field> dataInfluenceFields, Field idField) {
    idField.setAccessible(true);
    String tableId;
    try {
      tableId = idField.get(e).toString();
      for (Field f : dataInfluenceFields) {
        f.setAccessible(true);
        String val = f.get(e) != null ? String.valueOf(f.get(e)) : null;
        appendSingleDataInfluence(tableId, tableName, f.getName(), null, val);
      }
    } catch (IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void appendDeleteDataInfluences(Object e, String tableName,
      List<Field> dataInfluenceFields, Field idField) {
    idField.setAccessible(true);
    String tableId;
    try {
      tableId = idField.get(e).toString();
    } catch (IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
    dataInfluenceFields.forEach(
        field -> appendSingleDataInfluence(tableId, tableName, field.getName(), null, null));
  }

  @Override
  public List<ApolloAuditLogDTO> queryLogs(int page, int size) {
    return ApolloAuditUtil.logListToDTOList(logService.findAll(page, size));
  }

  @Override
  public List<ApolloAuditLogDTO> queryLogsByOpName(String opName, Date startDate, Date endDate,
      int page, int size) {
    if (startDate == null && endDate == null) {
      return ApolloAuditUtil.logListToDTOList(logService.findByOpName(opName, page, size));
    }
    return ApolloAuditUtil.logListToDTOList(
        logService.findByOpNameAndTime(opName, startDate, endDate, page, size));
  }

  @Override
  public List<ApolloAuditLogDetailsDTO> queryTraceDetails(String traceId) {
    List<ApolloAuditLogDetailsDTO> detailsDTOList = new ArrayList<>();
    logService.findByTraceId(traceId).forEach(log -> {
      detailsDTOList.add(new ApolloAuditLogDetailsDTO(ApolloAuditUtil.logToDTO(log),
          ApolloAuditUtil.dataInfluenceListToDTOList(
              dataInfluenceService.findBySpanId(log.getSpanId()))));
    });
    return detailsDTOList;
  }

  @Override
  public List<ApolloAuditLogDataInfluenceDTO> queryDataInfluencesByEntity(String entityName,
      String entityId, int page, int size) {
    return ApolloAuditUtil.dataInfluenceListToDTOList(
        dataInfluenceService.findByEntityNameAndEntityId(entityName, entityId, page, size));
  }

  @Override
  public List<ApolloAuditLogDataInfluenceDTO> queryDataInfluencesByField(String entityName,
      String entityId, String fieldName, int page, int size) {
    return ApolloAuditUtil.dataInfluenceListToDTOList(dataInfluenceService.findByEntityNameAndEntityIdAndFieldName(entityName, entityId,
        fieldName, page, size));
  }
}
