package com.ctrip.framework.apollo.audit;

import com.ctrip.framework.apollo.audit.aop.ApolloAuditSpanAspect;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScopeManager;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import com.ctrip.framework.apollo.audit.spi.defaultimpl.DefaultAuditSpanService;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogDataInfluenceRepository;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(ApolloAuditProperties.class)
@Import(ApolloAuditRegistrar.class)
@ConditionalOnProperty(
    prefix = "apollo.audit.log",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class ApolloAuditAutoConfiguration {

  private final ApolloAuditProperties auditProperties;

  public ApolloAuditAutoConfiguration(ApolloAuditProperties auditProperties) {
    this.auditProperties = auditProperties;
    System.out.println("\n ApolloAuditAutoConfigure \n success build \n");
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditLogService.class)
  public ApolloAuditLogService apolloAuditLogService(ApolloAuditLogRepository logRepository,
      ApolloAuditLogDataInfluenceRepository dataInfluenceRepository) {
    return new ApolloAuditLogService(logRepository, dataInfluenceRepository);
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditSpanService.class)
  public ApolloAuditSpanService apolloAuditSpanService() {
    System.out.println("ApolloAuditSpanService impl not found, register by default impl");
    return new DefaultAuditSpanService();
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditScopeManager.class)
  public ApolloAuditScopeManager auditScopeManager() {
    return new ApolloAuditScopeManager();
  }

  @Bean
  public ApolloAuditTracer auditTracer(ApolloAuditScopeManager manager) {
    return new ApolloAuditTracer(manager);
  }

  @Bean
  public ApolloAuditSpanAspect auditSpanAspect(ApolloAuditTracer tracer,
      ApolloAuditLogService logService,
      ApolloAuditSpanService spanService) {
    return new ApolloAuditSpanAspect(tracer, logService, spanService);
  }

}
