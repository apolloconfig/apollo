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
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDataInfluenceDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDetailsDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDTO;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ApolloAuditLogApiNoOpImpl implements ApolloAuditLogApi {

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
  public List<ApolloAuditLogDTO> queryLogs(int page, int size) {
    return null;
  }

  @Override
  public List<ApolloAuditLogDTO> queryLogsByOpName(String opName, Date startDate,
      Date endDate, int page, int size) {
    return null;
  }

  @Override
  public List<ApolloAuditLogDetailsDTO> queryTraceDetails(String traceId) {
    return null;
  }

  @Override
  public List<ApolloAuditLogDataInfluenceDTO> queryDataInfluencesByEntity(String entityName,
      String entityId, int page, int size) {
    return null;
  }

  @Override
  public List<ApolloAuditLogDataInfluenceDTO> queryDataInfluencesByField(String entityName,
      String entityId, String fieldName, int page, int size) {
    return null;
  }

  @Override
  public void appendDataInfluences(List entities, Class<?> clazz) {

  }

  @Override
  public void appendSingleDataInfluence(String entityId, String entityName, String fieldName,
      String fieldOldValue, String fieldNewValue) {

  }
}
