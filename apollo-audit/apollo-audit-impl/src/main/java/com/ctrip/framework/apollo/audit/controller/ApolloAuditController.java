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
package com.ctrip.framework.apollo.audit.controller;

import com.ctrip.framework.apollo.audit.ApolloAuditProperties;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDataInfluenceDTO;
import com.ctrip.framework.apollo.audit.dto.ApolloAuditLogDetailsDTO;
import java.util.Date;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * About page: index from 0, default size is 10
 *
 * @author luke
 */
@RestController
@RequestMapping("/apollo/audit")
public class ApolloAuditController {

  private final ApolloAuditLogApi api;
  private final ApolloAuditProperties properties;

  public ApolloAuditController(ApolloAuditLogApi api, ApolloAuditProperties properties) {
    this.api = api;
    this.properties = properties;
  }

  @GetMapping("/properties")
  public ApolloAuditProperties getProperties() {
    return properties;
  }

  @GetMapping("/logs")
  @PreAuthorize(value = "@apolloAuditLogQueryApiPreAuthorizer.hasQueryPermission()")
  public List<ApolloAuditLogDTO> findAllAuditLogs(int page, int size) {
    List<ApolloAuditLogDTO> logDTOList = api.queryLogs(page, size);
    return logDTOList;
  }

  @GetMapping("/trace")
  @PreAuthorize(value = "@apolloAuditLogQueryApiPreAuthorizer.hasQueryPermission()")
  public List<ApolloAuditLogDetailsDTO> findTraceDetails(@RequestParam String traceId) {
    List<ApolloAuditLogDetailsDTO> detailsDTOList = api.queryTraceDetails(traceId);
    return detailsDTOList;
  }

  @GetMapping("/logs/opName")
  @PreAuthorize(value = "@apolloAuditLogQueryApiPreAuthorizer.hasQueryPermission()")
  public List<ApolloAuditLogDTO> findAllAuditLogsByOpNameAndTime(@RequestParam String opName,
      @RequestParam int page, @RequestParam int size,
      @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.S") Date startDate,
      @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss.S") Date endDate) {
    List<ApolloAuditLogDTO> logDTOList = api.queryLogsByOpName(opName, startDate, endDate, page,
        size);
    return logDTOList;
  }

  @GetMapping("/logs/dataInfluences/field")
  @PreAuthorize(value = "@apolloAuditLogQueryApiPreAuthorizer.hasQueryPermission()")
  public List<ApolloAuditLogDataInfluenceDTO> findDataInfluencesByField(
      @RequestParam String entityName, @RequestParam String entityId,
      @RequestParam String fieldName, int page, int size) {
    List<ApolloAuditLogDataInfluenceDTO> dataInfluenceDTOList = api.queryDataInfluencesByField(
        entityName, entityId, fieldName, page, size);
    return dataInfluenceDTOList;
  }

  @GetMapping("/logs/by-name-or-type-or-operator")
  @PreAuthorize(value = "@apolloAuditLogQueryApiPreAuthorizer.hasQueryPermission()")
  public List<ApolloAuditLogDTO> findAuditLogsByNameOrTypeOrOperator(@RequestParam String query, int page, int size) {
    List<ApolloAuditLogDTO> logDTOList = api.searchLogByNameOrTypeOrOperator(query, page, size);
    return logDTOList;
  }

}
