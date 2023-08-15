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
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogDataInfluenceProducer;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScopeManager;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogDataInfluenceRepository;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogRepository;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import com.ctrip.framework.apollo.audit.spi.defaultimpl.DefaultAuditSpanService;
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
    logger.info("ApolloAuditAutoConfigure initializing...");
  }

  @Bean
  public ApolloAuditLogDataInfluenceService dataInfluenceService(
      ApolloAuditLogDataInfluenceRepository dataInfluenceRepository) {
    logger.info("registering ApolloAuditLogDataInfluenceService");
    return new ApolloAuditLogDataInfluenceService(dataInfluenceRepository);
  }

  @Bean
  public ApolloAuditLogService apolloAuditLogService(ApolloAuditLogRepository logRepository,
      ApolloAuditLogDataInfluenceService dataInfluenceService) {
    logger.info("registering ApolloAuditLogService");
    return new ApolloAuditLogService(logRepository, dataInfluenceService);
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditSpanService.class)
  public ApolloAuditSpanService apolloAuditSpanService() {
    logger.info("ApolloAuditSpanService impl not found, register by default impl");
    return new DefaultAuditSpanService();
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditScopeManager.class)
  public ApolloAuditScopeManager auditScopeManager() {
    return new ApolloAuditScopeManager();
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditTracer.class)
  public ApolloAuditTracer auditTracer(ApolloAuditScopeManager manager) {
    return new ApolloAuditTracer(manager);
  }

  @Bean
  public ApolloAuditLogDataInfluenceProducer apolloAuditLogDataInfluenceProducer(
      ApolloAuditLogService logService, ApolloAuditLogDataInfluenceService dataInfluenceService,
      ApolloAuditTracer tracer) {
    logger.info("registering ApolloAuditLogDataInfluenceProducer");
    return new ApolloAuditLogDataInfluenceProducer(logService, dataInfluenceService, tracer);
  }

  @Bean
  public ApolloAuditSpanAspect auditSpanAspect(ApolloAuditTracer tracer,
      ApolloAuditLogService logService, ApolloAuditSpanService spanService,
      ApolloAuditLogDataInfluenceProducer producer) {
    logger.info("registering ApolloAuditSpanAspect");
    return new ApolloAuditSpanAspect(tracer, logService, spanService, producer);
  }

}
