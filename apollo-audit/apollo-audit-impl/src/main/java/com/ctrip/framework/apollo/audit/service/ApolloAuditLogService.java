package com.ctrip.framework.apollo.audit.service;

import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogRepository;
import java.util.Date;
import java.util.List;

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
        .operator(span.context().getOperator())
        .opName(span.getOpName())
        .opType(span.getOpType().toString())
        .description(span.getDescription())
        .happenTime(new Date())
        .build();

    logRepository.save(auditLog);
  }

  public List<ApolloAuditLog> findAuditLogByTraceId(String traceId) {
    return logRepository.findByTraceId(traceId);
  }

}
