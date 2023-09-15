/*
 * Copyright 2023 Apollo Authors
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
package com.ctrip.framework.apollo.audit;

import com.ctrip.framework.apollo.audit.aop.ApolloAuditSpanAspect;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.component.ApolloAuditHttpTracerInterceptor;
import com.ctrip.framework.apollo.audit.component.JpaApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.component.NoOpAuditSpanService;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScopeManager;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.controller.ApolloAuditController;
import com.ctrip.framework.apollo.audit.listener.ApolloAuditLogDataInfluenceEventListener;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogDataInfluenceRepository;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogRepository;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(ApolloAuditProperties.class)
@Import(ApolloAuditRegistrar.class)
@ConditionalOnProperty(prefix = "apollo.audit.log", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ApolloAuditAutoConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(ApolloAuditAutoConfiguration.class);

  private final ApolloAuditProperties auditProperties;

  public ApolloAuditAutoConfiguration(ApolloAuditProperties auditProperties) {
    this.auditProperties = auditProperties;
    logger.debug("ApolloAuditAutoConfigure initializing...");
  }

  @Bean
  public ApolloAuditLogDataInfluenceService apolloAuditLogDataInfluenceService(
      ApolloAuditLogDataInfluenceRepository dataInfluenceRepository) {
    return new ApolloAuditLogDataInfluenceService(dataInfluenceRepository);
  }

  @Bean
  public ApolloAuditLogService apolloAuditLogService(ApolloAuditLogRepository logRepository,
      ApolloAuditLogDataInfluenceService dataInfluenceService) {
    return new ApolloAuditLogService(logRepository, dataInfluenceService);
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditSpanService.class)
  public ApolloAuditSpanService apolloAuditSpanService() {
    return new NoOpAuditSpanService();
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditScopeManager.class)
  public ApolloAuditScopeManager apolloAuditScopeManager() {
    return new ApolloAuditScopeManager();
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditTracer.class)
  public ApolloAuditTracer auditTracer(ApolloAuditScopeManager manager) {
    return new ApolloAuditTracer(manager);
  }

  @Bean
  public ApolloAuditLogApi apolloAuditLogApi(ApolloAuditLogService logService,
      ApolloAuditLogDataInfluenceService dataInfluenceService, ApolloAuditSpanService spanService,
      ApolloAuditTracer tracer) {
    return new JpaApolloAuditLogApi(logService, dataInfluenceService, spanService, tracer);
  }

  @Bean
  public ApolloAuditSpanAspect apolloAuditSpanAspect(ApolloAuditLogApi apolloAuditLogApi) {
    return new ApolloAuditSpanAspect(apolloAuditLogApi);
  }

  @Bean
  public ApolloAuditHttpTracerInterceptor apolloAuditHttpTracerInterceptor(
      ApolloAuditTracer tracer) {
    return new ApolloAuditHttpTracerInterceptor(tracer);
  }

  @Bean
  public ApolloAuditController apolloAuditController(ApolloAuditLogApi api) {
    return new ApolloAuditController(api);
  }

  @Bean
  public ApolloAuditLogDataInfluenceEventListener apolloAuditLogDataInfluenceEventListener(
      ApolloAuditLogApi api) {
    return new ApolloAuditLogDataInfluenceEventListener(api);
  }

}
