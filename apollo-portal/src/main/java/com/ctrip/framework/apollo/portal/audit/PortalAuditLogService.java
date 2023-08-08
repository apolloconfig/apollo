package com.ctrip.framework.apollo.portal.audit;

public class PortalAuditLogService {

//  private final AuditLogRepository auditLogRepository;
//  private final AuditLogDataInfluenceRepository dataInfluenceRepository;
//
//  public PortalAuditLogService(AuditLogRepository auditLogRepository,
//      AuditLogDataInfluenceRepository dataInfluenceRepository) {
//    this.auditLogRepository = auditLogRepository;
//    this.dataInfluenceRepository = dataInfluenceRepository;
//  }
//
//  @Override
//  public void logSpan(AuditSpan span) {
//    AuditLog auditLog = new AuditLog();
//    auditLog.setTraceId(span.context().getTraceId());
//    auditLog.setSpanId(span.context().getSpanId());
//    auditLog.setParentSpanId(span.getParentId());
//    auditLog.setFollowsFromSpanId(span.getFollowsFromId());
//    auditLog.setOperator(span.context().getOperator());
//    auditLog.setOpName(span.getOpName());
//    auditLog.setOpType(span.getOpType().toString());
//    auditLog.setDescription(span.getDescription());
//    auditLog.setDataChangeCreatedBy(span.context().getOperator());
//    auditLog = setHost(auditLog);
//    //TODO : data influence
//    auditLogRepository.save(auditLog);
//  }
//
//  @Override
//  public void logSpanDataInfluence(String spanId, Object... data) {
//
//  }
//
//  AuditLog setHost(AuditLog auditLog)  {
//    ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//    HttpServletRequest request = servletRequestAttributes.getRequest();
//    auditLog.setSourceHost(request.getRemoteHost());
//
//    InetAddress localhost = null;
//    try {
//      localhost = InetAddress.getLocalHost();
//    } catch (UnknownHostException e) {
//      throw new RuntimeException(e);
//    }
//    auditLog.setCurrentHost(localhost.getHostName());
//
//    return auditLog;
//  }

}
