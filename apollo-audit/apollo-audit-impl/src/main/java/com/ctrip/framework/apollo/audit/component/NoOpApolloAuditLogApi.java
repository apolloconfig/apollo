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

import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.api.ApolloAuditEntityWrapper;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;

public class NoOpApolloAuditLogApi implements ApolloAuditLogApi {

  //do nothing, for default impl

  @Override
  public Map extractSpan() {
    return null;
  }

  @Override
  public AutoCloseable appendSpan(OpType type, String name) {
    return appendSpan(type, name, null);
  }

  @Override
  public AutoCloseable appendSpan(OpType type, String name, String description) {
    return () -> {
    };
  }

  @Override
  public void appendDataInfluenceWrapper(Class<?> clazz) {

  }

  @Override
  public void appendDataInfluenceWrapper(Class<?> clazz, ApolloAuditEntityWrapper wrapper) {

  }

  @Override
  public void appendSingleDataInfluence(String entityId, String entityName, String fieldName,
      String fieldOldValue, String fieldNewValue) {

  }

  @Override
  public <T> void appendDataInfluences(List<T> entities, boolean isDeleted, Class<?> clazz) {

  }

  @Override
  public <T> void appendDataInfluences(List<T> entities, boolean isDeleted,
      ApolloAuditEntityWrapper wrapper) {

  }

  @Override
  public List<ApolloAuditLog> queryAllLogs(Pageable page) {
    return null;
  }

  @Override
  public List<ApolloAuditLog> queryLogsByOpName(String opName, Pageable page) {
    return null;
  }

  @Override
  public List<ApolloAuditLog> queryLogsByOperator(String operator, Pageable page) {
    return null;
  }

  @Override
  public List<ApolloAuditLog> queryRelatedLogs(ApolloAuditLog log, Pageable page) {
    return null;
  }

  @Override
  public List<ApolloAuditLogDataInfluence> queryDataInfluencesByLog(ApolloAuditLog log,
      Pageable page) {
    return null;
  }

  @Override
  public List<ApolloAuditLogDataInfluence> queryRelatedDataInfluences(
      ApolloAuditLogDataInfluence dataInfluence, Pageable page) {
    return null;
  }

  @Override
  public List<ApolloAuditLogDataInfluence> queryDataInfluencesByEntity(String entityName,
      String entityId, Pageable page) {
    return null;
  }
}
