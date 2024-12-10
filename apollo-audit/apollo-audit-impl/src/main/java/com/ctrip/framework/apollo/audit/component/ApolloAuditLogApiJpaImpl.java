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
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.util.ApolloAuditUtil;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

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
    return appendAuditLog(type, name, "no description");
  }

  @Override
  public AutoCloseable appendAuditLog(OpType type, String name, String description) {
    ApolloAuditTracer tracer = traceContext.tracer();
    if (Objects.isNull(tracer)) {
      return () -> {};
    }
    ApolloAuditScope scope = tracer.startActiveSpan(type, name, description);
    logService.logSpan(scope.activeSpan());
    return scope;
  }

  @Override
  public void appendDataInfluence(String entityName, String entityId, String fieldName,
      String fieldCurrentValue) {
    // might be
    if (traceContext.tracer() == null) {
      return;
    }
    if (traceContext.tracer().getActiveSpan() == null) {
      return;
    }
    String spanId = traceContext.tracer().getActiveSpan().spanId();
    OpType type = traceContext.tracer().getActiveSpan().getOpType();
    ApolloAuditLogDataInfluence.Builder builder = ApolloAuditLogDataInfluence.builder().spanId(spanId)
        .entityName(entityName).entityId(entityId).fieldName(fieldName);
    if (type == null) {
      return;
    }
    switch (type) {
      case CREATE:
      case UPDATE:
        builder.newVal(fieldCurrentValue);
        break;
      case DELETE:
        builder.oldVal(fieldCurrentValue);
    }
    dataInfluenceService.save(builder.build());
  }

  @Override
  public void appendDataInfluences(List<Object> entities, Class<?> beanDefinition) {
    String tableName = ApolloAuditUtil.getApolloAuditLogTableName(beanDefinition);
    if (Objects.isNull(tableName) || Objects.equals(tableName, "")) {
      return;
    }
    List<Field> dataInfluenceFields = ApolloAuditUtil.getAnnotatedFields(
        ApolloAuditLogDataInfluenceTableField.class, beanDefinition);
    Field idField = ApolloAuditUtil.getPersistenceIdFieldByAnnotation(beanDefinition);
    entities.forEach(e -> {
      try {
        idField.setAccessible(true);
        String tableId = idField.get(e).toString();
        for (Field f : dataInfluenceFields) {
          f.setAccessible(true);
          String val = String.valueOf(f.get(e));
          String fieldName = f.getAnnotation(ApolloAuditLogDataInfluenceTableField.class).fieldName();
          appendDataInfluence(tableName, tableId, fieldName, val);
        }
      } catch (IllegalAccessException ex) {
        throw new IllegalArgumentException("failed append data influence, "
            + "might due to wrong beanDefinition for entity audited", ex);
      }
    });
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
  public List<ApolloAuditLogDataInfluenceDTO> queryDataInfluencesByField(String entityName,
      String entityId, String fieldName, int page, int size) {
    return ApolloAuditUtil.dataInfluenceListToDTOList(dataInfluenceService.findByEntityNameAndEntityIdAndFieldName(entityName, entityId,
        fieldName, page, size));
  }

  @Override
  public List<ApolloAuditLogDTO> searchLogByNameOrTypeOrOperator(String query, int page, int size) {
    return ApolloAuditUtil.logListToDTOList(logService.searchLogByNameOrTypeOrOperator(query, page, size));
  }
}
