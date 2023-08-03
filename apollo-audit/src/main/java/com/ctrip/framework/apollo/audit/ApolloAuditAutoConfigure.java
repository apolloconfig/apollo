package com.ctrip.framework.apollo.audit;

import com.ctrip.framework.apollo.audit.aop.AuditSpanAspect;
import com.ctrip.framework.apollo.audit.context.AuditScopeManager;
import com.ctrip.framework.apollo.audit.context.AuditTracer;
import com.ctrip.framework.apollo.audit.repository.AuditLogDataInfluenceRepository;
import com.ctrip.framework.apollo.audit.repository.AuditLogRepository;
import com.ctrip.framework.apollo.audit.service.AuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.AuditLogService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
//@EnableJpaRepositories(basePackages = "com.ctrip.framework.apollo.audit.repository")
@EnableConfigurationProperties(ApolloAuditProperties.class)
//@PropertySource("classpath:application-audit.properties")
public class ApolloAuditAutoConfigure {

  private final ApolloAuditProperties auditProperties;
  //private final AuditLogRepository auditLogRepository;
  //private final AuditLogDataInfluenceRepository dataInfluenceRepository;

//
//  public ApolloAuditAutoConfigure(ApolloAuditProperties auditProperties,
//      AuditLogRepository auditLogRepository,
//      AuditLogDataInfluenceRepository dataInfluenceRepository) {
//    this.auditProperties = auditProperties;
//    this.auditLogRepository = auditLogRepository;
//    this.dataInfluenceRepository = dataInfluenceRepository;
//    System.out.println("\n ApolloAuditAutoConfigure \n success build \n");
//  }


  public ApolloAuditAutoConfigure(ApolloAuditProperties auditProperties) {
    this.auditProperties = auditProperties;
  }

//  @Bean
//  public AuditLogDataInfluenceService auditLogDataInfluenceService() {
//    return new AuditLogDataInfluenceService(dataInfluenceRepository);
//  }

//  @Bean
//  public AuditLogService auditLogService() {
//    return new AuditLogService(auditLogRepository, auditLogDataInfluenceService());
//  }

  @Bean
  @ConditionalOnMissingBean(AuditScopeManager.class)
  public AuditScopeManager auditScopeManager() {
    return new AuditScopeManager();
  }

  @Bean
  public AuditTracer auditTracer() {
    return new AuditTracer(auditScopeManager());
  }

  @Bean
  public AuditSpanAspect auditSpanAspect() {
    return new AuditSpanAspect(auditTracer());
  }


}
