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
package com.ctrip.framework.apollo.audit.controller;

import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDataInfluenceDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDetailsDTO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apollo/audit")
public class ApolloAuditController {

  private final ApolloAuditLogApi api;

  public ApolloAuditController(ApolloAuditLogApi api) {
    this.api = api;
  }

  @GetMapping("/logs")
  public List<ApolloAuditLogDTO> findAllAuditLogs(int page, int size) {
    List<ApolloAuditLogDTO> logDTOList = api.queryLogs(page, size);
    return logDTOList;
  }

  @GetMapping("/trace/{traceId}")
  public List<ApolloAuditLogDetailsDTO> findTraceDetails(@PathVariable String traceId) {
    List<ApolloAuditLogDetailsDTO> detailsDTOList = api.queryTraceDetails(traceId);
    return detailsDTOList;
  }

  @GetMapping("/logs/opName/{opName}")
  public List<ApolloAuditLogDTO> findAllAuditLogsByOpName(@PathVariable String opName, int page,
      int size) {
    List<ApolloAuditLogDTO> logDTOList = api.queryLogsByOpName(opName, page, size);
    return logDTOList;
  }

  @GetMapping("/logs/dataInfluences/entityName/{entityName}/entityId/{entityId}")
  public List<ApolloAuditLogDataInfluenceDTO> findDataInfluencesByEntity(
      @PathVariable String entityName, @PathVariable String entityId, int page, int size) {
    List<ApolloAuditLogDataInfluenceDTO> dataInfluenceDTOList = api.queryDataInfluencesByEntity(
        entityName, entityId, page, size);
    return dataInfluenceDTOList;
  }

}
