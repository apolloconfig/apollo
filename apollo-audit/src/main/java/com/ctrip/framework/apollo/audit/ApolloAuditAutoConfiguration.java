package com.ctrip.framework.apollo.audit;

import com.ctrip.framework.apollo.audit.aop.ApolloAuditSpanAspect;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScopeManager;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import com.ctrip.framework.apollo.audit.spi.defaultimpl.DefaultAuditSpanService;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogDataInfluenceRepository;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableConfigurationProperties(ApolloAuditProperties.class)
@AutoConfigureAfter({JpaRepositoriesAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
//@ComponentScan(basePackageClasses = ApolloAuditAutoConfiguration.class)
//@EntityScan("com.ctrip.framework.apollo.audit.entity")
@EnableJpaRepositories("com.ctrip.framework.apollo.audit.repository")

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
