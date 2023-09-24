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

import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDataInfluenceDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDetailsDTO;
import java.util.Date;
import java.util.List;

public interface ApolloAuditLogQueryApi {

  List<ApolloAuditLogDTO> queryLogs(int page, int size);

  List<ApolloAuditLogDTO> queryLogsByOpName(String opName, Date startDate, Date endDate, int page,
      int size);

  List<ApolloAuditLogDetailsDTO> queryTraceDetails(String traceId);

  List<ApolloAuditLogDataInfluenceDTO> queryDataInfluencesByEntity(String entityName,
      String entityId, int page, int size);


  List<ApolloAuditLogDataInfluenceDTO> queryDataInfluencesByField(String entityName,
      String entityId, String fieldName, int page, int size);


}