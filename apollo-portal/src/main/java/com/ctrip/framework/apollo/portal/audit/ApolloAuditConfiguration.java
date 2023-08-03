package com.ctrip.framework.apollo.portal.audit;

import com.ctrip.framework.apollo.audit.aop.AuditSpanAspect;
import com.ctrip.framework.apollo.audit.context.AuditScopeManager;
import com.ctrip.framework.apollo.audit.context.AuditTracer;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditLogRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class ApolloAuditConfiguration {

//
//  @Bean
//  public AuditScopeManager manager() {
//    return new AuditScopeManager();
//  }
//
//  @Bean
//  public AuditTracer tracer() {
//    return new AuditTracer(manager());
//  }
//
//  @Bean
//  public ApolloAuditLogRepository auditLogRepository() {
//    return new PortalAuditLogRepository();
//  }
//
//  @Bean
//  public AuditSpanAspect auditSpanAspect() {
//    return new AuditSpanAspect(tracer(), auditLogRepository());
//  }

}
