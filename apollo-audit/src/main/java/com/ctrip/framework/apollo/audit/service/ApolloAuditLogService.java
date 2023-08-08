package com.ctrip.framework.apollo.audit.service;

import com.ctrip.framework.apollo.audit.context.ApolloAuditSpan;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogDataInfluenceRepository;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogRepository;

public class ApolloAuditLogService {

  private final ApolloAuditLogRepository logRepository;
  private final ApolloAuditLogDataInfluenceRepository dataInfluenceRepository;

  public ApolloAuditLogService(ApolloAuditLogRepository logRepository,
      ApolloAuditLogDataInfluenceRepository dataInfluenceRepository) {
    this.logRepository = logRepository;
    this.dataInfluenceRepository = dataInfluenceRepository;
  }

  public void logSpan(ApolloAuditSpan span) {
    ApolloAuditLog auditLog = new ApolloAuditLog();
    auditLog.setTraceId(span.context().getTraceId());
    auditLog.setSpanId(span.context().getSpanId());
    auditLog.setParentSpanId(span.getParentId());
    auditLog.setFollowsFromSpanId(span.getFollowsFromId());
    auditLog.setOperator(span.context().getOperator());
    auditLog.setOpName(span.getOpName());
    auditLog.setOpType(span.getOpType().toString());
    auditLog.setDescription(span.getDescription());
    auditLog.setDataChangeCreatedBy(span.context().getOperator());
    //TODO : data influence
    logRepository.save(auditLog);
  }

  public void logSpanDataInfluence(String spanId, Object... data) {

  }


}
