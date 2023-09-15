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
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLogDataInfluence;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
  public List<ApolloAuditLog> findAllAuditLogs(Pageable page) {
    List<ApolloAuditLog> logs = api.queryAllLogs(page);
    return logs;
  }

  @GetMapping("/logs/related")
  public List<ApolloAuditLog> findByRelated(ApolloAuditLog log, Pageable page) {
    List<ApolloAuditLog> logs = api.queryRelatedLogs(log,page);
    return logs;
  }

  @GetMapping("/logs/opName/{opName}")
  public List<ApolloAuditLog> findAllAuditLogsByOpName(@PathVariable String opName, Pageable page) {
    List<ApolloAuditLog> logs = api.queryLogsByOpName(opName, page);
    return logs;
  }

  @GetMapping("/logs/operator/{operator}")
  public List<ApolloAuditLog> findAllAuditLogsByOperator(@PathVariable String operator,
      Pageable page) {
    List<ApolloAuditLog> logs = api.queryLogsByOperator(operator, page);
    return logs;
  }

  @GetMapping("/logs/dataInfluences/by-log")
  public List<ApolloAuditLogDataInfluence> findDataInfluencesBySpanId(
      @RequestBody ApolloAuditLog log,
      Pageable page) {
    List<ApolloAuditLogDataInfluence> dataInfluences = api.queryDataInfluencesByLog(log, page);
    return dataInfluences;
  }

  @GetMapping("/logs/dataInfluences/related")
  public List<ApolloAuditLogDataInfluence> findDataInfluencesByEntityName(
      @RequestBody ApolloAuditLogDataInfluence dataInfluence, Pageable page) {
    List<ApolloAuditLogDataInfluence> dataInfluences = api.queryRelatedDataInfluences(dataInfluence,
        page);
    return dataInfluences;
  }

  @GetMapping("/logs/dataInfluences/entityName/{entityName}/entityId/{entityId}")
  public List<ApolloAuditLogDataInfluence> findDataInfluencesByEntity(
      @PathVariable String entityName, @PathVariable String entityId, Pageable page) {
    List<ApolloAuditLogDataInfluence> dataInfluences = api.queryDataInfluencesByEntity(entityName,
        entityId, page);
    return dataInfluences;
  }

}
