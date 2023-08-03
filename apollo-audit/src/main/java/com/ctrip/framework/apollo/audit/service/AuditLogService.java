package com.ctrip.framework.apollo.audit.service;

import com.ctrip.framework.apollo.audit.context.AuditSpan;
import com.ctrip.framework.apollo.audit.entity.AuditLog;
import com.ctrip.framework.apollo.audit.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

  private final AuditLogRepository auditLogRepository;
  private final AuditLogDataInfluenceService dataInfluenceService;

  public AuditLogService(AuditLogRepository auditLogRepository,
      AuditLogDataInfluenceService dataInfluenceService) {
    this.auditLogRepository = auditLogRepository;
    this.dataInfluenceService = dataInfluenceService;
  }


  public AuditLog create(AuditSpan span) {
    AuditLog auditLog = new AuditLog();
    auditLog.setTraceId(span.context().getTraceId());
    auditLog.setSpanId(span.context().getSpanId());
    auditLog.setParentSpanId(span.getParentId());
    auditLog.setFollowsFromSpanId(span.getFollowsFromId());
    auditLog.setOperator(span.context().getOperator());
    auditLog.setOpName(span.getOpName());
    auditLog.setOpType(span.getOpType().toString());
    auditLog.setDescription(span.getDescription());
    //TODO : host
    //TODO : data influence
    return auditLogRepository.save(auditLog);
  }


}
