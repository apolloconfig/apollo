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
package com.ctrip.framework.apollo.audit.service;

import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

public class ApolloAuditLogService {

  private final ApolloAuditLogRepository logRepository;
  private final ApolloAuditLogDataInfluenceService dataInfluenceService;

  public ApolloAuditLogService(ApolloAuditLogRepository logRepository,
      ApolloAuditLogDataInfluenceService dataInfluenceService) {
    this.logRepository = logRepository;
    this.dataInfluenceService = dataInfluenceService;
  }

  public ApolloAuditLog save(ApolloAuditLog auditLog) {
    auditLog.setId(0);
    return logRepository.save(auditLog);
  }

  public void logSpan(ApolloAuditSpan span) {

    ApolloAuditLog auditLog = ApolloAuditLog.builder()
        .traceId(span.context().getTraceId())
        .spanId(span.context().getSpanId())
        .parentSpanId(span.getParentId())
        .followsFromSpanId(span.getFollowsFromId())
        .operator(span.context().getOperator() != null ? span.context().getOperator() : "unknown")
        .opName(span.getOpName())
        .opType(span.getOpType().toString())
        .description(span.getDescription())
        .build();

    auditLog.setId(0);
    auditLog.setDataChangeCreatedBy(
        span.context().getOperator() != null ? span.context().getOperator() : "unknown");
    logRepository.save(auditLog);
  }

  public List<ApolloAuditLog> findAuditLogByTraceId(String traceId, Pageable page) {
    return logRepository.findByTraceIdOrderByDataChangeCreatedTimeDesc(traceId, page);
  }

  public List<ApolloAuditLog> findAll(Pageable page) {
    Pageable pageable = pageSortByTime(page);
    return logRepository.findAll(pageable).getContent();
  }

  public List<ApolloAuditLog> findByOpType(String opType, Pageable page) {
    Pageable pageable = pageSortByTime(page);
    return logRepository.findByOpType(opType, pageable);
  }

  public List<ApolloAuditLog> findByOpName(String opName, Pageable page) {
    Pageable pageable = pageSortByTime(page);
    return logRepository.findByOpName(opName, pageable);
  }

  public List<ApolloAuditLog> findByOperator(String operator, Pageable page) {
    Pageable pageable = pageSortByTime(page);
    return logRepository.findByOperator(operator, pageable);
  }

  Pageable pageSortByTime(Pageable page) {
    return PageRequest.of(page.getPageNumber(), page.getPageSize(), Sort.by(
        new Order(Direction.DESC, "DataChangeCreatedTime")));
  }


}