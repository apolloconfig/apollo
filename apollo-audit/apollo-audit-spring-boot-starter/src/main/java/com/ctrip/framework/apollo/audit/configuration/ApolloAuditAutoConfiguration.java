/*
 * Copyright 2024 Apollo Authors
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
package com.ctrip.framework.apollo.audit.configuration;

import com.ctrip.framework.apollo.audit.ApolloAuditProperties;
import com.ctrip.framework.apollo.audit.ApolloAuditRegistrar;
import com.ctrip.framework.apollo.audit.aop.ApolloAuditSpanAspect;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.component.ApolloAuditHttpInterceptor;
import com.ctrip.framework.apollo.audit.component.ApolloAuditLogApiJpaImpl;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTraceContext;
import com.ctrip.framework.apollo.audit.controller.ApolloAuditController;
import com.ctrip.framework.apollo.audit.listener.ApolloAuditLogDataInfluenceEventListener;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogDataInfluenceRepository;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogRepository;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogDataInfluenceService;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditLogQueryApiPreAuthorizer;
import com.ctrip.framework.apollo.audit.spi.ApolloAuditOperatorSupplier;
import com.ctrip.framework.apollo.audit.spi.defaultimpl.ApolloAuditLogQueryApiDefaultPreAuthorizer;
import com.ctrip.framework.apollo.audit.spi.defaultimpl.ApolloAuditOperatorDefaultSupplier;
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
@ConditionalOnProperty(prefix = "apollo.audit.log", name = "enabled", havingValue = "true")
public class ApolloAuditAutoConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(ApolloAuditAutoConfiguration.class);

  private final ApolloAuditProperties apolloAuditProperties;

  public ApolloAuditAutoConfiguration(ApolloAuditProperties apolloAuditProperties) {
    this.apolloAuditProperties = apolloAuditProperties;
    logger.info("ApolloAuditAutoConfigure initializing...");
  }

  @Bean
  public ApolloAuditLogDataInfluenceService apolloAuditLogDataInfluenceService(
      ApolloAuditLogDataInfluenceRepository dataInfluenceRepository) {
    return new ApolloAuditLogDataInfluenceService(dataInfluenceRepository);
  }

  @Bean
  public ApolloAuditLogService apolloAuditLogService(ApolloAuditLogRepository logRepository) {
    return new ApolloAuditLogService(logRepository);
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditOperatorSupplier.class)
  public ApolloAuditOperatorSupplier apolloAuditLogOperatorSupplier() {
    return new ApolloAuditOperatorDefaultSupplier();
  }

  @Bean
  public ApolloAuditTraceContext apolloAuditTraceContext(
      ApolloAuditOperatorSupplier apolloAuditLogOperatorSupplier) {
    return new ApolloAuditTraceContext(apolloAuditLogOperatorSupplier);
  }

  @Bean
  public ApolloAuditLogApi apolloAuditLogApi(ApolloAuditLogService logService,
      ApolloAuditLogDataInfluenceService dataInfluenceService,
      ApolloAuditTraceContext apolloAuditTraceContext) {
    return new ApolloAuditLogApiJpaImpl(logService, dataInfluenceService, apolloAuditTraceContext);
  }

  @Bean
  public ApolloAuditSpanAspect apolloAuditSpanAspect(ApolloAuditLogApi apolloAuditLogApi) {
    return new ApolloAuditSpanAspect(apolloAuditLogApi);
  }

  @Bean
  public ApolloAuditHttpInterceptor apolloAuditHttpInterceptor(
      ApolloAuditTraceContext traceContext) {
    return new ApolloAuditHttpInterceptor(traceContext);
  }

  @Bean(name = "apolloAuditLogQueryApiPreAuthorizer")
  @ConditionalOnMissingBean(ApolloAuditLogQueryApiPreAuthorizer.class)
  public ApolloAuditLogQueryApiPreAuthorizer apolloAuditLogQueryApiPreAuthorizer() {
    return new ApolloAuditLogQueryApiDefaultPreAuthorizer();
  }

  @Bean
  public ApolloAuditController apolloAuditController(ApolloAuditLogApi api, ApolloAuditProperties apolloAuditProperties) {
    return new ApolloAuditController(api, apolloAuditProperties);
  }

  @Bean
  public ApolloAuditLogDataInfluenceEventListener apolloAuditLogDataInfluenceEventListener(
      ApolloAuditLogApi api) {
    return new ApolloAuditLogDataInfluenceEventListener(api);
  }
}
