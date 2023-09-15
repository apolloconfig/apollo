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
import com.ctrip.framework.apollo.audit.api.ApolloAuditEntityWrapper;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScope;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.context.ApolloAuditSpanContext;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import com.ctrip.framework.apollo.audit.util.ApolloAuditUtil;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;

public class JpaApolloAuditLogApi implements ApolloAuditLogApi {

  private static final Logger logger = LoggerFactory.getLogger(JpaApolloAuditLogApi.class);

  private final ApolloAuditLogService logService;
  private final ApolloAuditLogDataInfluenceService dataInfluenceService;
  private final ApolloAuditSpanService spanService;
  private final ApolloAuditTracer tracer;

  public JpaApolloAuditLogApi(ApolloAuditLogService logService,
      ApolloAuditLogDataInfluenceService dataInfluenceService, ApolloAuditSpanService spanService,
      ApolloAuditTracer tracer) {
    this.logService = logService;
    this.dataInfluenceService = dataInfluenceService;
    this.spanService = spanService;
    this.tracer = tracer;
  }

  @Override
  public Map extractSpan() {
    return tracer.extract();
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
  public void appendDataInfluenceWrapper(Class<?> clazz) {
    ApolloAuditEntityWrapper wrapper = new ApolloAuditEntityWrapper();
    String entityName = ApolloAuditUtil.getApolloAuditLogTableName(clazz);
    Field idField = ApolloAuditUtil.getPersistenceIdFieldByAnnotation(clazz);
    if (Objects.isNull(idField) || Objects.isNull(entityName)) {
      logger.warn("managed class:{} is not managed by Apollo audit", clazz);
      return;
    }
    List<Field> dataInfluenceFields = ApolloAuditUtil.getAnnotatedFields(
        ApolloAuditLogDataInfluenceTableField.class, clazz);
    wrapper.entityName(entityName).entityIdField(idField).dataInfluenceFields(dataInfluenceFields);
    appendDataInfluenceWrapper(clazz, wrapper);
  }

  @Override
  public void appendDataInfluenceWrapper(Class<?> clazz, ApolloAuditEntityWrapper wrapper) {
    tracer.scopeManager().activeSpanContext().putWrapper(clazz, wrapper);
  }

  /**
   * should have possibility to return null, for not creating unexpected wrapper
   *
   * @param clazz
   * @return
   */
  public ApolloAuditEntityWrapper getEntityWrapper(Class<?> clazz) {
    if (tracer.scopeManager().activeSpanContext() == null) {
      return null;
    }
    return tracer.scopeManager().activeSpanContext().getWrapper(clazz);
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
  public <T> void appendDataInfluences(List<T> entities, boolean isDeleted, Class<?> clazz) {
    ApolloAuditEntityWrapper wrapper = getEntityWrapper(clazz);
    appendDataInfluences(entities, isDeleted, wrapper);
  }

  @Override
  public <T> void appendDataInfluences(List<T> entities, boolean isDeleted,
      ApolloAuditEntityWrapper wrapper) {
    if (Objects.isNull(wrapper)) {
      return;
    }
    if (isDeleted) {
      appendDeleteDataInfluencesByWrapper(entities, wrapper);
    }
    else {
      appendCreateOrUpdateDataInfluencesByWrapper(entities, wrapper);
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

  @Override
  public List<ApolloAuditLog> queryAllLogs(Pageable page) {
    return logService.findAll(page);
  }

  @Override
  public List<ApolloAuditLog> queryLogsByOpName(String opName, Pageable page) {
    return logService.findByOpName(opName, page);
  }

  @Override
  public List<ApolloAuditLog> queryLogsByOperator(String operator, Pageable page) {
    return logService.findByOperator(operator, page);
  }

  @Override
  public List<ApolloAuditLog> queryRelatedLogs(ApolloAuditLog log, Pageable page) {
    return logService.findByTraceId(log.getTraceId(), page);
  }

  @Override
  public List<ApolloAuditLogDataInfluence> queryDataInfluencesByLog(ApolloAuditLog log,
      Pageable page) {
    return dataInfluenceService.findBySpanId(log.getSpanId(), page);
  }

  @Override
  public List<ApolloAuditLogDataInfluence> queryRelatedDataInfluences(
      ApolloAuditLogDataInfluence dataInfluence, Pageable page) {
    return dataInfluenceService.findBySpanId(dataInfluence.getSpanId(), page);
  }

  @Override
  public List<ApolloAuditLogDataInfluence> queryDataInfluencesByEntity(String entityName,
      String entityId, Pageable page) {
    return dataInfluenceService.findByEntityNameAndEntityId(entityName, entityId, page);
  }
}
