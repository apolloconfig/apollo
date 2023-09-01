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
import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTableId;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.api.ApolloAuditEntityWrapper;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScope;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import com.ctrip.framework.apollo.audit.util.ApolloAuditUtil;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DaoApolloAuditLogApi implements ApolloAuditLogApi {

  private static final Logger logger = LoggerFactory.getLogger(DaoApolloAuditLogApi.class);

  private final ApolloAuditLogService logService;
  private final ApolloAuditLogDataInfluenceService dataInfluenceService;
  private final ApolloAuditSpanService spanService;
  private final ApolloAuditTracer tracer;

  public DaoApolloAuditLogApi(ApolloAuditLogService logService,
      ApolloAuditLogDataInfluenceService dataInfluenceService, ApolloAuditSpanService spanService,
      ApolloAuditTracer tracer) {
    this.logService = logService;
    this.dataInfluenceService = dataInfluenceService;
    this.spanService = spanService;
    this.tracer = tracer;
  }

  @Override
  public AutoCloseable appendSpan(OpType type, String name) {
    return appendSpan(type, name, null);
  }

  @Override
  public AutoCloseable appendSpan(OpType type, String name, String description) {
    ApolloAuditSpanContext parentSpanContext = getParentContext();
    ApolloAuditSpan span = generateSpan(parentSpanContext, type, name, description);
    ApolloAuditScope scope = tracer.scopeManager().activate(span.getSpanContext());
    logService.logSpan(span);
    return scope;
  }

  @Override
  public void appendSingleDataInfluence(String tableId, String entityName, String fieldName,
      String fieldOldValue, String fieldNewValue) {
    if (tracer.scopeManager().activeSpanContext() == null) {
      return;
    }
    String spanId = tracer.scopeManager().activeSpanContext().getSpanId();
    ApolloAuditLogDataInfluence influence = ApolloAuditLogDataInfluence.builder().spanId(spanId)
        .entityName(entityName).entityId(tableId).fieldName(fieldName).oldVal(fieldOldValue)
        .newVal(fieldNewValue).build();

    dataInfluenceService.save(influence);
  }

  @Override
  public <T> void appendDataInfluencesByManagedClass(List<T> entities, OpType type,
      Class<?> managedClass) {

    String entityName = ApolloAuditUtil.getApolloAuditLogTableName(managedClass);
    Field idField = ApolloAuditUtil.getAnnotatedField(ApolloAuditLogDataInfluenceTableId.class,
        managedClass);
    if (Objects.isNull(idField)) {
      idField = ApolloAuditUtil.tryToGetIdField(managedClass);
    }
    List<Field> dataInfluenceFields = ApolloAuditUtil.getAnnotatedFields(
        ApolloAuditLogDataInfluenceTableField.class, managedClass);

    ApolloAuditEntityWrapper wrapper = new ApolloAuditEntityWrapper();
    wrapper.entityName(entityName)
        .entityIdField(idField)
        .dataInfluenceFields(dataInfluenceFields);

    appendDataInfluencesByWrapper(entities, type, wrapper);
  }

  @Override
  public <T> void appendDataInfluencesByWrapper(List<T> entities, OpType type,
      ApolloAuditEntityWrapper wrapper) {
    switch (type) {
      case CREATE:
      case UPDATE:
        appendCreateOrUpdateDataInfluencesByWrapper(entities, wrapper);
        break;
      case DELETE:
        appendDeleteDataInfluencesByWrapper(entities, wrapper);
        break;
    }
  }

  public <T> void appendCreateOrUpdateDataInfluencesByWrapper(List<T> entities,
      ApolloAuditEntityWrapper wrapper) {
    String tableName = wrapper.getEntityName();
    List<Field> dataInfluenceFields = wrapper.getDataInfluenceFields();
    Field tableIdField = wrapper.getEntityIdField();

    if (entities.isEmpty() || Objects.isNull(tableName) || Objects.isNull(tableIdField)
        || dataInfluenceFields.isEmpty()) {
      logger.debug("wrapper incomplete, will not log data influences");
      return;
    }

    entities.forEach(e -> {
      tableIdField.setAccessible(true);
      String tableId;
      try {
        tableId = tableIdField.get(e).toString();
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
    });
  }

  public <T> void appendDeleteDataInfluencesByWrapper(List<T> entities,
      ApolloAuditEntityWrapper wrapper) {
    String tableName = wrapper.getEntityName();
    List<Field> dataInfluenceFields = wrapper.getDataInfluenceFields();
    Field tableIdField = wrapper.getEntityIdField();

    if (entities.isEmpty() || Objects.isNull(tableName) || Objects.isNull(tableIdField)
        || dataInfluenceFields.isEmpty()) {
      logger.debug("wrapper incomplete, will not log data influences");
      return;
    }

    entities.forEach(e -> {
      tableIdField.setAccessible(true);
      String tableId;
      try {
        tableId = tableIdField.get(e).toString();
      } catch (IllegalAccessException ex) {
        throw new RuntimeException(ex);
      }
      dataInfluenceFields.forEach(
          field -> appendSingleDataInfluence(tableId, tableName, field.getName(), null, null));
    });
  }

  String getFollowSFromId() {
    if (tracer.scopeManager().getScope() == null) {
      return null;
    }
    if (tracer.scopeManager().getScope().getLastSpanContext() == null) {
      return null;
    }
    return tracer.scopeManager().getScope().getLastSpanContext().getSpanId();
  }


  ApolloAuditSpanContext getParentContext() {
    ApolloAuditSpanContext parentContext = tracer.scopeManager().activeSpanContext();
    if (parentContext == null) {
      parentContext = spanService.tryToGetParentSpanContext();
    }
    return parentContext;
  }

  // depend on parentSpanContext
  ApolloAuditSpan generateSpan(ApolloAuditSpanContext parentSpanContext, OpType type, String name,
      String description) {
    if (parentSpanContext == null) {
      return tracer.buildSpan(type, name).asRootSpan(spanService.getOperator())
          .description(description).followsFrom(getFollowSFromId()).build();
    } else {
      return tracer.buildSpan(type, name).asChildOf(parentSpanContext).description(description)
          .followsFrom(getFollowSFromId()).build();
    }
  }
}
