package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit/log")
public class ApolloAuditLogController {

  private final ApolloAuditLogService auditLogService;
  private final UserInfoHolder userInfoHolder;

  public ApolloAuditLogController(ApolloAuditLogService auditLogService,
      UserInfoHolder userInfoHolder) {
    this.auditLogService = auditLogService;
    this.userInfoHolder = userInfoHolder;
  }

  @GetMapping("/by-traceId")
  public List<ApolloAuditLog> findAuditLogByTraceId(String traceId) {
    return auditLogService.findAuditLogByTraceId(traceId);
  }
}
