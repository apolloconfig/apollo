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
package com.ctrip.framework.apollo.audit.api;

import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDataInfluenceDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDetailsDTO;
import java.util.Date;
import java.util.List;

/**
 * Mainly used to query AuditLogs and DataInfluences.
 *
 * @author luke0125
 * @since 2.2.0
 */
public interface ApolloAuditLogQueryApi {

  /**
   * Query all AuditLogs by page
   *
   * @param page index from 0
   * @param size size of a page
   * @return List of ApolloAuditLogDTO
   */
  List<ApolloAuditLogDTO> queryLogs(int page, int size);

  /**
   * Query AuditLogs by operator name and time limit and page
   *
   * @param opName    operation name of querying
   * @param startDate expect result after or equal this time
   * @param endDate   expect result before or equal this time
   * @param page      index from 0
   * @param size      size of a page
   * @return List of ApolloAuditLogDTO
   */
  List<ApolloAuditLogDTO> queryLogsByOpName(String opName, Date startDate, Date endDate, int page,
      int size);

  /**
   * Query AuditLogDetails by trace id.
   * <p></p>
   * An AuditLogDetail contains an AuditLog and DataInfluences it caused.
   * <p></p>
   * <pre>
   * {@code
   *   An AuditLogDetail:
   *   {
   *     LogDTO:{},
   *     DataInfluencesDTO:[]
   *   }
   * }
   * </pre>
   *
   * @param traceId unique id of a operation trace
   * @return List of ApolloAuditLogDetailsDTO
   */
  List<ApolloAuditLogDetailsDTO> queryTraceDetails(String traceId);

  /**
   * Query DataInfluences by specific entity's specified field and page
   *
   * @param entityName target entity's name(audit table name)
   * @param entityId   target entity's id(audit table id)
   * @param fieldName  target field's name(audit field id)
   * @param page       index from 0
   * @param size       size of a page
   * @return List of ApolloAuditLogDetailsDTO
   */
  List<ApolloAuditLogDataInfluenceDTO> queryDataInfluencesByField(String entityName,
      String entityId, String fieldName, int page, int size);

  /**
   * Fuzzy search related AuditLog by query-string and page, page index from 0.
   *
   * @param query input query string, used to fuzzy search
   * @param page  index from 0
   * @param size  size of a page
   * @return List of ApolloAuditLogDetailsDTO
   */
  List<ApolloAuditLogDTO> searchLogByNameOrTypeOrOperator(String query, int page, int size);

}