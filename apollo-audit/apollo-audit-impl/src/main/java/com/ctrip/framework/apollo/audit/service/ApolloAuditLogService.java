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
package com.ctrip.framework.apollo.audit.service;

import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogRepository;
import java.util.Date;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;

public class ApolloAuditLogService {

  private final ApolloAuditLogRepository logRepository;

  public ApolloAuditLogService(ApolloAuditLogRepository logRepository) {
    this.logRepository = logRepository;
  }

  public ApolloAuditLog save(ApolloAuditLog auditLog) {
    return logRepository.save(auditLog);
  }

  public void logSpan(ApolloAuditSpan span) {

    ApolloAuditLog auditLog = ApolloAuditLog.builder()
        .traceId(span.traceId())
        .spanId(span.spanId())
        .parentSpanId(span.parentId())
        .followsFromSpanId(span.followsFromId())
        .operator(span.operator() != null ? span.operator() : "anonymous")
        .opName(span.getOpName())
        .opType(span.getOpType().toString())
        .description(span.getDescription())
        .happenedTime(new Date())
        .build();
    logRepository.save(auditLog);
  }

  public List<ApolloAuditLog> findByTraceId(String traceId) {
    return logRepository.findByTraceIdOrderByDataChangeCreatedTimeDesc(traceId);
  }

  public List<ApolloAuditLog> findAll(int page, int size) {
    Pageable pageable = pageSortByTime(page, size);
    return logRepository.findAll(pageable).getContent();
  }

  public List<ApolloAuditLog> findByOpName(String opName, int page, int size) {
    Pageable pageable = pageSortByTime(page, size);
    return logRepository.findByOpName(opName, pageable);
  }

  public List<ApolloAuditLog> findByOpNameAndTime(String opName, Date startDate,
      Date endDate, int page, int size) {
    Pageable pageable = pageSortByTime(page, size);
    return logRepository.findByOpNameAndDataChangeCreatedTimeGreaterThanEqualAndDataChangeCreatedTimeLessThanEqual(
        opName, startDate, endDate, pageable);
  }

  public List<ApolloAuditLog> searchLogByNameOrTypeOrOperator(String query, int page, int size) {
    Pageable pageable = pageSortByTime(page, size);
    return logRepository.findByOpNameContainingOrOpTypeContainingOrOperatorContaining(query, query,
        query, pageable);
  }

  Pageable pageSortByTime(int page, int size) {
    return PageRequest.of(page, size, Sort.by(new Order(Direction.DESC, "dataChangeCreatedTime")));
  }

}
