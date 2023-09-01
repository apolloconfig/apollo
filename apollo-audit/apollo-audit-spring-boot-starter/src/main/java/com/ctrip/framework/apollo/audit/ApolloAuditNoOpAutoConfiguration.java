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

import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.component.ApolloAuditHttpTracerInterceptor;
import com.ctrip.framework.apollo.audit.component.NoOpApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.context.ApolloAuditScopeManager;
import com.ctrip.framework.apollo.audit.context.ApolloAuditTracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "apollo.audit.log", name = "enabled", havingValue = "false", matchIfMissing = true)
public class ApolloAuditNoOpAutoConfiguration {

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
  @ConditionalOnMissingBean(ApolloAuditLogApi.class)
  public ApolloAuditLogApi apolloAuditLogApi() {
    return new NoOpApolloAuditLogApi();
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditHttpTracerInterceptor.class)
  public ApolloAuditHttpTracerInterceptor apolloAuditLogHttpTracerInterceptor() {
    return new ApolloAuditHttpTracerInterceptor(null);
  }

}
