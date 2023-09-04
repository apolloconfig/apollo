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

import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apollo/audit")
public class ApolloAuditController {

  private final ApolloAuditLogService logService;
  private final ApolloAuditLogDataInfluenceService dataInfluenceService;

  public ApolloAuditController(ApolloAuditLogService logService,
      ApolloAuditLogDataInfluenceService dataInfluenceService) {
    this.logService = logService;
    this.dataInfluenceService = dataInfluenceService;
  }

  @GetMapping("/logs")
  public List<ApolloAuditLog> findAllAuditLogs(Pageable page) {
    List<ApolloAuditLog> logs = logService.findAll(page);
    return logs;
  }

  @GetMapping("/logs/op_type/{opType}")
  public List<ApolloAuditLog> findAllAuditLogsByOpType(@PathVariable String opType, Pageable page) {
    List<ApolloAuditLog> logs = logService.findByOpType(opType, page);
    return logs;
  }

  @GetMapping("/logs/op_name/{opName}")
  public List<ApolloAuditLog> findAllAuditLogsByOpName(@PathVariable String opName, Pageable page) {
    List<ApolloAuditLog> logs = logService.findByOpName(opName, page);
    return logs;
  }

  @GetMapping("/logs/operator/{operator}")
  public List<ApolloAuditLog> findAllAuditLogsByOperator(@PathVariable String operator, Pageable page) {
    List<ApolloAuditLog> logs = logService.findByOperator(operator, page);
    return logs;
  }

  @GetMapping("/logs/data_influences/{spanId}")
  public List<ApolloAuditLogDataInfluence> findDataInfluencesBySpanId(@PathVariable String spanId,
      Pageable page) {
    List<ApolloAuditLogDataInfluence> dataInfluences = dataInfluenceService.findBySpanId(spanId,
        page);
    return dataInfluences;
  }

  @GetMapping("/logs/data_influences/entity_name/{entityName}")
  public List<ApolloAuditLogDataInfluence> findDataInfluencesByEntityName(
      @PathVariable String entityName, Pageable page) {
    List<ApolloAuditLogDataInfluence> dataInfluences = dataInfluenceService.findByEntityName(entityName,
        page);
    return dataInfluences;
  }

  @GetMapping("/logs/data_influences/entity_name/{entityName}/entity_id/{entityId}")
  public List<ApolloAuditLogDataInfluence> findDataInfluencesByEntity(
      @PathVariable String entityName, @PathVariable String entityId, Pageable page) {
    List<ApolloAuditLogDataInfluence> dataInfluences = dataInfluenceService.findByEntityNameAndEntityId(
        entityName, entityId, page);
    return dataInfluences;
  }

  @GetMapping("/logs/data_influences/entity_name/{entityName}/entity_id/{entityId}/field/{fieldName}")
  public List<ApolloAuditLogDataInfluence> findDataInfluencesByEntityField(
      @PathVariable String entityName, @PathVariable String entityId,
      @PathVariable String fieldName, Pageable page) {
    List<ApolloAuditLogDataInfluence> dataInfluences = dataInfluenceService.findByEntityNameAndEntityIdAndFieldName(
        entityName, entityId, fieldName, page);
    return dataInfluences;
  }

}
