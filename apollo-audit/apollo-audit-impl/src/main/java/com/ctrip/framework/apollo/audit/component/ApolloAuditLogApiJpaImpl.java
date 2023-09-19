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
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApolloAuditLogApiJpaImpl implements ApolloAuditLogApi {

  private static final Logger logger = LoggerFactory.getLogger(ApolloAuditLogApiJpaImpl.class);

  private final ApolloAuditLogService logService;
  private final ApolloAuditLogDataInfluenceService dataInfluenceService;
  private final ApolloAuditSpanService spanService;

  public ApolloAuditLogApiJpaImpl(ApolloAuditLogService logService,
      ApolloAuditLogDataInfluenceService dataInfluenceService, ApolloAuditSpanService spanService) {
    this.logService = logService;
    this.dataInfluenceService = dataInfluenceService;
    this.spanService = spanService;
  }

  @Override
  public Map extractSpan() {
    ApolloAuditTracer tracer = spanService.getTracer();
    if(Objects.isNull(tracer)) {
      // in the main thread
      return null;
    }
    return tracer.extract();
  }

  @Override
  public AutoCloseable appendSpan(OpType type, String name) {
    return appendSpan(type, name, null);
  }

  @Override
  public AutoCloseable appendSpan(OpType type, String name, String description) {
    ApolloAuditSpan span = spanService.generateSpan(type, name, description);
    ApolloAuditScope scope = spanService.getTracer().scopeManager().activate(span.getSpanContext());
    logService.logSpan(span);
    return scope;
  }

  @Override
  public void appendSingleDataInfluence(String tableId, String entityName, String fieldName,
      String fieldOldValue, String fieldNewValue) {
    if (spanService.getTracer().scopeManager().activeSpanContext() == null) {
      return;
    }
    String spanId = spanService.getTracer().scopeManager().activeSpanContext().getSpanId();
    ApolloAuditLogDataInfluence influence = ApolloAuditLogDataInfluence.builder().spanId(spanId)
        .entityName(entityName).entityId(tableId).fieldName(fieldName).oldVal(fieldOldValue)
        .newVal(fieldNewValue).build();

    dataInfluenceService.save(influence);
  }

  @Override
  public void appendDataInfluences(List entities, Class<?> clazz) {
    String tableName = ApolloAuditUtil.getApolloAuditLogTableName(clazz);
    if (Objects.isNull(tableName) || tableName.equals("")) {
      logger.debug("entity not being managed by audit annotations");
      return;
    }
    List<Field> dataInfluenceFields = ApolloAuditUtil.getAnnotatedFields(
        ApolloAuditLogDataInfluenceTableField.class, clazz);
    Field idField = ApolloAuditUtil.getPersistenceIdFieldByAnnotation(clazz);
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
    } catch (IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
    dataInfluenceFields.forEach(field -> {
      field.setAccessible(true);
      try {
        String val = field.get(e).toString();
        appendSingleDataInfluence(tableId, tableName, field.getName(), null, val);
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
    });

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
  public List<ApolloAuditLogDTO> queryLogsByOpName(String opName, int page, int size) {
    return ApolloAuditUtil.logListToDTOList(logService.findByOpName(opName, page, size));
  }

  @Override
  public List<ApolloAuditLogDetailsDTO> queryTraceDetails(String traceId) {
    List<ApolloAuditLogDetailsDTO> detailsDTOList = new ArrayList<>();
    logService.findByTraceId(traceId).forEach(log -> {
      detailsDTOList.add(
          new ApolloAuditLogDetailsDTO(ApolloAuditUtil.logToDTO(log),
              ApolloAuditUtil.dataInfluenceListToDTOList(dataInfluenceService.findBySpanId(log.getSpanId()))
          )
      );
    });
    return detailsDTOList;
  }

  @Override
  public List<ApolloAuditLogDataInfluenceDTO> queryDataInfluencesByEntity(String entityName,
      String entityId, int page, int size) {
    return ApolloAuditUtil.dataInfluenceListToDTOList(
        dataInfluenceService.findByEntityNameAndEntityId(entityName, entityId, page, size));
  }
}
